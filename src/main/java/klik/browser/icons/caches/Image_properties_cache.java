//SOURCES ./Image_properties_actor.java
//SOURCES ./Image_properties_message.java
package klik.browser.icons.caches;

import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.actor.Job_termination_reporter;
import klik.browser.Change_type;
import klik.browser.icons.Paths_manager;
import klik.browser.icons.Refresh_target;
import klik.files_and_paths.Ding;
import klik.files_and_paths.Files_and_Paths;
import klik.properties.File_sort_by;
import klik.properties.Properties_manager;
import klik.properties.Static_application_properties;
import klik.util.Hourglass;
import klik.util.Logger;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

//**********************************************************
public class Image_properties_cache
//**********************************************************
{
    public final static boolean dbg = false;
    protected final Logger logger;
    protected final Aborter aborter;
    protected final String cache_name;
    protected final Path cache_file_path;
    private final Map<String, Image_properties> cache = new ConcurrentHashMap<>();
    protected final Properties_manager pm;
    Image_properties_actor image_properties_actor;

    //**********************************************************
    public Image_properties_cache(Path path, String cache_name_, Aborter aborter_, Logger logger_)
    //**********************************************************
    {
        logger = logger_;
        aborter = aborter_;
        cache_name = cache_name_;
        String local = cache_name+ path.toAbsolutePath();
        String cache_file_name = UUID.nameUUIDFromBytes(local.getBytes()) +".properties";
        Path dir = Files_and_Paths.get_aspect_ratio_and_rotation_caches_dir(null,logger);
        cache_file_path= Path.of(dir.toAbsolutePath().toString(), cache_file_name);
        if ( dbg) logger.log(cache_name+" cache file ="+cache_file_path);

        pm = new Properties_manager(cache_file_path,logger);
        image_properties_actor = new Image_properties_actor();
    }




    //**********************************************************
    public Image_properties get_from_cache(Path p, Job_termination_reporter tr, boolean wait_if_needed)
    //**********************************************************
    {
        Image_properties image_properties =  cache.get(key_from_path(p));
        if ( image_properties != null)
        {
            if ( tr != null) tr.has_ended("found in cache",null);
            return image_properties;
        }
        Image_properties_message imp = new Image_properties_message(p,this,aborter,logger);
        if ( wait_if_needed)
        {
            image_properties_actor.run(imp);
            return cache.get(key_from_path(p));
        }
        Actor_engine.run(image_properties_actor,imp,tr,logger);
        return null;
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
    public void inject(Path path, Image_properties val, boolean and_save)
    //**********************************************************
    {
        if(dbg) logger.log(cache_name+" inject "+path+" value="+val );
        cache.put(key_from_path(path),val);
        if ( and_save) save_one_item_to_disk(path,val);
    }

    //**********************************************************
    public void clear_RAM_cache_fx()
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
                    Image_properties ip = Image_properties.from_string(value);
                    if ( dbg) logger.log("reloading : "+key+" => "+ ip.to_string());
                    cache.put(key, ip);
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
        //if (dbg)
            logger.log(cache_name+": "+already_in_RAM+" already in RAM, "+reloaded+" items reloaded from file");

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
        for(Map.Entry<String, Image_properties> e : cache.entrySet())
        {
            saved++;
            pm.imperative_store(e.getKey(), e.getValue().to_string(), false, false);
        }
        pm.store_properties();
        if (dbg) logger.log(saved +" TRUE items of aspect ratio cache saved to file");
    }
    //**********************************************************
    public void save_one_item_to_disk(Path path, Image_properties ip)
    //**********************************************************
    {
        pm.imperative_store(key_from_path(path), ip.to_string(), false, true);
    }


    //**********************************************************
    public void all_image_properties_acquired_4(Paths_manager paths_manager, Refresh_target refresh_target, Change_type change_type, long start, Hourglass running_man)
    //**********************************************************
    {
        logger.log("Image_propertiew_cache::all_image_properties_acquired() ");
        Actor_engine.execute(()->save_whole_cache_to_disk(),logger);

        if (System.currentTimeMillis() - start > 5_000) {
            if (Static_application_properties.get_ding(logger)) {
                Ding.play("all_image_properties_acquired: done acquiring all image properties", logger);
            }
        }
        Comparator<Path> local_file_comparator = null;
        switch (Static_application_properties.get_sort_files_by(logger))
        {
            case File_sort_by.ASPECT_RATIO -> local_file_comparator = new Aspect_ratio_comparator();
            case File_sort_by.RANDOM_ASPECT_RATIO -> local_file_comparator = new Aspect_ratio_comparator_random();
            case File_sort_by.IMAGE_WIDTH -> local_file_comparator = new Image_width_comparator();
            case File_sort_by.IMAGE_HEIGHT -> local_file_comparator = new Image_height_comparator();
            default -> local_file_comparator = null;
        }
        if (local_file_comparator != null)
        {
            paths_manager.set_new_iconized_items_comparator(local_file_comparator);
        }
        //logger.log("all_image_properties_acquired, going to refresh");
        refresh_target.refresh_UI_after_scan_dir_5(change_type,"all_image_properties_acquired", running_man);

    }





    //**********************************************************
    class Aspect_ratio_comparator implements Comparator<Path>
    //**********************************************************
    {
        @Override
        public int compare(Path p1, Path p2)
        {
            Image_properties ip1 = get_from_cache(p1,null,true);
            if ( ip1 == null)
            {
                logger.log("panic234");
                return 0;
            }
            Double d1 = ip1.get_aspect_ratio();
            Image_properties ip2 = get_from_cache(p2,null,true);
            if ( ip2 == null)
            {
                logger.log("panic235");
                return 0;
            }
            Double d2 = ip2.get_aspect_ratio();
            int diff =  d1.compareTo(d2);
            if ( diff != 0) return diff;
            return (p1.toString().compareTo(p2.toString()));
        }
    };

    //**********************************************************
    class Aspect_ratio_comparator_random implements Comparator<Path>
    //**********************************************************
    {
        long seed;
        HashMap<Path,Long> cache_local = new HashMap<>();
        public Aspect_ratio_comparator_random()
        {
            Random r = new Random();
            seed = r.nextLong();

        }
        @Override
        public int compare(Path p1, Path p2) {
            Image_properties ip1 = get_from_cache(p1,null, true);
            if ( ip1 == null)
            {
                System.out.println("should not happen");
                return 0;
            }
            Double d1 = ip1.get_aspect_ratio();
            Image_properties ip2 = get_from_cache(p2,null, true);
            if ( ip2 == null)
            {
                System.out.println("should not happen");
                return 0;
            }
            Double d2 = ip2.get_aspect_ratio();

            int diff = d1.compareTo(d2);
            if (diff != 0) return diff;

            Long l1 = cache_local.get(p1);
            if ( l1 == null) {
                // same aspect ratio so the order must be pseudo random... but consistent for each comparator instance
                long s1 = UUID.nameUUIDFromBytes(p1.getFileName().toString().getBytes()).getMostSignificantBits();
                l1 = new Random(seed * s1).nextLong();
                cache_local.put(p1,l1);
            }

            Long l2 = cache_local.get(p2);
            if ( l2 == null) {
                // same aspect ratio so the order must be pseudo random... but consistent for each comparator instance
                long s2 = UUID.nameUUIDFromBytes(p2.getFileName().toString().getBytes()).getMostSignificantBits();
                l2 = new Random(seed * s2).nextLong();
                cache_local.put(p2, l2);
            }

            return l1.compareTo(l2);
        }
    };

    //**********************************************************
    class Image_width_comparator implements Comparator<Path>
    {
        @Override
        public int compare(Path p1, Path p2) {
            Image_properties ip1 = get_from_cache(p1,null, true);
            if ( ip1 == null) return 0;
            Double d1 = ip1.get_image_width();
            if ( d1 == null) return 0;
            Image_properties ip2 = get_from_cache(p2,null, true);
            if ( ip2 == null) return 0;
            Double d2 = ip2.get_image_width();
            if ( d2 == null) return 0;

            int diff =  d1.compareTo(d2);
            if ( diff != 0) return diff;
            return (p1.toString().compareTo(p2.toString()));
        }
    };


    //**********************************************************
    class Image_height_comparator implements Comparator<Path>
    {
        @Override
        public int compare(Path p1, Path p2) {
            Image_properties ip1 = get_from_cache(p1,null, true);
            if ( ip1 == null) return 0;
            Double d1 = ip1.get_image_height();
            if ( d1 == null) return 0;
            Image_properties ip2 = get_from_cache(p2,null, true);
            if ( ip2 == null) return 0;
            Double d2 = ip2.get_image_height();
            if ( d2 == null) return 0;

            int diff =  d1.compareTo(d2);
            if ( diff != 0) return diff;
            return (p1.toString().compareTo(p2.toString()));
        }
    };
}
