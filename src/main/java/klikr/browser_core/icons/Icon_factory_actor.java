// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES animated_gifs/Ffmpeg_utils.java
//SOURCES ../Image_and_properties.java
//SOURCES ../items/Iconifiable_item_type.java
//SOURCES ../../util/files_and_paths/Static_files_and_paths_utilities.java
//SOURCES ../../images/decoding/Fast_image_property_from_exif_metadata_extractor.java
//SOURCES ../../util/execute/Execute_command.java
//SOURCES animated_gifs/Animated_gif_from_folder_content.java

package klikr.browser_core.icons;

import javafx.scene.image.Image;
import javafx.stage.Window;
import klikr.settings.String_constants;
import klikr.settings.boolean_features.Feature;
import klikr.settings.boolean_features.Feature_cache;
import klikr.util.External_application;
import klikr.util.cache.Klikr_cache;
import klikr.util.execute.Execute_result;
import klikr.util.execute.actor.*;
import klikr.util.animated_gifs.Ffmpeg_utils;
import klikr.browser_core.Image_and_properties;
import klikr.browser_core.icons.image_properties_cache.*;
import klikr.browser_core.items.Iconifiable_item_type;
import klikr.settings.boolean_features.Booleans;
import klikr.util.cache.Cache_folder;
import klikr.settings.Non_booleans_properties;
import klikr.util.image.Icons_from_disk;
import klikr.util.execute.Execute_command;
import klikr.util.image.Static_image_utilities;
import klikr.util.image.decoding.Exif_metadata_extractor;
import klikr.util.image.icon_cache.Icon_caching;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;
import klikr.util.mmap.Mmap;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


