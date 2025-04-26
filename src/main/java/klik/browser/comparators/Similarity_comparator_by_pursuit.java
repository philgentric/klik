package klik.browser.comparators;

//SOURCES ../../image_ml/image_similarity/Feature_vector_source_for_image_similarity.java;

import klik.actor.Aborter;
import klik.browser.Browser;
import klik.browser.icons.image_properties_cache.Image_properties_RAM_cache;
import klik.image_ml.Feature_vector;
import klik.image_ml.image_similarity.Image_similarity;
import klik.util.log.Logger;

import java.nio.file.Path;
import java.util.*;


//**********************************************************
public class Similarity_comparator_by_pursuit extends Similarity_comparator
//**********************************************************
{
    public static final double SIMILARITY_THRESHOLD = 0.14;

    //**********************************************************
    public Similarity_comparator_by_pursuit(Path folder, Image_properties_RAM_cache image_properties_cache, Browser browser, double x, double y, Aborter aborter, Logger logger_)
    //**********************************************************
    {
        super(folder, x,y, aborter, logger_);

        //Collections.shuffle(images);
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
            if ( aborter.should_abort()) return;

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
            if ( aborter.should_abort()) return;

            //cn.closest is the closest of cn.p1
            Closest_neighbor cn2 = map.get(cn.closest());
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
        Image_similarity similarity = new Image_similarity(folder,browser, x,y,aborter, logger);

        dummy_names.clear();
        int max_friend = 2;
        int i = 0;
        for ( Closest_neighbor cn : done)
        {
            if ( aborter.should_abort()) return;

            Path p1 = cn.p1();
            dummy_names.put(p1, i);
            //logger.log(p1+" -> "+i);
            i++;
            Path p2 = cn.closest();
            dummy_names.put(p2, i);
            //logger.log(p2+" -> "+i);
            i++;
            List<Path> excluded = new ArrayList<>();
            excluded.add(p1);
            excluded.add(p2);
            i = extend(p1, excluded, similarity, remaining, i, max_friend, image_properties_cache);
            i = extend(p2, excluded, similarity, remaining, i, max_friend, image_properties_cache);
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

    }

    //**********************************************************
    private int extend(Path p1, List<Path> excluding, Image_similarity similarity, List<Path> remaining, int i, int max_friend, Image_properties_RAM_cache image_properties_cache)
    //**********************************************************
    {
        Feature_vector fv = fv_cache.get_from_cache(p1, null, true);
        List<Image_similarity.Most_similar> ms = similarity.find_similars_of(
                p1,
                fv,
                false,
                true,
                max_friend+3,
                Double.MAX_VALUE,
                image_properties_cache,
                remaining,
                null);

        int how_many = 0;
        for (Image_similarity.Most_similar m : ms)
        {
            Path p3 = m.path();
            if (excluding.contains(p3)) continue;
            dummy_names.put(p3, i);
            excluding.add(p3);
            //logger.log(p3+" -> "+i);
            i++;
            how_many++;
            if (how_many > max_friend) break;
        }
        return i;
    }

}
