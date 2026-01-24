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
import klikr.util.log.Stack_trace_getter;
import klikr.util.perf.Perf;
import klikr.util.ui.progress.Hourglass;
import klikr.util.ui.progress.Progress_window;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;


//**********************************************************
public class Feature_vector_cache implements Clearable_RAM_cache, Clearable_disk_cache
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
    private final Map<String, Feature_vector> the_cache = new ConcurrentHashMap<>();
    private final Feature_vector_creation_actor feature_vector_creation_actor;

    private final int instance_number;
    private static int instance_number_generator = 0;

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
    }


    //**********************************************************
    @Override
    public double clear_RAM()
    //**********************************************************
    {
        double returned = 0;
        for (Feature_vector fv : the_cache.values())
        {
            returned += fv.size();
        }
        the_cache.clear();
        if (dbg) logger.log("feature vector cache file cleared");
        return returned;
    }

    //**********************************************************
    @Override
    public Disk_cleared clear_disk(Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        try {
            long size = Files.size(cache_file_path);
            Files.delete(cache_file_path);
            return new Disk_cleared(cache_file_path, size);
        } catch (IOException e) {
            logger.log(""+e);
        }
        return null;
    }

    //**********************************************************
    @Override
    public String name()
    //**********************************************************
    {
        return "Feature vector cache for: "+tag;
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
    public Feature_vector get_from_cache_or_make(Path p, Job_termination_reporter tr, boolean wait_if_needed, Window owner, Aborter browser_aborter)
    //**********************************************************
    {
        Feature_vector feature_vector =  the_cache.get(key_from_path(p));
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
            Feature_vector x = the_cache.get(key_from_path(p));
            if ( x == null) logger.log("❌ PANIC null Feature_vector in cache after blocking call ");
            return x;
        }
        Actor_engine.run(feature_vector_creation_actor,imp,tr,logger);
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
        the_cache.put(key_from_path(path), fv);
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
                the_cache.put(path_string, fv);
                in_flight.decrementAndGet();
                reloaded++;
                if ( i%1000==0) logger.log(i+" feature vectors loaded from disk");
            }
        }
        catch (FileNotFoundException e)
        {
            if (dbg) logger.log("first time in this folder: "+e);
        }
        catch (EOFException e)
        {
            if (dbg) logger.log("empty feature cache: "+e);
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
            for (String s  : the_cache.keySet())
            {
                logger.log(s+" => "+ the_cache.get(s));
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
            if ( the_cache.size() == 0)
            {
                logger.log("feature vector cache empty, nothing to save");
                return;
            }
            Map.Entry<String, Feature_vector> e0 = the_cache.entrySet().iterator().next();
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
            dos.writeInt(the_cache.size());
            for(Map.Entry<String, Feature_vector> e : the_cache.entrySet())
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
        Feature_vector_cache feature_vector_cache = Clearable_shared_caches.fv_cache_of_caches.get(path_list_provider.get_name());
        AtomicInteger in_flight = new AtomicInteger(1); // '1' to keep it alive until update settles the final count

        if ( feature_vector_cache == null)
        {
            Optional<Hourglass> hourglass = Progress_window.show(
                    in_flight,
                    "Wait, making feature vectors",
                    3600*60,
                    x,
                    y,
                    owner,
                    logger);

            Or_aborter or_aborter = new Or_aborter(browser_aborter,Progress_window.get_aborter(hourglass, logger),logger);
            feature_vector_cache = new Feature_vector_cache(path_list_provider.get_name(), fvs, or_aborter,logger);
            Paths_and_feature_vectors paths_and_feature_vectors = feature_vector_cache.read_from_disk_and_update(paths,in_flight, owner, or_aborter,logger);
            Clearable_shared_caches.fv_cache_of_caches.put(path_list_provider.get_name(),feature_vector_cache);
            hourglass.ifPresent(Hourglass::close);
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
        logger.log("read_from_disk "+ the_cache.size()+" fv reloaded from disk");
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
            if ( !the_cache.containsKey(p.getFileName().toString()))
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
            logger.log(("feature vector cache update: "+missing_paths.size()+" new items added"));
            save_whole_cache_to_disk();
        }
        return new Paths_and_feature_vectors(this, paths);
    }


}
