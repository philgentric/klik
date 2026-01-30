// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES animated_gifs/Ffmpeg_utils.java
//SOURCES ../Image_and_properties.java
//SOURCES ../items/Iconifiable_item_type.java
//SOURCES ../../util/files_and_paths/Static_files_and_paths_utilities.java
//SOURCES ../../images/decoding/Fast_image_property_from_exif_metadata_extractor.java
//SOURCES ../../util/execute/Execute_command.java
//SOURCES animated_gifs/Animated_gif_from_folder_content.java

package klikr.browser.icons;

import javafx.scene.image.Image;
import javafx.stage.Window;
import klikr.properties.String_constants;
import klikr.util.External_application;
import klikr.look.Jar_utils;
import klikr.util.cache.Klikr_cache;
import klikr.util.execute.Execute_result;
import klikr.util.execute.actor.*;
import klikr.util.animated_gifs.Ffmpeg_utils;
import klikr.browser.Image_and_properties;
import klikr.browser.icons.image_properties_cache.*;
import klikr.browser.items.Iconifiable_item_type;
import klikr.properties.boolean_features.Booleans;
import klikr.util.cache.Cache_folder;
import klikr.properties.Non_booleans_properties;
import klikr.util.files_and_paths.Extensions;
import klikr.util.image.Icons_from_disk;
import klikr.util.execute.Execute_command;
import klikr.util.image.icon_cache.Icon_caching;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;
import klikr.util.mmap.Mmap;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


