// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.machine_learning.feature_vector;

//SOURCES ./Feature_vector_creation_actor.java
//SOURCES ./Feature_vector_message.java
//SOURCES ../../browser/Shared_services.java



import javafx.stage.Stage;
import javafx.stage.Window;
import klikr.util.cache.*;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.execute.actor.Job_termination_reporter;
import klikr.util.execute.actor.Or_aborter;
import klikr.path_lists.Path_list_provider;
import klikr.machine_learning.song_similarity.Feature_vector_for_song;
import klikr.util.files_and_paths.Static_files_and_paths_utilities;
import klikr.util.log.Logger;
import klikr.util.mmap.Mmap;
import klikr.util.perf.Perf;
import klikr.util.ui.progress.Hourglass;
import klikr.util.ui.progress.Progress_window;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

//**********************************************************
public class Feature_vector_cache implements Clearable_RAM_cache
//**********************************************************
{
    private final static boolean ultra_dbg = false;
    public final static boolean dbg = false;
    protected final Logger logger;
    protected final String tag;

    // classic RAM cache
    //private final Map<String, Feature_vector> the_cache = new ConcurrentHashMap<>();


    private final Feature_vector_creation_actor feature_vector_creation_actor;

    private final int instance_number;
    private static AtomicInteger instance_number_generator = new AtomicInteger(0);


    //**********************************************************
    private static byte[] doubles_to_bytes(double[] doubles)
    //**********************************************************
    {
        ByteBuffer buffer = ByteBuffer.allocate(doubles.length * 8);
        buffer.asDoubleBuffer().put(doubles);
        return buffer.array();
    }

    //**********************************************************
    private static double[] bytes_to_doubles(byte[] bytes)
    //**********************************************************
    {
        DoubleBuffer doubleBuffer = ByteBuffer.wrap(bytes).asDoubleBuffer();
        double[] doubles = new double[bytes.length / 8];
        doubleBuffer.get(doubles);
        return doubles;
    }

    //**********************************************************
    public Feature_vector_cache(
            String tag,
            Feature_vector_source fvs,
            Logger logger_)
    //**********************************************************
    {
        instance_number = instance_number_generator.getAndIncrement();
        logger = logger_;
        this.tag = tag;
        feature_vector_creation_actor = new Feature_vector_creation_actor(fvs);
    }


    //**********************************************************
    @Override
    public double clear_RAM()
    //**********************************************************
    {
        double returned = 0;
        /*
        for (Feature_vector fv : the_cache.values())
        {
            returned += fv.size();
        }
        the_cache.clear();
        */
        //mmap_index.clear();
        if (dbg) logger.log("feature vector cache file cleared");
        return returned;
    }


    //**********************************************************
    public static Path get_feature_vector_cache_dir(Stage owner, Logger logger)
    //**********************************************************
    {

        Path tmp_dir = Static_files_and_paths_utilities.get_absolute_hidden_dir_on_user_home(Cache_folder.feature_vectors_cache.name(), false,owner,logger);
        if (dbg) if (tmp_dir != null) {
            logger.log("Feature vector cache folder=" + tmp_dir.toAbsolutePath());
        }
        return tmp_dir;
    }

    //**********************************************************
    // if wait_if_needed is true, tr can be null
    // if tr is not null it must be safe to execute for emergency abort
    public Feature_vector get_from_cache_or_make(
            Path p,
            Job_termination_reporter tr,
            boolean wait_if_needed,
            Window owner,
            Aborter aborter)
    //**********************************************************
    {
        String key = key_from_path(p);
        /*
        // in RAM?
        Feature_vector feature_vector =  the_cache.get(key);
        if ( feature_vector != null)
        {
            if ( dbg)
                logger.log("feature_vector found in RAM cache for "+p);
            if ( tr != null) tr.has_ended("found in cache",null);
            return feature_vector;
        }
        if ( browser_aborter.should_abort())
        {
            logger.log(("feature vector cache instance#"+instance_number+" request aborted: ->"+browser_aborter.name+"<- reason="+browser_aborter.reason()+ " target path="+p));
            if (tr != null) tr.has_ended("aborted", null);
            return null;
        }
        */
        // look in mmap
        byte[] bytes = Mmap.instance.read_bytes(key);
        if (bytes != null) {
            Feature_vector feature_vector = new Feature_vector_double(bytes_to_doubles(bytes),"from_mmap_cache");
            //the_cache.put(key, feature_vector);  // Promote to RAM cache
            if (dbg) logger.log("feature_vector loaded from mmap for " + p);
            if (tr != null) tr.has_ended("loaded from mmap", null);
            return feature_vector;
        }


        if ( aborter.should_abort())
        {
            logger.log(("feature vector cache instance#"+instance_number+" request aborted: ->"+aborter.name+"<- reason="+aborter.reason()+ " target path="+p));
            if (tr != null) tr.has_ended("aborted", null);
            return null;
        }

        if ( dbg) logger.log("going to make feature_vector for "+p);

        Feature_vector_build_message imp = new Feature_vector_build_message(p,this,owner,aborter,logger);
        if ( wait_if_needed)
        {
            //logger.log("blocking FV creation call for "+p);
            feature_vector_creation_actor.run(imp); // blocking call
            if (tr != null) tr.has_ended("done after blocking", null);
            //Feature_vector x = the_cache.get(key_from_path(p));
            byte[] the_bytes = Mmap.instance.read_bytes(key_from_path(p));
            if ( the_bytes == null)
            {
                logger.log("❌ PANIC null Feature_vector in cache after blocking call for "+p);
                return null;
            }
            Feature_vector x = new Feature_vector_double(bytes_to_doubles(the_bytes),"made");
            return x;
        }
        Actor_engine.run(feature_vector_creation_actor,imp,tr,logger);
        return null;
    }

    //**********************************************************
    private static String key_from_path(Path p)
    //**********************************************************
    {
        return "FV_"+p.toAbsolutePath();
    }
    //**********************************************************
    public void inject(Path path, Feature_vector fv)
    //**********************************************************
    {
        if(dbg) logger.log(tag +" inject "+path);//+" value="+fv.to_string()+" components");

        String key = key_from_path(path);
        //the_cache.put(key, fv);

        if (fv instanceof Feature_vector_double fvd)
        {
            byte[] bytes = doubles_to_bytes(fvd.features);
            Mmap.instance.write_bytes(key, bytes, true);
        }
        else
        {
            logger.log("❌ BAD WARNING... cannot cache but Feature_vector_double");
        }
    }

    //**********************************************************
    public List<Path> reload_cache_from_disk(List<Path> paths, AtomicInteger in_flight, Aborter browser_aborter)
    //**********************************************************
    {
        List<Path> missing = new ArrayList<>();
        logger.log((tag +" reload_cache_from_disk"));

        int reloaded = 0;
        in_flight.set(paths.size());

        for ( Path p : paths)
        {
            String key = key_from_path(p);
            byte[] bytes= Mmap.instance.read_bytes(key);
            if ( bytes == null)
            {
                logger.log("no feature vector in mmap for "+key);
                missing.add(p);
                continue;
            }
            /*
            double[] vector = bytes_to_doubles(bytes);
            Feature_vector_double fv = new Feature_vector_double(vector);

            the_cache.put(key, fv);
            in_flight.decrementAndGet();
            reloaded++;
            if ( reloaded%1000==0)
                logger.log(reloaded+" feature vectors loaded from disk");
            */
        }

/*
        //if (dbg)
            logger.log((tag +": "+reloaded+" feature vectors reloaded from file"));

        if ( dbg)
        {
            logger.log("\n\n\n********************* "+ tag + " CACHE************************");
            for (String s  : the_cache.keySet())
            {
                logger.log(s+" => "+ the_cache.get(s));
                logger.log(s+" => "+ new Feature_vector_double(bytes_to_doubles(Mmap.instance.read_bytes(s))));
            }
            logger.log("****************************************************************\n\n\n");
        }

 */
        return missing;
    }

    //**********************************************************
    public static Feature_vector_cache preload_all_feature_vector_in_cache(
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
            logger.log("\n\n✅ Going to preload_all_feature_vector_in_cache\n\n");
            Feature_vector_cache feature_vector_cache = RAM_caches.fv_cache_of_caches.get(path_list_provider.get_key());
            AtomicInteger in_flight = new AtomicInteger(1); // '1' to keep it alive until update settles the final count

            if ( feature_vector_cache == null)
            {
                Optional<Hourglass> hourglass = Progress_window.show_with_in_flight(
                        browser_aborter,
                        in_flight,
                        "Wait, making feature vectors",
                        3600*60,
                        x,
                        y,
                        owner,
                        logger);


                feature_vector_cache = new Feature_vector_cache(path_list_provider.get_key(), fvs,logger);
                feature_vector_cache.read_from_disk_and_update(paths,in_flight, owner, browser_aborter,logger);
                RAM_caches.fv_cache_of_caches.put(path_list_provider.get_key(),feature_vector_cache);
                hourglass.ifPresent(Hourglass::close);
                return feature_vector_cache;
            }
            feature_vector_cache.update(paths, in_flight,owner, browser_aborter,logger);
            return feature_vector_cache;
        }
    }

    //**********************************************************
    private void read_from_disk_and_update(List<Path>paths , AtomicInteger in_flight, Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        List<Path> missing = reload_cache_from_disk(paths, in_flight,aborter);
        //logger.log("read_from_disk "+ the_cache.size()+" fv reloaded from disk");
        update( missing, in_flight, owner, aborter, logger);
    }

    //**********************************************************
    private void update(List<Path> missing_paths,
            AtomicInteger in_flight, Window owner, Aborter aborter,Logger logger)
    //**********************************************************
    {
        if ( ultra_dbg) logger.log("update: "+missing_paths.size()+" missing fv to be rebuild");

        Feature_vector_source_server.start  = System.nanoTime();
        /*List<Path> missing_paths = new ArrayList<>();
        for (Path p : missing)
        {
            //if ( !Guess_file_type.is_file_an_image(p.toFile())) continue;
            if ( !the_cache.containsKey(p.getFileName().toString()))
            {
                if ( ultra_dbg) logger.log("missing FV for :"+p);
                missing_paths.add(p);
            }
        }
        */

        in_flight.addAndGet(missing_paths.size()-1); //-1 to compensate the +1 "keep alive" in preload_all_feature_vector_in_cache
        CountDownLatch cdl = new CountDownLatch(missing_paths.size());
        Job_termination_reporter tr = (message, job) -> {
            in_flight.decrementAndGet();
            cdl.countDown();
        };
        for (Path p :missing_paths)
        {
            if ( aborter.should_abort())
            {
                while ( cdl.getCount() > 0 ) cdl.countDown();
                break;
            }
            get_from_cache_or_make(p,tr,false, owner, aborter);
        }
        try {
            cdl.await();
        } catch (InterruptedException e) {
            logger.log("Paths_and_feature_vectors from_disk interrupted:"+e);
        }

        Mmap.instance.save_index();
        Feature_vector_source_server.print_embeddings_stats(logger);

    }


}
