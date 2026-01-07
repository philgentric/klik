// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.audio;

//SOURCES ./Image_properties_actor.java
//SOURCES ./Image_properties_message.java

import javafx.stage.Stage;
import javafx.stage.Window;
import klikr.Shared_services;
import klikr.browser.Clearable_RAM_cache;
import klikr.browser.icons.image_properties_cache.Image_properties;
import klikr.browser.icons.image_properties_cache.Image_properties_message;
import klikr.path_lists.Path_list_provider;
import klikr.properties.Cache_folder;
import klikr.properties.Non_booleans_properties;
import klikr.properties.Properties_manager;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.execute.actor.Job_termination_reporter;
import klikr.util.log.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

//**********************************************************
public class Song_duration_RAM_cache implements Clearable_RAM_cache
//**********************************************************
{
    public final static boolean dbg = false;
    protected final Logger logger;
    protected final String cache_name;
    protected final Path cache_file_path;
    private final Map<String, Double> cache = new ConcurrentHashMap<>();
    protected final Properties_manager pm;
    private final Song_duration_actor song_duration_actor;


    //**********************************************************
    public static Song_duration_RAM_cache build(Path_list_provider path_list_provider, Window owner, Logger logger)
    //**********************************************************
    {
        return new Song_duration_RAM_cache(path_list_provider,"Image properties cache", owner, logger);
    }

    //**********************************************************
    public Song_duration_RAM_cache(
            Path_list_provider path_list_provider,
            String cache_name_,
            Window owner, Logger logger_)
    //**********************************************************
    {
        logger = logger_;
        cache_name = cache_name_;
        String local = cache_name+ path_list_provider.get_name();
        String cache_file_name = UUID.nameUUIDFromBytes(local.getBytes()) +".properties";
        Path dir = get_song_duration_cache_dir(null,logger);
        cache_file_path= Path.of(dir.toAbsolutePath().toString(), cache_file_name);
        if ( dbg) logger.log(cache_name+" cache file ="+cache_file_path);

        pm = new Properties_manager(cache_file_path,"image properties cache for folder "+path_list_provider.get_name(),owner, Shared_services.aborter(),logger);
        song_duration_actor = new Song_duration_actor(logger);
    }

    //**********************************************************
    public static Path get_song_duration_cache_dir(Stage owner, Logger logger)
    //**********************************************************
    {
        Path tmp_dir = Non_booleans_properties.get_absolute_hidden_dir_on_user_home(Cache_folder.song_duration_cache.name(), false,owner, logger);
        if (dbg) if (tmp_dir != null) {
            logger.log("Song duration cache folder=" + tmp_dir.toAbsolutePath());
        }
        return tmp_dir;
    }




    //**********************************************************
    // this routine will return the Image_properties if it is in the cache, if not,
    // if tr is null then this routine will BLOCK until the Image_properties is in the cache,
    // if tr is not null then this routine will return null and start the cache filling
    // in a separate thread, which will call tr.has_ended when finished
    public Double get(Path target_path, Aborter aborter, Job_termination_reporter tr, Window owner)
    //**********************************************************
    {
        Double song_duration =  cache.get(key_from_path(target_path));
        if ( song_duration != null)
        {
            if ( tr != null) tr.has_ended("found in cache",null);
            return song_duration;
        }
        if ( aborter.should_abort())
        {
            //logger.log(("yop! aboritng works on image properties cache instance#"+ instance_number+"  aborter "+aborter.name+" reason="+aborter.reason+ " target path="+p));
            return null;
        }
        else
        {
            //logger.log(instance_number+" OK aborter "+aborter.name+" reason="+aborter.reason);
        }
        //logger.log(Stack_trace_getter.get_stack_trace("Image_properties_RAM_cache get"));
        Duration_message imp = new Duration_message(target_path,this,null,null,aborter);
        if ( tr == null)
        {
            // blocking call
            song_duration_actor.run(imp);
            Double x = cache.get(key_from_path(target_path));
            return x;
        }
        Actor_engine.run(song_duration_actor,imp,tr,logger);
        return null;
    }

    //**********************************************************
    public void prefill_cache(Path p, Aborter aborter)
    //**********************************************************
    {
        Duration_message imp = new Duration_message(p,this,null,null,aborter);
        //logger.log(Stack_trace_getter.get_stack_trace("Image_properties_RAM_cache prefill"));
        Actor_engine.run(song_duration_actor,imp,null,logger);
    }

    //**********************************************************
    private static String key_from_path(Path p)
    //**********************************************************
    {
        String local = p.getFileName().toString();
        //String local = p.toAbsolutePath().toString();
        return local;//UUID.nameUUIDFromBytes(local.getBytes()).toString();
    }
    //**********************************************************
    public void inject(Path path, Double val, boolean and_save_to_disk)
    //**********************************************************
    {
        if(dbg) logger.log(cache_name+" inject "+path+" value="+val );
        cache.put(key_from_path(path),val);
        if ( and_save_to_disk) save_one_item_to_disk(path,val);
    }

    //**********************************************************
    @Override
    public void clear_RAM_cache()
    //**********************************************************
    {
        cache.clear();
        if (dbg) logger.log("image properties RAM cache file cleared");
    }
    //**********************************************************
    public synchronized void reload_cache_from_disk()
    //**********************************************************
    {
        int reloaded = 0;
        int already_in_RAM = 0;
        List<String> cleanup = new ArrayList<>();

        for(String key : pm.get_all_keys())
        {
            if ( dbg) logger.log("reloading : "+key);

            String value = pm.get(key);
            if (cache.get(key) == null)
            {
                try
                {
                    Double dur = Double.valueOf(value);
                    if ( dbg) logger.log("reloading : "+key+" => "+ dur.toString());
                    cache.put(key, dur);
                    reloaded++;
                    if ( dbg) logger.log("reloading : "+reloaded);
                }
                catch(NumberFormatException x)
                {
                    // this entry in the file cache is wrong
                    cleanup.add(key);
                }
            }
            else
            {
                already_in_RAM++;
                if ( dbg) logger.log("already in RAM : "+key+" => "+ cache.get(key));
            }
        }
        for ( String key:cleanup)
        {
            pm.remove(key);
        }
        if ( !cleanup.isEmpty()) pm.save_everything_to_disk(false);

        if ( dbg)
        {
            logger.log(cache_name+": "+already_in_RAM+" already in RAM, "+reloaded+" items reloaded from file");
            logger.log("\n\n\n********************* "+cache_name+ " CACHE************************");
            for (String s  : cache.keySet())
            {
                logger.log(s+" => "+cache.get(s));
            }
            logger.log("****************************************************************\n\n\n");
        }
    }

    //**********************************************************
    public void save_whole_cache_to_disk()
    //**********************************************************
    {

        int saved = 0;
        for(Map.Entry<String, Double> e : cache.entrySet())
        {
            saved++;
            pm.add(e.getKey(), e.getValue().toString(),false);
        }
        pm.save_everything_to_disk(false);
        if (dbg) logger.log(saved +" TRUE items of aspect ratio cache saved to file");
    }
    //**********************************************************
    public void save_one_item_to_disk(Path path, Double dur)
    //**********************************************************
    {
        pm.add(key_from_path(path), dur.toString(),false);
    }

}
