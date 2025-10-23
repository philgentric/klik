//SOURCES ../audio/Audio_player_FX_UI.java
package klik.properties;

import javafx.collections.ObservableList;
import javafx.geometry.Rectangle2D;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import klik.Shared_services;
import klik.util.execute.actor.Aborter;
import klik.browser.virtual_landscape.Virtual_landscape;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import klik.util.ui.Popups;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


//**********************************************************
public class Non_booleans_properties
//**********************************************************
{
    private static final boolean dbg = false;
    public static final String SCREEN_TOP_LEFT_X = "_SCREEN_TOP_LEFT_X";
    public static final String SCREEN_TOP_LEFT_Y = "_SCREEN_TOP_LEFT_Y";
    public static final String SCREEN_WIDTH = "_SCREEN_WIDTH";
    public static final String SCREEN_HEIGHT = "_SCREEN_HEIGHT";
    private static final int DEFAULT_SIZE_WARNING_MEGABYTES = 500;
    private static final String DISK_CACHE_SIZE_WARNING_MEGABYTES = "DISK_CACHE_SIZE_WARNING_MEGABYTES";


    public static final int DEFAULT_ICON_SIZE = 256;
    public static final int DEFAULT_FOLDER_ICON_SIZE = 256;
    public static final int DEFAULT_VIDEO_LENGTH = 1;
    public static final String ULTIM = "_ultim"; // must be lowercase because we test name.toLowerCase.contains("_ultim")


    public static final String ICON_SIZE = "ICON_SIZE";
    public static final String FOLDER_ICON_SIZE = "FOLDER_ICON_SIZE";
    public static final String VIDEO_SAMPLE_LENGTH = "VIDEO_SAMPLE_LENGTH";
    public static final String LANGUAGE_KEY = "LANGUAGE";
    public static final String FONT_SIZE = "FONT_SIZE";
    public static final String COLUMN_WIDTH = "Column_width"; //this must match the resource bundles
    public static final String STYLE_KEY = "STYLE";
    public static final String CUSTOM_COLOR = "Custom_color";

    public static final String USER_HOME = "user.home";
    public static final String CONF_DIR = ".klik";


    // when launcher starts, it creates a server to listen to UI_CHANGED messages
    // this is the port on which the launcher listens
    // the launcher will then broadcast UI_CHANGED messages to all running
    // applications (klik browsers and audio player)
    // the port on which apps listen for UI_CHANGED messages is NOT stored in a file
    // it is sent back to the Launcher for registration
    public static final String FILENAME_FOR_UI_CHANGE_REPORT_PORT_AT_LAUNCHER = "ui_change_report_port_at_launcher.txt";


    // When the launcher starts an application,
    // it creates a server to listen to a message from the application
    // to know that the application has started
    // this is the port on which the launcher listens
    // it is written in a file by the launcher
    // it means that there could be race conditions
    // if multiple applications are started simultaneously
    // the other implementation is to pass the port as a command line argument
    // but this is complicated to support for jvm/native with multiple OS
    public static final String FILENAME_FOR_PORT_TO_REPLY_ABOUT_START = "port_to_reply_about_start.txt";


    public static final String PROPERTIES_FILENAME = "klik.properties";
    public static final String TRASH_DIR = "klik_trash";
    public static final String FACE_RECO_DIR = "face_reco";
    private static final String AUDIO_PLAYER_CURRENT_SONG = "AUDIO_PLAYER_CURRENT_SONG";
    private static final String AUDIO_PLAYER_CURRENT_TIME = "AUDIO_PLAYER_CURRENT_TIME";
    private static final String AUDIO_PLAYER_EQUALIZER_BAND_ = "AUDIO_PLAYER_EQUALIZER_BAND_";
    private static final String AUDIO_PLAYER_VOLUME = "AUDIO_PLAYER_VOLUME";
    public static final String JAVA_VM_MAX_RAM = "max_RAM_in_GBytes"; // this is the maximum RAM that the Java VM can use, in GBytes
    public static final String DEFAULT_CUSTOM_COLOR = "#b8d4fe";
    public static final String PURPOSE = "Java VM max ram";
    public static final String FILENAME = "max_ram";

    // cached values


    private static int icon_size = -1;
    private static int folder_icon_size = -1;
    private static int video_length = -1;
    private static int column_width = -1;
    private static Color custom_color = null;



    //**********************************************************
    public static Rectangle2D get_window_bounds(String key, Window owner)
    //**********************************************************
    {
        IProperties pm = Shared_services.main_properties();
        String x_s = pm.get(key + SCREEN_TOP_LEFT_X);
        if (x_s == null) return default_rectangle();
        double x = Double.parseDouble(x_s);

        String y_s = pm.get(key + SCREEN_TOP_LEFT_Y);
        if (y_s == null) return default_rectangle();
        double y = Double.parseDouble(y_s);

        String w_s = pm.get(key + SCREEN_WIDTH);
        if (w_s == null) return default_rectangle();
        double w = Double.parseDouble(w_s);

        String h_s = pm.get(key + SCREEN_HEIGHT);
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

        System.out.println("WARNING: from file  bounds " + target  + " do not fit with any screen, changing the target");

        // use the first screen available (e.g. the main screen, the laptop screen, etc.)
        for ( Screen s : all_screens)
        {
            System.out.println("forcing screen bounds to: " + s.getVisualBounds());
            return s.getVisualBounds();
        }
        // normally never happens?
        System.out.println("SHOULD NOT HAPPEN: no screen found, using default rectangle");
        return new Rectangle2D(0,0,800,600); // default rectangle
    }

    //**********************************************************
    public static void save_window_bounds(Stage stage, String key, Logger logger)
    //**********************************************************
    {
        Rectangle2D r = new Rectangle2D(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
        if (dbg) logger.log("saving bounds=" + r);
        IProperties pm = Shared_services.main_properties();
        pm.set(key + SCREEN_TOP_LEFT_X, String.valueOf(r.getMinX()));
        pm.set(key + SCREEN_TOP_LEFT_Y, String.valueOf(r.getMinY()));
        pm.set(key + SCREEN_WIDTH, String.valueOf(r.getWidth()));
        pm.set(key + SCREEN_HEIGHT, String.valueOf(r.getHeight()));
    }


    //**********************************************************
    private static Rectangle2D default_rectangle()
    //**********************************************************
    {
        return new Rectangle2D(0, 0, 800, 600);
    }

    //**********************************************************
    public static int get_animated_gif_duration_for_a_video(Window owner)
    //**********************************************************
    {
        if (video_length > 0) return video_length;
        // first time, we look it up on disk
        String video_length_s = Shared_services.main_properties().get(VIDEO_SAMPLE_LENGTH);
        if (video_length_s == null) {
            video_length = DEFAULT_VIDEO_LENGTH;
        } else {
            double d_video_length = Double.parseDouble(video_length_s);
            video_length = (int) d_video_length;
        }
        Shared_services.main_properties().set(VIDEO_SAMPLE_LENGTH, String.valueOf(video_length));
        //if (icon_manager != null) icon_manager.icon_size_is_now(icon_size.get_icon_size());
        return video_length;
    }

    //**********************************************************
    public static void set_animated_gif_duration_for_a_video(int l,Window owner)
    //**********************************************************
    {
        video_length = l;
        Shared_services.main_properties().set(VIDEO_SAMPLE_LENGTH, String.valueOf(video_length));
    }


    //**********************************************************
    public static int get_column_width(Window owner)
    //**********************************************************
    {
        if (column_width > 0) return column_width;
        // first time, we look it up on disk
        String column_width_s = Shared_services.main_properties().get(COLUMN_WIDTH);
        if (column_width_s == null) {
            column_width = Virtual_landscape.MIN_COLUMN_WIDTH;
        } else {
            double local = Double.parseDouble(column_width_s);
            column_width = (int) local;
        }
        Shared_services.main_properties().set(COLUMN_WIDTH, String.valueOf(column_width));
        return column_width;
    }

    //**********************************************************
    public static void set_column_width(int l, Window owner)
    //**********************************************************
    {
        column_width = l;
        Shared_services.main_properties().set(COLUMN_WIDTH, String.valueOf(column_width));
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
    public static int get_icon_size(Window owner)
    //**********************************************************
    {
        if (icon_size > 0) return icon_size;
        // first time, we look it up on disk
        String icon_size_s = Shared_services.main_properties().get(ICON_SIZE);
        if (icon_size_s == null) {
            icon_size = DEFAULT_ICON_SIZE;
        } else {
            double d_icon_size = Double.parseDouble(icon_size_s);
            icon_size = (int) d_icon_size;
        }
        Shared_services.main_properties().set(ICON_SIZE, String.valueOf(icon_size));
        //if (icon_manager != null) icon_manager.icon_size_is_now(icon_size.get_icon_size());
        return icon_size;
    }

    //**********************************************************
    public static int get_folder_icon_size(Window owner)
    //**********************************************************
    {
        if (folder_icon_size > 0) return folder_icon_size;
        // first time, we look it up on disk
        String folder_icon_size_s = Shared_services.main_properties().get(FOLDER_ICON_SIZE);
        if (folder_icon_size_s == null) {
            folder_icon_size = DEFAULT_FOLDER_ICON_SIZE;
        } else {
            double d_icon_size = Double.parseDouble(folder_icon_size_s);
            folder_icon_size = (int) d_icon_size;
        }
        Shared_services.main_properties().set(FOLDER_ICON_SIZE, String.valueOf(folder_icon_size));
        //if (icon_manager != null) icon_manager.icon_size_is_now(icon_size.get_icon_size());
        return folder_icon_size;
    }


    //**********************************************************
    public static void set_cache_size_limit_warning_megabytes_fx(int warning_megabytes, Window owner)
    //**********************************************************
    {
        Shared_services.main_properties().set(DISK_CACHE_SIZE_WARNING_MEGABYTES, String.valueOf(warning_megabytes));
    }

    //**********************************************************
    public static int get_folder_warning_size(Window owner)
    //**********************************************************
    {
        int warning_megabytes = DEFAULT_SIZE_WARNING_MEGABYTES;
        String warning_bytes_s = Shared_services.main_properties().get(DISK_CACHE_SIZE_WARNING_MEGABYTES);
        if (warning_bytes_s != null) {
            warning_megabytes = (int) Double.parseDouble(warning_bytes_s);
        }
        Shared_services.main_properties().set(DISK_CACHE_SIZE_WARNING_MEGABYTES, String.valueOf(warning_megabytes));
        return warning_megabytes;
    }


    //**********************************************************
    public static void set_icon_size(int target_size, Window owner)
    //**********************************************************
    {
        icon_size = target_size;
        Shared_services.main_properties().set(ICON_SIZE, String.valueOf(icon_size));
    }


    //**********************************************************
    public static void set_folder_icon_size(int target_size, Window owner)
    //**********************************************************
    {
        folder_icon_size = target_size;
        Shared_services.main_properties().set(FOLDER_ICON_SIZE, String.valueOf(folder_icon_size));
    }


    static double font_size_cache = -1.0;

    //**********************************************************
    public static double get_font_size(Window owner,Logger logger)
    //**********************************************************
    {
        if (font_size_cache > 0) return font_size_cache;
        double font_size = 16; // this is the default immediately after installing or after erasing properties
        // first time, we look it up on disk
        String font_size_s = Shared_services.main_properties().get(FONT_SIZE);
        if (font_size_s != null) {
            try {
                font_size = Double.parseDouble(font_size_s);
            } catch (NumberFormatException e) {
                logger.log(Stack_trace_getter.get_stack_trace_for_throwable(e));
                font_size = 16;
            }
        }
        //Shared_services.main_properties().set(FONT_SIZE, String.valueOf(font_size));
        font_size_cache = font_size;
        return font_size;
    }


    //**********************************************************
    public static void set_font_size(double target_size, Window owner)
    //**********************************************************
    {
        font_size_cache = target_size;
        Shared_services.main_properties().set(FONT_SIZE, String.valueOf(target_size));
    }


    //**********************************************************
    public static String get_language_key(Window owner)
    //**********************************************************
    {
        String s = Shared_services.main_properties().get(LANGUAGE_KEY);
        if (s == null) {
            s = "English";
            Shared_services.main_properties().set(LANGUAGE_KEY, s);
        }
        return s;
    }



    public static void force_reload_from_disk(Window owner) {
        Shared_services.main_properties().force_reload_from_disk();
    }

/*
    //**********************************************************
    public static Path get_absolute_dir_on_user_home(String relative_dir_name, boolean can_fail, Logger logger)
    //**********************************************************
    {
        if (dbg) logger.log("dir_name=" + relative_dir_name);
        String home = System.getProperty(USER_HOME);
        if (dbg) logger.log("user home =" + home);
        Path dir = Paths.get(home, relative_dir_name);
        if ( !dir.toFile().exists())
        {
            try {
                Files.createDirectory(dir);
            } catch (IOException e) {
                String err = " Attempt to create a directory named->" + dir.toAbsolutePath() + "<- failed "+e;
                if ( can_fail)
                {
                    logger.log(err);
                    return null;
                }
                Popups.popup_Exception(e,300,err,logger);
                return null;
            }
        }
        return dir;
    }
*/

    // returns a directory using that relative name, on the user home, creates it if needed
    //**********************************************************
    public static Path get_absolute_hidden_dir_on_user_home(String relative_dir_name, boolean can_fail, Window owner, Logger logger)
    //**********************************************************
    {
        if (dbg) logger.log("dir_name=" + relative_dir_name);
        String home = System.getProperty(USER_HOME);
        if (dbg) logger.log("user home =" + home);
        return from_top_folder(home, relative_dir_name, can_fail, owner, logger);
    }


    //**********************************************************
    public static Path from_top_folder(String top_folder, String relative_dir_name, boolean can_fail, Window owner, Logger logger)
    //**********************************************************
    {
        Path conf_dir1 = Paths.get(top_folder, CONF_DIR);
        if (!conf_dir1.toFile().exists()) {
            try {
                Files.createDirectory(conf_dir1);
            } catch (IOException e) {
                String err = " Attempt to create a directory named->" + conf_dir1.toAbsolutePath() + "<- failed " + e;
                if (can_fail) {
                    logger.log(err);
                    return null;
                }
                Popups.popup_Exception(e, 300, err, owner, logger);
                return null;
            }
        }

        // do it deeper, this way icons don't show up in $home/.klik to avoid privacy violation when browsing $home

        Path conf_dir2 = Paths.get(conf_dir1.toString(), CONF_DIR + "_privacy_screen");
        if (!conf_dir2.toFile().exists()) {
            try {
                Files.createDirectory(conf_dir2);
            } catch (IOException e) {
                String err = " Attempt to create a directory named->" + conf_dir2.toAbsolutePath() + "<- failed";
                Popups.popup_Exception(e, 300, err, owner, logger);
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
                Popups.popup_Exception(e, 300, err, owner, logger);
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
    public static Path get_trash_dir(Path for_this, Window owner, Logger logger)
    //**********************************************************
    {
        // the idea of having multiple trash dirs is that when you are moving file to trash,
        // it is MUCH faster to move them to a trash dir on the same disk,
        // than to move them to the user home trash dir,
        // this is especially important on slow/network drives

        Path full_path = for_this.toAbsolutePath();
        if ((!full_path.toString().equals("/Volumes")) && (full_path.toString().startsWith("/Volumes"))) {
            // this is MacOS...
            //logger.log("what is trash_dir for :" + for_this.toAbsolutePath());
            Path volume = get_MacOS_volume(for_this, logger);
            if (volume == null) {
                logger.log("PANIC get_trash_dir " + for_this.toAbsolutePath() + " fails");
            }
            Path candidate =from_top_folder(volume.toString(), TRASH_DIR, true, owner, logger);
            if ( candidate != null) return candidate;
            {
                // happens for OneDrive, where the real path is
                // /Volumes/Macintosh HD/Users/<name>/OneDrive -<companyname>/floder... etc
                // and /Volumes/Macintosh HD is NOT writable, so cannot create a trash dir there
                // better use the user home
            }
        }

        Path trash_dir = get_absolute_hidden_dir_on_user_home(TRASH_DIR, false, owner, logger);
        if (trash_dir == null) {
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
        for (; ; ) {
            Path test = volume.getParent();
            if (test == null) {
                //logger.log("get_MacOS_volume test == null ");
                break;
            }
            if (test.getFileName() == null) {
                //logger.log("get_MacOS_volume test == null ");
                break;
            }
            //logger.log("get_MacOS_volume testing "+test);
            if (test.toString().equals("/Volumes")) {
                logger.log("get_MacOS_volume returning " + volume);
                return volume;
            }
            volume = volume.getParent();
            if (volume.toString().equals("/")) break;
        }
        logger.log("get_MacOS_volume FAILED! ");
        return null;

    }


    //**********************************************************
    public static List<Path> get_existing_trash_dirs(Window owner,Logger logger)
    //**********************************************************
    {
        List<Path> trashes = new ArrayList<>();
        for (File f : File.listRoots()) {
            //logger.log("root ->"+f+"<-");
            if (f.toString().equals("/")) {
                Path trash_dir = get_absolute_hidden_dir_on_user_home(TRASH_DIR, false, owner, logger);
                trashes.add(trash_dir);
                // unix system...
                Path volumes = Path.of("/", "Volumes");
                File[] files = volumes.toFile().listFiles();
                if (files == null) continue;
                for (File ff : files) {
                    if (ff.isDirectory()) {
                        Path test = Path.of(ff.toPath().toString(), ".klik");
                        if (Files.exists(test)) {
                            trashes.add(test);
                        }
                    }
                }
            } else {
                if (f.getName().startsWith("C:")) {
                    Path trash_dir = get_absolute_hidden_dir_on_user_home(TRASH_DIR, false, owner, logger);
                    trashes.add(trash_dir);
                } else {
                    Path trash_dir = from_top_folder(f.toPath().toString(), TRASH_DIR, true, owner, logger);
                    trashes.add(trash_dir);
                    logger.log("WARNING: untested trash on windows drives !!!!");
                }


            }
        }
        return trashes;
    }


    //**********************************************************
    public static void save_current_song(String path, Window owner)
    //**********************************************************
    {
        IProperties pm = Shared_services.main_properties();
        pm.set(AUDIO_PLAYER_CURRENT_SONG, path);

    }

    //**********************************************************
    public static String get_current_song(Window owner)
    //**********************************************************
    {
        IProperties pm = Shared_services.main_properties();
        return pm.get(AUDIO_PLAYER_CURRENT_SONG);
    }

    static int previous = -1;

    //**********************************************************
    public static void save_current_time_in_song(int time, Window owner)
    //**********************************************************
    {
        if (previous > 0) {
            if (previous / 10 == time / 10) return;
        }
        previous = time;
        //logger.log("save_current_time_in_song "+time);
        IProperties pm = Shared_services.main_properties();
        pm.set(AUDIO_PLAYER_CURRENT_TIME, "" + time);

    }


    //**********************************************************
    public static Integer get_current_time_in_song(Window owner,Logger logger)
    //**********************************************************
    {
        IProperties pm = Shared_services.main_properties();
        String s = pm.get(AUDIO_PLAYER_CURRENT_TIME);
        if (s == null)
        {
            logger.log("WARNING: cannot find player current time");
            return 0;
        }
        int returned = 0;
        try {
            returned = Integer.parseInt(s);
        } catch (NumberFormatException e) {
            logger.log("WARNING: cannot parse player current time->" + s + "<-");
        }
        //logger.log("player current time = "+returned);
        return returned;
    }

    //**********************************************************
    public static double get_equalizer_value_for_band(int i, Window owner,Logger logger)
    //**********************************************************
    {
        IProperties pm = Shared_services.main_properties();
        String s = pm.get(AUDIO_PLAYER_EQUALIZER_BAND_ + i);
        if (s == null) return 0;

        double value = 0;
        try {
            value = Double.valueOf(s);
        } catch (NumberFormatException e) {
            logger.log("WARNING: cannot parse equalizer value for band " + i + "->" + s + "<-");
        }
        return value;
    }

    //**********************************************************
    public static void save_equalizer_value_for_band(int i, double value, Window owner)
    //**********************************************************
    {
        IProperties pm = Shared_services.main_properties();
        pm.set(AUDIO_PLAYER_EQUALIZER_BAND_ + i, "" + value);
    }

    //**********************************************************
    public static void save_audio_volume(double value, Window owner)
    //**********************************************************
    {
        IProperties pm = Shared_services.main_properties();
        pm.set(AUDIO_PLAYER_VOLUME, "" + value);
    }

    //**********************************************************
    public static double get_audio_volume(Window owner,Logger logger)
    //**********************************************************
    {
        IProperties pm = Shared_services.main_properties();
        String s = pm.get(AUDIO_PLAYER_VOLUME);
        if (s == null) return 0.5;

        double value = 0;
        try {
            value = Double.valueOf(s);
        } catch (NumberFormatException e) {
            logger.log("WARNING: cannot parse volume->" + s + "<-");
        }
        return value;
    }


    //**********************************************************
    public static void save_java_VM_max_RAM(int value, Window owner, Logger logger)
    //**********************************************************
    {
        File_based_IProperties f = new File_based_IProperties(PURPOSE, FILENAME,owner,new Aborter("ram", logger), logger);
        f.set(JAVA_VM_MAX_RAM, "" + value);
    }
    //**********************************************************
    public static int get_java_VM_max_RAM(Window owner, Logger logger)
    //**********************************************************
    {
        File_based_IProperties f = new File_based_IProperties(PURPOSE,FILENAME, owner,new Aborter("ram", logger), logger);
        String s = f.get(JAVA_VM_MAX_RAM);
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