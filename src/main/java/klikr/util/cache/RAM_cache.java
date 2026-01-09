// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.util.cache;

import javafx.stage.Window;
import klikr.browser.Clearable_RAM_cache;
import klikr.path_lists.Path_list_provider;
import klikr.properties.Non_booleans_properties;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.execute.actor.Job_termination_reporter;
import klikr.util.files_and_paths.Static_files_and_paths_utilities;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.function.Function;

// a cache... what's so special: it is a cache 'that can fill itself'
// just provide 'value_extractor' so that, if a value is missing
// the cache can call to get the new value
// this will happen if you call get() and the value is not there
// get() has 2 mode:
//      a blocking one,
//      a fast-return-null mode that will callback when the value is there
// if you want to warm the cache, call 'prefill_cache' for all the items you want in,
// in all cases the threads are hidden
//
// can also save itself to disk, and reload of course

//**********************************************************
public class RAM_cache<K,V> implements Clearable_RAM_cache
//**********************************************************
{

    public final static boolean dbg = false;
    protected final Logger logger;
    protected final String name;
    private final RAM_cache_actor<K,V> actor;
    private final Function<K,String> string_key_maker;
    // high level, the key is anything i.e. "K" but internally we rely only on Strings
    private final Map<String,V> cache = new ConcurrentHashMap<>();
    private final Disk_engine disk_engine;


    // when disk engine is a properties file, keys are Strings
    //**********************************************************
    public RAM_cache(
            Path_list_provider path_list_provider,
            String cache_name_,
            Function<V, String> string_serializer,
            Function<String, V> string_deserializer,
            Function<String,V> value_extractor,
            Function<K,String> string_key_maker,
            Aborter aborter,
            Window owner, Logger logger_)
    //**********************************************************
    {
        this.string_key_maker = string_key_maker;
        logger = logger_;
        name = cache_name_;
        String local = name + path_list_provider.get_name();
        if ( dbg) logger.log(name +" local ="+local);
        String cache_file_name = path_list_provider.get_folder_path().getFileName().toString()+"_"+UUID.nameUUIDFromBytes(local.getBytes()) +".properties";
        if ( dbg) logger.log(name +" cache_file_name ="+cache_file_name);
        Path dir = Non_booleans_properties.get_absolute_hidden_dir_on_user_home(name, false,owner, logger);
        if ( dbg) logger.log(name +" dir ="+dir.toAbsolutePath().toString());

        disk_engine = new Properties_engine<V>(
                name,
                dir,
                string_serializer,
                string_deserializer,
                owner,
                aborter,
                logger
        );

        actor = new RAM_cache_actor(value_extractor,logger);
    }

    // when disk engine is a binary file,
    // keys are whatever you want BUT ...
    // since internally we use only Strings
    // you must provide methods:
    // 1. to get a String from your Object-key
    // 2. to get a Object-key from a String-key

    //**********************************************************
    public RAM_cache(
            Path_list_provider path_list_provider,
            String cache_name_,
            BiPredicate<K, DataOutputStream> key_serializer,
            Function<DataInputStream, K> key_deserializer,
            BiPredicate<V, DataOutputStream> value_serializer,
            Function<DataInputStream, V> value_deserializer,
            Function<K,V> value_extractor,
            Function<K,String> string_key_maker,
            Function<String,K> object_key_maker,
            Aborter aborter,
            Window owner, Logger logger_)
    //**********************************************************
    {
        this.string_key_maker = string_key_maker;
        logger = logger_;
        name = cache_name_;
        String local = name + path_list_provider.get_name();
        if ( dbg) logger.log(name +" local ="+local);
        String cache_file_name = path_list_provider.get_folder_path().getFileName().toString()+"_"+UUID.nameUUIDFromBytes(local.getBytes()) +".properties";
        if ( dbg) logger.log(name +" cache_file_name ="+cache_file_name);
        Path dir = Non_booleans_properties.get_absolute_hidden_dir_on_user_home(name, false,owner, logger);
        if ( dbg) logger.log(name +" dir ="+dir.toAbsolutePath().toString());
        Path cache_file_path = dir.resolve(cache_file_name);

        disk_engine = new Binary_file_engine(
                name,
                cache_file_path,
                key_serializer,
                key_deserializer,
                value_serializer,
                value_deserializer,
                string_key_maker,
                object_key_maker,
                owner,
                aborter,
                logger
        );

        actor = new RAM_cache_actor(value_extractor,logger);
    }


    //**********************************************************
    public static Path get_cache_dir(String cache_name, Window owner, Logger logger)
    //**********************************************************
    {
        Path tmp_dir = Non_booleans_properties.get_absolute_hidden_dir_on_user_home(cache_name, false,owner, logger);
        if (dbg) if (tmp_dir != null) {
            logger.log(cache_name+", cache folder=" + tmp_dir.toAbsolutePath());
        }
        return tmp_dir;
    }




    //**********************************************************
    // this routine will return the <T> if it is in the cache, if not, 2 cases
    // if tr is null then this routine will BLOCK until <T> is in the cache,
    // and return it
    // if tr is not null then this routine will return null
    // and start the cache filling in a thread,
    // which will call tr.has_ended when finished e.g. <T> available
    public V get(K object_key, Aborter aborter, Job_termination_reporter tr, Window owner)
    //**********************************************************
    {
        if ( object_key == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("FATAL "));
            return null;
        }
        String real_key= string_key_maker.apply(object_key);
        V value = cache.get(real_key);
        if ( value != null)
        {
            if ( tr != null) tr.has_ended("found in cache",null);
            return value;
        }
        if ( aborter.should_abort())
        {
            //logger.log(("yop! aborting works on cache , aborter "+aborter.name+" reason="+aborter.reason+ " target path="+p));
            return null;
        }

        RAM_cache_message<K,V> msg = new RAM_cache_message<K,V>(object_key, false, this,aborter,owner);
        if ( tr == null)
        {
            // blocking call
            actor.run(msg);
            return cache.get(real_key);
        }
        // call in a thread
        //logger.log("call in a thread "+key);
        Actor_engine.run(actor,msg,tr,logger);
        return null;
    }

    //**********************************************************
    public void prefill_cache(K key, boolean check_if_present, Aborter aborter, Window owner)
    //**********************************************************
    {
        RAM_cache_message<K,V> imp = new RAM_cache_message<K,V>(key,check_if_present,this,aborter,owner);
        Actor_engine.run(actor,imp,null,logger);
    }

    //**********************************************************
    static String path_to_key(Path p)
    //**********************************************************
    {
        String local = p.getFileName().toString();
        //String local = p.toAbsolutePath().toString();
        return local;//UUID.nameUUIDFromBytes(local.getBytes()).toString();
    }
    //**********************************************************
    public void inject(K object_key, V value, boolean and_save_to_disk)
    //**********************************************************
    {
        if ( object_key == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("FATAL "));
            return;
        }
        String real_key = string_key_maker.apply(object_key);
        if(dbg) logger.log("RAM cache: "+ name +" injecting: real_key="+real_key+" value="+value );
        cache.put(real_key,value);
        if ( and_save_to_disk) save_one_item_to_disk(object_key,value);
    }

    //**********************************************************
    @Override
    public void clear_RAM()
    //**********************************************************
    {
        cache.clear();
        if (dbg) logger.log(name +" RAM cache file cleared");
    }
    //**********************************************************
    public int reload_cache_from_disk()
    //**********************************************************
    {
        return disk_engine.load_from_disk(cache);
    }

    //**********************************************************
    public void save_whole_cache_to_disk()
    //**********************************************************
    {
        disk_engine.save_to_disk(cache);
    }
    //**********************************************************
    public void save_one_item_to_disk(K key, V value)
    //**********************************************************
    {
        logger.log("WARNING: save_one_item_to_disk not implemente yet");
        //disk_engine.save_one_to_disk(key,value);
    }

    public double clear_DISK(Aborter aborter, Window owner)
    {
        Path path = Non_booleans_properties.get_absolute_hidden_dir_on_user_home(name, false, owner,logger);

        return Static_files_and_paths_utilities.clear_folder(path, name, false,false,owner, aborter, logger);
    }
}
