package klik.browser.comparators;

//SOURCES ../../image_ml/image_similarity/Feature_vector_source_for_image_similarity.java;

import klik.actor.Aborter;
import klik.browser.Browser;
import klik.image_ml.Feature_vector;
import klik.image_ml.image_similarity.Image_similarity;
import klik.util.log.Logger;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;


//**********************************************************
public class Similarity_comparator_N extends Similarity_comparator
//**********************************************************
{
    public static final double SIMILARITY_THRESHOLD = 0.14;

    //**********************************************************
    public Similarity_comparator_N(Path folder, Aborter aborter, Logger logger_)
    //**********************************************************
    {
        super(folder, aborter, logger_);

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
        // ok "done" contains the pairs of closests
        // many remaining images are left out
        List<Path> remaining = new ArrayList<>(images);
        for ( Closest_neighbor cn : done)
        {
            Path p1 = cn.p1();
            remaining.remove(p1);
        }

        // we "extend" each pair by looking for the closest neighbors of each pair member
        Image_similarity similarity = new Image_similarity(folder,null,aborter, logger);

        dummy_names.clear();
        int max_friend = 2;
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
            List<Path> excluded = new ArrayList<>();
            excluded.add(p1);
            excluded.add(p2);
            i = extend(p1, excluded, similarity, remaining, i, max_friend);
            i = extend(p2, excluded, similarity, remaining, i, max_friend);
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
    private int extend(Path p1, List<Path> excluding, Image_similarity similarity, List<Path> remaining, int i, int max_friend)
    //**********************************************************
    {
        Feature_vector fv = fv_cache.get_from_cache(p1, null, true);
        List<Image_similarity.Most_similar> ms = similarity.find_similars_of(p1, fv,
                false, true,max_friend+3, Double.MAX_VALUE,
                remaining, null);

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

    /*
        int i = 0;
        record Dost(Path p, Double dist){}
        Comparator<? super Dost> comp = new Comparator<Dost>() {
            @Override
            public int compare(Dost o1, Dost o2)
            {
                    return o1.dist.compareTo(o2.dist);
            }
        };

        class Ensemble
        {
            List<Dost> dosts = new ArrayList<>();
        }
        List<Ensemble> sets  = new ArrayList<>();
        while (!images.isEmpty())
        {
            if ( aborter.should_abort()) return;
            Path p1 = images.remove(0);
            Dost d = new Dost(p1,0.0);
            Ensemble ensemble = new Ensemble();
            ensemble.dosts.add(d);
            sets.add(ensemble);

            Iterator<Path> it = images.iterator();
            while (it.hasNext())
            {
                if ( aborter.should_abort()) return;
                Path p2 = it.next();
                Double diff = similarity_cache.get(Path_pair.get(p1,p2));
                if ( diff == null)
                {
                    //logger.log("WTF diff == null for "+p1+" vs "+p2);
                    continue;
                }
                if ( diff < SIMILARITY_THRESHOLD)
                {
                    Dost d2 = new Dost(p2,diff);
                    ensemble.dosts.add(d2);
                    it.remove();
                }
            }
        }
        for ( Ensemble ensemble : sets)
        {
            Collections.sort(ensemble.dosts, comp);

            int count = 0;
            for ( Dost dost : ensemble.dosts)
            {
                if ( dummy_names.containsKey(dost.p())) continue;
                count++;
                if ( count >= 4) break;
            }
            if ( count< 2) continue;
            int count2 = 0;
            for ( Dost dost : ensemble.dosts)
            {
                logger.log(" "+dost.p()+" "+dost.dist());
                if ( dummy_names.containsKey(dost.p())) continue;
                dummy_names.put(dost.p(), i);
                i++;
                count2++;
                if ( count2 >= 4) break;
            }
        }
        for (Path p: images)
        {
            if ( dummy_names.containsKey(p)) continue;
            dummy_names.put(p,i);
            i++;
        }

        logger.log("init_dummy_names done !");

    }

*/
}
