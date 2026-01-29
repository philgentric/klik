// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.util.cache;

import javafx.stage.Window;
import klikr.properties.Non_booleans_properties;
import klikr.util.execute.actor.Aborter;
import klikr.util.files_and_paths.Static_files_and_paths_utilities;
import klikr.util.image.icon_cache.Icon_caching;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;
import klikr.util.mmap.Mmap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// caches that have a disk 'backup'
// we need to have a static list because when we 'clear all disk caches'
// we dont want just the 'live' ones
//**********************************************************
public enum Cache_folder
//**********************************************************
{

    icon_cache, // each image has a file
    folder_icon_cache, // each folder has a file

    // each folder has a file with one entry per image
    image_properties_cache,
    feature_vectors_cache,

    face_recognition_cache,

    // each playlist has a file with one entry per song
    song_duration_cache,
    // each playlist has a file with one entry per song
    song_bitrate_cache,


    // each folder has a file with one entry per 'close' image pair
    similarity_cache;

    private static final boolean dbg = false;

    //**********************************************************
    public static List<Disk_cleared> clear_all_disk_caches(Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        double total = 0;
        List<Disk_cleared> returned = new ArrayList<>();
        for ( Cache_folder cache_folder : Cache_folder.values())
        {
            Disk_cleared dc = clear_disk_cache( cache_folder, false,  owner,  aborter,  logger);
            returned.add(dc);
            total += dc.bytes();
        }

        total += Mmap.instance.clear_cache();

        String size_in_bytes = Static_files_and_paths_utilities.get_1_line_string_for_byte_data_size(total,owner,logger);
        logger.log("\n\n✅ Total cleared disk bytes: " + size_in_bytes+"\n\n");
        return returned;
    }


    //**********************************************************
    public static Disk_cleared clear_disk_cache(Cache_folder cache_folder, boolean show_popup, Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        Path path = get_cache_dir(cache_folder,owner,logger);
        return new Disk_cleared(path, Static_files_and_paths_utilities.clear_folder(path, cache_folder.name()+" cache on disk", show_popup, true, owner, aborter, logger));
    }

    //**********************************************************
    public static Path get_cache_dir(Cache_folder cache_folder, Window owner,Logger logger)
    //**********************************************************
    {
        Path tmp_dir = Static_files_and_paths_utilities.get_absolute_hidden_dir_on_user_home(cache_folder.name(), false, owner,logger);
        if (tmp_dir == null)
        {
            logger.log("WARNING get_absolute_hidden_dir_on_user_homer=" + null);
        }
        else
        {
            if (dbg) logger.log("get_absolute_hidden_dir_on_user_home=" + tmp_dir.toAbsolutePath());
        }
        return tmp_dir;
    }

    //**********************************************************
    public static void clear_one_icon_from_cache_on_disk(Path path, Window owner,Logger logger)
    //**********************************************************
    {
        Path icon_cache_dir = get_cache_dir( Cache_folder.icon_cache,owner,logger);
        int icon_size = Non_booleans_properties.get_icon_size(owner);

        Path icon_path = Icon_caching.path_for_icon_caching(path,String.valueOf(icon_size),Icon_caching.png_extension,owner,logger);
        try {
            Files.delete(icon_path);
            logger.log("one icon deleted from cache:" + icon_path);

        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace("WARNING: deleting one icon FAILED: " + e));
        }
    }

}
