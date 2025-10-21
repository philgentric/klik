package klik.machine_learning.feature_vector;

import javafx.stage.Window;
import klik.util.log.Logger;

import java.nio.file.Path;

//**********************************************************
public interface Feature_vector_source
//**********************************************************
{
    Feature_vector get_feature_vector(Path path, Window owner, Logger logger);
}
