// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ../../images/decoding/Fast_aspect_ratio_from_exif_metadata_extractor.java
//SOURCES ../../images/decoding/Fast_width_from_exif_metadata_extractor.java
//SOURCES ../../experimental/fusk/Fusk_static_core.java

package klikr.util.image;

import javafx.stage.Window;
import klikr.browser_core.Image_and_properties;
import klikr.browser_core.icons.image_properties_cache.Image_properties;
import klikr.browser_core.icons.image_properties_cache.Rotation;
import klikr.util.External_application;
import klikr.util.execute.actor.Aborter;
import klikr.browser_core.items.Iconifiable_item_type;
import klikr.look.Jar_utils;
import klikr.look.Look_and_feel_manager;

import javafx.scene.image.Image;
import klikr.settings.boolean_features.Feature;
import klikr.settings.boolean_features.Feature_cache;
import klikr.util.Check_remaining_RAM;
import klikr.util.execute.Execute_command;
//import klik.util.image.decoding.FITS;
import klikr.util.image.decoding.Fast_image_property_from_exif_metadata_extractor;
import klikr.util.image.decoding.Fast_rotation_from_exif_metadata_extractor;
import klikr.util.image.icon_cache.Icon_caching;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;

//import javax.imageio.ImageIO;
//import java.awt.*;
//import java.awt.geom.AffineTransform;
//import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;

