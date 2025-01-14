package klik.browser.comparators;

//SOURCES ../../image_ml/image_similarity/Feature_vector_source_vgg19.java;

import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.actor.Job_termination_reporter;
import klik.browser.Clearable_cache;
import klik.image_ml.image_similarity.Image_feature_vector_cache;
import klik.image_ml.image_similarity.Image_similarity;
import klik.properties.Static_application_properties;
import klik.util.files_and_paths.Guess_file_type;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;


//**********************************************************
public class Similarity_comparator implements Comparator<Path>, Clearable_cache
//**********************************************************
{
    private final static Map<Path, String> dummy_names = new HashMap<>();
    public static final double THRESHOLD = 0.1;
    //public static final double THRESHOLD = 1_000_000;
    private Map<Path_pair, Integer> distances  = new HashMap<>();
    private final ConcurrentHashMap<Path_pair, Double> similarities = new ConcurrentHashMap<>();

    private Image_feature_vector_cache fv_cache = null;
    Logger logger;
    private final Aborter aborter;
    boolean initialized = false;
    Path similarity_cache_file_path = null;

    //**********************************************************
    public Similarity_comparator(Aborter aborter, Logger logger_)
    //**********************************************************
    {
        this.aborter = aborter;
        logger = logger_;
    }


    //**********************************************************
    @Override
    public void clear_RAM_cache()
    //**********************************************************
    {
        if(fv_cache != null) fv_cache.clear_feature_vector_RAM_cache();
        distances.clear();
    }


    //**********************************************************
    @Override
    public int compare(Path p1, Path p2)
    //**********************************************************
    {
        Path_pair pp = Path_pair.get(p1,p2);
        Integer d = distances.get(pp);
        if (d != null) return d;

        String dummy_name1 = dummy_names.get(p1);
        if ( dummy_name1 == null)
        {
            init(p1.getParent());
        }
        if ( dummy_name1 == null)
        {
            logger.log("WTF dummy_name1 == null for "+p1);
            dummy_name1 = p1.getFileName().toString();
        }

        String dummy_name2 = dummy_names.get(p2);
        if ( dummy_name2 == null)
        {
            logger.log("WTF dummy_name2 == null for "+p2);
            dummy_name2 = p2.getFileName().toString();
        }

        d =  dummy_name1.compareTo(dummy_name2);
        distances.put(pp, d);
        //logger.log("compare "+p1+" vs "+p2+" == "+d);
        return d;
    }


    //**********************************************************
    void init(Path folder)
    //**********************************************************
    {
        if ( initialized) return;
        initialized = true;



        logger.log("init_dummy_names for: "+folder);
        Image_similarity.Result result = Image_similarity.preload_all_feature_vector_in_cache(folder, aborter, logger);
        if (result == null)
        {
            return;
        }
        fv_cache = result.image_feature_vector_ram_cache();
        File[] files_ = folder.toFile().listFiles();
        List<Path> images = new ArrayList<>();

        for (int i = 0 ; i < files_.length; i++)
        {
            if (aborter.should_abort()) return;
            File f1 = files_[i];
            Path p1 = f1.toPath();
            if (Guess_file_type.is_file_an_image(f1))
            {
                images.add(p1);
            }
        }


        {
            String cache_name = "similarity";
            String local = cache_name + folder.toAbsolutePath();
            String cache_file_name = UUID.nameUUIDFromBytes(local.getBytes()) + ".similarity_cache";
            Path dir = Static_application_properties.get_absolute_dir_on_user_home(Static_application_properties.IMAGE_SIMILARITY_CACHE_DIR, false, logger);
            if (dir != null)
            {
                logger.log("similarity cache folder=" + dir.toAbsolutePath());
            }
            similarity_cache_file_path = Path.of(dir.toAbsolutePath().toString(), cache_file_name);
        }
        if ( !reload_similarity_cache_from_disk(folder.toAbsolutePath().toString(), aborter))
        {
            // no cache on disk, have to recalculate
            Similarity_cache_warmer_actor actor = new Similarity_cache_warmer_actor(images, fv_cache, similarities, logger);
            CountDownLatch cdl = new CountDownLatch(images.size());
            for (Path p1 : images) {
                Similarity_cache_warmer_message m = new Similarity_cache_warmer_message(aborter, p1);
                Job_termination_reporter tr = (message, job) -> {
                    cdl.countDown();
                    if (cdl.getCount() % 100 == 0)
                        logger.log(" similarity cache filler: " + cdl.getCount() + " for " + p1);
                };
                Actor_engine.run(actor, m, tr, logger);
            }

            try {
                cdl.await();
            } catch (InterruptedException e) {
                logger.log("similarity cache interrupted" + e);
            }
            save_similarity_cache_to_disk();
        }
        //logger.log("\n\nmin "+Similarity_cache_warmer_actor.min+" max "+Similarity_cache_warmer_actor.max);
        if ( aborter.should_abort()) return;
        Collections.shuffle(images);
        while (!images.isEmpty())
        {
            if ( aborter.should_abort()) return;
            Path p1 = images.remove(0);
            dummy_names.put(p1,p1.getFileName().toString());
            Iterator<Path> it = images.iterator();
            while (it.hasNext())
            {
                if ( aborter.should_abort()) return;
                Path p2 = it.next();
                Double diff = similarities.get(Path_pair.get(p1,p2));
                if ( diff == null)
                {
                    logger.log("WTF diff == null for "+p1+" vs "+p2);
                    continue;
                }
                if ( diff < THRESHOLD)
                {
                    it.remove();
                    dummy_names.put(p2,p1.getFileName().toString()+diff+p2.getFileName().toString());
                }
            }
        }
        for (Path p: images)
        {
            dummy_names.put(p,p.getFileName().toString());
        }

        logger.log("init_dummy_names done !");
    }


    //**********************************************************
    public synchronized boolean reload_similarity_cache_from_disk(String folder, Aborter aborter)
    //**********************************************************
    {
        int reloaded = 0;
        try(DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(similarity_cache_file_path.toFile()))))
        {
            int number_of_items = dis.readInt();
            for ( int k = 0; k < number_of_items; k++)
            {
                if ( aborter.should_abort()) return false;
                String path1_string = dis.readUTF();
                String path2_string = dis.readUTF();
                double val = dis.readDouble();
                Path p1 = Path.of(folder,path1_string);
                Path p2 = Path.of(folder,path2_string);
                Path_pair p = Path_pair.get(p1,p2);
                similarities.put(p,val);
                logger.log("from disk similarity "+val+" for "+path1_string+" "+path2_string);
                reloaded++;
            }
            logger.log(reloaded+" similarities reloaded from file");
            return true;
        }
        catch (FileNotFoundException e)
        {
            logger.log("first time in this folder: "+e);
        }
        catch (IOException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
        }
        return false;
    }

    //**********************************************************
    public void save_similarity_cache_to_disk()
    //**********************************************************
    {

        int saved = 0;
        try(DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(similarity_cache_file_path.toFile()))))
        {
            dos.writeInt(similarities.size());
            for(Map.Entry<Path_pair, Double> e : similarities.entrySet())
            {
                Path_pair pp = e.getKey();
                Path pi1 = pp.i();
                Path pi2 = pp.j();
                dos.writeUTF(pi1.getFileName().toString());
                dos.writeUTF(pi2.getFileName().toString());
                dos.writeDouble(e.getValue());
                saved++;
                logger.log("to disk similarity "+e.getValue()+" for "+pi1.getFileName().toString()+" "+pi2.getFileName().toString());
            }
        }
        catch (IOException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
        }

        //if (dbg)
        logger.log(saved +" similarities from cache saved to file");
    }

}
