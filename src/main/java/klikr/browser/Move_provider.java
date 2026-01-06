// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.browser;

import javafx.stage.Window;
import klikr.util.execute.actor.Aborter;
import klikr.util.log.Logger;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public interface Move_provider {
    void move(Path destinationDir, boolean destinationIsTrash, List<File> theList, Window owner, double x, double y, Aborter aborter, Logger logger);
}
