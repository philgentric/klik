package klik.browser.icons;

import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.files_and_paths.Files_and_Paths;
import klik.properties.Properties_manager;
import klik.util.Logger;
import klik.util.Threads;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

//**********************************************************
public class Aspect_ratio_cache
//**********************************************************
{
    private static final boolean dbg = true;

    public record Aspect_ratio(double value, boolean truth){}
    Map<String, Aspect_ratio> aspect_ratio_cache = new ConcurrentHashMap<>();
    private Aspect_ratio_actor aspect_ratio_actor = new Aspect_ratio_actor();

    public final Logger logger;
    public final Aborter aborter;
    private final String cache_file_name;
    private final Path path_of_aspect_ratio_cache_file;
    //**********************************************************
    public Aspect_ratio_cache(Path folder_path, Logger logger_)
    //**********************************************************
    {
        logger = logger_;
        String s = folder_path.toAbsolutePath().toString();
        cache_file_name = UUID.nameUUIDFromBytes(s.getBytes()).toString();
        {
            Path dir = Files_and_Paths.get_icon_cache_dir(logger);
            path_of_aspect_ratio_cache_file= Path.of(dir.toAbsolutePath().toString(), cache_file_name);
        }
        aborter = new Aborter();
    }

    //**********************************************************
    class Aspect_ratio_comparator implements Comparator<Path>
            //**********************************************************
    {

        @Override
        public int compare(Path p1, Path p2) {
            Double d1 = get_aspect_ratio(p1);
            Double d2 = get_aspect_ratio(p2);
            return d1.compareTo(d2);        }
    };


    //**********************************************************
    static String key_from_path(Path p)
    //**********************************************************
    {
        return p.getFileName().toString();
    }

    //**********************************************************
    Double get_aspect_ratio(Path p)
    //**********************************************************
    {
        Aspect_ratio d = aspect_ratio_cache.get(key_from_path(p));
        if ( d == null)
        {
            if(dbg) logger.log("not in RAM for: "+p.toAbsolutePath());
            aspect_ratio_cache.put(key_from_path(p), new Aspect_ratio(1.0,false));
            aspect_ratio_actor.in_flight.incrementAndGet();
            Actor_engine.run(aspect_ratio_actor, new Aspect_ratio_message(p,aspect_ratio_cache,aborter,logger),null,logger);
            return 1.0;
        }
        if ( ! d.truth )
        {
            if(dbg) logger.log("RAM is fake for: "+p.toAbsolutePath());
            aspect_ratio_actor.in_flight.incrementAndGet();
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
        Path dir = Files_and_Paths.get_icon_cache_dir(logger);
        Properties_manager pm = new Properties_manager(path_of_aspect_ratio_cache_file,logger);

        int saved = 0;
        for(Map.Entry e : aspect_ratio_cache.entrySet())
        {
            Aspect_ratio ar = (Aspect_ratio) e.getValue();
            if (ar.truth)
            {
                saved++;
                pm.imperative_store((String) e.getKey(), Double.toString(ar.value), false, false);
            }
        }
        pm.store_properties();
        if (dbg) logger.log(saved +"items of aspect ratio cache saved to file");
    }

    private volatile boolean done = false;
    private Runnable look_for_end_runnable = null;
    //**********************************************************
    public void look_for_end(Paths_manager paths_manager,Refresh_target refresh_target)
    //**********************************************************
    {
        if ( done) return;
        if ( look_for_end_runnable != null) return;
        look_for_end_runnable = new Runnable() {
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
                        done = true;
                        paths_manager.file_comparator = new Aspect_ratio_comparator();
                        logger.log("aspect ratios loaded, going to refresh");
                        refresh_target.refresh();
                        return;
                    }
                }
            }
        };
        Threads.execute(look_for_end_runnable,logger);
    }
}
