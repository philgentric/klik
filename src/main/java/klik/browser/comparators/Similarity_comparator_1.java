package klik.browser.comparators;

//SOURCES ../../image_ml/image_similarity/Feature_vector_source_vgg19.java;

import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.actor.Job_termination_reporter;
import klik.browser.Clearable_cache;
import klik.image_ml.image_similarity.Image_feature_vector_RAM_cache;
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
public class Similarity_comparator_1 implements Comparator<Path>, Clearable_cache
//**********************************************************
{
    //private final static Map<Path, Feature_vector> cache = new HashMap<>();
    private final static ConcurrentHashMap<Integer, String> dummy_names = new ConcurrentHashMap<>();
    private final static ConcurrentHashMap<Path_pair, Double> similarities = new ConcurrentHashMap<>();


    List<Path> int_to_path = new ArrayList<>();
    private final Map<Path, Integer> path_to_int = new HashMap<>();

    Map<Path_pair, Integer> distances  = new HashMap<>();


    private Image_feature_vector_RAM_cache fv_cache = null;
    Logger logger;
    private final Aborter aborter;
    private boolean initialized = false;
    private Path similarity_cache_file_path;

    //**********************************************************
    public Similarity_comparator_1(Aborter aborter, Logger logger_)
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
            Path_pair p = Path_pair.get(i,j);
            Integer d = distances.get(p);
            if (d != null) return d;
        }
        String dummy_name1 = dummy_names.get(i);
        if ( dummy_name1 == null)
        {
            dummy_name1 = p1.getFileName().toString();
        }

        String dummy_name2 = dummy_names.get(j);
        if ( dummy_name2 == null)
        {
            dummy_name2 = p2.getFileName().toString();
        }

        Integer d =  dummy_name1.compareTo(dummy_name2);
        Path_pair p = Path_pair.get(i,j);
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
        Image_similarity.Result result = Image_similarity.preload_all_feature_vector_in_cache(folder, aborter, logger);
        if (result == null)
        {
            return;
        }
        Image_feature_vector_RAM_cache cache = result.image_feature_vector_ram_cache();
        logger.log("fv preloaded in "+(System.currentTimeMillis() - start)+" ms");

        //reload_similarity_cache_from_disk(folder.toAbsolutePath().toString(),aborter);

        fill_similarity_cache(images, cache, files_);
        if ( aborter.should_abort()) return;
        fill_dummy_names(folder,images);

        //save_similarity_cache_to_disk(int_to_path);
        logger.log("init_dummy_names done !"+(System.currentTimeMillis() - start)+" ms");
    }

    //**********************************************************
    private boolean fill_dummy_names(Path folder,List<Path> images)
    //**********************************************************
    {
        // pick one file at random and find its 1 closest neighbor, using actors
        Collections.shuffle(images);
        //CountDownLatch cdl = new CountDownLatch(images.size());
        Most_similar_actor most_similar_actor = new Most_similar_actor(int_to_path,path_to_int,similarities,dummy_names,logger);
        for(;;)
        {
            if ( aborter.should_abort()) return true;
            Path p1 = images.removeFirst();
            if ( images.isEmpty()) break;
            logger.log("images size: "+images.size());
            List<Path> images_copy = new ArrayList<>(images);
            Most_similar_message m = new Most_similar_message(p1,images_copy,aborter);
            /*Job_termination_reporter tr = (message, job) -> {
                cdl.countDown();
                if ( cdl.getCount() % 100 == 0) logger.log("init_dummy_names: "+cdl.getCount());
            };*/
            //Actor_engine.run(most_similar_actor, m,tr, logger);
            String closest_path = most_similar_actor.run(m);
            if (closest_path!= null)
            {
                images.remove(Path.of(folder.toAbsolutePath().toString(),closest_path));
                if ( images.isEmpty()) break;
            }
        }

        /*try {
            cdl.await();
        } catch (InterruptedException e) {
            logger.log("init_dummy_names interrupted (2) "+e);
        }*/
        logger.log("Dummy Names cache done");
        return true;
    }

    //**********************************************************
    private void fill_similarity_cache(List<Path> images, Image_feature_vector_RAM_cache cache, File[] files_)
    //**********************************************************
    {
        long start = System.currentTimeMillis();
        CountDownLatch cdl = new CountDownLatch(images.size());
        Similarity_cache_warmer_actor actor = new Similarity_cache_warmer_actor(int_to_path,path_to_int,images, cache, similarities,logger);
        for (int i = 0; i < files_.length; i++)
        {
            if ( aborter.should_abort()) return;
            File f1 = files_[i];
            if (!Guess_file_type.is_file_an_image(f1)) continue;
            Similarity_cache_warmer_message m = new Similarity_cache_warmer_message(aborter, i);
            int finalI = i;
            Job_termination_reporter tr = (message, job) -> {
                cdl.countDown();
                if ( cdl.getCount() % 100 == 0) logger.log(finalI + " similarity cache warmer: "+cdl.getCount());
            };
            Actor_engine.run(actor, m,tr, logger);
        }

        logger.log("similarity cache warmer, going to wait on :"+cdl.getCount());

        // wait for the actors to fill the similarity cache
        try {
            cdl.await();
        } catch (InterruptedException e) {
            logger.log("similarity cache warmer interrupted"+e);
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
                Path_pair p = Path_pair.get(i,j);
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
            for(Map.Entry<Path_pair, Double> e : similarities.entrySet())
            {
                Path_pair pp = e.getKey();
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
