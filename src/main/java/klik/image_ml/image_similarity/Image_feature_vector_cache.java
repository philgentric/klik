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
import klik.util.files_and_paths.Guess_file_type;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

//**********************************************************
public class Image_feature_vector_cache
//**********************************************************
{
    public final static boolean dbg = false;
    protected final Logger logger;
    private final Aborter aborter;
    protected final String cache_name;
    protected final Path cache_file_path;


    public record Images_and_feature_vectors(Image_feature_vector_cache image_feature_vector_ram_cache, List<Path> images)
    {
    }
    public final static Map<Path, Images_and_feature_vectors> images_and_feature_vectors_cache = new HashMap<>();
    private final Map<String, Feature_vector> fv_cache = new ConcurrentHashMap<>();
    private final Image_feature_vector_actor image_feature_vector_actor;

    private final int instance_number;
    private static int instance_number_generator = 0;
    private final Actor_engine_based_on_workers local_actor_engine;

    //**********************************************************
    public Image_feature_vector_cache(Path path, String cache_name_, Aborter aborter_, Logger logger_)
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
            logger.log("Image feature vector cache folder=" + tmp_dir.toAbsolutePath());
        }
        return tmp_dir;
    }

    //**********************************************************
    // if wait_if_needed is true, tr can be null
    public Feature_vector get_from_cache(Path p, Job_termination_reporter tr, boolean wait_if_needed)
    //**********************************************************
    {
        Feature_vector feature_vector =  fv_cache.get(key_from_path(p));
        if ( feature_vector != null)
        {
            if ( tr != null) tr.has_ended("found in cache",null);
            return feature_vector;
        }
        if ( aborter.should_abort())
        {
            logger.log(("image feature vector cache instance#"+instance_number+" shutting down on aborter: ->"+aborter.name+"<- reason="+aborter.reason+ " target path="+p));
            return null;
        }
        else {
            //logger.log(instance_number+" OK aborter "+aborter.name+" reason="+aborter.reason);
        }
        Image_feature_vector_message imp = new Image_feature_vector_message(p,this,aborter,logger);
        if ( wait_if_needed)
        {
            image_feature_vector_actor.run(imp); // blocking call
            Feature_vector x = fv_cache.get(key_from_path(p));
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
        fv_cache.put(key_from_path(path), fv);
    }

    //**********************************************************
    public void clear_feature_vector_RAM_cache()
    //**********************************************************
    {
        fv_cache.clear();
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
                fv_cache.put(path_string, fv);
                reloaded++;
                if ( i%1000==0) logger.log(i+" feature vectors loaded from disk");
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
            for (String s  : fv_cache.keySet())
            {
                logger.log(s+" => "+ fv_cache.get(s));
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
            dos.writeInt(fv_cache.size());
            for(Map.Entry<String, Feature_vector> e : fv_cache.entrySet())
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


    //**********************************************************
    public static Images_and_feature_vectors preload_all_feature_vector_in_cache(Path folder_path, Aborter aborter, Logger logger)
    //**********************************************************
    {
        Images_and_feature_vectors images_and_feature_vectors = images_and_feature_vectors_cache.get(folder_path);
        if ( images_and_feature_vectors == null)
        {
            images_and_feature_vectors = read_from_disk_and_update(folder_path,aborter,logger);
            images_and_feature_vectors_cache.put(folder_path,images_and_feature_vectors);
            return images_and_feature_vectors;
        }
        images_and_feature_vectors.image_feature_vector_ram_cache.update(folder_path,aborter,logger);
        return images_and_feature_vectors;
    }

     /*
        if ( images.isEmpty()) return null;
        CountDownLatch cdl = new CountDownLatch(images.size());
        Job_termination_reporter tr = (message, job) -> {
            cdl.countDown();
            if ( cdl.getCount() % 100 == 0) logger.log("preloading FVs into cache: "+cdl.getCount());
        };
        // start the cache warming on many threads
        for ( int i = 0 ; i < images.size(); i++)
        {
            Path p1 = images.get(i);
            image_feature_vector_ram_cache.get_from_cache(p1,tr,false);
        }
        try {
            cdl.await();
        } catch (InterruptedException e) {
            logger.log(""+e);
            return null;
        }
        logger.log("preloading FVs into cache done :"+images.size());
        image_feature_vector_ram_cache.save_whole_cache_to_disk();
        result = new Images_and_feature_vectors(image_feature_vector_ram_cache, images);
        images_and_feature_vectors_cache.put(folder_path,result);
        return result;
    }*/

    //**********************************************************
    private static Images_and_feature_vectors read_from_disk_and_update(Path folder_path, Aborter aborter, Logger logger)
    //**********************************************************
    {
        Image_feature_vector_cache image_feature_vector_ram_cache = new Image_feature_vector_cache(folder_path, "image_feature_vectors", aborter, logger);
        image_feature_vector_ram_cache.reload_cache_from_disk(aborter);

        logger.log("read_from_disk "+image_feature_vector_ram_cache.fv_cache.size()+" fv from disk for:"+folder_path);
        return image_feature_vector_ram_cache.update( folder_path, aborter, logger);
    }

    //**********************************************************
    private  Images_and_feature_vectors update(Path folder_path, Aborter aborter, Logger logger)
    //**********************************************************
    {
        File[] files = folder_path.toFile().listFiles();
        if ( files == null) return null;
        List<Path> images = new ArrayList<>();
        List<Path> missing_images = new ArrayList<>();
        for (File f : files)
        {
            if ( f.isDirectory()) continue;
            if ( f.getName().startsWith("._")) continue;
            if ( !Guess_file_type.is_file_an_image(f)) continue;
            images.add(f.toPath());
            if ( !fv_cache.containsKey(f.toPath().getFileName().toString()))
            {
                missing_images.add(f.toPath());
            }
        }
        CountDownLatch cdl = new CountDownLatch(missing_images.size());
        Job_termination_reporter tr = (message, job) -> {
            cdl.countDown();
        };
        for (Path p :missing_images)
        {
            get_from_cache(p,tr,false);
        }
        try {
            cdl.await();
        } catch (InterruptedException e) {
            logger.log("Images_and_feature_vectors from_disk interrupted:"+e);
        }
        logger.log("update: "+missing_images.size()+" new images added for:"+folder_path);

        if (!missing_images.isEmpty()) save_whole_cache_to_disk();
        return new Images_and_feature_vectors(this, images);
    }

    //**********************************************************
    private boolean has(Path path)
    //**********************************************************
    {
        return fv_cache.containsKey(path.toString());
    }


}
