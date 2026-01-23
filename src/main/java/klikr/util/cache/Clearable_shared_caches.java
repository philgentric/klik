package klikr.util.cache;

import javafx.stage.Window;
import klikr.browser.comparators.Similarity_comparator;
import klikr.browser.icons.image_properties_cache.Image_properties;
import klikr.images.caching.Image_cache_interface;
import klikr.machine_learning.feature_vector.Feature_vector_cache;
import klikr.machine_learning.similarity.Similarity_cache;
import klikr.util.Shared_services;
import klikr.util.execute.actor.Aborter;
import klikr.util.log.Logger;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

//**********************************************************
public class Clearable_shared_caches
//**********************************************************
{
    public final static Map<String, Image_cache_interface> image_caches = new HashMap<>();
    public final static Map<String, Klikr_cache<Path, Image_properties>> image_properties_cache_of_caches = new HashMap<>();
    public final static Map<String, Similarity_cache> similarity_cache_of_caches = new HashMap<>();
    public final static Map<String, Feature_vector_cache> fv_cache_of_caches = new HashMap<>();
    public final static Map<String, Similarity_comparator> similarity_comparator_cache = new HashMap<>();

    //**********************************************************
    public static void clear_all_disk_caches(Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {

        for ( String s  : image_properties_cache_of_caches.keySet())
        {
            Klikr_cache<Path, Image_properties> x = image_properties_cache_of_caches.get(s);
            x.clear_disk(owner, aborter, logger);
        }
        logger.log("✅ All image properties disk caches cleared");

        for (Similarity_cache sc : similarity_cache_of_caches.values())
        {
            sc.clear_disk(owner, aborter, logger);
        }
        similarity_cache_of_caches.clear();
        logger.log("✅ All similarity disk caches cleared");

        for (Klikr_cache<Path, Image_properties> rc : image_properties_cache_of_caches.values())
        {
            rc.clear_disk(owner, aborter, logger);
        }
        image_properties_cache_of_caches.clear();
        logger.log("✅ All image properties disk caches cleared");

        for (Feature_vector_cache fvc : fv_cache_of_caches.values())
        {
            fvc.clear_disk(owner, aborter, logger);
        }
        fv_cache_of_caches.clear();
        logger.log("✅ All feature vector disk caches cleared");
    }

    //**********************************************************
    public static void clear_all_RAM_caches(Logger logger)
    //**********************************************************
    {
        for ( String s  : image_properties_cache_of_caches.keySet())
        {
            Klikr_cache<Path, Image_properties> x = image_properties_cache_of_caches.get(s);
            x.clear_RAM();
        }
        logger.log("✅ All image properties RAM caches cleared");


        for ( Image_cache_interface ici : image_caches.values())
        {
            ici.clear_RAM();
        }
        logger.log("✅ All image RAM caches cleared");
        for (Similarity_cache sc : similarity_cache_of_caches.values())
        {
            sc.clear_RAM();
        }
        similarity_cache_of_caches.clear();
        logger.log("✅ All similarity RAM caches cleared");

        for (Klikr_cache<Path, Image_properties> rc : image_properties_cache_of_caches.values())
        {
            rc.clear_RAM();
        }
        image_properties_cache_of_caches.clear();
        logger.log("✅ All image properties RAM caches cleared");

        for (Feature_vector_cache fvc : fv_cache_of_caches.values())
        {
            fvc.clear_RAM();
        }
        fv_cache_of_caches.clear();
        logger.log("✅ All feature vector RAM caches cleared");
    }
}
