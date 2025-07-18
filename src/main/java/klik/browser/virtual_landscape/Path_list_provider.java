package klik.browser.virtual_landscape;
//SOURCES ../Move_provider.java
import javafx.stage.Window;
import klik.actor.Aborter;
import klik.browser.Move_provider;
import klik.util.log.Logger;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

//**********************************************************
public interface Path_list_provider
//**********************************************************
{
    Path get_folder_path();
    String get_name(); // absolute path if true folder
    Path resolve(String string);

    void reload();
    //List<Path> get_all(); // files and folder
    List<File> only_files(boolean consider_also_hidden_files); // only files, no folders
    List<Path> only_file_paths(boolean consider_also_hidden_files);
    List<Path> only_image_paths(boolean considerAlsoHiddenFiles);

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
