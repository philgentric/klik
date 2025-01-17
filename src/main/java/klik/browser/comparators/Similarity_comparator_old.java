package klik.browser.comparators;

//SOURCES ../../image_ml/image_similarity/Feature_vector_source_embeddings.java;

import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.actor.Job_termination_reporter;
import klik.browser.Clearable_RAM_cache;
import klik.image_ml.image_similarity.Image_feature_vector_cache;
import klik.properties.Static_application_properties;
import klik.util.files_and_paths.Guess_file_type;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import static klik.browser.comparators.Similarity_comparator.THRESHOLD;


//**********************************************************
public class Similarity_comparator_old implements Comparator<Path>, Clearable_RAM_cache
//**********************************************************
{
    // can be static as Path
    private final static ConcurrentHashMap<Path, String> dummy_names = new ConcurrentHashMap<>();

    // cannot be static as Path_pair is instable with dir content
    private final ConcurrentHashMap<Path_pair_int, Double> similarities = new ConcurrentHashMap<>();
    Map<Path_pair_int, Integer> distances  = new HashMap<>();

    List<Path> int_to_path = new ArrayList<>();
    private final Map<Path, Integer> path_to_int = new HashMap<>();



    private Image_feature_vector_cache fv_cache = null;
    Logger logger;
    private final Aborter aborter;
    private boolean initialized = false;
    private Path similarity_cache_file_path;

    //**********************************************************
    public Similarity_comparator_old(Aborter aborter, Logger logger_)
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
        dummy_names.clear();
        similarities.clear();

    }


    //**********************************************************
    @Override
    public int compare(Path p1, Path p2)
    //**********************************************************
    {
        if ( !initialized) init(p1.getParent());
        Integer i = path_to_int.get(p1);
        if ( i == null)
        {
            logger.log("WTF i == null for "+p1);
            return -1;
        }
        Integer j = path_to_int.get(p2);
        if ( j == null)
        {
            logger.log("WTF j == null for "+p2);
            return -1;
        }
        {
            Path_pair_int p = Path_pair_int.get(i,j);
            Integer d = distances.get(p);
            if (d != null) return d;
        }
        String dummy_name1 = dummy_names.get(p1);
        if ( dummy_name1 == null)
        {
            dummy_name1 = p1.getFileName().toString();
        }

        String dummy_name2 = dummy_names.get(p2);
        if ( dummy_name2 == null)
        {
            dummy_name2 = p2.getFileName().toString();
        }

        Integer d =  dummy_name1.compareTo(dummy_name2);
        Path_pair_int p = Path_pair_int.get(i,j);
        distances.put(p, d);

        //logger.log("compare "+p1+" vs "+p2+" == "+d);
        return d;
    }




    //**********************************************************
    void init(Path folder)
    //**********************************************************
    {
        initialized = true;
        long start = System.currentTimeMillis();

        logger.log("init for: "+folder);

        File[] files_ = folder.toFile().listFiles();
        if ( files_ == null)
        {
            logger.log("WTF files_ == null for "+folder);
            return;
        }
        if ( files_.length == 0)
        {
            logger.log("no files in "+folder);
            return;
        }

        List<Path> images = new ArrayList<>();
        for (int i = 0 ; i < files_.length; i++)
        {
            if (aborter.should_abort()) return;
            File f1 = files_[i];
            Path p1 = f1.toPath();
            int_to_path.add(p1);
            path_to_int.put(p1, i);
            logger.log("path vs int "+p1.getFileName().toString()+", "+i+")");
            if (Guess_file_type.is_file_an_image(f1))
            {
                images.add(p1);
            }
        }
        if (images.isEmpty())
        {
            logger.log(folder+" contains no images");
            return;
        }
        {
            String cache_name = "similarity";
            String local = cache_name + folder.toAbsolutePath();
            String cache_file_name = UUID.nameUUIDFromBytes(local.getBytes()) + ".similarity_cache";
            Path dir = Static_application_properties.get_absolute_dir_on_user_home(Static_application_properties.IMAGE_SIMILARITY_CACHE_DIR, false, logger);
            if (dir != null) {
                logger.log("similarity cache folder=" + dir.toAbsolutePath());
            }

            similarity_cache_file_path = Path.of(dir.toAbsolutePath().toString(), cache_file_name);
        }


        logger.log("preloading FVs into cache");
        Image_feature_vector_cache.Images_and_feature_vectors result = Image_feature_vector_cache.preload_all_feature_vector_in_cache(folder, aborter, logger);
        if (result == null)
        {
            return;
        }
        fv_cache = result.image_feature_vector_ram_cache();
        logger.log("fv preloaded in "+(System.currentTimeMillis() - start)+" ms");

        //reload_similarity_cache_from_disk(folder.toAbsolutePath().toString(),aborter);

        fill_similarity_cache(images, fv_cache, files_);
        if ( aborter.should_abort()) return;
        fill_dummy_names(files_,images);

        //save_similarity_cache_to_disk(int_to_path);
        logger.log("init_dummy_names done !"+(System.currentTimeMillis() - start)+" ms");
    }

    //**********************************************************
    private void fill_dummy_names(File[] files_, List<Path> images)
    //**********************************************************
    {

        for ( Path_pair_int p : similarities.keySet())
        {
            int i = p.i();
            if ( i >= int_to_path.size())
            {
                logger.log("WTF i >= int_to_path.size() for "+p);
                continue;
            }
            int j = p.j();
            if ( j >= int_to_path.size())
            {
                logger.log("WTF j >= int_to_path.size() for "+p);
                continue;
            }
            Path p1 = int_to_path.get(i);
            Path p2 = int_to_path.get(j);
            double diff = similarities.get(p);
            if ( diff > THRESHOLD)
            {
                logger.log("WTF????? diff > 0.2 for "+p1+" vs "+p2+" == "+diff);
            }
            dummy_names.put(p2,p1.getFileName().toString()+diff+p2.getFileName().toString());
        }


        logger.log("Dummy Names cache done");
    }

    //**********************************************************
    private void fill_similarity_cache(List<Path> images, Image_feature_vector_cache fv_cache, File[] files_)
    //**********************************************************
    {
        long start = System.currentTimeMillis();
        Similarity_cache_warmer_actor_old actor = new Similarity_cache_warmer_actor_old(int_to_path,path_to_int,images, fv_cache, similarities,logger);
        CountDownLatch cdl = new CountDownLatch(images.size());
        for (Path p1 : images)
        {
            int i =  path_to_int.get(p1);
            Similarity_cache_warmer_message_old m = new Similarity_cache_warmer_message_old(aborter,i);
            Job_termination_reporter tr = (message, job) -> {
                cdl.countDown();
                if ( cdl.getCount() % 100 == 0) logger.log(" similarity cache filler: "+cdl.getCount()+" for "+p1);
            };
            Actor_engine.run(actor, m,tr, logger);
        }

        try {
            cdl.await();
        } catch (InterruptedException e) {
            logger.log("similarity cache interrupted"+e);
        }

        for ( int i = 0; i < files_.length; i++)
        {
            for ( int j = i+1; j < files_.length; j++)
            {
                Path_pair_int p = Path_pair_int.get(i,j);
                Double x = similarities.get(p);
                if ( x == null)
                {
                    logger.log("WTF similarity == null for " + p);
                    continue;
                }
                if ( x > THRESHOLD)
                {
                    similarities.remove(p);
                }
            }
        }
        logger.log("similarity cache done in: "+(System.currentTimeMillis() - start)+" ms");
    }



    //**********************************************************
    public synchronized int reload_similarity_cache_from_disk(String folder, Aborter aborter)
    //**********************************************************
    {
        int reloaded = 0;
        try(DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(similarity_cache_file_path.toFile()))))
        {
            int number_of_items = dis.readInt();
            for ( int k = 0; k < number_of_items; k++)
            {
                if ( aborter.should_abort()) return -1;
                String path1_string = dis.readUTF();
                String path2_string = dis.readUTF();
                double val = dis.readDouble();
                Integer i = path_to_int.get(Path.of(folder,path1_string));
                if ( i == null) continue;
                Integer j = path_to_int.get(Path.of(folder,path2_string));
                if ( j == null) continue;
                Path_pair_int p = Path_pair_int.get(i,j);
                similarities.put(p,val);
                logger.log("from disk similarity "+val+" for "+path1_string+" "+path2_string);
                reloaded++;
            }
            logger.log(reloaded+" similarities reloaded from file");
        }
        catch (FileNotFoundException e)
        {
            logger.log("first time in this folder: "+e);
        }
        catch (IOException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
        }
        return reloaded;
    }

    //**********************************************************
    public void save_similarity_cache_to_disk(List<Path> int_to_path)
    //**********************************************************
    {
        if ( int_to_path.isEmpty()) return;

        int saved = 0;
        try(DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(similarity_cache_file_path.toFile()))))
        {
            dos.writeInt(similarities.size());
            for(Map.Entry<Path_pair_int, Double> e : similarities.entrySet())
            {
                Path_pair_int pp = e.getKey();
                Path pi1 = int_to_path.get(pp.i());
                Path pi2 = int_to_path.get(pp.j());
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
