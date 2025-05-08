package klik.browser.comparators;

//SOURCES ../../image_ml/image_similarity/Feature_vector_source_for_image_similarity.java;

import klik.actor.Aborter;
import klik.browser.Clearable_RAM_cache;
import klik.browser.Path_list_provider;
import klik.image_ml.image_similarity.Image_feature_vector_cache;
import klik.util.log.Logger;

import java.nio.file.Path;
import java.util.*;


//**********************************************************
public abstract class Similarity_comparator implements Comparator<Path>, Clearable_RAM_cache
//**********************************************************
{
    protected final Map<Path, Integer> dummy_names = new HashMap<>();

    private final Map<Path_pair, Integer> distances  = new HashMap<>();
    protected Image_feature_vector_cache fv_cache = null;
    Logger logger;
    protected Similarity_cache similarity_cache;
    protected List<Path> images;

    //**********************************************************
    public Similarity_comparator(Path_list_provider path_list_provider, double x, double y, Aborter aborter, Logger logger_)
    //**********************************************************
    {
        logger = logger_;

        Image_feature_vector_cache.Images_and_feature_vectors result = Image_feature_vector_cache.preload_all_feature_vector_in_cache(path_list_provider, x,y,aborter, logger);
        if (result == null) {
            return;
        }
        fv_cache = result.image_feature_vector_ram_cache();
        images = new ArrayList<>(result.images());

        similarity_cache = new Similarity_cache(path_list_provider, images, fv_cache, aborter, logger);
        shuffle();
    }

    //**********************************************************
    @Override
    public void clear_RAM_cache()
    //**********************************************************
    {
        if(fv_cache != null) fv_cache.clear_feature_vector_RAM_cache();
        distances.clear();
        dummy_names.clear();
        if ( similarity_cache != null) similarity_cache.clear();
        images.clear();
    }


    //**********************************************************
    @Override
    public int compare(Path p1, Path p2)
    //**********************************************************
    {
        Path_pair pp = Path_pair.get(p1,p2);
        Integer d = distances.get(pp);
        if (d != null) return d;

        Integer dummy_name1 = dummy_names.get(p1);

        if ( dummy_name1 == null)
        {
            //logger.log("WTF dummy_name1 == null for "+p1);
            dummy_name1 = 8888888;//p1.getFileName().toString();
            dummy_names.put(p1,dummy_name1);
        }

        Integer dummy_name2 = dummy_names.get(p2);
        if ( dummy_name2 == null)
        {
            logger.log(" dummy_name2 == null for "+p2);
            dummy_name2 = 9999999;//p2.getFileName().toString();
            dummy_names.put(p2,dummy_name2);
        }

        d =  dummy_name1.compareTo(dummy_name2);
        distances.put(pp, d);
        //logger.log("compare "+p1+" vs "+p2+" == "+d);
        return d;
    }

    public void shuffle() {
        Collections.shuffle(images);
    }


}
