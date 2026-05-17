// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.machine_learning.face_recognition;

import javafx.stage.Window;
import klikr.machine_learning.Load_balancer;
import klikr.machine_learning.ML_server_type;
import klikr.machine_learning.feature_vector.Feature_vector_double;
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
        return Load_balancer.get_random_active_port(ML_server_type.FaceNet,owner,logger);
    }


    //**********************************************************
    public Optional<Feature_vector_double> get_feature_vector(Path path, Window owner, Aborter can_be_null, Logger logger)
    //**********************************************************
    {
        return get_feature_vector_from_server(path, owner, can_be_null, logger);
    }

}
