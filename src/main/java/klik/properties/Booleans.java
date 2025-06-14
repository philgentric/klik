//SOURCES ../audio/Audio_player_FX_UI.java
package klik.properties;

import javafx.stage.Window;
import klik.properties.features.Feature;
import klik.util.log.Logger;
import klik.util.ui.Jfx_batch_injector;
import klik.util.ui.Popups;


// these are user preferences that are saved to disk

//**********************************************************
public class Booleans
//**********************************************************
{
    //static boolean ffmpeg_popup_done = false;
    //static boolean GraphicsMagick_popup_done = false;


    //**********************************************************
    public static void set_boolean(String s, boolean b)
    //**********************************************************
    {
        IProperties pm = Non_booleans.get_main_properties_manager();
        pm.set(s, String.valueOf(b));

    }

    // this routine default to FALSE for absent values
    //**********************************************************
    public static boolean get_boolean(String s)
    //**********************************************************
    {
        IProperties pm = Non_booleans.get_main_properties_manager();
        String bb = pm.get(s);
        Boolean b = Boolean.parseBoolean(bb);
        return b;
    }

    //**********************************************************
    public static boolean get_boolean_defaults_to_true(String s)
    //**********************************************************
    {
        IProperties pm = Non_booleans.get_main_properties_manager();
        String bb = pm.get(s);
        if ( bb == null)
        {
            pm.set(s, "true");
            return true;
        }
        Boolean b = Boolean.parseBoolean(bb);
        return b;
    }




    //**********************************************************
    public static void manage_show_ffmpeg_install_warning(Window owner, Logger logger)
    //**********************************************************
    {
        if ( get_boolean_defaults_to_true(Feature.Show_ffmpeg_install_warning.name()))
        {
            //if ( !ffmpeg_popup_done)
            {
                //ffmpeg_popup_done = true;
                String msg = "klik uses ffmpeg to support several features. It is easy and free to install ffmpeg (google it!)";
                logger.log("WARNING: " + msg);
                Jfx_batch_injector.inject(() -> {
                    if (Popups.popup_ask_for_confirmation(owner, msg,"If you do not want to see this warning about installing ffmepg again, click OK", logger)) {
                        set_boolean(Feature.Show_ffmpeg_install_warning.name(),false);
                    }
                },logger);
            }
        }
    }
    //**********************************************************
    public static void manage_show_GraphicsMagick_install_warning(Window owner,Logger logger)
    //**********************************************************
    {
        if ( get_boolean_defaults_to_true(Feature.Show_GraphicsMagick_install_warning.name()))
        {
            //if(!GraphicsMagick_popup_done)
            {
                //GraphicsMagick_popup_done = true;
                String msg = "klik uses the gm convert utility of GraphicsMagick.org to support some features. It is easy and free to install (GraphicsMagick.org)";
                logger.log("WARNING: " + msg);
                Jfx_batch_injector.inject(() -> {
                    if (Popups.popup_ask_for_confirmation(owner, msg,"If you do not want to see this warning about installing GraphicsMagick again, click OK", logger)) {
                        set_boolean(Feature.Show_GraphicsMagick_install_warning.name(),false);
                    }
                },logger);
            }
        }
    }



}
