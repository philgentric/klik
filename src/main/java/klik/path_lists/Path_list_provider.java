package klik.path_lists;
//SOURCES ../Move_provider.java
import javafx.stage.Window;
import klik.util.execute.actor.Aborter;
import klik.browser.Move_provider;
import klik.util.log.Logger;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

//**********************************************************
public interface Path_list_provider
//**********************************************************
{
    // an abstract interface to provide a list of paths (files)
    // could be in a disk folder OR in a 'playlist'
    Path get_folder_path();
    String get_name(); // absolute path of *folder* or *playlist file*
    Path resolve(String string);

    Change get_Change();


    void reload();
    List<File> only_files(boolean consider_also_hidden_files); // only files, no folders
    List<Path> only_file_paths(boolean consider_also_hidden_files);

    List<Path> only_image_paths(boolean considerAlsoHiddenFiles);
    List<Path> only_song_paths(boolean considerAlsoHiddenFiles);

    List<File> only_folders(boolean consider_also_hidden_folders);
    List<Path> only_folder_paths(boolean consider_also_hidden_folders);

    Move_provider get_move_provider();

    void delete(Path path, Window owner, double x, double y, Aborter aborter, Logger logger);
    void delete_multiple(List<Path> paths, Window owner, double x, double y, Aborter aborter, Logger logger);


    default boolean has_parent()
    {
        Path parent = get_folder_path().getParent();
        return parent != null;
    }

    int how_many_files_and_folders(boolean consider_also_hidden_files, boolean consider_also_hidden_folders);

}
