// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.machine_learning.image_similarity;

import javafx.stage.Window;
import klikr.machine_learning.Load_balancer;
import klikr.machine_learning.feature_vector.Feature_vector_double;
import klikr.machine_learning.monitoring.UDP_traffic_monitor;
import klikr.machine_learning.ML_server_type;
import klikr.settings.boolean_features.Feature;
import klikr.settings.boolean_features.Feature_cache;
import klikr.util.execute.actor.Aborter;
import klikr.machine_learning.feature_vector.Feature_vector;
import klikr.machine_learning.feature_vector.Feature_vector_source_server;
import klikr.util.log.Logger;

import java.nio.file.Path;
import java.util.Optional;

//**********************************************************
public class Feature_vector_source_for_image_similarity extends Feature_vector_source_server
//**********************************************************
{

    //**********************************************************
    public Feature_vector_source_for_image_similarity(Window owner,Logger logger)
    //**********************************************************
    {
        super(owner, logger);
        if (Feature_cache.get(Feature.Enable_ML_server_debug))
        {
            UDP_traffic_monitor.start_servers_monitoring(owner, logger);
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
        return Load_balancer.get_random_active_port(ML_server_type.MobileNet, owner,logger);
    }

    //**********************************************************
    public Optional<Feature_vector_double> get_feature_vector(Path path, Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        return get_feature_vector_from_server(path, owner, aborter, logger);
    }

}
