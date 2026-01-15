// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.machine_learning.image_similarity;

import javafx.stage.Window;
import klikr.machine_learning.Embeddings_servers_monitor;
import klikr.machine_learning.ML_registry_discovery;
import klikr.machine_learning.ML_server_type;
import klikr.machine_learning.ML_service_type;
import klikr.properties.Non_booleans_properties;
import klikr.properties.boolean_features.Feature;
import klikr.properties.boolean_features.Feature_cache;
import klikr.util.execute.actor.Aborter;
import klikr.machine_learning.feature_vector.Feature_vector;
import klikr.machine_learning.feature_vector.Feature_vector_source_server;
import klikr.util.log.Logger;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

//**********************************************************
public class Feature_vector_source_for_image_similarity extends Feature_vector_source_server
//**********************************************************
{

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
    public int get_random_port(Window owner, Logger logger)
    //**********************************************************
    {
        int port = ML_registry_discovery.get_random_active_port(new ML_service_type(ML_server_type.MobileNet_image_similarity_embeddings_server, null), owner,logger);
        if (port == -1) {
            logger.log("No active MobileNet servers found in registry. Servers need to be started.");
        }
        return port;
    }

    //**********************************************************
    public Optional<Feature_vector> get_feature_vector(Path path, Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        return get_feature_vector_from_server(path, owner, aborter, logger);
    }

}
