// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.machine_learning.feature_vector;

//SOURCES ./Feature_vector_creation_actor.java
//SOURCES ./Feature_vector_message.java
//SOURCES ../../browser/Shared_services.java



import klikr.machine_learning.python_via_html_ML.Feature_vector_source_server;
import klikr.util.Kontext;
import klikr.util.cache.*;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.execute.actor.Job_termination_reporter;
import klikr.path_lists.Path_list_provider;
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
import java.util.concurrent.atomic.LongAdder;

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
    public static long start = System.nanoTime();
    static LongAdder tx_count = new LongAdder();
    static LongAdder SUM_dur_us = new LongAdder(); // microseconds


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
    public static Path get_feature_vector_cache_dir(Kontext context)
    //**********************************************************
    {

        Path tmp_dir = Static_files_and_paths_utilities.get_absolute_hidden_dir_on_user_home(Cache_folder.feature_vectors_cache.name(), false,context);
        if (dbg) if (tmp_dir != null) {
            context.log("Feature vector cache folder=" + tmp_dir.toAbsolutePath());
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
            Kontext context)
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


        if ( context.should_abort())
        {
            logger.log(("feature vector cache instance#"+instance_number+" request aborted: ->"+context.aborter().name+"<- reason="+context.abort_reason()+ " target path="+p));
            if (tr != null) tr.has_ended("aborted", null);
            return null;
        }

        if ( dbg) logger.log("going to make feature_vector for "+p);

        Feature_vector_build_message imp = new Feature_vector_build_message(p,this,context);
        if ( wait_if_needed)
        {
            long local_start = System.nanoTime();
            //logger.log("blocking FV creation call for "+p);
            feature_vector_creation_actor.run(imp); // blocking call
            if (tr != null) tr.has_ended("done after blocking", null);
            //Feature_vector x = the_cache.get(key_from_path(p));
            byte[] the_bytes = Mmap.instance.read_bytes(key_from_path(p));
            if ( the_bytes == null)
            {
                logger.log(Logger.error+"PANIC null Feature_vector in cache after blocking call for "+p);
                return null;
            }
            long local_end = System.nanoTime();
            long local_dur_us = (local_end - local_start)/1000;
            SUM_dur_us.add(local_dur_us);
            tx_count.add(1);
            return new Feature_vector_double(bytes_to_doubles(the_bytes),"made");
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
            logger.log(Logger.error+"BAD WARNING... cannot cache but Feature_vector_double");
        }
    }

    //**********************************************************
    public List<Path> get_missing_files_not_in_mmap(List<Path> paths, AtomicInteger in_flight)
    //**********************************************************
    {
        List<Path> missing = new ArrayList<>();
        //logger.log((tag +" reload_cache_from_disk"));

        in_flight.set(paths.size());

        for ( Path p : paths)
        {
            String key = key_from_path(p);
            byte[] bytes= Mmap.instance.read_bytes(key);
            if ( bytes == null)
            {
                missing.add(p);
            }
        }
        return missing;
    }

    //**********************************************************
    public static Feature_vector_cache preload_all_feature_vector_in_cache(
            Feature_vector_source fvs,
            List<Path> paths,
            Path_list_provider path_list_provider,
            Kontext context)
    //**********************************************************
    {
        try( Perf p = new Perf("preload_all_feature_vector_in_cache"))
        {
            context.log("\n\n"+ Logger.ok+" Going to preload_all_feature_vector_in_cache\n\n");
            Feature_vector_cache feature_vector_cache = RAM_caches.fv_cache_of_caches.get(path_list_provider.get_key());
            AtomicInteger in_flight = new AtomicInteger(1); // '1' to keep it alive until update settles the final count

            if ( feature_vector_cache == null)
            {
                Optional<Hourglass> hourglass = Progress_window.show_with_in_flight(
                        in_flight,
                        "Wait, making feature vectors",
                        3600*60,
                        context);


                feature_vector_cache = new Feature_vector_cache(path_list_provider.get_key(), fvs, context.logger());
                feature_vector_cache.read_from_disk_and_update(paths,in_flight, context);
                RAM_caches.fv_cache_of_caches.put(path_list_provider.get_key(),feature_vector_cache);
                hourglass.ifPresent(Hourglass::close);
                return feature_vector_cache;
            }
            feature_vector_cache.update(paths, in_flight,context);
            return feature_vector_cache;
        }
    }

    //**********************************************************
    private void read_from_disk_and_update(List<Path>paths , AtomicInteger in_flight, Kontext context)
    //**********************************************************
    {
        List<Path> missing = get_missing_files_not_in_mmap(paths, in_flight);
        update( missing, in_flight, context);
    }

    //**********************************************************
    private void update(List<Path> missing_paths,
            AtomicInteger in_flight, Kontext context)
    //**********************************************************
    {
        if ( ultra_dbg) logger.log("update: "+missing_paths.size()+" missing fv to be rebuild");

        start  = System.nanoTime();


        in_flight.addAndGet(missing_paths.size()-1); //-1 to compensate the +1 "keep alive" in preload_all_feature_vector_in_cache
        CountDownLatch cdl = new CountDownLatch(missing_paths.size());
        Job_termination_reporter tr = (message, job) -> {
            in_flight.decrementAndGet();
            cdl.countDown();
        };
        for (Path p :missing_paths)
        {
            if ( context.should_abort())
            {
                while ( cdl.getCount() > 0 ) cdl.countDown();
                break;
            }
            get_from_cache_or_make(p,tr,false, context);
        }
        try {
            cdl.await();
        } catch (InterruptedException e) {
            logger.log("Paths_and_feature_vectors from_disk interrupted:"+e);
        }

        Mmap.instance.save_index();
        print_embeddings_stats(logger);

    }

    //**********************************************************
    public static void print_embeddings_stats(Logger logger)
    //**********************************************************
    {
        long end = System.nanoTime();
        double dur_us = (double)(end - start)/1_000.0;
        logger.log("feature vector TX_rate="+1_000_000*tx_count.doubleValue()/(double)dur_us+" tx/s (tx_count="+tx_count+" for: "+dur_us/1_000_000+" seconds)");
        logger.log("total server call time="+ SUM_dur_us.doubleValue()/1_000_000 +"s, average concurrency="+SUM_dur_us.doubleValue() /dur_us);
    }

}
