package klik.image_ml.image_similarity;

//SOURCES ./Image_feature_vector_actor.java
//SOURCES ./Image_feature_vector_message.java



import javafx.stage.Stage;
import klik.actor.Aborter;
import klik.actor.Job_termination_reporter;
import klik.actor.workers.Actor_engine_based_on_workers;
import klik.image_ml.Feature_vector;
import klik.level3.experimental.RAM_disk;
import klik.properties.Static_application_properties;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

//**********************************************************
public class Image_feature_vector_RAM_cache
//**********************************************************
{
    public final static boolean dbg = false;
    protected final Logger logger;
    private final Aborter aborter;
    protected final String cache_name;
    protected final Path cache_file_path;
    private final Map<String, Feature_vector> cache = new ConcurrentHashMap<>();
    private final Image_feature_vector_actor image_feature_vector_actor;

    private final int instance_number;
    private static int instance_number_generator = 0;
    private final Actor_engine_based_on_workers local_actor_engine;

    //**********************************************************
    public Image_feature_vector_RAM_cache(Path path, String cache_name_, Aborter aborter_, Logger logger_)
    //**********************************************************
    {
        instance_number = instance_number_generator++;
        logger = logger_;
        aborter = aborter_;
        cache_name = cache_name_;
        String local = cache_name+ path.toAbsolutePath();
        String cache_file_name = UUID.nameUUIDFromBytes(local.getBytes()) +".fv_cache";
        Path dir = get_image_feature_vector_cache_dir(null,logger);
        cache_file_path= Path.of(dir.toAbsolutePath().toString(), cache_file_name);
        if ( dbg) logger.log(cache_name+" cache file ="+cache_file_path);
        image_feature_vector_actor = new Image_feature_vector_actor();

        // reason to use workers is to limit the number of concurrent HTTP requests
        // to the vgg19 python servers that are not good at queuing requests
        local_actor_engine = new Actor_engine_based_on_workers(logger);

    }

    //**********************************************************
    public static Path get_image_feature_vector_cache_dir(Stage owner, Logger logger)
    //**********************************************************
    {
        if ( RAM_disk.get_use_RAM_disk(logger))
        {
            Path tmp_dir = RAM_disk.get_absolute_dir_on_RAM_disk(Static_application_properties.IMAGE_PROPERTIES_CACHE_DIR, owner, logger);
            //if (dbg)
            if (tmp_dir != null) {
                logger.log("Image feature vector cache folder=" + tmp_dir.toAbsolutePath());
            }
            return tmp_dir;
        }

        Path tmp_dir = Static_application_properties.get_absolute_dir_on_user_home(Static_application_properties.IMAGE_FEATURE_VECTOR_CACHE_DIR, false,logger);
        if (dbg) if (tmp_dir != null) {
            logger.log("Aspect ratio and rotation cache folder=" + tmp_dir.toAbsolutePath());
        }
        return tmp_dir;
    }

    //**********************************************************
    // if wait_if_needed is true, tr can be null
    public Feature_vector get_from_cache(Path p, Job_termination_reporter tr, boolean wait_if_needed)
    //**********************************************************
    {
        Feature_vector feature_vector =  cache.get(key_from_path(p));
        if ( feature_vector != null)
        {
            if ( tr != null) tr.has_ended("found in cache",null);
            return feature_vector;
        }
        if ( aborter.should_abort())
        {
            logger.log((instance_number+" PANIC aborter "+aborter.name+" reason="+aborter.reason+ " target path="+p));
            return null;
        }
        else {
            //logger.log(instance_number+" OK aborter "+aborter.name+" reason="+aborter.reason);
        }
        Image_feature_vector_message imp = new Image_feature_vector_message(p,this,aborter,logger);
        if ( wait_if_needed)
        {
            image_feature_vector_actor.run(imp); // blocking call
            Feature_vector x = cache.get(key_from_path(p));
            if ( x == null)
            {
                logger.log("PANIC null Feature_vector in cache after blocking call ");
            }
            return x;
        }
        local_actor_engine.run(image_feature_vector_actor,imp,tr,logger);
        return null;
    }

    //**********************************************************
    private static String key_from_path(Path p)
    //**********************************************************
    {
        return p.getFileName().toString();
    }
    //**********************************************************
    public void inject(Path path, Feature_vector fv)
    //**********************************************************
    {
        if(dbg) logger.log(cache_name+" inject "+path+" value="+fv );
        cache.put(key_from_path(path),fv);
    }

    //**********************************************************
    public void clear_feature_vector_RAM_cache()
    //**********************************************************
    {
        cache.clear();
        if (dbg) logger.log("feature vector cache file cleared");
    }
    //**********************************************************
    public synchronized void reload_cache_from_disk(Aborter aborter)
    //**********************************************************
    {
        int reloaded = 0;
        try(DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(cache_file_path.toFile()))))
        {
            int number_of_vectors = dis.readInt();
            for ( int i = 0; i < number_of_vectors; i++)
            {
                if ( aborter.should_abort()) return;
                String path_string = dis.readUTF();
                int size_of_vector = dis.readInt();
                double[] vector = new double[size_of_vector];
                for ( int j = 0; j < size_of_vector; j++)
                {
                    double val = dis.readDouble();
                    vector[j] = val;
                }
                Feature_vector fv = new Feature_vector(vector);
                cache.put(path_string,fv);
                reloaded++;
            }
        }
        catch (FileNotFoundException e)
        {
            logger.log("first time in this folder: "+e);
        }
        catch (IOException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
        }

        //if (dbg)
            logger.log(cache_name+": "+reloaded+" feature vectors reloaded from file");

        if ( dbg)
        {
            logger.log("\n\n\n********************* "+cache_name+ " CACHE************************");
            for (String s  : cache.keySet())
            {
                logger.log(s+" => "+cache.get(s));
            }
            logger.log("****************************************************************\n\n\n");
        }
    }

    //**********************************************************
    public void save_whole_cache_to_disk()
    //**********************************************************
    {
        int saved = 0;
        try(DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(cache_file_path.toFile()))))
        {
            dos.writeInt(cache.size());
            for(Map.Entry<String, Feature_vector> e : cache.entrySet())
            {
                saved++;
                dos.writeUTF(e.getKey());
                Feature_vector fv = e.getValue();
                dos.writeInt(fv.features.length);
                for ( int i = 0 ; i < fv.features.length; i++)
                {
                    dos.writeDouble(fv.features[i]);
                }
            }
        }
        catch (IOException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
        }

        //if (dbg)
            logger.log(saved +" feature vectors from cache saved to file");
    }



}
