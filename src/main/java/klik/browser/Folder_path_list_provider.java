package klik.browser;

import javafx.stage.Window;
import klik.actor.Aborter;
import klik.browser.virtual_landscape.Path_list_provider;
import klik.util.files_and_paths.Moving_files;
import klik.util.files_and_paths.Static_files_and_paths_utilities;
import klik.util.log.Logger;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

//**********************************************************
public class Folder_path_list_provider implements Path_list_provider
//**********************************************************
{
    private final Path folder_path;

    //**********************************************************
    public Folder_path_list_provider(Path folder_path)
    //**********************************************************
    {
        this.folder_path = folder_path;
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
    public List<Path> get_paths()
    //**********************************************************
    {
        File f = folder_path.toFile();
        File[] files = f.listFiles();
        if ( files == null) return new ArrayList<>();
        List<Path> returned = new ArrayList<>();
        for (File file : files) {
            returned.add(file.toPath());
        }
        return returned;
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
    public List<File> get_file_list()
    //**********************************************************
    {
        File f = folder_path.toFile();
        File[] files = f.listFiles();
        if ( files == null) return new ArrayList<>();
        List<File> returned = new ArrayList<>();
        for (File file : files)
        {
            if( file.isDirectory() ) continue;
            returned.add(file);
        }
        return returned;
    }

    //**********************************************************
    @Override
    public Move_provider get_move_provider()
    //**********************************************************
    {
        return (owner, x, y, destination_dir, destination_is_trash, the_list, safeMoveFilesOrDirs, logger) -> Moving_files.safe_move_files_or_dirs(owner,x,y,
                destination_dir,
                destination_is_trash,
                the_list,
                new Aborter("safe_move_files_or_dirs",logger),
                logger);
    }

    //**********************************************************
    @Override
    public void delete(Path path, Window owner, double x, double y, Aborter aborter, Logger logger)
    //**********************************************************
    {
        Static_files_and_paths_utilities.move_to_trash(path,owner,x,y, null, aborter, logger);
    }

    @Override
    public void delete_multiple(List<Path> paths, Window owner, double x, double y, Aborter aborter, Logger logger) {
        Static_files_and_paths_utilities.move_to_trash_multiple(paths,owner,x,y, null, aborter, logger);
    }

}
