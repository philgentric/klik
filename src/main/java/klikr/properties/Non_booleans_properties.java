// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ../audio/Audio_player_FX_UI.java
package klikr.properties;

import javafx.collections.ObservableList;
import javafx.geometry.Rectangle2D;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import klikr.util.Shared_services;
import klikr.System_info;
import klikr.util.execute.actor.Aborter;
import klikr.browser.virtual_landscape.Virtual_landscape;
import klikr.util.log.Logger;


//**********************************************************
public class Non_booleans_properties
//**********************************************************
{
    private static final boolean dbg = false;
    private static final int DEFAULT_SIZE_WARNING_MEGABYTES = 500;


    public static final int DEFAULT_ICON_SIZE = 256;
    public static final int DEFAULT_FOLDER_ICON_SIZE = 256;
    public static final int DEFAULT_VIDEO_LENGTH = 1;



    private static final int DEFAULT_NUMBER_OF_IMAGE_SIMILARITY_SERVERS = System_info.how_many_cores();

    // cached values


    private static int icon_size = -1;
    private static int folder_icon_size = -1;
    private static int video_length = -1;
    private static int column_width = -1;
    private static Color custom_color = null;


    //**********************************************************
    public static int get_number_of_image_similarity_servers(Window owner)
    //**********************************************************
    {
        return get_int(String_constants.NUMBER_OF_IMAGE_SIMILARITY_SERVERS,DEFAULT_NUMBER_OF_IMAGE_SIMILARITY_SERVERS,owner);
    }

    //**********************************************************
    public static void set_number_of_image_similarity_servers(int value, Window owner)
    //**********************************************************
    {
        icon_size = value;
        set_int(value,String_constants.NUMBER_OF_IMAGE_SIMILARITY_SERVERS, owner);
    }


    //**********************************************************
    public static int get_icon_size(Window owner)
    //**********************************************************
    {
        if (icon_size > 0) return icon_size;
        icon_size = get_int(String_constants.ICON_SIZE,DEFAULT_ICON_SIZE,owner);
        return icon_size;
    }


    //**********************************************************
    public static void set_icon_size(int value, Window owner)
    //**********************************************************
    {
        icon_size = value;
        set_int(value,String_constants.ICON_SIZE, owner);
    }

    //**********************************************************
    public static int get_column_width(Window owner)
    //**********************************************************
    {
        if (column_width > 0) return column_width;
        column_width = get_int(String_constants.COLUMN_WIDTH, Virtual_landscape.MIN_COLUMN_WIDTH,owner);
        return column_width;
    }

    //**********************************************************
    public static void set_column_width(int l, Window owner)
    //**********************************************************
    {
        column_width = l;
        set_int(l,String_constants.COLUMN_WIDTH, owner);
    }

    //**********************************************************
    public static int get_animated_gif_duration_for_a_video(Window owner)
    //**********************************************************
    {
        if (video_length > 0) return video_length;
        // first time, we look it up on disk
        video_length = get_int(String_constants.VIDEO_SAMPLE_LENGTH,DEFAULT_VIDEO_LENGTH,owner);
        return video_length;
    }

    //**********************************************************
    public static void set_animated_gif_duration_for_a_video(int l,Window owner)
    //**********************************************************
    {
        video_length = l;
        set_int(l,String_constants.VIDEO_SAMPLE_LENGTH, owner);
    }


    //**********************************************************
    public static Integer get_int(
            String ID,
            Integer default_value,
            Window owner)
    //**********************************************************
    {
        Integer returned;
        // first time, we look it up on disk
        String video_length_s = Shared_services.main_properties().get(ID);
        if (video_length_s == null)
        {
            Shared_services.main_properties().set(ID, String.valueOf(default_value));
            return default_value;
        }
        else
        {
            double d_video_length = Double.parseDouble(video_length_s);
            returned  = (int) d_video_length;
        }
        return returned;
    }

    //**********************************************************
    public static void set_int(int val, String ID, Window owner)
    //**********************************************************
    {
        Shared_services.main_properties().set(ID, String.valueOf(val));
    }

    //**********************************************************
    public static Double get_double(
            String ID,
            Double default_value,
            Window owner)
    //**********************************************************
    {
        Double returned;
        // first time, we look it up on disk
        String s = Shared_services.main_properties().get(ID);
        if (s == null)
        {
            Shared_services.main_properties().set(ID, String.valueOf(default_value));
            return default_value;
        }
        else
        {
            returned = Double.parseDouble(s);
        }
        return returned;
    }

    //**********************************************************
    public static void set_double(double val, String ID, Window owner)
    //**********************************************************
    {
        Shared_services.main_properties().set(ID, String.valueOf(val));
    }




    //**********************************************************
    public static Rectangle2D get_window_bounds(String key, Window owner)
    //**********************************************************
    {
        File_storage pm = Shared_services.main_properties();
        String x_s = pm.get(key + String_constants.SCREEN_TOP_LEFT_X);
        if (x_s == null) return default_rectangle();
        double x = Double.parseDouble(x_s);

        String y_s = pm.get(key + String_constants.SCREEN_TOP_LEFT_Y);
        if (y_s == null) return default_rectangle();
        double y = Double.parseDouble(y_s);

        String w_s = pm.get(key + String_constants.SCREEN_WIDTH);
        if (w_s == null) return default_rectangle();
        double w = Double.parseDouble(w_s);

        String h_s = pm.get(key + String_constants.SCREEN_HEIGHT);
        if (h_s == null) return default_rectangle();
        double h = Double.parseDouble(h_s);

        Rectangle2D target = new Rectangle2D(x, y, w, h);
        // before returning this rectangle, let us check if there is a screen that contains this rectangle

        ObservableList<Screen> all_screens = Screen.getScreens();
        for ( Screen s : all_screens)
        {
            Rectangle2D screen_bounds = s.getVisualBounds();
            if ( screen_bounds.getMinX() > target.getMinX())
            {
                //System.out.println("from file minX not ok: "+screen_bounds.getMinX() +">"+ target.getMinX());
                continue;
            }
            else
            {
                //System.out.println("from file minX ok: "+screen_bounds.getMinX() +"<="+ target.getMinX());
            }
            if ( screen_bounds.getMaxX() < target.getMaxX())
            {
                //System.out.println("from file maxX not ok: "+screen_bounds.getMaxX() +"<"+ target.getMaxX());
                continue;
            }
            else
            {
                //System.out.println("from file maxX ok: "+screen_bounds.getMaxX() +">="+ target.getMaxX());
            }

            if ( screen_bounds.getMinY() > target.getMinY())
            {
                //System.out.println("from file minY not ok: "+screen_bounds.getMinY() +">"+ target.getMinY());
                continue;
            }
            else
            {
                //System.out.println("from file minY ok: "+screen_bounds.getMinY() +"<="+ target.getMinY());
            }
            if ( screen_bounds.getMaxY() < target.getMaxY())
            {
                //System.out.println("from file maxY not ok: "+screen_bounds.getMaxY() +"<"+ target.getMaxY());
                continue;
            }
            else
            {
                //System.out.println("from file maxY ok: "+screen_bounds.getMaxY() +">="+ target.getMaxY());
            }



            //System.out.println(" from file  bounds " + target + " are within screen bounds " + screen_bounds);
            return target; // the stage bounds are inside this screen, so we can return the target rectangle

        }

        // if we arrive here, the bounds are not valid, so we need to compute the bounds based on the current stage

        System.out.println("❗ WARNING: from file bounds " + target  + " do not fit with any screen, changing the target");

        // use the first screen available (e.g. the main screen, the laptop screen, etc.)
        for ( Screen s : all_screens)
        {
            System.out.println("✅ forcing screen bounds to: " + s.getVisualBounds());
            return s.getVisualBounds();
        }
        // normally never happens?
        System.out.println("❌ SHOULD NOT HAPPEN: no screen found, using default rectangle");
        return new Rectangle2D(0,0,800,600); // default rectangle
    }

    //**********************************************************
    public static void save_window_bounds(Stage stage, String key, Logger logger)
    //**********************************************************
    {
        Rectangle2D r = new Rectangle2D(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
        if (dbg) logger.log("saving bounds=" + r);
        File_storage pm = Shared_services.main_properties();
        pm.set(key + String_constants.SCREEN_TOP_LEFT_X, String.valueOf(r.getMinX()));
        pm.set(key + String_constants.SCREEN_TOP_LEFT_Y, String.valueOf(r.getMinY()));
        pm.set(key + String_constants.SCREEN_WIDTH, String.valueOf(r.getWidth()));
        pm.set(key + String_constants.SCREEN_HEIGHT, String.valueOf(r.getHeight()));
    }


    //**********************************************************
    private static Rectangle2D default_rectangle()
    //**********************************************************
    {
        return new Rectangle2D(0, 0, 800, 600);
    }


/*
    //**********************************************************
    public static Color get_custom_color(Window owner)
    //**********************************************************
    {
        if (custom_color != null) return custom_color;
        // first time, we look it up on disk
        String custom_color_s = Shared_services.main_properties().get(CUSTOM_COLOR);
        if (custom_color_s == null)
        {
            custom_color = Color.valueOf(DEFAULT_CUSTOM_COLOR);
        }
        else
        {
            custom_color = Color.valueOf(custom_color_s);
        }
        Shared_services.main_properties().set(CUSTOM_COLOR, custom_color.toString());
        return custom_color;
    }

    //**********************************************************
    public static void set_custom_color(Color c, Window owner)
    //**********************************************************
    {
        custom_color = c;
        Shared_services.main_properties().set(CUSTOM_COLOR, custom_color.toString());
    }
*/

    //**********************************************************
    public static int get_folder_icon_size(Window owner)
    //**********************************************************
    {
        if (folder_icon_size > 0) return folder_icon_size;
        folder_icon_size = get_int(String_constants.FOLDER_ICON_SIZE,DEFAULT_FOLDER_ICON_SIZE,owner);
        return folder_icon_size;
    }



    //**********************************************************
    public static void set_cache_size_limit_warning_megabytes_fx(int warning_megabytes, Window owner)
    //**********************************************************
    {
        Shared_services.main_properties().set(String_constants.DISK_CACHE_SIZE_WARNING_MEGABYTES, String.valueOf(warning_megabytes));
    }

    //**********************************************************
    public static int get_folder_warning_size(Window owner)
    //**********************************************************
    {
        return get_int(String_constants.DISK_CACHE_SIZE_WARNING_MEGABYTES,DEFAULT_SIZE_WARNING_MEGABYTES,owner);
    }

    //**********************************************************
    public static void set_folder_icon_size(int value, Window owner)
    //**********************************************************
    {
        folder_icon_size = value;
        set_int(value,String_constants.FOLDER_ICON_SIZE,owner);
    }


    static double font_size_cache = -1.0;

    //**********************************************************
    public static double get_font_size(Window owner,Logger logger)
    //**********************************************************
    {
        if (font_size_cache > 0) return font_size_cache;
        font_size_cache = get_double(String_constants.FONT_SIZE,16.0,owner);
        return font_size_cache;
    }


    //**********************************************************
    public static void set_font_size(double value, Window owner)
    //**********************************************************
    {
        set_double(value,String_constants.FONT_SIZE,owner);
    }


    //**********************************************************
    public static String get_language_key(Window owner)
    //**********************************************************
    {
        String s = Shared_services.main_properties().get(String_constants.LANGUAGE_KEY);
        if (s == null) {
            s = "English";
            Shared_services.main_properties().set(String_constants.LANGUAGE_KEY, s);
        }
        return s;
    }



    //**********************************************************
    public static void force_reload_from_disk(Window owner)
    //**********************************************************
    {
        Shared_services.main_properties().reload_from_disk();
    }


    //**********************************************************
    public static void save_java_VM_max_RAM(int value, Window owner, Logger logger)
    //**********************************************************
    {
        File_storage_using_Properties f = new File_storage_using_Properties(String_constants.PURPOSE, String_constants.RAM_FILENAME,true,owner,new Aborter("ram", logger), logger);
        f.set(String_constants.JAVA_VM_MAX_RAM, "" + value);
    }
    //**********************************************************
    public static int get_java_VM_max_RAM(Window owner, Logger logger)
    //**********************************************************
    {
        File_storage_using_Properties f = new File_storage_using_Properties(String_constants.PURPOSE, String_constants.RAM_FILENAME, true, owner,new Aborter("ram", logger), logger);
        String s = f.get(String_constants.JAVA_VM_MAX_RAM);
        if (s == null)
        {
            logger.log("warning, no java VM max RAM found, defaulting to 1 GBytes");
            return 1; // default to 1 GBytes
        }

        int value = 0;
        try {
            value = Integer.valueOf(s);
        }
        catch (NumberFormatException e)
        {
            logger.log("WARNING: cannot parse volume->" + s + "<-");
            return 1;
        }
        return value;
    }

}