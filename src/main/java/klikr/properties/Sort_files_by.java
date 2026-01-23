// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.properties;

import javafx.stage.Window;
import klikr.util.Shared_services;
import klikr.browser.icons.image_properties_cache.Image_properties;
import klikr.browser.virtual_landscape.Path_comparator_source;
import klikr.path_lists.Path_list_provider;
import klikr.browser.comparators.*;
import klikr.machine_learning.feature_vector.Feature_vector_cache;
import klikr.machine_learning.feature_vector.Feature_vector_source;
import klikr.machine_learning.image_similarity.Feature_vector_source_for_image_similarity;
import klikr.machine_learning.similarity.Similarity_cache;
import klikr.properties.boolean_features.Feature;
import klikr.properties.boolean_features.Feature_cache;
import klikr.util.cache.Clearable_RAM_caches;
import klikr.util.cache.Clearable_shared_caches;
import klikr.util.cache.Klikr_cache;
import klikr.util.log.Logger;
import klikr.util.execute.actor.Aborter;
import klikr.util.log.Stack_trace_getter;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// warning: these names are used as-is in the resource bundles !!!
public enum Sort_files_by {
    FILE_NAME,
    ASPECT_RATIO,
    FILE_CREATION_DATE,
    FILE_LAST_ACCESS_DATE,
    FILE_SIZE,
    IMAGE_WIDTH,
    IMAGE_HEIGHT,
    RANDOM,
    RANDOM_ASPECT_RATIO,
    NAME_GIFS_FIRST,
    SIMILARITY_BY_PAIRS,
    SIMILARITY_BY_PURSUIT,;

  public static final String SORT_FILES_BY = "sort_files_by";

    public final static boolean dbg = false;


    //**********************************************************
    public static Comparator<Path> get_non_image_comparator(Path_list_provider path_list_provider,Window owner, Aborter aborter,Logger logger)
    //**********************************************************
    {
        switch(Sort_files_by.get_sort_files_by(path_list_provider.get_folder_path(), owner))
        {
            case NAME_GIFS_FIRST, ASPECT_RATIO, RANDOM_ASPECT_RATIO, IMAGE_HEIGHT , IMAGE_WIDTH, SIMILARITY_BY_PURSUIT, SIMILARITY_BY_PAIRS,
                 FILE_NAME:
                return new Alphabetical_file_name_comparator();
            case RANDOM:
                return new Random_comparator();
            case FILE_CREATION_DATE:
                return new Date_comparator(logger);
            case FILE_LAST_ACCESS_DATE:
                return new Last_access_comparator(logger);
            case FILE_SIZE:
                return new Decreasing_disk_footprint_comparator(aborter,owner);
        }
        return null;
    }


    //**********************************************************
    public static Comparator<Path> get_image_comparator(
        Path_list_provider path_list_provider,
        Path_comparator_source path_comparator_source,
        Klikr_cache<Path, Image_properties> image_properties_cache,
        Window owner,
        double x, double y, Aborter aborter, Logger logger)
    //**********************************************************
    {

        switch(Sort_files_by.get_sort_files_by(path_list_provider.get_folder_path(), owner))
        {
            case SIMILARITY_BY_PURSUIT: {
                Feature_vector_source fvs = new Feature_vector_source_for_image_similarity(owner, logger);
                return get_similarity_comparator_by_pursuit(fvs, path_list_provider, path_comparator_source, image_properties_cache, owner, x, y, aborter, logger);
            }
            case SIMILARITY_BY_PAIRS: {
                Feature_vector_source fvs = new Feature_vector_source_for_image_similarity(owner, logger);
                return get_similarity_comparator_pairs_of_closests(fvs, path_list_provider, owner, x, y, aborter, logger);
            }
            case FILE_NAME:
                return new Alphabetical_file_name_comparator();
            case ASPECT_RATIO:
                return new Aspect_ratio_comparator(image_properties_cache,aborter,owner);
            case RANDOM_ASPECT_RATIO:
                return new Aspect_ratio_comparator_random(image_properties_cache,aborter,owner);
            case IMAGE_HEIGHT:
                return new Image_height_comparator(image_properties_cache,aborter,owner,logger);
            case IMAGE_WIDTH:
                return new Image_width_comparator(image_properties_cache,aborter,owner);
            case RANDOM:
                return new Random_comparator();
            case FILE_CREATION_DATE:
                return new Date_comparator(logger);
            case FILE_LAST_ACCESS_DATE:
                return new Last_access_comparator(logger);
            case FILE_SIZE:
                return new Decreasing_disk_footprint_comparator(aborter,owner);
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
        Klikr_cache<Path, Image_properties> image_properties_cache,
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
        Similarity_cache similarity_cache = Clearable_shared_caches.similarity_cache_of_caches.get(path_list_provider.get_folder_path().toAbsolutePath().toString());
        if (similarity_cache == null)
        {
            similarity_cache = new Similarity_cache(fvs, paths, path_list_provider, owner, x, y, Shared_services.aborter(), logger);
            Clearable_shared_caches.similarity_cache_of_caches.put(path_list_provider.get_folder_path().toAbsolutePath().toString(), similarity_cache);
        }
        return similarity_cache;
    }

    //**********************************************************
    public static boolean need_image_properties(Path folder_path, Window owner)
    //**********************************************************
    {
        switch(Sort_files_by.get_sort_files_by(folder_path, owner))
        {
            case SIMILARITY_BY_PURSUIT:
                return false;
            case SIMILARITY_BY_PAIRS:
                return false;
            case FILE_NAME:
                return false;
            case ASPECT_RATIO:
                return true;
            case RANDOM_ASPECT_RATIO:
                return true;
            case IMAGE_HEIGHT:
                return true;
            case IMAGE_WIDTH:
                return true;
            case RANDOM:
                return false;
            case FILE_CREATION_DATE:
                return false;
            case FILE_SIZE:
                return false;
            case NAME_GIFS_FIRST:
                return false;
        }
        return false;
    }

    private static Map<Path, Sort_files_by> cached = new HashMap<>();
    //**********************************************************
    public static Sort_files_by get_sort_files_by(Path folder_path, Window owner)
    //**********************************************************
    {
        Sort_files_by from_cache = cached.get(folder_path);
        if ( from_cache != null)
        {
            if (dbg) System.out.println(Stack_trace_getter.get_stack_trace("sort files by (1): "+ Sort_files_by.FILE_NAME));
            return from_cache;
        }

        String s = Shared_services.main_properties().get(SORT_FILES_BY);
        if (s == null)
        {
            Shared_services.main_properties().set(SORT_FILES_BY, Sort_files_by.FILE_NAME.name());
            if (dbg) System.out.println(Stack_trace_getter.get_stack_trace("sort files by (2): "+ Sort_files_by.FILE_NAME));
            cached.put(folder_path, Sort_files_by.FILE_NAME);
            return Sort_files_by.FILE_NAME;
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
            if (dbg) System.out.println(Stack_trace_getter.get_stack_trace("sort files by (4): "+ Sort_files_by.FILE_NAME));

            return Sort_files_by.FILE_NAME;
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
