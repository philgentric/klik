// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.properties;

import javafx.stage.Window;
import klik.Shared_services;
import klik.browser.virtual_landscape.Browsing_caches;
import klik.browser.virtual_landscape.Path_comparator_source;
import klik.path_lists.Path_list_provider;
import klik.browser.comparators.*;
import klik.browser.icons.image_properties_cache.Image_properties_RAM_cache;
import klik.machine_learning.feature_vector.Feature_vector_cache;
import klik.machine_learning.feature_vector.Feature_vector_source;
import klik.machine_learning.image_similarity.Feature_vector_source_for_image_similarity;
import klik.machine_learning.similarity.Similarity_cache;
import klik.properties.boolean_features.Feature;
import klik.properties.boolean_features.Feature_cache;
import klik.util.log.Logger;
import klik.util.execute.actor.Aborter;
import klik.util.log.Stack_trace_getter;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// warning: these names are used as-is in the resource bundles !!!
public enum Sort_files_by {
  NAME,
  DATE,
  SIZE,
  SIMILARITY_BY_PAIRS,
  SIMILARITY_BY_PURSUIT,
  IMAGE_WIDTH,
  IMAGE_HEIGHT,
  ASPECT_RATIO,
  RANDOM,
  RANDOM_ASPECT_RATIO,
  NAME_GIFS_FIRST;

  public static final String SORT_FILES_BY = "sort_files_by";

    public final static boolean dbg = false;


    //**********************************************************
    public static Comparator<Path> get_non_image_comparator(Path_list_provider path_list_provider,Window owner, Aborter aborter,Logger logger)
    //**********************************************************
    {
        switch(Sort_files_by.get_sort_files_by(path_list_provider.get_folder_path(), owner))
        {
            case NAME_GIFS_FIRST, ASPECT_RATIO, RANDOM_ASPECT_RATIO, IMAGE_HEIGHT , IMAGE_WIDTH,SIMILARITY_BY_PURSUIT, SIMILARITY_BY_PAIRS, NAME:
                return new Alphabetical_file_name_comparator();
            case RANDOM:
                return new Random_comparator();
            case DATE:
                return new Date_comparator(logger);
            case SIZE:
                return new Decreasing_disk_footprint_comparator(aborter);
        }
        return null;
    }


    //**********************************************************
    public static Comparator<Path> get_image_comparator(
        Path_list_provider path_list_provider,
        Path_comparator_source path_comparator_source,
        Image_properties_RAM_cache image_properties_cache,
        Window owner,
        double x, double y, Aborter aborter, Logger logger)
    //**********************************************************
    {
        Feature_vector_source fvs = new Feature_vector_source_for_image_similarity(Shared_services.aborter());

        switch(Sort_files_by.get_sort_files_by(path_list_provider.get_folder_path(), owner))
        {
            case SIMILARITY_BY_PURSUIT:
                return get_similarity_comparator_by_pursuit(fvs,path_list_provider, path_comparator_source, image_properties_cache, owner, x, y, aborter, logger);
            case SIMILARITY_BY_PAIRS:
                return get_similarity_comparator_pairs_of_closests(fvs,path_list_provider, owner, x, y, aborter, logger);
            case NAME:
                return new Alphabetical_file_name_comparator();
            case ASPECT_RATIO:
                return new Aspect_ratio_comparator(image_properties_cache,aborter);
            case RANDOM_ASPECT_RATIO:
                return new Aspect_ratio_comparator_random(image_properties_cache,aborter);
            case IMAGE_HEIGHT:
                return new Image_height_comparator(image_properties_cache,aborter,logger);
            case IMAGE_WIDTH:
                return new Image_width_comparator(image_properties_cache,aborter);
            case RANDOM:
                return new Random_comparator();
            case DATE:
                return new Date_comparator(logger);
            case SIZE:
                return new Decreasing_disk_footprint_comparator(aborter);
            case NAME_GIFS_FIRST:
                return new Alphabetical_file_name_comparator_gif_first();
            }
        return null;
    }


