// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.browser_core.comparators;

//SOURCES ../../image_ml/image_similarity/Feature_vector_source_for_image_similarity.java;
//SOURCES ../../image_ml/image_similarity/Path_pair.java;

import klikr.machine_learning.similarity.Integer_pair;
import klikr.util.P2S;
import klikr.util.cache.Clearable_RAM_cache;
import klikr.path_lists.Path_list_provider;
import klikr.machine_learning.feature_vector.Feature_vector_cache;
import klikr.machine_learning.similarity.Similarity_cache;
import klikr.settings.boolean_features.Feature;
import klikr.settings.boolean_features.Feature_cache;
import klikr.util.cache.Size_;
import klikr.util.execute.actor.Aborter;
import klikr.util.log.Logger;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;


// a per-folder cache of item distances
//**********************************************************
public abstract class Similarity_comparator implements Comparator<Path>, Clearable_RAM_cache
//**********************************************************
{
    protected final Map<String, Integer> dummy_names = new HashMap<>();
    private final Map<Integer_pair, Integer> distances_cache = new HashMap<>();
    private final List<Path> paths = new ArrayList<>();

    protected final Supplier<Feature_vector_cache> fv_cache_supplier;
    protected final Logger logger;
    protected final Similarity_cache similarity_cache;
    protected final List<Path> images;
    protected final Aborter aborter;

    //**********************************************************
    public Similarity_comparator(Supplier<Feature_vector_cache> fv_cache_supplier, Similarity_cache similarity_cache, Path_list_provider path_list_provider, Aborter aborter, Logger logger)
    //**********************************************************
    {
        this.aborter = aborter;
        this.fv_cache_supplier = fv_cache_supplier;
        this.logger = logger;
        this.similarity_cache = similarity_cache;
        this.images = path_list_provider.only_image_paths(false,Feature_cache.get(Feature.Show_hidden_files),aborter);
        shuffle();
    }


    //**********************************************************
    @Override
    public double clear_RAM()
    //**********************************************************
    {
        double returned = 0;
        if(fv_cache_supplier.get() != null)
        {
            returned += fv_cache_supplier.get().clear_RAM();
        }


        Function<Integer_pair, Long> size_of_Integer_pair = integer_pair -> 8L;
        returned += Size_.of_Map(distances_cache,size_of_Integer_pair, Size_.of_Integer_F());
        distances_cache.clear();

        returned += Size_.of_Map(dummy_names,Size_.of_String_F(), Size_.of_Integer_F());
        dummy_names.clear();

        if ( similarity_cache != null)
        {
            returned += similarity_cache.clear_RAM();
        }

        returned += images.size()* Size_.of_Path();
        images.clear();
        return returned;
    }


    //**********************************************************
    @Override
    public int compare(Path p1, Path p2)
    //**********************************************************
    {
        Integer x = Hidden_files.show_last(p1, p2);
        if (x != null) return x;

        int i = paths.indexOf(p1);
        if ( i == -1)
        {
            paths.add(p1);
            i = paths.indexOf(p1);
        }
        int j = paths.indexOf(p2);
        if ( j == -1)
        {
            paths.add(p2);
            j = paths.indexOf(p2);
        }

        Integer_pair pp = Integer_pair.build(i,j);
        Integer d = distances_cache.get(pp);
        if (d != null) return d;

        Integer dummy_name1 = dummy_names.get(p1);

        if ( dummy_name1 == null)
        {
            //logger.log("WTF dummy_name1 == null for "+p1);
            dummy_name1 = 8888888;//p1.getFileName().toString();
            dummy_names.put(P2S.p2s(p1),dummy_name1);
        }

        Integer dummy_name2 = dummy_names.get(P2S.p2s(p2));
        if ( dummy_name2 == null)
        {
            logger.log(" dummy_name2 == null for "+p2);
            dummy_name2 = 9999999;//p2.getFileName().toString();
            dummy_names.put(P2S.p2s(p2),dummy_name2);
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
        for ( Path path : path_list_provider.only_file_paths(false,Feature_cache.get(Feature.Show_hidden_files),aborter))
        {
            if ( images.contains(path)) continue;
            dummy_names.put(P2S.p2s(path), i);
            //logger.log(path+" -> "+i);
            i++;
        }
    }


}
