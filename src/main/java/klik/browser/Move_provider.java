package klik.browser;

import javafx.stage.Window;
import klik.actor.Aborter;
import klik.util.log.Logger;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public interface Move_provider {
    void move(Window owner, double x, double y, Path destinationDir, boolean destinationIsTrash, List<File> theList, Aborter safeMoveFilesOrDirs, Logger logger);
}