// an actor-style asynchronous icon factory
//**********************************************************
public class Icon_factory_actor implements Actor
//**********************************************************
{
    private static final boolean image_as_file = true;


    private static final boolean verbose_dbg = true;
    private static final boolean dbg = true;
    private static final boolean pdf_dbg = true;
    private static final boolean aborting_dbg = false;

    Logger logger;
    Icon_writer_actor writer;
    private final Window owner;
    private final Aborter aborter;
    private final Klikr_cache<Path, Image_properties> image_properties_cache;

    private final Path icon_cache_dir;

    //**********************************************************
    public Icon_factory_actor(
            Klikr_cache<Path, Image_properties> image_properties_cache,
            Window owner,
            Aborter aborter, Logger logger_)
    //**********************************************************
    {
        this.image_properties_cache = image_properties_cache;
        this.aborter = aborter;
        this.owner = owner;
        logger = logger_;
        if (dbg) logger.log("✅ Icon_factory created");
        icon_cache_dir = Cache_folder.get_cache_dir( Cache_folder.icon_cache,owner,logger);
        writer = new Icon_writer_actor(icon_cache_dir, owner, logger);
    }


    //**********************************************************
    @Override
    public String name()
    //**********************************************************
    {
        return "Icon_factory_actor";
    }


    //**********************************************************
    @Override
    public String run(Message m)
    //**********************************************************
    {
        Icon_factory_request icon_factory_request = (Icon_factory_request) m;
        if (dbg) logger.log("✅ icon request processing starts ");

        Icon_destination destination = icon_factory_request.destination;
        if (destination == null) {
            logger.log(Stack_trace_getter.get_stack_trace("SHOULD NOT HAPPEN icon factory : cancel! destination==null"));
            return null;
        }

        Optional<Image_and_properties> image_and_properties = Optional.empty();
        for(;;)
        {
            image_and_properties = try_once(destination, icon_factory_request);
            if ( image_and_properties.isEmpty())
            {
                // DONT RETRY
                return "no icon";
            }
            if (image_and_properties.get().image() != null) {
                double w = image_and_properties.get().image().getWidth();
                double h = image_and_properties.get().image().getHeight();
                if ((w > 0.0)&&(h > 0.0)) {
                    // SUCCESS!!
                    break;
                }
            }
            logger.log("something went wrong, image w = "+image_and_properties.get().image().getWidth()+", h = "+image_and_properties.get().image().getHeight());

            // retry a few times
            if (icon_factory_request.retry_count < Icon_factory_request.max_retry)
            {
                icon_factory_request.retry_count++;
                logger.log("❗ RETRYING : " + icon_factory_request.retry_count + " times, after empty icon for : " + destination.get_item_path() );
                try {
                    Thread.sleep(100*icon_factory_request.retry_count);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                continue;
            }
            logger.log("❗ too many retries after empty icon for : " + destination.get_item_path() );
            return "icon failed";
        }

        check(image_and_properties.get());

        destination.receive_icon(image_and_properties.get());
        Image_properties image_properties = image_and_properties.get().properties();
        if ( image_properties != null)
        {
            image_properties_cache.inject(destination.get_item_path(),image_properties,false);
        }
        //logger.log("Icon_factory_actor: "+ instance.decrementAndGet());

        return "icon done";
    }

    //**********************************************************
    private void check(Image_and_properties image_and_properties)
    //**********************************************************
    {
        if ( image_and_properties.image() == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("❗ image_and_properties.image() == null"));
            return;
        }
        if ( image_and_properties.properties() == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("❗ image_and_properties.properties() == null"));
            return;
        }
        if ( image_and_properties.properties().rotation() == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("❗ image_and_properties.properties().rotation() == null"));
        }
    }


    //**********************************************************
    private Optional<Image_and_properties> try_once(Icon_destination destination, Icon_factory_request icon_factory_request )
    //**********************************************************
    {
        return do_it(destination.get_item_type(), icon_factory_request, destination);

        /*
        switch (destination.get_item_type())
        {
            case gif -> {
                //return process_image_icons_cached_as_file( destination.get_item_type(), icon_factory_request, destination);
                return process_image_icons_not_cached(destination.get_item_type(), icon_factory_request, destination);
            }
            default -> {
                return do_it(destination.get_item_type(), icon_factory_request, destination);
            }
        }*/
    }

    //**********************************************************
    private Optional<Image_and_properties> try_once_old(Icon_destination destination, Icon_factory_request icon_factory_request )
    //**********************************************************
    {
        switch (destination.get_item_type())
        {
            case pdf -> {
                if (dbg) logger.log("✅ "+destination.get_item_path() + " type is PDF");
                return process_pdf_icons_cached_as_pixels(icon_factory_request, destination);
            }
            case video -> {
                if (dbg) logger.log("✅ "+destination.get_item_path() + " type is VIDEO");
                return process_video_icons_cached_as_file(icon_factory_request, destination);
            }

            // not is used as detecting animated_gif via EXIF is WAY too expensive
            /*case animated_gif -> {
                if (dbg) logger.log("✅ "+destination.get_item_path() + " type is VIDEO");
                return process_animated_gif(icon_factory_request, destination);
            }*/

            case folder, symbolic_link_on_folder -> {
                // for folder the path of the image chosen to represent the folder as an icon is needed
                return process_image_icons_cached_as_pixel(destination.get_item_type(), icon_factory_request, destination);
            }

            case gif -> {
                //return process_image_icons_cached_as_file( destination.get_item_type(), icon_factory_request, destination);
                return process_image_icons_not_cached(destination.get_item_type(), icon_factory_request, destination);
            }


            case png, javafx_image_not_gif_not_png, non_javafx_image -> {
                return process_image_icons_cached_as_pixel(destination.get_item_type(), icon_factory_request, destination);
            }
            case no_path, other -> {
                logger.log(Stack_trace_getter.get_stack_trace("❗ HAPPENS in Icon_factory_actor"+icon_factory_request.destination));
                return process_image_icons_cached_as_pixel(destination.get_item_type(), icon_factory_request, destination);
            }

        }
        logger.log("❗ WARNING icon is null for "+destination.get_item_path());

        return Optional.empty();
    }


    //**********************************************************
    private Optional<Image_and_properties> process_image_icons_cached_as_pixel(Iconifiable_item_type item_type, Icon_factory_request icon_factory_request, Icon_destination destination )
    //**********************************************************
    {
        if ( dbg)  logger.log("✅ Icon_factory thread: process_image:" + destination.get_string());

        Path icon_path = destination.get_path_for_display_icon_destination();
        if (icon_path == null)
        {
            if (dbg)
                logger.log("❗ Icon_factory thread: returning null icon because icon path is null for item:" + destination.get_string() + "\ntypically happens when there is no image to use as a icon in that folder");
            return Optional.empty();
        }
        if (icon_factory_request.aborter.should_abort())
        {
            if ( aborting_dbg)
                logger.log("❗ Icon_factory thread: aborting0 "+icon_factory_request.aborter.reason());
            return Optional.empty();
        }

        {
            Image image_from_cache = Mmap.instance.read_image_as_pixel(icon_path.toAbsolutePath().toString());

            if (icon_factory_request.aborter.should_abort())
            {
                if (aborting_dbg)
                    logger.log("❗ Icon_factory thread: aborting2 " + icon_factory_request.aborter.name + " reason: " + icon_factory_request.aborter.reason());
                return Optional.empty();
            }
            if (image_from_cache != null) {
                if (dbg)
                    logger.log("✅ Icon_factory thread: found in cache: " + icon_path.getFileName());

                Image_properties properties = new Image_properties(image_from_cache.getWidth(), image_from_cache.getHeight(), Rotation.normal);
                return Optional.of(new Image_and_properties(image_from_cache, properties));
            }
        }
        if (dbg)
            logger.log("\n\n✅ process_image Loading icon from cache FAILED for " + icon_path.toAbsolutePath());

        Optional<Image> op = Icons_from_disk.read_original_image_from_disk_and_return_icon(icon_path, item_type, icon_factory_request.icon_size, true, icon_factory_request.owner, icon_factory_request.aborter, logger);
        if (icon_factory_request.aborter.should_abort())
        {
            if ( aborting_dbg)
                logger.log("❗ Icon_factory thread: aborting3");
            return Optional.empty();
        }
        boolean write_icon_to_cache = true;
        Image image_from_disk = null;
        if (op.isEmpty()) {
            //if (dbg)
            logger.log("❌ Making an icon FAILED (1) for " + icon_path.getFileName());
            //return Optional.empty();

            image_from_disk = Jar_utils.get_broken_icon(icon_factory_request.icon_size,icon_factory_request.owner,logger);
            write_icon_to_cache = false; // do not write the broken icon to the cache
        }
        else
        {
            image_from_disk = op.get();
        }
        if (image_from_disk.getWidth() < 1.0) {
            // this "should not happen" as it was seen when there was a multithreading bug: too many icon requests were arriving at the same time
            logger.log("❌ Making an icon FAILED (2) getWidth() ==0 for " + icon_path.getFileName());
            return Optional.empty();
        }
        if (image_from_disk.getHeight() ==0) {
            // this "should not happen" as it was seen when there was a multithreading bug: too many icon requests were arriving at the same time
            logger.log("❌ Making an icon FAILED (3) getHeight() ==0 for " + icon_path.getFileName());
            return Optional.empty();
        }

        // next we write the icon into cache, with an actor on a thread
        switch (item_type) {
            case gif:
                // dont try to WRITE the icon for gifs, they are either small or animated
            case symbolic_link_on_folder:
            case folder:
                // no need for folders
                break;
            default:
                Path icon_cache_dir = Cache_folder.get_cache_dir( Cache_folder.icon_cache,owner,logger);
                if (icon_path.getParent().toAbsolutePath().toString().equals(icon_cache_dir.toAbsolutePath().toString()))
                {
                    // the user is browsing the icon cache. if we save a file for the icon, it will trigger a new icon request...
                    // ad nauseam ! ==> storm avoidance = dont save the icon.
                    if (dbg) logger.log("✅ Icon_factory thread: (storm avoidance) not saving the icon for a file which is in the icons' cache folder " + icon_path.getFileName());
                    break;
                }

                if (dbg)
                    logger.log("✅ Icon_factory thread: sending icon write to file in cache dir for " + icon_path.getFileName());

                if ( write_icon_to_cache)
                {
                    Icon_write_message iwm = new Icon_write_message(image_from_disk, icon_factory_request.icon_size, icon_path, aborter);
                    writer.push(iwm);
                }
                break;
        }

        Image_properties properties = image_properties_cache.get(destination.get_path_for_display_icon_destination(),aborter,null,owner);
        if (properties == null)
        {
            properties = new Image_properties(image_from_disk.getWidth(), image_from_disk.getHeight(), Rotation.normal);
            image_properties_cache.inject(destination.get_path_for_display_icon_destination(), properties,false);
            if ( dbg) logger.log("properties computed "+properties.to_string()+ "for path :"+destination.get_path_for_display_icon_destination());
        }
        else {
            if ( dbg) logger.log("properties from cache "+properties.to_string()+ "for path :"+destination.get_path_for_display_icon_destination());
        }
        return Optional.of(new Image_and_properties(image_from_disk,properties));
    }

    //**********************************************************
    private Optional<Image_and_properties> process_image_icons_not_cached(Iconifiable_item_type item_type, Icon_factory_request icon_factory_request, Icon_destination destination )
    //**********************************************************
    {
        if ( dbg)  logger.log("✅ Icon_factory thread: process_not_iconized_not_cached:" + destination.get_string());

        Path icon_path = destination.get_path_for_display_icon_destination();
        if (icon_path == null)
        {
            if (dbg)
                logger.log("❗ Icon_factory thread: returning null icon because icon path is null for item:" + destination.get_string() + "\ntypically happens when there is no image to use as a icon in that folder");
            return Optional.empty();
        }
        if (icon_factory_request.aborter.should_abort())
        {
            if ( aborting_dbg) logger.log("❗ Icon_factory thread: aborting0 "+icon_factory_request.aborter.reason());
            return Optional.empty();
        }

        Optional<Image> image = Icons_from_disk.read_original_image_from_disk_and_return_icon(icon_path,item_type,icon_factory_request.icon_size,true,owner,aborter,logger);
        if (icon_factory_request.aborter.should_abort())
        {
            if (aborting_dbg) logger.log("❗ Icon_factory_actor aborting2 " + icon_factory_request.aborter.name + " reason: " + icon_factory_request.aborter.reason());
            return Optional.empty();
        }
        if (image.isEmpty()) return Optional.empty();

        if (dbg) logger.log("✅ Icon_factory, loaded from file: " + icon_path.getFileName());

        Image_properties properties = new Image_properties(image.get().getWidth(), image.get().getHeight(), Rotation.normal);
        return Optional.of(new Image_and_properties(image.get(), properties));
    }



    //**********************************************************
    private Optional<Image> process_image_with_no_cached_icon(Icon_factory_request icon_factory_request, Icon_destination destination)
    //**********************************************************
    {
        if ( dbg) logger.log("✅ Icon_factory_actor process_image_with_no_cached_icon:" + destination.get_string());

        Path path = destination.get_path_for_display_icon_destination();
        if (path == null)
        {
            if (dbg)
                logger.log("❗ Icon_factory_actor returning null icon because icon path is null for item:" + destination.get_string() + "\ntypically happens when there is no image to use as a icon in that folder");

            return Optional.empty();//Look_and_feel_manager.get_large_folder_icon(icon_factory_request.icon_size);
        }
        if (icon_factory_request.aborter.should_abort())
        {
            if ( aborting_dbg) logger.log("❗ Icon_factory_actor aborting1");
            return Optional.empty();
        }
        if ( icon_factory_request.destination.get_item_type() == Iconifiable_item_type.pdf)
        {
            logger.log(Stack_trace_getter.get_stack_trace("❌ SHOULD NOT HAPPEN"));
        }

        Optional<Image> op = Icons_from_disk.read_original_image_from_disk_and_return_icon(path, icon_factory_request.destination.get_item_type(), icon_factory_request.icon_size, true, icon_factory_request.owner,icon_factory_request.aborter, logger);
        //Image image = Icons_from_disk.load_native_resolution_image_from_disk(path, true, icon_factory_request.aborter, logger);
        if (op.isEmpty()) {
            if (dbg)
                logger.log("❗ Making an icon FAILED (4) for " + path.getFileName());
            return Optional.empty();
        }

        return op;
    }

    //**********************************************************
    private Optional<Image_and_properties> process_video_icons_cached_as_file(Icon_factory_request icon_factory_request, Icon_destination destination)
    //**********************************************************
    {
        if (verbose_dbg) logger.log("✅ Icon_factory_actor  process_video " + destination.get_item_path().toAbsolutePath());

        double length = Non_booleans_properties.get_animated_gif_duration_for_a_video(icon_factory_request.owner);
        Path resulting_gif_name = Icon_caching.path_for_icon_caching(destination.get_path_for_display_icon_destination(), String.valueOf(icon_factory_request.icon_size)+"_"+length, Icon_caching.gif_extension, owner, logger);
        Path cache_key = Paths.get(icon_cache_dir.toAbsolutePath().toString(), resulting_gif_name.getFileName().toString());

        // we are going to create an animated gif using ffmpeg
        // ... unless it is already in the mmap cache

        Image image_from_cache = null;
        if ( image_as_file)
        {
            image_from_cache = Mmap.instance.read_image_as_file(cache_key.toAbsolutePath().toString());
        }
        else
        {
            byte[] bytes = Mmap.instance.read_file(cache_key);
            if (bytes != null) {
                try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
                    image_from_cache = new Image(bais);
                    logger.log("Retrieved image FROM FILE, w= " + image_from_cache.getWidth() + " h= " + image_from_cache.getHeight());
                    logger.log("error:" + image_from_cache.isError() + " " + image_from_cache.getException());
                    } catch (IOException e) {
                        logger.log(Stack_trace_getter.get_stack_trace("" + e));
                    }
            }
        }
        if (icon_factory_request.aborter.should_abort()) {
            if (aborting_dbg) logger.log("❗ Icon_factory_actor process_video aborting4");
            return Optional.empty();
        }
        if (image_from_cache != null)
        {
            if (dbg)
                logger.log("✅ Icon_factory_actor process_video found in cache(): " + destination.get_item_path().getFileName());

            Image_properties properties = new Image_properties(image_from_cache.getWidth(), image_from_cache.getHeight(), Rotation.normal);
            return Optional.of(new Image_and_properties(image_from_cache, properties));
        }
        if (dbg)
            logger.log("❗ Icon_factory_actor process_video load from GIF tmp FAILED for " + destination.get_item_path());

        //File gif_animated_icon_file = Icons_from_disk.file_for_cache(icon_cache_dir, destination.get_icon_path(), ""+icon_factory_request.icon_size+"_"+length, gif_extension);
        if (icon_factory_request.aborter.should_abort()) {
            if (aborting_dbg) logger.log("❗ Icon_factory_actor process_video aborting5");
            return Optional.empty();
        }


        double skip = 0;
        {
            Double duration_in_seconds = Ffmpeg_utils.get_media_duration(destination.get_item_path(), icon_factory_request.owner, logger);
            if (duration_in_seconds != null) {
                if (duration_in_seconds > 3 * 3600) {
                    logger.log("❗ WARNING: ffprobe reports duration that looks wrong");
                    duration_in_seconds = 1800.0;
                }

                if (duration_in_seconds < 0) {
                    duration_in_seconds = length;
                }

                if (duration_in_seconds < length) {
                    length = duration_in_seconds;
                } else {
                    // jump to the middle of the movie
                    skip = duration_in_seconds / 2 - length;
                }
            }
        }


        Ffmpeg_utils.video_to_gif(
                destination.get_item_path(),
                icon_factory_request.icon_size,
                10,
                cache_key,
                length,
                skip,
                0,
                icon_factory_request.get_aborter(), icon_factory_request.owner,logger);
        if (icon_factory_request.aborter.should_abort()) {
            if (aborting_dbg) logger.log("❗ Icon_factory_actor process_video aborting6");
            return Optional.empty();
        }

        if (cache_key.toFile().length() == 0) {
            logger.log("❗process_video ERROR animated gif empty " + cache_key.toAbsolutePath());
            return Optional.empty();
        }

        if (verbose_dbg)
            logger.log("✅ Icon_factory process_video Animated gif icon MADE for " + destination.get_item_path().getFileName() + " as " + cache_key.toAbsolutePath());


        image_from_cache = Icons_from_disk.load_icon(cache_key, logger);
        //Image image_from_cache = Icons_from_disk.get_image_from_cache(destination.get_path_for_display_icon_destination(), icon_factory_request.icon_size, icon_factory_request.owner,logger);

        if (icon_factory_request.aborter.should_abort()) {
            if (aborting_dbg) logger.log("❗ Icon_factory_actor process_video aborting7");
            return Optional.empty();
        }
        if (image_from_cache == null) {
            logger.log("❗ Icon_factory_actor process_video load from file FAILED (5) for " + destination.get_item_path().getFileName());
            return Optional.empty();
        }
        if ((image_from_cache.getHeight() == 0) && (image_from_cache.getWidth() == 0)) {
            logger.log("❗ Icon_factory_actor process_video load from file FAILED (6) for " + destination.get_item_path().getFileName());
            return Optional.empty();
        }
        //logger.log("Icon_factory returning image for :" + destination.get_item_path().getFileName());
        Runnable on_end = ()->
        {/*
            try
            {
                Files.delete(cache_key);
            }
            catch (IOException e)
            {
                logger.log(""+e);
            }*/
        };
        if ( image_as_file)
        {
            Mmap.instance.write_image_as_file(cache_key,(int)image_from_cache.getWidth(),(int)image_from_cache.getHeight(),true, on_end);
        }
        else
        {
            Mmap.instance.write_file(cache_key,true);
        }
        logger.log("Icon_factory process_video, WROTE animated GIF to cache, returning image for :" + destination.get_item_path().getFileName());


        Image_properties properties = new Image_properties(image_from_cache.getWidth(), image_from_cache.getHeight(), Rotation.normal);
        return Optional.of(new Image_and_properties(image_from_cache, properties));

    }

    //**********************************************************
    private Optional<Image_and_properties> process_image_icons_cached_as_file(Iconifiable_item_type item_type, Icon_factory_request icon_factory_request, Icon_destination destination)
    //**********************************************************
    {
        if (verbose_dbg) logger.log("✅ Icon_factory_actor  process_image_icons_cached_as_file " + destination.get_item_path().toAbsolutePath());

        double length = Non_booleans_properties.get_animated_gif_duration_for_a_video(icon_factory_request.owner);
        Path resulting_gif_name = Icon_caching.path_for_icon_caching(destination.get_path_for_display_icon_destination(), String.valueOf(icon_factory_request.icon_size)+"_"+length, Icon_caching.gif_extension, owner, logger);
        Path cache_key = Paths.get(icon_cache_dir.toAbsolutePath().toString(), resulting_gif_name.getFileName().toString());

        // we are going to create an animated gif using ffmpeg
        // ... unless it is already in the mmap cache

        Image image_from_cache = null;
        if ( image_as_file)
        {
            image_from_cache = Mmap.instance.read_image_as_file(cache_key.toAbsolutePath().toString());
        }
        else
        {
            byte[] bytes = Mmap.instance.read_file(cache_key);
            if (bytes != null) {
                try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
                    image_from_cache = new Image(bais);
                    logger.log("Retrieved image FROM FILE, w= " + image_from_cache.getWidth() + " h= " + image_from_cache.getHeight());
                    logger.log("error:" + image_from_cache.isError() + " " + image_from_cache.getException());
                } catch (IOException e) {
                    logger.log(Stack_trace_getter.get_stack_trace("" + e));
                }
            }
        }
        if (icon_factory_request.aborter.should_abort()) {
            if (aborting_dbg) logger.log("❗ Icon_factory_actor process_image_icons_cached_as_file aborting4");
            return Optional.empty();
        }
        if (image_from_cache != null)
        {
            if (dbg)
                logger.log("✅ Icon_factory_actor process_image_icons_cached_as_file found in cache(): " + destination.get_item_path().getFileName());

            Image_properties properties = new Image_properties(image_from_cache.getWidth(), image_from_cache.getHeight(), Rotation.normal);
            return Optional.of(new Image_and_properties(image_from_cache, properties));
        }
        if (dbg)
            logger.log("❗ Icon_factory_actor process_image_icons_cached_as_file load from GIF tmp FAILED for " + destination.get_item_path());

        //File gif_animated_icon_file = Icons_from_disk.file_for_cache(icon_cache_dir, destination.get_icon_path(), ""+icon_factory_request.icon_size+"_"+length, gif_extension);
        if (icon_factory_request.aborter.should_abort()) {
            if (aborting_dbg) logger.log("❗ Icon_factory_actor process_image_icons_cached_as_file aborting5");
            return Optional.empty();
        }

        {
            Optional<Image> op = Icons_from_disk.read_original_image_from_disk_and_return_icon(destination.get_path_for_display_icon_destination(), item_type, icon_factory_request.icon_size, true, owner, aborter, logger);

            if (icon_factory_request.aborter.should_abort()) {
                if (aborting_dbg) logger.log("❗ Icon_factory_actor process_image_icons_cached_as_file aborting7");
                return Optional.empty();
            }
            if (op.isEmpty()) {
                logger.log("❗ Icon_factory_actor process_image_icons_cached_as_file load from file FAILED (5) for " + destination.get_item_path().getFileName());
                return Optional.empty();
            }
            if ((op.get().getHeight() == 0) && (op.get().getWidth() == 0)) {
                logger.log("❗ Icon_factory_actor process_image_icons_cached_as_file load from file FAILED (6) for " + destination.get_item_path().getFileName());
                return Optional.empty();
            }
            //logger.log("Icon_factory returning image for :" + destination.get_item_path().getFileName());
            Runnable on_end = () ->
            {/*
            try
            {
                Files.delete(cache_key);
            }
            catch (IOException e)
            {
                logger.log(""+e);
            }*/
            };
            if (image_as_file) {
                Mmap.instance.write_image_as_file(cache_key,(int) op.get().getWidth(),(int) op.get().getHeight(), true, on_end);
            } else {
                Mmap.instance.write_file(cache_key, true);
            }
            logger.log("Icon_factory process_image_icons_cached_as_file, WROTE animated GIF to cache, returning image for :" + destination.get_item_path().getFileName());


            Image_properties properties = new Image_properties(op.get().getWidth(), op.get().getHeight(), Rotation.normal);
            return Optional.of(new Image_and_properties(op.get(), properties));
        }
    }



    //**********************************************************
    private Optional<Image_and_properties> process_pdf_icons_cached_as_pixels(Icon_factory_request icon_factory_request, Icon_destination destination)
    //**********************************************************
    {

        if (pdf_dbg)
            logger.log("✅ Icon_factory_actor  process_pdf " + destination.get_item_path().toAbsolutePath());


        Path resulting_png_name = Icon_caching.path_for_icon_caching(destination.get_path_for_display_icon_destination(), String.valueOf(icon_factory_request.icon_size), Icon_caching.png_extension, owner, logger);
        Path cache_key = Paths.get(icon_cache_dir.toAbsolutePath().toString(), resulting_png_name.getFileName().toString());

        logger.log("TRY FROM CACHE, as pixels: "+cache_key);
        Image image_from_cache = Mmap.instance.read_image_as_pixel(cache_key.toAbsolutePath().toString());
        if (icon_factory_request.aborter.should_abort())
        {
            if ( aborting_dbg) logger.log("❗ Icon_factory_actor aborting8");
            return Optional.empty();
        }
        if (image_from_cache != null)
        {
            if (pdf_dbg) logger.log("✅ Icon_factory_actor found in cache(1) for: " + destination.get_item_path().getFileName());
            Image_properties properties = new Image_properties(image_from_cache.getWidth(),image_from_cache.getHeight(),Rotation.normal);
            return Optional.of(new Image_and_properties(image_from_cache,properties));
        }

        if (pdf_dbg)
            logger.log("❗ Icon_factory_actor  load from disk cache FAILED for " + destination.get_item_path().toAbsolutePath() + " MAKING IT NOW");

        // we are going to create the PNG using pdfbox!

        File file_in = destination.get_item_path().toFile();
        if (icon_factory_request.aborter.should_abort())
        {
            if ( aborting_dbg) logger.log("❗ Icon_factory_actor aborting9");
            return Optional.empty();
        }
        {
            // gm convert -density 300 -resize 256x256 -quality 90 input.pdf output.png
            List<String> command_line_for_GraphicsMagic = new ArrayList<>();
            command_line_for_GraphicsMagic.add(External_application.GraphicsMagick.get_command(owner,logger));
            command_line_for_GraphicsMagic.add("convert");
            command_line_for_GraphicsMagic.add("-density");
            command_line_for_GraphicsMagic.add("300");
            command_line_for_GraphicsMagic.add("-resize");
            command_line_for_GraphicsMagic.add(""+icon_factory_request.icon_size+"x"+icon_factory_request.icon_size);
            command_line_for_GraphicsMagic.add("-quality");
            command_line_for_GraphicsMagic.add("90");
            command_line_for_GraphicsMagic.add(file_in.getAbsolutePath().toString());
            command_line_for_GraphicsMagic.add(cache_key.toAbsolutePath().toString());
            StringBuilder sb = null;
            if ( pdf_dbg) sb = new StringBuilder();
            File wd = file_in.getParentFile();
            Execute_result res = Execute_command.execute_command_list(command_line_for_GraphicsMagic, wd, 2000, sb,logger);
            if ( !res.status())
            {
                List<String> verify = new ArrayList<>();
                verify.add(External_application.GraphicsMagick.get_command(owner,logger));
                verify.add("--version");
                String home = System.getProperty(String_constants.USER_HOME);
                Execute_result res2 = Execute_command.execute_command_list(verify, new File(home), 20 * 1000, null, logger);
                if ( !res2.status())
                {
                    Booleans.manage_show_graphicsmagick_install_warning(owner,logger);
                }
                return Optional.empty();
            }
            if ( pdf_dbg) logger.log(sb.toString());
        }

        if (pdf_dbg) logger.log("✅ image of PDF write done (2)" + resulting_png_name);
        image_from_cache = Icons_from_disk.load_icon(resulting_png_name,logger);
        if (icon_factory_request.aborter.should_abort())
        {
            if ( aborting_dbg) logger.log("❗ Icon_factory_actor aborting14");
            return Optional.empty();
        }
        if (image_from_cache == null)
        {
            logger.log("❗ Icon_factory_actor load from file FAILED (7) for " + destination.get_item_path().getFileName());
            return Optional.empty();
        }
        else
        {
            if (pdf_dbg) logger.log("✅ image of PDF found on disk OK (3)" + resulting_png_name);
        }

        if ( Icon_writer_actor.use_mmap )
        {
            Runnable on_end = ()->
            {
                /*try
                {
                    Files.delete(resulting_png_name);
                }
                catch (IOException e)
                {
                    logger.log(""+e);
                }*/
            };
            String key = cache_key.toAbsolutePath().toString();
            logger.log("mmap WRITE key ->"+key+"<-");
            Mmap.instance.write_image_as_pixels(key, image_from_cache,true, on_end);
        }
        Image_properties properties = new Image_properties(image_from_cache.getWidth(),image_from_cache.getHeight(),Rotation.normal);
        return Optional.of(new Image_and_properties(image_from_cache,properties));
    }

