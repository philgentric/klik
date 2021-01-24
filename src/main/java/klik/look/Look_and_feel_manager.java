package klik.look;

import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import klik.Klik_application;
import klik.properties.Properties;
import klik.util.Logger;
import klik.util.Stack_trace_getter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

public class Look_and_feel_manager
{

    public static final boolean icon_load_dbg = true;
    public static final boolean look_dbg = false;
    public static Logger logger;

    private static Look_and_feel instance = null;
    public static List<Look_and_feel> registered = new ArrayList<>();
    private static Image default_icon = null;
    public static Image broken_icon = null;
    public static Image denied_icon = null;
    public static Image folder_icon = null;
    public static Image trash_icon = null;
    public static Image up_icon = null;


    public static Look_and_feel get_instance()
    {
        if (logger == null)
        {
            // must init !
            return null;
        }
        return instance;
    }

    public static void init_Look_and_feel(Logger logger_)
    {
        logger = logger_;
        if (registered.isEmpty() == false) return;
        registered.add(new Look_and_feel_shiny_black(logger_));
        registered.add(new Look_and_feel_orange(logger_));
        registered.add(new Look_and_feel_grey(logger_));
        registered.add(new Look_and_feel_plain(logger_));
        registered.add(new Look_and_feel_b_and_w(logger_));
        registered.add(new Look_and_feel_red(logger_));
        registered.add(new Look_and_feel_blue(logger_));
        instance = Properties.get_style(logger_);

    }

    public static void set_look_and_feel(Look_and_feel style)
    {
        logger.log(Stack_trace_getter.get_stack_trace("setting style = "+style.name));
        instance = style;
        Properties.set_style(style);
        reset();
    }

    public static void reset() {
        default_icon = null;
        broken_icon = null;
        denied_icon = null;
        trash_icon = null;
        folder_icon = null;
        up_icon = null;
    }


    //**********************************************************
    public static Image get_default_directory_icon(double icon_size)
    //**********************************************************
    {
        if (folder_icon == null)
        {
            folder_icon = load_icon_fx_from_jar(get_instance().get_folder_icon_file_name(), icon_size);
        }
        return folder_icon;
    }

    //**********************************************************
    public static Image get_default_icon(double icon_size)
    //**********************************************************
    {
        if (default_icon == null) {
            load_default_icon(icon_size);
        }
        if (default_icon == null) return null;
        if (default_icon.getHeight() != icon_size) {
            load_default_icon(icon_size);
        }
        return default_icon;
    }

    //**********************************************************
    public static Image load_default_icon(double icon_size)
    //**********************************************************
    {
        default_icon = load_icon_fx_from_jar(Look_and_feel_manager.get_instance().get_default_image_file_name(), icon_size);
        return default_icon;
    }


    //**********************************************************
    private static Image load_denied_icon(double icon_size)
    //**********************************************************
    {
        denied_icon = load_icon_fx_from_jar(Look_and_feel_manager.get_instance().get_denied_icon_file_name(), icon_size);
        return denied_icon;
    }

    //**********************************************************
    public static Image get_denied_icon(double icon_size)
    //**********************************************************
    {
        if (denied_icon == null) default_icon = load_denied_icon(icon_size);
        return denied_icon;
    }

    //**********************************************************
    public static Image get_broken_icon(double icon_size)
    //**********************************************************
    {
        if (broken_icon == null) load_broken_icon(icon_size);
        return broken_icon;
    }

    //**********************************************************
    private static void load_broken_icon(double icon_size)
    //**********************************************************
    {
        broken_icon = load_icon_fx_from_jar(Look_and_feel_manager.get_instance().get_broken_icon_file_name(), icon_size     );
    }


