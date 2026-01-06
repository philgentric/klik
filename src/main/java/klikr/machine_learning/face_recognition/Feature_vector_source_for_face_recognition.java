// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.machine_learning.face_recognition;

import javafx.stage.Window;
import klikr.machine_learning.ML_servers_util;
import klikr.util.execute.actor.Aborter;
import klikr.machine_learning.feature_vector.Feature_vector;
import klikr.machine_learning.feature_vector.Feature_vector_source_server;
import klikr.util.log.Logger;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

//**********************************************************
public class Feature_vector_source_for_face_recognition extends Feature_vector_source_server
//**********************************************************
{
    // server's port to get embeddings:
    // TODO: this is a not-shared config with the shell script "launch_face_recognition_servers"
    static int[] face_embeddings_ports = {8020, 8021};
    static Random random = new Random();
    static AtomicBoolean server_started = new AtomicBoolean(false);

    //**********************************************************
    public Feature_vector_source_for_face_recognition(Window owner, Logger logger)
    //**********************************************************
    {
        super(owner,logger);
    }

    //**********************************************************
    public int get_random_port(Logger logger)
    //**********************************************************
    {
        int returned = random.nextInt(face_embeddings_ports[0],face_embeddings_ports[0]+face_embeddings_ports.length);
        logger.log("face recognition embeddings, get_random_port: "+returned);
        return returned;
    }

    @Override
    protected String get_server_python_name() {
        return "FaceNet_embeddings_server";
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
    protected void set_server_started(boolean b)
    //**********************************************************
    {
        server_started.set(b);
    }

    //**********************************************************
    @Override
    protected boolean start_servers(Window owner, Logger logger)
    //**********************************************************
    {
        ML_servers_util.start_face_recognition_servers(owner,logger);
        return true;
    }


    //**********************************************************
    public Optional<Feature_vector> get_feature_vector(Path path, Window owner, Aborter aborter,Logger logger)
    //**********************************************************
    {
        return get_feature_vector_from_server(path, owner, aborter, logger);
    }

}
