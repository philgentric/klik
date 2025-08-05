package klik.image_ml.image_similarity;

import javafx.stage.Window;
import klik.actor.Aborter;
import klik.actor.Actor;
import klik.actor.Message;
import klik.browser.comparators.Similarity_comparator_by_pursuit;
import klik.image_ml.Feature_vector;
import klik.util.log.Logger;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


//**********************************************************
public class Similarity_cache_warmer_actor implements Actor
//**********************************************************
{
    // image at distances larger than this value will not be stored in the cache
    // i.e. they are not similar enough
    public static final double SIMILARITY_THRESHOLD = 0.14;

    private final List<Path> images;
    private final ConcurrentHashMap<Path_pair, Double> similarities_hashtable;
    private final Image_feature_vector_cache cache;
    private final Logger logger;

    //**********************************************************
    public Similarity_cache_warmer_actor(List<Path> images, Image_feature_vector_cache cache, ConcurrentHashMap<Path_pair, Double> similarities, Logger logger)
    //**********************************************************
    {
        this.images = images;
        this.cache = cache;
        this.similarities_hashtable = similarities;
        this.logger = logger;
    }

    //**********************************************************
    @Override
    public String run(Message m)
    //**********************************************************
    {

        Similarity_cache_warmer_message scwm = (Similarity_cache_warmer_message)m;
        Aborter browser_aborter = scwm.get_aborter();
        Window owner = scwm.get_owner();
        Feature_vector emb1 = cache.get_from_cache(scwm.p1,null,true,owner,browser_aborter);
        if ( emb1 == null)
        {
            emb1 = cache.get_from_cache(scwm.p1,null,true,owner,browser_aborter);
            if ( emb1 == null)
            {
                logger.log(" emb1 == null for "+scwm.p1);
                return "ERROR";
            }
        }

        for (Path p2 : images)
        {
            if ( p2.getFileName().toString().equals(scwm.p1.getFileName().toString())) continue;
            //if (!Guess_file_type.is_file_an_image(p2.toFile())) {
            //    continue;
            //}
            Path_pair pp = Path_pair.get(scwm.p1, p2);
            // already in cache?
            if ( similarities_hashtable.get(pp) != null)
            {
                //logger.log("not computed: similarity already in cache "+p1+" vs "+p2);
                continue;
            }
            if (browser_aborter.should_abort()) return "aborted";

            //logger.log("processing "+p1+" vs "+p2);
            Feature_vector emb2 = cache.get_from_cache(p2, null, true,owner, browser_aborter);
            if (emb2 == null) {
                emb2 = cache.get_from_cache(p2, null, true,owner, browser_aborter);
                if (emb2 == null) {
                    logger.log(" emb2 == null for " + p2);
                    continue;
                }
            }
            double diff = emb1.compare(emb2);
            //logger.log("similarity = "+diff+" "+dnm.p1+" vs "+p2);
            //if ( diff < min_similarity) min_similarity = diff;
            //if ( diff > max_similarity) max_similarity = diff;


            // to avoid 'OutOfMemoryError: Java heap space'
            // we limit the number of entries
            if ( diff < SIMILARITY_THRESHOLD) similarities_hashtable.put(pp, diff);
        }

        return "Done";
    }

    @Override
    public String name() {
        return "Similarity_cache_warmer_actor";
    }
}
