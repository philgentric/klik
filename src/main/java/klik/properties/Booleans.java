//SOURCES ../audio/Audio_player_frame.java
package klik.properties;

import javafx.stage.Window;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import klik.util.ui.Jfx_batch_injector;
import klik.util.ui.Popups;

import java.util.HashMap;
import java.util.Map;


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
    public static final String RAM_DISK_IS_ACTIVE = "ram_disk_is_active";
    public static final String DING_IS_ON = "play_ding_when_long_operations_end";
    public static final String ESCAPE_FAST_EXIT = "escape_fast_exit";
    public static final String ENABLE_SHIFT_D_IS_SURE_DELETE = "enable_shift_d_is_sure_delete";

    public static final String AUTO_PURGE_DISK_CACHES = "AUTO_PURGE_DISK_CACHES";

    private static final String SHOW_FFMPEG_INSTALL_WARNING = "SHOW_FFMPEG_INSTALL_WARNING";
    private static final String SHOW_GraphicsMagick_INSTALL_WARNING = "SHOW_GraphicsMagick_INSTALL_WARNING";


    public static Map<String , Boolean> boolean_properties_cache;




    //**********************************************************
    public static void set_boolean(String s, boolean b, Logger logger)
    //**********************************************************
    {
        if (boolean_properties_cache == null)
        {
            get_all_booleans();
        }
        if (boolean_properties_cache == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("FATAL: boolean_properties_cache==null"));
        }
        boolean_properties_cache.put(s, b);
        Properties_manager pm = Non_booleans.get_main_properties_manager();
        pm.add(s, String.valueOf(b));

    }
    //**********************************************************
    public static boolean get_boolean(String s)
    //**********************************************************
    {
        if (boolean_properties_cache == null)
        {
            get_all_booleans();
        }
        if (boolean_properties_cache == null)
        {
            System.out.println(Stack_trace_getter.get_stack_trace("FATAL: boolean_properties_cache==null"));
        }
        Boolean tmp = boolean_properties_cache.get(s);
        if (tmp == null)
        {
            System.out.println("WARNING: boolean_properties_cache.get("+s+") == null");
            Properties_manager pm = Non_booleans.get_main_properties_manager();

            pm.add(s, "false");
            boolean_properties_cache.put(s, false);
            return false;
        }
        return tmp;
    }


    //**********************************************************
    public static Map<String, Boolean> get_all_booleans()
    //**********************************************************
    {
        boolean_properties_cache = new HashMap<>();
        Properties_manager pm = Non_booleans.get_main_properties_manager();

        for ( String k : pm.get_all_keys())
        {
            String v = pm.get(k);
            if (v == null)
            {
                boolean_properties_cache.put(k, false);
                pm.add(k, String.valueOf(false));
                System.out.println("SAVING MISSING boolean value for " + k + " = " + v+" (false)");
                continue;
            }
            Boolean b = Boolean.parseBoolean(v);
            if ( b)
            {
                boolean_properties_cache.put(k, true);
                if ( dbg) System.out.println("boolean value for " + k + " = " + v+" (true)");
                continue;
            }
            if ( v.trim().toLowerCase().equals("false"))
            {
                boolean_properties_cache.put(k, false);
                if ( dbg) System.out.println("boolean value for " + k + " = " + v+" (false)");
            }

            if ( dbg) System.out.println(("ignoring as not a boolean " + k + " = " + v));
            //System.out.println(Stack_trace_getter.get_stack_trace("NOT A BOOLEAN value for " + k + " = " + v));
        }
        return boolean_properties_cache;
    }


    //**********************************************************
    public static boolean get_show_ffmpeg_install_warning()
    //**********************************************************
    {
        String s = Non_booleans.get_main_properties_manager().get(SHOW_FFMPEG_INSTALL_WARNING);
        if (s == null) {
            Non_booleans.get_main_properties_manager().add(SHOW_FFMPEG_INSTALL_WARNING, "true");
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
        Non_booleans.get_main_properties_manager().add(SHOW_FFMPEG_INSTALL_WARNING, String.valueOf(b));
    }

    //**********************************************************
    public static boolean get_show_GraphicsMagick_install_warning()
    //**********************************************************
    {
        String s = Non_booleans.get_main_properties_manager().get(SHOW_GraphicsMagick_INSTALL_WARNING);
        if (s == null) {
            Non_booleans.get_main_properties_manager().add(SHOW_GraphicsMagick_INSTALL_WARNING, "true");
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
        Non_booleans.get_main_properties_manager().add(SHOW_GraphicsMagick_INSTALL_WARNING, String.valueOf(b));
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
