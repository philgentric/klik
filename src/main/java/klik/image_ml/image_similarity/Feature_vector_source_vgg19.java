package klik.image_ml.image_similarity;

import klik.image_ml.Feature_vector;
import klik.image_ml.Feature_vector_source;
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
    static int[] port = {
    //        8200};
             8200,8201,8202,8203,8204,8205,8206,8207,8208,8209
            ,8210,8211,8212,8213,8214,8215,8216,8217,8218,8219
    };
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
    public Feature_vector get_feature_vector_from_server(Path path, Logger logger)
    //**********************************************************
    {
        if ( path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BAD WARNING"));
            return null;
        }
        //Ml_servers_util.init_image_similarity(logger);
        int random_port = get_random_port();
        return Feature_vector_source.get_feature_vector_from_server_generic(path,random_port,logger);
    }

}
