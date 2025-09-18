package klik.machine_learning.feature_vector;

import javafx.stage.Window;
import klik.actor.Aborter;
import klik.actor.Message;
import klik.util.log.Logger;

import java.nio.file.Path;

//**********************************************************
public class Feature_vector_build_message implements Message
//**********************************************************
{

    public final Path path;
    public final Logger logger;
    public final Feature_vector_cache feature_vector_cache;
    public final Aborter aborter;
    public final Window owner;

    //**********************************************************
    public Feature_vector_build_message(Path path, Feature_vector_cache feature_vector_cache, Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        this.path = path;
        this.logger = logger;
        this.feature_vector_cache = feature_vector_cache;
        this.owner = owner;
        this.aborter = aborter;
    }

    @Override
    public Aborter get_aborter() {
        return aborter;
    }

}
