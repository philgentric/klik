package klik.image_ml.face_recognition;

import klik.actor.Aborter;
import klik.image_ml.Feature_vector;
import klik.image_ml.Feature_vector_source;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;

import java.nio.file.Path;
import java.util.Random;

//**********************************************************
public class Feature_vector_source_for_face_recognition extends Feature_vector_source
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

}
