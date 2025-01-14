package klik.image_ml.image_similarity;

import klik.actor.Aborter;
import klik.actor.Message;
import klik.util.log.Logger;

import java.nio.file.Path;

//**********************************************************
public class Image_feature_vector_message implements Message
//**********************************************************
{

    public final Path path;
    public final Logger logger;
    public final Image_feature_vector_cache image_feature_vector_cache;
    public final Aborter aborter;

    //**********************************************************
    public Image_feature_vector_message(Path path, Image_feature_vector_cache image_feature_vector_cache, Aborter aborter_, Logger logger)
    //**********************************************************
    {
        this.path = path;
        this.logger = logger;
        this.image_feature_vector_cache = image_feature_vector_cache;
        aborter = aborter_;
    }

    @Override
    public Aborter get_aborter() {
        return aborter;
    }

}
