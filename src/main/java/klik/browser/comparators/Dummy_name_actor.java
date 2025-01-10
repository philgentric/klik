package klik.browser.comparators;

import klik.actor.Aborter;
import klik.actor.Actor;
import klik.actor.Message;
import klik.image_ml.Feature_vector;
import klik.image_ml.image_similarity.Image_feature_vector_RAM_cache;
import klik.util.files_and_paths.Guess_file_type;
import klik.util.log.Logger;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Dummy_name_actor implements Actor {

    private final File[] p2s;
    private final ConcurrentHashMap<Path_pair, Double> similarities;
    private final Image_feature_vector_RAM_cache cache;
    private final Logger logger;

    public Dummy_name_actor(File[] p2s, Image_feature_vector_RAM_cache cache,ConcurrentHashMap<Path_pair, Double> similarities, Logger logger)
    {
        this.p2s = p2s;
        this.cache = cache;
        this.similarities = similarities;
        this.logger = logger;
    }

    @Override
    public String run(Message m)
    {

        int count2 =0;
        Dummy_name_message dnm = (Dummy_name_message)m;
        Path p1 = dnm.p1;
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
        for ( File f2 : p2s) {
            if (aborter.should_abort()) return "aborted";
            Path p2 = f2.toPath();
            if (!Guess_file_type.is_file_an_image(f2)) {
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
            similarities.put(new Path_pair(p1, p2), diff);

            count2++;
            if (count2 % 1000 == 0) logger.log(" Dummy name actor " + count2 + " for: "+p1.getFileName());
        }

        return "Done";
    }

    @Override
    public String name() {
        return Actor.super.name();
    }
}
