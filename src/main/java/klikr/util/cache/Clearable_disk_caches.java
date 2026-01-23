package klikr.util.cache;

import javafx.stage.Window;
import klikr.browser.icons.image_properties_cache.Image_properties;
import klikr.properties.Non_booleans_properties;
import klikr.util.Shared_services;
import klikr.util.execute.actor.Aborter;
import klikr.util.files_and_paths.Static_files_and_paths_utilities;
import klikr.util.image.icon_cache.Icon_caching;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

//**********************************************************
public class Clearable_disk_caches
//**********************************************************
{
    private static final boolean dbg = true;

    private static final List<Clearable_disk_cache> clearable_disk_caches = new ArrayList<>();

    //**********************************************************
    public static void record(Clearable_disk_cache clearable_DISK_cache)
    //**********************************************************
    {
        clearable_disk_caches.add(clearable_DISK_cache);
    }

    //**********************************************************
    public static void clear_all_disk_caches(Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        for ( Clearable_disk_cache clearable_disk_cache : clearable_disk_caches)
        {
            Shared_services.logger().log("Clearing disk cache:"+clearable_disk_cache.name());
            clearable_disk_cache.clear_disk(owner,aborter, logger);
        }

        Clearable_shared_caches.clear_all_disk_caches(owner, aborter, logger);


    }


    //**********************************************************
    public static double clear_disk_cache(Cache_folder cache_folder, boolean show_popup, Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        Path path = get_cache_dir(cache_folder,owner,logger);
        return Static_files_and_paths_utilities.clear_folder(path, cache_folder.name()+" cache on disk", show_popup, false, owner, aborter, logger);
    }

    //**********************************************************
    public static Path get_cache_dir(Cache_folder cache_folder, Window owner,Logger logger)
    //**********************************************************
    {
        Path tmp_dir = Static_files_and_paths_utilities.get_absolute_hidden_dir_on_user_home(cache_folder.name(), false, owner,logger);
        if (tmp_dir == null)
        {
            logger.log("WARNING get_absolute_hidden_dir_on_user_homer=" + null);
        }
        else
        {
            if (dbg) logger.log("get_absolute_hidden_dir_on_user_home=" + tmp_dir.toAbsolutePath());
        }
        return tmp_dir;
    }

    //**********************************************************
    public static void clear_one_icon_from_cache_on_disk(Path path, Window owner,Logger logger)
    //**********************************************************
    {
        Path icon_cache_dir = get_cache_dir( Cache_folder.icon_cache,owner,logger);
        int icon_size = Non_booleans_properties.get_icon_size(owner);

        Path icon_path = Icon_caching.path_for_icon_caching(path,String.valueOf(icon_size),Icon_caching.png_extension,owner,logger);
        try {
            Files.delete(icon_path);
            logger.log("one icon deleted from cache:" + icon_path);

        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace("WARNING: deleting one icon FAILED: " + e));
        }
    }

}
