package klik.images;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import klik.actor.Actor_engine;
import klik.images.decoding.Image_decode_request;
import klik.images.decoding.Image_decoding_actor;
import klik.util.Logger;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

//**********************************************************
public class Image_cache_cafeine implements Cache_interface
//**********************************************************
{
    private static final boolean ultra_dbg = false;
    private final Image_decoding_actor image_decoding_actor;
    Logger logger;
    Cache<String, Image_context> cache;

    //**********************************************************
    public Image_cache_cafeine(Logger logger_)
    //**********************************************************
    {
        cache = Caffeine.newBuilder()
                .maximumSize(Image_decoding_actor.CACHE_SIZE)
                .build();
        logger = logger_;
        image_decoding_actor = new Image_decoding_actor(logger);// need a single instance
    }


    //**********************************************************
    @Override
    public Image_context get(String key)
    //**********************************************************
    {
        return cache.getIfPresent(key);
    }

    //**********************************************************
    @Override
    public Object put(String key, Image_context value)
    //**********************************************************
    {
        if (ultra_dbg) logger.log("writing in Caffeine:" + value.path.getFileName());
        cache.put(key, value);
        return null;
    }


    //**********************************************************
    @Override
    public void preload(Image_display_handler image_display_handler, boolean ultimate, boolean forward, boolean high_quality)//, int target_width)
    //**********************************************************
    {
        //int increment = -1;
        //if (forward) increment = 1;
        int how_many_preload_to_request = Image_decoding_actor.FORWARD_PRELOAD_SIZE;

        if ( image_display_handler.image_indexer == null)
        {
            // may happen when opening a folder in aspect ratio (slow) mode
            return;
        }
        //if (ultra_dbg) logger.log("preloading target: " + how_many_preload_to_request);
        final List<Path> kk = image_display_handler.image_indexer.get_paths(image_display_handler.get_image_context().path, how_many_preload_to_request, forward, ultimate);

        for (Path path: kk)
        {
            Image_decode_request idr = new Image_decode_request(path, high_quality, this);
            if (ultra_dbg) logger.log("preloading request: " + idr.get_string());
            Actor_engine.run(image_decoding_actor,idr,null,logger);
        }
        check_decoded_image_cache_size(image_display_handler, logger);


    }

    //**********************************************************
    public void check_decoded_image_cache_size(Image_display_handler image_context_owner, Logger logger)
    //**********************************************************
    {
        if (ultra_dbg)
            logger.log("------------ cache content: ---------------");

        for (Map.Entry e : cache.asMap().entrySet())
        {
            String key = (String) e.getKey();
            Image_context local_image_context = (Image_context) e.getValue();

            if ( image_context_owner.image_indexer.distance_larger_than(image_context_owner.get_image_context().path,local_image_context.path, Image_decoding_actor.FORWARD_PRELOAD_SIZE))
            {
                cache.invalidate(key);
                if (ultra_dbg) logger.log("       Evicted:" + key + ", distance too large from:" + image_context_owner.get_image_context().path + " to " + local_image_context.path.toAbsolutePath());
            }
            else
            {
                if (ultra_dbg)
                    logger.log("       NOT evicted:" + key );
            }
        }
        if (ultra_dbg) logger.log("---------- end cache content: -------------");

    }

    //**********************************************************
    @Override // Image_cache_interface
    public void evict(Path path)
    //**********************************************************
    {
        Image_decode_request request = new Image_decode_request(path,false,null);
        String key = request.make_key();
        cache.invalidate(key);
        if (ultra_dbg) logger.log("       Evicted:" + key );
    }

    @Override
    //**********************************************************
    public void clear_all()
    //**********************************************************
    {
        cache.invalidateAll();
        cache.cleanUp();
    }

    //**********************************************************
    @Override
    public void print()
    //**********************************************************
    {
        CacheStats s = cache.stats();


        ConcurrentMap<String, Image_context> m = cache.asMap();

        long total_pixel = 0;
        for ( Map.Entry<String ,Image_context> e : m.entrySet())
        {
            Image_context ic = e.getValue();
            logger.log("   cache entry: "+ic.path);
            total_pixel += ic.image.getHeight()*ic.image.getWidth();

        }
        logger.log("cache hitRate: "+s.hitRate());
        logger.log("cache loadCount: "+s.loadCount());
        logger.log("cache totalLoadTime: "+s.totalLoadTime());
        logger.log("cache size: "+total_pixel/1_000_000+" Mpixels");

    }

/*
    //**********************************************************
    private int distance(int possible_deletion_target, int current, Image_indexer image_file_source)
    //**********************************************************
    {
        int distance1 = current - possible_deletion_target;
        if (distance1 < 0) distance1 = -distance1;

        int max = image_file_source.how_many_images();
        int distance2 = max - current + possible_deletion_target;
        if (distance1 < distance2) return distance1;
        else return distance2;

    }
*/
}
