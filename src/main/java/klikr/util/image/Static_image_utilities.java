// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.util.image;


import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.ImageLineByte;
import ar.com.hjg.pngj.PngWriter;
import javafx.scene.image.*;
import javafx.stage.Window;
import klikr.util.execute.actor.Aborter;
import klikr.images.Image_context;
import klikr.look.Jar_utils;
import klikr.util.image.rescaling.Image_rescaling_filter;
import klikr.util.image.rescaling.Vips_utils;
import klikr.util.log.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;


//**********************************************************
public class Static_image_utilities
//**********************************************************
{

    private static final boolean dbg = false;

    //**********************************************************
    public static void write_png_to_disk(Image image,
                                         Path out_path,
                                         Logger logger)
    //**********************************************************
    {

        int w = (int) image.getWidth();
        int h = (int) image.getHeight();
        PixelReader pr = image.getPixelReader();
        if (pr == null) throw new IllegalArgumentException("Image has no pixels");

        ImageInfo image_info = new ImageInfo(w, h, 8, true); // 8-bit RGBA

        try (OutputStream os = Files.newOutputStream(out_path)){

            PngWriter png = new PngWriter(os, image_info);
            ImageLineByte line = new ImageLineByte(image_info);
            byte[] scan = line.getScanlineByte();

            for (int y = 0; y < h; y++) {
                int idx = 0;
                for (int x = 0; x < w; x++) {
                    int argb = pr.getArgb(x, y);
                    // PNG expects RGBA
                    scan[idx++] = (byte) ((argb >> 16) & 0xFF); // R
                    scan[idx++] = (byte) ((argb >> 8) & 0xFF); // G
                    scan[idx++] = (byte) (argb & 0xFF); // B
                    scan[idx++] = (byte) ((argb >> 24) & 0xFF); // A
                }
                png.writeRow(line, y);
            }
            png.end();
        }
        catch (IOException e)
        {
            logger.log("Icon_writer_actor: Error writing icon to cache: " + e.getMessage());
        }
        if (dbg) logger.log("Icon_writer_actor: Icon written to cache: ");
    }


    //**********************************************************
    public static Optional<Image_context> get_Image_context_with_alternate_rescaler(
            Path path_,
            double window_width,
            double window_height,
            Image_rescaling_filter filter,
            Window owner, Aborter aborter, Logger logger_)
    //**********************************************************
    {
        if (!Files.exists(path_)) return Optional.empty();
        Optional<Image> op = Full_image_from_disk.load_native_resolution_image_from_disk(path_, true, owner, aborter, logger_);
        if (op.isEmpty()) return Optional.empty();
        Image local_image = op.get();
        if (local_image.isError()) {
            javafx.scene.image.Image broken = Jar_utils.get_broken_icon(300, owner, logger_);

            return Optional.of(new Image_context(path_, path_, broken, logger_));
        }
        logger_.log("using alternate rescaling : "+filter.name());

        double width_ratio = window_width/local_image.getWidth();
        double height_ratio = window_height/local_image.getHeight();
        double scale = width_ratio;
        if ( width_ratio > height_ratio)
        {
            scale = height_ratio;
        }
        Image resized_image = Vips_utils.resize(local_image, scale,filter,logger_);
        if ( resized_image == null) return Optional.empty();
/*
        int h = 10000;
        int w = 10000;
        WritableImage writable_image = new WritableImage(w,h);
        PixelWriter pixel_writer = writable_image.getPixelWriter();
        for ( int y = 0; y < h ; y++)
        {
            for ( int x = 0; x < w/2; x+=3)
            {
                int r = 0;
                int g = 0;
                int b = 0;
                Color color = Color.rgb(r,g,b);
                pixel_writer.setColor(x,y,color);
                r = 255;
                g = 255;
                b = 255;
                color = Color.rgb(r,g,b);
                pixel_writer.setColor(x+1,y,color);
                r = 255;
                g = 255;
                b = 255;
                color = Color.rgb(r,g,b);
                pixel_writer.setColor(x+2,y,color);
            }
        }
        write_png_to_disk(writable_image,new File("fgfgfgf.png"),"wow",logger_);
*/



        return Optional.of(new Image_context(path_, path_, resized_image, logger_));
    }
/*
    //**********************************************************
    public static Image transform_with_alternate_rescaler(
            Image input,
            double scale,
            Image_rescaling_filter filter,
            Logger logger)
    //**********************************************************
    {
        return Vips_utils.resize(input,scale,filter,logger);
    }

    // this is obsolete, the new awy is to leverage VIPS
    //**********************************************************
    public static WritableImage transform_with_alternate_rescaler_old(
            Image input,
            int window_width,
            int window_height,
            Logger logger)
    //**********************************************************
    {
        // we want the image to 'fit' in the window
        // let us look at the aspect ratios to see which constraint is the most demanding
        // is it window_width or window_height?

        double width_ratio = (double)window_width/input.getWidth();
        double height_ratio = (double)window_height/input.getHeight();
        double scale = width_ratio;
        if ( width_ratio > height_ratio)
        {
            scale = height_ratio;
        }
        int target_width = (int) (input.getWidth() * scale);
        logger.log("target_width=" + target_width);

        int source_image_width = (int) input.getWidth();
        int source_image_height = (int) input.getHeight();
        logger.log("source_image_width=" + source_image_width);
        logger.log("source_image_height=" + source_image_height);

        double scaleX = (double) target_width / (double) source_image_width;
        int target_height = (int) ((double) source_image_height * scaleX);
        logger.log("target_height=" + target_height);

        double scaleY = (double) target_height / (double) source_image_height;
        logger.log("scaleX=" + scaleX);
        logger.log("scaleY=" + scaleY);

        double ratioX = (double) source_image_width / target_width;
        double ratioY = (double) source_image_height / target_height;
        logger.log("ratioX=" + ratioX);
        logger.log("ratioY=" + ratioY);

        WritableImage output = new WritableImage(target_width, target_height);

        PixelReader reader = input.getPixelReader();
        if (reader == null) {
            logger.log("FATAL: cannot get a PixelReader, wrong image format?");
            return null;
        }
        PixelWriter writer = output.getPixelWriter();

        final boolean bicubic = false;
        if ( bicubic)
        {
            bicubic(source_image_width, source_image_height,
                    target_width, target_height,
                    ratioX, ratioY,
                    reader, writer);
        }
        else
        {
            lanczos_resize(source_image_width, source_image_height,
                    target_width, target_height,
                    ratioX, ratioY,
                    1.0,
                    reader, writer);
        }

        return output;
    }


    static class Pixel {
        int red;
        int green;
        int blue;
        int opacity;

        Pixel(int red, int green, int blue, int opacity) {
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.opacity = opacity;

        }
    }

    static Pixel[] alloc_pixels(int w, int h) {
        Pixel[] returned = new Pixel[w * h];
        int index = 0;
        for (int X = 0; X < w; X++) {
            for (int Y = 0; Y < h; Y++) {
                returned[index++] = new Pixel(0, 0, 0, 0);
            }
        }
        return returned;
    }

    static Pixel[] AcquireImagePixels(PixelReader reader, int x_, int y_, int w_, int h_) {
        Pixel[] returned = new Pixel[w_ * h_];
        int index = 0;
        for (int xx = x_; xx < x_+w_; xx++) {
            for (int yy = y_; yy < y_+h_; yy++) {
                int argb = reader.getArgb(xx, yy);
                // extract red:
                int alpha = (argb >> 24) & 0xFF;
                int opacity = 255 - alpha;
                int red = (argb >> 16) & 0xFF;
                int green = (argb >> 8) & 0xFF;
                int blue = argb & 0xFF;
                returned[index++] = new Pixel(red, green, blue, opacity);
            }
        }
        return returned;
    }

    public static int RoundDoubleToQuantum(double value) {
        int quantum = (int) Math.round(value);
        if (quantum < 0) return 0;
        if (quantum > 255) return 255;
        return quantum;
    }

    static final int TransparentOpacity = 24;
    static final double MagickEpsilon = 1.0e-12;


    static boolean HorizontalFilter(PixelReader reader, PixelWriter writer,
                                    int target_width, int target_height,
                                    int source_image_width, int source_image_height,
                                    double x_factor, double y_factor,
                                    double blur, int span, long quantum[])
    {
        double[] contribution_pixel = new double[128];
        double[] contribution_weight = new double[128];
        double scale = blur * Math.max(1.0 / x_factor, 1.0);
        double support = scale * 3.0; // support for lanczos is 3.0
        scale = 1.0 / scale;
        for (int x = 0; x < target_width; x++)
        {
            double center = (x + 0.5) / x_factor;
            long start = (long) Math.max(center - support + 0.5, 0);
            long stop = (long) Math.min(center + support + 0.5, source_image_width);
            double density = 0.0;
            int n;
            for (n = 0; n < (stop - start); n++) {
                contribution_pixel[n] = start + n;
                contribution_weight[n] = Lanczos(scale * (start + n - center + 0.5));
                density += contribution_weight[n];
            }
            if ((density != 0.0) && (density != 1.0)) {
                density = 1.0 / density;
                for (int i = 0; i < n; i++)
                    contribution_weight[i] *= density;
            }

            Pixel[] q = alloc_pixels(1, target_height);

            Pixel[] p = AcquireImagePixels(
                    reader,
                    (int) contribution_pixel[0], //x
                    0, //y
                    (int) (contribution_pixel[n - 1] - contribution_pixel[0] + 1), // w
                    source_image_height // h
            );

            Pixel zero = new Pixel(0, 0, 0, 0);
            Pixel pixel = zero;
            for (int y = 0; y < target_height; y++)
            {
                double normalize = 0.0;
                for (int i = 0; i < n; i++)
                {
                    int j = (int) (y * (contribution_pixel[n - 1] - contribution_pixel[0] + 1) +
                            (contribution_pixel[i] - contribution_pixel[0]));
                    double weight = contribution_weight[i];
                    double transparency_coeff = weight * (1 - ((double) p[j].opacity / TransparentOpacity));
                    pixel.red += transparency_coeff * p[j].red;
                    pixel.green += transparency_coeff * p[j].green;
                    pixel.blue += transparency_coeff * p[j].blue;
                    pixel.opacity += weight * p[j].opacity;
                    normalize += transparency_coeff;
                }
                normalize = 1.0 / (Math.abs(normalize) <= MagickEpsilon ? 1.0 : normalize);
                pixel.red *= normalize;
                pixel.green *= normalize;
                pixel.blue *= normalize;
                q[y].red = RoundDoubleToQuantum(pixel.red);
                q[y].green = RoundDoubleToQuantum(pixel.green);
                q[y].blue = RoundDoubleToQuantum(pixel.blue);
                q[y].opacity = RoundDoubleToQuantum(pixel.opacity);

                int alpha = 0;//255 - (q[y].opacity & 0xFF);
                int argb = ((alpha & 0xFF) << 24) |
                        ((q[y].red & 0xFF) << 16) |
                        ((q[y].green & 0xFF) << 8) |
                        (q[y].blue & 0xFF);
                writer.setArgb(x, y, argb);
                System.out.println(""+x+" "+y+" "+argb);
            }
            quantum[0] = quantum[0] + 1;
        }
        return true;
    }


    static boolean VerticalFilter(PixelReader reader, PixelWriter writer,
                                        int target_width, int target_height,
                                        int source_image_width, int source_image_height,
                                        double x_factor, double y_factor,
                                        double blur, int span,long quantum[])
    {
        double[] contribution_pixel = new double[128];
        double[] contribution_weight = new double[128];
        double scale=blur*Math.max(1.0/y_factor,1.0);
        double support=scale*3.0; // support for lanczos is 3.0
        scale=1.0/scale;
        for (int y=0; y < target_height; y++)
        {
            double center= (y+0.5)/y_factor;
            long start=(long) Math.max(center-support+0.5,0);
            long stop=(long) Math.min(center+support+0.5,source_image_height);
            double density=0.0;
            int n;
            for ( n=0; n < (stop-start); n++)
            {
                contribution_pixel[n]=start+n;
                contribution_weight[n]=Lanczos(scale*(start+n-center+0.5));
                density+=contribution_weight[n];
            }
            if ((density != 0.0) && (density != 1.0))
            {
                density = 1.0 / density;
                for (int i = 0; i < n; i++)
                {
                    contribution_weight[i] *= density;
                }
            }

            Pixel[] q = alloc_pixels(target_width,1);

            Pixel[] p = AcquireImagePixels(
                    reader,
                    0, // x
                    (int)contribution_pixel[0], //y
                    source_image_width, //w
                    (int)(contribution_pixel[n-1]-contribution_pixel[0]+1) // h
                );

            Pixel zero = new Pixel(0,0,0,0);
            Pixel pixel = zero;
            for (int x=0; x <  target_width; y++)
            {
                double normalize=0.0;
                for (int i=0; i < n; i++)
                {
                    int j= (int) (x + (contribution_pixel[i]-contribution_pixel[0])*source_image_width);
                    double weight=contribution_weight[i];
                    double transparency_coeff = weight * (1 - ((double) p[j].opacity/TransparentOpacity));
                    pixel.red+=transparency_coeff*p[j].red;
                    pixel.green+=transparency_coeff*p[j].green;
                    pixel.blue+=transparency_coeff*p[j].blue;
                    pixel.opacity+=weight*p[j].opacity;
                    normalize += transparency_coeff;
                }
                normalize = 1.0 / (Math.abs(normalize) <= MagickEpsilon ? 1.0 : normalize);
                pixel.red *= normalize;
                pixel.green *= normalize;
                pixel.blue *= normalize;
                q[x].red=RoundDoubleToQuantum(pixel.red);
                q[x].green=RoundDoubleToQuantum(pixel.green);
                q[x].blue=RoundDoubleToQuantum(pixel.blue);
                q[x].opacity=RoundDoubleToQuantum(pixel.opacity);

                int alpha = 255 - (q[y].opacity & 0xFF);
                int argb = ((alpha & 0xFF) << 24) |
                        ((q[y].red & 0xFF) << 16) |
                        ((q[y].green & 0xFF) << 8) |
                        (q[y].blue & 0xFF);
                writer.setArgb(x, y, argb);
            }
            quantum[0] = quantum[0]+1;
        }
        return true;
    }


    static double Sinc( double x)
    {
        if (x == 0.0)
            return(1.0);
        return(sin(Math.PI*x)/(Math.PI*x));
    }
    
    static double Lanczos( double x)
    {
        if (x < -3.0)
            return(0.0);
        if (x < 0.0)
            return(Sinc(-x)*Sinc(-x/3.0));
        if (x < 3.0)
            return(Sinc(x)*Sinc(x/3.0));
        return(0.0);
    }
    
    private static boolean lanczos_resize(int source_image_width, int source_image_height,
                                       int target_width, int target_height,
                                       double ratioX, double ratioY,
                                       double blur,// nop is 1.0, blur > 1.0 ==> softer, blur < 1.0 ==> sharper
                                       PixelReader reader, PixelWriter writer)
    {     

        double x_factor=(double) target_width/source_image_width;
        double y_factor=(double) target_height/source_image_height;

        double x_support=blur*Math.max(1.0/x_factor,1.0)*3.0;
        double y_support=blur*Math.max(1.0/y_factor,1.0)*3.0;
        double support=Math.max(x_support,y_support);
        if (support < 3.0) support=3.0;

        long[] quantum= {0};
        int span=source_image_width+target_height;
        boolean status=HorizontalFilter(
                reader,writer,
                target_width,target_height,
                source_image_width, source_image_height,
                x_factor, y_factor,
                blur,
                span,
                quantum);

         System.out.println("lanczos_resize done, quantum= "+quantum[0]);
        return status;
    }



    //**********************************************************
    private static void bicubic(int source_image_width, int source_image_height,
                                int target_width, int target_height,
                                double ratioX, double ratioY,
                                PixelReader reader, PixelWriter writer)
    //**********************************************************
    {
        for (int j = 0; j < target_height; j++)
        {
            double y = j * ratioY;
            int yInt = (int) y;
            double yFrac = y - yInt;
            for (int i = 0; i < target_width; i++)
            {
                double x = i * ratioX;
                int xInt = (int)x;
                double xFrac = x - xInt;
                double aAcc = 0, rAcc = 0, gAcc = 0, bAcc = 0;
                double weightSum = 0;

                // Loop through the 4x4 neighborhood
                for (int m = -1; m <= 2; m++)
                {
                    int sampleY = clamp(yInt + m, 0, source_image_height - 1);
                    double wy = cubic(m - yFrac);
                    for (int n = -1; n <= 2; n++)
                    {
                        int sampleX = clamp(xInt + n, 0, source_image_width - 1);
                        double wx = cubic(n - xFrac);
                        double w = wx * wy;
                        int argb = reader.getArgb(sampleX, sampleY);
                        aAcc += ((argb >> 24) & 0xFF) * w;
                        rAcc += ((argb >> 16) & 0xFF) * w;
                        gAcc += ((argb >> 8)  & 0xFF) * w;
                        bAcc += (argb & 0xFF) * w;
                        weightSum += w;
                    }
                }
                int a = clamp((int) Math.round(aAcc / weightSum), 0, 255);
                int r = clamp((int) Math.round(rAcc / weightSum), 0, 255);
                int g = clamp((int) Math.round(gAcc / weightSum), 0, 255);
                int b = clamp((int) Math.round(bAcc / weightSum), 0, 255);
                int argb = (a << 24) | (r << 16) | (g << 8) | b;
                writer.setArgb(i, j, argb);
            }


        }
    }

    //**********************************************************
    private static int clamp(int val, int min, int max)
    //**********************************************************
    {
        return Math.max(min, Math.min(val, max));
    }

    // Cubic interpolation kernel function with a = -0.5
    //**********************************************************
    private static double cubic(double x)
    //**********************************************************
    {
        double a = -0.5;
        x = Math.abs(x);
        if (x <= 1.0) 
        {
            return (a + 2) * Math.pow(x, 3) - (a + 3) * Math.pow(x, 2) + 1;
        } 
        else if (x < 2.0) 
        {
            return a * Math.pow(x, 3) - 5 * a * Math.pow(x, 2) + 8 * a * x - 4 * a;
        } 
        else 
        {
            return 0.0;
        }
    }

    public boolean use_FFM()
    {
        // Load the GraphicsMagick shared library
        Linker linker = Linker.nativeLinker();
        SymbolLookup lookup = SymbolLookup.libraryLookup("GraphicsMagickWand", Arena.global());

        // Find essential functions
        MethodHandle genesis = linker.downcallHandle(
                lookup.find("MagickWandGenesis").get(),
                FunctionDescriptor.ofVoid()
        );
        MethodHandle terminus = linker.downcallHandle(
                lookup.find("MagickWandTerminus").get(),
                FunctionDescriptor.ofVoid()
        );
        MethodHandle newWand = linker.downcallHandle(
                lookup.find("NewMagickWand").get(),
                FunctionDescriptor.of(ADDRESS)
        );
        MethodHandle readImage = linker.downcallHandle(
                lookup.find("MagickReadImage").get(),
                FunctionDescriptor.of(JAVA_BOOLEAN, ADDRESS, ADDRESS)
        );
        MethodHandle resizeImage = linker.downcallHandle(
                lookup.find("MagickResizeImage").get(),
                FunctionDescriptor.of(JAVA_BOOLEAN, ADDRESS, JAVA_LONG, JAVA_LONG, JAVA_INT, JAVA_DOUBLE)
        );
        MethodHandle writeImage = linker.downcallHandle(
                lookup.find("MagickWriteImage").get(),
                FunctionDescriptor.of(JAVA_BOOLEAN, ADDRESS, ADDRESS)
        );

        // Initialize GraphicsMagick
        try {
            genesis.invoke();
        } catch (Throwable e) {
            System.out.println(e);
            return false;
        }
        System.out.println("init OK");

        try (Arena arena = Arena.ofConfined()) {
            // Create a new MagickWand
            MemorySegment wand = null;
            try {
                wand = (MemorySegment) newWand.invoke();
            } catch (Throwable e) {
                System.out.println(e);
                return false;
            }

            // Read input image
            MemorySegment inputPath = arena.allocateFrom("input.jpg");
            try {
                readImage.invoke(wand, inputPath);
            } catch (Throwable e) {
                System.out.println(e);
                return false;            }
            System.out.println("read OK");

            // Apply Lanczos filter (filter constant = 22)
            int LanczosFilter = 22;
            long newWidth = 800, newHeight = 600;
            double blur = 1.0;

            try {
                resizeImage.invoke(wand, newWidth, newHeight, LanczosFilter, blur);
            } catch (Throwable e) {
                System.out.println(e);
                return false;            }

            System.out.println("resize OK");

            // Write output image
            MemorySegment outputPath = arena.allocateFrom("output.jpg");
            try {
                writeImage.invoke(wand, outputPath);
            } catch (Throwable e) {
                System.out.println(e);
                return false;
            }

            System.out.println("resize OK");

        }

        // Cleanup
        try {
            terminus.invoke();
        } catch (Throwable e) {
            System.out.println(e);
            return false;
        }


        return true;
    }
*/




/*
    TODO restore the alternate rescaler option, this code used AWT Graphics2D
    there is an alternative using pure javafx




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
            System.out.println("SPEED is on");
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
            // it causes a change in image length
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

        return true;
    }


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
            // it causes a change in image length
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
            // it causes a change in image length
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
*/


}
