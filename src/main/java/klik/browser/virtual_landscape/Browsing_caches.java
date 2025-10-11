package klik.browser.virtual_landscape;

import javafx.stage.Window;
import klik.actor.Aborter;
import klik.machine_learning.feature_vector.Feature_vector_cache;
import klik.machine_learning.similarity.Similarity_cache;
import klik.browser.icons.image_properties_cache.Image_properties_RAM_cache;
import klik.util.log.Logger;

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

    public final Image_properties_RAM_cache image_properties_RAM_cache;

    public final static Map<String,Image_properties_RAM_cache> image_properties_RAM_cache_of_caches = new HashMap<>();
    public final static Map<String, Similarity_cache> similarity_cache_of_caches = new HashMap<>();
    public final static Map<String, Feature_vector_cache> fv_cache_of_caches = new HashMap<>();

    //**********************************************************
    public Browsing_caches(Path_list_provider path_list_provider, Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        Image_properties_RAM_cache tmp = image_properties_RAM_cache_of_caches.get(path_list_provider.get_folder_path().toAbsolutePath().toString());
        if ( tmp == null)
        {
            tmp = new Image_properties_RAM_cache(path_list_provider, "Image properties cache", owner,aborter, logger);
            image_properties_RAM_cache_of_caches.put(path_list_provider.get_folder_path().toAbsolutePath().toString(), tmp);
        }
        image_properties_RAM_cache = tmp;

    }

    //**********************************************************
    public static void scroll_position_cache_write(Path folder_path, Path top_left_item_path)
    //**********************************************************
    {
        scroll_position_cache.put(folder_path.toAbsolutePath().toString(), top_left_item_path);
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
