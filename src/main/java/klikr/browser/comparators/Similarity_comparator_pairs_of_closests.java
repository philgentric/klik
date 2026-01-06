// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.browser.comparators;

//SOURCES ../../image_ml/image_similarity/Feature_vector_source_for_image_similarity.java;

import klikr.util.execute.actor.Aborter;
import klikr.path_lists.Path_list_provider;
import klikr.machine_learning.feature_vector.Feature_vector_cache;
import klikr.machine_learning.similarity.Similarity_cache;
import klikr.util.log.Logger;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;


//**********************************************************
public class Similarity_comparator_pairs_of_closests extends Similarity_comparator
//**********************************************************
{

    // the distances are computed in the constructor
    //**********************************************************
    public Similarity_comparator_pairs_of_closests(
            Supplier<Feature_vector_cache> fv_cache_supplier,
            Similarity_cache similarity_cache,
            Path_list_provider path_list_provider,
            double x, double y,
            Aborter aborter,
            Logger logger_)
    //**********************************************************
    {
        super(fv_cache_supplier,similarity_cache, path_list_provider, logger_);

        //logger.log("\n\nmin "+Similarity_cache_warmer_actor.min+" max "+Similarity_cache_warmer_actor.max);
        if ( aborter.should_abort()) return;

        // "closest" is a relation that can be asymmetric
        // P2 is closest to P1, P1->P2
        // = starting from P1, scanning the set, I find P2
        // this the definition for 'Closest_neighbor'
        // but P2->P3 i.e. there is a P3!=P1 which is P2 closest neighbor

        // first we make a map P1->P2
        List<Closest_neighbor> candidates = new ArrayList<>();
        Map<Path, Closest_neighbor> map = new HashMap<>();
        for ( Path p1 : images)
        {
            Closest_neighbor cn = Closest_neighbor.find_closest_of(p1, images, similarity_cache);
            if ( cn == null)
            {
                //logger.log("find_closest_of == null for "+p1+" means closest is farther away than the THRESHOLD");
                continue;
            }
            else
            {
                //logger.log("find_closest_of "+p1+" == "+cn.dist());
            }
            candidates.add(cn);
            map.put(p1,cn);
        }
        // we make a second pass to find the "true" pairs
        // of P1->P2 & P2->P1
        // (there might be very few)

        List<Closest_neighbor> done = new ArrayList<>();
        for( Closest_neighbor cn : candidates)
        {
            //cn.closest is the closest of cn.p1
            Closest_neighbor cn2 = map.get(cn.closest());
            // we have cn2.closest is closest to cn.closest
            if (cn.p1().equals(cn2.closest()))
            {
                done.add(cn);
            }
        }

        dummy_names.clear();
        int i = 0;
        for ( Closest_neighbor cn : done)
        {
            Path p1 = cn.p1();
            dummy_names.put(p1, i);
            //logger.log(p1+" -> "+i);
            i++;
            Path p2 = cn.closest();
            dummy_names.put(p2, i);
            //logger.log(p2+" -> "+i);
            i++;
        }

        // then we complete the fill 'blindly'
        for ( Path p : images)
        {
            if ( !dummy_names.containsKey(p))
            {
                dummy_names.put(p, i);
                //logger.log(p+" -> "+i);
                i++;
            }
        }
        add_non_images(path_list_provider, i);
    }



}
