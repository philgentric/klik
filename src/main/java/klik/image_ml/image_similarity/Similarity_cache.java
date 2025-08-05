package klik.image_ml.image_similarity;
//SOURCES ./Similarity_cache_warmer_actor.java
//SOURCES ./Similarity_cache_warmer_message.java

import javafx.stage.Window;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.actor.Job_termination_reporter;
import klik.browser.virtual_landscape.Path_list_provider;
import klik.properties.Cache_folder;
import klik.properties.Non_booleans_properties;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import klik.util.ui.Hourglass;
import klik.util.ui.Show_running_film_frame_with_abort_button;

import java.io.*;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

//**********************************************************
public class Similarity_cache
//**********************************************************
{
    private final Path_list_provider path_list_provider;
    private final Path similarity_cache_file_path;
    private final Logger logger;
    private final ConcurrentHashMap<Path_pair, Double> similarities = new ConcurrentHashMap<>();

    //**********************************************************
    public Similarity_cache(Path_list_provider path_list_provider, Window owner, double x, double y, Aborter browser_aborter, Logger logger)
    //**********************************************************
    {
        this.path_list_provider = path_list_provider;
        this.logger = logger;
        String cache_name = "similarity";
        String local = cache_name + path_list_provider.get_folder_path();
        String cache_file_name = UUID.nameUUIDFromBytes(local.getBytes()) + ".similarity_cache";
        Path dir = Non_booleans_properties.get_absolute_hidden_dir_on_user_home(Cache_folder.klik_image_similarity_cache.name(), false, owner,logger);
        if (dir != null)
        {
            logger.log("similarity cache folder=" + dir.toAbsolutePath());
        }
        similarity_cache_file_path = Path.of(dir.toAbsolutePath().toString(), cache_file_name);

        if ( !reload_similarity_cache_from_disk(browser_aborter))
        {
            // no cache on disk, have to recalculate and save
            // in a thread!

            Image_feature_vector_cache.Images_and_feature_vectors result = Image_feature_vector_cache.preload_all_feature_vector_in_cache(path_list_provider, owner, x, y, browser_aborter, logger);
            if (result == null)
            {
                logger.log(Stack_trace_getter.get_stack_trace("ERROR: cannot preload all feature vectors"));
                return;
            }
            Image_feature_vector_cache fv_cache = result.fv_cache();
            fill_cache_and_save_to_disk(result.images(),fv_cache, owner, x,y,browser_aborter, logger);

            //logger.log("similarities min_similarity="+Similarity_cache_warmer_actor.min_similarity+" max_similarity="+Similarity_cache_warmer_actor.max_similarity);
        }
    }

    //**********************************************************
    private void fill_cache_and_save_to_disk(List<Path> images, Image_feature_vector_cache fv_cache, Window owner, double x, double y, Aborter browser_aborter, Logger logger)
    //**********************************************************
    {
        AtomicInteger in_flight = new AtomicInteger(images.size());
        Hourglass hourglass = Show_running_film_frame_with_abort_button.show_running_film(in_flight,
                "Wait: computing image similarities",30*60,x,y,logger);
        Similarity_cache_warmer_actor actor = new Similarity_cache_warmer_actor(images, fv_cache, similarities, logger);
        CountDownLatch cdl = new CountDownLatch(images.size());
        for (Path p1 : images)
        {
            if ( browser_aborter.should_abort())
            {
                logger.log("aborting Similarity_cache "+ browser_aborter.reason);
                break;
            }
            Similarity_cache_warmer_message m = new Similarity_cache_warmer_message(owner,browser_aborter, p1);
            Job_termination_reporter tr = (message, job) -> {
                cdl.countDown();
                in_flight.decrementAndGet();
                if (cdl.getCount() % 100 == 0)
                    logger.log(" Remaining to fill similarity cache: " + cdl.getCount());
            };
            Actor_engine.run(actor, m, tr, logger);
        }

        try {
            cdl.await();
        } catch (InterruptedException e) {
            hourglass.close();
            logger.log("similarity cache interrupted" + e);
        }
        save_similarity_cache_to_disk();
        hourglass.close();
    }

    //**********************************************************
    public boolean reload_similarity_cache_from_disk(Aborter browser_aborter)
    //**********************************************************
    {
        logger.log("reloading similarities from disk");

        int reloaded = 0;
        try(DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(similarity_cache_file_path.toFile()))))
        {
            int number_of_items = dis.readInt();
            for ( int k = 0; k < number_of_items; k++)
            {
                if ( browser_aborter.should_abort())
                {
                    logger.log("aborting : Similarity_cache::reload_similarity_cache_from_disk "+browser_aborter.reason);
                    return false;
                }
                String path1_string = dis.readUTF();
                String path2_string = dis.readUTF();
                double val = dis.readDouble();
                Path p1 = path_list_provider.resolve(path1_string);
                Path p2 = path_list_provider.resolve(path2_string);
                Path_pair p = Path_pair.get(p1,p2);
                similarities.put(p,val);
                if ( k%10000 == 0) logger.log("wait: already "+k +" similarities loaded from disk ....");
                reloaded++;
            }
            logger.log("Done: "+reloaded+" similarities reloaded from disk");
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
