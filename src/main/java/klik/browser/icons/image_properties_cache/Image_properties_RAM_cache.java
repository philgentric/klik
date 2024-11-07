//SOURCES ./Image_properties_actor.java
//SOURCES ./Image_properties_message.java
package klik.browser.icons.image_properties_cache;

import javafx.stage.Stage;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.actor.Job_termination_reporter;
import klik.browser.Change_type;
import klik.browser.comparators.Aspect_ratio_comparator;
import klik.browser.comparators.Aspect_ratio_comparator_random;
import klik.browser.comparators.Image_height_comparator;
import klik.browser.comparators.Image_width_comparator;
import klik.browser.icons.Paths_manager;
import klik.browser.icons.Refresh_target;
import klik.util.files_and_paths.Ding;
import klik.level3.experimental.RAM_disk;
import klik.properties.File_sort_by;
import klik.properties.Properties_manager;
import klik.properties.Static_application_properties;
import klik.util.performance_monitor.Performance_monitor;
import klik.util.ui.Hourglass;
import klik.util.log.Logger;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

//**********************************************************
public class Image_properties_RAM_cache
//**********************************************************
{
    public final static boolean dbg = false;
    protected final Logger logger;
    private final Aborter aborter;
    protected final String cache_name;
    protected final Path cache_file_path;
    private final Map<String, Image_properties> cache = new ConcurrentHashMap<>();
    protected final Properties_manager pm;
    private final Image_properties_actor image_properties_actor;

    private final int instance_number;
    private static int instance_number_generator = 0;
    //**********************************************************
    public Image_properties_RAM_cache(Path path, String cache_name_, Aborter aborter_, Logger logger_)
    //**********************************************************
    {
        instance_number = instance_number_generator++;
        logger = logger_;
        aborter = aborter_;
        cache_name = cache_name_;
        String local = cache_name+ path.toAbsolutePath();
        String cache_file_name = UUID.nameUUIDFromBytes(local.getBytes()) +".properties";
        Path dir = get_image_properties_cache_dir(null,logger);
        cache_file_path= Path.of(dir.toAbsolutePath().toString(), cache_file_name);
        if ( dbg) logger.log(cache_name+" cache file ="+cache_file_path);

        pm = new Properties_manager(cache_file_path,logger);
        image_properties_actor = new Image_properties_actor();
    }

    //**********************************************************
    public static Path get_image_properties_cache_dir(Stage owner, Logger logger)
    //**********************************************************
    {
        if ( RAM_disk.get_use_RAM_disk(logger))
        {
            Path tmp_dir = RAM_disk.get_absolute_dir_on_RAM_disk(Static_application_properties.IMAGE_PROPERTIES_CACHE_DIR, owner, logger);
            //if (dbg)
            if (tmp_dir != null) {
                logger.log("Aspect ratio and rotation cache folder=" + tmp_dir.toAbsolutePath());
            }
            return tmp_dir;
        }

        Path tmp_dir = Static_application_properties.get_absolute_dir_on_user_home(Static_application_properties.IMAGE_PROPERTIES_CACHE_DIR, false,logger);
        if (dbg) if (tmp_dir != null) {
            logger.log("Aspect ratio and rotation cache folder=" + tmp_dir.toAbsolutePath());
        }
        return tmp_dir;
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
        if ( aborter.should_abort())
        {
            logger.log((instance_number+" PANIC aborter "+aborter.name+" reason="+aborter.reason+ " target path="+p));
            return null;
        }
        else {
            //logger.log(instance_number+" OK aborter "+aborter.name+" reason="+aborter.reason);
        }
        Image_properties_message imp = new Image_properties_message(p,this,aborter,logger);
        if ( wait_if_needed)
        {
            image_properties_actor.run(imp); // blocking call
            Image_properties x = cache.get(key_from_path(p));
            if ( x == null)
            {
                logger.log("PANIC null Image_properties in cache after blocking call ");
            }
            return x;
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
    public void inject(Path path, Image_properties val, boolean and_save_to_disk)
    //**********************************************************
    {
        if(dbg) logger.log(cache_name+" inject "+path+" value="+val );
        cache.put(key_from_path(path),val);
        if ( and_save_to_disk) save_one_item_to_disk(path,val);
    }

    //**********************************************************
    public void clear_image_properties_RAM_cache_fx()
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
            pm.add(e.getKey(), e.getValue().to_string(), false);
        }
        pm.store_properties();
        if (dbg) logger.log(saved +" TRUE items of aspect ratio cache saved to file");
    }
    //**********************************************************
    public void save_one_item_to_disk(Path path, Image_properties ip)
    //**********************************************************
    {
        pm.add_and_save(key_from_path(path), ip.to_string());
    }


    //**********************************************************
    public void all_image_properties_acquired_4(Paths_manager paths_manager, Refresh_target refresh_target, Change_type change_type, long start, Hourglass running_man)
    //**********************************************************
    {
        //logger.log("Image_propertiew_cache::all_image_properties_acquired() ");
        Actor_engine.execute(this::save_whole_cache_to_disk,logger);

        if (System.currentTimeMillis() - start > 5_000) {
            if (Static_application_properties.get_ding(logger)) {
                Ding.play("all_image_properties_acquired: done acquiring all image properties", logger);
            }
        }
        determine_file_comparator(paths_manager);
        //logger.log("all_image_properties_acquired, going to refresh");
        refresh_target.refresh_UI_after_scan_dir_5(change_type,"all_image_properties_acquired", running_man);

        long end = System.currentTimeMillis();
        Performance_monitor.register_new_record("Browser",paths_manager.folder_path.toString(),end-start,logger);
    }

    record File_comp_cache(File_sort_by file_sort_by, Comparator<Path> comparator){}

    private File_comp_cache file_comp_cache;

    //**********************************************************
    private void determine_file_comparator(Paths_manager paths_manager)
    //**********************************************************
    {
        Comparator<Path> local_file_comparator = null;
        if ( file_comp_cache != null)
        {
            if ( file_comp_cache.file_sort_by() == File_sort_by.get_sort_files_by(logger))
            {
                logger.log("getting file comparator from cache="+file_comp_cache);
                local_file_comparator = file_comp_cache.comparator();
            }
        }
        if ( local_file_comparator == null) {
            local_file_comparator = create_new_file_comparator();
        }
        if (local_file_comparator != null)
        {
            //logger.log("setting file_comp_cache ="+file_comp_cache);
            file_comp_cache =  new File_comp_cache(File_sort_by.get_sort_files_by(logger),local_file_comparator);
            paths_manager.set_new_iconized_items_comparator(local_file_comparator);
        }
    }

    //**********************************************************
    Comparator<Path> create_new_file_comparator()
    //**********************************************************
    {
        Comparator<Path> local_file_comparator = null;
        switch (File_sort_by.get_sort_files_by(logger))
        {
            case File_sort_by.ASPECT_RATIO -> local_file_comparator = new Aspect_ratio_comparator(this);
            case File_sort_by.RANDOM_ASPECT_RATIO -> local_file_comparator = new Aspect_ratio_comparator_random(this);
            case File_sort_by.IMAGE_WIDTH -> local_file_comparator = new Image_width_comparator(this);
            case File_sort_by.IMAGE_HEIGHT -> local_file_comparator = new Image_height_comparator(this,logger);
        }
        return local_file_comparator;
    }



}
