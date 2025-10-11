//SOURCES ../../images/decoding/Fast_aspect_ratio_from_exif_metadata_extractor.java
//SOURCES ../../images/decoding/Fast_width_from_exif_metadata_extractor.java
//SOURCES ../../experimental/fusk/Fusk_static_core.java

package klik.util.files_and_paths;

import javafx.application.Platform;
import javafx.stage.Window;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.browser.icons.Icon_writer_actor;
//import klik.browser.icons.JavaFX_to_Swing;
import klik.images.decoding.Fast_aspect_ratio_from_exif_metadata_extractor;
import klik.images.decoding.Fast_width_from_exif_metadata_extractor;
import klik.look.Look_and_feel_manager;
import klik.experimental.fusk.Fusk_static_core;

import javafx.scene.image.Image;
import klik.properties.boolean_features.Feature;
import klik.properties.boolean_features.Feature_cache;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import klik.util.ui.Popups;

//import javax.imageio.ImageIO;
//import java.awt.*;
//import java.awt.geom.AffineTransform;
//import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

//static utilities for loading images and icons from the disk
//**********************************************************
public class From_disk
//**********************************************************
{
    public static final boolean dbg = false;
    public static final int MIN_REMAINING_FREE_MEMORY_10MB = 10_000_000;

    private static boolean user_warned_about_slow_disk = false;
    //**********************************************************
    public static InputStream get_image_InputStream(Path original_image_file, boolean try_fusked, boolean report_if_not_found, Aborter aborter, Logger logger)
    //**********************************************************
    {
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
                                ()->Popups.popup_warning(
                                        "Reading file "+original_image_file+ " was ridiculously slow.",
                                        "Maybe this is a network drive and your network connection is slow?",
                                        false,
                                        null,logger)),logger);

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
    public static Double determine_width(Path path, boolean report_if_not_found, Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        if (dbg) logger.log("\n\nFrom_disk determine_width "+path);
        double returned = Fast_width_from_exif_metadata_extractor.get_width(path,report_if_not_found, null,aborter, logger);
        // the only other way is to load the image!
        if ( returned > 0) return returned;
        if (aborter.should_abort())
        {
            //logger.log("determine_width aborting");
            return null;
        }
        if(Guess_file_type.is_file_an_image(path.toFile()))
        {
            Image i = load_native_resolution_image_from_disk( path,  true, owner, aborter,  logger);
            if ( i==null)
            {
                logger.log("cannot load image to get aspect ratio(1)"+path);
                return null;
            }
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
    public static double determine_aspect_ratio(Path path, boolean report_if_not_found, Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        if (dbg) logger.log("\n\nFrom_disk get_aspect_ratio "+path);
        double returned = Fast_aspect_ratio_from_exif_metadata_extractor.get_aspect_ratio(path,report_if_not_found,aborter, null, logger);
        // the only other way is to load the image!
        if ( returned > 0) return returned;
        if (aborter.should_abort())
        {
            //logger.log("get_aspect_ratio aborting");
            return -1;
        }
        if(Guess_file_type.is_file_an_image(path.toFile()))
        {
            Image i = load_native_resolution_image_from_disk( path,  true, owner,aborter,  logger);
            if ( i==null)
            {
                logger.log("cannot load image to get aspect ratio(1)"+path);
                return 1.0;
            }
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
    public static Image load_native_resolution_image_from_disk(Path original_image_file, boolean report_if_not_found, Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        if (get_remaining_memory() < MIN_REMAINING_FREE_MEMORY_10MB) {
            logger.log("load_image_fx NOT DONE because running low on memory ! ");
            return null;
        }
        InputStream input_stream = get_image_InputStream(original_image_file, Feature_cache.get(Feature.Fusk_is_on), report_if_not_found, aborter, logger);
        if ( input_stream == null) return null;
        Image image = null;
        try
        {
            image =new Image(input_stream);
        }
        catch (OutOfMemoryError e)
        {
            logger.log("OutOfMemoryError when loading image from disk: "+original_image_file.toAbsolutePath()+" : "+e);
            Popups.popup_Exception(null,100,"Your java VM machine is running out of RAM!\nclose some windows and/or try to increase the max in build.gradle.works and restart",owner,logger);
            return null;
        }
        catch (Exception e)
        {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
            Popups.popup_Exception(e,100,"An error occurred while loading an image from disk",owner,logger);
            return null;
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
                logger.log("IMAGE decode Panic :"+image.getException());
                Popups.popup_Exception(image.getException(),100,"Your java VM machine is running out of RAM, try to increase the max in build.gradle.works and restart",owner,logger);
            }
            else if( image.getException().toString().contains("No loader for image data"))
            {
                logger.log("IMAGE decode failed :"+image.getException());
                // this occurs on damaged images like download not finished, or fusk wrong pin code
                // Popups.popup_Exception(image.getException(),100,"If this image was fusked, maybe the pin code is wrong?",logger);
            }
            else
            {
                logger.log("IMAGE ERROR :"+original_image_file.toAbsolutePath()+" : "+image.getException());
            }
        }
        return image;

    }

    //private static boolean use_ImageIO = false;
    // this call RESIZES to the target icon size

    //private static long elapsed_read_original_image_from_disk_and_return_icon =0;
    //**********************************************************
    public static Image read_original_image_from_disk_and_return_icon(Path original_image_file, double icon_size,  boolean report_if_not_found, Aborter aborter, Logger logger)
    //**********************************************************
    {
        //long start = System.currentTimeMillis();
        Image image = null;
        try(InputStream input_stream = get_image_InputStream(original_image_file, Feature_cache.get(Feature.Fusk_is_on), report_if_not_found, aborter,logger))
        {
            if ( input_stream == null)
            {
                logger.log(Stack_trace_getter.get_stack_trace("input_stream == null for"+original_image_file));
                return null;
            }
            if ( aborter.should_abort())
            {
                if ( dbg) logger.log("read_original_image_from_disk_and_return_icon aborted");
                return null;
            }
            /*
            this code uses AWT, which is not supported by gluon
            if ( use_ImageIO)
            {
                //logger.log("using ImageIO");
                BufferedImage ii = ImageIO.read(input_stream);
                input_stream.close();
                if (ii == null)
                {
                    logger.log("ImageIO.read returned null for "+original_image_file);
                    return null;
                }
                AffineTransform trans = new AffineTransform();
                int target_width = (int)icon_size;
                int target_height = (int)icon_size;
                double s = 1.0;
                if(ii.getHeight()>ii.getWidth())
                {
                    s = (double) target_height / ii.getHeight();
                    target_width = (int) (ii.getWidth() * s);
                }
                else
                {
                    s = (double) target_width / ii.getWidth();
                    target_height = (int) (ii.getHeight() * s);
                }
                trans.scale(s, s);

                BufferedImage sink_bi = new BufferedImage(target_width,target_height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g_for_returned_image = sink_bi.createGraphics();

                g_for_returned_image.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g_for_returned_image.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g_for_returned_image.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
                g_for_returned_image.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g_for_returned_image.drawRenderedImage(ii, trans);
                image = JavaFX_to_Swing.toFXImage(sink_bi,null);
            }
            else*/
            {
                //logger.log("using javafx Image");
                image = new Image(input_stream, icon_size, icon_size, true, true);
                if ( image.isError())
                {
                    //if ( dbg)
                        logger.log("From_disk WARNING: an error occurred when reading: "+original_image_file.toAbsolutePath());
                   image = null;
                }
            }
        }
        catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
        }

        //long now = System.currentTimeMillis();
        //elapsed_read_original_image_from_disk_and_return_icon += now-start;
        //logger.log("elapsed_read_original_image_from_disk_and_return_icon:"+elapsed_read_original_image_from_disk_and_return_icon);
        return image;
    }

    //**********************************************************
    public static long get_remaining_memory()
    //**********************************************************
    {
        //https://stackoverflow.com/questions/3571203/what-are-runtime-getruntime-totalmemory-and-freememory
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        long max = runtime.maxMemory();
        long remaining = max - used;
        return remaining;
    }

    //**********************************************************
    public static File file_for_icon_caching(Path cache_dir, Path original_image_file, String tag, String extension)
    //**********************************************************
    {
        if ( original_image_file == null) return null;
        return new File(cache_dir.toFile(), Icon_writer_actor.make_cache_name(original_image_file.toAbsolutePath().toString(), tag, extension));
    }

    //**********************************************************
    public static Image load_icon_from_disk_cache(
            Path original_image_file, // this is NOT the ICON path, this is the true full size image
            Path cache_dir,
            int icon_size,
            String tag,
            String extension,
            boolean dbg_local,
            Window owner,
            Logger logger)
    //**********************************************************
    {
        if (get_remaining_memory() < MIN_REMAINING_FREE_MEMORY_10MB)
        {
            logger.log("load_icon_from_cache_fx WARNING: running low on memory ! loading default icon");
            return Look_and_feel_manager.get_default_icon(icon_size,owner,logger);
        }
        File f = file_for_icon_caching(cache_dir,original_image_file,tag, extension);
        if (dbg) logger.log("load_icon_from_disk file is:"+f.getAbsolutePath()+" for "+original_image_file);
        try (FileInputStream input_stream = new FileInputStream(f))
        {
            Image image = new Image(input_stream);
            return image;
        }
        catch (FileNotFoundException e) {
            // this happens the first time one visits a directory...
            // or when the icon cache dir content has been erased etc.
            // so quite a lot, so it is logged only in debug
            if (dbg_local)
                logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
        }
        return null;
    }
}
