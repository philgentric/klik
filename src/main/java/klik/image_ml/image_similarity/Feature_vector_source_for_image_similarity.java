package klik.image_ml.image_similarity;

import klik.actor.Aborter;
import klik.image_ml.Feature_vector_source;

import java.util.Random;

//**********************************************************
public class Feature_vector_source_for_image_similarity extends Feature_vector_source
//**********************************************************
{
    // server's port to get embeddings for image similarity:
    public static int[] ports = {
    //        8200};
            8200 , 8201 , 8202 , 8203 , 8204 , 8205 , 8206 , 8207 , 8208 , 8209
            , 8210 , 8211 , 8212 , 8213 , 8214 , 8215 , 8216 , 8217 , 8218 , 8219
            , 8220 , 8221 , 8222 , 8223 , 8224 , 8225 , 8226 , 8227 , 8228 , 8229
            , 8230 , 8231 , 8232 , 8233
            };


    static Random random = new Random();

    public Feature_vector_source_for_image_similarity(Aborter aborter)
    {
        super(aborter);
    }

    //**********************************************************
    public int get_random_port()
    //**********************************************************
    {
        int returned = random.nextInt(ports[0],ports[0]+ports.length);
        return returned;
    }



}