// an actor-style asynchronous icon factory
//**********************************************************
public class Icon_factory_actor implements Actor
//**********************************************************
{

    // if( Feature_cache.get(Feature.Enable_mmap_caching))
    // there are 3 possibilities for caching PNG icons (of JPGs):
    // - if cache_png_in_mmap is false,
    // for jpg, png and pdf, we store the icons in a cache folder:
    // - if cache_png_in_mmap is true
    // for jpg, png and pdf, we store the icons in mmap
    // - if use_pixels_in_mmap is true, we store PIXELS in mmap
    // (takes more RAM/file but speed is unbeatable)
    // - if use_pixels_in_mmap is false, we store the PNG files in mmap
    // ... we do the job the OS should normally do i.e. for optimal disk reading
    // speed the OS maps the file in virtual memory too, but here we bypass
    // the (complex!) OS decision process about when to map or not
    private static final boolean use_pixels_in_mmap = true;
    // not meaningful if cache_png_in_mmap is false
    // when use_pixels_in_mmap = true, png icon read is 10 times FASTER (pixels versus file)

    // for GIF, and especially animated gifs, which can be quite large...
    // if cache_gifs_as_files_in_mmap is false, we do not cache GIFS at all,
    // the icon is made from the original file ...which is slow
    // if cache_gifs_as_files_in_mmap is  true,
    // we MAKE a sized icon new gif file, that we store as FILE in mmap,
    // only the first time, when the cache misses, we use Image() to get a resized Image
    // the next time, the resizes icon is in mmap as anfile
    // and then animated gifs are loaded much faster ... but the RAM+file cost is high
    private static final boolean cache_gifs_as_files_in_mmap = true; //Feature_cache.get(Feature.Enable_mmap_caching);


    private static final boolean dbg = false;
    private static final boolean make_icon_dbg = false;

    private static final boolean verbose_dbg = false;
    private static final boolean pdf_dbg = false;
    private static final boolean aborting_dbg = false;

    Logger logger;
    Icon_writer_actor writer;
    private final Window owner;
    private final Klikr_cache<Path, Image_properties> image_properties_cache;

    private final Path icon_cache_dir;

    //**********************************************************
    public Icon_factory_actor(
            Klikr_cache<Path, Image_properties> image_properties_cache,
            Window owner, Logger logger)
    //**********************************************************
    {
        this.image_properties_cache = image_properties_cache;
        this.owner = owner;
        this.logger = logger;
        if (dbg) logger.log(Logger.ok+" Icon_factory actor created");
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
        if (dbg) logger.log(Logger.ok+" Icon caching, icon request processing starts ");

        Icon_destination destination = icon_factory_request.destination;
        if (destination == null) {
            logger.log(Stack_trace_getter.get_stack_trace("SHOULD NOT HAPPEN icon factory : cancel! destination==null"));
            return "should not happen";
        }

        Image_and_properties image_and_properties = null;
        for(;;)
        {
            image_and_properties = do_it(icon_factory_request, destination);
            if ( image_and_properties == null)
            {
                // happens for example for 'rotten' files i.e an empty file with a .jpg extension
                // = no retry
                //logger.log(Stack_trace_getter.get_stack_trace("SHOULD NOT HAPPEN icon factory :image_and_properties==null"));
                return "icon failed 1";

//                logger.log("WARNING: returning BROKEN for: "+icon_factory_request.destination.get_item_path());
//                image_and_properties = Image_and_properties.broken(owner,logger);
//                break;
            }
            if ( image_and_properties.properties() == null)
            {
                // = no retry
                logger.log(Stack_trace_getter.get_stack_trace("SHOULD NOT HAPPEN icon factory :image_and_properties.properties()==null"));
                return "icon failed 2";

//                logger.log("WARNING: returning BROKEN for: "+icon_factory_request.destination.get_item_path());
//                image_and_properties = Image_and_properties.broken(owner,logger);
//                break;
            }
            if ( image_and_properties.properties().rotation() == null)
            {
                // = no retry
                logger.log(Stack_trace_getter.get_stack_trace("SHOULD NOT HAPPEN icon factory :image_and_properties.properties().rotation()==null"));
                return "icon failed 3";

//                logger.log("WARNING: returning BROKEN for: "+icon_factory_request.destination.get_item_path());
//                image_and_properties = Image_and_properties.broken(owner,logger);
//                break;
            }
            if (image_and_properties.image() != null) {
                double w = image_and_properties.image().getWidth();
                double h = image_and_properties.image().getHeight();
                if ((w > 0.0)&&(h > 0.0)) {
                    // SUCCESS!!
                    break;
                }
            }
            logger.log("Icon caching, something went wrong, image w = "+image_and_properties.image().getWidth()+", h = "+image_and_properties.image().getHeight());

            // retry a few times
            if (icon_factory_request.retry_count < Icon_factory_request.max_retry)
            {
                icon_factory_request.retry_count++;
                logger.log(Logger.warning+" Icon caching, RETRYING : " + icon_factory_request.retry_count + " times, after empty icon for : " + destination.get_item_path() );
                try {
                    Thread.sleep(100*icon_factory_request.retry_count);
                } catch (InterruptedException e) {
                    logger.log(""+e);
                    return "failed";
                }
                continue;
            }
            logger.log(Logger.warning+" Icon caching, too many retries after empty icon for : " + destination.get_item_path() );
            return "icon failed";
        }


        if ( dbg)  check(image_and_properties);

        destination.receive_icon(image_and_properties);
        Image_properties image_properties = image_and_properties.properties();
        if ( image_properties != null)
        {
            Path p = destination.get_item_path();
            if ( p == null)
            {
                logger.log(Stack_trace_getter.get_stack_trace(""));
            }
            image_properties_cache.inject(p,image_properties,false);
        }
        //logger.log("Icon_factory_actor: "+ instance.decrementAndGet());

        return "icon done";
    }

    //**********************************************************
    private void check(Image_and_properties image_and_properties)
    //**********************************************************
    {
        if ( image_and_properties == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace(Logger.warning+" Icon caching, image_and_properties == null"));
            return;
        }
        if ( image_and_properties.image() == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace(Logger.warning+" Icon caching, image_and_properties.image() == null"));
            return;
        }
        if ( image_and_properties.properties() == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace(Logger.warning+" Icon caching, image_and_properties.properties() == null"));
            return;
        }
        if ( image_and_properties.properties().rotation() == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace(Logger.warning+" Icon caching, image_and_properties.properties().rotation() == null"));
        }
    }


    /*
    we cache icons to make folder navigation faster
    - if cache_png_in_mmap is false : cache to file
    - if cache_png_in_mmap is true: cache to mmap 
    generally much faster ...
    if use_pixels_in_mmap is true, we cache (decoded image) PIXELS directly
    => blazing fast load, at a high RAM/file cost

    javafx Image is capable of loading ONLY jpeg, gif, animated gif, png
    ... good news: it can resize (e.g. to icon size)

    javafx does not have an image-writing capability, the easy way is to use ImageIO,
    but it involves AWT dependencies, which is not supported with GraalVM/native

    so the ONLY image writing capability in Klikr is a PNG writer,
    used only for caching JPG icons on disk, when caching on disk

    The situation is frankly complex:

    for JPG, we make an icon using read_original_image_from_disk_and_return_icon
        So it is 'directly' a javaFX Image for the icon
        If cache_png_in_mmap is false, we WRITE a .PNG file in the cache folder
        If cache_png_in_mmap is true, we write PIXELS in Mmap

    for the other image formats we use GraphicsMagick, or ImageMagick
    if GraphicsMagick failed, to convert file-to-file to PNG, that javaFX can load
    (slow ...) so the icon is also PNG

    for PDF, we use GraphicsMagick to WRITE a PNG of the first page using
        So we then need to load that PNG into a javaFX Image for the icon
        If cache_png_in_mmap is false, nothing else to do, 
            since we already have the file in the cache folder
        If cache_png_in_mmap is true, we can write either the PIXELS or
            the .PNG AS A FILE in Mmap
            and we delete the .PNG in the cache folder

    for video, we use FFMPEG to WRITE an animated GIF
        So we then need to load that GIF into a javaFX Image for the icon
        If cache_png_in_mmap is false, nothing else to do, 
            since we already have the file in the cache folder
        If cache_png_in_mmap is true, we write the .GIF file in Mmap
            we do not have the option to write PIXELS because javaFX does not
            support PixelReader for multi-frame GIFs
            and we delete the .GIF in the cache folder

    for GIF we make an icon just like for JPG, but we produce a GIF (not a PNG) so that
    for animated GIFs the icon is also animated !


TODO:
        If cache_png_in_mmap is false, we do nothing
            - for single GIF the gain is too small,
            - for animated, we cannot write the icon
            this is why browsing folders with many animated GIFS used to be slow
        If cache_png_in_mmap is true,
            if cache_gifs_as_files_in_mmap is true, we copy the whole .GIF file in Mmap
            however, this means that we cannot use a resized version!
            (for which we cannot make a file)
            this is why browsing folders with many animated GIFS is fast
            but the RAM+file cost is high


     */

    //**********************************************************
    private Path make_icon_path(
            Path original_path,
            Icon_factory_request icon_factory_request,
            Icon_destination destination)
    //**********************************************************
    {
        if ( original_path == null )
        {
            logger.log(Stack_trace_getter.get_stack_trace(""));
            return null;
        }
        String icon_filename_adder = "";
        Iconifiable_item_type item_type = destination.get_item_type();
        if ( item_type == Iconifiable_item_type.video)
        {
            double length = Non_booleans_properties.get_animated_gif_duration_for_a_video(icon_factory_request.owner);
            icon_filename_adder = "_"+length;
        }
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
        return Icon_caching.path_for_icon_caching(original_path, String.valueOf(icon_factory_request.icon_size)+icon_filename_adder, extension, owner, logger);
    }

    //**********************************************************
    private Image_and_properties get_icon_from_cache(Path original_path,
                                                     Path icon_path,
                                                     Icon_factory_request icon_factory_request,
                                                     Icon_destination destination)
    //**********************************************************
    {
        Iconifiable_item_type item_type = destination.get_item_type();
        switch (item_type)
        {
            case gif:
            {
                Image_and_properties icon = null;
                if ( Feature_cache.get(Feature.Enable_mmap_caching))
                {
                    Optional<Image_and_properties> op;
                    op = Mmap.instance.read_image_as_file(icon_path);
                    icon = op.orElse(null);
                    if( dbg) logger.log("Icon caching, READ of GIF icon from mmap, as file");
                }
                else
                {
                    icon =  Icons_from_disk.load_icon_from_disk_cache(
                            original_path,
                            icon_factory_request.icon_size,
                            String.valueOf(icon_factory_request.icon_size),
                            Icon_caching.png_extension,
                            false, icon_factory_request.owner, logger);
                }
                return icon;
            }

            case video:
            {
                Image_and_properties icon = null;
                if ( Feature_cache.get(Feature.Enable_mmap_caching))
                {
                    Optional<Image_and_properties> op;
                    op = Mmap.instance.read_image_as_file(icon_path);
                    icon = op.orElse(null);
                    if( dbg) logger.log("Icon caching, READ of icon from mmap, as file");
                }
                else
                {
                    icon =  Icons_from_disk.load_icon_from_disk_cache(
                            original_path,
                            icon_factory_request.icon_size,
                            String.valueOf(icon_factory_request.icon_size),
                            Icon_caching.png_extension,
                            false, icon_factory_request.owner, logger);
                }
                return icon;
            }

            default:
            if( Feature_cache.get(Feature.Enable_mmap_caching))
            {
                if (use_pixels_in_mmap)
                {
                    Optional<Image_and_properties> op = Mmap.instance.read_image_as_pixels(icon_path.toAbsolutePath().toString());
                    if( dbg) if ( op.isPresent()) logger.log("Icon caching, READ of icon from mmap, as pixels");
                    return op.orElse(null);
                }
                else
                {
                    Optional<Image_and_properties> op = Mmap.instance.read_image_as_file(icon_path);
                    if( dbg) if ( op.isPresent()) logger.log("Icon caching, READ of icon from mmap, as file");
                    return op.orElse(null);
                }
            }
            else
            {
                Image_and_properties icon = Icons_from_disk.load_icon_from_disk_cache(
                        original_path,
                        icon_factory_request.icon_size,
                        String.valueOf(icon_factory_request.icon_size),
                        Icon_caching.png_extension,
                        false, icon_factory_request.owner, logger);
                if( dbg) if ( icon != null) logger.log("Icon caching, READ of icon from disk");
                return icon;
            }
        }
    }

    //**********************************************************
    private Image_and_properties make_icon(Path original_path,
                            Path icon_path,
                            Icon_factory_request icon_factory_request,
                            Icon_destination destination)
    //**********************************************************
    {
        if (make_icon_dbg)
            logger.log(Logger.warning+" Icon caching, attempt to READ icon from cache FAILED for " + destination.get_item_path());

        //Image icon_from_cache = null;
        Iconifiable_item_type item_type = destination.get_item_type();
        Path item_path = destination.get_item_path();
        if (item_path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace(""));
            return null;
        }
        switch ( item_type )
        {

            case video:
            {
                // we are going to create an animated gif using ffmpeg
                double length = Non_booleans_properties.get_animated_gif_duration_for_a_video(icon_factory_request.owner);
                double skip = 0;
                {
                    Double duration_in_seconds = Ffmpeg_utils.get_media_duration(item_path, icon_factory_request.owner, logger);
                    if (duration_in_seconds != null) {
                        if (duration_in_seconds > 3 * 3600) {
                            logger.log(Logger.warning+" WARNING: Icon caching, ffprobe reports duration that looks wrong");
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
                        item_path,
                        icon_factory_request.icon_size,
                        10,
                        icon_path,
                        length,
                        skip,
                        0,
                        icon_factory_request.get_aborter(), icon_factory_request.owner, logger);
                if (icon_path.toFile().length() == 0) {
                    logger.log(Logger.warning+"Icon caching, icon file empty " + icon_path.toAbsolutePath());
                    return null;
                }

                Image icon_from_cache = Icons_from_disk.load_icon(icon_path, logger);
                if (icon_from_cache == null)
                {
                    logger.log(Logger.warning+" Icon caching, load from file FAILED (5) for " + item_path);
                    return null;
                }
                else
                {
                    return Image_and_properties.build(icon_from_cache, false);
                }
            }

            case pdf:
            {
                // we are going to create a png of the front page using GraphicsMagick (gm)
                File file_in = item_path.toFile();
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
                    command_line_for_GraphicsMagic.add(icon_path.toAbsolutePath().toString());
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
                        return null;
                    }
                    if ( pdf_dbg) logger.log(sb.toString());
                    if (icon_path.toFile().length() == 0) {
                        logger.log(Logger.warning+" icon file empty " + icon_path.toAbsolutePath());
                        return null;
                    }
                }

                Image icon_from_cache = Icons_from_disk.load_icon(icon_path, logger);
                if (icon_from_cache == null) {
                    logger.log(Logger.warning+" Icon caching, load from file FAILED (5) for " + item_path);
                    return null;
                }
                else
                {
                    if (pdf_dbg) logger.log(Logger.ok+" Icon caching, png icon for PDF write done (2)");
                    return Image_and_properties.build(icon_from_cache,false);
                }
            }

            case gif:
            {
                // make a resized Image (this is only for the 'first time')
                Image_and_properties icon_from_cache =  Icons_from_disk.read_original_image_from_disk_and_return_icon(original_path, item_type, icon_factory_request.icon_size, true, icon_factory_request.owner, icon_factory_request.aborter, logger);
                if ( make_icon_dbg)
                {
                    if ( icon_from_cache != null)
                    {
                        logger.log("Icon caching, MADE icon for:"+ original_path+" w="+icon_from_cache.image().getWidth()+" h="+icon_from_cache.image().getHeight());
                    }
                    else
                    {
                        logger.log("Icon caching, making icon FAILED for:"+ original_path);
                    }
                }
                return icon_from_cache;
            }

            default: {
                Image_and_properties icon_from_disk = Icons_from_disk.read_original_image_from_disk_and_return_icon(original_path, item_type, icon_factory_request.icon_size, true, icon_factory_request.owner, icon_factory_request.aborter, logger);
                if ( make_icon_dbg)
                {
                    if ( icon_from_disk != null)
                    {
                        logger.log("Icon factory, MADE icon for:"+ original_path+" w="+icon_from_disk.image().getWidth()+" h="+icon_from_disk.image().getHeight());
                    }
                    else
                    {
                        logger.log("Icon factory, making icon FAILED for:"+ original_path);
                    }
                }
                return icon_from_disk;
            }
        }
    }

    // storm avoider
    //**********************************************************
    private boolean is_icon_cache_folder(Path original_path )
    //**********************************************************
    {
        if (original_path.getParent().toAbsolutePath().toString().equals(icon_cache_dir.toAbsolutePath().toString()))
        {
            // the user is browsing the icon cache. if we save a file for the icon, it will trigger the change detector ...
            // so a new icon request...  ad nauseam ! ==> storm avoidance = dont save the icon.
            if (dbg) logger.log(Logger.ok+" Icon_factory thread: (storm avoidance) not saving the icon for a file which is in the icons' cache folder" );
            return true;
        }
        return false;
    }

    private static final Set<String> icons_in_flight = ConcurrentHashMap.newKeySet();
    //**********************************************************
    private void write_icon_to_cache_in_a_thread(Image_and_properties icon_from_cache,
                                                 Path original_path,
                                                 Path icon_path,
                                                 Icon_factory_request icon_factory_request,
                                                 Icon_destination destination)
    //**********************************************************
    {
       Runnable delete_on_end = ()->{
            try
            {
                Files.delete(icon_path);
            }
            catch(IOException e)
            {
                logger.log(""+e);
            }
        };
        Iconifiable_item_type item_type = destination.get_item_type();

        // write the icon in the cache, using another thread
        Runnable icon_writer = () ->
        {
            if (icon_factory_request.aborter.should_abort())
            {
                if (aborting_dbg) logger.log(Logger.warning+" Icon_factory_actor aborting-4");
                return;
            }
            if ( icons_in_flight.contains(icon_path.toAbsolutePath().toString()) )
            {
                logger.log(Logger.warning+" Icon_factory_actor aborting-5, skipping icon write request, as another one is in flight, for: "+icon_path.toAbsolutePath());
                return;
            }
            icons_in_flight.add(icon_path.toAbsolutePath().toString());

            switch (item_type)
            {
                case video:
                {
                    Mmap.instance.write_image_as_file(icon_path, true, delete_on_end);
                    if ( dbg) logger.log("Icon caching, WROTE icon as file to disk :" + icon_path + " w=" + icon_from_cache.image().getWidth() + " h=" + icon_from_cache.image().getHeight());
                }
                break;

                case gif:
                    if (cache_gifs_as_files_in_mmap)
                    {
                        if (is_icon_cache_folder(original_path))
                        {
                            logger.log("Skipping gif icon save as the visited folder IS the icon cache!");
                        }
                        else
                        {
                            // we use GraphickMagick to create a resized icoh FILE
                            // gm convert <input.gif> -coalesce -resize 128x128 <output.gif>
                            List<String> command_line_for_GraphicsMagic = new ArrayList<>();
                            command_line_for_GraphicsMagic.add(External_application.GraphicsMagick.get_command(owner, logger));
                            command_line_for_GraphicsMagic.add("convert");
                            command_line_for_GraphicsMagic.add(original_path.toAbsolutePath().toString());
                            command_line_for_GraphicsMagic.add("-coalesce");
                            command_line_for_GraphicsMagic.add("-resize");
                            command_line_for_GraphicsMagic.add("" + icon_factory_request.icon_size + "x" + icon_factory_request.icon_size);
                            command_line_for_GraphicsMagic.add(icon_path.toAbsolutePath().toString());
                            StringBuilder sb = null;
                            if (pdf_dbg) sb = new StringBuilder();
                            Path item_path = destination.get_item_path();
                            if (item_path == null)
                            {
                                logger.log(Stack_trace_getter.get_stack_trace(""));
                                return;
                            }
                            File file_in = item_path.toFile();
                            File wd = file_in.getParentFile();
                            Execute_result res = Execute_command.execute_command_list(command_line_for_GraphicsMagic, wd, 2000, sb, logger);
                            if (!res.status()) {
                                List<String> verify = new ArrayList<>();
                                verify.add(External_application.GraphicsMagick.get_command(owner, logger));
                                verify.add("--version");
                                String home = System.getProperty(String_constants.USER_HOME);
                                Execute_result res2 = Execute_command.execute_command_list(verify, new File(home), 20 * 1000, null, logger);
                                if (!res2.status()) {
                                    Booleans.manage_show_graphicsmagick_install_warning(owner, logger);
                                }
                                logger.log(Logger.warning+" GIF icon resize failed " + icon_path.toAbsolutePath());
                                return;
                            }
                            if (pdf_dbg) logger.log(sb.toString());
                            if (icon_path.toFile().length() == 0) {
                                logger.log(Logger.warning+" GIF icon resize failed " + icon_path.toAbsolutePath());
                                return;
                            }
                            Mmap.instance.write_image_as_file(icon_path, true, delete_on_end);
                            if (dbg)
                                logger.log("Icon caching, WROTE GIF as file to mmap :" + icon_path + " w=" + icon_factory_request.icon_size + " h=" + icon_factory_request.icon_size);
                        }
                    }
                    break;

                default:
                    if ( is_icon_cache_folder(original_path)) break;
                    if( Feature_cache.get(Feature.Enable_mmap_caching))
                    {
                        if (use_pixels_in_mmap)
                        {
                            Runnable on_end = null;
                            if ( item_type == Iconifiable_item_type.pdf)
                            {
                                // for pdf, the png has been made on disk, it must be deleted to save disk space
                                on_end = delete_on_end;
                            }
                            if (!Mmap.instance.write_image_as_pixels(icon_path.toAbsolutePath().toString(), icon_from_cache, true, on_end))
                            {
                                Exif_metadata_extractor extractor = new Exif_metadata_extractor(icon_path,owner, logger);
                                double how_many_pixels = icon_from_cache.image().getWidth()*icon_from_cache.image().getHeight();
                                logger.log("something is wrong with:"+extractor.get_exif_metadata(how_many_pixels,true,new Aborter("dummy",logger),true) );
                            }
                            if ( dbg) logger.log("Icon caching, WROTE pixels to mmap :" + icon_path.toAbsolutePath().toString() + " w=" + icon_from_cache.image().getWidth() + " h=" + icon_from_cache.image().getHeight());
                        }
                        else
                        {
                            // for pdf, the png is already on disk
                            if ( item_type != Iconifiable_item_type.pdf)
                            {
                                // we need to save the icon (Image) to disk
                                Static_image_utilities.write_png_to_disk(icon_from_cache, icon_path, logger);
                            }
                            // then we save the file in mmap
                            Mmap.instance.write_image_as_file(icon_path, true, delete_on_end);
                            if ( dbg) logger.log("Icon caching, WROTE image as file to cache :" + icon_path + " w=" + icon_from_cache.image().getWidth() + " h=" + icon_from_cache.image().getHeight());
                        }
                    }
                    else
                    {
                        if ( item_type != Iconifiable_item_type.pdf)
                        {
                            Static_image_utilities.write_png_to_disk(icon_from_cache, icon_path, logger);
                            if (dbg)
                                logger.log("Icon caching, WROTE file to disk :" + icon_path.toAbsolutePath().toString() + " w=" + icon_from_cache.image().getWidth() + " h=" + icon_from_cache.image().getHeight());
                        }
                    }
                    break;
            }
            icons_in_flight.remove(icon_path.toAbsolutePath().toString());
        };
        Actor_engine.execute(icon_writer,"icon writer",logger);

    }
    //**********************************************************
    private Image_and_properties do_it(
            Icon_factory_request icon_factory_request,
            Icon_destination destination)
    //**********************************************************
    {
        // TODO: clarify the difference between
        // destination.get_item_path()
        // and
        // destination.get_path_for_display_icon_destination()

        if (icon_factory_request.aborter.should_abort())
        {
            if (aborting_dbg) logger.log(Logger.warning+" Icon_factory_actor aborting-1");
            return null;
        }

        Path original_path = destination.get_path_for_display_icon_destination();
        if( original_path == null)
        {
            // this happens quite a lot, for example
            // for empty folder (option: show folder with icons)
            //logger.log(Stack_trace_getter.get_stack_trace(""));
            return null;
        }


        if (verbose_dbg) logger.log(Logger.ok+" Icon caching, original:" + original_path);

        Path icon_path = make_icon_path(original_path, icon_factory_request, destination);
        if (verbose_dbg) logger.log("icon_path= "+icon_path);
        if( icon_path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("icon_path == null ???"));
            return null;
        }

        if (icon_factory_request.aborter.should_abort())
        {
            if (aborting_dbg) logger.log(Logger.warning+" Icon_factory_actor aborting-2");
            return null;
        }

        // try to read the icon from the cache

        Image_and_properties iap_from_cache = get_icon_from_cache(original_path, icon_path, icon_factory_request, destination);
        if (iap_from_cache != null)
        {
            if (iap_from_cache.properties() == null)
            {
                logger.log(Stack_trace_getter.get_stack_trace(Logger.error+" FATAL iap_from_cache.properties() == null ???"));
                return null;
            }
            if (dbg) logger.log(Logger.ok+" Icon caching, READ icon from cache(): " + destination.get_item_path()+ " w="+iap_from_cache.image().getWidth()+" h="+iap_from_cache.image().getHeight());
           return iap_from_cache;
        }
        if (icon_factory_request.aborter.should_abort())
        {
            if (aborting_dbg) logger.log(Logger.warning+" Icon_factory_actor aborting-3");
            return null;
        }

        // the icon was not in the cache, let us MAKE one, if needed rotated
        iap_from_cache = make_icon(original_path,icon_path, icon_factory_request, destination);
        if (iap_from_cache == null) {
            logger.log(Logger.warning+" Icon caching, load from file FAILED (5) for " + destination.get_item_path());
            return null;
        }
        if (iap_from_cache.properties() == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("FATAL iap_from_cache.properties() == null ???"));
            return null;
        }
        if (iap_from_cache.properties().rotation() == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("FATAL iap_from_cache.properties().rotation() == null ???"));
            return null;
        }
        if ((iap_from_cache.image().getHeight() == 0) && (iap_from_cache.image().getWidth() == 0)) {
            logger.log(Logger.warning+" Icon caching, load from file FAILED (6) for " + destination.get_item_path());
            return null;
        }

        write_icon_to_cache_in_a_thread(iap_from_cache,original_path,icon_path,icon_factory_request, destination);


        if ( dbg) logger.log("Icon caching, returning icon for :" + destination.get_item_path()+ " w="+iap_from_cache.image().getWidth()+" h="+iap_from_cache.image().getHeight());

        return iap_from_cache;
    }
}
