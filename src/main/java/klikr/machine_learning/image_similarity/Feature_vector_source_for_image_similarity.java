// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.machine_learning.image_similarity;

import javafx.stage.Window;
import klikr.machine_learning.Embeddings_servers_monitor;
import klikr.machine_learning.ML_servers_util;
import klikr.properties.boolean_features.Feature;
import klikr.properties.boolean_features.Feature_cache;
import klikr.util.execute.actor.Aborter;
import klikr.machine_learning.feature_vector.Feature_vector;
import klikr.machine_learning.feature_vector.Feature_vector_source_server;
import klikr.util.log.Logger;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

//**********************************************************
public class Feature_vector_source_for_image_similarity extends Feature_vector_source_server
//**********************************************************
{
    // server's port to get embeddings for image similarity:
    public static int[] ports = {
            8200 , 8201 , 8202 , 8203 , 8204 , 8205 , 8206 , 8207 , 8208 , 8209
            , 8210 , 8211 , 8212 , 8213 , 8214 , 8215 , 8216 , 8217 , 8218 , 8219
            , 8220 , 8221 , 8222 , 8223 , 8224 , 8225 , 8226 , 8227 , 8228 , 8229
            , 8230 , 8231 , 8232 , 8233
            };


    static Random random = new Random();
    static AtomicBoolean server_started = new AtomicBoolean(false);

    //**********************************************************
    public Feature_vector_source_for_image_similarity(Window owner,Logger logger)
    //**********************************************************
    {
        super(owner, logger);
        if (Feature_cache.get(Feature.Enable_feature_vector_monitoring))
        {
            Embeddings_servers_monitor.start_servers_monitoring(owner, logger);
        }
        //logger.log(Stack_trace_getter.get_stack_trace("Feature_vector_source_for_image_similarity"));
    }

    //**********************************************************
    public String get_server_python_name()
    //**********************************************************
    {
        return "MobileNet_embeddings_server";
    }

    //**********************************************************
    @Override
    protected boolean server_started()
    //**********************************************************
    {
        return server_started.get();
    }

    //**********************************************************
    @Override
    protected boolean start_servers(Window owner, Logger logger)
    //**********************************************************
    {
        return ML_servers_util.start_image_similarity_servers(owner,logger);
    }

    //**********************************************************
    @Override
    protected void set_server_started(boolean b)
    //**********************************************************
    {
        server_started.set(b);
    }


    //**********************************************************
    @Override //
    public int get_random_port(Logger logger)
    //**********************************************************
    {
        int returned = random.nextInt(ports[0],ports[0]+ports.length);
        return returned;
    }

    //**********************************************************
    public Optional<Feature_vector> get_feature_vector(Path path, Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        return get_feature_vector_from_server(path, owner, aborter, logger);
    }

}
