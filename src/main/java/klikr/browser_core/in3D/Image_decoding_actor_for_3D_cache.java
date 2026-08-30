// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.browser_core.in3D;

import javafx.scene.image.Image;
import javafx.scene.paint.PhongMaterial;
import javafx.stage.Window;
import klikr.browser_core.Image_and_properties;
import klikr.look.Jar_utils;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Actor;
import klikr.util.execute.actor.Message;
import klikr.util.image.Full_image_from_disk;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;

import java.nio.file.Files;
import java.nio.file.Path;

//**********************************************************
public class Image_decoding_actor_for_3D_cache implements Actor
//**********************************************************
{
    private static final boolean dbg = false;
    Logger logger;


    //**********************************************************
    public Image_decoding_actor_for_3D_cache(Logger logger_)
    //**********************************************************
    {
        logger = logger_;
    }


    //**********************************************************
    @Override
    public String name()
    //**********************************************************
    {
        return "Image_decoding_actor_for_cache";
    }

    //**********************************************************
    @Override
    public String run(Message m)
    //**********************************************************
    {
        Image_decode_request_for_3D_cache request = (Image_decode_request_for_3D_cache) m;
        if (dbg) logger.log("decode request:"+request.get_string());

        if ( request.cache.get(request.path) != null)
        {
            if (dbg)
                logger.log("NOT decoding because image found in cache:"+request.path);
            return"found in cache";
        }

        if ( m.get_aborter().should_abort()) return "aborted";

        // this is the expensive operation:
        Image local_image = get_Image(request.path,request.owner,request.aborter, logger);
        if (local_image != null)
        {
            // OutOfMemory can manisfest either as an exception, and then we get a null
            // or the image is of length zero (!)
            if ( (local_image.getWidth() > 1) && (local_image.getHeight() > 1))
            {
                request.cache.put(request.path, new PhongMaterial(){{setDiffuseMap(local_image);}});
                if (dbg) logger.log(Logger.ok+"  image decoded ok is now in cache: " + request.path.getFileName() );
                return "OK, image decoded";
            }
        }
        logger.log( Stack_trace_getter.get_stack_trace(Logger.error+"BAD WARNING get_Image_and_index failed"));
        return Logger.error+" image load failed";
    }

    //**********************************************************
    public static Image get_Image(Path path, Window owner, Aborter aborter, Logger logger_)
    //**********************************************************
    {
        if ( !Files.exists(path)) return null;
        Image_and_properties iap = Full_image_from_disk.load_native_resolution_image_from_disk(path, true, owner, aborter,logger_);
        if (iap == null) return null;
        if (iap.image() == null) return null;
        if ( iap.image().isError())
        {
            return Jar_utils.get_broken_icon(300,owner,logger_);
        }
        return iap.image();
    }
}
