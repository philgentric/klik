// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.util.image;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.stage.Window;
import klik.browser.virtual_landscape.Browsing_caches;
import klik.util.execute.actor.Aborter;
import klik.util.execute.actor.Actor_engine;
import klik.experimental.fusk.Fusk_static_core;
import klik.look.Jar_utils;
import klik.properties.Cache_folder;
import klik.properties.boolean_features.Booleans;
import klik.util.Check_remaining_RAM;
import klik.util.execute.Execute_command;
import klik.util.files_and_paths.Extensions;
import klik.util.files_and_paths.Static_files_and_paths_utilities;
//import klik.util.image.decoding.FITS;
import klik.util.image.decoding.Fast_aspect_ratio_from_exif_metadata_extractor;
import klik.util.image.decoding.Fast_width_from_exif_metadata_extractor;
import klik.properties.boolean_features.Feature;
import klik.properties.boolean_features.Feature_cache;
import klik.util.files_and_paths.Guess_file_type;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import klik.util.ui.Popups;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

//**********************************************************
public class Full_image_from_disk
//**********************************************************
{
    public static final boolean dbg = false;

    static boolean user_warned_about_slow_disk = false;

    //**********************************************************
    public static InputStream get_image_InputStream(Path original_image_file, boolean try_fusked, boolean report_if_not_found, Aborter aborter, Logger logger)
    //**********************************************************
    {
        //logger.log("get_image_InputStream");
        if (try_fusked)
        {
            long start = System.currentTimeMillis();
            byte[] buf= Fusk_static_core.defusk_file_to_bytes(original_image_file, aborter, logger);
            if ( buf == null)
            {
                logger.log("WARNING: defusk_file_to_bytes failed");

                if ( System.currentTimeMillis()-start > 1000)
                {
                    if ( !user_warned_about_slow_disk)
                    {
                        user_warned_about_slow_disk = true;
                        Actor_engine.execute(()-> Platform.runLater(
                                ()-> Popups.popup_warning(
                                        "Reading file "+original_image_file+ " was ridiculously slow.",
                                        "Maybe this is a network drive and your network connection is slow?",
                                        false,
                                        null,logger)),"Warm user about slow disk",logger);

                    }
                }
                return null;
            }
            else if ( buf.length == 0)
            {
                // not fusked, fall back to standard
            }
            else
            {
                if ( dbg) logger.log("fusked image detected "+original_image_file);
                // was fusked !
                return new ByteArrayInputStream(buf);
            }
        }
        // "standard"
        try
        {
            return new FileInputStream(original_image_file.toFile());
        }
        catch(FileNotFoundException e)
        {
            /* when the file system is under strain, this can fail, reporting "file not found", but the file is there */
            if (Files.isDirectory(original_image_file))
            {
                logger.log(Stack_trace_getter.get_stack_trace("SHOULD NOT HAPPEN (try to file-open a directory!) get_image_InputStream:"+e));
                return null;
            }
            //logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
            if ( report_if_not_found)
            {
                logger.log(Stack_trace_getter.get_stack_trace("get_image_InputStream:"+e));
            }
            return null;
        }
    }

    //**********************************************************
    @Deprecated
    public static Double determine_width(Path path, boolean report_if_not_found, Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        if (dbg) logger.log("\n\nIcons_from_disk determine_width "+path);
        double returned = Fast_width_from_exif_metadata_extractor.get_width(path,report_if_not_found, null,aborter, logger).orElse(0.0);
        // the only other way is to load the image!
        if ( returned > 0) return returned;
        if (aborter.should_abort())
        {
            //logger.log("determine_width aborting");
            return null;
        }
        if(Guess_file_type.is_this_file_an_image(path.toFile(),owner,logger))
        {
            Optional<Image> op = load_native_resolution_image_from_disk( path,  true, owner, aborter,  logger);
            if ( op.isEmpty())
            {
                logger.log("cannot load image to get aspect ratio(1)"+path);
                return null;
            }
            Image i = op.get();
            if (i.isError())
            {
                logger.log("cannot load image to get aspect ratio(2)"+path);
                return null;
            }
            return i.getWidth();
        }
        return null;
    }
    //**********************************************************
    @Deprecated
    public static Double determine_aspect_ratio(Path path, boolean report_if_not_found, Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        if (dbg) logger.log("\n\nIcons_from_disk get_aspect_ratio "+path);
        double returned = Fast_aspect_ratio_from_exif_metadata_extractor.get_aspect_ratio(path,report_if_not_found,aborter, null, logger).orElse(1.0);
        // the only other way is to load the image!
        if ( returned > 0) return returned;
        if (aborter.should_abort())
        {
            //logger.log("get_aspect_ratio aborting");
            return null;
        }
        if(Guess_file_type.is_this_file_an_image(path.toFile(),owner, logger))
        {
            Optional<Image> op = load_native_resolution_image_from_disk( path,  true, owner, aborter,  logger);
            if ( op.isEmpty())
            {
                logger.log("cannot load image to get aspect ratio(1)"+path);
                return null;
            }

            Image i = op.get();
            if (i.isError())
            {
                logger.log("cannot load image to get aspect ratio(2)"+path);
                return 1.0;
            }
            return i.getWidth()/i.getHeight();
        }
        return 1.0;//default
    }


    //**********************************************************
    public static Optional<Image> load_native_resolution_image_from_disk(Path original_image_file, boolean report_if_not_found, Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        //logger.log("load_native_resolution_image_from_disk");
        if (Check_remaining_RAM.RAM_running_low(logger)) {
            logger.log("load_native_resolution_image_from_disk NOT DONE because running low on memory ! ");
            return Optional.of(Jar_utils.get_broken_icon(300,owner,logger));
        }
        /*
        if ( Guess_file_type.use_nasa_fits_java_lib)
        {
            if ( Guess_file_type.is_this_extension_a_fits(Extensions.get_extension(original_image_file.getFileName().toString())))
            {
                logger.log("image extension is FITS");

                return FITS.load_FITS_image(original_image_file, aborter, owner, logger);
            }
        }*/
        if ( Guess_file_type.is_this_extension_a_non_javafx_type(Extensions.get_extension(original_image_file.getFileName().toString())))
        {
            logger.log("image extension indicates type cannot be loaded by javafx, using GraphicsMagick for "+original_image_file);
            return use_GraphicsMagick_for_full_image(original_image_file, aborter, owner, logger);
        }

        // use javafx Image

        InputStream input_stream = get_image_InputStream(original_image_file, Feature_cache.get(Feature.Fusk_is_on), report_if_not_found, aborter, logger);
        if ( input_stream == null) return Optional.empty();
        Image image = null;
        try
        {

            image =new Image(input_stream);
        }
        catch (OutOfMemoryError e)
        {
            logger.log("OutOfMemoryError when loading image from disk: "+original_image_file.toAbsolutePath()+" : "+e);
            Browsing_caches.clear_all_RAM_caches(logger);
            Popups.popup_Exception(null,100,"Your java VM machine is running out of RAM!\nclose some windows and/or try to increase the max in build.gradle.works and restart",owner,logger);
            return Optional.of(Jar_utils.get_broken_icon(300,owner,logger));
        }
        catch (Exception e)
        {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
            Popups.popup_Exception(e,100,"An error occurred while loading an image from disk",owner,logger);
            return Optional.of(Jar_utils.get_broken_icon(300,owner,logger));
        }
        try {
            input_stream.close();
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
            e.printStackTrace();
        }
        if ( image.isError())
        {
            if( image.getException().toString().contains("OutOfMemoryError"))
            {
                logger.log("❌ IMAGE decode Panic :"+image.getException()+" "+original_image_file.toAbsolutePath());
                Popups.popup_Exception(image.getException(),100,"Your java VM machine is running out of RAM, try to increase the max in build.gradle.works and restart",owner,logger);
            }
            else if( image.getException().toString().contains("No loader for image data"))
            {
                logger.log("❌ IMAGE decode failed :"+image.getException()+" "+original_image_file.toAbsolutePath());
                // this occurs on damaged images like download not finished, or fusk wrong pin code
                // Popups.popup_Exception(image.getException(),100,"If this image was fusked, maybe the pin code is wrong?",logger);
            }
            else
            {
                logger.log("IMAGE ERROR :"+original_image_file.toAbsolutePath()+" : "+image.getException());
            }
        }
        return Optional.of(image);

    }

    //**********************************************************
    private static Optional<Image> use_GraphicsMagick_for_full_image(Path original_image_file, Aborter aborter, Window owner, Logger logger)
    //**********************************************************
    {
        //logger.log("using GraphicsMagick_for_full_image");
        Path icon_cache_dir = Static_files_and_paths_utilities.get_cache_dir(Cache_folder.klik_icon_cache, owner, logger);
        Path png_path = icon_cache_dir.resolve(original_image_file.getFileName().toString()+"_full.png");

        if ( !png_path.toFile().exists())
        {
            logger.log("png (converted image) does not exist, creating "+png_path);
            // use GraphicsMagick to convert to png
            List<String> list = List.of("gm", "convert", original_image_file.toAbsolutePath().toString(), png_path.toAbsolutePath().toString());
            Execute_command.execute_command_list(list, new File("."), 20_000,null, logger);
        }
        else
        {
            logger.log("png (converted image) exists:  "+png_path);
        }

        if ( aborter.should_abort()) return Optional.empty();

        try ( InputStream is = new FileInputStream(png_path.toFile())) {
            return Optional.of(new Image(is));
        }
        catch (IOException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
            // GraphicsMagick failed, let us try the same with imageMagick
            return  use_ImageMagick_for_full_image(original_image_file, aborter, owner, logger);
        }
    }

    //**********************************************************
    private static Optional<Image> use_ImageMagick_for_full_image(Path original_image_file, Aborter aborter, Window owner, Logger logger)
    //**********************************************************
    {
        logger.log("using ImageMagick (fallback!) to load image by converting it");
        Path icon_cache_dir = Static_files_and_paths_utilities.get_cache_dir(Cache_folder.klik_icon_cache, owner, logger);
        Path png_path = icon_cache_dir.resolve(original_image_file.getFileName().toString()+"_full.png");

        if ( !png_path.toFile().exists())
        {
            logger.log("png (converted image) does not exist, creating "+png_path);
            // use ImageMagick to convert to png
            List<String> list = List.of("magick", original_image_file.toAbsolutePath().toString(), png_path.toAbsolutePath().toString());
            if ( Execute_command.execute_command_list(list, new File("."), 20_000,null, logger)==null);
            {
                Booleans.manage_show_imagemagick_install_warning(owner,logger);
            }
        }
        else
        {
            logger.log("SHOULD NOT HAPPEN ! png (converted image) exists:  "+png_path);
        }

        if ( aborter.should_abort()) return Optional.empty();

        try ( InputStream is = new FileInputStream(png_path.toFile())) {
            return Optional.of(new Image(is));
        }
        catch (IOException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
        }
        return Optional.of(Jar_utils.get_broken_icon(300,owner,logger));
    }



}