    //**********************************************************
    public static Image load_icon_fx_from_jar(String image_file_path, double icon_size)
    //**********************************************************
    {

        if ( icon_load_dbg) logger.log("Partial path for the icon->" + image_file_path +"<-");
        ClassLoader l = Thread.currentThread().getContextClassLoader();
        if ( icon_load_dbg) logger.log("ClassLoader=" + l.toString() );

        {
            URL url1 = Klik_application.class.getResource("");
            if (url1 == null)
            {
                logger.log("Method1 fails: Klik_application.class.getResource(\"\");  failed");
            }
            else
            {
                logger.log("Method1 works: Klik_application.class.getResource(\"\");"+url1.getPath());
            }
        }
        {
            URL url2 = Klik_application.class.getResource(".");
            if (url2 == null)
            {
                logger.log("Method2 fails: Klik_application.class.getResource(\".\");  failed"  );
            }
            else
            {
                logger.log("Method2 works: Klik_application.class.getResource().getPath(\".\")=" + url2.getPath());
            }
        }
        {
            URL url3 = Klik_application.class.getResource("../images");
            if (url3 == null)
            {
                logger.log("Method3 fails: Klik_application.class.getResource(\"../images\");  failed");
            }
            else
            {
                logger.log("Method3 works: Klik_application.class.getResource(\"../images\"); " + url3.getPath());
            }
        }
        {
            String classpath  = System.getProperty("java.class.path");
            URL url5 = Klik_application.class.getResource(classpath);
            if (url5 == null)
            {
                logger.log("Method5 failed: classpath->"+classpath+"<-");
            }
            else
            {
                logger.log("Method5 works: classpath " + url5.getPath());
            }
        }

        /*
        this gives the original source path: not the one being deployed
        URL url_loader = Klik_application.class.getProtectionDomain().getCodeSource().getLocation();
        logger.log("===Klik_application.class.getProtectionDomain().getCodeSource().getLocation()====" + url_loader.toString() );
        logger.log("===getProtectionDomain().getCodeSource().getLocation().getPath()====" + url_loader.getPath() );
        */

        URL url4 = Klik_application.class.getResource("../"+image_file_path);
        if( url4 == null)
        {
            logger.log("Method4 fails :Klik_application.class.getResource(\"../\"+image_file_path+);  failed"  );
            return null;
        }
        logger.log("Method4 works :Klik_application.class.getResource(\"../\"+image_file_path+)" +url4.getPath() );

        if ( icon_load_dbg) logger.log("path for icon=" + url4.getPath() );






        String decoded_path = null;
        try
        {
            decoded_path = URLDecoder.decode(url4.getPath(),"UTF-8");
        }
        catch ( UnsupportedEncodingException e)
        {
            logger.log("path for icon decoded failed" + url4.getPath()+ " " + e.toString());

            return null;
        }
      try (FileInputStream input_stream = new FileInputStream(decoded_path))
      //  try (InputStream input_stream = Klik_application.class.getResourceAsStream(url2.getPath()))
        {


            Image image = new Image(input_stream, icon_size, icon_size, true, true);

            if ( image.isError())
            {
                //return null;
                logger.log("WARNING: an error occurred when reading: "+image_file_path);


                return get_broken_icon(icon_size);
            }
            return image;
        } catch (FileNotFoundException e) {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
        }


        return null;
    }




    //**********************************************************
    public static Image get_up_icon(double icon_size)
    //**********************************************************
    {
        if (up_icon == null)
        {
            up_icon = load_icon_fx_from_jar(Look_and_feel_manager.get_instance().get_up_icon_file_name(),icon_size);
        }
        return up_icon;
    }


    //**********************************************************
    public static Image get_trash_icon(double icon_size)
    //**********************************************************
    {
        if (trash_icon == null)
        {
            trash_icon = load_icon_fx_from_jar(Look_and_feel_manager.get_instance().get_trash_icon_file_name(),icon_size);
        }
        return trash_icon;
    }

    //**********************************************************
    public static void set_button_look_as_trash(Button trash, double button_height)
    //**********************************************************
    {
        Image trash_image = Look_and_feel_manager.get_trash_icon(button_height);
        set_button_look(trash, button_height, trash_image, true);
    }

    //**********************************************************
    public static void set_button_look_as_up(Button up, double button_height)
    //**********************************************************
    {
        Image up_image = get_up_icon(button_height);
        set_button_look(up, button_height, up_image, true);
    }

    //**********************************************************
    public static void set_button_look_as_folder(Button button, double button_height)
    //**********************************************************
    {
        Image image = get_default_directory_icon(button_height);
        set_button_look(button, button_height, image, true);
    }

    //**********************************************************
    public static void set_button_look(Button button, double button_height, Image image, boolean is_dir)
    //**********************************************************
    {
        ImageView image_view = new ImageView(image);
        image_view.setPreserveRatio(true);
        image_view.setFitHeight(button_height / 2);
        button.setGraphic(image_view);

        if ( look_dbg)logger.log(Stack_trace_getter.get_stack_trace("set_button_look"));
        if ( is_dir)
        {
            Look_and_feel_manager.get_instance().set_directory_style(button);

        }
        else
        {
            Look_and_feel_manager.get_instance().set_file_style(button);

        }
    }



}
