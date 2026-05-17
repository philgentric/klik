// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.machine_learning.similarity;
//SOURCES ./Similarity_cache_warmer_actor.java
//SOURCES ./Similarity_cache_warmer_message.java

import javafx.stage.Window;
import klikr.util.Check_remaining_RAM;
import klikr.util.cache.*;
import klikr.machine_learning.feature_vector.Feature_vector;
import klikr.path_lists.Path_list_provider_for_file_system;
import klikr.settings.boolean_features.Feature;
import klikr.settings.boolean_features.Feature_cache;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.execute.actor.Job_termination_reporter;
import klikr.path_lists.Path_list_provider;
import klikr.machine_learning.feature_vector.Feature_vector_cache;
import klikr.machine_learning.feature_vector.Feature_vector_source;
import klikr.util.files_and_paths.Static_files_and_paths_utilities;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;
import klikr.util.mmap.Mmap;
import klikr.util.ui.progress.Hourglass;
import klikr.util.ui.progress.Progress_window;

import java.io.*;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.function.Function;

//**********************************************************
public class Similarity_cache implements Clearable_RAM_cache
//**********************************************************
{
    private final Path folder_path;
    private final Window owner;
    private final Aborter aborter;
    private final Logger logger;
    private final List<Path> paths;
    private Klikr_cache<Integer_pair, Double> similarities;
    //private final String name;

    //**********************************************************
    public Similarity_cache(
            Feature_vector_source fvs,
            //List<Path> paths,
            Path_list_provider path_list_provider,
            Window owner, double x, double y,
            Aborter aborter, Logger logger)
    //**********************************************************
    {
        //this.path_list_provider = path_list_provider;
        this.owner = owner;
        this.logger = logger;
        this.aborter = aborter;
        String cache_name = "similarity";
        String local = cache_name + path_list_provider.get_folder_path();
        //name = UUID.nameUUIDFromBytes(local.getBytes()) + ".similarity_cache";
        folder_path = Static_files_and_paths_utilities.get_absolute_hidden_dir_on_user_home(Cache_folder.similarity_cache.name(), false, owner,logger);
        if (folder_path != null)
        {
            logger.log("similarity cache folder=" + folder_path.toAbsolutePath());
        }
        //similarity_cache_file_path = Path.of(folder_path.toAbsolutePath().toString(), name);

        paths = path_list_provider.only_image_paths(false, Feature_cache.get(Feature.Show_hidden_files),aborter);
        Feature_vector_cache fv_cache = RAM_caches.fv_cache_of_caches.get(path_list_provider.get_key());
        if ( fv_cache == null)
        {
            fv_cache = Feature_vector_cache.preload_all_feature_vector_in_cache(fvs, paths, path_list_provider, owner, x, y, aborter, logger);
        }
        make_similarity_cache(fv_cache);
        fill_cache_and_save_to_disk(paths, fv_cache, x,y);
    }


    //**********************************************************
    @Override
    public double clear_RAM()
    //**********************************************************
    {
        return similarities.clear_RAM();
    }

    //**********************************************************
    private void make_similarity_cache(Feature_vector_cache fv_cache)
    //**********************************************************
    {

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

        /*
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

        Function<Path_pair,String> internal_string_key_maker = new Function<Path_pair, String>() {
            @Override
            public String apply(Path_pair path_pair) {return path_pair.to_string_key();}
        };
        Function<String,Path_pair> path_pair_maker = new Function<String, Path_pair>() {
            @Override
            public Path_pair apply(String s) {return Path_pair.from_string_key(s);}
        };

        Function<Path_pair, Double> similarity_extractor = new Function<Path_pair, Double>() {
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

        similarities = new Klikr_cache<Path_pair, Double>(
                new Path_list_provider_for_file_system(folder_path, owner, logger),
                Cache_folder.similarity_cache.name(),
                key_serializer, key_deserializer,
                value_serializer, value_deserializer,
                similarity_extractor,
                internal_string_key_maker,path_pair_maker,
                Size_.of_Double_F(),
                aborter, owner, logger);
*/

        BiPredicate<Integer_pair, DataOutputStream> key_serializer= new BiPredicate<Integer_pair, DataOutputStream>() {
            @Override
            public boolean test(Integer_pair integer_pair, DataOutputStream dos)
            {
                try {
                    dos.writeInt(integer_pair.i());
                    dos.writeInt(integer_pair.j());
                    return true;
                } catch (IOException e) {
                    logger.log(""+e);
                }
                return false;
            }
        };

        Function<DataInputStream, Integer_pair> key_deserializer = new Function<DataInputStream, Integer_pair>() {
            @Override
            public Integer_pair apply(DataInputStream dis)
            {
                try {
                    int i = dis.readInt();
                    int j = dis.readInt();
                    return Integer_pair.build(i,j);
                } catch (IOException e) {
                    logger.log(""+e);
                }

                return null;
            }
        };

        Function<Integer_pair,String> internal_string_key_maker = new Function<Integer_pair, String>() {
            @Override
            public String apply(Integer_pair integer_pair) {return integer_pair.to_string_key();}
        };
        Function<String,Integer_pair> integer_pair_maker = new Function<String, Integer_pair>() {
            @Override
            public Integer_pair apply(String s) {return Integer_pair.from_string_key(s);}
        };

        Function<Integer_pair, Double> similarity_extractor = new Function<Integer_pair, Double>() {
            @Override
            public Double apply(Integer_pair integer_pair)
            {
                int i = integer_pair.i();
                if ( i >= paths.size())
                {
                    logger.log("❌ PANIC asking for:"+i+" but path size is "+paths.size());
                    return 0.0;
                }
                Path pi = paths.get(i);
                Feature_vector fvi = fv_cache.get_from_cache_or_make(pi,null,true,owner,aborter);
                if ( fvi == null)
                {
                    fvi = fv_cache.get_from_cache_or_make(pi,null,true,owner,aborter);
                    if ( fvi == null)
                    {
                        logger.log(" fv == null for "+pi);
                        return null;
                    }
                    logger.log(Stack_trace_getter.get_stack_trace("trying twice worked!!!!"));
                }
                int j = integer_pair.j();
                if ( j >= paths.size())
                {
                    logger.log("❌ PANIC asking for:"+j+" but path size is "+paths.size());
                    return 0.0;
                }
                Path pj = paths.get(j);

                Feature_vector fvj = fv_cache.get_from_cache_or_make(pj,null,true,owner,aborter);
                if ( fvj == null)
                {
                    fvj = fv_cache.get_from_cache_or_make(pj,null,true,owner,aborter);
                    if ( fvj == null)
                    {
                        logger.log(" fv == null for "+pj);
                        return null;
                    }
                    logger.log(Stack_trace_getter.get_stack_trace("trying twice worked!!!!"));
                }
                return  fvi.distance(fvj,logger);
            }
        };

        similarities = new Klikr_cache<Integer_pair, Double>(
                new Path_list_provider_for_file_system(folder_path, owner, logger),
                Cache_folder.similarity_cache.name(),
                key_serializer, key_deserializer,
                value_serializer, value_deserializer,
                similarity_extractor,
                internal_string_key_maker,integer_pair_maker,
                Size_.of_Double_F(),
                aborter, owner, logger);

    }

    //**********************************************************
    public Double get(Path p1, Path p2)
    //**********************************************************
    {
        int i = paths.indexOf(p1);
        if ( i < 0)
        {
            logger.log("❌ PANIC not in paths!"+p1);
            return 0.0;
        }
        int j = paths.indexOf(p2);
        if ( j < 0)
        {
            logger.log("❌ PANIC not in paths!"+p2);
            return 0.0;
        }
        Integer_pair integer_pair = Integer_pair.build(i,j);
        return similarities.get(integer_pair,aborter,null,owner);
    }


    //**********************************************************
    private void fill_cache_and_save_to_disk(
            List<Path> paths,
            Feature_vector_cache fv_cache,
            double x, double y)
    //**********************************************************
    {
        similarities.reload_cache_from_disk();
        AtomicInteger in_flight = new AtomicInteger(paths.size());
        Optional<Hourglass> hourglass = Progress_window.show_with_in_flight(
                aborter,
                in_flight,
                "Wait: computing item similarities",
                3600*60,
                x,
                y,
                owner,
                logger);
        Similarity_cache_warmer_actor actor = new Similarity_cache_warmer_actor(paths, fv_cache, similarities, logger);
        CountDownLatch cdl = new CountDownLatch(paths.size());
        for (int i = 0; i < paths.size(); i++)
        {
            Path p1 = paths.get(i);
            if ( aborter.should_abort())
            {
                logger.log("aborting Similarity_cache "+ aborter.reason());
                while ( cdl.getCount()> 0) cdl.countDown();
                break;
            }
            Similarity_cache_warmer_message m = new Similarity_cache_warmer_message(owner,aborter, p1,i);
            Job_termination_reporter tr = (message, job) -> {
                cdl.countDown();
                in_flight.decrementAndGet();
                if (cdl.getCount() % 100 == 0)
                {
                    logger.log(" Remaining to fill similarity cache: " + cdl.getCount());
                    Check_remaining_RAM.RAM_running_low("Feature vectors",owner,logger);
                }
            };
            Actor_engine.run(actor, m, tr, logger);
        }

        try {
            cdl.await();
        } catch (InterruptedException e) {
            hourglass.ifPresent(Hourglass::close);
            logger.log("similarity cache interrupted" + e);
        }
        Mmap.instance.save_index();
        similarities.save_whole_cache_to_disk();
        hourglass.ifPresent(Hourglass::close);
    }

}
