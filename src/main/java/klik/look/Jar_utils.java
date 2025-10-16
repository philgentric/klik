package klik.look;

import javafx.scene.image.*;
import javafx.stage.Window;
import klik.Klik_application;
import klik.util.image.Static_image_utilities;
import klik.properties.Non_booleans_properties;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Path;

//**********************************************************
public class Jar_utils
//**********************************************************
{

    public static Image broken_icon = null;

    //**********************************************************
    public static Image load_jfx_image_from_jar(String image_file_path, double icon_size, Window owner,Logger logger)
    //**********************************************************
    {
        InputStream s = get_jar_InputStream_by_name(image_file_path);
        if ( s == null)
        {
            logger.log("load_icon_fx_from_jar failed for: " + image_file_path);
            return null;
        }

        Image image = new Image(s, icon_size, icon_size, true, true);
        if (image.isError())
        {
            logger.log("WARNING: an error occurred when reading: " + image_file_path);
            return get_broken_icon(icon_size, owner,logger);
        }
        return image;
    }


    // loads bytes from an image in the jar
    // and returns a square byte array with the image centered

    //**********************************************************
    public static byte[] load_image_bytes_from_jar(String image_file_path,Window owner, Logger logger)
    //**********************************************************
    {
        InputStream input_stream = get_jar_InputStream_by_name(image_file_path);
        if ( input_stream == null)
        {
            logger.log("load_icon_fx_from_jar failed for: " + image_file_path);
            return null;
        }

        int icon_size = 128;
        Image image = new Image(input_stream, icon_size, icon_size, true, true);
        if (image.isError())
        {
            logger.log("WARNING: an error occurred when reading: " + image_file_path);
            image = get_broken_icon(icon_size, owner,logger);
        }


        int w = (int)image.getWidth();
        int h = (int)image.getHeight();

        logger.log("icon is "+w+"x"+h+" thus "+(w*h)+" pixels");


        //read the BGRA bytes
        byte[] in_bytes = new byte[w * h * 4];
        WritablePixelFormat<ByteBuffer> fmt = PixelFormat.getByteBgraPreInstance();
        PixelReader pr = image.getPixelReader();
        pr.getPixels(0,0,w,h,fmt,in_bytes,0,w*4);

        logger.log("in_bytes length is "+in_bytes.length);

        int max = h;
        if ( w>max) max=w;

        byte[] out_bytes = new byte[max * max * 4];

        // copy the image into the center of the buf

        int y2 = 0;
        for ( int y=(w-h)/2; y< (w+h)/2;y++)
        {
            for(int x=0; x<w; x++)
            {
                out_bytes[(y*w+x)*4+0] = in_bytes[(y2*w+x)*4+0];
                out_bytes[(y*w+x)*4+1] = in_bytes[(y2*w+x)*4+1];
                out_bytes[(y*w+x)*4+2] = in_bytes[(y2*w+x)*4+2];
                out_bytes[(y*w+x)*4+3] = in_bytes[(y2*w+x)*4+3];
            }
            y2++;
        }

        Path klik_trash = Non_booleans_properties.get_trash_dir(Path.of("").toAbsolutePath(),owner,logger);
        String tmp_icon_file_name = klik_trash.resolve("tmp_klik_icon.png").toString();

        // create a conformant png file from the bytes

        WritableImage icon = new WritableImage(max,max);
        PixelWriter pw = icon.getPixelWriter();
        pw.setPixels(0,0,max,max,fmt,out_bytes,0,max*4);
        File out_file = new File(tmp_icon_file_name);
        Static_image_utilities.write_png_to_disk(icon, out_file.toPath(), logger);

        // read it back
        try {
            InputStream s = new FileInputStream(tmp_icon_file_name);
            byte[] icon_bytes = new byte[s.available()];
            s.read(icon_bytes);
            return icon_bytes;
        } catch (IOException e)
        {
            logger.log("Warning: cannot read from toto.png");
        }
        return null;
    }

    // this reads the icon ok, but it is rendered as a square even if it is a rectangle
    //**********************************************************
    public static byte[] load_image_bytes_from_jar_square(String image_file_path, Window owner, Logger logger)
    //**********************************************************
    {
        InputStream s = get_jar_InputStream_by_name(image_file_path);
        if ( s == null)
        {
            logger.log("load_icon_fx_from_jar failed for: " + image_file_path);
            return null;
        }

        try
        {
            // Read the stream into a byte array
            byte[] icon_bytes = new byte[s.available()];
            s.read(icon_bytes);
            return icon_bytes;
        }
        catch (IOException e)
        {
            logger.log("WARNING: an error occurred when reading: " + image_file_path + " " + e.getMessage());
            return null;
        }
    }



    //**********************************************************
    public static Image get_broken_icon(double icon_size, Window owner, Logger logger)
    //**********************************************************
    {
        if (broken_icon != null)
        {
            if ( broken_icon.getHeight() == icon_size) return broken_icon;
        }
        Look_and_feel local_instance = Look_and_feel_manager.get_instance(owner,logger);
        if (local_instance == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BAD WARNING: cannot get look and feel instance"));
            return null;
        }
        String path = local_instance.get_broken_icon_path();
        if (path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BAD WARNING: cannot get broken icon path"));
            return null;
        }
        broken_icon = load_jfx_image_from_jar(path, icon_size,owner,logger);
        return broken_icon;
    }

    //**********************************************************
    public static URL get_URL_by_name(String name)
    //**********************************************************
    {
        // this scheme works with Jbang
        ClassLoader class_loader = Thread.currentThread().getContextClassLoader();
        //System.out.println("get_URL_by_name trying with class_loader : "+class_loader+" ...");
        URL url = class_loader.getResource(name);
        if (url != null)
        {
            //System.out.println("... worked!");
            return url;
        }
        // this scheme works with Gradle
        return Klik_application.class.getResource(name);
    }

    //**********************************************************
    public static InputStream get_jar_InputStream_by_name(String name)
    //**********************************************************
    {
        // this scheme works with Jbang
        ClassLoader class_loader = Thread.currentThread().getContextClassLoader();
        //System.out.println("get_InputStream_by_name trying with class_loader : "+class_loader+ " ...");
        InputStream s = class_loader.getResourceAsStream(name);
        if (s != null)
        {
            //System.out.println("... worked");
            return s;
        }
        //System.out.println("Thread.currentThread().getContextClassLoader().getResourceAsStream DID NOT work");
        // this scheme works with Gradle
        return Klik_application.class.getResourceAsStream(name);
    }

}
