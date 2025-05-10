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
    String get_name(); // absolute path if true folder
    List<Path> get_paths();
    Path resolve(String string);

    List<File> get_file_list(); // only files, no folders

    Move_provider get_move_provider();

    void delete(Path path, Window owner, double x, double y, Aborter aborter, Logger logger);
    void delete_multiple(List<Path> paths, Window owner, double x, double y, Aborter aborter, Logger logger);
}