//static utilities for loading images and icons from the disk
//**********************************************************
public class Icons_from_disk
// **********************************************************
{
    public static final boolean dbg = false;

    // private static boolean use_ImageIO = false;
    // this call RESIZES to the target icon length

    // private static long elapsed_read_original_image_from_disk_and_return_icon =0;
    // **********************************************************
    public static Image_and_properties read_original_image_from_disk_and_return_icon(
            Path original_image_file,
            Iconifiable_item_type item_type,
            double icon_size,
            boolean report_if_not_found,
            Window owner, Aborter aborter, Logger logger)
    // **********************************************************
    {
        // logger.log("read_original_image_from_disk_and_return_icon");

        if (Check_remaining_RAM.RAM_running_low("icon creation",owner,logger)) {

            logger.log("read_original_image_from_disk_and_return_icon NOT DONE because running low on memory ! ");
            Image i = Jar_utils.get_broken_icon(icon_size, owner, logger);
            if (i == null) return null;
            return Image_and_properties.build(i,true);
        }

        switch (item_type) {
            /*
             * we use GraphicsMagick for FITS images now
             * case image_fits -> {
             * logger.log("using FITS for "+ item_type+ " "+original_image_file);
             * return use_fits_NASA(original_image_file,icon_size,aborter,owner, logger);
             * }
             */
            case non_javafx_image -> {
                logger.log("using NON-javafx for " + item_type + " " + original_image_file);
                Image i =  use_GraphicsMagick_for_icon(original_image_file, icon_size, owner, logger);
                if (i == null) return null;
                return Image_and_properties.build(i,false);
            }
            default -> {
            }
        }

        // long start = System.currentTimeMillis();
        Image image = null;
        try (InputStream input_stream = Full_image_from_disk.get_image_InputStream(original_image_file,
                Feature_cache.get(Feature.Fusk_is_on), report_if_not_found, aborter, logger)) {
            if (input_stream == null) {
                logger.log(Stack_trace_getter.get_stack_trace("input_stream == null for" + original_image_file));
                return null;
            }
            if (aborter.should_abort()) {
                if (dbg)
                    logger.log("read_original_image_from_disk_and_return_icon aborted");
                return null;
            }

            byte[] bytes = input_stream.readAllBytes();
            return use_javafx_Image(bytes, original_image_file,icon_size, logger);
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
        }

        // long now = System.currentTimeMillis();
        // elapsed_read_original_image_from_disk_and_return_icon += now-start;
        // logger.log("elapsed_read_original_image_from_disk_and_return_icon:"+elapsed_read_original_image_from_disk_and_return_icon);
        return null;
    }

    // **********************************************************
    private static Image use_GraphicsMagick_for_icon(Path original_image_file, double icon_size, Window owner,
            Logger logger)
    // **********************************************************
    {
        logger.log("use_GraphicsMagick_for_icon");

        String tag = String.valueOf((int) icon_size);
        Path png_path = Icon_caching.path_for_icon_caching(original_image_file, tag, Icon_caching.png_extension, owner, logger);
        if ( png_path == null ) return null;

        // String command_string_to_create_tmp_icon = "gm convert
        // "+original_image_file.toAbsolutePath()+ " "+ png_path.toAbsolutePath();
        // Execute_via_script_in_tmp_file.execute(command_string_to_create_tmp_icon,
        // false, owner, logger);
        List<String> list = List.of(
                External_application.GraphicsMagick.get_command(owner,logger),
                "convert", original_image_file.toAbsolutePath().toString(),
                png_path.toAbsolutePath().toString());
        Execute_command.execute_command_list(list, new File("."), 20_000, null, logger);

        try (InputStream is = new FileInputStream(png_path.toFile())) {
            // use the javafx Image constructor that resizes while loading
            return new Image(is, icon_size, icon_size, true, true);
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
            // GraphicsMagick failed, let us try the same with imageMagick
            return use_ImageMagick_for_icon(original_image_file, icon_size, owner, logger);
        }
    }

    // **********************************************************
    private static Image use_ImageMagick_for_icon(Path original_image_file, double icon_size, Window owner,
            Logger logger)
    // **********************************************************
    {
        logger.log("use_ImageMagick_for_icon");

        String tag = String.valueOf((int) icon_size);
        Path png_path = Icon_caching.path_for_icon_caching(original_image_file, tag, Icon_caching.png_extension, owner, logger);
        if ( png_path == null ) return null;

        // String command_string_to_create_tmp_icon = "magick
        // "+original_image_file.toAbsolutePath()+ " "+ png_path.toAbsolutePath();
        // Execute_via_script_in_tmp_file.execute(command_string_to_create_tmp_icon,
        // false, owner,logger);
        List<String> list = List.of(External_application.ImageMagick.get_command(owner,logger), original_image_file.toAbsolutePath().toString(),
                png_path.toAbsolutePath().toString());
        Execute_command.execute_command_list(list, new File("."), 20_000, null, logger);

        try (InputStream is = new FileInputStream(png_path.toFile())) {
            // use the javafx Image constructor that resizes while loading
            return new Image(is, icon_size, icon_size, true, true);
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
        }
        return null;
    }

    /*
     * //**********************************************************
     * private static Optional<Image> use_fits_NASA(Path original_image_file, double
     * icon_size, Aborter aborter, Window owner, Logger logger)
     * //**********************************************************
     * {
     * logger.log("use_fits_NASA");
     * 
     * Optional<Image> op = FITS.load_FITS_image(original_image_file, aborter,
     * owner,logger);
     * if (op.isEmpty()) return op;
     * 
     * // make an icon
     * ImageView iv = new ImageView(op.get());
     * iv.setFitWidth(icon_size);
     * iv.setFitHeight(icon_size);
     * iv.setPreserveRatio(true);
     * iv.setSmooth(true);
     * 
     * SnapshotParameters params = new SnapshotParameters();
     * params.setFill(Color.TRANSPARENT);
     * CountDownLatch cdl = new CountDownLatch(1);
     * AtomicReference<WritableImage> x = new AtomicReference<>();
     * Platform.runLater(()->{
     * x.set(iv.snapshot(params, null));
     * cdl.countDown();
     * });
     * try {
     * cdl.await();
     * } catch (InterruptedException e) {
     * logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
     * return null;
     * }
     * return Optional.of(x.get());
     * }
     */
    // **********************************************************
    private static Image_and_properties use_javafx_Image(byte[] bytes, Path path_for_dbg, double icon_size, Logger logger)
    // **********************************************************
    {
        // logger.log("use_javafx_Image");
        InputStream is1 = new ByteArrayInputStream(bytes);
        Image image = new Image(is1, icon_size, icon_size, true, true);
        if (image.isError()) {
            logger.log(("Icons_from_disk WARNING: an error occurred when reading AND resizing: "));
            return null;

            // the image format is not supported WITH RESIZE
            // but it may be supported WITHOUT rise e.g. TIF
            // if ( dbg)
            // logger.log(Stack_trace_getter.get_stack_trace("Icons_from_disk WARNING: an
            // error occurred when reading AND resizing:
            // "+original_image_file.toAbsolutePath()));
            // image =
            // Icons_from_disk.load_native_resolution_image_from_disk(original_image_file,
            // true, null, aborter,logger);
        }

        // find out if the image is rotated
        InputStream is2 = new ByteArrayInputStream(bytes);
        Rotation rot = Fast_rotation_from_exif_metadata_extractor.get_rotation_from_InputStream(is2,path_for_dbg,logger);
        if ( rot == null )
        {
            logger.log(Stack_trace_getter.get_stack_trace(" WARNING rotation not found for "+path_for_dbg));
            rot = Rotation.normal;
        }
        return new Image_and_properties(image,new Image_properties(image.getWidth(),image.getHeight(),rot,false));

        /*
         * this code uses AWT, which is not supported by gluon
         * if ( use_ImageIO)
         * {
         * //logger.log("using ImageIO");
         * BufferedImage ii = ImageIO.read(input_stream);
         * input_stream.close();
         * if (ii == null)
         * {
         * logger.log("ImageIO.read returned null for "+original_image_file);
         * return null;
         * }
         * AffineTransform trans = new AffineTransform();
         * int target_width = (int)icon_size;
         * int target_height = (int)icon_size;
         * double s = 1.0;
         * if(ii.getHeight()>ii.getWidth())
         * {
         * s = (double) target_height / ii.getHeight();
         * target_width = (int) (ii.getWidth() * s);
         * }
         * else
         * {
         * s = (double) target_width / ii.getWidth();
         * target_height = (int) (ii.getHeight() * s);
         * }
         * trans.scale(s, s);
         * 
         * BufferedImage sink_bi = new BufferedImage(target_width,target_height,
         * BufferedImage.TYPE_INT_ARGB);
         * Graphics2D g_for_returned_image = sink_bi.createGraphics();
         * 
         * g_for_returned_image.setRenderingHint(RenderingHints.KEY_RENDERING,
         * RenderingHints.VALUE_RENDER_QUALITY);
         * g_for_returned_image.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
         * RenderingHints.VALUE_INTERPOLATION_BICUBIC);
         * g_for_returned_image.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING,
         * RenderingHints.VALUE_COLOR_RENDER_QUALITY);
         * g_for_returned_image.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
         * RenderingHints.VALUE_ANTIALIAS_ON);
         * 
         * g_for_returned_image.drawRenderedImage(ii, trans);
         * image = JavaFX_to_Swing.toFXImage(sink_bi,null);
         * }
         * 
         */
    }




    // **********************************************************
    public static Image_and_properties load_icon_from_disk_cache(
            Path original_image_file, // this is NOT the ICON path, this is the true full length image
            int icon_size, // used for the NAME (not for resizing)
            String tag, // icon length or empty
            String extension,
            boolean dbg_local,
            Window owner,
            Logger logger)
    // **********************************************************
    {
        // logger.log("load_icon_from_disk_cache");

        if (Check_remaining_RAM.RAM_running_low("icon from disk",owner,logger)) {
            logger.log("load_icon_from_disk_cache WARNING: running low on memory ! loading default icon");
            Image i = Look_and_feel_manager.get_default_icon(icon_size, owner, logger);
            return Image_and_properties.build(i,false);
        }
        Path path = Icon_caching.path_for_icon_caching(original_image_file, tag, extension, owner, logger);
        if ( path == null ) return null;
        if (dbg)
            logger.log("load_icon_from_disk file is:" + path.toAbsolutePath() + " for " + original_image_file);
        try (InputStream input_stream = Files.newInputStream(path))
        {
            Image i = new Image(input_stream);
            return Image_and_properties.build(i,false);
        } catch (FileNotFoundException e) {
            // this happens the first time one visits a directory...
            // or when the icon cache dir content has been erased etc.
            // so quite a lot, so it is logged only in debug
            if (dbg_local)
                logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
        } catch (NoSuchFileException e) {
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

    // only for icons i.e. NOT general purpose, which requires fusk support
    // **********************************************************
    public static Image load_icon(Path path, Logger logger)
    // **********************************************************
    {
        try (InputStream input_stream = Files.newInputStream(path)) {
            Image image = new Image(input_stream);
            return image;
        } catch (FileNotFoundException e) {
            // this happens the first time one visits a directory...
            // or when the icon cache dir content has been erased etc.
            // so quite a lot, so it is logged only in debug
            if (dbg)
                logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
        } catch (NoSuchFileException e) {
            // this happens the first time one visits a directory...
            // or when the icon cache dir content has been erased etc.
            // so quite a lot, so it is logged only in debug
            if (dbg)
                logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
        }
        return null;
    }

    // **********************************************************
    public static Image load_icon2(
            Path path,
            int size,
            Logger logger)
    // **********************************************************
    {
        try (InputStream input_stream = Files.newInputStream(path)) {
            Image image = new Image(input_stream,size,size,true,true);
            return image;
        } catch (FileNotFoundException e) {
            // this happens the first time one visits a directory...
            // or when the icon cache dir content has been erased etc.
            // so quite a lot, so it is logged only in debug
            if (dbg)
                logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
        } catch (NoSuchFileException e) {
            // this happens the first time one visits a directory...
            // or when the icon cache dir content has been erased etc.
            // so quite a lot, so it is logged only in debug
            if (dbg)
                logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
        }
        return null;
    }
}
