package klik.browser.comparators;

import klik.browser.Clearable_cache;
import klik.face_recognition.Feature_vector;
import klik.face_recognition.Feature_vector_source;
import klik.face_recognition.Feature_vector_source_vgg19;
import klik.util.files_and_paths.Guess_file_type;
import klik.util.log.Logger;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

//**********************************************************
public class Similarity_comparator implements Comparator<Path>, Clearable_cache
//**********************************************************
{
    private final static Map<Path, Feature_vector> cache = new HashMap<>();
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
        cache.clear();
    }

    //**********************************************************
    @Override
    public int compare(Path p1, Path p2)
    //**********************************************************
    {
        Feature_vector emb1 = cache.get(p1);
        if ( emb1 == null)
        {
            if (!Guess_file_type.is_file_an_image(p1.toFile())) return 0;
            emb1 = compute_VGG19(p1);
            cache.put(p1,emb1);
        }
        Feature_vector emb2 = cache.get(p2);
        if ( emb2 == null)
        {
            if (!Guess_file_type.is_file_an_image(p2.toFile())) return 0;
            emb2 = compute_VGG19(p2);
            cache.put(p2,emb2);
        }

        double diff =  emb1.compare(emb2);
        logger.log("image similarity "+diff+" for:\n"+p1.toAbsolutePath()+"\n"+p2.toAbsolutePath());
        if ( diff < 0.01) return 0;
        return (p1.getFileName().toString().compareTo(p2.getFileName().toString()));
    }

    //**********************************************************
    private Feature_vector compute_VGG19(Path p)
    //**********************************************************
    {
        Feature_vector_source feature_vector_source = new Feature_vector_source_vgg19();
        return feature_vector_source.get_feature_vector_from_server(p,logger);
    }
}
