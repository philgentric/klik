// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.machine_learning.similarity;
//SOURCES ./Similarity_cache_warmer_actor.java
//SOURCES ./Similarity_cache_warmer_message.java

import javafx.stage.Window;
import klik.browser.Clearable_RAM_cache;
import klik.util.execute.actor.Aborter;
import klik.util.execute.actor.Actor_engine;
import klik.util.execute.actor.Job_termination_reporter;
import klik.util.execute.actor.Or_aborter;
import klik.path_lists.Path_list_provider;
import klik.machine_learning.feature_vector.Feature_vector_cache;
import klik.machine_learning.feature_vector.Feature_vector_source;
import klik.properties.Cache_folder;
import klik.properties.Non_booleans_properties;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import klik.util.ui.progress.Progress_window;

import java.io.*;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

//**********************************************************
public class Similarity_cache implements Clearable_RAM_cache
//**********************************************************
{
    private final Path_list_provider path_list_provider;
    private final Path similarity_cache_file_path;
    private final Logger logger;
    private final ConcurrentHashMap<Path_pair, Double> similarities = new ConcurrentHashMap<>();

    //**********************************************************
    public Similarity_cache(
            Feature_vector_source fvs,
            List<Path> paths,
            Path_list_provider path_list_provider,
            Window owner, double x, double y,
            Aborter browser_aborter, Logger logger)
    //**********************************************************
    {
        this.path_list_provider = path_list_provider;
        this.logger = logger;
        String cache_name = "similarity";
        String local = cache_name + path_list_provider.get_folder_path();
        String cache_file_name = UUID.nameUUIDFromBytes(local.getBytes()) + ".similarity_cache";
        Path dir = Non_booleans_properties.get_absolute_hidden_dir_on_user_home(Cache_folder.klik_similarity_cache.name(), false, owner,logger);
        if (dir != null)
        {
            logger.log("similarity cache folder=" + dir.toAbsolutePath());
        }
        similarity_cache_file_path = Path.of(dir.toAbsolutePath().toString(), cache_file_name);

        if ( !reload_similarity_cache_from_disk(browser_aborter))
        {
            // no cache on disk, have to recalculate and save
            // in a thread!

            Feature_vector_cache.Paths_and_feature_vectors result = Feature_vector_cache.preload_all_feature_vector_in_cache(fvs,paths,path_list_provider, owner, x, y, browser_aborter, logger);
            if (result == null)
            {
                logger.log(Stack_trace_getter.get_stack_trace("ERROR: cannot preload all feature vectors"));
                return;
            }
            Feature_vector_cache fv_cache = result.fv_cache();
            fill_cache_and_save_to_disk(result.paths(),fv_cache, owner, x,y,browser_aborter, logger);

            //logger.log("similarities min_similarity="+Similarity_cache_warmer_actor.min_similarity+" max_similarity="+Similarity_cache_warmer_actor.max_similarity);
        }
    }

    //**********************************************************
    private void fill_cache_and_save_to_disk(List<Path> paths, Feature_vector_cache fv_cache, Window owner, double x, double y, Aborter browser_aborter, Logger logger)
    //**********************************************************
    {
        AtomicInteger in_flight = new AtomicInteger(paths.size());
        Progress_window progress_window = Progress_window.show(
                in_flight,
                "Wait: computing item similarities",
                3600*60,
                x,
                y,
                owner,
                logger);
        Aborter local = new Or_aborter(progress_window.aborter,browser_aborter,logger);
        Similarity_cache_warmer_actor actor = new Similarity_cache_warmer_actor(paths, fv_cache, similarities, logger);
        CountDownLatch cdl = new CountDownLatch(paths.size());
        for (Path p1 : paths)
        {
            if ( local.should_abort())
            {
                logger.log("aborting Similarity_cache "+ local.reason());
                break;
            }
            Similarity_cache_warmer_message m = new Similarity_cache_warmer_message(owner,local, p1);
            Job_termination_reporter tr = (message, job) -> {
                cdl.countDown();
                in_flight.decrementAndGet();
                if (cdl.getCount() % 100 == 0)
                    logger.log(" Remaining to fill similarity cache: " + cdl.getCount());
            };
            Actor_engine.run(actor, m, tr, logger);
        }

        try {
            cdl.await();
        } catch (InterruptedException e) {
            progress_window.close();
            logger.log("similarity cache interrupted" + e);
        }
        save_similarity_cache_to_disk();
        progress_window.close();
    }

    //**********************************************************
    public boolean reload_similarity_cache_from_disk(Aborter local)
    //**********************************************************
    {
        logger.log("reloading similarities from disk");

        int reloaded = 0;
        try(DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(similarity_cache_file_path.toFile()))))
        {
            int number_of_items = dis.readInt();
            for ( int k = 0; k < number_of_items; k++)
            {
                if ( local.should_abort())
                {
                    logger.log("aborting : Similarity_cache::reload_similarity_cache_from_disk "+local.reason());
                    return false;
                }
                String path1_string = dis.readUTF();
                String path2_string = dis.readUTF();
                double val = dis.readDouble();
                Path p1 = path_list_provider.resolve(path1_string);
                Path p2 = path_list_provider.resolve(path2_string);
                Path_pair p = Path_pair.get(p1,p2);
                similarities.put(p,val);
                if ( k%10000 == 0) logger.log("wait: already "+k +" similarities loaded from disk ....");
                reloaded++;
            }
            logger.log("Done: "+reloaded+" similarities reloaded from disk");
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
                //logger.log("to disk similarity "+e.getValue()+" for "+pi1.getFileName().toString()+" "+pi2.getFileName().toString());
            }
        }
        catch (IOException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
        }

        //if (dbg)
        logger.log(saved +" similarities from cache saved to file");
    }

    //**********************************************************
    @Override
    public void clear_RAM_cache()
    //**********************************************************
    {
        similarities.clear();
    }

    //**********************************************************
    public Double get(Path_pair pathPair)
    //**********************************************************
    {
        return similarities.get(pathPair);
    }
}
