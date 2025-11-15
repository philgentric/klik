// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.browser.icons.image_properties_cache;

//SOURCES ./Image_properties_actor.java
//SOURCES ./Image_properties_message.java

import javafx.stage.Stage;
import javafx.stage.Window;
import klik.Shared_services;
import klik.browser.Clearable_RAM_cache;
import klik.util.execute.actor.Aborter;
import klik.util.execute.actor.Actor_engine;
import klik.util.execute.actor.Job_termination_reporter;
import klik.path_lists.Path_list_provider;
import klik.properties.*;
import klik.util.files_and_paths.Guess_file_type;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

//**********************************************************
public class Image_properties_RAM_cache implements Clearable_RAM_cache
//**********************************************************
{
    public final static boolean dbg = false;
    protected final Logger logger;
    protected final String cache_name;
    protected final Path cache_file_path;
    private final Map<String, Image_properties> cache = new ConcurrentHashMap<>();
    protected final Properties_manager pm;
    private final Image_properties_actor image_properties_actor;

    private final int instance_number;
    private static int instance_number_generator = 0;


    //**********************************************************
    public static Image_properties_RAM_cache get(Path_list_provider path_list_provider,Window owner, Logger logger)
    //**********************************************************
    {
        return new Image_properties_RAM_cache(path_list_provider,"Image properties cache", owner, logger);
    }

    //**********************************************************
    public Image_properties_RAM_cache(
            Path_list_provider path_list_provider,
            String cache_name_,
            Window owner, Logger logger_)
    //**********************************************************
    {
        instance_number = instance_number_generator++;
        logger = logger_;
        cache_name = cache_name_;
        String local = cache_name+ path_list_provider.get_name();
        String cache_file_name = UUID.nameUUIDFromBytes(local.getBytes()) +".properties";
        Path dir = get_image_properties_cache_dir(null,logger);
        cache_file_path= Path.of(dir.toAbsolutePath().toString(), cache_file_name);
        if ( dbg) logger.log(cache_name+" cache file ="+cache_file_path);

        pm = new Properties_manager(cache_file_path,"image properties cache for folder "+path_list_provider.get_name(),owner, Shared_services.aborter(),logger);
        image_properties_actor = new Image_properties_actor();
    }

    //**********************************************************
    public static Path get_image_properties_cache_dir(Stage owner, Logger logger)
    //**********************************************************
    {
        Path tmp_dir = Non_booleans_properties.get_absolute_hidden_dir_on_user_home(Cache_folder.klik_image_properties_cache.name(), false,owner, logger);
        if (dbg) if (tmp_dir != null) {
            logger.log("Image properties cache folder=" + tmp_dir.toAbsolutePath());
        }
        return tmp_dir;
    }




    //**********************************************************
    // this routine will return the Image_properties if it is in the cache, if not,
    // if tr is null then this routine will BLOCK until the Image_properties is in the cache,
    // if tr is not null then this routine will return null and start the cache filling
    // in a separate thread, which will call tr.has_ended when finished
    public Image_properties get(Path p, Aborter aborter, Job_termination_reporter tr)
    //**********************************************************
    {
        Image_properties image_properties =  cache.get(key_from_path(p));
        if ( image_properties != null)
        {
            if ( tr != null) tr.has_ended("found in cache",null);
            return image_properties;
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
        Image_properties_message imp = new Image_properties_message(p,this,aborter,logger);
        if ( tr == null)
        {
            image_properties_actor.run(imp); // blocking call
            Image_properties x = cache.get(key_from_path(p));
            if ( x == null)
            {
                if (Guess_file_type.is_this_path_an_image(p,logger))
                {
                    logger.log(Stack_trace_getter.get_stack_trace("❗ WARNING null Image_properties in cache after blocking call for :" + p));
                }
            }
            return x;
        }
        Actor_engine.run(image_properties_actor,imp,tr,logger);
        return null;
    }

    //**********************************************************
    public void prefill_cache(Path p, Aborter aborter)
    //**********************************************************
    {
        Image_properties_message imp = new Image_properties_message(p,this,aborter,logger);
        Actor_engine.run(image_properties_actor,imp,null,logger);
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
    public void inject(Path path, Image_properties val, boolean and_save_to_disk)
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
                    Image_properties ip = Image_properties.from_string(value);
                    if ( ip == null)
                    {
                        cleanup.add(key);
                    }
                    else
                    {
                        if ( dbg) logger.log("reloading : "+key+" => "+ ip.to_string());
                        cache.put(key, ip);
                        reloaded++;
                        if ( dbg) logger.log("reloading : "+reloaded);
                    }

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
        if ( !cleanup.isEmpty()) pm.store_properties(false);

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
        for(Map.Entry<String, Image_properties> e : cache.entrySet())
        {
            saved++;
            pm.add(e.getKey(), e.getValue().to_string());
        }
        pm.store_properties(false);
        if (dbg) logger.log(saved +" TRUE items of aspect ratio cache saved to file");
    }
    //**********************************************************
    public void save_one_item_to_disk(Path path, Image_properties ip)
    //**********************************************************
    {
        pm.add(key_from_path(path), ip.to_string());
    }

}
