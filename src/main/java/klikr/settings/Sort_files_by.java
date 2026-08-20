// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.settings;

import javafx.stage.Window;
import klikr.settings.boolean_features.Settings_not_saved_to_disk;
import klikr.util.Shared_services;
import klikr.browser_core.icons.image_properties_cache.Image_properties;
import klikr.browser_core.virtual_landscape.Path_comparator_source;
import klikr.path_lists.Path_list_provider;
import klikr.browser_core.comparators.*;
import klikr.machine_learning.feature_vector.Feature_vector_cache;
import klikr.machine_learning.feature_vector.Feature_vector_source;
import klikr.machine_learning.similarity.Similarity_cache;
import klikr.settings.boolean_features.Feature;
import klikr.settings.boolean_features.Feature_cache;
import klikr.util.cache.RAM_caches;
import klikr.util.cache.Klikr_cache;
import klikr.util.log.Logger;
import klikr.util.execute.actor.Aborter;
import klikr.util.log.Stack_trace_getter;

import java.nio.file.Path;
import java.util.*;

// warning: these names are used as-is in the resource bundles !!!
public enum Sort_files_by {
    FILE_NAME,
    ASPECT_RATIO,
    FILE_CREATION_DATE,
    FILE_LAST_ACCESS_DATE,
    FILE_SIZE,
    FILE_AND_FOLDER_SIZE, // not saved to disk
    IMAGE_WIDTH,
    IMAGE_HEIGHT,
    RANDOM,
    RANDOM_ASPECT_RATIO,
    PLAYLIST_ORDER;
    //NAME_GIFS_FIRST
    //SIMILARITY_BY_PAIRS,
    //SIMILARITY_BY_PURSUIT,

    public final static boolean dbg = false;

    public static final String SORT_FILES_BY_FOR_ALL_FOLDERS = "sort_files_by_for_all_folders";
    public static final String SORT_FILES_BY_FOR_FOLDER_ = "sort_files_by_for_folder_";



    //**********************************************************
    public static Comparator<Path> get_non_image_comparator(Path_list_provider path_list_provider,Window owner, Aborter aborter,Logger logger)
    //**********************************************************
    {

        switch(Sort_files_by.get_sort_files_by(path_list_provider.get_key(), owner,logger))
        {
            case //NAME_GIFS_FIRST, SIMILARITY_BY_PURSUIT, SIMILARITY_BY_PAIRS,
                 ASPECT_RATIO, RANDOM_ASPECT_RATIO, IMAGE_HEIGHT , IMAGE_WIDTH, FILE_NAME:
                return new Alphabetical_file_name_comparator();
            case RANDOM:
                return new Random_comparator();
            case FILE_CREATION_DATE:
                return new Date_comparator(logger);
            case FILE_LAST_ACCESS_DATE:
                return new Last_access_comparator(logger);
            case FILE_SIZE:
                return new File_size_comparator();
            case FILE_AND_FOLDER_SIZE:
                return new Decreasing_disk_footprint_comparator(aborter,owner);
        }
        return null;
    }


    //**********************************************************
    public static Comparator<Path> get_image_comparator(
        Path_list_provider path_list_provider,
        Path_comparator_source path_comparator_source,
        Klikr_cache<Path, Image_properties> image_properties_cache,
        Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        switch(Sort_files_by.get_sort_files_by(path_list_provider.get_key(), owner,logger))
        {
            /*
            case SIMILARITY_BY_PURSUIT: {
                Feature_vector_source fvs = new Feature_vector_source_for_image_similarity(owner, logger);
                return get_similarity_comparator_by_pursuit(fvs, path_list_provider, path_comparator_source, image_properties_cache, owner, x, y, aborter, logger);
            }
            case SIMILARITY_BY_PAIRS: {
                Feature_vector_source fvs = new Feature_vector_source_for_image_similarity(owner, logger);
                return get_similarity_comparator_pairs_of_closests(fvs, path_list_provider, owner, x, y, aborter, logger);
            }*/
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
                return new File_size_comparator();
            //case NAME_GIFS_FIRST:
            //    return new Alphabetical_file_name_comparator_gif_first();
            }
        return null;
    }


