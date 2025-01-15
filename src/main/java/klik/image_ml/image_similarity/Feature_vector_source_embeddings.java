package klik.image_ml.image_similarity;

import klik.actor.Actor_engine;
import klik.image_ml.Feature_vector;
import klik.image_ml.Feature_vector_source;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import klik.util.log.System_out_logger;

import java.nio.file.Path;
import java.util.Random;

//**********************************************************
public class Feature_vector_source_embeddings implements Feature_vector_source
//**********************************************************
{
    // server's port to get embeddings:
    // TODO: this is a not-shared config with the shell script "launch_servers"
    public static int[] ports = {
    //        8200};
            8200 , 8201 , 8202 , 8203 , 8204 , 8205 , 8206 , 8207 , 8208 , 8209
            , 8210 , 8211 , 8212 , 8213 , 8214 , 8215 , 8216 , 8217 , 8218 , 8219
            , 8220 , 8221 , 8222 , 8223 , 8224 , 8225 , 8226 , 8227 , 8228 , 8229
            , 8230 , 8231 , 8232 , 8233
            };


    static long start = System.currentTimeMillis();
    static long tx_count = 0;
    static long SUM_dur = 0;
    static Random random = new Random();

    static{
        Logger l = new System_out_logger();
        Runnable r = () ->
        {
            for(;;)
            {
                try {
                    Thread.sleep(60_000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                get_embeddings_stats(l);
            }
        };
        Actor_engine.execute(r, l);
    }

    //**********************************************************
    public static int get_random_port()
    //**********************************************************
    {
        int returned = random.nextInt(ports[0],ports[0]+ports.length);
        return returned;
    }


    //**********************************************************
    @Override
    public Feature_vector get_feature_vector_from_server(Path path, Logger logger)
    //**********************************************************
    {
        long local_start = System.currentTimeMillis();
        if ( path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BAD WARNING"));
            return null;
        }
        //Ml_servers_util.init_image_similarity(logger);
        int random_port = get_random_port();
        Feature_vector x = Feature_vector_source.get_feature_vector_from_server_generic(path, random_port, logger);
        long local_end = System.currentTimeMillis();
        long local_dur = local_end - local_start;
        SUM_dur += local_dur;
        tx_count++;
        return x;
    }

    //**********************************************************
    public static void get_embeddings_stats(Logger logger)
    //**********************************************************
    {
        long end = System.currentTimeMillis();
        long local_dur = end - start;
        double dur_minutes = (double)local_dur/60_000.0;
        logger.log("TX_rate="+(double)tx_count/(double)dur_minutes+" tx/min (tx_count="+tx_count+" for: "+dur_minutes+" minutes)");

        logger.log("total server call time="+ SUM_dur +"ms, average concurency="+(double) SUM_dur /(double)local_dur);

    }


}
