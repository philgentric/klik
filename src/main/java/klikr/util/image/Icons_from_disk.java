// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ../../images/decoding/Fast_aspect_ratio_from_exif_metadata_extractor.java
//SOURCES ../../images/decoding/Fast_width_from_exif_metadata_extractor.java
//SOURCES ../../experimental/fusk/Fusk_static_core.java

package klikr.util.image;

import javafx.stage.Window;
import klikr.browser.icons.Icon_factory_request;
import klikr.browser.icons.Icon_writer_actor;
import klikr.util.External_application;
import klikr.util.execute.actor.Aborter;
import klikr.browser.items.Iconifiable_item_type;
import klikr.look.Jar_utils;
import klikr.look.Look_and_feel_manager;

import javafx.scene.image.Image;
import klikr.properties.boolean_features.Feature;
import klikr.properties.boolean_features.Feature_cache;
import klikr.util.Check_remaining_RAM;
import klikr.util.execute.Execute_command;
//import klik.util.image.decoding.FITS;
import klikr.util.image.icon_cache.Icon_caching;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;
import klikr.util.mmap.Mmap;

//import javax.imageio.ImageIO;
//import java.awt.*;
//import java.awt.geom.AffineTransform;
//import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

//static utilities for loading images and icons from the disk
//**********************************************************
public class Icons_from_disk
// **********************************************************
{
    public static final boolean dbg = false;

    // private static boolean use_ImageIO = false;
    // this call RESIZES to the target icon size

    // private static long elapsed_read_original_image_from_disk_and_return_icon =0;
    // **********************************************************
    public static Optional<Image> read_original_image_from_disk_and_return_icon(
            Path original_image_file,
            Iconifiable_item_type item_type,
            double icon_size,
            boolean report_if_not_found,
            Aborter aborter, Window owner, Logger logger)
    // **********************************************************
    {
        // logger.log("read_original_image_from_disk_and_return_icon");

        if (Check_remaining_RAM.RAM_running_low("icon creation",owner,logger)) {

            logger.log("read_original_image_from_disk_and_return_icon NOT DONE because running low on memory ! ");
            return Optional.of(Jar_utils.get_broken_icon(300, owner, logger));
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
                return use_GraphicsMagick_for_icon(original_image_file, icon_size, owner, logger);
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
                return Optional.empty();
            }
            if (aborter.should_abort()) {
                if (dbg)
                    logger.log("read_original_image_from_disk_and_return_icon aborted");
                return Optional.empty();
            }

            return use_javafx_Image(input_stream, icon_size, logger);
            /*
             * switch ( item_type)
             * {
             * case javafx_image_not_gif_not_png, image_png, image_gif -> {
             * logger.log("using javafx for "+ item_type+ " "+original_image_file);
             * return use_javafx_Image(input_stream,icon_size,logger);
             * }
             * default -> {
             * logger.log(Stack_trace_getter.
             * get_stack_trace("Icons_from_disk WARNING: unexpected item_type "+ item_type+
             * " for "+original_image_file));
             * return Optional.empty();
             * }
             * }
             */
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
        }

        // long now = System.currentTimeMillis();
        // elapsed_read_original_image_from_disk_and_return_icon += now-start;
        // logger.log("elapsed_read_original_image_from_disk_and_return_icon:"+elapsed_read_original_image_from_disk_and_return_icon);
        return Optional.of(image);
    }

    // **********************************************************
    private static Optional<Image> use_GraphicsMagick_for_icon(Path original_image_file, double icon_size, Window owner,
            Logger logger)
    // **********************************************************
    {
        logger.log("use_GraphicsMagick_for_icon");

        String tag = String.valueOf((int) icon_size);
        Path png_path = Icon_caching.path_for_icon_caching(original_image_file, tag, Icon_caching.png_extension, owner,
                logger);

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
            return Optional.of(new Image(is, icon_size, icon_size, true, true));
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
            // GraphicsMagick failed, let us try the same with imageMagick
            return use_ImageMagick_for_icon(original_image_file, icon_size, owner, logger);
        }
    }

    // **********************************************************
    private static Optional<Image> use_ImageMagick_for_icon(Path original_image_file, double icon_size, Window owner,
            Logger logger)
    // **********************************************************
    {
        logger.log("use_ImageMagick_for_icon");

        String tag = String.valueOf((int) icon_size);
        Path png_path = Icon_caching.path_for_icon_caching(original_image_file, tag, Icon_caching.png_extension, owner,
                logger);

        // String command_string_to_create_tmp_icon = "magick
        // "+original_image_file.toAbsolutePath()+ " "+ png_path.toAbsolutePath();
        // Execute_via_script_in_tmp_file.execute(command_string_to_create_tmp_icon,
        // false, owner,logger);
        List<String> list = List.of(External_application.ImageMagick.get_command(owner,logger), original_image_file.toAbsolutePath().toString(),
                png_path.toAbsolutePath().toString());
        Execute_command.execute_command_list(list, new File("."), 20_000, null, logger);

        try (InputStream is = new FileInputStream(png_path.toFile())) {
            // use the javafx Image constructor that resizes while loading
            return Optional.of(new Image(is, icon_size, icon_size, true, true));
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
        }
        return Optional.empty();
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
     * return Optional.empty();
     * }
     * return Optional.of(x.get());
     * }
     */
    // **********************************************************
    private static Optional<Image> use_javafx_Image(InputStream input_stream, double icon_size, Logger logger)
    // **********************************************************
    {
        // logger.log("use_javafx_Image");
        Image image = new Image(input_stream, icon_size, icon_size, true, true);
        if (image.isError()) {
            logger.log(("Icons_from_disk WARNING: an error occurred when reading AND resizing: "));
            return Optional.empty();

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
        return Optional.of(image);

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
    public static Image get_image_from_cache(
            Path path,
            int icon_size,
            Window owner,
            Logger logger)
    // **********************************************************
    {
        Image image_from_cache;
        if ( Icon_writer_actor.use_mmap)
        {
            image_from_cache = Mmap.instance.read_image(
                    Icon_caching.tag_for_icon_caching(
                            path,
                            String.valueOf(icon_size)
                    ));
        }
        else
        {
            image_from_cache = load_icon_from_disk_cache(
                    path,
                    icon_size,
                    String.valueOf(icon_size),
                    Icon_caching.png_extension,
                    false, owner, logger);
        }
        return image_from_cache;
    }


    // **********************************************************
    public static Image load_icon_from_disk_cache(
            Path original_image_file, // this is NOT the ICON path, this is the true full size image
            int icon_size,
            String tag, // icon size or empty
            String extension,
            boolean dbg_local,
            Window owner,
            Logger logger)
    // **********************************************************
    {
        // logger.log("load_icon_from_disk_cache");

        if (Check_remaining_RAM.RAM_running_low("icon from disk",owner,logger)) {
            logger.log("load_icon_from_disk_cache WARNING: running low on memory ! loading default icon");
            return Look_and_feel_manager.get_default_icon(icon_size, owner, logger);
        }
        Path path = Icon_caching.path_for_icon_caching(original_image_file, tag, extension, owner, logger);
        if (dbg)
            logger.log("load_icon_from_disk file is:" + path.toAbsolutePath() + " for " + original_image_file);
        try (InputStream input_stream = Files.newInputStream(path)) {
            Image image = new Image(input_stream);
            return image;
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
}
