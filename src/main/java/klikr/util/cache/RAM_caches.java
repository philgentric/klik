package klikr.util.cache;

import javafx.stage.Window;
import klikr.browser.comparators.Similarity_comparator;
import klikr.browser.icons.image_properties_cache.Image_properties;
import klikr.browser.virtual_landscape.Scroll_position_cache;
import klikr.images.caching.Image_cache_interface;
import klikr.machine_learning.feature_vector.Feature_vector_cache;
import klikr.machine_learning.similarity.Similarity_cache;
import klikr.util.files_and_paths.Static_files_and_paths_utilities;
import klikr.util.log.Logger;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//**********************************************************
public class RAM_caches
//**********************************************************
{
    private static final boolean dbg = false;
    public final static Map<String, Image_cache_interface> image_caches = new ConcurrentHashMap<>();
    public final static Map<String, Klikr_cache<Path, Image_properties>> image_properties_cache_of_caches = new ConcurrentHashMap<>();
    public final static Map<String, Similarity_cache> similarity_cache_of_caches = new ConcurrentHashMap<>();
    public final static Map<String, Feature_vector_cache> fv_cache_of_caches = new ConcurrentHashMap<>();
    public final static Map<String, Similarity_comparator> similarity_comparator_cache = new ConcurrentHashMap<>();
    public static Klikr_cache<Path, Double> duration_cache;
    public static Klikr_cache<Path, Double> bitrate_cache;


    //**********************************************************
    public static void clear_all_RAM_caches(Window owner,Logger logger)
    //**********************************************************
    {
        double total = 0;

        for ( String s  : image_properties_cache_of_caches.keySet())
        {
            Klikr_cache<Path, Image_properties> x = image_properties_cache_of_caches.get(s);
            total += x.clear_RAM();
        }
        if ( dbg) logger.log("✅ All image properties RAM caches cleared");


        for ( Image_cache_interface ici : image_caches.values())
        {
            total += ici.clear_RAM();
        }
        if ( dbg) logger.log("✅ All image RAM caches cleared");


        for (Similarity_cache sc : similarity_cache_of_caches.values())
        {
            total += sc.clear_RAM();
        }
        similarity_cache_of_caches.clear();
        if ( dbg) logger.log("✅ All similarity RAM caches cleared");


        for (Klikr_cache<Path, Image_properties> rc : image_properties_cache_of_caches.values())
        {
            total += rc.clear_RAM();
        }
        image_properties_cache_of_caches.clear();
        if ( dbg) logger.log("✅ All image properties RAM caches cleared");


        for (Feature_vector_cache fvc : fv_cache_of_caches.values())
        {
            total += fvc.clear_RAM();
        }
        fv_cache_of_caches.clear();
        if ( dbg) logger.log("✅ All feature vector RAM caches cleared");

        if (duration_cache != null) total += duration_cache.clear_RAM();
        if ( dbg) logger.log("✅ song duration cache cleared");

        if (bitrate_cache !=null) total += bitrate_cache.clear_RAM();
        if ( dbg) logger.log("✅ song bitrate cache cleared");

        total += Scroll_position_cache.scroll_position_cache_clear();
        if ( dbg) logger.log("✅ scroll position cache cleared");

        String size_in_bytes = Static_files_and_paths_utilities.get_1_line_string_for_byte_data_size(total,owner,logger);
        logger.log("\n\n✅ Total cleared RAM bytes: " + size_in_bytes+"\n\n");
    }


    //**********************************************************
    public static void clear_image_comparators_caches()
    //**********************************************************
    {
        for ( Similarity_comparator x : RAM_caches.similarity_comparator_cache.values())
        {
            x.clear_RAM();
        }
    }


    //**********************************************************
    public static void clear_image_feature_vector_RAM_cache()
    //**********************************************************
    {
        for( Feature_vector_cache x : RAM_caches.fv_cache_of_caches.values())
        {
            x.clear_RAM();
        }
        RAM_caches.fv_cache_of_caches.clear();
    }

    /*
    //**********************************************************
    public static void clear_all_disk_caches(List<Disk_cleared> cleared_folders, Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {

        for ( String s  : image_properties_cache_of_caches.keySet())
        {
            Klikr_cache<Path, Image_properties> x = image_properties_cache_of_caches.get(s);
            cleared_folders.add(x.clear_folder(owner, aborter, logger));
        }
        if ( dbg) logger.log("✅ All image properties disk caches cleared");

        for (Similarity_cache sc : similarity_cache_of_caches.values())
        {
            cleared_folders.add(sc.clear_folder(owner, aborter, logger));
        }
        similarity_cache_of_caches.clear();
        if ( dbg) logger.log("✅ All similarity disk caches cleared");

        for (Klikr_cache<Path, Image_properties> rc : image_properties_cache_of_caches.values())
        {
            cleared_folders.add(rc.clear_folder(owner, aborter, logger));
        }
        image_properties_cache_of_caches.clear();
        if ( dbg) logger.log("✅ All image properties disk caches cleared");

        for (Feature_vector_cache fvc : fv_cache_of_caches.values())
        {
            cleared_folders.add(fvc.clear_folder(owner, aborter, logger));
        }
        fv_cache_of_caches.clear();
        if ( dbg) logger.log("✅ All feature vector disk caches cleared");

        double total = 0;
        for ( Disk_cleared dc : cleared_folders)
        {
            if ( dbg) logger.log("✅ Cleared folder: " + dc.path()+ " "+dc.bytes()+ " bytes");
            total += dc.bytes();
        }
        String size_in_bytes = Static_files_and_paths_utilities.get_1_line_string_for_byte_data_size(total,owner,logger);

        logger.log("✅ Total cleared disk bytes: " + size_in_bytes);
    }
*/
}
