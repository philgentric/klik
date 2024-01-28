package klik.browser.icons.caches;

import klik.actor.Aborter;
import klik.files_and_paths.Files_and_Paths;
import klik.properties.Properties_manager;
import klik.util.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

//**********************************************************
public class Cache_for_doubles
//**********************************************************
{
    public final static boolean dbg = false;
    protected final Logger logger;
    protected final Aborter aborter;
    protected final String cache_name;
    protected final Path cache_file_path;
    private final Map<String, Double> cache = new ConcurrentHashMap<>();
    protected final Properties_manager pm;

    //**********************************************************
    public Cache_for_doubles(Path path, String cache_name_, Aborter aborter_,  Logger logger_)
    //**********************************************************
    {
        logger = logger_;
        aborter = aborter_;
        cache_name = cache_name_;
        String local = cache_name+path.toAbsolutePath().toString();
        String cache_file_name = UUID.nameUUIDFromBytes(local.getBytes()).toString()+".properties";
        Path dir = Files_and_Paths.get_aspect_ratio_and_rotation_caches_dir(logger);
        cache_file_path= Path.of(dir.toAbsolutePath().toString(), cache_file_name);
        if ( dbg)
        {
            logger.log(cache_name+" cache file ="+cache_file_path);
        }
        pm = new Properties_manager(cache_file_path,logger);
    }


    //**********************************************************
    public void put_in_cache(Path p,double v)
    //**********************************************************
    {
        cache.put(key_from_path(p),v);
    }

    //**********************************************************
    public Double get_from_cache(Path p)
    //**********************************************************
    {
        return cache.get(key_from_path(p));
    }

    //**********************************************************
    private static String key_from_path(Path p)
    //**********************************************************
    {
        return p.getFileName().toString();
    }
    //**********************************************************
    public void inject(Path path, double val, boolean and_save)
    //**********************************************************
    {
        if(dbg) logger.log(cache_name+" inject "+path+" value="+val );
        cache.put(key_from_path(path),val);
        if ( and_save) save_one_item_to_disk(path,val);
    }

    //**********************************************************
    public void clear_RAM_cache()
    //**********************************************************
    {
        cache.clear();
        if (dbg) logger.log("aspect ratio cache file cleared");
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
                    double d = Double.valueOf(value);
                    if ( dbg) logger.log("reloading : "+key+" => "+ d);
                    cache.put(key, d);
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
        if ( !cleanup.isEmpty()) pm.store_properties();
        if (dbg) logger.log(cache_name+": "+already_in_RAM+" already in RAM, "+reloaded+" items reloaded from file");

        if ( dbg)
        {
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
            pm.imperative_store(e.getKey(), Double.toString(e.getValue()), false, false);
        }
        pm.store_properties();
        if (dbg) logger.log(saved +" TRUE items of aspect ratio cache saved to file");
    }
    //**********************************************************
    public void save_one_item_to_disk(Path path, Double rotation)
    //**********************************************************
    {
        pm.imperative_store(key_from_path(path), Double.toString(rotation), false, true);
    }

}
