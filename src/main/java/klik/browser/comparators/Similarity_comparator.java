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
    //private final static Map<Path, Feature_vector> cache = new HashMap<>();
    private final static Map<Path, String> dummy_names = new HashMap<>();
    private Image_feature_vector_RAM_cache cache = null;
    Logger logger;
    private final Aborter aborter;

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
        if(cache != null) cache.clear_feature_vector_RAM_cache();
        distances.clear();
    }

    record Double_path(String p1, String p2){};
    Map<Double_path, Integer> distances  = new HashMap<>();

    //**********************************************************
    @Override
    public int compare(Path p1, Path p2)
    //**********************************************************
    {
        logger.log("compare "+p1+" vs "+p2);
        {
            Integer d = distances.get(new Double_path(p1.getFileName().toString(), p2.getFileName().toString()));
            if (d != null) return d;
            d = distances.get(new Double_path(p2.getFileName().toString(), p1.getFileName().toString()));
            if (d != null) return d;
        }
        String dummy_name1 = dummy_names.get(p1);
        if (dummy_name1 == null) init_dummy_names(p1.getParent());
        dummy_name1 = dummy_names.get(p1);
        if ( dummy_name1 == null)
        {
            logger.log("WTF dummy_name1 == null for "+p1);
            dummy_name1 = p1.getFileName().toString();
        }

        String dummy_name2 = dummy_names.get(p2);
        if (dummy_name2 == null) init_dummy_names(p2.getParent());
        dummy_name2 = dummy_names.get(p2);
        if ( dummy_name2 == null)
        {
            logger.log("WTF dummy_name2 == null for "+p2);
            dummy_name2 = p2.getFileName().toString();
        }

        Integer d =  dummy_name1.compareTo(dummy_name2);
        distances.put(new Double_path(p1.getFileName().toString(),p2.getFileName().toString()), d);

        logger.log("compare "+p1+" vs "+p2+" == "+d);
        return d;
    }


    //**********************************************************
    void init_dummy_names(Path folder)
    //**********************************************************
    {
        logger.log("init_dummy_names for: "+folder);
        Image_similarity.Result result = Image_similarity.preload_all_feature_vector_in_cache(folder, aborter, logger);
        if (result == null)
        {
            return;
        }
        Image_feature_vector_RAM_cache cache = result.image_feature_vector_ram_cache();
        File[] files = folder.toFile().listFiles();
        List<Path> files2 = new ArrayList<>();
        ConcurrentHashMap<Path_pair, Double> similarities = new ConcurrentHashMap<>();

        Dummy_name_actor actor = new Dummy_name_actor(files, cache, similarities,logger);
        int count = 0;
        for (int i = 0 ; i < files.length; i++)
        {
            if (aborter.should_abort()) return;
            File f1 = files[i];
            Path p1 = f1.toPath();
            if (!Guess_file_type.is_file_an_image(f1)) {
                continue;
            }
            count++;
        }
        CountDownLatch cdl = new CountDownLatch(count);


        for (int i = 0 ; i < files.length; i++)
        {
            File f1 = files[i];
            Path p1 = f1.toPath();
            if (!Guess_file_type.is_file_an_image(f1))
            {
                dummy_names.put(p1,p1.getFileName().toString());
                continue;
            }
            files2.add(p1);

            Dummy_name_message m = new Dummy_name_message(aborter, p1);
            int finalI = i;
            Job_termination_reporter tr = (message, job) -> {
                cdl.countDown();
                if ( cdl.getCount() % 100 == 0) logger.log(finalI + " init_dummy_names: "+cdl.getCount());
            };
            Actor_engine.run(actor, m,tr, logger);
        }

        try {
            cdl.await();
        } catch (InterruptedException e) {
            logger.log("init_dummy_names interrupted"+e);
        }

        if ( aborter.should_abort()) return;
        Collections.shuffle(files2);
        while (!files2.isEmpty())
        {
            if ( aborter.should_abort()) return;
            Path p1 = files2.remove(0);
            dummy_names.put(p1,p1.getFileName().toString());
            Iterator<Path> it = files2.iterator();
            int count2 = 0;
            while (it.hasNext())
            {
                if ( aborter.should_abort()) return;
                Path p2 = it.next();
                //logger.log("processing "+p1+" vs "+p2);
                Double diff =  similarities.get(new Path_pair(p1,p2));
                if ( diff == null) diff = similarities.get(new Path_pair(p2,p1));
                if ( diff == null)
                {
                    logger.log("WTF diff == null for "+p1+" vs "+p2);
                    continue;
                }
                if ( diff < 0.2)
                {
                    it.remove();
                    dummy_names.put(p2,p1.getFileName().toString()+diff+p2.getFileName().toString());
                    count2++;
                    //if (count > 7) break;
                }
            }
        }
        for (Path p: files2)
        {
            dummy_names.put(p,p.getFileName().toString());
        }

        logger.log("init_dummy_names done !");
    }

    //**********************************************************
    void init_dummy_names_old(Path folder)
    //**********************************************************
    {
        logger.log("init_dummy_names OLD "+folder);
        Image_similarity.Result result = Image_similarity.preload_all_feature_vector_in_cache(folder, aborter, logger);
        if (result == null)
        {
            return;
        }
        Image_feature_vector_RAM_cache cache = result.image_feature_vector_ram_cache();
        File[] files = folder.toFile().listFiles();
        List<Path> files2 = new ArrayList<>();
        ConcurrentHashMap<Path_pair, Double> similarities = new ConcurrentHashMap<>();
        int count2 =0;
        for (int i = 0 ; i < files.length; i++)
        {
            if ( aborter.should_abort()) return;
            File f1 = files[i];
            Path p1 = f1.toPath();
            if (!Guess_file_type.is_file_an_image(f1))
            {
                dummy_names.put(p1,p1.getFileName().toString());
                continue;
            }
            files2.add(p1);
            Feature_vector emb1 = cache.get_from_cache(p1,null,true);
            if ( emb1 == null)
            {
                emb1 = cache.get_from_cache(p1,null,true);
                if ( emb1 == null)
                {
                    logger.log("WTF emb1 == null for "+p1);
                    continue;
                }
            }
            for ( int j = i+1; j < files.length ; j++)
            {
                if ( aborter.should_abort()) return;
                File f2 = files[j];
                Path p2 = f2.toPath();
                if (!Guess_file_type.is_file_an_image(f2))
                {
                    continue;
                }
                //logger.log("processing "+p1+" vs "+p2);
                Feature_vector emb2 = cache.get_from_cache(p2,null,true);
                if ( emb2 == null)
                {
                    emb2 = cache.get_from_cache(p2,null,true);
                    if ( emb2 == null)
                    {
                        logger.log("WTF emb2 == null for "+p2);
                        continue;
                    }
                }
                double diff =  emb1.compare(emb2);
                similarities.put(new Path_pair(p1,p2), diff);

                count2++;
                if( count2%1000 == 0) logger.log("init_dummy_names OLD "+count2);
                //process(p1,p2, diff);
            }
        }

        Collections.shuffle(files2);
        while (!files2.isEmpty())
        {
            if ( aborter.should_abort()) return;
            Path p1 = files2.remove(0);
            dummy_names.put(p1,p1.getFileName().toString());
            Iterator<Path> it = files2.iterator();
            int count = 0;
            while (it.hasNext())
            {
                if ( aborter.should_abort()) return;
                Path p2 = it.next();
                //logger.log("processing "+p1+" vs "+p2);
                Double diff =  similarities.get(new Path_pair(p1,p2));
                if ( diff == null) diff = similarities.get(new Path_pair(p2,p1));
                if ( diff == null)
                {
                    logger.log("WTF diff == null for "+p1+" vs "+p2);
                    continue;
                }
                if ( diff < 0.2)
                {
                    it.remove();
                    dummy_names.put(p2,p1.getFileName().toString()+diff+p2.getFileName().toString());
                    count++;
                    //if (count > 7) break;
                }
            }
        }
        for (Path p: files2)
        {
            dummy_names.put(p,p.getFileName().toString());
        }

        logger.log("init_dummy_names done !");
    }


    void process(Path p1, Path p2, double diff)
    {
        logger.log("image similarity "+diff+" for:\n"+p1.toAbsolutePath()+"\n"+p2.toAbsolutePath());

        String dummy_name1 = dummy_names.get(p1);
        String dummy_name2 = dummy_names.get(p2);
        if ( dummy_name1 == null)
        {
            if ( dummy_name2 == null)
            {
                dummy_name1 = p1.getFileName().toString();
                dummy_names.put(p1, dummy_name1);
                if (diff < 0.8)
                {
                    dummy_name2 = dummy_name1 + "_" + diff;// + p2.getFileName().toString();
                    System.out.println(dummy_name1+" diff = " + diff+" is small "+dummy_name2);
                    dummy_names.put(p2, dummy_name2);
                }
            }
            else
            {
                if (diff < 0.8)
                {
                    dummy_name1 = dummy_name2 + "_" + diff;// + p1.getFileName().toString();
                    System.out.println(dummy_name1+" diff = " + diff+" is small "+dummy_name2);
                    dummy_names.put(p1, dummy_name1);
                }
                else
                {
                    dummy_name1 = p1.getFileName().toString();
                    dummy_names.put(p1, dummy_name1);
                }
            }
        }
        else
        {
            if ( dummy_name2 == null)
            {
                if (diff < 0.8)
                {
                    dummy_name2 = dummy_name1 + "_" + diff;// + p2.getFileName().toString();
                    System.out.println(dummy_name1+" diff = " + diff+" is small "+dummy_name2);
                    dummy_names.put(p2, dummy_name2);
                }
                else
                {
                    dummy_name2 = p2.getFileName().toString();
                    dummy_names.put(p2, dummy_name2);
                }
            }
            else
            {
                if (diff < 0.8)
                {
                    if ( dummy_name1.equals(p1.getFileName().toString()))
                    {
                        dummy_name1 = dummy_name2 + "_" + diff;// + p1.getFileName().toString();
                        System.out.println(dummy_name1+" diff = " + diff+" is small "+dummy_name2);
                        dummy_names.put(p1, dummy_name1);
                    }
                    else if ( dummy_name2.equals(p2.getFileName().toString()))
                    {
                        dummy_name2 = dummy_name1 + "_" + diff;// + p2.getFileName().toString();
                        System.out.println(dummy_name1+" diff = " + diff+" is small "+dummy_name2);
                        dummy_names.put(p2, dummy_name2);
                    }
                    else
                    {
                        System.out.println(dummy_name1 + " AHHH SHIT diff = " + diff + " is small " + dummy_name2+" lexical distance = " +dummy_name1.compareTo(dummy_name2));
                    }
                }
            }
        }
    }
    //**********************************************************
    int default_val(Path f1, Path f2)
    //**********************************************************
    {
        int diff = f1.getFileName().toString().compareToIgnoreCase(f2.getFileName().toString());
        if (diff != 0) return diff;
        // in case the file names differ by case
        return f1.getFileName().toString().compareTo(f2.getFileName().toString());
    }
    //**********************************************************
    private Feature_vector compute_VGG19(Path p)
    //**********************************************************
    {
        Feature_vector_source feature_vector_source = new Feature_vector_source_vgg19();
        return feature_vector_source.get_feature_vector_from_server(p,logger);
    }
}
