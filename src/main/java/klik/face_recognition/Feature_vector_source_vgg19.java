package klik.face_recognition;

import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;

import java.nio.file.Path;
import java.util.Random;

//**********************************************************
public class Feature_vector_source_vgg19 implements Feature_vector_source
//**********************************************************
{
    // server's port to get embeddings:
    // TODO: this is a not-shared config with the shell script "launch_servers"
    static int[] port = {8030, 8031, 8032, 8033, 8034, 8035, 8036, 8037, 8038, 8039};
    static Random random = new Random();
    //**********************************************************
    public static int get_random_port()
    //**********************************************************
    {
        int returned = random.nextInt(port[0],port[0]+port.length);
        return returned;
    }


    //**********************************************************
    @Override
    public Feature_vector get_feature_vector_from_server(Path path,  Logger logger)
    //**********************************************************
    {
        if ( path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BAD WARNING"));
            return null;
        }
        int random_port = get_random_port();
        return Feature_vector_source.get_feature_vector_from_server_generic(path,random_port,logger);
    }

}
