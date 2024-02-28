package klik.level3.experimental;

//import javafx.embed.swing.SwingFXUtils;

import javafx.scene.image.WritableImage;
import klik.actor.Aborter;
import klik.browser.icons.JavaFX_to_Swing;
import klik.images.Image_context;
import klik.look.Look_and_feel_manager;
import klik.util.From_disk;
import klik.util.Logger;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;


//**********************************************************
public class Static_image_utilities
//**********************************************************
{

    private static final boolean dbg = false;

    //**********************************************************
    public static Image_context get_Image_context_with_alternate_rescaler(Path path_, int width, Aborter aborter,Logger logger_)
    //**********************************************************
    {
        if (!Files.exists(path_)) return null;
        javafx.scene.image.Image local_image = From_disk.load_native_resolution_image_from_disk(path_, true, aborter, logger_);
        if ( local_image == null) return null;
        if ( local_image.isError())
        {
            javafx.scene.image.Image broken = Look_and_feel_manager.get_broken_icon(300);

            new Image_context(path_,path_, broken,logger_);
        }

        WritableImage resized_image = Static_image_utilities.transform_with_alternate_rescaler(local_image,width,true,logger_);
        return new Image_context(path_,path_,resized_image,logger_);
    }
    //**********************************************************
    public static WritableImage transform_with_alternate_rescaler(
            javafx.scene.image.Image in,
            int target_width,
            boolean quality_bool,
            Logger logger)
    //**********************************************************
    {
        double source_image_width = in.getWidth();
        double source_image_height = in.getHeight();

        AffineTransform trans = new AffineTransform();
        double s = (double) target_width / source_image_width;

        trans.scale(s, s);

        BufferedImage sink_bi = new BufferedImage(target_width,(int)(source_image_height*s), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g_for_returned_image = sink_bi.createGraphics();

        String quality = null;
        if (quality_bool) {
            System.out.println("QUALITY is on");
            quality = "Quality ";
            g_for_returned_image.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            g_for_returned_image.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g_for_returned_image.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING,
                    RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        } else {
            quality = "Speed ";
            g_for_returned_image.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_SPEED);
            g_for_returned_image.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g_for_returned_image.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING,
                    RenderingHints.VALUE_COLOR_RENDER_SPEED);
        }

        BufferedImage source_bi = JavaFX_to_Swing.fromFXImage(in, null,logger);
        g_for_returned_image.drawRenderedImage(source_bi, trans);
        WritableImage out = JavaFX_to_Swing.toFXImage(sink_bi,null);
        return out;
    }

    //**********************************************************
    public static boolean transform(
            Image_context ic1,
            BufferedImage source_bi,
            int display_area_width,
            int display_area_height,
            Graphics2D g_for_returned_image,
            boolean quality_bool,
            Logger logger)
    //**********************************************************
    {
        int source_image_width = source_bi.getWidth();
        int source_image_height = source_bi.getHeight();

        int W = source_image_width;
        int H = source_image_height;

        double s = 1.0;
        AffineTransform trans = new AffineTransform();

        double zoom = 1.0;

        // now we compute WHERE the image will land into the panel
        // 1) we put it in the middle
        int target_x = 0;
        int target_y = 0;

        {
            s = Static_image_utilities.compute_scale(display_area_width, display_area_height, source_image_width, source_image_height, zoom);
            trans.scale(s, s);
            // it causes a change in image size
            W = (int) ((double) source_image_width * s);
            H = (int) ((double) source_image_height * s);

            target_x = (display_area_width - W) / 2;
            target_y = (display_area_height - H) / 2;
        }
        int scroll_x = 0;
        int x = (int) ((double) (target_x + scroll_x) / s);
        int scroll_y = 0;
        int y = (int) ((double) (target_y + scroll_y) / s);

        {
            if (dbg == true) logger.log("Doing the painting, rotated 0");
            trans.translate(x, y);
        }
        if (dbg) logger.log("...drawImage");



        /*
         * The RENDERING hint is a general hint that provides a high level recommendation
         * as to whether to bias algorithm choices more for speed or quality when
         * evaluating tradeoffs. This hint could be consulted for any rendering
         * or image manipulation operation, but decisions will usually honor other,
         * more specific hints in preference to this hint.
         */

        /*
         * The INTERPOLATION hint controls how image pixels are filtered or resampled
         * during an image rendering operation.
         * Implicitly images are defined to provide color samples at integer coordinate
         * locations. When images are rendered upright with no scaling onto a destination,
         * the choice of which image pixels map to which device pixels is obvious and the
         * samples at the integer coordinate locations in the image are transfered to the
         * pixels at the corresponding integer locations on the device pixel grid one for
         * one. When images are rendered in a scaled, rotated, or otherwise transformed
         * coordinate system, then the mapping of device pixel coordinates back to the
         * image can raise the question of what color sample to use for the continuous
         * coordinates that lie between the integer locations of the provided image
         * samples. Interpolation algorithms define functions which provide a color
         * sample for any continuous coordinate in an image based on the color samples
         * at the surrounding integer coordinates.
         */

        String quality = null;
        if (quality_bool) {
            quality = "Quality ";
            g_for_returned_image.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            g_for_returned_image.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g_for_returned_image.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING,
                    RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        } else {
            quality = "Speed ";
            g_for_returned_image.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_SPEED);
            g_for_returned_image.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g_for_returned_image.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING,
                    RenderingHints.VALUE_COLOR_RENDER_SPEED);
        }

        //try
        {
            g_for_returned_image.drawRenderedImage(source_bi, trans);
        }
		/*catch (OutOfMemoryError e)
		{
			Icon_maker.logger.log("OutOfMemoryError in drawRenderedImage() 1 going to clear image cache");
			Image_cache.clear_all(true);
			return false;
		}
		*/
        return true;
    }

    /*
    // reads the exif data and DRAWS a properly rotated and scaled
    // copy of the image
    // uses the rotation extracted from EXIF
    // as well as the requested ZOOM & SCROLL
    //**********************************************************
    public static boolean transform2(
            Image_context ic,
            BufferedImage bi,
            int display_area_width,
            int display_area_height,
            Graphics2D g_for_returned_image,
            boolean quality_bool,
            Logger logger)
    //**********************************************************
    {
        int source_image_width = bi.getWidth();
        int source_image_height = bi.getHeight();
        //double scalex = (double)display_area_width/(double)source_image_width;
        //double scaley = (double)display_area_height/(double)source_image_height;

        {
            if (
                    (display_area_height > source_image_height) &&
                            (display_area_width > source_image_width)) {
                ic.set_pix_for_pix(true);
            }
        }

        int W = source_image_width;
        int H = source_image_height;

        double s = 1.0;
        AffineTransform trans = new AffineTransform();

        double zoom = ic.get_zoom_factor();

        // now we compute WHERE the image will land into the panel
        // 1) we put it in the middle
        int target_x = 0;
        int target_y = 0;
        if ((ic.get_rotation() == 90) || (ic.get_rotation() == 270)) {

            s = Static_image_utilities.compute_scale(display_area_width, display_area_height, source_image_height, source_image_width, zoom);
            if (ic.get_pix_for_pix() == true) {
                s = 1.0;
            }
            trans.scale(s, s);
            // it causes a change in image size
            W = (int) ((double) source_image_width * s);
            H = (int) ((double) source_image_height * s);

            target_x = (display_area_width - H) / 2;
            target_y = (display_area_height - W) / 2;
        } else {
            s = Static_image_utilities.compute_scale(display_area_width, display_area_height, source_image_width, source_image_height, zoom);
            if (ic.get_pix_for_pix() == true) {
                s = 1.0;
            }
            trans.scale(s, s);
            // it causes a change in image size
            W = (int) ((double) source_image_width * s);
            H = (int) ((double) source_image_height * s);

            target_x = (display_area_width - W) / 2;
            target_y = (display_area_height - H) / 2;
        }
        double scroll_x = ic.get_scroll_x();
        int x = (int) ((double) (target_x + scroll_x) / s);
        double scroll_y = ic.get_scroll_y();
        int y = (int) ((double) (target_y + scroll_y) / s);
        if (ic.get_rotation() == 90) {
            if (dbg == true) logger.log("Doing the painting, rotated 90");
            trans.rotate(Math.toRadians(90));
            trans.translate(y, -x - source_image_height);
        } else if (ic.get_rotation() == 180) {
            if (dbg == true) logger.log("Doing the painting, rotated 180");
            // also works to do this trans BEFORE rotate
            //trans.translate(x+w,y+h);
            trans.rotate(Math.toRadians(180));
            trans.translate(-x - source_image_width, -y - source_image_height);
        } else if (ic.get_rotation() == 270) {
            if (dbg == true) logger.log("Doing the painting, rotated 270");
            trans.rotate(Math.toRadians(270));
            trans.translate(-y - source_image_width, x);
        } else {
            if (dbg == true) logger.log("Doing the painting, rotated 0");
            trans.translate(x, y);
        }
        if (dbg) logger.log("...drawImage");

*/

        /*
         * The RENDERING hint is a general hint that provides a high level recommendation
         * as to whether to bias algorithm choices more for speed or quality when
         * evaluating tradeoffs. This hint could be consulted for any rendering
         * or image manipulation operation, but decisions will usually honor other,
         * more specific hints in preference to this hint.
         */

        /*
         * The INTERPOLATION hint controls how image pixels are filtered or resampled
         * during an image rendering operation.
         * Implicitly images are defined to provide color samples at integer coordinate
         * locations. When images are rendered upright with no scaling onto a destination,
         * the choice of which image pixels map to which device pixels is obvious and the
         * samples at the integer coordinate locations in the image are transfered to the
         * pixels at the corresponding integer locations on the device pixel grid one for
         * one. When images are rendered in a scaled, rotated, or otherwise transformed
         * coordinate system, then the mapping of device pixel coordinates back to the
         * image can raise the question of what color sample to use for the continuous
         * coordinates that lie between the integer locations of the provided image
         * samples. Interpolation algorithms define functions which provide a color
         * sample for any continuous coordinate in an image based on the color samples
         * at the surrounding integer coordinates.
         */
/*
        String quality = null;
        if (quality_bool) {
            quality = "Quality ";
            g_for_returned_image.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            g_for_returned_image.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g_for_returned_image.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING,
                    RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        } else {
            quality = "Speed ";
            g_for_returned_image.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_SPEED);
            g_for_returned_image.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g_for_returned_image.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING,
                    RenderingHints.VALUE_COLOR_RENDER_SPEED);
        }

        long start = System.nanoTime();
        {
            g_for_returned_image.drawRenderedImage(bi, trans);
        }
        long end = System.nanoTime();
        long delta = end - start;
        if (delta > 1000000) {
            quality += (end - start) / 1000000 + "ms ";
        } else if (delta > 1000) {
            quality += (end - start) / 1000 + "us ";
        }

        ic.set_quality(quality);
        return true;
    }
*/
    //**********************************************************
    public static double compute_scale(
            int display_area_width,
            int display_area_height,
            int source_image_width,
            int source_image_height,
            double zoom)
    //**********************************************************
    {

        double sx = (double) display_area_width / (double) source_image_width;
        double sy = (double) display_area_height / (double) source_image_height;
        double s;
        if (sx < sy) s = sx;
        else s = sy;
        s = s * zoom;

        return s;
    }



}