    //**********************************************************
    private static Similarity_comparator_pairs_of_closests get_similarity_comparator_pairs_of_closests(Feature_vector_source fvs, Path_list_provider path_list_provider, Window owner, double x, double y, Aborter aborter, Logger logger)
    //**********************************************************
    {
        List<Path> paths = path_list_provider.only_image_paths(false,Feature_cache.get(Feature.Show_hidden_files),aborter);
        Similarity_cache similarity_cache = get_similarity_cache(fvs,path_list_provider, owner, logger);
        Feature_vector_cache fv_cache = Feature_vector_cache.preload_all_feature_vector_in_cache(fvs, paths, path_list_provider, owner, aborter, logger);
        return new Similarity_comparator_pairs_of_closests(
            ()->fv_cache,
            similarity_cache,
            path_list_provider,
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
        List<Path> paths = path_list_provider.only_image_paths(false,Feature_cache.get(Feature.Show_hidden_files),aborter);
        Similarity_cache similarity_cache = get_similarity_cache(fvs, path_list_provider, owner, logger);
        Feature_vector_cache fv_cache = Feature_vector_cache.preload_all_feature_vector_in_cache(fvs,paths, path_list_provider, owner, aborter, logger);
        return new Similarity_comparator_by_pursuit(
            ()->fv_cache,
            similarity_cache,
            path_list_provider,
            path_comparator_source,
            image_properties_cache,
            owner, aborter, logger);
    }

    //**********************************************************
    private static Similarity_cache get_similarity_cache(
            Feature_vector_source fvs,
            Path_list_provider path_list_provider, Window owner, Logger logger)
    //**********************************************************
    {
        Optional<Path> folder_path = path_list_provider.get_folder_path();
        if ( folder_path.isEmpty() )
        {
            logger.log(Stack_trace_getter.get_stack_trace("folder_path == null"));
            return null;
        }
        Similarity_cache similarity_cache = RAM_caches.similarity_cache_of_caches.get(folder_path.get().toAbsolutePath().toString());
        if (similarity_cache == null)
        {
            similarity_cache = new Similarity_cache(fvs, path_list_provider, owner, Shared_services.aborter(), logger);
            RAM_caches.similarity_cache_of_caches.put(folder_path.get().toAbsolutePath().toString(), similarity_cache);
        }
        return similarity_cache;
    }

    //**********************************************************
    public static boolean need_image_properties(String key, Window owner, Logger logger)
    //**********************************************************
    {
        switch(Sort_files_by.get_sort_files_by(key, owner,logger))
        {
            case ASPECT_RATIO, RANDOM_ASPECT_RATIO, IMAGE_HEIGHT, IMAGE_WIDTH:
                return true;

            case FILE_NAME, RANDOM, FILE_CREATION_DATE, FILE_SIZE, FILE_AND_FOLDER_SIZE, PLAYLIST_ORDER:
                return false;
        }
        return false;
    }

    // during a session we cache 'sort_files_by' PER FOLDER
    // and it is saved to file too
    // EXCEPT for items in never_saved_to_disk
    private static final Map<String, Sort_files_by> cacheds = new HashMap<>();
    private static Sort_files_by cached = null;
    //**********************************************************
    public static Sort_files_by get_sort_files_by(String key, Window owner, Logger logger)
    //**********************************************************
    {

        if ( !Feature_cache.get(Feature.Remember_sorting_method_per_folder))
        {
            // NOT per folder
            if ( cached != null) return cached;
            String s = Shared_services.main_properties().get(SORT_FILES_BY_FOR_ALL_FOLDERS);
            if (s == null) {
                Shared_services.main_properties().set_and_save(SORT_FILES_BY_FOR_ALL_FOLDERS, Sort_files_by.FILE_NAME.name());
                if (dbg) logger.log(("sort files by (2): " + Sort_files_by.FILE_NAME));
                cached = Sort_files_by.FILE_NAME;
                return Sort_files_by.FILE_NAME;
            }
            if (s.isBlank()) {
                Shared_services.main_properties().set_and_save(SORT_FILES_BY_FOR_ALL_FOLDERS, Sort_files_by.FILE_NAME.name());
                if (dbg) logger.log(("sort files by (3): " + Sort_files_by.FILE_NAME));
                cached = Sort_files_by.FILE_NAME;
                return Sort_files_by.FILE_NAME;
            }

            try
            {
                Sort_files_by returned = Sort_files_by.valueOf(s);
                cached = returned;
                if (dbg) logger.log(Stack_trace_getter.get_stack_trace("sort files by (4): "+returned));
                return returned;
            }
            catch (IllegalArgumentException e)
            {
                logger.log("sort files by (4): "+e);
            }

            cached = Sort_files_by.FILE_NAME;
            return Sort_files_by.FILE_NAME;
        }

        // per folder

        Sort_files_by from_cache = cacheds.get(key);
        if (from_cache != null) {
            if (dbg)
                logger.log(Stack_trace_getter.get_stack_trace("CACHED sort files by (1): " + from_cache.name() + " for:" + key));
            return from_cache;
        }

        String s = Shared_services.main_properties().get(SORT_FILES_BY_FOR_FOLDER_ + key);
        if (s == null) {
            Shared_services.main_properties().set_and_save(SORT_FILES_BY_FOR_FOLDER_ + key, Sort_files_by.FILE_NAME.name());
            if (dbg) logger.log(("sort files by (2): " + Sort_files_by.FILE_NAME));
            cacheds.put(key, Sort_files_by.FILE_NAME);
            return Sort_files_by.FILE_NAME;
        }
        if (s.isBlank()) {
            Shared_services.main_properties().set_and_save(SORT_FILES_BY_FOR_FOLDER_ + key, Sort_files_by.FILE_NAME.name());
            if (dbg) logger.log(("sort files by (3): " + Sort_files_by.FILE_NAME));
            cacheds.put(key, Sort_files_by.FILE_NAME);
            return Sort_files_by.FILE_NAME;
        }

        try
        {
            Sort_files_by returned = Sort_files_by.valueOf(s);
            if (dbg) logger.log(Stack_trace_getter.get_stack_trace("sort files by (4): "+returned));
            cacheds.put(key, returned);
            return returned;
        }
        catch (IllegalArgumentException e)
        {
            logger.log("sort files by (4): "+e);
        }

        cacheds.put(key, Sort_files_by.FILE_NAME);
        return Sort_files_by.FILE_NAME;
    }

    //**********************************************************
    public static void set_sort_files_by_for_folder(String key, Sort_files_by sort_files_by, boolean and_save, Window owner, Logger logger)
    //**********************************************************
    {
        for (int i = 0 ; i < Settings_not_saved_to_disk.never_saved_to_disk.length; i++)
        {
            if ( sort_files_by == Settings_not_saved_to_disk.never_saved_to_disk[i])
            {
                logger.log("warning: "+sort_files_by.name()+" not saved on disk");
                and_save = false;
                break;
            }
        }

        if ( !Feature_cache.get(Feature.Remember_sorting_method_per_folder))
        {
            cached = sort_files_by;
            // all folders are sorted the same way
            if ( and_save) Shared_services.main_properties().set_and_save(SORT_FILES_BY_FOR_ALL_FOLDERS, sort_files_by.name());
            return;
        }



        if(dbg) logger.log("sort files by CACHE : " + sort_files_by.name() + " for folder: " + key);
        cacheds.put(key, sort_files_by);
        if ( and_save )
        {
            if(dbg) logger.log("sort files by SAVING : " + sort_files_by.name() + " for folder: " + key);
            Shared_services.main_properties().set_and_save(SORT_FILES_BY_FOR_FOLDER_ + key, sort_files_by.name());
        }
    }

}
