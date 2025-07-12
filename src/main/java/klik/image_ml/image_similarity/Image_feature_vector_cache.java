package klik.image_ml.image_similarity;

//SOURCES ./Image_feature_vector_actor.java
//SOURCES ./Image_feature_vector_message.java
//SOURCES ../../browser/Shared_services.java



import javafx.stage.Stage;
import javafx.stage.Window;
import klik.actor.Aborter;
import klik.actor.Job_termination_reporter;
import klik.actor.workers.Actor_engine_based_on_workers;
import klik.browser.Shared_services;
import klik.browser.virtual_landscape.Browsing_caches;
import klik.browser.virtual_landscape.Path_list_provider;
import klik.image_ml.Feature_vector;
import klik.properties.Non_booleans;
import klik.properties.Cache_folder;
import klik.properties.boolean_features.Feature;
import klik.properties.boolean_features.Feature_cache;
import klik.util.files_and_paths.Guess_file_type;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import klik.util.ui.Show_running_film_frame_with_abort_button;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

//**********************************************************
public class Image_feature_vector_cache
//**********************************************************
{
    public final static boolean dbg = false;
    protected final Logger logger;
    private final Aborter shared_services_aborter;
    protected final String cache_type;
    protected final Path cache_file_path;


    public record Images_and_feature_vectors(Image_feature_vector_cache fv_cache, List<Path> images) { }
    private final Map<String, Feature_vector> path_to_feature_vector_cache = new ConcurrentHashMap<>();
    private final Image_feature_vector_actor image_feature_vector_actor;

    private final int instance_number;
    private static int instance_number_generator = 0;
    private final Actor_engine_based_on_workers local_actor_engine;

    //**********************************************************
    private Image_feature_vector_cache(String tag, String cache_type_, Aborter shared_services_aborter, Logger logger_)
    //**********************************************************
    {
        instance_number = instance_number_generator++;
        logger = logger_;
        this.shared_services_aborter = shared_services_aborter; // as this is a shared cache, closing the browser that created it must not disable it
        cache_type = cache_type_;
        String local = cache_type + tag;//path.toAbsolutePath();
        String cache_file_name = UUID.nameUUIDFromBytes(local.getBytes()) +".fv_cache";
        Path dir = get_image_feature_vector_cache_dir(null, shared_services_aborter,logger);
        cache_file_path= Path.of(dir.toAbsolutePath().toString(), cache_file_name);
        if ( dbg) logger.log(cache_type +" cache file ="+cache_file_path);
        image_feature_vector_actor = new Image_feature_vector_actor(shared_services_aborter);

        // reason to use workers is to limit the number of concurrent HTTP requests
        // to the vgg19 python servers that are not good at queuing requests
        local_actor_engine = new Actor_engine_based_on_workers(logger);

    }

    //**********************************************************
    public static Path get_image_feature_vector_cache_dir(Stage owner, Aborter aborter, Logger logger)
    //**********************************************************
    {

        Path tmp_dir = Non_booleans.get_absolute_hidden_dir_on_user_home(Cache_folder.klik_image_feature_vectors_cache.name(), false,owner,logger);
        if (dbg) if (tmp_dir != null) {
            logger.log("Image feature vector cache folder=" + tmp_dir.toAbsolutePath());
        }
        return tmp_dir;
    }

    //**********************************************************
    // if wait_if_needed is true, tr can be null
    public Feature_vector get_from_cache(Path p, Job_termination_reporter tr, boolean wait_if_needed, Window owner, Aborter browser_aborter)
    //**********************************************************
    {
        Feature_vector feature_vector =  path_to_feature_vector_cache.get(key_from_path(p));
        if ( feature_vector != null)
        {
            if ( tr != null) tr.has_ended("found in cache",null);
            return feature_vector;
        }
        if ( shared_services_aborter.should_abort())
        {
            logger.log(("image feature vector cache instance#"+instance_number+" request aborted: ->"+shared_services_aborter.name+"<- reason="+shared_services_aborter.reason+ " target path="+p));
            return null;
        }
        if ( browser_aborter.should_abort())
        {
            logger.log(("image feature vector cache instance#"+instance_number+" request aborted: ->"+browser_aborter.name+"<- reason="+browser_aborter.reason+ " target path="+p));
            return null;
        }
        else
        {
            //logger.log(instance_number+" OK aborter "+aborter.name+" reason="+aborter.reason);
        }
        Image_feature_vector_message imp = new Image_feature_vector_message(p,this,owner,browser_aborter,logger);
        if ( wait_if_needed)
        {
            image_feature_vector_actor.run(imp); // blocking call
            Feature_vector x = path_to_feature_vector_cache.get(key_from_path(p));
            //if ( x == null) logger.log("PANIC null Feature_vector in cache after blocking call ");
            return x;
        }
        local_actor_engine.run(image_feature_vector_actor,imp,tr,logger);
        return null;
    }

    //**********************************************************
    private static String key_from_path(Path p)
    //**********************************************************
    {
        return p.getFileName().toString();
    }
    //**********************************************************
    public void inject(Path path, Feature_vector fv)
    //**********************************************************
    {
        if(dbg) logger.log(cache_type +" inject "+path+" value="+fv );
        path_to_feature_vector_cache.put(key_from_path(path), fv);
    }

    //**********************************************************
    public void clear_feature_vector_RAM_cache()
    //**********************************************************
    {
        path_to_feature_vector_cache.clear();
        if (dbg) logger.log("feature vector cache file cleared");
    }
    //**********************************************************
    public synchronized void reload_cache_from_disk(AtomicInteger in_flight, Aborter browser_aborter)
    //**********************************************************
    {
        int reloaded = 0;
        try(DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(cache_file_path.toFile()))))
        {
            int number_of_vectors = dis.readInt();
            in_flight.set(number_of_vectors);
            for ( int i = 0; i < number_of_vectors; i++)
            {
                if ( browser_aborter.should_abort())
                {
                    logger.log("aborting : Image_feature_vector_cache::reload_cache_from_disk "+browser_aborter.reason);
                    return;
                }
                String path_string = dis.readUTF();
                int size_of_vector = dis.readInt();
                double[] vector = new double[size_of_vector];
                for ( int j = 0; j < size_of_vector; j++)
                {
                    double val = dis.readDouble();
                    vector[j] = val;
                }
                Feature_vector fv = new Feature_vector(vector);
                path_to_feature_vector_cache.put(path_string, fv);
                in_flight.decrementAndGet();
                reloaded++;
                if ( i%1000==0) logger.log(i+" feature vectors loaded from disk");
            }
        }
        catch (FileNotFoundException e)
        {
            logger.log("first time in this folder: "+e);
        }
        catch (IOException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
        }

        //if (dbg)
            logger.log((cache_type +": "+reloaded+" feature vectors reloaded from file"));

        if ( dbg)
        {
            logger.log("\n\n\n********************* "+ cache_type + " CACHE************************");
            for (String s  : path_to_feature_vector_cache.keySet())
            {
                logger.log(s+" => "+ path_to_feature_vector_cache.get(s));
            }
            logger.log("****************************************************************\n\n\n");
        }
    }

    //**********************************************************
    public void save_whole_cache_to_disk()
    //**********************************************************
    {
        int saved = 0;
        try(DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(cache_file_path.toFile()))))
        {
            dos.writeInt(path_to_feature_vector_cache.size());
            for(Map.Entry<String, Feature_vector> e : path_to_feature_vector_cache.entrySet())
            {
                saved++;
                dos.writeUTF(e.getKey());
                Feature_vector fv = e.getValue();
                dos.writeInt(fv.features.length);
                for ( int i = 0 ; i < fv.features.length; i++)
                {
                    dos.writeDouble(fv.features[i]);
                }
            }
        }
        catch (IOException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
        }

       // logger.log("feature vector components min = "+min+" max = "+ max);
        //if (dbg)
            logger.log(saved +" feature vectors from cache saved to file");
    }

    //**********************************************************
    public static Images_and_feature_vectors preload_all_feature_vector_in_cache(
            Path_list_provider path_list_provider,
            Window owner,
            double x, double y,
            Aborter browser_aborter,
            Logger logger)
    //**********************************************************
    {
        Image_feature_vector_cache image_feature_vector_cache = Browsing_caches.fv_cache_of_caches.get(path_list_provider.get_name());
        AtomicInteger in_flight = new AtomicInteger(1); // '1' to keep it alive until update settles the final count
        if ( image_feature_vector_cache == null)
        {
            Show_running_film_frame_with_abort_button.show_running_film(in_flight,"Wait, calling ML servers to get feature vectors",20000, x,y,logger);
            image_feature_vector_cache = new Image_feature_vector_cache(path_list_provider.get_name(), "image_feature_vectors", Shared_services.shared_services_aborter, logger);
            Images_and_feature_vectors images_and_feature_vectors = image_feature_vector_cache.read_from_disk_and_update(path_list_provider,in_flight, owner, browser_aborter,logger);
            Browsing_caches.fv_cache_of_caches.put(path_list_provider.get_name(),image_feature_vector_cache);
            return images_and_feature_vectors;
        }
        return image_feature_vector_cache.update(path_list_provider, in_flight,owner, browser_aborter,logger);
    }

    //**********************************************************
    private Images_and_feature_vectors read_from_disk_and_update(Path_list_provider path_list_provider , AtomicInteger in_flight, Window owner, Aborter browser_aborter, Logger logger)
    //**********************************************************
    {
        reload_cache_from_disk(in_flight,browser_aborter);

        logger.log("read_from_disk "+path_to_feature_vector_cache.size()+" fv from disk for:"+path_list_provider.get_name());
        return update( path_list_provider, in_flight, owner, browser_aborter, logger);
    }

    //**********************************************************
    private  Images_and_feature_vectors update(
            Path_list_provider path_list_provider,
            AtomicInteger in_flight, Window owner, Aborter browser_aborter,Logger logger)
    //**********************************************************
    {
        List<Path> images = new ArrayList<>();
        List<Path> missing_images = new ArrayList<>();
        for (Path p : path_list_provider.only_image_paths(Feature_cache.get(Feature.Show_hidden_files)))
        {
            if ( !Guess_file_type.is_file_an_image(p.toFile())) continue;
            images.add(p);
            if ( !path_to_feature_vector_cache.containsKey(p.getFileName().toString()))
            {
                missing_images.add(p);
            }
        }
        in_flight.addAndGet(missing_images.size()-1); //-1 to compensate the +1 "keep alive" in preload_all_feature_vector_in_cache
        CountDownLatch cdl = new CountDownLatch(missing_images.size());
        Job_termination_reporter tr = (message, job) -> {
            in_flight.decrementAndGet();
            cdl.countDown();
        };
        for (Path p :missing_images)
        {
            get_from_cache(p,tr,false, owner, browser_aborter);
        }
        try {
            cdl.await();
        } catch (InterruptedException e) {
            logger.log("Images_and_feature_vectors from_disk interrupted:"+e);
        }

        if (!missing_images.isEmpty())
        {
            logger.log(("update: "+missing_images.size()+" new images added for:"+path_list_provider.get_name()));
            save_whole_cache_to_disk();
        }
        return new Images_and_feature_vectors(this, images);
    }


}
