// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ../../audio/Audio_player_FX_UI.java
package klikr.properties.boolean_features;

import javafx.application.Platform;
import javafx.stage.Window;
import klikr.properties.File_storage;
import klikr.util.Shared_services;
import klikr.util.log.Logger;
import klikr.util.ui.Popups;


// these are user preferences that are saved to disk

//**********************************************************
public class Booleans
//**********************************************************
{

    public static final String GOT_IT_DONT_SHOW_ME_THIS_AGAIN = "Got it ! Dont show me this again.";

    //**********************************************************
    public static void save_boolean(String s, boolean b, Window owner)
    //**********************************************************
    {
        File_storage pm = Shared_services.main_properties();
        pm.set_and_save(s, String.valueOf(b));

    }

    // this routine default to FALSE for absent values
    //**********************************************************
    public static boolean get_boolean_defaults_to_false(String s)
    //**********************************************************
    {
        File_storage pm = Shared_services.main_properties();
        String bb = pm.get(s);
        return Boolean.parseBoolean(bb); // returns false if bb is null
    }

    //**********************************************************
    public static boolean get_boolean_defaults_to_true(String s)
    //**********************************************************
    {
        File_storage pm = Shared_services.main_properties();
        String bb = pm.get(s);
        if ( bb == null)
        {
            pm.set_and_save(s, "true");
            return true;
        }
        return Boolean.parseBoolean(bb);
    }




    private static boolean show_ffmpeg_install_warning_done = false;
    //**********************************************************
    public static void manage_show_ffmpeg_install_warning(Window owner, Logger logger)
    //**********************************************************
    {
        if ( get_boolean_defaults_to_true(Feature.Show_ffmpeg_install_warning.name()))
        {
            if ( show_ffmpeg_install_warning_done)
            {
                logger.log("ffmpeg warning already shown to the user, in this session");
                return;
            }
            show_ffmpeg_install_warning_done = true;
            String msg = "Klikr uses ffmpeg to support several features, " +
                    "\nlike making an animated gif icon for movies."+
                    "\nffmpeg is easy and free to install (https://ffmpeg.org/)"+
                    "\nOn Mac, type 'brew install ffmpeg' in a shell";

            Platform.runLater(()->{
                if ( Popups.info_popup(msg, GOT_IT_DONT_SHOW_ME_THIS_AGAIN,owner,logger))
                {
                    Feature_cache.update_cached_boolean_and_save(Feature.Show_ffmpeg_install_warning, false,owner);
                }
            });
        }
    }

    private static boolean show_graphicsmagick_install_warning_done = false;
    //**********************************************************
    public static void manage_show_graphicsmagick_install_warning(Window owner, Logger logger)
    //**********************************************************
    {
        if ( get_boolean_defaults_to_true(Feature.Show_graphicsmagick_install_warning.name()))
        {
            if ( show_graphicsmagick_install_warning_done) return;
            show_graphicsmagick_install_warning_done = true;
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    String msg = "Klikr uses the gm convert utility of graphicsmagick (gm) to support some features. " +
                            "\nIt is easy and free to install (http://www.graphicsmagick.org/)" +
                            "\nOn Mac, type: 'brew install graphicsmagick' in a shell";
                    if ( Popups.info_popup(msg,GOT_IT_DONT_SHOW_ME_THIS_AGAIN,owner,logger))
                    {
                        Feature_cache.update_cached_boolean_and_save(Feature.Show_graphicsmagick_install_warning, false,owner);
                    }

                }
            };
            Platform.runLater(r);

        }
    }

    private static boolean show_fpcalc_install_warning_done = false;
    //**********************************************************
    public static void manage_show_fpcalc_install_warning(Window owner, Logger logger)
    //**********************************************************
    {
        if ( get_boolean_defaults_to_true(Feature.Show_fpcalc_install_warning.name()))
        {
            if ( show_fpcalc_install_warning_done) return;
            show_fpcalc_install_warning_done = true;
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    String msg = "Klikr uses the fpcalc utility of chromaprint for song similarity estimation. " +
                            "\nIt is easy and free to install (https://acoustid.org/chromaprint)" +
                            "\nOn Mac: use the launcher or type 'brew install chromaprint' in a shell";
                    if ( Popups.info_popup(msg,GOT_IT_DONT_SHOW_ME_THIS_AGAIN,owner,logger))
                    {
                        Feature_cache.update_cached_boolean_and_save(Feature.Show_fpcalc_install_warning, false,owner);
                    }

                }
            };
            Platform.runLater(r);
        }
    }

    private static boolean show_imagemagick_install_warning_done = false;
    //**********************************************************
    public static void manage_show_imagemagick_install_warning(Window owner, Logger logger)
    //**********************************************************
    {
        if ( get_boolean_defaults_to_true(Feature.Show_imagemagick_install_warning.name()))
        {
            if ( show_imagemagick_install_warning_done) return;
            show_imagemagick_install_warning_done = true;
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    String msg = "Klikr uses the convert utility of imagemagick ('magick'') to support some features. " +
                            "\nIt is easy and free to install (http://www.imagemagick.org/)" +
                            "\nOn Mac: use the launcher or type 'brew install imagemagick' in a shell";
                    if ( Popups.info_popup(msg,GOT_IT_DONT_SHOW_ME_THIS_AGAIN,owner,logger))
                    {
                        Feature_cache.update_cached_boolean_and_save(Feature.Show_imagemagick_install_warning, false,owner);
                    }

                }
            };
            Platform.runLater(r);

        }
    }

    private static boolean show_mediainfo_install_warning_done = false;
    //**********************************************************
    public static void manage_show_mediainfo_install_warning(Window owner, Logger logger)
    //**********************************************************
    {
        if ( get_boolean_defaults_to_true(Feature.Show_mediainfo_install_warning.name()))
        {
            if ( show_mediainfo_install_warning_done) return;
            show_mediainfo_install_warning_done = true;
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    String msg = "Klikr uses the convert utility of imagemagick ('magick'') to support some features. " +
                            "\nIt is easy and free to install (http://www.imagemagick.org/)" +
                            "\nOn Mac: use the launcher or type 'brew install imagemagick' in a shell";
                    if ( Popups.info_popup(msg,GOT_IT_DONT_SHOW_ME_THIS_AGAIN,owner,logger))
                    {
                        Feature_cache.update_cached_boolean_and_save(Feature.Show_mediainfo_install_warning, false,owner);
                    }

                }
            };
            Platform.runLater(r);

        }
    }

    private static boolean show_ytdlp_install_warning_done = false;
    //**********************************************************
    public static void manage_show_ytdlp_install_warning(Window owner, Logger logger)
    //**********************************************************
    {
        if ( get_boolean_defaults_to_true(Feature.Show_ytdlp_install_warning.name()))
        {
            if ( show_ytdlp_install_warning_done) return;
            show_ytdlp_install_warning_done = true;
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    String msg = "Klikr uses yt-dlp to support some features. " +
                            "\nIt is easy and free to install (https://github.com/yt-dlp/yt-dlp)" +
                            "\nOn Mac: use the launcher or type 'brew install yt-dlp' in a shell";
                    if ( Popups.info_popup(msg,GOT_IT_DONT_SHOW_ME_THIS_AGAIN,owner,logger))
                    {
                        Feature_cache.update_cached_boolean_and_save(Feature.Show_ytdlp_install_warning, false,owner);
                    }

                }
            };
            Platform.runLater(r);

        }
    }


}
