package klik.machine_learning.feature_vector;

//SOURCES ./Feature_vector_creation_actor.java
//SOURCES ./Feature_vector_message.java
//SOURCES ../../browser/Shared_services.java



import javafx.stage.Stage;
import javafx.stage.Window;
import klik.util.execute.actor.Aborter;
import klik.util.execute.actor.Job_termination_reporter;
import klik.util.execute.actor.Or_aborter;
import klik.util.execute.actor.workers.Actor_engine_based_on_workers;
import klik.browser.Clearable_RAM_cache;
import klik.browser.virtual_landscape.Browsing_caches;
import klik.path_lists.Path_list_provider;
import klik.machine_learning.song_similarity.Feature_vector_for_song;
import klik.properties.Non_booleans_properties;
import klik.properties.Cache_folder;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import klik.util.perf.Perf;
import klik.util.ui.Progress_window;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;


//**********************************************************
public class Feature_vector_cache implements Clearable_RAM_cache
//**********************************************************
{
    private final static boolean ultra_dbg = false;
    byte features_are_double = 0x01;
    byte feature_vector_is_string = 0x02;
    public final static boolean dbg = false;
    protected final Logger logger;
    //private final Aborter aborter;
    protected final String tag;
    protected final Path cache_file_path;




    public record Paths_and_feature_vectors(Feature_vector_cache fv_cache, List<Path> paths) { }
    private final Map<String, Feature_vector> path_to_feature_vector_cache = new ConcurrentHashMap<>();
    private final Feature_vector_creation_actor feature_vector_creation_actor;

    private final int instance_number;
    private static int instance_number_generator = 0;
    private final Actor_engine_based_on_workers local_actor_engine;

    //**********************************************************
    public Feature_vector_cache(
            String tag,
            Feature_vector_source fvs,
            Aborter aborter,
            Logger logger_)
    //**********************************************************
    {
        instance_number = instance_number_generator++;
        logger = logger_;
        //this.aborter = aborter; // as this is a shared cache, closing the browser that created it must not disable it
        this.tag = tag;
        String cache_file_name = UUID.nameUUIDFromBytes(tag.getBytes()) +".fv_cache";
        Path dir = get_feature_vector_cache_dir(null,logger);
        cache_file_path= Path.of(dir.toAbsolutePath().toString(), cache_file_name);
        if ( dbg) logger.log(tag +" cache file ="+cache_file_path);
        feature_vector_creation_actor = new Feature_vector_creation_actor(fvs);

        // reason to use workers is to limit the number of concurrent HTTP requests
        // to the python servers that are not good at queuing requests
        local_actor_engine = new Actor_engine_based_on_workers("feature vector cache warmer",aborter,logger);

    }

    //**********************************************************
    public static Path get_feature_vector_cache_dir(Stage owner, Logger logger)
    //**********************************************************
    {

        Path tmp_dir = Non_booleans_properties.get_absolute_hidden_dir_on_user_home(Cache_folder.klik_feature_vectors_cache.name(), false,owner,logger);
        if (dbg) if (tmp_dir != null) {
            logger.log("Feature vector cache folder=" + tmp_dir.toAbsolutePath());
        }
        return tmp_dir;
    }

    //**********************************************************
    // if wait_if_needed is true, tr can be null
    public Feature_vector get_from_cache_or_make(Path p, Job_termination_reporter tr, boolean wait_if_needed, Window owner, Aborter browser_aborter)
    //**********************************************************
    {
        Feature_vector feature_vector =  path_to_feature_vector_cache.get(key_from_path(p));
        if ( feature_vector != null)
        {
            if ( dbg) logger.log("feature_vector found in cache for "+p);
            if ( tr != null) tr.has_ended("found in cache",null);
            return feature_vector;
        }
        if ( browser_aborter.should_abort())
        {
            logger.log(("feature vector cache instance#"+instance_number+" request aborted: ->"+browser_aborter.name+"<- reason="+browser_aborter.reason()+ " target path="+p));
            return null;
        }
        else
        {
            //logger.log(instance_number+" OK aborter "+aborter.name+" reason="+aborter.reason);
        }
        if ( dbg) logger.log("going to make feature_vector for "+p);

        Feature_vector_build_message imp = new Feature_vector_build_message(p,this,owner,browser_aborter,logger);
        if ( wait_if_needed)
        {
            feature_vector_creation_actor.run(imp); // blocking call
            Feature_vector x = path_to_feature_vector_cache.get(key_from_path(p));
            if ( x == null) logger.log("❌ PANIC null Feature_vector in cache after blocking call ");
            return x;
        }
        local_actor_engine.run(feature_vector_creation_actor,imp,tr,logger);
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
        if(dbg) logger.log(tag +" inject "+path+" value="+fv.to_string()+" components");
        path_to_feature_vector_cache.put(key_from_path(path), fv);
    }

    //**********************************************************
    @Override
    public void clear_RAM_cache()
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
            byte type = dis.readByte();

            int number_of_vectors = dis.readInt();
            in_flight.set(number_of_vectors);
            for ( int i = 0; i < number_of_vectors; i++)
            {
                if ( browser_aborter.should_abort())
                {
                    logger.log("aborting : Feature_vector_cache::reload_cache_from_disk "+browser_aborter.reason());
                    return;
                }
                String path_string = dis.readUTF();
                Feature_vector fv = null;
                if ( type == features_are_double)
                {
                    int size_of_vector = dis.readInt();
                    double[] vector = new double[size_of_vector];
                    for (int j = 0; j < size_of_vector; j++) {
                        double val = dis.readDouble();
                        vector[j] = val;
                    }
                    fv = new Feature_vector_double(vector);
                }
                if ( type == feature_vector_is_string)
                {
                    String s = dis.readUTF();
                    fv = new Feature_vector_for_song(s,logger);
                }
                if ( fv == null)
                {
                    logger.log("❌ Fatal, unknown feature vector type in cache file");
                    return;
                }
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
            logger.log((tag +": "+reloaded+" feature vectors reloaded from file"));

        if ( dbg)
        {
            logger.log("\n\n\n********************* "+ tag + " CACHE************************");
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
            // extract first feature vector to decide type
            if ( path_to_feature_vector_cache.size() == 0)
            {
                logger.log("feature vector cache empty, nothing to save");
                return;
            }
            Map.Entry<String, Feature_vector> e0 = path_to_feature_vector_cache.entrySet().iterator().next();
            boolean feature_vectors_are_double = false;
            if ( e0.getValue() instanceof Feature_vector_double)
            {
                dos.writeByte(features_are_double);
                feature_vectors_are_double = true;
            }
            if ( e0.getValue() instanceof Feature_vector_for_song)
            {
                dos.writeByte(feature_vector_is_string);
            }
            dos.writeInt(path_to_feature_vector_cache.size());
            for(Map.Entry<String, Feature_vector> e : path_to_feature_vector_cache.entrySet())
            {
                Feature_vector fv = e.getValue();
                if ( fv == null)
                {
                    logger.log("❌ PANIC null feature vector for key="+e.getKey());
                    continue;
                }
                if ( feature_vectors_are_double)
                {
                    Feature_vector_double fvd = (Feature_vector_double) fv;
                    if (fvd.features == null) {
                        logger.log("❌ PANIC null features for key=" + e.getKey());
                        continue;
                    }
                    saved++;
                    dos.writeUTF(e.getKey());
                    dos.writeInt(fvd.features.length);
                    for (int i = 0; i < fvd.features.length; i++) {
                        dos.writeDouble(fvd.features[i]);
                    }
                }
                else
                {
                    Feature_vector_for_song fvb = (Feature_vector_for_song) fv;
                    if (fvb.original_string == null) {
                        logger.log("❌ PANIC null original_string for key=" + e.getKey());
                        continue;
                    }
                    saved++;
                    dos.writeUTF(e.getKey());
                    dos.writeUTF(fvb.original_string);
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
    public static Paths_and_feature_vectors preload_all_feature_vector_in_cache(
            Feature_vector_source fvs,
            List<Path> paths,
            Path_list_provider path_list_provider,
            Window owner,
            double x, double y,
            Aborter browser_aborter,
            Logger logger)
    //**********************************************************
    {
        try( Perf p = new Perf("preload_all_feature_vector_in_cache"))
        {
        Feature_vector_cache feature_vector_cache = Browsing_caches.fv_cache_of_caches.get(path_list_provider.get_name());
        AtomicInteger in_flight = new AtomicInteger(1); // '1' to keep it alive until update settles the final count
        if ( feature_vector_cache == null)
        {
            Progress_window progress_window = Progress_window.show(
                    in_flight,
                    "Wait, making feature vectors",
                    3600*60,
                    x,
                    y,
                    owner,
                    logger);

            Or_aborter or_aborter = new Or_aborter(browser_aborter,progress_window.aborter,logger);
            feature_vector_cache = new Feature_vector_cache(path_list_provider.get_name(), fvs, or_aborter,logger);
            Paths_and_feature_vectors paths_and_feature_vectors = feature_vector_cache.read_from_disk_and_update(paths,in_flight, owner, or_aborter,logger);
            Browsing_caches.fv_cache_of_caches.put(path_list_provider.get_name(),feature_vector_cache);
            progress_window.close();
            return paths_and_feature_vectors;
        }
        return feature_vector_cache.update(paths, in_flight,owner, browser_aborter,logger);
        }
    }

    //**********************************************************
    private Paths_and_feature_vectors read_from_disk_and_update(List<Path>paths , AtomicInteger in_flight, Window owner, Aborter browser_aborter, Logger logger)
    //**********************************************************
    {
        reload_cache_from_disk(in_flight,browser_aborter);
        logger.log("read_from_disk "+path_to_feature_vector_cache.size()+" fv reloaded from disk");
        return update( paths, in_flight, owner, browser_aborter, logger);
    }

    //**********************************************************
    private Paths_and_feature_vectors update(
            List<Path> paths,
            AtomicInteger in_flight, Window owner, Aborter browser_aborter,Logger logger)
    //**********************************************************
    {
        if ( ultra_dbg) logger.log("update "+paths+" fv to be rebuild");

        Feature_vector_source_server.start  = System.nanoTime();
        List<Path> missing_paths = new ArrayList<>();
        for (Path p : paths)
        {
            //if ( !Guess_file_type.is_file_an_image(p.toFile())) continue;
            if ( !path_to_feature_vector_cache.containsKey(p.getFileName().toString()))
            {
                if ( ultra_dbg) logger.log("missing FV for :"+p);
                missing_paths.add(p);
            }
        }
        in_flight.addAndGet(missing_paths.size()-1); //-1 to compensate the +1 "keep alive" in preload_all_feature_vector_in_cache
        CountDownLatch cdl = new CountDownLatch(missing_paths.size());
        Job_termination_reporter tr = (message, job) -> {
            in_flight.decrementAndGet();
            cdl.countDown();
        };
        for (Path p :missing_paths)
        {
            get_from_cache_or_make(p,tr,false, owner, browser_aborter);
        }
        try {
            cdl.await();
        } catch (InterruptedException e) {
            logger.log("Paths_and_feature_vectors from_disk interrupted:"+e);
        }

        Feature_vector_source_server.print_embeddings_stats(logger);
        if (!missing_paths.isEmpty())
        {
            logger.log(("update: "+missing_paths.size()+" new items added"));
            save_whole_cache_to_disk();
        }
        return new Paths_and_feature_vectors(this, paths);
    }


}
