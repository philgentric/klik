// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.machine_learning.face_recognition;

import javafx.stage.Window;
import klikr.machine_learning.ML_registry_discovery;
import klikr.machine_learning.ML_server_type;
import klikr.machine_learning.ML_service_type;
import klikr.util.execute.actor.Aborter;
import klikr.machine_learning.feature_vector.Feature_vector;
import klikr.machine_learning.feature_vector.Feature_vector_source_server;
import klikr.util.log.Logger;

import java.nio.file.Path;
import java.util.Optional;

//**********************************************************
public class Feature_vector_source_for_face_recognition extends Feature_vector_source_server
//**********************************************************
{
    //**********************************************************
    public Feature_vector_source_for_face_recognition(Window owner, Logger logger)
    //**********************************************************
    {
        super(owner,logger);
    }

    //**********************************************************
    @Override
    public int get_random_port(Window owner, Logger logger)
    //**********************************************************
    {
        int port = ML_registry_discovery.get_random_active_port(new ML_service_type(ML_server_type.FaceNet,null),owner,logger);
        if (port == -1) {
            logger.log("Registry discovery failed for FaceNet, falling back to legacy port range");
        }
        logger.log("face recognition embeddings, get_random_port: " + port);
        return port;
    }


    //**********************************************************
    public Optional<Feature_vector> get_feature_vector(Path path, Window owner, Aborter aborter,Logger logger)
    //**********************************************************
    {
        return get_feature_vector_from_server(path, owner, aborter, logger);
    }

}
