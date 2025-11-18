// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.path_lists;

import javafx.stage.Window;
import klik.browser.virtual_landscape.Image_found;
import klik.util.execute.actor.Aborter;
import klik.browser.Move_provider;
import klik.util.files_and_paths.Guess_file_type;
import klik.util.files_and_paths.Moving_files;
import klik.util.files_and_paths.Static_files_and_paths_utilities;
import klik.util.log.Logger;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

//**********************************************************
public class Path_list_provider_for_file_system implements Path_list_provider
//**********************************************************
{
    private final Path folder_path;
    private final Logger logger;
    private final Window owner;
    Change change = new Change();
    //**********************************************************
    public Path_list_provider_for_file_system(Path folder_path, Window owner, Logger logger)
    //**********************************************************
    {
        this.folder_path = folder_path;
        this.logger = logger;
        this.owner = owner;
    }

    //**********************************************************
    @Override
    public Path get_folder_path()
    //**********************************************************
    {
        return folder_path;
    }

    //**********************************************************
    @Override
    public List<Path> only_file_paths(boolean consider_also_hidden_files)
    //**********************************************************
    {
        File dir = folder_path.toFile();
        File[] files = dir.listFiles();
        if ( files == null) return new ArrayList<>();
        List<Path> returned = new ArrayList<>();
        for (File file : files)
        {
            if ( file.isDirectory() ) continue;
            if (! consider_also_hidden_files)
            {
                if ( Guess_file_type.should_ignore(file.toPath(),owner,logger)) continue;
            }
            returned.add(file.toPath());
        }
        return returned;
    }

    //**********************************************************
    @Override
    public List<Path> only_image_paths(boolean consider_also_hidden_files)
    //**********************************************************
    {
        File dir = folder_path.toFile();
        File[] files = dir.listFiles();
        if ( files == null) return new ArrayList<>();
        List<Path> returned = new ArrayList<>();
        for (File file : files)
        {
            if ( file.isDirectory() ) continue;
            if ( !Guess_file_type.is_this_path_an_image(file.toPath(),owner,logger)) continue;
            if (! consider_also_hidden_files)
            {
                if ( Guess_file_type.should_ignore(file.toPath(),owner,logger)) continue;
            }
            returned.add(file.toPath());
        }
        return returned;
    }


    //**********************************************************
    @Override
    public List<Path> only_song_paths(boolean consider_also_hidden_files)
    //**********************************************************
    {
        File dir = folder_path.toFile();
        File[] files = dir.listFiles();
        if ( files == null) return new ArrayList<>();
        List<Path> returned = new ArrayList<>();
        for (File file : files)
        {
            if ( file.isDirectory() ) continue;
            if ( !Guess_file_type.is_this_path_a_music(file.toPath(),owner,logger)) continue;
            if (! consider_also_hidden_files)
            {
                if ( Guess_file_type.should_ignore(file.toPath(),owner,logger)) continue;
            }
            returned.add(file.toPath());
        }
        return returned;
    }

    //**********************************************************
    @Override
    public int how_many_files_and_folders(boolean consider_also_hidden_files, boolean consider_also_hidden_folders)
    //**********************************************************
    {
        File dir = folder_path.toFile();
        File[] files = dir.listFiles();
        if ( files == null) return 0;
        int returned = 0;
        for (File file : files)
        {
            if ( file.isDirectory() )
            {
                if (! consider_also_hidden_folders)
                {
                    if ( Guess_file_type.should_ignore(file.toPath(),owner,logger)) continue;
                }
                returned++;
                continue;
            }
            if (! consider_also_hidden_files)
            {
                if ( Guess_file_type.should_ignore(file.toPath(),owner,logger)) continue;
            }
            returned++;
        }
        return returned;
    }

    //**********************************************************
    @Override
    public Files_and_folders files_and_folders(Image_found imgfnd, boolean consider_also_hidden_files, boolean consider_also_hidden_folders, Aborter aborter)
    //**********************************************************
    {
        List<Path> file_paths = new ArrayList<>();
        List<Path> folder_paths = new ArrayList<>();
        File dir = folder_path.toFile();
        File[] files = dir.listFiles();
        if ( files == null) return new Files_and_folders(file_paths,folder_paths);
        for (File file : files)
        {
            if ( file.isDirectory() )
            {
                if ( ! consider_also_hidden_folders)
                {
                    if ( Guess_file_type.should_ignore(file.toPath(),owner,logger)) continue;
                }
                folder_paths.add(file.toPath());
            }
            else
            {
                if (! consider_also_hidden_files)
                {
                    if ( Guess_file_type.should_ignore(file.toPath(),owner,logger)) continue;

                    if ( Guess_file_type.is_this_file_an_image(file,owner,logger))
                    {
                        imgfnd.image_found();
                    }
                }
                file_paths.add(file.toPath());

            }
            if ( aborter.should_abort()) break;
        }
        return new Files_and_folders(file_paths,folder_paths);
    }

    //**********************************************************
    @Override
    public List<Path> only_folder_paths(boolean consider_also_hidden_folders)
    //**********************************************************
    {
        File dir = folder_path.toFile();
        File[] files = dir.listFiles();
        if ( files == null) return new ArrayList<>();
        List<Path> returned = new ArrayList<>();
        for (File file : files)
        {
            if ( !file.isDirectory() ) continue;
            if (! consider_also_hidden_folders)
            {
                if ( Guess_file_type.should_ignore(file.toPath(),owner,logger)) continue;
            }
            returned.add(file.toPath());
        }
        return returned;
    }

    //**********************************************************
    @Override
    public String get_name()
    //**********************************************************
    {
        return folder_path.toAbsolutePath().toString();
    }


    //**********************************************************
    @Override
    public void reload()
    //**********************************************************
    {
        // we dont keep an internal state, so nothing to do here
        // just notify listeners
        change.call_change_listeners();
    }

    //**********************************************************
    @Override
    public Path resolve(String string)
    //**********************************************************
    {
        return folder_path.resolve(string);
    }

    @Override
    public Change get_Change() {
        return change;
    }



    //**********************************************************
    @Override
    public List<File> only_files(boolean hidden_files)
    //**********************************************************
    {
        File f = folder_path.toFile();
        File[] files = f.listFiles();
        if ( files == null) return new ArrayList<>();
        List<File> returned = new ArrayList<>();
        for (File file : files)
        {
            if( file.isDirectory() ) continue;
            if (!hidden_files) if ( Guess_file_type.should_ignore(file.toPath(),owner,logger)) continue;
            returned.add(file);
        }
        return returned;
    }

    //**********************************************************
    @Override
    public List<File> only_folders(boolean consider_also_hidden_folders)
    //**********************************************************
    {
        File f = folder_path.toFile();
        File[] files = f.listFiles();
        if ( files == null) return new ArrayList<>();
        List<File> returned = new ArrayList<>();
        for (File file : files)
        {
            if( !file.isDirectory() ) continue;
            if (!consider_also_hidden_folders) if ( Guess_file_type.should_ignore(file.toPath(),owner,logger)) continue;
            returned.add(file);
        }
        return returned;
    }

    //**********************************************************
    @Override
    public Move_provider get_move_provider()
    //**********************************************************
    {
        return ( destination_dir, destination_is_trash, the_list, owner, x, y,aborter, logger) -> Moving_files.safe_move_files_or_dirs(
                destination_dir,
                destination_is_trash,
                the_list,
                owner, x, y,
                aborter,
                logger);
    }

    //**********************************************************
    @Override
    public void delete(Path path, Window owner, double x, double y, Aborter aborter, Logger logger)
    //**********************************************************
    {
        Static_files_and_paths_utilities.move_to_trash(path,owner,x,y, null, aborter, logger);
    }

    //**********************************************************
    @Override
    public void delete_multiple(List<Path> paths, Window owner, double x, double y, Aborter aborter, Logger logger)
    //**********************************************************
    {
        Static_files_and_paths_utilities.move_to_trash_multiple(paths,owner,x,y, null, aborter, logger);
    }


}
