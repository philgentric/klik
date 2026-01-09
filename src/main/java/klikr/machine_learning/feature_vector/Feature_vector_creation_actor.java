// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.machine_learning.feature_vector;

//SOURCES ../Feature_vector_source.java

import klikr.util.execute.actor.Actor;
import klikr.util.execute.actor.Message;
import klikr.util.execute.actor.virtual_threads.Concurrency_limiter;

import java.util.Optional;

//**********************************************************
public class Feature_vector_creation_actor implements Actor
//**********************************************************
{
    public static final boolean dbg = false;
    final Feature_vector_source fvs;
    Concurrency_limiter cl = new Concurrency_limiter("Feature_vector_creation_actor",0.5);

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
        try
        {
            cl.acquire();
        } catch (InterruptedException e) {
            return ""+e;
        }
        Feature_vector_build_message image_feature_vector_message = (Feature_vector_build_message) m;
        if (dbg) image_feature_vector_message.logger.log("Feature_vector_creation_actor START for"+image_feature_vector_message.path);

        if (image_feature_vector_message.aborter.should_abort())
        {
            image_feature_vector_message.logger.log("Feature_vector_creation_actor aborting "+image_feature_vector_message.path);
            cl.release();
            return "aborted";
        }

        Optional<Feature_vector> fv = fvs.get_feature_vector(image_feature_vector_message.path, image_feature_vector_message.owner, image_feature_vector_message.aborter, image_feature_vector_message.logger);

        if ( fv.isEmpty())
        {
            image_feature_vector_message.logger.log("Warning: fv source failed");
            cl.release();
            return "Warning: embeddings server failed";
        }
        //image_feature_vector_message.logger.log("OK: fv made by source");
        image_feature_vector_message.feature_vector_cache.inject(image_feature_vector_message.path,fv.get());
        cl.release();
        return "ok";
    }

}
