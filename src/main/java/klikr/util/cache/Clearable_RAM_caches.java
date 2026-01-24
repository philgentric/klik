package klikr.util.cache;

import javafx.stage.Window;
import klikr.browser.comparators.Similarity_comparator;
import klikr.browser.icons.image_properties_cache.Image_properties;
import klikr.images.caching.Image_cache_interface;
import klikr.machine_learning.feature_vector.Feature_vector_cache;
import klikr.machine_learning.similarity.Similarity_cache;
import klikr.util.log.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//**********************************************************
public class Clearable_RAM_caches
//**********************************************************
{
    private static final List<Clearable_RAM_cache> clearable_RAM_caches = new ArrayList<>();

    //**********************************************************
    public static void record(Clearable_RAM_cache clearable_RAM_cache)
    //**********************************************************
    {
        clearable_RAM_caches.add(clearable_RAM_cache);
    }

    //**********************************************************
    public static void clear_all_RAM_caches(Window owner, Logger logger)
    //**********************************************************
    {
        for ( Clearable_RAM_cache clearable_RAM_cache : clearable_RAM_caches )
        {
            clearable_RAM_cache.clear_RAM();
        }

        Clearable_shared_caches.clear_all_RAM_caches(owner, logger);

    }


    //**********************************************************
    public static void clear_image_comparators_caches()
    //**********************************************************
    {
        for ( Similarity_comparator x : Clearable_shared_caches.similarity_comparator_cache.values())
        {
            x.clear_RAM();
        }
    }

    //**********************************************************
    public static void clear_image_feature_vector_RAM_cache()
    //**********************************************************
    {
        for( Feature_vector_cache x : Clearable_shared_caches.fv_cache_of_caches.values())
        {
            x.clear_RAM();
        }
        Clearable_shared_caches.fv_cache_of_caches.clear();
    }

}
