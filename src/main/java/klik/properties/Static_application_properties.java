package klik.properties;

import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.geometry.Rectangle2D;

import klik.browser.icons.Icon_manager;
import klik.files_and_paths.Files_and_Paths;
import klik.util.Logger;
import klik.util.Popups;
import klik.util.Stack_trace_getter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

;
//**********************************************************
public class Static_application_properties
//**********************************************************
{
    private static final boolean dbg = false;
    public static final String SORT_FILES_BY = "sort_files_by";
    private static final String LEVEL2 = "LEVEL2";
    public static Properties_manager the_properties_manager;
    private static int icon_size = -1;
    private static int video_length = -1;
    private static int button_width = -1;
    public static final int DEFAULT_ICON_SIZE = 256;
    public static final int DEFAULT_VIDEO_LENGTH = 1;

    public static final String ULTIM = "_ultim"; // must be lowercase because we test name.toLowerCase.contains("_ultim")
    public static final String SHOW_FOLDER_SIZE = "show_folder_size";
    public static final String SHOW_HIDDEN_FILES = "show_hidden_files";
    public static final String SHOW_HIDDEN_DIRECTORIES = "show_hidden_directories";
    private static final String ENABLE_FUSK = "enable_fusk";
    private static final String SHOW_FFMPEG_INSTALL_WARNING = "SHOW_FFMPEG_INSTALL_WARNING";
    private static final String MAX_EXCLUDED_KEYWORDS = "max_number_of_excluded_keywords";
    public static final String EXCLUDED_KEYWORD_PREFIX = "excluded_keyword_";
    public static final String ICON_SIZE_KEYWORD = "ICON_SIZE";
    public static final String VIDEO_LENGTH_KEYWORD = "VIDEO_SAMPLE_LENGTH";
    public static final String LANGUAGE_KEYWORD = "LANGUAGE";
    public static final String FONT_SIZE_KEYWORD = "FONT_SIZE_KEYWORD";
    public static final String BUTTON_WIDTH_KEYWORD = "BUTTON_WIDTH_KEYWORD";
    public static final String VERTICAL_SCROLL_INVERTED = "vertical_scroll_inverted";
    public static final String ESCAPE = "escape_fast_exit";
    public static final String SHOW_ICONS = "show_icons";
    public static final String CONF_DIR = ".klik";//+File.separator;
    public static final String PROPERTIES_FILENAME = "klik_properties.txt";
    public static final String TRASH_DIR = "klik_trash";
    public static final String ICON_CACHE_DIR = "klik_icon_cache";
    public static final String FOLDER_ICON_CACHE_DIR = "klik_folder_icon_cache";
    public static final String USER_HOME = "user.home";
    public static final String SINGLE_COLUMN = "single_column";
    public static final String ICONS_FOR_FOLDERS = "icons_for_folders";
    public static final String MONITOR_BROWSED_FOLDERS = "monitor_browsed_folders";
    public static final String SCREEN_TOP_LEFT_X = "screen_top_left_x";
    public static final String SCREEN_TOP_LEFT_Y = "screen_top_left_y";
    public static final String SCREEN_WIDTH = "screen_width";
    public static final String SCREEN_HEIGHT = "screen_height";


    //**********************************************************
    public static Properties_manager get_properties_manager(Logger logger)
    //**********************************************************
    {

        if (the_properties_manager == null)
        {
            String home = System.getProperty(USER_HOME);
            Path p = Paths.get(home, CONF_DIR, PROPERTIES_FILENAME);
            the_properties_manager = new Properties_manager(p, logger);
        }
        return the_properties_manager;
    }


    //**********************************************************
    public static File_sorter get_sort_files_by(Logger logger)
    //**********************************************************
    {
        String s = Static_application_properties.get_properties_manager(logger).get(SORT_FILES_BY);
        if (s == null) {
            Static_application_properties.get_properties_manager(logger).save_unico(SORT_FILES_BY, File_sorter.NAME.name(), false);
            return File_sorter.NAME;
        }
        else
        {
            return File_sorter.valueOf(s);
        }
    }

    //**********************************************************
    public static void set_sort_files_by(File_sorter b, Logger logger)
    //**********************************************************
    {
        Static_application_properties.get_properties_manager(logger).save_unico(SORT_FILES_BY, b.name(), false);
    }


    //**********************************************************
    public static boolean get_show_hidden_files(Logger logger)
    //**********************************************************
    {
        String s = get_properties_manager(logger).get(SHOW_HIDDEN_FILES);
        if (s == null) {
            get_properties_manager(logger).save_unico(SHOW_HIDDEN_FILES, "false", false);
            return false;
        } else {
            return Boolean.parseBoolean(s);
        }
    }


    //**********************************************************
    public static void set_monitor_browsed_folders(boolean b, Logger logger)
    //**********************************************************
    {
        get_properties_manager(logger).save_unico(MONITOR_BROWSED_FOLDERS, String.valueOf(b), false);
    }

    //**********************************************************
    public static boolean get_monitor_browsed_folders(Logger logger)
    //**********************************************************
    {
        String s = get_properties_manager(logger).get(MONITOR_BROWSED_FOLDERS);
        if (s == null) {
            get_properties_manager(logger).save_unico(MONITOR_BROWSED_FOLDERS, "false", false);
            return false;
        } else {
            return Boolean.parseBoolean(s);
        }
    }


    //**********************************************************
    public static boolean get_show_hidden_directories(Logger logger)
    //**********************************************************
    {
        String s = get_properties_manager(logger).get(SHOW_HIDDEN_DIRECTORIES);
        if (s == null) {
            get_properties_manager(logger).save_unico(SHOW_HIDDEN_DIRECTORIES, "false", false);
            return false;
        } else {
            return Boolean.parseBoolean(s);
        }
    }

    //**********************************************************
    public static boolean get_enable_fusk(Logger logger)
    //**********************************************************
    {
        String s = get_properties_manager(logger).get(ENABLE_FUSK);
        if (s == null) {
            get_properties_manager(logger).save_unico(ENABLE_FUSK, "false", false);
            return false;
        } else {
            return Boolean.parseBoolean(s);
        }
    }
    //**********************************************************
    public static void set_enable_fusk(boolean b, Logger logger)
    //**********************************************************
    {
        get_properties_manager(logger).save_unico(ENABLE_FUSK, String.valueOf(b), false);
    }

    //**********************************************************
    public static void set_show_hidden_files(boolean b, Logger logger)
    //**********************************************************
    {
        get_properties_manager(logger).save_unico(SHOW_HIDDEN_FILES, String.valueOf(b), false);
    }

    //**********************************************************
    public static void set_show_hidden_directories(boolean b, Logger logger)
    //**********************************************************
    {
        get_properties_manager(logger).save_unico(SHOW_HIDDEN_DIRECTORIES, String.valueOf(b), false);
    }


    //**********************************************************
    public static boolean get_show_folder_size(Logger logger)
    //**********************************************************
    {
        String s = get_properties_manager(logger).get(SHOW_FOLDER_SIZE);
        if (s == null) {
            get_properties_manager(logger).save_unico(SHOW_FOLDER_SIZE, "true", false);
            return false;
        } else {
            return Boolean.parseBoolean(s);
        }
    }

    //**********************************************************
    public static void set_show_folder_size(boolean b, Logger logger)
    //**********************************************************
    {
        get_properties_manager(logger).save_unico(SHOW_FOLDER_SIZE, String.valueOf(b), false);
    }


    //**********************************************************
    public static Rectangle2D get_bounds(Logger logger)
    //**********************************************************
    {
        Properties_manager pm = get_properties_manager(logger);
        String x_s = pm.get(SCREEN_TOP_LEFT_X);
        if (x_s == null) return null;
        double x = Double.parseDouble(x_s);
        String y_s = pm.get(SCREEN_TOP_LEFT_Y);
        if (y_s == null) return null;
        double y = Double.parseDouble(y_s);
        String w_s = pm.get(SCREEN_WIDTH);
        if (w_s == null) return null;
        double w = Double.parseDouble(w_s);
        String h_s = pm.get(SCREEN_HEIGHT);
        if (h_s == null) return null;
        double h = Double.parseDouble(h_s);
        return new Rectangle2D(x, y, w, h);
    }

    //**********************************************************
    public static void save_bounds(Rectangle2D r, Logger logger)
    //**********************************************************
    {
        logger.log("saving bounds="+r);
        Properties_manager pm = get_properties_manager(logger);
        pm.save_unico(SCREEN_TOP_LEFT_X, String.valueOf(r.getMinX()), false);
        pm.save_unico(SCREEN_TOP_LEFT_Y, String.valueOf(r.getMinY()), false);
        pm.save_unico(SCREEN_WIDTH, String.valueOf(r.getWidth()), false);
        pm.save_unico(SCREEN_HEIGHT, String.valueOf(r.getHeight()), false);
    }

    // returns a directory using that relative name
    //**********************************************************
    public static Path get_trash_dir(Logger logger)
    //**********************************************************
    {
        Path trash_dir = get_absolute_dir(logger, TRASH_DIR);
        if ( trash_dir == null)
        {
            logger.log("PANIC: trash dir unknown");
            return null;
        }
        if (dbg) logger.log("trash_dir file=" + trash_dir.toAbsolutePath());
        return trash_dir;
    }

    // returns a directory using that relative name
    //**********************************************************
    public static Path get_absolute_dir(Logger logger, String relative_dir_name)
    //**********************************************************
    {
        if (dbg) logger.log("dir_name=" + relative_dir_name);
        String home = System.getProperty(USER_HOME);
        if (dbg) logger.log("user home =" + home);

        Path conf_dir1 = Paths.get(home,CONF_DIR);
        if (!conf_dir1.toFile().exists())
        {
            try {
                Files.createDirectory(conf_dir1);

                // do it once more, this way icons don't show up in $home/.klik
                // privacy violation
                // when browsing $home
            } catch (IOException e) {
                String err = " Attempt to create a directory named->" + conf_dir1.toAbsolutePath() + "<- failed";
                logger.log_stack_trace(err);
                Popups.popup_Exception(e,300,err,logger);
                return null;
            }
        }

        Path conf_dir2 = Paths.get(conf_dir1.toString(),CONF_DIR+"_privacy_screen");
        if (!conf_dir2.toFile().exists())
        {
            try {
                Files.createDirectory(conf_dir2);
            } catch (IOException e) {
                String err = " Attempt to create a directory named->" + conf_dir2.toAbsolutePath() + "<- failed";
                logger.log_stack_trace(err);
                Popups.popup_Exception(e,300,err,logger);
                return null;
            }
        }


        Path returned = Paths.get(conf_dir2.toAbsolutePath().toString(), relative_dir_name);
        if (dbg) logger.log("get_dir returns=" + returned.toAbsolutePath());
        if (!Files.exists(returned)) {
            try {
                Files.createDirectory(returned);
                return returned;
            } catch (IOException e) {
                String err = " Attempt to create a directory named->" + returned.toAbsolutePath() + "<- failed";
                logger.log_stack_trace(err);
                Popups.popup_Exception(e, 300," Attempt to create dir named->" + returned.toAbsolutePath() + "<- failed",logger);
                return null;
            }
        }
        if (dbg) {
            String err = "directory named->" + returned.toAbsolutePath() + "<- OK";
            logger.log_stack_trace(err);
        }
        return returned;
    }


    //**********************************************************
    public static int get_animated_gif_duration_for_a_video(Logger logger)
    //**********************************************************
    {
        if (video_length > 0) return video_length;
        // first time, we look it up on disk
        String video_length_s = get_properties_manager(logger).get(VIDEO_LENGTH_KEYWORD);
        if (video_length_s == null) {
            video_length = DEFAULT_VIDEO_LENGTH;
        } else {
            double d_video_length = Double.parseDouble(video_length_s);
            video_length = (int) d_video_length;
        }
        get_properties_manager(logger).save_unico(VIDEO_LENGTH_KEYWORD, String.valueOf(video_length), false);
        //if (icon_manager != null) icon_manager.icon_size_is_now(icon_size.get_icon_size());
        return video_length;
    }

    //**********************************************************
    public static void set_animated_gif_duration_for_a_video(int l, Logger logger)
    //**********************************************************
    {
        video_length = l;
        get_properties_manager(logger).save_unico(VIDEO_LENGTH_KEYWORD, String.valueOf(video_length), false);
    }

    //**********************************************************
    public static int get_button_width(Logger logger)
    //**********************************************************
    {
        if (button_width > 0) return button_width;
        // first time, we look it up on disk
        String video_length_s = get_properties_manager(logger).get(BUTTON_WIDTH_KEYWORD);
        if (video_length_s == null) {
            button_width = Icon_manager.MIN_BUTTON_WIDTH;
        } else {
            double local = Double.parseDouble(video_length_s);
            button_width = (int) local;
        }
        get_properties_manager(logger).save_unico(BUTTON_WIDTH_KEYWORD, String.valueOf(button_width), false);
        return button_width;
    }

    //**********************************************************
    public static void set_button_width(int l, Logger logger)
    //**********************************************************
    {
        button_width = l;
        get_properties_manager(logger).save_unico(BUTTON_WIDTH_KEYWORD, String.valueOf(button_width), false);
    }




    //**********************************************************
    public static int get_icon_size(Logger logger)
    //**********************************************************
    {
        if (icon_size > 0) return icon_size;
        // first time, we look it up on disk
        String icon_size_s = get_properties_manager(logger).get(ICON_SIZE_KEYWORD);
        if (icon_size_s == null) {
            icon_size = DEFAULT_ICON_SIZE;
        } else {
            double d_icon_size = Double.parseDouble(icon_size_s);
            icon_size = (int) d_icon_size;
        }
        get_properties_manager(logger).save_unico(ICON_SIZE_KEYWORD, String.valueOf(icon_size), false);
        //if (icon_manager != null) icon_manager.icon_size_is_now(icon_size.get_icon_size());
        return icon_size;
    }


    //**********************************************************
    public static void set_icon_size(int target_size, Logger logger)
    //**********************************************************
    {
        icon_size = target_size;
        get_properties_manager(logger).save_unico(ICON_SIZE_KEYWORD, String.valueOf(icon_size), false);
    }

    static double font_size_cache = -1.0;

    //**********************************************************
    public static double get_font_size(Logger logger)
    //**********************************************************
    {
        if (font_size_cache > 0) return font_size_cache;
        double font_size = 16; // this is the default immediately after installing or after erasing properties
        // first time, we look it up on disk
        String font_size_s = get_properties_manager(logger).get(FONT_SIZE_KEYWORD);
        if (font_size_s != null) {
            try {
                font_size = Double.parseDouble(font_size_s);
            } catch (NumberFormatException e) {
                logger.log(Stack_trace_getter.get_stack_trace_for_throwable(e));
            }
        }
        get_properties_manager(logger).save_unico(FONT_SIZE_KEYWORD, String.valueOf(font_size), false);
        font_size_cache = font_size;
        return font_size;
    }


    //**********************************************************
    public static void set_font_size(double target_size, Logger logger)
    //**********************************************************
    {
        font_size_cache = target_size;
        get_properties_manager(logger).save_unico(FONT_SIZE_KEYWORD, String.valueOf(target_size), false);
    }


    //**********************************************************
    public static int get_excluded_keyword_list_max_size(Logger logger)
    //**********************************************************
    {
        int max = 100;
        String s = get_properties_manager(logger).get(MAX_EXCLUDED_KEYWORDS, String.valueOf(max));
        try {
            max = Integer.parseInt(s);
        } catch (NumberFormatException e) {
            logger.log(" ERROR setting MAX_EXCLUDED_KEYWORDS ... default 100 applied" + e);
        }
        return max;
    }

    //**********************************************************
    public static String get_language(Logger logger)
    //**********************************************************
    {
        String s = get_properties_manager(logger).get(LANGUAGE_KEYWORD);
        if (s == null) {
            s = "english-US";
        }
        get_properties_manager(logger).save_unico(LANGUAGE_KEYWORD, s, false);
        return s;
    }

    //**********************************************************
    public static void set_language(String s, Logger logger)
    //**********************************************************
    {
        get_properties_manager(logger).save_unico(LANGUAGE_KEYWORD, s, false);

    }

    //**********************************************************
    public static boolean get_show_icons(Logger logger)
    //**********************************************************
    {
        String s = Static_application_properties.get_properties_manager(logger).get(SHOW_ICONS);
        if (s == null) {
            // happens after install or after erasing properties
            Static_application_properties.get_properties_manager(logger).save_unico(SHOW_ICONS, "true", false);
            return false;
        } else {
            return Boolean.parseBoolean(s);
        }
    }

    //**********************************************************
    public static void set_show_icons(boolean b, Logger logger)
    //**********************************************************
    {
        Static_application_properties.get_properties_manager(logger).save_unico(SHOW_ICONS, String.valueOf(b), false);

    }

    //**********************************************************
    public static void set_vertical_scroll_inverted(boolean b, Logger logger)
    //**********************************************************
    {
        Static_application_properties.get_properties_manager(logger).save_unico(VERTICAL_SCROLL_INVERTED, String.valueOf(b), false);
    }

    //**********************************************************
    public static boolean get_vertical_scroll_inverted(Logger logger)
    //**********************************************************
    {
        String s = Static_application_properties.get_properties_manager(logger).get(VERTICAL_SCROLL_INVERTED);
        if (s == null) {
            Static_application_properties.get_properties_manager(logger).save_unico(VERTICAL_SCROLL_INVERTED, "true", false);
            return true;
        } else {
            return Boolean.parseBoolean(s);
        }
    }


    //**********************************************************
    public static boolean is_this_trash(Path dir, Logger logger)
    //**********************************************************
    {
        Path trash = get_trash_dir(logger);
        return Files_and_Paths.is_same_path(trash, dir, logger);
    }

    //**********************************************************
    public static void set_escape(boolean b, Logger logger)
    //**********************************************************
    {
        Static_application_properties.get_properties_manager(logger).save_unico(ESCAPE, String.valueOf(b), false);
    }

    //**********************************************************
    public static boolean get_escape(Logger logger)
    //**********************************************************
    {
        String s = Static_application_properties.get_properties_manager(logger).get(ESCAPE);
        if (s == null) {
            Static_application_properties.get_properties_manager(logger).save_unico(ESCAPE, "true", false);
            return true;
        } else {
            return Boolean.parseBoolean(s);
        }
    }



    //**********************************************************
    public static boolean get_show_icons_for_folders(Logger logger)
    //**********************************************************
    {
        String s = Static_application_properties.get_properties_manager(logger).get(ICONS_FOR_FOLDERS);
        if (s == null) {
            Static_application_properties.get_properties_manager(logger).save_unico(ICONS_FOR_FOLDERS, "false", false);
            return false;
        }
        else
        {
            return Boolean.parseBoolean(s);
        }
    }
    //**********************************************************
    public static void set_show_icons_for_folders(boolean b, Logger logger)
    //**********************************************************
    {
        Static_application_properties.get_properties_manager(logger).save_unico(ICONS_FOR_FOLDERS, String.valueOf(b), false);
    }

    //**********************************************************
    public static boolean get_single_column(Logger logger)
    //**********************************************************
    {
        String s = Static_application_properties.get_properties_manager(logger).get(SINGLE_COLUMN);
        if (s == null) {
            Static_application_properties.get_properties_manager(logger).save_unico(SINGLE_COLUMN, "false", false);
            return false;
        }
        else
        {
            return Boolean.parseBoolean(s);
        }
    }
    //**********************************************************
    public static void set_single_column(boolean b, Logger logger)
    //**********************************************************
    {
        Static_application_properties.get_properties_manager(logger).save_unico(SINGLE_COLUMN, String.valueOf(b), false);
    }

    public static List<String> get_cleanup_tokens(Logger logger)
    {
        return Static_application_properties.get_properties_manager(logger).get_values_for_base("CLEANUP_TOKEN_");
    }

    //**********************************************************
    public static boolean get_show_ffmpeg_install_warning(Logger logger)
    //**********************************************************
    {
        String s = Static_application_properties.get_properties_manager(logger).get(SHOW_FFMPEG_INSTALL_WARNING);
        if (s == null) {
            Static_application_properties.get_properties_manager(logger).save_unico(SHOW_FFMPEG_INSTALL_WARNING, "true", false);
            return true;
        }
        else
        {
            return Boolean.parseBoolean(s);
        }
    }
    //**********************************************************
    public static void set_show_ffmpeg_install_warning(boolean b, Logger logger)
    //**********************************************************
    {
        Static_application_properties.get_properties_manager(logger).save_unico(SHOW_FFMPEG_INSTALL_WARNING, String.valueOf(b), false);
    }

    //**********************************************************
    public static void manage_show_ffmpeg_install_warning(Stage owner, Logger logger)
    //**********************************************************
    {
        if ( Static_application_properties.get_show_ffmpeg_install_warning(logger))
        {
            String msg = "klik uses ffmpeg to support several features. It is easy and free to install ffmpeg (google it!)";
            logger.log("WARNING: " + msg);
            Platform.runLater(() -> {
                if (Popups.popup_ask_for_confirmation(owner, msg,"If you do not want to see this warning about installing ffmepg again, click OK", logger)) {
                    Static_application_properties.set_show_ffmpeg_install_warning(false,logger);
                }
            });
        }
    }

    public static boolean get_level2(Logger logger) {
        String s = Static_application_properties.get_properties_manager(logger).get(LEVEL2);
        if (s == null) {
            Static_application_properties.get_properties_manager(logger).save_unico(LEVEL2, "false", false);
            return true;
        }
        else
        {
            return Boolean.parseBoolean(s);
        }
    }

    //**********************************************************
    public static void set_level2(boolean b, Logger logger)
    //**********************************************************
    {
        Static_application_properties.get_properties_manager(logger).save_unico(LEVEL2, String.valueOf(b), false);
    }
}
