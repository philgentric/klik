// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.browser.virtual_landscape;

import klikr.browser.comparators.Similarity_comparator;
import klikr.images.caching.Image_cache_interface;
import klikr.machine_learning.feature_vector.Feature_vector_cache;
import klikr.machine_learning.similarity.Similarity_cache;
import klikr.browser.icons.image_properties_cache.Image_properties_RAM_cache;
import klikr.util.log.Logger;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

// this class just groups all the caches that a Virtual_landscape
// may have, most are "on demand" examples:
// the image properties cache is needed for sorting image by aspect ratio (not if images are sorted by name)
// the image feature vectors and sililaroty pairs caches are needed only when comparing images by similarity
//
// there are 2 levels:
// 1 cache per folder
// 1 static global cache-of-caches tp get faster response when you revisit the same folder
//
//

//**********************************************************
public class Browsing_caches
//**********************************************************
{


    //long term (process = VM life time) caches
    // make sure we go again at the same scroll point when we enter a given folder AGAIN
    // the key is the visited folder, the value is the scroll position,
    // that is : the path of the top-left item when leaving that folder
    private final static Map<String, Path> scroll_position_cache = new HashMap<>();

    //public final Image_properties_RAM_cache image_properties_RAM_cache;

    public final static Map<String, Image_properties_RAM_cache> image_properties_RAM_cache_of_caches = new HashMap<>();
    public final static Map<String, Similarity_cache> similarity_cache_of_caches = new HashMap<>();
    public final static Map<String, Feature_vector_cache> fv_cache_of_caches = new HashMap<>();
    public final static Map<String, Image_cache_interface> image_caches = new HashMap<>();
    public final static Map<String, Similarity_comparator> similarity_comparator_cache = new HashMap<>();


    //**********************************************************
    public static void clear_all_RAM_caches(Logger logger)
    //**********************************************************
    {
        scroll_position_cache.clear();
        logger.log("✅ scroll position RAM cache cleared");

        for ( Image_cache_interface x : image_caches.values())
        {
            x.clear_RAM_cache();
        }
        logger.log("✅ All iage RAM caches cleared");
        for (Similarity_cache x : similarity_cache_of_caches.values())
        {
            x.clear_RAM_cache();
        }
        logger.log("✅ All similarity RAM caches cleared");
        for (Image_properties_RAM_cache x : image_properties_RAM_cache_of_caches.values())
        {
            x.clear_RAM_cache();
        }
        logger.log("✅ All image properties RAM caches cleared");
        for (Feature_vector_cache x : fv_cache_of_caches.values())
        {
            x.clear_RAM_cache();
        }
        logger.log("✅ All feature vector RAM caches cleared");
    }



    //**********************************************************
    public static void scroll_position_cache_write(Path folder_path, Path top_left_item_path)
    //**********************************************************
    {
        if ( top_left_item_path != null) scroll_position_cache.put(folder_path.toAbsolutePath().toString(), top_left_item_path);
    }

    //**********************************************************
    public static void scroll_position_cache_clear()
    //**********************************************************
    {
        scroll_position_cache.clear();
    }


    //**********************************************************
    public static Path scroll_position_cache_read(Path folder_path)
    //**********************************************************
    {
        return scroll_position_cache.get(folder_path.toAbsolutePath().toString());
    }
}
