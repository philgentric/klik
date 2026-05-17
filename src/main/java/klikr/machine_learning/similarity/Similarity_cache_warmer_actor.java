// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.machine_learning.similarity;

import javafx.stage.Window;
import klikr.machine_learning.feature_vector.Top_k;
import klikr.util.cache.Klikr_cache;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Actor;
import klikr.util.execute.actor.Message;
import klikr.machine_learning.feature_vector.Feature_vector;
import klikr.machine_learning.feature_vector.Feature_vector_cache;
import klikr.util.log.Logger;
import klikr.util.mmap.Mmap;
import klikr.util.mmap.Save_and_what;

import java.nio.file.Path;
import java.util.List;


//**********************************************************
public class Similarity_cache_warmer_actor implements Actor
//**********************************************************
{
    // items at distances larger than this value will not be stored in the cache
    // i.e. they are not similar enough
    public static final double SIMILARITY_THRESHOLD = 0.14;

    private final List<Path> paths;
    private final Klikr_cache<Integer_pair, Double> similarities;
    private final Feature_vector_cache cache;
    private final Logger logger;

    //**********************************************************
    public Similarity_cache_warmer_actor(
            List<Path> paths, 
            Feature_vector_cache cache,
            Klikr_cache<Integer_pair, Double> similarities,
            Logger logger)
    //**********************************************************
    {
        this.paths = paths;
        this.cache = cache;
        this.similarities = similarities;
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
        Path p1 = scwm.p1;
        Feature_vector emb1 = cache.get_from_cache_or_make(p1,null,true,owner,browser_aborter);
        if ( emb1 == null)
        {
            emb1 = cache.get_from_cache_or_make(scwm.p1,null,true,owner,browser_aborter);
            if ( emb1 == null)
            {
                logger.log(" emb1 == null for "+scwm.p1);
                return "ERROR";
            }
        }

        Top_k<Integer> top_5 = new Top_k<>(5); // Keep top 5

        for (int j = 0; j < paths.size(); j++)
        {
            Path p2 = paths.get(j);
            if ( p2.getFileName().toString().equals(scwm.p1.getFileName().toString())) continue;

            Integer_pair pp = Integer_pair.build(scwm.index_of_p1, j);
            // already in cache?
            if ( similarities.get(pp,browser_aborter,null,owner) != null)
            {
                //logger.log("not computed: similarity already in cache "+p1+" vs "+p2);
                continue;
            }
            if (browser_aborter.should_abort())
            {
                Mmap.instance.save_index();
                return "aborted";
            }

            //logger.log("processing "+p1+" vs "+p2);
            Feature_vector emb2 = cache.get_from_cache_or_make(p2, null, true,owner, browser_aborter);
            if (emb2 == null) {
                emb2 = cache.get_from_cache_or_make(p2, null, true,owner, browser_aborter);
                if (emb2 == null) {
                    logger.log(" emb2 == null for " + p2);
                    continue;
                }
            }
            double diff = emb1.distance(emb2,logger);

            // to avoid 'OutOfMemoryError: Java heap space'
            // we limit the number of entries
            // strategy#1, hard-coded threshold
            //if ( diff < SIMILARITY_THRESHOLD) similarities.inject(pp, diff,false);
            // or keep top-K
            top_5.add(diff, j);
        }

        for (Top_k.Result<Integer> res : top_5.get_results())
        {
            similarities.inject(Integer_pair.build(scwm.index_of_p1, res.item()), res.score(), false);
        }
        return "Done";
    }

    @Override
    public String name() {
        return "Similarity_cache_warmer_actor";
    }
}
