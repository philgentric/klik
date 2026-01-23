// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.util.cache;

// caches that have a disk 'backup'
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
    similarity_cache
}
