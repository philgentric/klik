// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.images.caching;

import javafx.stage.Window;
import klikr.util.cache.Size_;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Actor_engine;
import klikr.images.Image_context;
import klikr.images.Image_display_handler;
import klikr.util.log.Logger;

import java.nio.file.Path;
import java.util.*;

//**********************************************************
public class Image_cache_linkedhashmap implements Image_cache_interface
//**********************************************************
{
    private final Image_decoding_actor_for_cache image_decoding_actor;
    Logger logger;
    private final int forward_size;
    private final Aborter aborter;
    private final LinkedHashMap<String, Image_context> cache;

    //**********************************************************
    public Image_cache_linkedhashmap(int forward_size, Aborter aborter, Logger logger)
    //**********************************************************
    {
        this.logger = logger;
        this.forward_size = forward_size;
        this.aborter = aborter;
        image_decoding_actor = new Image_decoding_actor_for_cache(logger);// need a single instance


        cache = new LinkedHashMap<>(2*forward_size+1, 0.75f, true)
        {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Image_context> eldest) {
                if (size() > 2 * forward_size + 1)
                {
                    logger.log("Image_cache_linkedhashmap removing eldest entry: " + eldest.getKey());
                    return true;
                }
                return false;
            }
        };
    }


    //**********************************************************
    @Override
    public Image_context get(String key_)
    //**********************************************************
    {
        return cache.get(key_);
    }

    //**********************************************************
    @Override
    public void put(String key_, Image_context value)
    //**********************************************************
    {
        cache.put(key_, value);
    }


    //**********************************************************
    @Override
    public void preload(Image_display_handler image_display_handler, boolean ultimate, boolean forward)
    //**********************************************************
    {

        if (image_display_handler.image_indexer == null)
        {
            // may happen when opening a folder in aspect ratio (slow) mode
            return;
        }
        final List<Path> kk = image_display_handler.image_indexer.get_paths(image_display_handler.get_image_context().get().path, forward_size, forward, ultimate);

        for (Path path: kk)
        {
            Image_decode_request_for_cache idr = new Image_decode_request_for_cache(path, this, image_display_handler.image_window,aborter);
            Actor_engine.run(image_decoding_actor,idr,null,logger);
        }

    }

    //**********************************************************
    public void check_decoded_image_cache_size(Image_display_handler image_context_owner, Logger logger)
    //**********************************************************
    {
    }

    //**********************************************************
    @Override // Image_cache_interface
    public void evict(Path path, Window owner)
    //**********************************************************
    {

        String key = Image_decode_request_for_cache.get_key(path);
        cache.remove(key);
    }

    @Override
    //**********************************************************
    public double clear_RAM()
    //**********************************************************
    {
        double returned = Size_.of_Map(cache,Size_.of_String_F(),Image_context.size_F());
        cache.clear();
        return returned;
    }

    //**********************************************************
    @Override
    public void print()
    //**********************************************************
    {
    }

}
