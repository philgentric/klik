package klik.browser.classic;

import javafx.stage.Window;
import klik.actor.Aborter;
import klik.browser.Move_provider;
import klik.browser.virtual_landscape.Path_list_provider;
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

    //**********************************************************
    public Path_list_provider_for_file_system(Path folder_path)
    //**********************************************************
    {
        this.folder_path = folder_path;
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
                if ( Guess_file_type.should_ignore(file.toPath())) continue;
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
            if ( !Guess_file_type.is_this_extension_an_image(file.toPath())) continue;
            if (! consider_also_hidden_files)
            {
                if ( Guess_file_type.should_ignore(file.toPath())) continue;
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
            if ( !Guess_file_type.is_this_path_a_music(file.toPath())) continue;
            if (! consider_also_hidden_files)
            {
                if ( Guess_file_type.should_ignore(file.toPath())) continue;
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
                    if ( Guess_file_type.should_ignore(file.toPath())) continue;
                }
                returned++;
                continue;
            }
            if (! consider_also_hidden_files)
            {
                if ( Guess_file_type.should_ignore(file.toPath())) continue;
            }
            returned++;
        }
        return returned;
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
                if ( Guess_file_type.should_ignore(file.toPath())) continue;
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

    }

    //**********************************************************
    @Override
    public Path resolve(String string)
    //**********************************************************
    {
        return folder_path.resolve(string);
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
            if (!hidden_files) if ( Guess_file_type.should_ignore(file.toPath())) continue;
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
            if (!consider_also_hidden_folders) if ( Guess_file_type.should_ignore(file.toPath())) continue;
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
