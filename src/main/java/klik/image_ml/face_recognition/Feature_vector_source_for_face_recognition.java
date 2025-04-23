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

    public Feature_vector_source_for_face_recognition(Aborter aborter) {
        super(aborter);
    }

    //**********************************************************
    public int get_random_port()
    //**********************************************************
    {
        int returned = random.nextInt(port[0],port[0]+port.length);
        return returned;
    }

    /*

    //**********************************************************
    @Override
    public Feature_vector get_feature_vector_from_server(Path path, Logger logger)
    //**********************************************************
    {
        if ( path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BAD WARNING"));
            return null;
        }
        //ML_servers_util.init_face_reco(logger);
        int random_port = get_random_port();
        return Feature_vector_source.get_feature_vector_from_server_generic(path,random_port,logger);
    }
*/
}
