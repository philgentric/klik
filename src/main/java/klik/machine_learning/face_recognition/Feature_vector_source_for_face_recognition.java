// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.machine_learning.face_recognition;

import javafx.stage.Window;
import klik.machine_learning.ML_servers_util;
import klik.util.execute.Nix_execute_via_script_in_tmp_file;
import klik.util.execute.actor.Aborter;
import klik.machine_learning.feature_vector.Feature_vector;
import klik.machine_learning.feature_vector.Feature_vector_source_server;
import klik.util.log.Logger;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

//**********************************************************
public class Feature_vector_source_for_face_recognition extends Feature_vector_source_server
//**********************************************************
{
    // server's port to get embeddings:
    // TODO: this is a not-shared config with the shell script "launch_servers"
    static int[] port = {8020, 8021};
    static Random random = new Random();
    static AtomicBoolean server_started = new AtomicBoolean(false);

    //**********************************************************
    public Feature_vector_source_for_face_recognition(Window owner, Logger logger)
    //**********************************************************
    {
        super(owner,logger);
    }

    //**********************************************************
    public int get_random_port()
    //**********************************************************
    {
        int returned = random.nextInt(port[0],port[0]+port.length);
        System.out.println("Feature_vector_source_for_Enable_face_recognition, get_random_port: "+returned);
        return returned;
    }

    @Override
    protected String get_server_python_name() {
        return "FaceNet_embeddings_server";
    }

    //**********************************************************
    @Override
    protected boolean get_server_started()
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
