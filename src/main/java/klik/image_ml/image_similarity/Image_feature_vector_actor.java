package klik.image_ml.image_similarity;

import klik.actor.Actor;
import klik.actor.Message;
import klik.image_ml.Feature_vector;
import klik.image_ml.Feature_vector_source;

//**********************************************************
public class Image_feature_vector_actor implements Actor
//**********************************************************
{
    public static final boolean dbg = false;
    Feature_vector_source fvs = new Feature_vector_source_vgg19();

    //**********************************************************
    @Override
    public String run(Message m)
    //**********************************************************
    {
        Image_feature_vector_message image_feature_vector_message = (Image_feature_vector_message) m;
        if (dbg) image_feature_vector_message.logger.log("Image_feature_vector_actor START for"+image_feature_vector_message.path);

        if (image_feature_vector_message.aborter.should_abort())
        {
            image_feature_vector_message.logger.log("Image_feature_vector_actor aborting "+image_feature_vector_message.path);
            return "aborted";
        }

        Feature_vector fv = fvs.get_feature_vector_from_server(image_feature_vector_message.path, image_feature_vector_message.logger);

        if ( fv == null)
        {
            image_feature_vector_message.logger.log("BADBADBAD VGG19 server failed");
            return "error";
        }
        image_feature_vector_message.image_feature_vector_cache.inject(image_feature_vector_message.path,fv);
        return "ok";
    }

}
