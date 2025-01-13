package klik.browser.comparators;

//SOURCES ../../image_ml/image_similarity/Feature_vector_source_vgg19.java;

import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.actor.Job_termination_reporter;
import klik.browser.Clearable_cache;
import klik.image_ml.Feature_vector;
import klik.image_ml.Feature_vector_source;
import klik.image_ml.image_similarity.Feature_vector_source_vgg19;
import klik.image_ml.image_similarity.Image_feature_vector_RAM_cache;
import klik.image_ml.image_similarity.Image_similarity;
import klik.util.files_and_paths.Guess_file_type;
import klik.util.log.Logger;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;


//**********************************************************
public class Similarity_comparator implements Comparator<Path>, Clearable_cache
//**********************************************************
{
    private final static Map<Path, String> dummy_names = new HashMap<>();
    Map<Path_pair, Integer> distances  = new HashMap<>();
    private final Map<Path, Integer> path_to_int = new HashMap<>();
    List<Path> int_to_path = new ArrayList<>();
    private final ConcurrentHashMap<Path_pair, Double> similarities = new ConcurrentHashMap<>();

    private Image_feature_vector_RAM_cache fv_cache = null;
    Logger logger;
    private final Aborter aborter;
    boolean initialized = false;

    //**********************************************************
    public Similarity_comparator(Aborter aborter, Logger logger_)
    //**********************************************************
    {
        this.aborter = aborter;
        logger = logger_;
    }


    //**********************************************************
    @Override
    public void clear_RAM_cache()
    //**********************************************************
    {
        if(fv_cache != null) fv_cache.clear_feature_vector_RAM_cache();
        distances.clear();
    }


    //**********************************************************
    @Override
    public int compare(Path p1, Path p2)
    //**********************************************************
    {
        init(p1.getParent());

        //logger.log("compare "+p1+" vs "+p2);
        Integer i = path_to_int.get(p1);
        if ( i == null)
        {
            logger.log("WTF i == null for "+p1);
            return -1;
        }
        Integer j = path_to_int.get(p2);
        if ( j == null)
        {
            logger.log("WTF j == null for "+p2);
            return -1;
        }
        {
            Path_pair p = Path_pair.get(i,j);
            Integer d = distances.get(p);
            if (d != null) return d;
        }

        String dummy_name1 = dummy_names.get(p1);
        if ( dummy_name1 == null)
        {
            logger.log("WTF dummy_name1 == null for "+p1);
            dummy_name1 = p1.getFileName().toString();
        }

        String dummy_name2 = dummy_names.get(p2);
        if ( dummy_name2 == null)
        {
            logger.log("WTF dummy_name2 == null for "+p2);
            dummy_name2 = p2.getFileName().toString();
        }

        int d =  dummy_name1.compareTo(dummy_name2);
        distances.put(Path_pair.get(i,j), d);

        //logger.log("compare "+p1+" vs "+p2+" == "+d);
        return d;
    }


    //**********************************************************
    void init(Path folder)
    //**********************************************************
    {
        if ( initialized) return;
        initialized = true;
        logger.log("init_dummy_names for: "+folder);
        Image_similarity.Result result = Image_similarity.preload_all_feature_vector_in_cache(folder, aborter, logger);
        if (result == null)
        {
            return;
        }
        fv_cache = result.image_feature_vector_ram_cache();
        File[] files_ = folder.toFile().listFiles();
        List<Path> images = new ArrayList<>();

        for (int i = 0 ; i < files_.length; i++)
        {
            if (aborter.should_abort()) return;
            File f1 = files_[i];
            Path p1 = f1.toPath();
            path_to_int.put(p1, i);
            int_to_path.add(p1);
            if (Guess_file_type.is_file_an_image(f1))
            {
                images.add(p1);
            }

        }
        Similarity_cache_warmer_actor actor = new Similarity_cache_warmer_actor(int_to_path,path_to_int,images, fv_cache, similarities,logger);
        CountDownLatch cdl = new CountDownLatch(images.size());


        for (Path p1 : images)
        {
            int i =  path_to_int.get(p1);
            Similarity_cache_warmer_message m = new Similarity_cache_warmer_message(aborter,i);
            Job_termination_reporter tr = (message, job) -> {
                cdl.countDown();
                if ( cdl.getCount() % 100 == 0) logger.log(" similarity cache filler: "+cdl.getCount()+" for "+p1);
            };
            Actor_engine.run(actor, m,tr, logger);
        }

        try {
            cdl.await();
        } catch (InterruptedException e) {
            logger.log("init_dummy_names interrupted"+e);
        }

        if ( aborter.should_abort()) return;
        Collections.shuffle(images);
        while (!images.isEmpty())
        {
            if ( aborter.should_abort()) return;
            Path p1 = images.remove(0);
            dummy_names.put(p1,p1.getFileName().toString());
            Iterator<Path> it = images.iterator();
            while (it.hasNext())
            {
                if ( aborter.should_abort()) return;
                Path p2 = it.next();
                int i =  path_to_int.get(p1);
                int j =  path_to_int.get(p2);
                //logger.log("processing "+p1+" vs "+p2);
                Double diff = similarities.get(Path_pair.get(i,j));
                if ( diff == null)
                {
                    logger.log("WTF diff == null for "+p1+" vs "+p2);
                    continue;
                }
                if ( diff < 0.2)
                {
                    it.remove();
                    dummy_names.put(p2,p1.getFileName().toString()+diff+p2.getFileName().toString());
                }
            }
        }
        for (Path p: images)
        {
            dummy_names.put(p,p.getFileName().toString());
        }

        logger.log("init_dummy_names done !");
    }

}
