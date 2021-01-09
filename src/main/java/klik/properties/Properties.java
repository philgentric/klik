package klik.properties;

import javafx.geometry.Rectangle2D;
import klik.look.Look_and_feel;
import klik.look.Look_and_feel_plain;
import klik.look.Look_and_feel_manager;
import klik.look.Look_and_feel_shiny_black;
import klik.util.Constants;
import klik.util.Logger;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

//**********************************************************
public class Properties
//**********************************************************
{

    public static final String SHOW_GIFS_FIRST = "show_gifs_first";
    public static final String SHOW_HIDDEN_FILES = "show_hidden_files";
    public static final int DEFAULT_ICON_SIZE = 256;
    private static Properties_manager the_properties_manager;
    private static boolean dbg = false;
    private static int icon_size = -1;
    private static final String MAX_EXCLUDED_KEYWORDS = "max_number_of_excluded_keywords";
    public static final String EXCLUDED_KEYWORD_PREFIX = "excluded_keyword_";
    public static final String ICON_SIZE_KEYWORD = "ICON_SIZE";
    public static final String STYLE_KEYWORD = "STYLE";
    public static final String LANGUAGE_KEYWORD = "LANGUAGE";


    //**********************************************************
    public static Properties_manager get_properties_manager()
    //**********************************************************
    {

        if (the_properties_manager == null) {
            String home = System.getProperty(Constants.USER_HOME);
            Path p = Paths.get(home, Constants.PROPERTIES_FILENAME);
            the_properties_manager = new Properties_manager(p);
        }
        return the_properties_manager;
    }

    //**********************************************************
    public static boolean get_show_hidden_files()
    //**********************************************************
    {
        String s = get_properties_manager().get(SHOW_HIDDEN_FILES);
        if (s == null) {
            get_properties_manager().save_unico(SHOW_HIDDEN_FILES, "false");
            return false;
        } else {
            return Boolean.valueOf(s);
        }
    }

    //**********************************************************
    public static void set_show_hidden_files(boolean b)
    //**********************************************************
    {
        get_properties_manager().save_unico(SHOW_HIDDEN_FILES, "" + b);
    }

    //**********************************************************
    public static boolean get_show_gifs_first()
    //**********************************************************
    {
        String s = get_properties_manager().get(SHOW_GIFS_FIRST);
        if (s == null) {
            get_properties_manager().save_unico(SHOW_GIFS_FIRST, "false");
            return false;
        } else {
            return Boolean.valueOf(s);
        }
    }

    //**********************************************************
    public static void set_show_gifs_first(boolean b)
    //**********************************************************
    {
        get_properties_manager().save_unico(SHOW_GIFS_FIRST, "" + b);
    }

    //**********************************************************
    public static Rectangle2D get_bounds()
    //**********************************************************
    {
        Properties_manager pm = get_properties_manager();
        String x_s = pm.get(Constants.SCREEN_TOP_LEFT_X);
        if ( x_s == null) return null;
        double x = Double.valueOf(x_s);
        String y_s = pm.get(Constants.SCREEN_TOP_LEFT_Y);
        if ( y_s == null) return null;
        double y = Double.valueOf(y_s);
        String w_s = pm.get(Constants.SCREEN_WIDTH);
        if ( w_s == null) return null;
        double w = Double.valueOf(w_s);
        String h_s = pm.get(Constants.SCREEN_HEIGHT);
        if ( h_s == null) return null;
        double h = Double.valueOf(h_s);
        return new Rectangle2D(x,y,w,h);
    }
    //**********************************************************
    public static void save_bounds(Rectangle2D r)
    //**********************************************************
    {
        Properties_manager pm = get_properties_manager();
        pm.save_unico(Constants.SCREEN_TOP_LEFT_X,r.getMinX()+"");
        pm.save_unico(Constants.SCREEN_TOP_LEFT_Y,r.getMinY()+"");
        pm.save_unico(Constants.SCREEN_WIDTH,r.getWidth()+"");
        pm.save_unico(Constants.SCREEN_HEIGHT,r.getHeight()+"");
    }

    // returns a directory using that relative name
    //**********************************************************
    public static Path get_trash_dir(Logger logger)
    //**********************************************************
    {
        Path trash_dir = null;
        Properties_manager pm = get_properties_manager();
        String trash_dir_name = pm.get(Constants.TRASH_DIR_KEY);
        if (dbg) logger.log("trash_dir name=" + trash_dir_name);
        if (trash_dir_name == null) {
            // inject default
            trash_dir = get_absolute_dir(logger, Constants.TRASH_DIR_KEY);
            pm.imperative_store(Constants.TRASH_DIR_KEY, trash_dir.toAbsolutePath().toString(), true);
        } else {
            trash_dir = Paths.get(trash_dir_name);
        }
        if (dbg) logger.log("trash_dir file=" + trash_dir.toAbsolutePath());
        if (Files.exists(trash_dir) == false) {

            try {
                Files.createDirectory(trash_dir);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null,
                        " Attempt to create trash dir named->" + trash_dir.toAbsolutePath() + "<- failed",
                        "Failed", JOptionPane.INFORMATION_MESSAGE);
                return null;
            }
        }
        return trash_dir;
    }

    // returns a directory using that relative name
    //**********************************************************
    public static Path get_absolute_dir(Logger logger, String relative_dir_name)
    //**********************************************************
    {
        if (dbg) logger.log("dir_name=" + relative_dir_name);
        String p = System.getProperty(Constants.USER_HOME);
        if (dbg) logger.log("path=" + p);
        Path returned = (new File(p, relative_dir_name)).toPath();
        if (dbg) logger.log("get_dir returns=" + returned.toAbsolutePath());
        if (Files.exists(returned) == false) {
            try {
                Files.createDirectory(returned);
                return returned;
            } catch (IOException e) {
                String err = " Attempt to create a directory named->" + returned.toAbsolutePath() + "<- failed";
                logger.log(err);
                JOptionPane.showMessageDialog(null, err,
                        "Failed", JOptionPane.INFORMATION_MESSAGE);
                return null;
            }
        }
        if (dbg) {
            String err = "directory named->" + returned.toAbsolutePath() + "<- OK";
            logger.log(err);
        }
        return returned;
    }

    //**********************************************************
    public static int get_icon_size()
    //**********************************************************
    {
        if (icon_size > 0) return icon_size;
        // first time, we look it up on disk
        String icon_size_s = get_properties_manager().get(ICON_SIZE_KEYWORD);
        if (icon_size_s == null)
        {
            icon_size = DEFAULT_ICON_SIZE;
        }
        else
            {
            double d_icon_size = Double.valueOf(icon_size_s);
            icon_size = (int) d_icon_size;
        }
        get_properties_manager().save_unico(ICON_SIZE_KEYWORD, "" + icon_size);
        //if (icon_manager != null) icon_manager.icon_size_is_now(icon_size.get_icon_size());
        return icon_size;
    }


    //**********************************************************
    public static void set_icon_size(int target_size)
    //**********************************************************
    {
        icon_size = target_size;
        get_properties_manager().save_unico(ICON_SIZE_KEYWORD, "" + icon_size);
    }

    //**********************************************************
    public static Look_and_feel get_style(Logger logger)
    //**********************************************************
    {
        Look_and_feel s = null;
        String style_s = get_properties_manager().get(STYLE_KEYWORD);
        if (style_s == null)
        {
            s = new Look_and_feel_shiny_black(logger);
        }
        else
            {
            for (Look_and_feel laf : Look_and_feel_manager.registered)
            {
                if (laf.name.equals(style_s)) {
                    s = laf;
                    break;
                }
            }
        }
        if (s == null)
        {
            s = new Look_and_feel_plain(logger);
        }
        get_properties_manager().save_unico(STYLE_KEYWORD, "" + s.name);
        return s;
    }

    //**********************************************************
    public static void set_style(Look_and_feel s)
    //**********************************************************
    {
        get_properties_manager().save_unico(STYLE_KEYWORD, "" + s.name);

    }

    //**********************************************************
    public static int get_excluded_keyword_list_max_size(Logger logger)
    //**********************************************************
    {
        int max = 100;
        String s = get_properties_manager().get(MAX_EXCLUDED_KEYWORDS, "" + max);
        try {
            Integer max_ = Integer.valueOf(s);
            max = max_;
        } catch (NumberFormatException e) {
            logger.log(" ERROR setting max = 100" + e);
        }
        return max;
    }

    //**********************************************************
    public static String get_language(Logger logger)
    //**********************************************************
    {
        String s = get_properties_manager().get(LANGUAGE_KEYWORD);
        if (s == null)
        {
            s = "english-US";
        }
        get_properties_manager().save_unico(LANGUAGE_KEYWORD, "" + s);
        return s;
    }

    //**********************************************************
    public static void set_language(String s)
    //**********************************************************
    {
        get_properties_manager().save_unico(LANGUAGE_KEYWORD, "" + s);

    }
}
