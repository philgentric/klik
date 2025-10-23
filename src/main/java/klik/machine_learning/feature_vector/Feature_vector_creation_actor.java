package klik.machine_learning.feature_vector;

//SOURCES ../Feature_vector_source.java

import klik.util.execute.actor.Actor;
import klik.util.execute.actor.Message;

//**********************************************************
public class Feature_vector_creation_actor implements Actor
//**********************************************************
{
    public static final boolean dbg = false;
    final Feature_vector_source fvs;

    //**********************************************************
    public Feature_vector_creation_actor(Feature_vector_source fvs)
    //**********************************************************
    {
        this.fvs = fvs;
    }

    //**********************************************************
    @Override
    public String name()
    //**********************************************************
    {
        return "Feature_vector_creation_actor";
    }

    //**********************************************************
    @Override
    public String run(Message m)
    //**********************************************************
    {
        Feature_vector_build_message image_feature_vector_message = (Feature_vector_build_message) m;
        if (dbg) image_feature_vector_message.logger.log("Feature_vector_creation_actor START for"+image_feature_vector_message.path);

        if (image_feature_vector_message.aborter.should_abort())
        {
            image_feature_vector_message.logger.log("Feature_vector_creation_actor aborting "+image_feature_vector_message.path);
            return "aborted";
        }

        Feature_vector fv = fvs.get_feature_vector(image_feature_vector_message.path, image_feature_vector_message.owner, image_feature_vector_message.logger);

        if ( fv == null)
        {
            image_feature_vector_message.logger.log("Warning: fv source failed");
            return "Warning: embeddings server failed";
        }
        //image_feature_vector_message.logger.log("OK: fv made by source");
        image_feature_vector_message.feature_vector_cache.inject(image_feature_vector_message.path,fv);
        return "ok";
    }

}
