// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.machine_learning.feature_vector;

import javafx.stage.Window;
import klikr.util.execute.actor.Aborter;
import klikr.util.log.Logger;

import java.nio.file.Path;
import java.util.Optional;

//**********************************************************
public interface Feature_vector_source
//**********************************************************
{
    Optional<Feature_vector> get_feature_vector(Path path, Window owner, Aborter aborter, Logger logger);
}
