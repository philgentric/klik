// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.path_lists;
//SOURCES ../Move_provider_for_playlist.java
import javafx.stage.Window;
import klikr.Window_type;
import klikr.browser_core.virtual_landscape.Image_found;
import klikr.util.execute.actor.Aborter;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

//**********************************************************
public interface Path_list_provider
//**********************************************************
{
    // an abstract interface to provide a list of paths (files)
    // can be
    // (1) a disk folder
    // OR
    // (2) a 'playlist' = a list of paths that may not be in the same folder

    void set_cache_creation_time(long cache_creation_time);

    Path get_cache_save_path();

    boolean is_rescan_needed();

    String to_string();

    String get_key(); // never null (absolute path of *folder*) or (absolute path of *playlist file*)

    Optional<Path> get_folder_path();
    Path resolve(String string);

    Change_broadcaster get_change_broadcaster();

    Files_and_folders files_and_folders(boolean force_rescan, Image_found imgfnd, boolean consider_also_hidden_files, boolean consider_also_hidden_folders, Aborter aborter);
    void reload(String origin, Aborter aborter);

    List<Path> only_file_paths(boolean force_rescan, boolean consider_also_hidden_files, Aborter aborter);
    List<Path> only_image_paths(boolean force_rescan, boolean consider_also_hidden_files, Aborter aborter);
    List<Path> only_song_paths(boolean force_rescan, boolean consider_also_hidden_files, Aborter aborter);
    List<Path> only_folder_paths(boolean force_rescan, boolean consider_also_hidden_folders, Aborter aborter);

    Move_provider get_move_in_provider();
    void delete(Path path, Window owner, Aborter aborter, Logger logger);
    void delete_multiple(List<Path> paths, Window owner, Aborter aborter, Logger logger);

    int how_many_files_and_folders(boolean force_rescan, boolean consider_also_hidden_files, boolean consider_also_hidden_folders, Aborter aborter);

    static Path_list_provider get_appropriate(Window_type window_type, Path path, Window owner, Aborter aborter, Logger logger)
    {
        switch (window_type)
        {
            case File_system_2D, File_system_3D, File_system_diskview:
                return new Path_list_provider_for_file_system(path, owner, logger);
            case Image_playlist, Song_playlist:
                return new Path_list_provider_for_playlist(path, owner,aborter, logger);
        }
        return null;
    }

    //**********************************************************
    default boolean has_parent()
    //**********************************************************
    {
        Optional<Path> folder_path = get_folder_path();
        if (folder_path.isEmpty())
        {
            System.out.println(Stack_trace_getter.get_stack_trace(""));
            return false;
        }

        return folder_path.get().getParent()!=null;
    }

}