    //**********************************************************
    private static Similarity_comparator_pairs_of_closests get_similarity_comparator_pairs_of_closests(Feature_vector_source fvs, Path_list_provider path_list_provider, Window owner, double x, double y, Aborter aborter, Logger logger)
    //**********************************************************
    {
        List<Path> paths = path_list_provider.only_image_paths(Feature_cache.get(Feature.Show_hidden_files));
        Similarity_cache similarity_cache = get_similarity_cache(fvs,paths,path_list_provider, owner, x, y, logger);
        Feature_vector_cache fv_cache = Feature_vector_cache.preload_all_feature_vector_in_cache(fvs, paths, path_list_provider, owner, x, y, aborter, logger).fv_cache();
        return new Similarity_comparator_pairs_of_closests(
            ()->fv_cache,
            similarity_cache,
            path_list_provider,
            x, y,
            aborter, logger);
    }

    //**********************************************************
    private static Similarity_comparator_by_pursuit get_similarity_comparator_by_pursuit(
        Feature_vector_source fvs,
        Path_list_provider path_list_provider,
        Path_comparator_source path_comparator_source,
        Image_properties_RAM_cache image_properties_cache,
        Window owner, double x, double y,
        Aborter aborter, Logger logger)
    //**********************************************************
    {
        List<Path> paths = path_list_provider.only_image_paths(Feature_cache.get(Feature.Show_hidden_files));
        Similarity_cache similarity_cache = get_similarity_cache(fvs, paths, path_list_provider, owner, x, y, logger);
        Feature_vector_cache fv_cache = Feature_vector_cache.preload_all_feature_vector_in_cache(fvs,paths, path_list_provider, owner, x, y, aborter, logger).fv_cache();
        return new Similarity_comparator_by_pursuit(
            ()->fv_cache,
            similarity_cache,
            path_list_provider,
            path_comparator_source,
            image_properties_cache,
            owner,
            x, y,
            aborter, logger);
    }

    //**********************************************************
    private static Similarity_cache get_similarity_cache(
            Feature_vector_source fvs,
            List<Path> paths,
            Path_list_provider path_list_provider, Window owner,double x, double y, Logger logger)
    //**********************************************************
    {
        Similarity_cache similarity_cache = Browsing_caches.similarity_cache_of_caches.get(path_list_provider.get_folder_path().toAbsolutePath().toString());
        if (similarity_cache == null)
        {
            similarity_cache = new Similarity_cache(fvs, paths, path_list_provider, owner, x, y, Shared_services.aborter(), logger);
            Browsing_caches.similarity_cache_of_caches.put(path_list_provider.get_folder_path().toAbsolutePath().toString(), similarity_cache);
        }
        return similarity_cache;
    }

    private static Map<Path, Sort_files_by> cached = new HashMap<>();
    //**********************************************************
    public static Sort_files_by get_sort_files_by(Path folder_path, Window owner)
    //**********************************************************
    {
        Sort_files_by from_cache = cached.get(folder_path);
        if ( from_cache != null)
        {
            if (dbg) System.out.println(Stack_trace_getter.get_stack_trace("sort files by (1): "+ Sort_files_by.NAME));
            return from_cache;
        }

        String s = Shared_services.main_properties().get(SORT_FILES_BY);
        if (s == null)
        {
            Shared_services.main_properties().set(SORT_FILES_BY, Sort_files_by.NAME.name());
            if (dbg) System.out.println(Stack_trace_getter.get_stack_trace("sort files by (2): "+ Sort_files_by.NAME));
            cached.put(folder_path, Sort_files_by.NAME);
            return Sort_files_by.NAME;
        }

        try
        {
            Sort_files_by returned = Sort_files_by.valueOf(s);

            if (dbg) System.out.println(Stack_trace_getter.get_stack_trace("sort files by (3): "+returned));
            cached.put(folder_path, returned);
            return returned;
        }
        catch ( IllegalArgumentException e)
        {
            if (dbg) System.out.println(Stack_trace_getter.get_stack_trace("sort files by (4): "+ Sort_files_by.NAME));

            return Sort_files_by.NAME;
        }

    }

    //**********************************************************
    public static void set_sort_files_by(Path folder_path, Sort_files_by b, Window owner, Logger logger)
    //**********************************************************
    {
        cached.put(folder_path, b);

        if ( b == Sort_files_by.SIMILARITY_BY_PAIRS)
        {
            logger.log("warning: SIMILARITY_BY_PAIRS not saved to properties");
            return;
        }
        if ( b == Sort_files_by.SIMILARITY_BY_PURSUIT)
        {
            logger.log("warning: SIMILARITY_BY_PURSUIT not saved to properties");
            return;
        }
        Shared_services.main_properties().set(SORT_FILES_BY, b.name());
    }

}
