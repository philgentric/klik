package klik.browser.comparators;

import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.actor.Job_termination_reporter;
import klik.image_ml.image_similarity.Image_feature_vector_cache;
import klik.properties.Cache_folders;
import klik.properties.Static_application_properties;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;

import java.io.*;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

//**********************************************************
public class Similarity_cache
//**********************************************************
{
    private final Path folder;
    private final Aborter aborter;
    private final Path similarity_cache_file_path;
    private final Logger logger;
    private final ConcurrentHashMap<Path_pair, Double> similarities = new ConcurrentHashMap<>();

    //**********************************************************
    public Similarity_cache(Path folder, List<Path> images, Image_feature_vector_cache fv_cache, Aborter aborter, Logger logger)
    //**********************************************************
    {
        this.aborter = aborter;
        this.folder = folder;
        this.logger = logger;
        String cache_name = "similarity";
        String local = cache_name + folder.toAbsolutePath();
        String cache_file_name = UUID.nameUUIDFromBytes(local.getBytes()) + ".similarity_cache";
        Path dir = Static_application_properties.get_absolute_dir_on_user_home(Cache_folders.klik_image_similarity_cache.name(), false, logger);
        if (dir != null)
        {
            logger.log("similarity cache folder=" + dir.toAbsolutePath());
        }
        similarity_cache_file_path = Path.of(dir.toAbsolutePath().toString(), cache_file_name);

        if ( !reload_similarity_cache_from_disk())
        {
            // no cache on disk, have to recalculate
            Similarity_cache_warmer_actor actor = new Similarity_cache_warmer_actor(images, fv_cache, similarities,logger);
            CountDownLatch cdl = new CountDownLatch(images.size());
            for (Path p1 : images) {
                Similarity_cache_warmer_message m = new Similarity_cache_warmer_message(aborter, p1);
                Job_termination_reporter tr = (message, job) -> {
                    cdl.countDown();
                    if (cdl.getCount() % 100 == 0)
                        logger.log(" similarity cache filler: " + cdl.getCount() + " for " + p1);
                };
                Actor_engine.run(actor, m, tr, logger);
            }

            try {
                cdl.await();
            } catch (InterruptedException e) {
                logger.log("similarity cache interrupted" + e);
            }
            save_similarity_cache_to_disk();

            //logger.log("similarities min_similarity="+Similarity_cache_warmer_actor.min_similarity+" max_similarity="+Similarity_cache_warmer_actor.max_similarity);
        }
    }

    //**********************************************************
    public boolean reload_similarity_cache_from_disk()
    //**********************************************************
    {
        logger.log("reloading similarities from file");

        int reloaded = 0;
        try(DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(similarity_cache_file_path.toFile()))))
        {
            int number_of_items = dis.readInt();
            for ( int k = 0; k < number_of_items; k++)
            {
                if ( aborter.should_abort()) return false;
                String path1_string = dis.readUTF();
                String path2_string = dis.readUTF();
                double val = dis.readDouble();
                Path p1 = folder.resolve(path1_string);
                Path p2 = folder.resolve(path2_string);
                Path_pair p = Path_pair.get(p1,p2);
                similarities.put(p,val);
                if ( k%1000000 == 0) logger.log(k +"similarities loaded from disk ");
                reloaded++;
            }
            logger.log(reloaded+" similarities reloaded from file");
            return true;
        }
        catch (FileNotFoundException e)
        {
            logger.log("first time in this folder: "+e);
        }
        catch (IOException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
        }
        return false;
    }

    //**********************************************************
    public void save_similarity_cache_to_disk()
    //**********************************************************
    {

        int saved = 0;
        try(DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(similarity_cache_file_path.toFile()))))
        {
            dos.writeInt(similarities.size());
            for(Map.Entry<Path_pair, Double> e : similarities.entrySet())
            {
                Path_pair pp = e.getKey();
                Path pi1 = pp.i();
                Path pi2 = pp.j();
                dos.writeUTF(pi1.getFileName().toString());
                dos.writeUTF(pi2.getFileName().toString());
                dos.writeDouble(e.getValue());
                saved++;
                //logger.log("to disk similarity "+e.getValue()+" for "+pi1.getFileName().toString()+" "+pi2.getFileName().toString());
            }
        }
        catch (IOException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
        }

        //if (dbg)
        logger.log(saved +" similarities from cache saved to file");
    }

    //**********************************************************
    public void clear()
    //**********************************************************
    {
        similarities.clear();
    }

    //**********************************************************
    public Double get(Path_pair pathPair)
    //**********************************************************
    {
        return similarities.get(pathPair);
    }
}
