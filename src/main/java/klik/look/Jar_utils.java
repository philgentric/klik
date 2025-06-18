package klik.look;

import javafx.scene.image.Image;
import javafx.stage.Window;
import klik.Klik_application;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;

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
