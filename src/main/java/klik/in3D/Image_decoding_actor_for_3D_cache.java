// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.in3D;

import javafx.scene.image.Image;
import javafx.scene.paint.PhongMaterial;
import javafx.stage.Window;
import klik.look.Jar_utils;
import klik.util.execute.actor.Aborter;
import klik.util.execute.actor.Actor;
import klik.util.execute.actor.Message;
import klik.util.image.Full_image_from_disk;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

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
        Optional<Image> option = get_Image(request.path,request.owner,request.aborter, logger);
        if (option.isPresent())
        {
            Image image = option.get();
            // OutOfMemory can manisfest either as an exception, and then we get a null
            // or the image is of size zero (!)
            if ( (image.getWidth() > 1) && (image.getHeight() > 1))
            {
                request.cache.put(request.path, new PhongMaterial(){{setDiffuseMap(image);}});
                if (dbg) logger.log("✅  image decoded ok is now in cache: " + request.path.getFileName() );
                return "OK, image decoded";
            }
        }
        logger.log( Stack_trace_getter.get_stack_trace("❌ BAD WARNING get_Image_and_index failed"));
        return "❌  image load failed";
    }

    //**********************************************************
    public static Optional<Image> get_Image(Path path, Window owner, Aborter aborter, Logger logger_)
    //**********************************************************
    {
        if ( !Files.exists(path)) return Optional.empty();
        Optional<Image> op = Full_image_from_disk.load_native_resolution_image_from_disk(path, true, owner, aborter,logger_);
        if (op.isEmpty()) return Optional.empty();
        Image local_image = op.get();
        if ( local_image.isError())
        {
            Image broken = Jar_utils.get_broken_icon(300,owner,logger_);
            return Optional.of(broken);
        }

        Optional<Image> returned = Optional.of(local_image);
        return returned;
    }
}
