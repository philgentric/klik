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

    public static final String GOT_IT_DONT_SHOW_ME_THIS_AGAIN = "Got it ! Dont show me this again.";

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
                    "\nOn Mac: use the launcher or type 'brew install ffmpeg' in a shell";

            Platform.runLater(()->{
            if ( Popups.info_popup(msg, GOT_IT_DONT_SHOW_ME_THIS_AGAIN,owner,logger))
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
                            "\nOn Mac: use the launcher or type 'brew install graphicsmagick' in a shell";
                    if ( Popups.info_popup(msg,GOT_IT_DONT_SHOW_ME_THIS_AGAIN,owner,logger))
                    {
                        set_boolean(Feature.Show_graphicsmagick_install_warning.name(), false,owner);
                    }

                }
            };
            Platform.runLater(r);

        }
    }

    //**********************************************************
    public static void manage_show_imagemagick_install_warning(Window owner, Logger logger)
    //**********************************************************
    {
        if ( get_boolean_defaults_to_true(Feature.Show_imagemagick_install_warning.name(),owner))
        {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    String msg = "klik uses the convert utility of imagemagick ('magick'') to support some features. " +
                            "\nIt is easy and free to install (http://www.imagemagick.org/)" +
                            "\nOn Mac: use the launcher or type 'brew install imagemagick' in a shell";
                    if ( Popups.info_popup(msg,GOT_IT_DONT_SHOW_ME_THIS_AGAIN,owner,logger))
                    {
                        set_boolean(Feature.Show_imagemagick_install_warning.name(), false,owner);
                    }

                }
            };
            Platform.runLater(r);

        }
    }

    //**********************************************************
    public static void manage_show_mediainfo_install_warning(Window owner, Logger logger)
    //**********************************************************
    {
        if ( get_boolean_defaults_to_true(Feature.Show_mediainfo_install_warning.name(),owner))
        {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    String msg = "klik uses the convert utility of imagemagick ('magick'') to support some features. " +
                            "\nIt is easy and free to install (http://www.imagemagick.org/)" +
                            "\nOn Mac: use the launcher or type 'brew install imagemagick' in a shell";
                    if ( Popups.info_popup(msg,GOT_IT_DONT_SHOW_ME_THIS_AGAIN,owner,logger))
                    {
                        set_boolean(Feature.Show_mediainfo_install_warning.name(), false,owner);
                    }

                }
            };
            Platform.runLater(r);

        }
    }

    //**********************************************************
    public static void manage_show_ytdlp_install_warning(Window owner, Logger logger)
    //**********************************************************
    {
        if ( get_boolean_defaults_to_true(Feature.Show_ytdlp_install_warning.name(),owner))
        {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    String msg = "klik uses yt-dlp to support some features. " +
                            "\nIt is easy and free to install (https://github.com/yt-dlp/yt-dlp)" +
                            "\nOn Mac: use the launcher or type 'brew install yt-dlp' in a shell";
                    if ( Popups.info_popup(msg,GOT_IT_DONT_SHOW_ME_THIS_AGAIN,owner,logger))
                    {
                        set_boolean(Feature.Show_ytdlp_install_warning.name(), false,owner);
                    }

                }
            };
            Platform.runLater(r);

        }
    }


}
