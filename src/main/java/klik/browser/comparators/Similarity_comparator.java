package klik.browser.comparators;

//SOURCES ../../image_ml/image_similarity/Feature_vector_source_vgg19.java;

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
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;


//**********************************************************
public class Similarity_comparator implements Comparator<Path>, Clearable_cache
//**********************************************************
{
    //private final static Map<Path, Feature_vector> cache = new HashMap<>();
    private final static Map<Path, String> dummy_names = new HashMap<>();
    private Image_feature_vector_RAM_cache cache = null;
    Logger logger;

    //**********************************************************
    public Similarity_comparator(Logger logger_)
    //**********************************************************
    {
        logger = logger_;
    }


    //**********************************************************
    @Override
    public void clear_RAM_cache()
    //**********************************************************
    {
        cache.clear_feature_vector_RAM_cache();
    }

    //**********************************************************
    @Override
    public int compare(Path p1, Path p2)
    //**********************************************************
    {
        String dummy_name1 = null;
        if (!Guess_file_type.is_file_an_image(p1.toFile()))
        {
            dummy_name1 = p1.getFileName().toString();
        }
        else
        {
            dummy_name1 = dummy_names.get(p1);
            if (dummy_name1 == null) init_dummy_names(p1.getParent());
            dummy_name1 = dummy_names.get(p1);
        }

        String dummy_name2 = null;
        if (!Guess_file_type.is_file_an_image(p2.toFile()))
        {
            dummy_name2 = p2.getFileName().toString();
        }
        else
        {
            dummy_name2 = dummy_names.get(p2);
            if (dummy_name2 == null) init_dummy_names(p2.getParent());
            dummy_name2 = dummy_names.get(p2);
        }

        return (dummy_name1.toString().compareTo(dummy_name2.toString()));
    }


    //**********************************************************
    void init_dummy_names(Path folder)
    //**********************************************************
    {
        Image_similarity.Result result = Image_similarity.preload_all_feature_vector_in_cache(folder, logger);
        if (result == null)
        {
            return;
        }

        Image_feature_vector_RAM_cache cache = result.image_feature_vector_ram_cache();

        File files[] = folder.toFile().listFiles();

        for (int i = 0 ; i < files.length; i++)
        {
            File f1 = files[i];
            if (!Guess_file_type.is_file_an_image(f1)) continue;
            Path p1 = f1.toPath();
            logger.log("processing "+p1);
            Feature_vector emb1 = cache.get_from_cache(p1,null,true);

            for ( int j = i+1; j < files.length ; j++)
            {
                File f2 = files[j];
                if ( f1.toPath().toString().equals(f2.toPath().toString())) continue;
                if (!Guess_file_type.is_file_an_image(f2)) continue;
                Path p2 = f2.toPath();
                logger.log("processing "+p2);
                Feature_vector emb2 = cache.get_from_cache(p2,null,true);
                process(emb1,emb2,p1,p2);
            }
        }
        logger.log("init_dummy_names done !");
    }


    void process(Feature_vector emb1, Feature_vector emb2, Path p1, Path p2)
    {

        double diff =  emb1.compare(emb2);
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
