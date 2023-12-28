package klik.browser.icons;

import javafx.application.Platform;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.files_and_paths.Files_and_Paths;
import klik.properties.File_sort_by;
import klik.properties.Properties_manager;
import klik.properties.Static_application_properties;
import klik.util.Logger;
import klik.util.Popups;
import klik.util.Threads;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

//**********************************************************
public class Aspect_ratio_cache
//**********************************************************
{
    private static final boolean dbg = false;

    public record Aspect_ratio(double value, boolean truth){}
    Map<String, Aspect_ratio> aspect_ratio_cache = new ConcurrentHashMap<>();
    private Aspect_ratio_actor aspect_ratio_actor;

    public final Logger logger;
    public final Aborter aborter;
    private final String cache_file_name;
    private final Path path_of_aspect_ratio_cache_file;

    public AtomicInteger in_flight = new AtomicInteger(0);

    //**********************************************************
    public Aspect_ratio_cache(Path folder_path, Aborter aborter_, Logger logger_)
    //**********************************************************
    {
        logger = logger_;
        String s = folder_path.toAbsolutePath().toString();
        cache_file_name = UUID.nameUUIDFromBytes(s.getBytes()).toString();
        {
            Path dir = Files_and_Paths.get_aspect_ratio_cache_dir(logger);
            path_of_aspect_ratio_cache_file= Path.of(dir.toAbsolutePath().toString(), cache_file_name);
        }
        aborter = aborter_;
        aspect_ratio_actor = new Aspect_ratio_actor(in_flight);
    }

    //**********************************************************
    class Aspect_ratio_comparator implements Comparator<Path>
            //**********************************************************
    {

        @Override
        public int compare(Path p1, Path p2) {
            Double d1 = get_aspect_ratio(p1);
            Double d1r= Double.valueOf(String.format("%.1f",d1));
            Double d2 = get_aspect_ratio(p2);
            Double d2r= Double.valueOf(String.format("%.1f",d2));
            return d1r.compareTo(d2r);
        }
    };

    //**********************************************************
    class Aspect_ratio_comparator_random implements Comparator<Path>
            //**********************************************************
    {
        private static final Locale local_us = new Locale("en");

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
            Double d1r= Double.valueOf(String.format(local_us, "%.1f",d1));
            Double d2 = get_aspect_ratio(p2);
            Double d2r= Double.valueOf(String.format(local_us, "%.1f",d2));

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


    //**********************************************************
    class Random_comparator_aspect_ratio implements Comparator<Path>
            //**********************************************************
    {

        long seed;
        public Random_comparator_aspect_ratio()
        {
            Random r = new Random();
            seed = r.nextLong();
        }
        @Override
        public int compare(Path p1, Path p2) {

            long s1 = UUID.nameUUIDFromBytes(p1.getFileName().toString().getBytes()).getMostSignificantBits();
            Long l1 = new Random(seed*s1).nextLong();
            long s2 = UUID.nameUUIDFromBytes(p2.getFileName().toString().getBytes()).getMostSignificantBits();
            Long l2 = new Random(seed*s2).nextLong();
            int diff=  l1.compareTo(l2);
            if (diff != 0) return diff;

            Double d1 = get_aspect_ratio(p1);
            Double d1r= Double.valueOf(String.format("%.1f",d1));
            Double d2 = get_aspect_ratio(p2);
            Double d2r= Double.valueOf(String.format("%.1f",d2));

            return d1r.compareTo(d2r);
        }

    };




    //**********************************************************
    static String key_from_path(Path p)
    //**********************************************************
    {
        return p.toAbsolutePath().toString();
    }

    //**********************************************************
    Double get_aspect_ratio(Path p)
    //**********************************************************
    {
        //logger.log(Stack_trace_getter.get_stack_trace("get_aspect_ratio"));
        Aspect_ratio d = aspect_ratio_cache.get(key_from_path(p));
        if ( d == null)
        {
            if(dbg) logger.log("not in RAM for: "+p.toAbsolutePath());
            aspect_ratio_cache.put(key_from_path(p), new Aspect_ratio(1.0,false));
            in_flight.incrementAndGet();
            Actor_engine.run(aspect_ratio_actor, new Aspect_ratio_message(p,aspect_ratio_cache,aborter,logger),null,logger);
            return 1.0;
        }
        if ( ! d.truth )
        {
            if(dbg) logger.log("RAM is fake for: "+p.toAbsolutePath());
            in_flight.incrementAndGet();
            Actor_engine.run(aspect_ratio_actor, new Aspect_ratio_message(p,aspect_ratio_cache,aborter,logger),null,logger);
            return 1.0;
        }
        if(dbg) logger.log(d+" in RAM for: "+p.toAbsolutePath());
        return d.value;
    }



    //**********************************************************
    public void clear_aspect_ratio_RAM_cache()
    //**********************************************************
    {
        aspect_ratio_cache.clear();
        if (dbg) logger.log("aspect ratio cache file cleared");

    }

    //**********************************************************
    void reload_aspect_ratio_cache()
    //**********************************************************
    {
        Properties_manager pm = new Properties_manager(path_of_aspect_ratio_cache_file,logger);
        int reloaded = 0;
        for(String s : pm.get_all_keys())
        {
            String v = pm.get(s);
            logger.log("from Properties ->"+v+"<-");
            if (aspect_ratio_cache.get(s) == null)
            {
                aspect_ratio_cache.put(s, new Aspect_ratio(Double.valueOf(v),true));
                reloaded++;
            }
        }
        if (dbg) logger.log("aspect ratio cache, "+reloaded+" items reloaded from file");

    }
    //**********************************************************
    void save_aspect_ratio_cache()
    //**********************************************************
    {
        Path dir = Files_and_Paths.get_aspect_ratio_cache_dir(logger);
        Properties_manager pm = new Properties_manager(path_of_aspect_ratio_cache_file,logger);

        int saved = 0;
        for(Map.Entry<String, Aspect_ratio> e : aspect_ratio_cache.entrySet())
        {
            Aspect_ratio ar = e.getValue();
            if (ar.truth)
            {
                saved++;
                pm.imperative_store(e.getKey(), Double.toString(ar.value), false, false);
            }
        }
        pm.store_properties();
        if (dbg) logger.log(saved +"items of aspect ratio cache saved to file");
    }

    private AtomicBoolean done = new AtomicBoolean(false);
    private Runnable look_for_end_runnable = null;
    //**********************************************************
    public void look_for_end(Paths_manager paths_manager, Refresh_target refresh_target, Aborter aborter)
    //**********************************************************
    {
        if ( done.get()) return;
        if (look_for_end_runnable != null)
        {
            //already running;
            return;
        }

        look_for_end_runnable = new Runnable() {
            @Override
            public void run() {

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                for(;;)
                {
                    if ( aborter.should_abort()) return;


                    if ( in_flight.get() == 0)
                    {
                        done.set(true);

                        if (Static_application_properties.get_sort_files_by(logger)== File_sort_by.RANDOM_ASPECT_RATIO)
                        {
                            paths_manager.file_comparator = new Aspect_ratio_comparator_random();
                        }
                        /*else if (Static_application_properties.get_sort_files_by(logger)== File_sort_by.ASPECT_RATIO_RANDOM)
                        {
                            paths_manager.file_comparator = new Random_comparator_aspect_ratio();
                        }*/
                        else
                        {
                            paths_manager.file_comparator = new Aspect_ratio_comparator();
                        }
                        logger.log("aspect ratios loaded, going to refresh");
                        refresh_target.refresh();
                        return;
                    }
                    else
                    {
                        logger.log("aspect ratios remaining: "+aspect_ratio_actor.in_flight.get());
                        Runnable r = new Runnable() {
                            @Override
                            public void run() {
                                Popups.popup_warning(null,"please wait!","Filling aspect ratio cache,"+aspect_ratio_actor.in_flight.get()+" files remaining",true,logger);
                            }
                        };
                        Platform.runLater(r);
                    }
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                }
            }
        };
        Threads.execute(look_for_end_runnable,logger);
    }
}
