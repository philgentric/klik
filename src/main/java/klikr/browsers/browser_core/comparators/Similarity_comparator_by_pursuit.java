// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.browsers.browser_core.comparators;

//SOURCES ../../image_ml/image_similarity/Server_Feature_vector_source_for_image_similarity.java;

import javafx.stage.Window;
import klikr.browsers.browser_core.icons.image_properties_cache.Image_properties;
import klikr.util.Kontext;
import klikr.util.P2S;
import klikr.util.cache.Klikr_cache;
import klikr.util.execute.actor.Aborter;
import klikr.browsers.browser_core.virtual_landscape.Path_comparator_source;
import klikr.path_lists.Path_list_provider;
import klikr.machine_learning.feature_vector.Feature_vector;
import klikr.machine_learning.feature_vector.Feature_vector_cache;
import klikr.machine_learning.similarity.Most_similar;
import klikr.machine_learning.similarity.Similarity_engine;
import klikr.machine_learning.similarity.Similarity_cache;
import klikr.settings.boolean_features.Feature;
import klikr.settings.boolean_features.Feature_cache;
import klikr.util.log.Logger;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;


//**********************************************************
public class Similarity_comparator_by_pursuit extends Similarity_comparator
//**********************************************************
{

    //**********************************************************
    public Similarity_comparator_by_pursuit(
            Supplier<Feature_vector_cache> fv_cache_supplier,
            Similarity_cache similarity_cache,
            Path_list_provider path_list_provider,
            Path_comparator_source path_comparator_source,
            Klikr_cache<Path, Image_properties> image_properties_cache,
            Kontext context)
    //**********************************************************
    {
        super(fv_cache_supplier,similarity_cache, path_list_provider, context);

        //Collections.shuffle(images);
        //context.log("\n\nmin "+Similarity_cache_warmer_actor.min+" max "+Similarity_cache_warmer_actor.max);
        if ( context.should_abort()) return;

        // "closest" is a relation that can be asymmetric
        // P2 is closest to P1, P1->P2
        // = starting from P1, scanning the set, I find P2
        // this the definition for 'Closest_neighbor'
        // but P2->P3 i.e. there is a P3!=P1 which is P2 closest neighbor

        // first we make a map P1->P2
        List<Closest_neighbor> candidates = new ArrayList<>();
        Map<String, Closest_neighbor> map_of_closest_neighbors = new HashMap<>();
        for ( Path p1 : images)
        {
            if ( context.should_abort()) return;

            Closest_neighbor cn = Closest_neighbor.find_closest_of(p1, images, similarity_cache);
            if ( cn == null)
            {
                //context.log("find_closest_of == null for "+p1+" means closest is farther away than the THRESHOLD");
                continue;
            }
            else
            {
                //context.log("find_closest_of "+p1+" == "+cn.dist());
            }
            candidates.add(cn);
            map_of_closest_neighbors.put(P2S.p2s(p1),cn);
        }
        // we make a second pass to find the "true" pairs
        // of P1->P2 & P2->P1
        // (there might be very few)

        List<Closest_neighbor> done = new ArrayList<>();
        for( Closest_neighbor cn : candidates)
        {
            if ( context.should_abort()) return;

            //cn.closest is the closest of cn.p1
            Closest_neighbor cn2 = map_of_closest_neighbors.get(P2S.p2s(cn.closest()));
            // we have cn2.closest is closest to cn.closest
            if (cn.p1().equals(cn2.closest()))
            {
                done.add(cn);
            }
        }
        // ok "done" contains the pairs of closests
        // many remaining images are left out
        List<Path> remaining = new ArrayList<>(images);
        for ( Closest_neighbor cn : done)
        {
            Path p1 = cn.p1();
            remaining.remove(p1);
        }

        // we "extend" each pair by looking for the closest neighbors of each pair member
        List<Path> paths =  path_list_provider.only_image_paths(false,Feature_cache.get(Feature.Show_hidden_files), context.aborter());
        Similarity_engine similarity = new Similarity_engine(paths,path_list_provider,path_comparator_source, context);

        dummy_names.clear();
        int max_friend = 2;
        int i = 0;
        for ( Closest_neighbor cn : done)
        {
            if ( context.should_abort()) return;

            Path p1 = cn.p1();
            dummy_names.put(P2S.p2s(p1), i);
            //context.log(p1+" -> "+i);
            i++;
            Path p2 = cn.closest();
            dummy_names.put(P2S.p2s(p2), i);
            //context.log(p2+" -> "+i);
            i++;
            List<Path> excluded = new ArrayList<>();
            excluded.add(p1);
            excluded.add(p2);
            i = extend(p1, excluded, similarity, remaining, i, max_friend, image_properties_cache, fv_cache_supplier, context);
            i = extend(p2, excluded, similarity, remaining, i, max_friend, image_properties_cache, fv_cache_supplier, context);
        }

        // then we complete the fill 'blindly'
        for ( Path p : images)
        {
            if ( !dummy_names.containsKey(P2S.p2s(p)))
            {
                dummy_names.put(P2S.p2s(p), i);
                //context.log(p+" -> "+i);
                i++;
            }
        }

        add_non_images(path_list_provider, i);

    }

    //**********************************************************
    private int extend(Path p1,
                       List<Path> excluding,
                       Similarity_engine similarity,
                       List<Path> remaining,
                       int i,
                       int max_friend,
                       Klikr_cache<Path, Image_properties> image_properties_cache,
                       Supplier<Feature_vector_cache> fv_cache_supplier,
                       Kontext context)
    //**********************************************************
    {
        Feature_vector_cache fv_cache = fv_cache_supplier.get();
        Feature_vector fv = fv_cache.get_from_cache_or_make(p1, null, true, context);
        List<Most_similar> ms = similarity.find_similars_of_special(
                image_properties_cache,
                p1,
                fv,
                max_friend+3,
                Double.MAX_VALUE,
                fv_cache_supplier,
                remaining,
                null,
                context);

        int how_many = 0;
        for (Most_similar m : ms)
        {
            Path p3 = m.path();
            if (excluding.contains(p3)) continue;
            dummy_names.put(P2S.p2s(p3), i);
            excluding.add(p3);
            //context.log(p3+" -> "+i);
            i++;
            how_many++;
            if (how_many > max_friend) break;
        }
        return i;
    }

}
