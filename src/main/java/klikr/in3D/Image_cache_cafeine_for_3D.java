// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ./Image_decoding_actor_for_cache.java
//SOURCES ./Image_decode_request_for_cache.java
package klikr.in3D;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import javafx.scene.paint.PhongMaterial;
import javafx.stage.Window;
import klikr.util.Check_remaining_RAM;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.log.Logger;

import java.nio.file.Path;
import java.util.List;

//**********************************************************
public class Image_cache_cafeine_for_3D
//**********************************************************
{
    private static final boolean ultra_dbg = false;
    private final Image_decoding_actor_for_3D_cache image_decoding_actor;
    Logger logger;
    Cache<Path, PhongMaterial> cache;

    private final int forward_size;
    private final Aborter aborter;

    //**********************************************************
    public Image_cache_cafeine_for_3D(int forward_size_, Aborter aborter, Logger logger_)
    //**********************************************************
    {
        this.aborter = aborter;
        forward_size = forward_size_;
        cache = Caffeine.newBuilder()
                .maximumSize(2*forward_size+1)
                .removalListener((key,value,cause)->{
                    if ( cause.wasEvicted())
                    {
                        logger.log("Cafeine evicted: "+key+" cause:"+ cause);
                    }
                })
                .build();
        logger = logger_;
        if(ultra_dbg) logger.log("Cafeine max length = "+(2*forward_size+1));
        image_decoding_actor = new Image_decoding_actor_for_3D_cache(logger);// need a single instance
    }


    //**********************************************************
    public PhongMaterial get(Path path)
    //**********************************************************
    {
        return cache.getIfPresent(path);
    }

    //**********************************************************
    
    public void put(Path path, PhongMaterial value)
    //**********************************************************
    {
        if (ultra_dbg)
            logger.log("writing in Caffeine:" + path.getFileName());
        cache.put(path, value);
    }


    //**********************************************************
    public void preload(List<Path> paths, Window owner)
    //**********************************************************
    {
        if (ultra_dbg) logger.log("preloading request! " + forward_size);

        if (Check_remaining_RAM.RAM_running_low("3D full-length image preload", owner,logger))
        {
            //if (ultra_dbg)
                logger.log("clearing image cache as RAM is low");
            clear_all();
            return;
        }

        for (Path path: paths)
        {
            Image_decode_request_for_3D_cache idr = new Image_decode_request_for_3D_cache(
                    path,
                    this,
                    aborter,
                    owner);
            if (ultra_dbg)
                logger.log("preloading request: " + idr.get_string());
            Actor_engine.run(image_decoding_actor,idr,null,logger);
        }
    }


    //**********************************************************
    public void clear_all()
    //**********************************************************
    {
        cache.invalidateAll();
        cache.cleanUp();
    }

}
