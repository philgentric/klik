package klik.properties;

import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.geometry.Rectangle2D;

import klik.browser.icons.Icon_manager;
import klik.files_and_paths.Files_and_Paths;
import klik.look.Look_and_feel;
import klik.look.Look_and_feel_manager;
import klik.look.styles.Look_and_feel_light;
import klik.util.Logger;
import klik.util.Popups;
import klik.util.Stack_trace_getter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

;
//**********************************************************
public class Static_application_properties
//**********************************************************
{
    private static final boolean dbg = false;
    public static Properties_manager the_properties_manager;
    public static final String SORT_FILES_BY = "sort_files_by";
    private static final String LEVEL2 = "LEVEL2";
    private static final int DEFAULT_SIZE_WARNING_MEGABYTES = 100;
    private static final String DISK_CACHE_SIZE_WARNING_MEGABYTES = "DISK_CACHE_SIZE_WARNING_MEGABYTES";
    private static final String AUTO_PURGE_DISK_CACHES = "AUTO_PURGE_DISK_CACHES";
    private static int icon_size = -1;
    private static int folder_icon_size = -1;
    private static int video_length = -1;
    private static int column_width = -1;
    public static final int DEFAULT_ICON_SIZE = 256;
    public static final int DEFAULT_FOLDER_ICON_SIZE = 32;
    public static final int DEFAULT_VIDEO_LENGTH = 1;
    public static final String STYLE = "STYLE";
    public static final String ULTIM = "_ultim"; // must be lowercase because we test name.toLowerCase.contains("_ultim")
    //public static final String SHOW_FOLDER_SIZE = "show_folder_size"; // this is way too expensive
    public static final String SHOW_HIDDEN_FILES = "show_hidden_files";
    public static final String SHOW_HIDDEN_DIRECTORIES = "show_hidden_directories";
    private static final String ENABLE_FUSK = "enable_fusk";
    private static final String SHOW_FFMPEG_INSTALL_WARNING = "SHOW_FFMPEG_INSTALL_WARNING";
    private static final String SHOW_IMAGEMAGICK_INSTALL_WARNING = "SHOW_IMAGEMAGICK_INSTALL_WARNING";
    private static final String MAX_EXCLUDED_KEYWORDS = "max_number_of_excluded_keywords";
    public static final String EXCLUDED_KEYWORD_PREFIX = "excluded_keyword_";
    public static final String ICON_SIZE = "ICON_SIZE";
    public static final String FOLDER_ICON_SIZE = "FOLDER_ICON_SIZE";
    public static final String VIDEO_SAMPLE_LENGTH = "VIDEO_SAMPLE_LENGTH";
    public static final String LANGUAGE = "LANGUAGE";
    public static final String FONT_SIZE = "FONT_SIZE";
    public static final String COLUMN_WIDTH = "Column_width"; //this must match the ressource bundles
    public static final String VERTICAL_SCROLL_INVERTED = "vertical_scroll_inverted";
    public static final String ESCAPE = "escape_fast_exit";
    public static final String DING = "play_ding_when_long_operations_end";
    public static final String SHOW_ICONS = "show_icons";
    public static final String CONF_DIR = ".klik";//+File.separator;
    public static final String PROPERTIES_FILENAME = "klik_properties.txt";
    public static final String TRASH_DIR = "klik_trash";
    public static final String ICON_CACHE_DIR = "klik_icon_cache";
    public static final String ASPECT_RATIO_AND_ROTATION_CACHES_DIR = "klik_aspect_ratio_cache";
    public static final String FOLDER_ICON_CACHE_DIR = "klik_folder_icon_cache";
    public static final String USER_HOME = "user.home";
    public static final String SINGLE_COLUMN = "single_column";
    public static final String ICONS_FOR_FOLDERS = "icons_for_folders";
    public static final String MONITOR_BROWSED_FOLDERS = "monitor_browsed_folders";
    public static final String IMAGE_WINDOW_SCREEN_TOP_LEFT_X = "IMAGE_WINDOW_SCREEN_TOP_LEFT_X";
    public static final String IMAGE_WINDOW_SCREEN_TOP_LEFT_Y = "IMAGE_WINDOW_SCREEN_TOP_LEFT_Y";
    public static final String IMAGE_WINDOW_SCREEN_WIDTH = "IMAGE_WINDOW_SCREEN_WIDTH";
    public static final String IMAGE_WINDOW_SCREEN_HEIGHT = "IMAGE_WINDOW_SCREEN_HEIGHT";
    public static final String BROWSER_SCREEN_TOP_LEFT_X = "BROWSER_SCREEN_TOP_LEFT_X";
    public static final String BROWSER_SCREEN_TOP_LEFT_Y = "BROWSER_SCREEN_TOP_LEFT_Y";
    public static final String BROWSER_SCREEN_WIDTH = "BROWSER_SCREEN_WIDTH";
    public static final String BROWSER_SCREEN_HEIGHT = "BROWSER_SCREEN_HEIGHT";


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
    public static Look_and_feel read_look_and_feel_from_properties_file(Logger logger)
    //**********************************************************
    {
        Look_and_feel look_and_feel = null;
        String style_s = Static_application_properties.get_properties_manager(logger).get(STYLE);
        if (style_s == null)
        {
            // DEFAULT STYLE, first time klik is launched on the platform
            look_and_feel = new Look_and_feel_light(logger);
        } else {
            for (Look_and_feel laf : Look_and_feel_manager.registered) {
                if (laf.name.equals(style_s)) {
                    look_and_feel = laf;
                    break;
                }
            }
        }
        if (look_and_feel == null) {
            look_and_feel = new Look_and_feel_light(logger);
        }
        Static_application_properties.get_properties_manager(logger).save_unico(STYLE, look_and_feel.name,false);
        return look_and_feel;
    }

    //**********************************************************
    public static void set_style(Look_and_feel style,Logger logger)
    //**********************************************************
    {
        Static_application_properties.get_properties_manager(logger).save_unico(STYLE, style.name,false);
    }

    //**********************************************************
    public static File_sort_by get_sort_files_by(Logger logger)
    //**********************************************************
    {
        String s = Static_application_properties.get_properties_manager(logger).get(SORT_FILES_BY);
        if (s == null) {
            Static_application_properties.get_properties_manager(logger).save_unico(SORT_FILES_BY, File_sort_by.NAME.name(), false);
            return File_sort_by.NAME;
        }
        else
        {
            try {
                return File_sort_by.valueOf(s);
            }
            catch ( IllegalArgumentException e)
            {
                return File_sort_by.NAME;
            }
        }
    }

    //**********************************************************
    public static void set_sort_files_by(File_sort_by b, Logger logger)
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
            get_properties_manager(logger).save_unico(MONITOR_BROWSED_FOLDERS, "true", false);
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
    public static boolean get_auto_purge_disk_caches(Logger logger)
    //**********************************************************
    {
        String s = get_properties_manager(logger).get(AUTO_PURGE_DISK_CACHES);
        if (s == null) {
            get_properties_manager(logger).save_unico(AUTO_PURGE_DISK_CACHES, "false", false);
            return false;
        } else {
            return Boolean.parseBoolean(s);
        }
    }
    //**********************************************************
    public static void set_auto_purge_icon_disk_cache(boolean b, Logger logger)
    //**********************************************************
    {
        get_properties_manager(logger).save_unico(AUTO_PURGE_DISK_CACHES, String.valueOf(b), false);
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
    public static Rectangle2D get_image_window_stored_bounds(Logger logger)
    //**********************************************************
    {
        Properties_manager pm = get_properties_manager(logger);
        String x_s = pm.get(IMAGE_WINDOW_SCREEN_TOP_LEFT_X);
        if (x_s == null) return default_rectangle();
        double x = Double.parseDouble(x_s);
        String y_s = pm.get(IMAGE_WINDOW_SCREEN_TOP_LEFT_Y);
        if (y_s == null) return default_rectangle();
        double y = Double.parseDouble(y_s);
        String w_s = pm.get(IMAGE_WINDOW_SCREEN_WIDTH);
        if (w_s == null) return default_rectangle();
        double w = Double.parseDouble(w_s);
        String h_s = pm.get(IMAGE_WINDOW_SCREEN_HEIGHT);
        if (h_s == null) return default_rectangle();
        double h = Double.parseDouble(h_s);
        return new Rectangle2D(x, y, w, h);
    }

    //**********************************************************
    public static Rectangle2D get_browser_stored_bounds(Logger logger)
    //**********************************************************
    {
        Properties_manager pm = get_properties_manager(logger);
        String x_s = pm.get(BROWSER_SCREEN_TOP_LEFT_X);
        if (x_s == null) return default_rectangle();
        double x = Double.parseDouble(x_s);
        String y_s = pm.get(BROWSER_SCREEN_TOP_LEFT_Y);
        if (y_s == null) return default_rectangle();
        double y = Double.parseDouble(y_s);
        String w_s = pm.get(BROWSER_SCREEN_WIDTH);
        if (w_s == null) return default_rectangle();
        double w = Double.parseDouble(w_s);
        String h_s = pm.get(BROWSER_SCREEN_HEIGHT);
        if (h_s == null) return default_rectangle();
        double h = Double.parseDouble(h_s);
        return new Rectangle2D(x, y, w, h);
    }

    //**********************************************************
    private static Rectangle2D default_rectangle()
    //**********************************************************
    {
        return new Rectangle2D(0, 0, 800, 600);
    }

    //**********************************************************
    public static void save_image_window_bounds(Rectangle2D r, Logger logger)
    //**********************************************************
    {
        if ( dbg) logger.log("saving bounds="+r);
        Properties_manager pm = get_properties_manager(logger);
        pm.save_unico(IMAGE_WINDOW_SCREEN_TOP_LEFT_X, String.valueOf(r.getMinX()), false);
        pm.save_unico(IMAGE_WINDOW_SCREEN_TOP_LEFT_Y, String.valueOf(r.getMinY()), false);
        pm.save_unico(IMAGE_WINDOW_SCREEN_WIDTH, String.valueOf(r.getWidth()), false);
        pm.save_unico(IMAGE_WINDOW_SCREEN_HEIGHT, String.valueOf(r.getHeight()), false);
    }

    //**********************************************************
    public static void save_browser_bounds(Rectangle2D r, Logger logger)
    //**********************************************************
    {
        if ( dbg) logger.log("saving bounds="+r);
        Properties_manager pm = get_properties_manager(logger);
        pm.save_unico(BROWSER_SCREEN_TOP_LEFT_X, String.valueOf(r.getMinX()), false);
        pm.save_unico(BROWSER_SCREEN_TOP_LEFT_Y, String.valueOf(r.getMinY()), false);
        pm.save_unico(BROWSER_SCREEN_WIDTH, String.valueOf(r.getWidth()), false);
        pm.save_unico(BROWSER_SCREEN_HEIGHT, String.valueOf(r.getHeight()), false);
    }


    //**********************************************************
    public static int get_animated_gif_duration_for_a_video(Logger logger)
    //**********************************************************
    {
        if (video_length > 0) return video_length;
        // first time, we look it up on disk
        String video_length_s = get_properties_manager(logger).get(VIDEO_SAMPLE_LENGTH);
        if (video_length_s == null) {
            video_length = DEFAULT_VIDEO_LENGTH;
        } else {
            double d_video_length = Double.parseDouble(video_length_s);
            video_length = (int) d_video_length;
        }
        get_properties_manager(logger).save_unico(VIDEO_SAMPLE_LENGTH, String.valueOf(video_length), false);
        //if (icon_manager != null) icon_manager.icon_size_is_now(icon_size.get_icon_size());
        return video_length;
    }

    //**********************************************************
    public static void set_animated_gif_duration_for_a_video(int l, Logger logger)
    //**********************************************************
    {
        video_length = l;
        get_properties_manager(logger).save_unico(VIDEO_SAMPLE_LENGTH, String.valueOf(video_length), false);
    }



    //**********************************************************
    public static int get_column_width(Logger logger)
    //**********************************************************
    {
        if (column_width > 0) return column_width;
        // first time, we look it up on disk
        String column_width_s = get_properties_manager(logger).get(COLUMN_WIDTH);
        if (column_width_s == null) {
            column_width = Icon_manager.MIN_COLUMN_WIDTH;
        } else {
            double local = Double.parseDouble(column_width_s);
            column_width = (int) local;
        }
        get_properties_manager(logger).save_unico(COLUMN_WIDTH, String.valueOf(column_width), false);
        return column_width;
    }

    //**********************************************************
    public static void set_column_width(int l, Logger logger)
    //**********************************************************
    {
        column_width = l;
        get_properties_manager(logger).save_unico(COLUMN_WIDTH, String.valueOf(column_width), false);
    }




    //**********************************************************
    public static int get_icon_size(Logger logger)
    //**********************************************************
    {
        if (icon_size > 0) return icon_size;
        // first time, we look it up on disk
        String icon_size_s = get_properties_manager(logger).get(ICON_SIZE);
        if (icon_size_s == null) {
            icon_size = DEFAULT_ICON_SIZE;
        } else {
            double d_icon_size = Double.parseDouble(icon_size_s);
            icon_size = (int) d_icon_size;
        }
        get_properties_manager(logger).save_unico(ICON_SIZE, String.valueOf(icon_size), false);
        //if (icon_manager != null) icon_manager.icon_size_is_now(icon_size.get_icon_size());
        return icon_size;
    }

    //**********************************************************
    public static int get_folder_icon_size(Logger logger)
    //**********************************************************
    {
        if (folder_icon_size > 0) return folder_icon_size;
        // first time, we look it up on disk
        String folder_icon_size_s = get_properties_manager(logger).get(FOLDER_ICON_SIZE);
        if (folder_icon_size_s == null) {
            folder_icon_size = DEFAULT_FOLDER_ICON_SIZE;
        } else {
            double d_icon_size = Double.parseDouble(folder_icon_size_s);
            folder_icon_size = (int) d_icon_size;
        }
        get_properties_manager(logger).save_unico(FOLDER_ICON_SIZE, String.valueOf(folder_icon_size), false);
        //if (icon_manager != null) icon_manager.icon_size_is_now(icon_size.get_icon_size());
        return folder_icon_size;
    }



    //**********************************************************
    public static void set_cache_size_limit_warning_megabytes(int warning_megabytes, Logger logger)
    //**********************************************************
    {
        get_properties_manager(logger).save_unico(DISK_CACHE_SIZE_WARNING_MEGABYTES, String.valueOf(warning_megabytes), false);
    }

    //**********************************************************
    public static int get_cache_size_limit_warning_megabytes(Logger logger)
    //**********************************************************
    {
        int warning_megabytes = DEFAULT_SIZE_WARNING_MEGABYTES;
        String warning_bytes_s = get_properties_manager(logger).get(DISK_CACHE_SIZE_WARNING_MEGABYTES);
        if (warning_bytes_s != null)
        {
            warning_megabytes = (int)Double.parseDouble(warning_bytes_s);
        }
        get_properties_manager(logger).save_unico(DISK_CACHE_SIZE_WARNING_MEGABYTES, String.valueOf(warning_megabytes), false);
        return warning_megabytes;
    }




    //**********************************************************
    public static void set_icon_size(int target_size, Logger logger)
    //**********************************************************
    {
        icon_size = target_size;
        get_properties_manager(logger).save_unico(ICON_SIZE, String.valueOf(icon_size), false);
    }



    //**********************************************************
    public static void set_folder_icon_size(int target_size, Logger logger)
    //**********************************************************
    {
        folder_icon_size = target_size;
        get_properties_manager(logger).save_unico(FOLDER_ICON_SIZE, String.valueOf(folder_icon_size), false);
    }



    static double font_size_cache = -1.0;

    //**********************************************************
    public static double get_font_size(Logger logger)
    //**********************************************************
    {
        if (font_size_cache > 0) return font_size_cache;
        double font_size = 16; // this is the default immediately after installing or after erasing properties
        // first time, we look it up on disk
        String font_size_s = get_properties_manager(logger).get(FONT_SIZE);
        if (font_size_s != null) {
            try {
                font_size = Double.parseDouble(font_size_s);
            } catch (NumberFormatException e) {
                logger.log(Stack_trace_getter.get_stack_trace_for_throwable(e));
            }
        }
        get_properties_manager(logger).save_unico(FONT_SIZE, String.valueOf(font_size), false);
        font_size_cache = font_size;
        return font_size;
    }


    //**********************************************************
    public static void set_font_size(double target_size, Logger logger)
    //**********************************************************
    {
        font_size_cache = target_size;
        get_properties_manager(logger).save_unico(FONT_SIZE, String.valueOf(target_size), false);
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
        String s = get_properties_manager(logger).get(LANGUAGE);
        if (s == null) {
            s = "english-US";
        }
        get_properties_manager(logger).save_unico(LANGUAGE, s, false);
        return s;
    }

    //**********************************************************
    public static void set_language(String s, Logger logger)
    //**********************************************************
    {
        get_properties_manager(logger).save_unico(LANGUAGE, s, false);

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




    // returns a directory using that relative name, on the user home
    // creates it if needed
    //**********************************************************
    public static Path get_absolute_dir_on_user_home(String relative_dir_name, boolean can_fail, Logger logger)
    //**********************************************************
    {
        if (dbg) logger.log("dir_name=" + relative_dir_name);
        String home = System.getProperty(USER_HOME);
        if (dbg) logger.log("user home =" + home);

        return from_home(home, relative_dir_name,can_fail, logger);
    }

    //**********************************************************
    private static Path from_home(String home, String relative_dir_name, boolean can_fail, Logger logger)
    //**********************************************************
    {
        Path conf_dir1 = Paths.get(home,CONF_DIR);
        if (!conf_dir1.toFile().exists())
        {
            try {
                Files.createDirectory(conf_dir1);
            } catch (IOException e) {
                String err = " Attempt to create a directory named->" + conf_dir1.toAbsolutePath() + "<- failed";
                if ( can_fail)
                {
                    logger.log(err);
                    return null;
                }
                Popups.popup_Exception(e,300,err,logger);
                return null;
            }
        }

        // do it deeper, this way icons don't show up in $home/.klik
        // to avoid privacy violation
        // when browsing $home

        Path conf_dir2 = Paths.get(conf_dir1.toString(),CONF_DIR+"_privacy_screen");
        if (!conf_dir2.toFile().exists())
        {
            try {
                Files.createDirectory(conf_dir2);
            } catch (IOException e) {
                String err = " Attempt to create a directory named->" + conf_dir2.toAbsolutePath() + "<- failed";
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
                Popups.popup_Exception(e, 300,err,logger);
                return null;
            }
        }
        if (dbg) {
            String err = "directory named->" + returned.toAbsolutePath() + "<- OK";
            logger.log_stack_trace(err);
        }
        return returned;
    }

    // returns a directory using that relative name
    //**********************************************************
    public static Path get_trash_dir(Path for_this, Logger logger)
    //**********************************************************
    {

        Path full_path = for_this.toAbsolutePath();
        if ((!full_path.toString().equals("/Volumes")) && (full_path.toString().startsWith("/Volumes")))
        {
            // this is MacOS...
            logger.log("get_trash_dir "+for_this.toAbsolutePath().toString());
            Path volume = get_MacOS_volume(for_this,logger);
            if ( volume == null) {
                logger.log("PANIC get_trash_dir " + for_this.toAbsolutePath().toString()+" fails");
            }
            return from_home(volume.toString(),TRASH_DIR,true, logger);
        }

        Path trash_dir = get_absolute_dir_on_user_home(TRASH_DIR,false,logger);
        if ( trash_dir == null)
        {
            logger.log("PANIC: trash dir unknown");
            return null;
        }
        if (dbg) logger.log("trash_dir file=" + trash_dir.toAbsolutePath());
        return trash_dir;
    }

    //**********************************************************
    private static Path get_MacOS_volume(Path for_this, Logger logger)
    //**********************************************************
    {
        //logger.log("get_MacOS_volume ENTRY = "+for_this);

        Path volume = for_this.toAbsolutePath();
        for(;;)
        {
            Path test = volume.getParent();
            if ( test == null)
            {
                //logger.log("get_MacOS_volume test == null ");
                break;
            }
            if ( test.getFileName() == null)
            {
                //logger.log("get_MacOS_volume test == null ");
                break;
            }
            //logger.log("get_MacOS_volume testing "+test);
            if ( test.toString().equals("/Volumes"))
            {
                logger.log("get_MacOS_volume returning "+volume);
                return volume;
            }
            volume = volume.getParent();
            if (volume.toString().equals("/")) break;
        }
        logger.log("get_MacOS_volume FAILED! ");
        return null;

    }


    //**********************************************************
    public static List<Path> get_existing_trash_dirs(Logger logger)
    //**********************************************************
    {
        List<Path> trashes = new ArrayList<>();
        for ( File f : File.listRoots())
        {
            logger.log("root ->"+f+"<-");
            if ( f.toString().equals("/"))
            {
                Path trash_dir = get_absolute_dir_on_user_home(TRASH_DIR,false,logger);
                trashes.add(trash_dir);
                // unix system...
                Path volumes = Path.of("/","Volumes");
                File files[] = volumes.toFile().listFiles();
                for ( File ff : files)
                {
                    if ( ff.isDirectory())
                    {
                        Path test = Path.of(ff.toPath().toString(),".klik");
                        if (Files.exists(test)) {
                            trashes.add(test);
                        }
                    }
                }
            }
            else
            {
                if ( f.getName().startsWith("C:")) {
                    Path trash_dir = get_absolute_dir_on_user_home(TRASH_DIR,false,logger);
                    trashes.add(trash_dir);
                }
                else {
                    Path trash_dir = from_home(f.toPath().toString(), TRASH_DIR,true, logger);
                    trashes.add(trash_dir);
                    logger.log("WARNING: untested trash on windows drives !!!!");
                }


            }
        }
        return trashes;
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
    public static void set_ding(boolean b, Logger logger)
    //**********************************************************
    {
        Static_application_properties.get_properties_manager(logger).save_unico(DING, String.valueOf(b), false);
    }

    //**********************************************************
    public static boolean get_ding(Logger logger)
    //**********************************************************
    {
        String s = Static_application_properties.get_properties_manager(logger).get(DING);
        if (s == null) {
            Static_application_properties.get_properties_manager(logger).save_unico(DING, "true", false);
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

    //**********************************************************
    public static List<String> get_cleanup_tokens(Logger logger)
    //**********************************************************
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
    public static boolean get_show_imagemagick_install_warning(Logger logger)
    //**********************************************************
    {
        String s = Static_application_properties.get_properties_manager(logger).get(SHOW_IMAGEMAGICK_INSTALL_WARNING);
        if (s == null) {
            Static_application_properties.get_properties_manager(logger).save_unico(SHOW_IMAGEMAGICK_INSTALL_WARNING, "true", false);
            return true;
        }
        else
        {
            return Boolean.parseBoolean(s);
        }
    }
    //**********************************************************
    public static void set_show_imagemagick_install_warning(boolean b, Logger logger)
    //**********************************************************
    {
        Static_application_properties.get_properties_manager(logger).save_unico(SHOW_IMAGEMAGICK_INSTALL_WARNING, String.valueOf(b), false);
    }

    static boolean ffmpeg_popup_done = false;
    //**********************************************************
    public static void manage_show_ffmpeg_install_warning(Stage owner, Logger logger)
    //**********************************************************
    {
        if ( Static_application_properties.get_show_ffmpeg_install_warning(logger))
        {
            if ( !ffmpeg_popup_done)
            {
                ffmpeg_popup_done = true;
                String msg = "klik uses ffmpeg to support several features. It is easy and free to install ffmpeg (google it!)";
                logger.log("WARNING: " + msg);
                Platform.runLater(() -> {
                    if (Popups.popup_ask_for_confirmation(owner, msg,"If you do not want to see this warning about installing ffmepg again, click OK", logger)) {
                        Static_application_properties.set_show_ffmpeg_install_warning(false,logger);
                    }
                });
            }
        }
    }
    static boolean imagemagick_popup_done = false;
    //**********************************************************
    public static void manage_show_imagemagick_install_warning(Stage owner, Logger logger)
    //**********************************************************
    {
        if ( Static_application_properties.get_show_imagemagick_install_warning(logger))
        {
            if(!imagemagick_popup_done)
            {
                imagemagick_popup_done = true;
                String msg = "klik uses the convert utility of imagemagick.org to support some features. It is easy and free to install (google it!)";
                logger.log("WARNING: " + msg);
                Platform.runLater(() -> {
                    if (Popups.popup_ask_for_confirmation(owner, msg,"If you do not want to see this warning about installing imagemagick again, click OK", logger)) {
                        Static_application_properties.set_show_imagemagick_install_warning(false,logger);
                    }
                });
            }
        }
    }

    static Boolean level2_cache = null;
    //**********************************************************
    public static boolean get_level2(Logger logger)
    //**********************************************************
    {
        if ( level2_cache == null)
        {
            String s = Static_application_properties.get_properties_manager(logger).get(LEVEL2);
            if (s == null) {
                Static_application_properties.get_properties_manager(logger).save_unico(LEVEL2, "false", false);
                level2_cache = false;
            } else {
                logger.log("LEVEL2=" + s);
                level2_cache = Boolean.parseBoolean(s);
            }
        }
        return level2_cache;
    }

    /*
    // should never be used: the user should edit the properties file directly
    //**********************************************************
    public static void set_level2(boolean b, Logger logger)
    //**********************************************************
    {
        level2_cache = b;
        Static_application_properties.get_properties_manager(logger).save_unico(LEVEL2, String.valueOf(b), false);
    }
    */


        /*
    //**********************************************************
    public static boolean get_show_folder_size(Logger logger)
    //**********************************************************
    {
        String s = get_properties_manager(logger).get(SHOW_FOLDER_SIZE);
        if (s == null) {
            get_properties_manager(logger).save_unico(SHOW_FOLDER_SIZE, "false", false);
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
    */

}
