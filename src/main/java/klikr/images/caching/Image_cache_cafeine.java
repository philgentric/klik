// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ./Image_decoding_actor_for_cache.java
//SOURCES ./Image_decode_request_for_cache.java
package klikr.images.caching;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import javafx.stage.Window;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Actor_engine;
import klikr.images.Image_context;
import klikr.images.Image_display_handler;
import klikr.util.Check_remaining_RAM;
import klikr.util.log.Logger;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

//**********************************************************
public class Image_cache_cafeine implements Image_cache_interface
//**********************************************************
{
    private static final boolean ultra_dbg = false;
    private final Image_decoding_actor_for_cache image_decoding_actor;
    Logger logger;
    Cache<String, Image_context> cache;
    private final int forward_size;
    private final Aborter aborter;

    //**********************************************************
    public Image_cache_cafeine(int forward_size_, Aborter aborter, Logger logger_)
    //**********************************************************
    {
        this.aborter = aborter;
        forward_size = forward_size_;
        cache = Caffeine.newBuilder()
                .maximumSize(2*forward_size+1)
                /*.removalListener((key,value,cause)->{
                    if ( cause.wasEvicted())
                    {
                        logger.log("Cafeine evisted: "+key+" cause:"+ cause);
                    }
                })*/
                .build();
        logger = logger_;
        if(ultra_dbg) logger.log("Cafeine max size = "+(2*forward_size+1));
        image_decoding_actor = new Image_decoding_actor_for_cache(logger);// need a single instance
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
    public void put(String key, Image_context value)
    //**********************************************************
    {
        if (ultra_dbg) logger.log("writing in Caffeine:" + value.path.getFileName());
        cache.put(key, value);
    }


    //**********************************************************
    @Override
    public void preload(Image_display_handler image_display_handler, boolean ultimate, boolean forward)
    //**********************************************************
    {
        if (ultra_dbg) logger.log("preloading request! " + forward_size);

        if (Check_remaining_RAM.RAM_running_low(logger))
        {
            //if (ultra_dbg)
                logger.log("Clearing image cache as RAM is low");
            clear_RAM_cache();
            return;
        }

        if (image_display_handler.image_indexer == null)
        {
            // may happen when opening a folder in aspect ratio (slow) mode
            return;
        }
        //if (ultra_dbg) logger.log("preloading target: " + how_many_preload_to_request);
        final List<Path> kk = image_display_handler.image_indexer.get_paths(image_display_handler.get_image_context().get().path, forward_size, forward, ultimate);

        for (Path path: kk)
        {
            Image_decode_request_for_cache idr = new Image_decode_request_for_cache(
                    path,
                    this,
                    image_display_handler.image_window,
                    aborter);
            if (ultra_dbg)
                logger.log("preloading request: " + idr.get_string());
            Actor_engine.run(image_decoding_actor,idr,null,logger);
        }
        //check_decoded_image_cache_size(image_display_handler, logger);


    }

    //**********************************************************
    public void check_decoded_image_cache_size(Image_display_handler image_context_owner, Logger logger)
    //**********************************************************
    {
        if ( image_context_owner.image_indexer ==null) return;
        if (ultra_dbg)
            logger.log("------------ cache content: ---------------");

        for (Map.Entry e : cache.asMap().entrySet())
        {
            String key = (String) e.getKey();
            Image_context local_image_context = (Image_context) e.getValue();

            if ( image_context_owner.image_indexer.distance_larger_than(forward_size,image_context_owner.get_image_context().get().path,local_image_context.path))
            {
                cache.invalidate(key);
                if (ultra_dbg) logger.log("       Evicted:" + key + ", distance too large from:" + image_context_owner.get_image_context().get().path + " to " + local_image_context.path.toAbsolutePath());
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
    public void evict(Path path, Window owner)
    //**********************************************************
    {
        String key = Image_decode_request_for_cache.get_key(path);
        cache.invalidate(key);
        if (ultra_dbg) logger.log("       Evicted:" + key );
    }

    @Override
    //**********************************************************
    public void clear_RAM_cache()
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
        int count = 0;
        for ( Map.Entry<String ,Image_context> e : m.entrySet())
        {
            Image_context ic = e.getValue();
            logger.log("   cache entry: "+ic.path);
            total_pixel += ic.image.getHeight()*ic.image.getWidth();
            count++;

        }
        logger.log(count+ " images in cache");
        logger.log("Total cache size: "+total_pixel/1_000_000+" Mpixels");
        logger.log("cache hitRate: "+s.hitRate());
        logger.log("cache loadCount: "+s.loadCount());
        logger.log("cache totalLoadTime: "+s.totalLoadTime());

    }

    public long compute_cache_size_in_pixels()
    {
        ConcurrentMap<String, Image_context> m = cache.asMap();
        long total_pixel = 0;
        for ( Map.Entry<String ,Image_context> e : m.entrySet())
        {
            Image_context ic = e.getValue();
            logger.log("   cache entry: "+ic.path);
            total_pixel += ic.image.getHeight()*ic.image.getWidth();
        }
        return total_pixel;
    }

/*
    //**********************************************************
    private int distance(int possible_deletion_target, int current, Indexer image_file_source)
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
