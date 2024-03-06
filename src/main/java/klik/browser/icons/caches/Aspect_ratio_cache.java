package klik.browser.icons.caches;

import javafx.stage.Stage;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.browser.icons.Paths_manager;
import klik.browser.icons.Refresh_target;
import klik.files_and_paths.Ding;
import klik.files_and_paths.Guess_file_type;
import klik.properties.File_sort_by;
import klik.properties.Static_application_properties;
import klik.util.Logger;
import klik.util.execute.Scheduled_thread_pool;
import org.apache.commons.io.FilenameUtils;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

//**********************************************************
public class Aspect_ratio_cache extends Cache_for_doubles
//**********************************************************
{
    private static final boolean dbg = false;
    private Aspect_ratio_actor aspect_ratio_actor;

    public AtomicInteger in_flight = new AtomicInteger(0);
    //**********************************************************
    public Aspect_ratio_cache(Path folder_path, Aborter aborter_, Logger logger_)
    //**********************************************************
    {
        super(folder_path,"aspect_ratio_cache",aborter_,logger_);
        if(dbg) logger.log("Aspect_ratio_cache CONSTRUCTOR for: "+folder_path);
        aspect_ratio_actor = new Aspect_ratio_actor(in_flight);
    }


    //**********************************************************
    public Double get_aspect_ratio(Path path)
    //**********************************************************
    {
        //logger.log(Stack_trace_getter.get_stack_trace("get_aspect_ratio"));
        Double aspect_ratio = get_from_cache(path);
        if ( aspect_ratio == null)
        {
            if(dbg) logger.log("not in RAM for: "+path.toAbsolutePath());
            Double guess;
            if (Guess_file_type.is_this_extension_a_pdf(FilenameUtils.getExtension(path.getFileName().toString())))
            {
                if(dbg) logger.log("not in RAM for: "+path.toAbsolutePath()+" setting ISO "+ Aspect_ratio_actor.ISO_A4_aspect_ratio);
                guess = Aspect_ratio_actor.ISO_A4_aspect_ratio;
            }
            else
            {
                guess = 1.0;
            }
            put_in_cache(path, guess);// store it, but it will be soon replaced by the actor
            in_flight.incrementAndGet();
            Actor_engine.run(aspect_ratio_actor, new Aspect_ratio_message(path,this,aborter,logger),null,logger);
            return guess;
        }
        return aspect_ratio;
    }




    private AtomicBoolean done_look_for_end = new AtomicBoolean(false);
    private Runnable look_for_end_runnable = null;
    ScheduledFuture<?>  the_scheduled_future = null;
    //**********************************************************
    public void look_for_end(Paths_manager paths_manager, Refresh_target refresh_target, Stage stage)
    //**********************************************************
    {
        if ( done_look_for_end.get()) return;
        if (look_for_end_runnable != null)
        {
            //already running;
            return;
        }

        long start = System.currentTimeMillis();
        look_for_end_runnable = new Runnable() {
            @Override
            public void run()
            {

                if ( aborter.should_abort())
                {
                    logger.log("Aspect ratio cache look_for_end_runnable aborting");
                    the_scheduled_future.cancel(true);
                    return;
                }
                if ( in_flight.get() > 0)
                {
                    //if ( dbg)
                        logger.log("aspect ratios remaining: "+aspect_ratio_actor.in_flight.get());
                    return;
                }

                // this is the end
                the_scheduled_future.cancel(true);
                done_look_for_end.set(true);
                if ( System.currentTimeMillis()-start > 5_000)
                {
                    if (Static_application_properties.get_ding(logger))
                    {
                        Ding.play("Aspect_ratio_cache: done acquiring all aspect ratios",logger);
                    }
                }
                Comparator<Path> local_file_comparator = null;
                if (Static_application_properties.get_sort_files_by(logger)== File_sort_by.RANDOM_ASPECT_RATIO)
                {
                    local_file_comparator = new Aspect_ratio_comparator_random();
                }
                else if (Static_application_properties.get_sort_files_by(logger)== File_sort_by.ASPECT_RATIO)
                {
                    local_file_comparator = new Aspect_ratio_comparator();
                }
                if ( local_file_comparator != null)
                {
                    paths_manager.image_file_comparator = local_file_comparator;
                    paths_manager.iconized = new ConcurrentSkipListMap<>(local_file_comparator);
                }
                if ( dbg) logger.log("aspect ratios engine done, going to refresh");
                refresh_target.refresh();
            }
        };
        the_scheduled_future = Scheduled_thread_pool.execute(look_for_end_runnable, 100, TimeUnit.MILLISECONDS);
    }


    //**********************************************************
    static double round_to_one_decimal(double d)
    //**********************************************************
    {
        double dd = 10.0*d;
        return Math.round(dd)*10.0;
    }



    //**********************************************************
    class Aspect_ratio_comparator implements Comparator<Path>
    {
        @Override
        public int compare(Path p1, Path p2) {
            Double d1 = get_aspect_ratio(p1);
            Double d1r= round_to_one_decimal(d1);
            Double d2 = get_aspect_ratio(p2);
            Double d2r= round_to_one_decimal(d2);
            int diff =  d1r.compareTo(d2r);
            if ( diff != 0) return diff;
            return (p1.toString().compareTo(p2.toString()));
        }
    };

    //**********************************************************
    class Aspect_ratio_comparator_random implements Comparator<Path>
    {
        long seed;
        public Aspect_ratio_comparator_random()
        {
            Random r = new Random();
            seed = r.nextLong();
        }
        @Override
        public int compare(Path p1, Path p2) {
            // round the aspect ratio a bit
            Double d1 = get_aspect_ratio(p1);
            Double d1r= round_to_one_decimal(d1);
            Double d2 = get_aspect_ratio(p2);
            Double d2r= round_to_one_decimal(d2);
            int diff = d1r.compareTo(d2r);
            if (diff != 0) return diff;
            // same aspect ratio so the order must be pseudo random... but consistent for each comparator instance
            long s1 = UUID.nameUUIDFromBytes(p1.getFileName().toString().getBytes()).getMostSignificantBits();
            Long l1 = new Random(seed*s1).nextLong();
            long s2 = UUID.nameUUIDFromBytes(p2.getFileName().toString().getBytes()).getMostSignificantBits();
            Long l2 = new Random(seed*s2).nextLong();
            return l1.compareTo(l2);
        }
    };
}
