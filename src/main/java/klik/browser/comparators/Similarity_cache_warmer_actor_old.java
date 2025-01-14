package klik.browser.comparators;

import klik.actor.Aborter;
import klik.actor.Actor;
import klik.actor.Message;
import klik.image_ml.Feature_vector;
import klik.image_ml.image_similarity.Image_feature_vector_cache;
import klik.util.files_and_paths.Guess_file_type;
import klik.util.log.Logger;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//**********************************************************
public class Similarity_cache_warmer_actor_old implements Actor
//**********************************************************
{

    private final List<Path> images;
    private final ConcurrentHashMap<Path_pair_int, Double> similarities;
    private final Image_feature_vector_cache cache;
    private final Logger logger;
    private final List<Path> int_to_path;
    private final Map<Path,Integer> path_to_int;

    //**********************************************************
    public Similarity_cache_warmer_actor_old(List<Path> int_to_path, Map<Path,Integer> path_to_int, List<Path> images, Image_feature_vector_cache cache, ConcurrentHashMap<Path_pair_int, Double> similarities, Logger logger)
    //**********************************************************
    {
        this.images = images;
        this.cache = cache;
        this.similarities = similarities;
        this.logger = logger;
        this.int_to_path = int_to_path;
        this.path_to_int = path_to_int;
    }

    //**********************************************************
    @Override
    public String run(Message m)
    //**********************************************************
    {

        Similarity_cache_warmer_message_old dnm = (Similarity_cache_warmer_message_old)m;
        Path p1 = int_to_path.get(dnm.i1);
        Feature_vector emb1 = cache.get_from_cache(p1,null,true);
        if ( emb1 == null)
        {
            emb1 = cache.get_from_cache(p1,null,true);
            if ( emb1 == null)
            {
                logger.log("WTF emb1 == null for "+p1);
                return "WTF";
            }
        }

        Aborter aborter = dnm.get_aborter();
        for (Path p2 : images)
        {
            int j = path_to_int.get(p2);
            if ( j <= dnm.i1) continue; // half matrix
            // already in cache?
            if ( similarities.get(Path_pair_int.get(dnm.i1, j)) != null)
            {
                //logger.log("not computed: similarity already in cache "+p1+" vs "+p2);
                continue;
            }
            if (aborter.should_abort()) return "aborted";
            if (!Guess_file_type.is_file_an_image(p2.toFile())) {
                continue;
            }
            //logger.log("processing "+p1+" vs "+p2);
            Feature_vector emb2 = cache.get_from_cache(p2, null, true);
            if (emb2 == null) {
                emb2 = cache.get_from_cache(p2, null, true);
                if (emb2 == null) {
                    logger.log("WTF emb2 == null for " + p2);
                    continue;
                }
            }
            double diff = emb1.compare(emb2);
            similarities.put(Path_pair_int.get(dnm.i1, j), diff);
        }

        return "Done";
    }

    @Override
    public String name() {
        return Actor.super.name();
    }
}
