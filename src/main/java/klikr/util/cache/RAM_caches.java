package klikr.util.cache;

import javafx.stage.Window;
import klikr.browser_core.comparators.Similarity_comparator;
import klikr.browser_core.icons.image_properties_cache.Image_properties;
import klikr.browser_core.virtual_landscape.Scroll_position_cache;
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
    public final static Map<String, Klikr_cache<Path, Image_properties>> image_properties_cache_of_caches = new ConcurrentHashMap<>();
    public final static Map<String, Image_cache_interface> image_caches = new ConcurrentHashMap<>();
    public final static Map<String, Similarity_cache> similarity_cache_of_caches = new ConcurrentHashMap<>();
    public final static Map<String, Similarity_comparator> similarity_comparator_cache = new ConcurrentHashMap<>();

    public final static Map<String, Feature_vector_cache> fv_cache_of_caches = new ConcurrentHashMap<>();
    public static Klikr_cache<Path, Double> duration_cache;
    public static Klikr_cache<Path, Double> bitrate_cache;

    public final static Map<String, Long> folder_file_count_cache = new HashMap<>();
    public final static Map<String,Long> folder_total_size_cache = new HashMap<>();
    public final static Map<String,Long> file_size_cache = new HashMap<>();


    //**********************************************************
    public static void clear_all_RAM_caches(Window owner,Logger logger)
    //**********************************************************
    {
        double total = 0;

        for (Klikr_cache<Path, Image_properties> kc : image_properties_cache_of_caches.values())
        {
            total += kc.clear_RAM();
        }
        image_properties_cache_of_caches.clear();
        if ( dbg) logger.log(Logger.ok+" All image properties RAM caches cleared");


        for ( Image_cache_interface ici : image_caches.values())
        {
            total += ici.clear_RAM();
        }
        image_caches.clear();
        if ( dbg) logger.log(Logger.ok+" All image RAM caches cleared");


        for (Similarity_cache sc : similarity_cache_of_caches.values())
        {
            total += sc.clear_RAM();
        }
        similarity_cache_of_caches.clear();
        if ( dbg) logger.log(Logger.ok+" All similarity RAM caches cleared");


        for (Similarity_comparator sc : similarity_comparator_cache.values())
        {
            total += sc.clear_RAM();
        }
        similarity_comparator_cache.clear();
        if ( dbg) logger.log(Logger.ok+" All similarity comparator RAM caches cleared");






        for (Feature_vector_cache fvc : fv_cache_of_caches.values())
        {
            total += fvc.clear_RAM();
        }
        fv_cache_of_caches.clear();
        if ( dbg) logger.log(Logger.ok+" All feature vector RAM caches cleared");


        if (duration_cache != null) total += duration_cache.clear_RAM();
        if ( dbg) logger.log(Logger.ok+" song duration cache cleared");

        if (bitrate_cache !=null) total += bitrate_cache.clear_RAM();
        if ( dbg) logger.log(Logger.ok+" song bitrate cache cleared");


        total += folder_file_count_cache.size();
        folder_file_count_cache.clear();
        total += folder_total_size_cache.size();
        folder_total_size_cache.clear();
        total += file_size_cache.size();
        file_size_cache.clear();


        total += Scroll_position_cache.scroll_position_cache_clear();
        if ( dbg) logger.log(Logger.ok+" scroll position cache cleared");

        String size_in_bytes = Static_files_and_paths_utilities.get_1_line_string_for_byte_data_size(total,owner,logger);
        logger.log("\n\n"+ Logger.ok+" Total cleared RAM bytes: " + size_in_bytes+"\n\n");
    }




}
