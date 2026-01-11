// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.machine_learning.similarity;
//SOURCES ./Similarity_cache_warmer_actor.java
//SOURCES ./Similarity_cache_warmer_message.java

import javafx.stage.Window;
import klikr.browser.Clearable_RAM_cache;
import klikr.browser.virtual_landscape.Browsing_caches;
import klikr.machine_learning.feature_vector.Feature_vector;
import klikr.path_lists.Path_list_provider_for_file_system;
import klikr.properties.boolean_features.Feature;
import klikr.properties.boolean_features.Feature_cache;
import klikr.util.animated_gifs.Ffmpeg_utils;
import klikr.util.cache.RAM_cache;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.execute.actor.Job_termination_reporter;
import klikr.util.execute.actor.Or_aborter;
import klikr.path_lists.Path_list_provider;
import klikr.machine_learning.feature_vector.Feature_vector_cache;
import klikr.machine_learning.feature_vector.Feature_vector_source;
import klikr.properties.Cache_folder;
import klikr.properties.Non_booleans_properties;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;
import klikr.util.ui.progress.Progress_window;

import java.io.*;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.function.Function;

//**********************************************************
public class Similarity_cache implements Clearable_RAM_cache
//**********************************************************
{
    private final Path_list_provider path_list_provider;
    private final Path similarity_cache_file_path;
    private final Window owner;
    private final Aborter aborter;
    private final Logger logger;
    //private final ConcurrentHashMap<Path_pair, Double> similarities = new ConcurrentHashMap<>();
    private RAM_cache<Path_pair, Double> similarities;

    //**********************************************************
    public Similarity_cache(
            Feature_vector_source fvs,
            List<Path> paths,
            Path_list_provider path_list_provider,
            Window owner, double x, double y,
            Aborter aborter, Logger logger)
    //**********************************************************
    {
        this.path_list_provider = path_list_provider;
        this.owner = owner;
        this.logger = logger;
        this.aborter = aborter;
        String cache_name = "similarity";
        String local = cache_name + path_list_provider.get_folder_path();
        String cache_file_name = UUID.nameUUIDFromBytes(local.getBytes()) + ".similarity_cache";
        Path dir = Non_booleans_properties.get_absolute_hidden_dir_on_user_home(Cache_folder.similarity_cache.name(), false, owner,logger);
        if (dir != null)
        {
            logger.log("similarity cache folder=" + dir.toAbsolutePath());
        }
        similarity_cache_file_path = Path.of(dir.toAbsolutePath().toString(), cache_file_name);

        Feature_vector_cache fv_cache = Browsing_caches.fv_cache_of_caches.get(path_list_provider.get_name());
        if ( fv_cache == null) {
            Feature_vector_cache.Paths_and_feature_vectors result = Feature_vector_cache.preload_all_feature_vector_in_cache(fvs, paths, path_list_provider, owner, x, y, aborter, logger);

            if (result == null) {
                logger.log(Stack_trace_getter.get_stack_trace("ERROR: cannot preload all feature vectors"));
                return;
            }
            fv_cache = result.fv_cache();
        }
        make_similarity_cache(dir,fv_cache);
        // prefill the matrix with all 'close pairs'

        fill_cache_and_save_to_disk(
                path_list_provider.only_image_paths(Feature_cache.get(Feature.Show_hidden_files)),
                fv_cache,
                x,y);
    }


    //**********************************************************
    @Override
    public void clear_RAM()
    //**********************************************************
    {
        similarities.clear_RAM();
    }

    //**********************************************************
    private void make_similarity_cache(Path folder_path, Feature_vector_cache fv_cache)
    //**********************************************************
    {
        BiPredicate<Path_pair, DataOutputStream> key_serializer= new BiPredicate<Path_pair, DataOutputStream>() {
            @Override
            public boolean test(Path_pair path_pair, DataOutputStream dos)
            {
                try {
                    String si = path_pair.i().toAbsolutePath().normalize().toString();
                    dos.writeUTF(si);
                    String sj = path_pair.j().toAbsolutePath().normalize().toString();
                    dos.writeUTF(sj);
                    return true;
                } catch (IOException e) {
                    logger.log(""+e);
                }
                return false;
            }
        };

        Function<DataInputStream, Path_pair> key_deserializer = new Function<DataInputStream, Path_pair>() {
            @Override
            public Path_pair apply(DataInputStream dis)
            {
                try {
                    String si = dis.readUTF();
                    Path pi = Path.of(si);
                    String sj = dis.readUTF();
                    Path pj = Path.of(sj);
                    return Path_pair.build(pi,pj);
                } catch (IOException e) {
                    logger.log(""+e);
                }

                return null;
            }
        };

        BiPredicate<Double, DataOutputStream> value_serializer = new BiPredicate<Double, DataOutputStream>() {
            @Override
            public boolean test(Double d, DataOutputStream dos) {
                try {
                    dos.writeDouble(d);
                    return true;
                } catch (IOException e) {
                    logger.log(""+e);
                }
                return false;
            }
        };
        Function<DataInputStream, Double> value_deserializer = new Function<DataInputStream, Double>() {
            @Override
            public Double apply(DataInputStream dis) {
                try {
                    return dis.readDouble();
                } catch (IOException e) {
                    logger.log(""+e);
                }
                return null;
            }
        };


        Function<Path_pair,String> string_key_maker = new Function<Path_pair, String>() {
            @Override
            public String apply(Path_pair path_pair) {return path_pair.to_string_key();}
        };
        Function<String,Path_pair> object_key_maker = new Function<String, Path_pair>() {
            @Override
            public Path_pair apply(String s) {return Path_pair.from_string_key(s);}
        };

        Function<Path_pair, Double> value_extractor = new Function<Path_pair, Double>() {
            @Override
            public Double apply(Path_pair path_pair)
            {
                Feature_vector fvi = fv_cache.get_from_cache_or_make(path_pair.i(),null,true,owner,aborter);
                if ( fvi == null)
                {
                    fvi = fv_cache.get_from_cache_or_make(path_pair.i(),null,true,owner,aborter);
                    if ( fvi == null)
                    {
                        logger.log(" fv == null for "+path_pair.i());
                        return null;
                    }
                }
                Feature_vector fvj = fv_cache.get_from_cache_or_make(path_pair.j(),null,true,owner,aborter);
                if ( fvj == null)
                {
                    fvj = fv_cache.get_from_cache_or_make(path_pair.j(),null,true,owner,aborter);
                    if ( fvj == null)
                    {
                        logger.log(" fv == null for "+path_pair.j());
                        return null;
                    }
                }
                return  fvi.distance(fvj);
            }
        };

        similarities = new RAM_cache<Path_pair, Double>(
                new Path_list_provider_for_file_system(folder_path, owner, logger),
                Cache_folder.song_duration_cache.name(),
                key_serializer, key_deserializer,
                value_serializer, value_deserializer,
                value_extractor,
                string_key_maker,object_key_maker,
                aborter, owner, logger
        );

    }

    public Double get(Path_pair path_pair)
    {
        return similarities.get(path_pair,aborter,null,owner);
    }


    //**********************************************************
    private void fill_cache_and_save_to_disk(
            List<Path> paths,
            Feature_vector_cache fv_cache,
            double x, double y)
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
        Aborter local = new Or_aborter(progress_window.aborter,aborter,logger);
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
        similarities.save_whole_cache_to_disk();
        progress_window.close();
    }
/*
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
                Path_pair p = Path_pair.build(p1,p2);
                similarities.put(p.to_String(),val);
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
    public Double get(Path_pair pathPair)
    //**********************************************************
    {
        return similarities.get(pathPair);
    }*/
}
