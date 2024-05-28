package klik.browser.icons.caches;

import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.browser.icons.Paths_manager;
import klik.browser.icons.Refresh_target;
import klik.files_and_paths.Ding;
import klik.files_and_paths.Files_and_Paths;
import klik.images.decoding.Fast_image_property_from_exif_metadata_extractor;
import klik.properties.File_sort_by;
import klik.properties.Properties_manager;
import klik.properties.Static_application_properties;
import klik.search.Show_running_man_frame;
import klik.util.Logger;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

//**********************************************************
public class Image_properties_cache
//**********************************************************
{
    public final static boolean dbg = false;
    protected final Logger logger;
    protected final Aborter aborter;
    protected final String cache_name;
    protected final Path cache_file_path;
    private final Map<String, Image_properties> cache = new ConcurrentHashMap<>();
    protected final Properties_manager pm;
    LinkedBlockingQueue<String> end = new LinkedBlockingQueue<>();
    Image_properties_actor image_properties_actor;
    //**********************************************************
    public Image_properties_cache(Path path, String cache_name_, Aborter aborter_, Logger logger_)
    //**********************************************************
    {
        logger = logger_;
        aborter = aborter_;
        cache_name = cache_name_;
        String local = cache_name+path.toAbsolutePath().toString();
        String cache_file_name = UUID.nameUUIDFromBytes(local.getBytes()).toString()+".properties";
        Path dir = Files_and_Paths.get_aspect_ratio_and_rotation_caches_dir(null,logger);
        cache_file_path= Path.of(dir.toAbsolutePath().toString(), cache_file_name);
        if ( dbg)
        {
            logger.log(cache_name+" cache file ="+cache_file_path);
        }
        pm = new Properties_manager(cache_file_path,logger);
        image_properties_actor = new Image_properties_actor(end);
    }


    //**********************************************************
    private void put_in_cache(Path p,Image_properties ip)
    //**********************************************************
    {
        cache.put(key_from_path(p),ip);
    }

    //**********************************************************
    public Image_properties really_get_from_cache(Path path)
    //**********************************************************
    {
        // try the cache
        Image_properties returned = get_from_cache(path);
        if ( returned != null) return returned;
        // ok, so now we must REALLY get the data

        returned = Fast_image_property_from_exif_metadata_extractor.get_image_properties(path,true,aborter,logger);

        return returned;

    }


    //**********************************************************
    public Image_properties get_from_cache(Path p)
    //**********************************************************
    {
        Image_properties image_properties =  cache.get(key_from_path(p));
        if ( image_properties == null)
        {
            Image_properties_message imp = new Image_properties_message(p,this,aborter,logger);
            image_properties_actor.increment_in_flight();
            Actor_engine.run(image_properties_actor,imp,null,logger);
        }
        return image_properties;
    }

    //**********************************************************
    private static String key_from_path(Path p)
    //**********************************************************
    {
        String local = p.getFileName().toString();
        //String local = p.toAbsolutePath().toString();
        return local;//UUID.nameUUIDFromBytes(local.getBytes()).toString();
    }
    //**********************************************************
    public void inject(Path path, Image_properties val, boolean and_save)
    //**********************************************************
    {
        if(dbg) logger.log(cache_name+" inject "+path+" value="+val );
        cache.put(key_from_path(path),val);
        if ( and_save) save_one_item_to_disk(path,val);
    }

    //**********************************************************
    public void clear_RAM_cache_fx()
    //**********************************************************
    {
        cache.clear();
        if (dbg) logger.log("aspect ratio cache file cleared");
    }
    //**********************************************************
    public synchronized void reload_cache_from_disk()
    //**********************************************************
    {
        int reloaded = 0;
        int already_in_RAM = 0;
        List<String> cleanup = new ArrayList<>();

        for(String key : pm.get_all_keys())
        {
            if ( dbg) logger.log("reloading : "+key);

            String value = pm.get(key);
            if (cache.get(key) == null)
            {
                try
                {
                    Image_properties ip = Image_properties.from_string(value);
                    if ( dbg) logger.log("reloading : "+key+" => "+ ip.to_string());
                    cache.put(key, ip);
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
                if ( dbg) logger.log("already in RAM : "+key+" => "+ cache.get(key));
            }
        }
        for ( String key:cleanup)
        {
            pm.remove(key);
        }
        if ( !cleanup.isEmpty()) pm.store_properties();
        //if (dbg)
            logger.log(cache_name+": "+already_in_RAM+" already in RAM, "+reloaded+" items reloaded from file");

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
        for(Map.Entry<String, Image_properties> e : cache.entrySet())
        {
            saved++;
            pm.imperative_store(e.getKey(), e.getValue().to_string(), false, false);
        }
        pm.store_properties();
        if (dbg) logger.log(saved +" TRUE items of aspect ratio cache saved to file");
    }
    //**********************************************************
    public void save_one_item_to_disk(Path path, Image_properties ip)
    //**********************************************************
    {
        pm.imperative_store(key_from_path(path), ip.to_string(), false, true);
    }



    private AtomicBoolean done_look_for_end = new AtomicBoolean(false);
    private Runnable look_for_end_runnable = null;
    //**********************************************************
    public void look_for_end(Paths_manager paths_manager, Refresh_target refresh_target)
    //**********************************************************
    {
        if ( done_look_for_end.get()) return;
        if (look_for_end_runnable != null)
        {
            //already running;
            return;
        }

        CountDownLatch running_man = Show_running_man_frame.show_running_man("Image properties are being computed", 20*60,  aborter, logger);


        long start = System.currentTimeMillis();
        look_for_end_runnable = new Runnable() {
            @Override
            public void run()
            {
                for(;;)
                {
                    String s = null;
                    try {
                        s = end.poll(100, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    if (s == null)
                    {
                        logger.log("look_for_end timeout");

                        if (aborter.should_abort()) {
                            logger.log("Aspect ratio cache look_for_end_runnable aborting");
                            process_end(paths_manager,refresh_target,start,running_man);
                            return;
                        }
                        logger.log("look_for_end, image geometries remaining: " + image_properties_actor.get_in_flight());
                        if (image_properties_actor.get_in_flight() == 0)
                        {
                            process_end(paths_manager,refresh_target,start,running_man);
                            return;
                        }
                    }
                    else
                    {
                        logger.log("look_for_end received END signal");

                        process_end(paths_manager,refresh_target,start, running_man);
                        return;
                    }
                }
            }
        };
        Actor_engine.execute(look_for_end_runnable,logger);
    }

    //**********************************************************
    private void process_end(Paths_manager paths_manager, Refresh_target refresh_target, long start, CountDownLatch running_man)
    //**********************************************************
    {
        logger.log("process_end() ");
        // this is the end
        running_man.countDown();
        done_look_for_end.set(true);
        if (System.currentTimeMillis() - start > 5_000) {
            if (Static_application_properties.get_ding(logger)) {
                Ding.play("Aspect_ratio_cache: done acquiring all aspect ratios", logger);
            }
        }
        Comparator<Path> local_file_comparator = null;
        switch (Static_application_properties.get_sort_files_by(logger))
        {
            case File_sort_by.ASPECT_RATIO -> local_file_comparator = new Aspect_ratio_comparator();
            case File_sort_by.RANDOM_ASPECT_RATIO -> local_file_comparator = new Aspect_ratio_comparator_random();
            case File_sort_by.IMAGE_WIDTH -> local_file_comparator = new Image_width_comparator();
            case File_sort_by.IMAGE_HEIGHT -> local_file_comparator = new Image_height_comparator();
            default -> local_file_comparator = null;
        }
        if (local_file_comparator != null) {
            paths_manager.image_file_comparator = local_file_comparator;
            paths_manager.iconized_sorted = new ConcurrentSkipListMap<>(local_file_comparator);

            // now that we changed the iconized container we must do a scan dir
            logger.log("image geometry cache reloaded, going to refresh");

            refresh_target.refresh_UI_after_scan_dir("look_for_end");
            //refresh_target.refresh_all("aspect ratio cache");
        }
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
    //**********************************************************
    {
        @Override
        public int compare(Path p1, Path p2)
        {
            Image_properties ip1 = get_from_cache(p1);
            if ( ip1 == null) return 0;
            Double d1 = ip1.get_aspect_ratio();
            Image_properties ip2 = get_from_cache(p2);
            if ( ip2 == null) return 0;
            Double d2 = ip2.get_aspect_ratio();
            int diff =  d1.compareTo(d2);
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
            Image_properties ip1 = get_from_cache(p1);
            if ( ip1 == null) return 0;
            Double d1 = ip1.get_aspect_ratio();
            Image_properties ip2 = get_from_cache(p2);
            if ( ip2 == null) return 0;
            Double d2 = ip2.get_aspect_ratio();

            // round the aspect ratio a bit
            Double d1r= round_to_one_decimal(d1);
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

    //**********************************************************
    class Image_width_comparator implements Comparator<Path>
    {
        @Override
        public int compare(Path p1, Path p2) {
            Image_properties ip1 = get_from_cache(p1);
            if ( ip1 == null) return 0;
            Double d1 = ip1.get_image_width();
            if ( d1 == null) return 0;
            Image_properties ip2 = get_from_cache(p2);
            if ( ip2 == null) return 0;
            Double d2 = ip2.get_image_width();
            if ( d2 == null) return 0;

            int diff =  d1.compareTo(d2);
            if ( diff != 0) return diff;
            return (p1.toString().compareTo(p2.toString()));
        }
    };


    //**********************************************************
    class Image_height_comparator implements Comparator<Path>
    {
        @Override
        public int compare(Path p1, Path p2) {
            Image_properties ip1 = get_from_cache(p1);
            if ( ip1 == null) return 0;
            Double d1 = ip1.get_image_height();
            if ( d1 == null) return 0;
            Image_properties ip2 = get_from_cache(p2);
            if ( ip2 == null) return 0;
            Double d2 = ip2.get_image_height();
            if ( d2 == null) return 0;

            int diff =  d1.compareTo(d2);
            if ( diff != 0) return diff;
            return (p1.toString().compareTo(p2.toString()));
        }
    };
}