/*
    //**********************************************************
    private Optional<Image_and_properties> process_animated_gif(Icon_factory_request icon_factory_request, Icon_destination destination)
    //**********************************************************
    {
        if (verbose_dbg) logger.log("✅ Icon_factory_actor  process_animated_gif " + destination.get_item_path().toAbsolutePath());

        boolean bypass = true;
        if ( bypass)
        {
            Image image = Icons_from_disk.load_icon(destination.get_item_path(), logger);
            Image_properties properties = new Image_properties(image.getWidth(), image.getHeight(), Rotation.normal);
            return Optional.of(new Image_and_properties(image, properties));

        }
        Path resulting_gif_name = Icon_caching.path_for_icon_caching(destination.get_path_for_display_icon_destination(), String.valueOf(icon_factory_request.icon_size), Icon_caching.gif_extension, owner, logger);
        Path cache_key = Paths.get(icon_cache_dir.toAbsolutePath().toString(), resulting_gif_name.getFileName().toString());

        // we are going to create a scaled down animated gif using Image
        // ... unless it is already in the mmap cache

        Image image_from_cache = null;
        boolean image_as_file = false;
        if ( image_as_file)
        {
            image_from_cache = Mmap.instance.read_image_as_file(cache_key.toAbsolutePath().toString());
        }
        else
        {
            byte[] bytes = Mmap.instance.read_file(cache_key);
            if (bytes != null) {
                try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
                    image_from_cache = new Image(bais);
                    logger.log("Retrieved image FROM FILE, w= " + image_from_cache.getWidth() + " h= " + image_from_cache.getHeight());
                    logger.log("error:" + image_from_cache.isError() + " " + image_from_cache.getException());
                } catch (IOException e) {
                    logger.log(Stack_trace_getter.get_stack_trace("" + e));
                }
            }
        }
        if (icon_factory_request.aborter.should_abort()) {
            if (aborting_dbg) logger.log("❗ Icon_factory_actor process_animated_gif aborting4");
            return Optional.empty();
        }
        if (image_from_cache != null)
        {
            if (dbg)
                logger.log("✅ Icon_factory_actor process_animated_gif found in cache(): " + destination.get_item_path().getFileName());

            Image_properties properties = new Image_properties(image_from_cache.getWidth(), image_from_cache.getHeight(), Rotation.normal);
            return Optional.of(new Image_and_properties(image_from_cache, properties));
        }
        if (dbg)
            logger.log("❗ Icon_factory_actor process_animated_gif load from cache FAILED for " + destination.get_item_path());

        if (icon_factory_request.aborter.should_abort()) {
            if (aborting_dbg) logger.log("❗ Icon_factory_actor process_animated_gif aborting5");
            return Optional.empty();
        }
        if (cache_key.toFile().length() == 0) {
            logger.log("❗process_animated_gif ERROR animated gif empty " + cache_key.toAbsolutePath());
            return Optional.empty();
        }
        if (verbose_dbg)
            logger.log("✅ Icon_factory process_animated_gif Animated gif icon MADE for " + destination.get_item_path().getFileName() + " as " + cache_key.toAbsolutePath());


        image_from_cache = Icons_from_disk.load_icon2(cache_key, icon_factory_request.icon_size, logger);
        //Optional<Image> ll = Icons_from_disk.read_original_image_from_disk_and_return_icon(cache_key,Iconifiable_item_type.animated_gif,icon_factory_request.icon_size,true,owner,aborter,logger);

        if (icon_factory_request.aborter.should_abort()) {
            if (aborting_dbg) logger.log("❗ Icon_factory_actor process_animated_gif aborting7");
            return Optional.empty();
        }
        if ( image_from_cache != null ) {
            logger.log("Icon_factory_actor process_animated_gif read_original_image_from_disk_and_return_icon OK for " + destination.get_item_path().getFileName());
        }
        else
        {
            logger.log("❗ Icon_factory_actor process_animated_gif load from file FAILED (5) for " + destination.get_item_path().getFileName());
            return Optional.empty();
        }
        if ((image_from_cache.getHeight() == 0) && (image_from_cache.getWidth() == 0)) {
            logger.log("❗ Icon_factory_actor process_animated_gif load from file FAILED (6) for " + destination.get_item_path().getFileName());
            return Optional.empty();
        }

        if ( image_as_file)
        {
            Mmap.instance.write_image_as_file(cache_key,image_from_cache,true, null);
        }
        else
        {
            Mmap.instance.write_file(cache_key,true);
        }
        logger.log("Icon_factory process_animated_gif, WROTE animated GIF to cache, returning image for :" + destination.get_item_path().getFileName());


        Image_properties properties = new Image_properties(image_from_cache.getWidth(), image_from_cache.getHeight(), Rotation.normal);
        return Optional.of(new Image_and_properties(image_from_cache, properties));

    }

*/



    //**********************************************************
    private Optional<Image_and_properties> do_it(
            Iconifiable_item_type item_type,
            Icon_factory_request icon_factory_request,
            Icon_destination destination)
    //**********************************************************
    {
        if (verbose_dbg) logger.log("✅ Icon_factory_actor  do_it " + destination.get_item_path().toAbsolutePath());

        String icon_filename_adder = "";
        if ( item_type == Iconifiable_item_type.video)
        {
            double length = Non_booleans_properties.get_animated_gif_duration_for_a_video(icon_factory_request.owner);
            icon_filename_adder = "_"+length;
        }
        Path resulting_name = null;
        String extension;
        switch(item_type)
        {
            case gif, video:
                extension = Icon_caching.gif_extension;
                break;
            default:
                extension = Icon_caching.png_extension;
                break;
        }
        resulting_name = Icon_caching.path_for_icon_caching(destination.get_path_for_display_icon_destination(), String.valueOf(icon_factory_request.icon_size)+icon_filename_adder, extension, owner, logger);
        Path cache_key = Paths.get(icon_cache_dir.toAbsolutePath().toString(), resulting_name.getFileName().toString());

        // we are going to create an animated gif using ffmpeg
        // ... unless it is already in the mmap cache


        boolean use_pixels_in_mmap = true;
        Image image_from_cache = null;
        switch (item_type)
        {
            case gif, video:
                image_from_cache = Mmap.instance.read_image_as_file(cache_key.toAbsolutePath().toString());
                break;
            default:
                //case pdf, non_javafx_image, javafx_image_not_gif_not_png, png:
                if ( use_pixels_in_mmap)
                {
                    image_from_cache = Mmap.instance.read_image_as_pixel(cache_key.toAbsolutePath().toString());
                }
                else
                {
                    image_from_cache = Mmap.instance.read_image_as_file(cache_key.toAbsolutePath().toString());
                }
                break;
        }

        if (icon_factory_request.aborter.should_abort()) {
            if (aborting_dbg) logger.log("❗ Icon_factory_actor do_it aborting4");
            return Optional.empty();
        }
        if (image_from_cache != null)
        {
            if (dbg)
                logger.log("✅ Icon_factory_actor do_it found in cache(): " + destination.get_item_path().getFileName());

            Image_properties properties = new Image_properties(image_from_cache.getWidth(), image_from_cache.getHeight(), Rotation.normal);
            return Optional.of(new Image_and_properties(image_from_cache, properties));
        }

        if (dbg) logger.log("❗ Icon_factory_actor do_it: attempt to load icon from cache FAILED for " + destination.get_item_path());

        if (icon_factory_request.aborter.should_abort()) {
            if (aborting_dbg) logger.log("❗ Icon_factory_actor do_it aborting5");
            return Optional.empty();
        }


        switch ( item_type )
        {
            case video: {
                double length = Non_booleans_properties.get_animated_gif_duration_for_a_video(icon_factory_request.owner);
                double skip = 0;
                {
                    Double duration_in_seconds = Ffmpeg_utils.get_media_duration(destination.get_item_path(), icon_factory_request.owner, logger);
                    if (duration_in_seconds != null) {
                        if (duration_in_seconds > 3 * 3600) {
                            logger.log("❗ WARNING: ffprobe reports duration that looks wrong");
                            duration_in_seconds = 1800.0;
                        }

                        if (duration_in_seconds < 0) {
                            duration_in_seconds = length;
                        }

                        if (duration_in_seconds < length) {
                            length = duration_in_seconds;
                        } else {
                            // jump to the middle of the movie
                            skip = duration_in_seconds / 2 - length;
                        }
                    }
                }


                Ffmpeg_utils.video_to_gif(
                        destination.get_item_path(),
                        icon_factory_request.icon_size,
                        10,
                        cache_key,
                        length,
                        skip,
                        0,
                        icon_factory_request.get_aborter(), icon_factory_request.owner, logger);
                if (cache_key.toFile().length() == 0) {
                    logger.log("❗do_it icon file empty " + cache_key.toAbsolutePath());
                    return Optional.empty();
                }
            }
            break;
            case pdf:
            {
                File file_in = destination.get_item_path().toFile();
                if (icon_factory_request.aborter.should_abort())
                {
                    if ( aborting_dbg) logger.log("❗ Icon_factory_actor aborting9");
                    return Optional.empty();
                }
                {
                    // gm convert -density 300 -resize 256x256 -quality 90 input.pdf output.png
                    List<String> command_line_for_GraphicsMagic = new ArrayList<>();
                    command_line_for_GraphicsMagic.add(External_application.GraphicsMagick.get_command(owner,logger));
                    command_line_for_GraphicsMagic.add("convert");
                    command_line_for_GraphicsMagic.add("-density");
                    command_line_for_GraphicsMagic.add("300");
                    command_line_for_GraphicsMagic.add("-resize");
                    command_line_for_GraphicsMagic.add(""+icon_factory_request.icon_size+"x"+icon_factory_request.icon_size);
                    command_line_for_GraphicsMagic.add("-quality");
                    command_line_for_GraphicsMagic.add("90");
                    command_line_for_GraphicsMagic.add(file_in.getAbsolutePath().toString());
                    command_line_for_GraphicsMagic.add(cache_key.toAbsolutePath().toString());
                    StringBuilder sb = null;
                    if ( pdf_dbg) sb = new StringBuilder();
                    File wd = file_in.getParentFile();
                    Execute_result res = Execute_command.execute_command_list(command_line_for_GraphicsMagic, wd, 2000, sb,logger);
                    if ( !res.status())
                    {
                        List<String> verify = new ArrayList<>();
                        verify.add(External_application.GraphicsMagick.get_command(owner,logger));
                        verify.add("--version");
                        String home = System.getProperty(String_constants.USER_HOME);
                        Execute_result res2 = Execute_command.execute_command_list(verify, new File(home), 20 * 1000, null, logger);
                        if ( !res2.status())
                        {
                            Booleans.manage_show_graphicsmagick_install_warning(owner,logger);
                        }
                        return Optional.empty();
                    }
                    if ( pdf_dbg) logger.log(sb.toString());
                    if (cache_key.toFile().length() == 0) {
                        logger.log("❗do_it icon file empty " + cache_key.toAbsolutePath());
                        return Optional.empty();
                    }
                }

                if (pdf_dbg) logger.log("✅ image of PDF write done (2)");
            }
            break;
            default: {
                Optional<Image> op = Icons_from_disk.read_original_image_from_disk_and_return_icon(destination.get_path_for_display_icon_destination(), item_type, icon_factory_request.icon_size, true, icon_factory_request.owner, icon_factory_request.aborter, logger);
                if (icon_factory_request.aborter.should_abort())
                {
                    if ( aborting_dbg)
                        logger.log("❗ Icon_factory thread: aborting3");
                    return Optional.empty();
                }
                image_from_cache = op.get();
            }
            break;
        }


        if (icon_factory_request.aborter.should_abort()) {
            if (aborting_dbg) logger.log("❗ Icon_factory_actor do_it aborting6");
            return Optional.empty();
        }

        if ( image_from_cache == null) {
            // if needed, load the generated image
            image_from_cache = Icons_from_disk.load_icon(cache_key, logger);
        }
        if (verbose_dbg)
            logger.log("✅ Icon_factory do_it Animated gif icon MADE for " + destination.get_item_path().getFileName() + " as " + cache_key.toAbsolutePath());

        if (icon_factory_request.aborter.should_abort()) {
            if (aborting_dbg) logger.log("❗ Icon_factory_actor do_it aborting7");
            return Optional.empty();
        }
        if (image_from_cache == null) {
            logger.log("❗ Icon_factory_actor do_it load from file FAILED (5) for " + destination.get_item_path().getFileName());
            return Optional.empty();
        }

        if ((image_from_cache.getHeight() == 0) && (image_from_cache.getWidth() == 0)) {
            logger.log("❗ Icon_factory_actor do_it load from file FAILED (6) for " + destination.get_item_path().getFileName());
            return Optional.empty();
        }
        //logger.log("Icon_factory returning image for :" + destination.get_item_path().getFileName());
        Runnable on_end = ()->
        {/*
            try
            {
                Files.delete(cache_key);
            }
            catch (IOException e)
            {
                logger.log(""+e);
            }*/
        };

        switch (item_type)
        {
            case gif, video:
                Mmap.instance.write_image_as_file(cache_key,(int)image_from_cache.getWidth(),(int)image_from_cache.getHeight(),true, on_end);
                break;
            default:
                if ( use_pixels_in_mmap)
                {
                    Mmap.instance.write_image_as_pixels(cache_key.toAbsolutePath().toString(),image_from_cache,true, on_end);
                }
                else
                {
                    Mmap.instance.write_image_as_file(cache_key,(int)image_from_cache.getWidth(),(int)image_from_cache.getHeight(),true, on_end);
                }
                break;

        }

        logger.log("Icon_factory do_it, WROTE animated GIF to cache, returning image for :" + destination.get_item_path().getFileName());

        Image_properties properties = new Image_properties(image_from_cache.getWidth(), image_from_cache.getHeight(), Rotation.normal);
        return Optional.of(new Image_and_properties(image_from_cache, properties));

    }

}
