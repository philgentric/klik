//SOURCES ../audio/Audio_player_FX_UI.java
package klik.properties;

import javafx.stage.Window;
import klik.util.log.Logger;
import klik.util.ui.Jfx_batch_injector;
import klik.util.ui.Popups;


// these are user preferences that are saved to disk

//**********************************************************
public class Booleans
//**********************************************************
{
    private static final boolean dbg = false;
    public static final String SHOW_HIDDEN_FILES = "show_hidden_files";
    public static final String SHOW_HIDDEN_DIRECTORIES = "show_hidden_directories";
    public static final String SINGLE_COLUMN = "single_column";
    public static final String ICONS_FOR_FOLDERS = "icons_for_folders";
    public static final String DONT_ZOOM_SMALL_IMAGES = "dont_zoom_small_images";
    public static final String VERTICAL_SCROLL_INVERTED = "vertical_scroll_inverted";
    public static final String SHOW_ICONS = "show_icons";
    public static final String MONITOR_BROWSED_FOLDERS = "monitor_browsed_folders";
    public static final String FUSK_IS_ACTIVE = "fusk_is_active";
    public static final String DING_IS_ON = "play_ding_when_long_operations_end";
    public static final String ESCAPE_FAST_EXIT = "escape_fast_exit";
    public static final String ENABLE_SHIFT_D_IS_SURE_DELETE = "enable_shift_d_is_sure_delete";

    public static final String AUTO_PURGE_DISK_CACHES = "AUTO_PURGE_DISK_CACHES";

    private static final String SHOW_FFMPEG_INSTALL_WARNING = "SHOW_FFMPEG_INSTALL_WARNING";
    private static final String SHOW_GraphicsMagick_INSTALL_WARNING = "SHOW_GraphicsMagick_INSTALL_WARNING";

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
    public static boolean get_show_ffmpeg_install_warning()
    //**********************************************************
    {
        String s = Non_booleans.get_main_properties_manager().get(SHOW_FFMPEG_INSTALL_WARNING);
        if (s == null) {
            Non_booleans.get_main_properties_manager().set(SHOW_FFMPEG_INSTALL_WARNING, "true");
            return true;
        }
        else
        {
            return Boolean.parseBoolean(s);
        }
    }
    //**********************************************************
    public static void set_show_ffmpeg_install_warning(boolean b)
    //**********************************************************
    {
        Non_booleans.get_main_properties_manager().set(SHOW_FFMPEG_INSTALL_WARNING, String.valueOf(b));
    }

    //**********************************************************
    public static boolean get_show_GraphicsMagick_install_warning()
    //**********************************************************
    {
        String s = Non_booleans.get_main_properties_manager().get(SHOW_GraphicsMagick_INSTALL_WARNING);
        if (s == null) {
            Non_booleans.get_main_properties_manager().set(SHOW_GraphicsMagick_INSTALL_WARNING, "true");
            return true;
        }
        else
        {
            return Boolean.parseBoolean(s);
        }
    }
    //**********************************************************
    public static void set_show_GraphicsMagick_install_warning(boolean b)
    //**********************************************************
    {
        Non_booleans.get_main_properties_manager().set(SHOW_GraphicsMagick_INSTALL_WARNING, String.valueOf(b));
    }

    static boolean ffmpeg_popup_done = false;
    //**********************************************************
    public static void manage_show_ffmpeg_install_warning(Window owner, Logger logger)
    //**********************************************************
    {
        if ( get_show_ffmpeg_install_warning())
        {
            if ( !ffmpeg_popup_done)
            {
                ffmpeg_popup_done = true;
                String msg = "klik uses ffmpeg to support several features. It is easy and free to install ffmpeg (google it!)";
                logger.log("WARNING: " + msg);
                Jfx_batch_injector.inject(() -> {
                    if (Popups.popup_ask_for_confirmation(owner, msg,"If you do not want to see this warning about installing ffmepg again, click OK", logger)) {
                        set_show_ffmpeg_install_warning(false);
                    }
                },logger);
            }
        }
    }
    static boolean GraphicsMagick_popup_done = false;
    //**********************************************************
    public static void manage_show_GraphicsMagick_install_warning(Window owner,Logger logger)
    //**********************************************************
    {
        if ( get_show_GraphicsMagick_install_warning())
        {
            if(!GraphicsMagick_popup_done)
            {
                GraphicsMagick_popup_done = true;
                String msg = "klik uses the gm convert utility of GraphicsMagick.org to support some features. It is easy and free to install (GraphicsMagick.org)";
                logger.log("WARNING: " + msg);
                Jfx_batch_injector.inject(() -> {
                    if (Popups.popup_ask_for_confirmation(owner, msg,"If you do not want to see this warning about installing GraphicsMagick again, click OK", logger)) {
                        set_show_GraphicsMagick_install_warning(false);
                    }
                },logger);
            }
        }
    }



}
