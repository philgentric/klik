package klik.browser.virtual_landscape;

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
    String get_name2(); // absolute path if true folder
    Path resolve(String string);

    void reload();
    List<Path> get_path_list(); // files and folder
    List<File> only_files(); // only files, no folders

    Move_provider get_move_provider();

    void delete(Path path, Window owner, double x, double y, Aborter aborter, Logger logger);
    void delete_multiple(List<Path> paths, Window owner, double x, double y, Aborter aborter, Logger logger);

    Path get_path();

    default boolean has_parent()
    {
        Path parent = get_path().getParent();
        return parent != null;
    }
}
