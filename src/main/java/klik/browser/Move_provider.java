package klik.browser;

import javafx.stage.Window;
import klik.util.execute.actor.Aborter;
import klik.util.log.Logger;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public interface Move_provider {
    void move(Path destinationDir, boolean destinationIsTrash, List<File> theList, Window owner, double x, double y, Aborter aborter, Logger logger);
}
