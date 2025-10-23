package klik.machine_learning.face_recognition;

import javafx.stage.Window;
import klik.util.execute.actor.Aborter;
import klik.machine_learning.feature_vector.Feature_vector;
import klik.machine_learning.feature_vector.Feature_vector_source_server;
import klik.util.log.Logger;

import java.nio.file.Path;
import java.util.Random;

//**********************************************************
public class Feature_vector_source_for_face_recognition extends Feature_vector_source_server
//**********************************************************
{
    // server's port to get embeddings:
    // TODO: this is a not-shared config with the shell script "launch_servers"
    static int[] port = {8020, 8021};
    static Random random = new Random();

    //**********************************************************
    public Feature_vector_source_for_face_recognition(Aborter aborter)
    //**********************************************************
    {
        super(aborter);
    }

    //**********************************************************
    public int get_random_port()
    //**********************************************************
    {
        int returned = random.nextInt(port[0],port[0]+port.length);
        System.out.println("Feature_vector_source_for_Enable_face_recognition, get_random_port: "+returned);
        return returned;
    }

    //**********************************************************
    public Feature_vector get_feature_vector(Path path, Window owner, Logger logger)
    //**********************************************************
    {
        return get_feature_vector_from_server(path, owner, logger);
    }

}
