//SOURCES ../../audio/Audio_player_FX_UI.java
package klik.properties.boolean_features;

import javafx.application.Platform;
import javafx.stage.Window;
import klik.properties.IProperties;
import klik.properties.Non_booleans_properties;
import klik.util.log.Logger;
import klik.util.ui.Popups;


// these are user preferences that are saved to disk

//**********************************************************
public class Booleans
//**********************************************************
{

    //**********************************************************
    public static void set_boolean(String s, boolean b, Window owner)
    //**********************************************************
    {
        IProperties pm = Non_booleans_properties.get_main_properties_manager(owner);
        pm.set(s, String.valueOf(b));

    }

    // this routine default to FALSE for absent values
    //**********************************************************
    public static boolean get_boolean(String s,  Window owner)
    //**********************************************************
    {
        IProperties pm = Non_booleans_properties.get_main_properties_manager(owner);
        String bb = pm.get(s);
        Boolean b = Boolean.parseBoolean(bb);
        return b;
    }

    //**********************************************************
    public static boolean get_boolean_defaults_to_true(String s, Window owner)
    //**********************************************************
    {
        IProperties pm = Non_booleans_properties.get_main_properties_manager(owner);
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
        if ( get_boolean_defaults_to_true(Feature.Show_ffmpeg_install_warning.name(),owner))
        {
            String msg = "klik uses ffmpeg to support several features. " +
                    "\nIt is easy and free to install (https://ffmpeg.org/)"+
            "\nOn Mac: 'brew install ffmpeg'";

            Platform.runLater(()->{
            if ( Popups.info_popup(msg,owner,logger))
            {
                set_boolean(Feature.Show_ffmpeg_install_warning.name(), false,owner);
            }});
        }
    }
    //**********************************************************
    public static void manage_show_graphicsmagick_install_warning(Window owner, Logger logger)
    //**********************************************************
    {
        if ( get_boolean_defaults_to_true(Feature.Show_graphicsmagick_install_warning.name(),owner))
        {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    String msg = "klik uses the gm convert utility of graphicsmagick (gm) to support some features. " +
                            "\nIt is easy and free to install (http://www.graphicsmagick.org/)" +
                            "\nOn Mac: 'brew install graphicsmagick'";
                    if ( Popups.info_popup(msg,owner,logger))
                    {
                        set_boolean(Feature.Show_graphicsmagick_install_warning.name(), false,owner);
                    }

                }
            };
            Platform.runLater(r);

        }
    }



}
