package klik.browser.icons;

import javafx.stage.Stage;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.files_and_paths.Ding;
import klik.files_and_paths.Files_and_Paths;
import klik.files_and_paths.Guess_file_type;
import klik.properties.File_sort_by;
import klik.properties.Properties_manager;
import klik.properties.Static_application_properties;
import klik.util.Logger;
import org.apache.commons.io.FilenameUtils;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

//**********************************************************
public class Aspect_ratio_cache
//**********************************************************
{
    private static final boolean dbg = true;
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
        String s = "aspect_ratio_cache.properties";//folder_path.toAbsolutePath().toString();
        cache_file_name = UUID.nameUUIDFromBytes(s.getBytes()).toString();
        {
            Path dir = Files_and_Paths.get_aspect_ratio_and_rotation_caches_dir(logger);
            path_of_aspect_ratio_cache_file= Path.of(dir.toAbsolutePath().toString(), cache_file_name);
        }
        logger.log("Aspect_ratio_cache CONSTRUCTOR:  using "+s+" => "+path_of_aspect_ratio_cache_file);
        aborter = aborter_;
        aspect_ratio_actor = new Aspect_ratio_actor(in_flight);
    }


    //**********************************************************
    public void inject(Path path, double aspect_ratio)
    //**********************************************************
    {
        if(dbg) logger.log("Aspect_ratio_cache inject "+path+" aspect_ratio="+aspect_ratio );
        aspect_ratio_cache.put(key_from_path(path),new Aspect_ratio(aspect_ratio,true));
        save_aspect_ratio_cache();
    }


    //**********************************************************
    static String key_from_path(Path p)
    //**********************************************************
    {
        return p.toAbsolutePath().toString();
    }

    //**********************************************************
    Aspect_ratio get_aspect_ratio(Path path)
    //**********************************************************
    {
        //logger.log(Stack_trace_getter.get_stack_trace("get_aspect_ratio"));
        Aspect_ratio aspect_ratio = aspect_ratio_cache.get(key_from_path(path));
        if ( aspect_ratio == null)
        {
            if(dbg) logger.log("not in RAM for: "+path.toAbsolutePath());
            Aspect_ratio tmp;
            if (Guess_file_type.is_this_extension_a_pdf(FilenameUtils.getExtension(path.getFileName().toString())))
            {
                if(dbg) logger.log("not in RAM for: "+path.toAbsolutePath()+" setting ISO "+Aspect_ratio_message.ISO_A4_aspect_ratio);
                tmp = new Aspect_ratio(Aspect_ratio_message.ISO_A4_aspect_ratio,false);
            }
            else
            {
                tmp = new Aspect_ratio(1.0,false);
            }
            aspect_ratio_cache.put(key_from_path(path), tmp);
            in_flight.incrementAndGet();
            Actor_engine.run(aspect_ratio_actor, new Aspect_ratio_message(path,aspect_ratio_cache,aborter,logger),null,logger);
            return tmp;
        }
        if ( aspect_ratio.truth() )
        {
            if(dbg) logger.log("aspect ration RAM is true for: "+path.toAbsolutePath());
        }
        else
        {
            if(dbg) logger.log("aspect ration RAM is fake for: "+path.toAbsolutePath());
            in_flight.incrementAndGet();
            Actor_engine.run(aspect_ratio_actor, new Aspect_ratio_message(path,aspect_ratio_cache,aborter,logger),null,logger);
        }


        return aspect_ratio;
    }

    //**********************************************************
    public void clear_aspect_ratio_RAM_cache()
    //**********************************************************
    {
        aspect_ratio_cache.clear();
        if (dbg) logger.log("aspect ratio cache file cleared");
    }

    //**********************************************************
    synchronized void reload_aspect_ratio_cache()
    //**********************************************************
    {
        Properties_manager pm = new Properties_manager(path_of_aspect_ratio_cache_file,logger);
        int reloaded = 0;
        int already_in_RAM = 0;
        List<String> cleanup = new ArrayList<>();

        for(String key : pm.get_all_keys())
        {
            if ( dbg) logger.log("reloading : "+key);

            String value = pm.get(key);
            if (aspect_ratio_cache.get(key) == null)
            {
                try
                {
                    double d = Double.valueOf(value);
                    if ( dbg) logger.log("reloading : "+key+" => "+ d);
                    aspect_ratio_cache.put(key, new Aspect_ratio(d,true));
                    reloaded++;
                    if ( dbg) logger.log("reloading : "+reloaded);

                }
                catch(NumberFormatException x)
                {
                    // this entry in the file cache is wrong
                    cleanup.add(key);
                }
            }
            else
            {
                already_in_RAM++;
                if ( dbg) logger.log("already in RAM : "+key+" => "+ aspect_ratio_cache.get(key));
            }
        }
        for ( String key:cleanup)
        {
            pm.remove(key);
        }
        if ( !cleanup.isEmpty()) pm.store_properties();
        if (dbg) logger.log("aspect ratio cache: "+already_in_RAM+" already in RAM, "+reloaded+" items reloaded from file");

        if ( dbg)
        {
            logger.log("\n\n\n*********************ASPECT RATIO CACHE************************");
            for (String s  : aspect_ratio_cache.keySet())
            {
                logger.log("aspect ratio cache: "+s+" "+aspect_ratio_cache.get(s));
            }
            logger.log("****************************************************************\n\n\n");
        }
    }
    //**********************************************************
    void save_aspect_ratio_cache()
    //**********************************************************
    {
        Properties_manager pm = new Properties_manager(path_of_aspect_ratio_cache_file,logger);

        int saved = 0;
        for(Map.Entry<String, Aspect_ratio> e : aspect_ratio_cache.entrySet())
        {
            Aspect_ratio ar = e.getValue();
            if (ar.truth())
            {
                saved++;
                pm.imperative_store(e.getKey(), Double.toString(ar.value()), false, false);
            }
        }
        pm.store_properties();
        if (dbg) logger.log(saved +" TRUE items of aspect ratio cache saved to file");
    }

    private AtomicBoolean done = new AtomicBoolean(false);
    private Runnable look_for_end_runnable = null;
    //**********************************************************
    public void look_for_end(Paths_manager paths_manager, Refresh_target refresh_target, Stage stage, Aborter aborter)
    //**********************************************************
    {
        if ( done.get()) return;
        if (look_for_end_runnable != null)
        {
            //already running;
            return;
        }
        long start = System.currentTimeMillis();
        look_for_end_runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                for(;;)
                {
                    if ( aborter.should_abort()) return;
                    if ( in_flight.get() == 0)
                    {
                        if ( System.currentTimeMillis()-start > 5_000)
                        {
                            if (Static_application_properties.get_ding(logger))
                            {
                                Ding.play(logger);
                            }
                        }
                        done.set(true);
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
                        return;
                    }


                    if ( dbg) logger.log("aspect ratios remaining: "+aspect_ratio_actor.in_flight.get());
                        /*Runnable r = new Runnable() {
                            @Override
                            public void run() {
                                Popups.popup_warning(stage,"please wait!","Filling aspect ratio cache,"+aspect_ratio_actor.in_flight.get()+" files remaining",true,logger);
                            }
                        };
                        Platform.runLater(r);
                         */

                    try {
                        Thread.sleep(in_flight.get()/3);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };
        Actor_engine.execute(look_for_end_runnable,logger);
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
            Aspect_ratio d1 = get_aspect_ratio(p1);
            Double d1r= round_to_one_decimal(d1.value());
            Aspect_ratio d2 = get_aspect_ratio(p2);
            Double d2r= round_to_one_decimal(d2.value());
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
            Aspect_ratio d1 = get_aspect_ratio(p1);
            Double d1r= round_to_one_decimal(d1.value());
            Aspect_ratio d2 = get_aspect_ratio(p2);
            Double d2r= round_to_one_decimal(d2.value());
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
