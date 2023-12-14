package klik.browser.icons;

import klik.actor.Actor_engine;
import klik.files_and_paths.Files_and_Paths;
import klik.properties.Properties_manager;
import klik.util.Logger;
import klik.util.Threads;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Aspect_ratio_cache {
    public static final String ASPECT_RATIO_CACHE_FILE = "aspect_ratio_cache_file";
    private static final boolean dbg = true;

    public record Aspect_ratio(double value, boolean truth){}
    Map<String, Aspect_ratio> aspect_ratio_cache = new ConcurrentHashMap<>();

    Aspect_ratio_actor aspect_ratio_actor = new Aspect_ratio_actor();
    public final Logger logger;

    public Aspect_ratio_cache(Logger logger_)
    {
        logger = logger_;
    }

    class Aspect_ratio_comparator implements Comparator<Path>
    {

        @Override
        public int compare(Path p1, Path p2) {
            Double d1 = get_aspect_ratio(p1);
            Double d2 = get_aspect_ratio(p2);
            return d1.compareTo(d2);        }
    };
    Aspect_ratio_comparator aspect_ratio_comparator = new Aspect_ratio_comparator();



    //**********************************************************
    Double get_aspect_ratio(Path p)
    //**********************************************************
    {
        Aspect_ratio d = aspect_ratio_cache.get(p.toAbsolutePath().toString());
        if ( d == null)
        {
            logger.log("not in RAM for: "+p.toAbsolutePath());
            aspect_ratio_cache.put(p.toAbsolutePath().toString(), new Aspect_ratio(1.0,false));
            aspect_ratio_actor.in_flight.incrementAndGet();
            Actor_engine.run(aspect_ratio_actor, new Aspect_ratio_message(p,aspect_ratio_cache,logger),null,logger);
            return 1.0;
        }
        if ( ! d.truth ) {
            logger.log("RAM is fake for: "+p.toAbsolutePath());

            aspect_ratio_actor.in_flight.incrementAndGet();
            Actor_engine.run(aspect_ratio_actor, new Aspect_ratio_message(p,aspect_ratio_cache,logger),null,logger);
            return 1.0;
        }
        logger.log(d+" in RAM for: "+p.toAbsolutePath());
        return d.value;
    }

    //**********************************************************
    public static Path get_path_of_aspect_ratio_cache_file(Logger logger)
    //**********************************************************
    {
        Path dir = Files_and_Paths.get_icon_cache_dir(logger);
        return Path.of(dir.toAbsolutePath().toString(), ASPECT_RATIO_CACHE_FILE);
    }

    //**********************************************************
    public void erase_aspect_ratio_cache_file()
    //**********************************************************
    {
        Properties_manager pm = new Properties_manager(get_path_of_aspect_ratio_cache_file(logger),logger);
        pm.erase_all_and_save();
        aspect_ratio_cache.clear();
        if (dbg) logger.log("aspect ratio cache file cleared");

    }

    //**********************************************************
    void reload_aspect_ratio_cache()
    //**********************************************************
    {
        Properties_manager pm = new Properties_manager(get_path_of_aspect_ratio_cache_file(logger),logger);
        for(String s : pm.get_all_keys())
        {
            String v = pm.get(s);
            if (aspect_ratio_cache.get(s) == null) {
                aspect_ratio_cache.put(s, new Aspect_ratio(Double.valueOf(v),true));
            }
        }
        if (dbg) logger.log("aspect ratio cache reloaded from file");
    }
    //**********************************************************
    void save_aspect_ratio_cache()
    //**********************************************************
    {
        Path dir = Files_and_Paths.get_icon_cache_dir(logger);
        Properties_manager pm = new Properties_manager(get_path_of_aspect_ratio_cache_file(logger),logger);

        int saved = 0;
        for(Map.Entry e : aspect_ratio_cache.entrySet())
        {
            Aspect_ratio ar = (Aspect_ratio) e.getValue();
            if (ar.truth) {
                saved++;
                pm.imperative_store((String) e.getKey(), Double.toString(ar.value), false, false);
            }
        }
        pm.store_properties();
        if (dbg) logger.log(saved +"items of aspect ratio cache saved to file");
    }

    public void look_for_end(Paths_manager paths_manager,Refresh_target refresh_target)
    {
        Runnable look_for_end = new Runnable() {
            @Override
            public void run() {

                for(;;)
                {
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                    if ( aspect_ratio_actor.in_flight.get() == 0)
                    {
                        paths_manager.file_comparator = new Aspect_ratio_comparator();
                        logger.log("\n\n\n refresh");
                        refresh_target.refresh();
                        return;
                    }
                    else
                    {
                        logger.log("in_flight:"+ aspect_ratio_actor.in_flight.get());

                    }

                }

            }
        };
        Threads.execute(look_for_end,logger);
    }
}
