package klik.browser.comparators;

//SOURCES ../../image_ml/image_similarity/Feature_vector_source_embeddings.java;

import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.actor.Job_termination_reporter;
import klik.browser.Clearable_RAM_cache;
import klik.image_ml.image_similarity.Image_feature_vector_cache;
import klik.properties.Static_application_properties;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;


//**********************************************************
public class Similarity_comparator_1 implements Comparator<Path>, Clearable_RAM_cache
//**********************************************************
{
    private final static Map<Path, String> dummy_names = new HashMap<>();
    public static final double THRESHOLD = 0.5;
    private Map<Path_pair, Integer> distances  = new HashMap<>();

    private Image_feature_vector_cache fv_cache = null;
    Logger logger;
    private final Aborter aborter;
    boolean initialized = false;
    private Similarity_cache similarity_cache;

    //**********************************************************
    public Similarity_comparator_1(Path folder, Aborter aborter, Logger logger_)
    //**********************************************************
    {
        this.aborter = aborter;
        logger = logger_;






        Image_feature_vector_cache.Images_and_feature_vectors result = Image_feature_vector_cache.preload_all_feature_vector_in_cache(folder, aborter, logger);
        if (result == null)
        {
            return;
        }
        fv_cache = result.image_feature_vector_ram_cache();
        List<Path> images = new ArrayList<>(result.images());

        similarity_cache = new Similarity_cache(folder,images, fv_cache,aborter,logger);

        //logger.log("\n\nmin "+Similarity_cache_warmer_actor.min+" max "+Similarity_cache_warmer_actor.max);
        if ( aborter.should_abort()) return;
        Collections.shuffle(images);

        List<Closest_neighbor> candidates = new ArrayList<>();
        List<Closest_neighbor> done = new ArrayList<>();
        Map<Path, Closest_neighbor> map = new HashMap<>();
        for ( Path p1 : images)
        {
            Closest_neighbor cn = find_closest_of(p1, images);
            if ( cn == null)
            {
                logger.log("find_closest_of == null for "+p1);
                continue;
            }
            else
            {
                logger.log("find_closest_of "+p1+" == "+cn.dist());
            }
            candidates.add(cn);
            map.put(p1,cn);
        }
        for ( int i = 0 ; i < 10 ; i++)
        {
            secouer(candidates, done, map);
            logger.log("after secouer done: "+done.size()+" candidates: "+candidates.size());
            if ( candidates.isEmpty()) break;
        }

        dummy_names.clear();
        Map<Path,Path> name_changed_by = new HashMap<>();
        List<Path> dont_change = new ArrayList<>();
        for ( Closest_neighbor cn : done)
        {

            if ( dummy_names.get(cn.p1()) != null)
            {
                // P1 name already changed ? by who?
                Path before = name_changed_by.get(cn.p1());
                if ( before == null)
                {
                    logger.log("WTF before == null for "+cn.p1());
                }
                else
                {
                    Closest_neighbor other = map.get(before);
                    logger.log("ALREADY DONE\n" +cn.p1().getFileName().toString() +" closest neighbor is "+cn.closest().getFileName().toString()+" at: "+cn.dist());
                    logger.log(other.p1().getFileName().toString() +" closest neighbor is "+other.closest().getFileName().toString()+" at: "+other.dist());

                }
                continue;
            }

            if ( dont_change.contains(cn.p1()) )
            {
                logger.log("not changing "+cn.p1().getFileName().toString()+" to "+cn.closest().getFileName().toString()+" as it was already used");
                continue;
            }
            logger.log("\n" +cn.p1().getFileName().toString() +" closest neighbor is "+cn.closest().getFileName().toString()+" at: "+cn.dist());
            logger.log("changing "+cn.p1().getFileName().toString()+" to "+cn.closest().getFileName().toString());
            dummy_names.put(cn.p1(), cn.closest().getFileName().toString()+cn.dist());
            dont_change.add(cn.closest());
            name_changed_by.put(cn.p1(), cn.closest());
        }
        int count  = 0;
        for ( Path p : images)
        {
            if ( !dummy_names.containsKey(p))
            {
                logger.log("was not mapped? "+p);
                count++;
                dummy_names.put(p, p.getFileName().toString());
            }
        }
        logger.log("init_dummy_names done !"+count+" not maaped");



    }


    //**********************************************************
    @Override
    public void clear_RAM_cache()
    //**********************************************************
    {
        if(fv_cache != null) fv_cache.clear_feature_vector_RAM_cache();
        distances.clear();
        dummy_names.clear();
        similarity_cache.clear();
    }


    //**********************************************************
    @Override
    public int compare(Path p1, Path p2)
    //**********************************************************
    {
        Path_pair pp = Path_pair.get(p1,p2);
        Integer d = distances.get(pp);
        if (d != null) return d;

        String dummy_name1 = dummy_names.get(p1);
        if ( dummy_name1 == null)
        {
            init(p1.getParent());
        }
        if ( dummy_name1 == null)
        {
            //logger.log("WTF dummy_name1 == null for "+p1);
            dummy_name1 = p1.getFileName().toString();
            dummy_names.put(p1,dummy_name1);
        }

        String dummy_name2 = dummy_names.get(p2);
        if ( dummy_name2 == null)
        {
            //logger.log("WTF dummy_name2 == null for "+p2);
            dummy_name2 = p2.getFileName().toString();
            dummy_names.put(p2,dummy_name2);
        }

        d =  dummy_name1.compareTo(dummy_name2);
        distances.put(pp, d);
        //logger.log("compare "+p1+" vs "+p2+" == "+d);
        return d;
    }


    //**********************************************************
    void init(Path folder)
    //**********************************************************
    {
        if ( initialized) return;
        initialized = true;
        //Ml_servers_util.init_image_similarity(logger);


    }

    //**********************************************************
    private Closest_neighbor find_closest_of(Path p1, List<Path> images)
    //**********************************************************
    {
        double min = Double.MAX_VALUE;
        Path closest = null;
        for ( Path p2 : images)
        {
            if ( p1.equals(p2)) continue;
            Double d = similarity_cache.get(Path_pair.get(p1,p2));
            if ( d == null)
            {
                // typically means the similarity is above the THRESHOLD
                //logger.log("WTF no similarity for "+p1+" and "+p2);
                continue;
            }
            if (  d < min)
            {
                min = d;
                closest = p2;
            }
        }
        if ( closest == null) return null;
        Closest_neighbor cn = new Closest_neighbor(p1, closest,min);
        return cn;
    }

    record Closest_neighbor(Path p1, Path closest, double dist){} // P1 has P2 as its closest neighbor (but maybe P2 has P3 as its closest neighbor)


    //**********************************************************
    private void secouer(List<Closest_neighbor> candidates, List<Closest_neighbor> done, Map<Path, Closest_neighbor> map)
    //**********************************************************
    {
        Iterator<Closest_neighbor> it = candidates.iterator();
        while (it.hasNext())
        {
            Closest_neighbor cn = it.next();
            // we know that cn.p2() IS the closest image to cn.p1()
            // but is it symetric?
            Closest_neighbor cn2 = map.get(cn.closest());

            if ( cn2.closest().equals(cn.p1()))
            {
                logger.log("symetric "+cn.p1()+" "+cn.closest()+" "+cn.dist());
                if ( !done.contains(cn)) done.add(cn);
            }
            else
            {
                it.remove();
            }
        }
    }



}
