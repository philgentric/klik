package klik.browser.comparators;

//SOURCES ../../image_ml/image_similarity/Feature_vector_source_for_image_similarity.java;

import klik.browser.Clearable_RAM_cache;
import klik.browser.virtual_landscape.Path_list_provider;
import klik.browser.virtual_landscape.Virtual_landscape;
import klik.image_ml.image_similarity.Image_feature_vector_cache;
import klik.util.log.Logger;

import java.io.File;
import java.nio.file.Path;
import java.util.*;


// a per-folder cache of image distances
//**********************************************************
public abstract class Similarity_comparator implements Comparator<Path>, Clearable_RAM_cache
//**********************************************************
{
    protected final Map<Path, Integer> dummy_names = new HashMap<>();

    private final Map<Path_pair, Integer> distances_cache = new HashMap<>();
    protected Image_feature_vector_cache fv_cache = null;
    Logger logger;
    protected final Similarity_cache similarity_cache;
    protected final List<Path> images;

    //**********************************************************
    public Similarity_comparator(Similarity_cache similarity_cache, Path_list_provider path_list_provider, Logger logger)
    //**********************************************************
    {
        this.logger = logger;
        this.similarity_cache = similarity_cache;
        this.images = path_list_provider.only_image_paths(Virtual_landscape.show_hidden_files);
        shuffle();
    }


    //**********************************************************
    @Override
    public void clear_RAM_cache()
    //**********************************************************
    {
        if(fv_cache != null) fv_cache.clear_feature_vector_RAM_cache();
        distances_cache.clear();
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
        Integer d = distances_cache.get(pp);
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
        distances_cache.put(pp, d);
        //logger.log("compare "+p1+" vs "+p2+" == "+d);
        return d;
    }

    public void shuffle() {
        Collections.shuffle(images);
    }

    protected void add_non_images(Path_list_provider path_list_provider, int i) {
        // then we add the non-images
        for ( File f : path_list_provider.only_files(Virtual_landscape.show_hidden_files))
        {
            if ( images.contains(f.toPath())) continue;
            dummy_names.put(f.toPath(), i);
            //logger.log(f.toPath()+" -> "+i);
            i++;
        }
    }


}
