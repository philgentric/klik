package klik.look;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.stage.Window;
import klik.Klik_application;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;

import javax.imageio.IIOException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

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


    //**********************************************************
    public static byte[] load_image_bytes_from_jar0(String image_file_path,Window owner, Logger logger)
    //**********************************************************
    {
        InputStream s = get_jar_InputStream_by_name(image_file_path);
        if ( s == null)
        {
            logger.log("load_icon_fx_from_jar failed for: " + image_file_path);
            return null;
        }

        int icon_size = 128;
        Image image = new Image(s, icon_size, icon_size, true, true);
        if (image.isError())
        {
            logger.log("WARNING: an error occurred when reading: " + image_file_path);
            image = get_broken_icon(icon_size, owner,logger);
        }
        // convert Image to byte[]
        // take the pixel reader

        int w = (int)image.getWidth();
        int h = (int)image.getHeight();

// Create a new Byte Buffer, but we'll use BGRA (1 byte for each channel) //

        byte[] buf = new byte[w * h * 4];

/* Since you can get the output in whatever format with a WritablePixelFormat,
   we'll use an already created one for ease-of-use. */

        image.getPixelReader().getPixels(0, 0, w, h, PixelFormat.getByteBgraInstance(), buf, 0, w * 4);
        return buf;
    }

    //**********************************************************
    public static byte[] load_image_bytes_from_jar(String image_file_path, Window owner, Logger logger)
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
