package klik.look;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.stage.StageStyle;
import klik.Klik_application;
import klik.look.styles.*;
import klik.properties.Static_application_properties;
import klik.util.Logger;
import klik.util.Stack_trace_getter;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

//**********************************************************
public class Look_and_feel_manager
//**********************************************************
{
    // https://docs.oracle.com/javafx/2/api/javafx/scene/doc-files/cssref.html


    public static final boolean icon_load_dbg = false;
    public static final boolean look_dbg = false;
    public static Logger logger;

    private static Look_and_feel instance = null;
    public static List<Look_and_feel> registered = new ArrayList<>();
    private static Image default_icon = null;
    public static Image broken_icon = null;
    public static Image denied_icon = null;
    public static Image large_folder_icon = null;
    public static Image folder_icon = null;
    public static Image trash_icon = null;
    public static Image bookmarks_icon = null;
    public static Image view_icon = null;
    public static Image up_icon = null;
    public static Image preferences_icon = null;
    public static Image not_found_icon = null;
    public static Image unknown_error_icon = null;
    public static Image dummy_icon = null;


    //**********************************************************
    public static Look_and_feel get_instance()
    //**********************************************************
    {
        if (logger == null)
        {
            // must init otherwise debugging very hard!
            // so this is intended to cause a crash
            System.out.println(Stack_trace_getter.get_stack_trace("you must call init_Look_and_feel with a non null logger"));
            return null;
        }
        return instance;
    }

    //**********************************************************
    public static Look_and_feel get_look_and_feel_instance(Logger logger)
    //**********************************************************
    {
        Look_and_feel returned = Look_and_feel_manager.get_instance();
        if ( returned == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BADBADBAD: Look_and_feel_manager.get_instance() returns null?"));
        }
        return returned;
    }


    //**********************************************************
    public static void init_Look_and_feel(Logger logger_)
    //**********************************************************
    {
        logger = logger_;
        if (!registered.isEmpty()) return;
        // if you want to add a new style, its here!
        registered.add(new Look_and_feel_light(logger_));
        registered.add(new Look_and_feel_dark(logger_));
        registered.add(new Look_and_feel_wood(logger_));
        instance = Static_application_properties.read_look_and_feel_from_properties_file(logger_);
    }

    //**********************************************************
    public static void set_look_and_feel(Look_and_feel style)
    //**********************************************************
    {
        //logger.log(Stack_trace_getter.get_stack_trace("setting style = " + style.name));
        logger.log(("setting style = " + style.name));
        instance = style;
        Static_application_properties.set_style(style,logger);
        reset();
    }

    //**********************************************************
    public static void reset()
    //**********************************************************
    {
        default_icon = null;
        broken_icon = null;
        denied_icon = null;
        trash_icon = null;
        folder_icon = null;
        up_icon = null;
        preferences_icon = null;
    }

    /**********************************************************



                            ICON SECTION




    *///**********************************************************

    //**********************************************************
    public static Image get_dummy_icon(double icon_size)
    //**********************************************************
    {
        if (dummy_icon != null)
        {
            if ( dummy_icon.getHeight() == icon_size) return dummy_icon;
        }
        Look_and_feel i = get_instance();
        if (i == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BADBADBAD: cannot get look and feel instance"));
            return null;
        }
        String path = i.get_dummy_icon_path();
        if (path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BADBADBAD: cannot get dummy icon path"));
            return null;
        }
        dummy_icon = load_icon_fx_from_jar(path, icon_size);
        return dummy_icon;
    }
    //**********************************************************
    public static Image get_folder_icon(double icon_size)
    //**********************************************************
    {
        if (folder_icon != null)
        {
            if ( folder_icon.getHeight() == icon_size) return folder_icon;
        }
        Look_and_feel i = get_instance();
        if (i == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BADBADBAD: cannot get look and feel instance"));
            return null;
        }
        String path = i.get_folder_icon_path();
        if (path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BADBADBAD: cannot get folder icon path"));
            return null;
        }
        folder_icon = load_icon_fx_from_jar(path, icon_size);
        return folder_icon;
    }

    /*
    //**********************************************************
    public static Image get_large_folder_icon(double icon_size)
    //**********************************************************
    {
        if (large_folder_icon != null)
        {
            if ( large_folder_icon.getHeight() == icon_size) return large_folder_icon;
        }
        Look_and_feel i = get_instance();
        if (i == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BADBADBAD: cannot get look and feel instance"));
            return null;
        }
        String path = i.get_folder_icon_path();
        if (path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BADBADBAD: cannot get folder icon path"));
            return null;
        }
        large_folder_icon = load_icon_fx_from_jar(path, icon_size);
        return large_folder_icon;
    }



    //**********************************************************
    public static Path get_folder_icon_path()
    //**********************************************************
    {
        Look_and_feel i = get_instance();
        if (i == null) {
            logger.log(Stack_trace_getter.get_stack_trace("BADBADBAD: cannot get look and feel instance"));
            return null;
        }
        String path = i.get_folder_icon_path();
        if (path == null) {
            logger.log(Stack_trace_getter.get_stack_trace("BADBADBAD: cannot get folder icon path"));
            return null;
        }
        return Path.of(path);
    }
    */

    //**********************************************************
    public static Image get_default_icon(double icon_size)
    //**********************************************************
    {
        if (default_icon != null)
        {
            if ( default_icon.getHeight() == icon_size) return default_icon;
        }
        Look_and_feel i = get_instance();
        if (i == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BADBADBAD: cannot get look and feel instance"));
            return null;
        }
        String path = i.get_default_icon_path();
        if (path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BADBADBAD: cannot get default icon path"));
            return null;
        }
        default_icon = load_icon_fx_from_jar(path, icon_size);
        return default_icon;
    }


    //**********************************************************
    public static Image get_denied_icon(double icon_size)
    //**********************************************************
    {
        if (denied_icon != null)
        {
            if ( denied_icon.getHeight() == icon_size) return denied_icon;
        }
        Look_and_feel i = get_instance();
        if (i == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BADBADBAD: cannot get look and feel instance"));
            return null;
        }
        String path = i.get_denied_icon_path();
        if (path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BADBADBAD: cannot get denied icon path"));
            return null;
        }
        denied_icon = load_icon_fx_from_jar(path, icon_size);
        return denied_icon;
    }


    //**********************************************************
    public static Image get_trash_icon(double icon_size)
    //**********************************************************
    {
        if (trash_icon != null)
        {
            if ( trash_icon.getHeight() == icon_size) return trash_icon;
        }
        Look_and_feel i = get_instance();
        if (i == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BADBADBAD: cannot get look and feel instance"));
            return null;
        }
        String path = i.get_trash_icon_path();
        if (path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BADBADBAD: cannot get trash icon path"));
            return null;
        }
        trash_icon = load_icon_fx_from_jar(path, icon_size);
        return trash_icon;
    }


    //**********************************************************
    public static Image get_up_icon(double icon_size)
    //**********************************************************
    {
        if (up_icon != null)
        {
            if ( up_icon.getHeight() == icon_size) return up_icon;
        }
        Look_and_feel i = get_instance();
        if (i == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BADBADBAD: cannot get look and feel instance"));
            return null;
        }
        String path = i.get_up_icon_path();
        if (path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BADBADBAD: cannot get up icon path"));
            return null;
        }
        up_icon = load_icon_fx_from_jar(path, icon_size);
        return up_icon;
    }


    //**********************************************************
    public static Image get_bookmarks_icon(double icon_size)
    //**********************************************************
    {
        if (bookmarks_icon != null)
        {
            if ( bookmarks_icon.getHeight() == icon_size) return bookmarks_icon;
        }
        Look_and_feel i = get_instance();
        if (i == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BADBADBAD: cannot get look and feel instance"));
            return null;
        }
        String path = i.get_bookmarks_icon_path();
        if (path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BADBADBAD: cannot get up bookmarks path"));
            return null;
        }
        bookmarks_icon = load_icon_fx_from_jar(path, icon_size);
        return bookmarks_icon;
    }

    //**********************************************************
    public static Image get_view_icon(double icon_size)
    //**********************************************************
    {
        if (view_icon != null)
        {
            if ( view_icon.getHeight() == icon_size) return view_icon;
        }
        Look_and_feel i = get_instance();
        if (i == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BADBADBAD: cannot get look and feel instance"));
            return null;
        }
        String path = i.get_view_icon_path();
        if (path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BADBADBAD: cannot get up view icon path"));
            return null;
        }
        view_icon = load_icon_fx_from_jar(path, icon_size);
        return view_icon;
    }

    //**********************************************************
    public static Image get_preferences_icon(double icon_size)
    //**********************************************************
    {
        if (preferences_icon != null)
        {
            if ( preferences_icon.getHeight() == icon_size) return preferences_icon;
        }
        Look_and_feel i = get_instance();
        if (i == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BADBADBAD: cannot get look and feel instance"));
            return null;
        }
        String path = i.get_preferences_icon_path();
        if (path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BADBADBAD: cannot get up preferences icon path"));
            return null;
        }
        preferences_icon = load_icon_fx_from_jar(path, icon_size);
        return preferences_icon;
    }


    //**********************************************************
    public static Image get_broken_icon(double icon_size)
    //**********************************************************
    {
        if (broken_icon != null)
        {
            if ( broken_icon.getHeight() == icon_size) return broken_icon;
        }
        Look_and_feel i = get_instance();
        if (i == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BADBADBAD: cannot get look and feel instance"));
            return null;
        }
        String path = i.get_broken_icon_path();
        if (path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BADBADBAD: cannot get broken icon path"));
            return null;
        }
        broken_icon = load_icon_fx_from_jar(path, icon_size);
        return broken_icon;
    }


    //**********************************************************
    public static Image get_not_found_icon(double icon_size)
    //**********************************************************
    {
        if (not_found_icon != null)
        {
            if ( not_found_icon.getHeight() == icon_size) return not_found_icon;
        }
        Look_and_feel i = get_instance();
        if (i == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BADBADBAD: cannot get look and feel instance"));
            return null;
        }
        String path = i.get_not_found_icon_path();
        if (path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BADBADBAD: cannot get not_found icon path"));
            return null;
        }
        not_found_icon = load_icon_fx_from_jar(path, icon_size);
        return not_found_icon;
    }


    //**********************************************************
    public static Image get_unknown_error_icon(double icon_size)
    //**********************************************************
    {
        if (unknown_error_icon != null)
        {
            if ( unknown_error_icon.getHeight() == icon_size) return unknown_error_icon;
        }
        Look_and_feel i = get_instance();
        if (i == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BADBADBAD: cannot get look and feel instance"));
            return null;
        }
        String path = i.get_unknown_error_icon_path();
        if (path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BADBADBAD: cannot get unknown_error icon path"));
            return null;
        }
        unknown_error_icon = load_icon_fx_from_jar(path, icon_size);
        return unknown_error_icon;
    }



    //**********************************************************
    public static Image load_icon_fx_from_jar(String image_file_path, double icon_size)
    //**********************************************************
    {

        if (icon_load_dbg)
        {
            logger.log("looking for icon->" + image_file_path + "<-");
            {
                String path = "";
                URL url1 = Klik_application.class.getResource(path);
                if (url1 == null)
                {
                    logger.log("Method1 fails: Klik_application.class.getResource(" + path + ");  failed");
                }
                else
                {
                    logger.log("Method1 works: Klik_application.class.getResource(" + path + ");" + url1.getPath());
                }
            }
            {
                String path = ".";
                URL url2 = Klik_application.class.getResource(path);
                if (url2 == null)
                {
                    logger.log("Method2 fails: Klik_application.class.getResource(" + path + ");  failed");
                }
                else
                {
                    logger.log("Method2 works: Klik_application.class.getResource(" + path + ")" + url2.getPath());
                }
            }
            {
                String path = "../";
                URL url3 = Klik_application.class.getResource(path);
                if (url3 == null)
                {
                    logger.log("Method3 fails: Klik_application.class.getResource(" + path + ");  failed");
                }
                else
                {
                    logger.log("Method3 works: Klik_application.class.getResource(" + path + "); " + url3.getPath());
                }
            }
            {
                String classpath = System.getProperty("java.class.path");
                URL url5 = Klik_application.class.getResource(classpath);
                if (url5 == null)
                {
                    logger.log("Method5 failed");// this is a long string to print
                    // : classpath->"+classpath+"<-");
                }
                else
                {
                    logger.log("Method5 works: classpath " + url5.getPath());
                }
            }
        }

        /*
        this gives the original source path: not the one being deployed
        URL url_loader = Klik_application.class.getProtectionDomain().getCodeSource().getLocation();
        logger.log("===Klik_application.class.getProtectionDomain().getCodeSource().getLocation()====" + url_loader.toString() );
        logger.log("===getProtectionDomain().getCodeSource().getLocation().getPath()====" + url_loader.getPath() );
        */

        URL url4 = Klik_application.class.getResource(image_file_path);
        if (url4 == null)
        {
            logger.log("Method4 failed :Klik_application.class.getResource(" + image_file_path + ");  failed");
            InputStream input_stream = Klik_application.class.getResourceAsStream(image_file_path);
            if (input_stream == null)
            {
                logger.log("Method4 bis failed");
                return null;
            }
            logger.log("Method4 bis worked");

            //requestedWidth=0 means the resizing preserves aspect ratio and targets the HEIGHT
            Image image = new Image(input_stream, 0, icon_size, true, true);

            if (image.isError())
            {
                //return null;
                logger.log("WARNING: an error occurred when reading: " + image_file_path);


                return get_broken_icon(icon_size);
            }
            return image;
        }
        if (icon_load_dbg) logger.log("Method4 works :Klik_application.class.getResource(" + image_file_path + ") path:" + url4.getPath());

        if (icon_load_dbg) logger.log("path for icon=" + url4.getPath());

        try (InputStream input_stream = Klik_application.class.getResourceAsStream(image_file_path))
        {

            if ( input_stream == null)
            {
                return null;
            }
            Image image = new Image(input_stream, icon_size, icon_size, true, true);

            if (image.isError())
            {
                logger.log("WARNING: an error occurred when reading: " + image_file_path);
                return get_broken_icon(icon_size);
            }
            return image;
        }
        catch (IOException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace_for_throwable(e));
        }


        return get_broken_icon(icon_size);
    }


    /*
    //**********************************************************
    public static Image get_default_icon(double icon_size)
    //**********************************************************
    {
        if (default_icon == null)
        {
            load_default_icon(icon_size);
        }
        if (default_icon == null) return null;
        if (default_icon.getHeight() != icon_size)
        {
            load_default_icon(icon_size);
        }
        return default_icon;
    }

    //**********************************************************
    private static void load_default_icon(double icon_size)
    //**********************************************************
    {
        default_icon = load_icon_fx_from_jar(Objects.requireNonNull(get_instance()).get_default_image_path(), icon_size);
    }

    //**********************************************************
    private static Image load_denied_icon(double icon_size)
    //**********************************************************
    {
        denied_icon = load_icon_fx_from_jar(Objects.requireNonNull(get_instance()).get_denied_icon_path(), icon_size);
        return denied_icon;
    }


    //**********************************************************
    public static Image get_denied_icon(double icon_size)
    //**********************************************************
    {
        if (denied_icon == null) denied_icon = load_denied_icon(icon_size);
        return denied_icon;
    }


    //**********************************************************
    public static Image get_not_found_icon(double icon_size)
    //**********************************************************
    {
        if (not_found_icon == null) not_found_icon = load_not_found_icon(icon_size);
        return not_found_icon;
    }
    //**********************************************************
    private static Image load_not_found_icon(double icon_size)
    //**********************************************************
    {
        not_found_icon = load_icon_fx_from_jar(Objects.requireNonNull(get_instance()).get_not_found_icon_path(), icon_size);
        return not_found_icon;
    }


    //**********************************************************
    public static Image get_default_trash_icon(double icon_size)
    //**********************************************************
    {
        if (trash_icon == null)
        {
            trash_icon = load_icon_fx_from_jar(Objects.requireNonNull(get_instance()).get_trash_icon_path(), icon_size);
        }
        return trash_icon;
    }




    //**********************************************************
    private static Image load_unknown_error_icon(double icon_size)
    //**********************************************************
    {
        unknown_error_icon = load_icon_fx_from_jar(Objects.requireNonNull(get_instance()).get_unknown_error_icon_path(), icon_size);
        return unknown_error_icon;
    }

    //**********************************************************
    public static Image get_unknown_error_icon(double icon_size)
    //**********************************************************
    {
        if (unknown_error_icon == null) unknown_error_icon = load_unknown_error_icon(icon_size);
        return unknown_error_icon;
    }


    //**********************************************************
    public static Image get_broken_icon(double icon_size)
    //**********************************************************
    {
        if (broken_icon == null) load_broken_icon(icon_size);
        return broken_icon;
    }

    //**********************************************************
    private static void load_broken_icon(double icon_size)
    //**********************************************************
    {
        broken_icon = load_icon_fx_from_jar(Objects.requireNonNull(get_instance()).get_broken_icon_path(), icon_size);
    }

    //**********************************************************
    public static Image get_default_up_icon(double icon_size)
    //**********************************************************
    {
        if (up_icon == null)
        {
            up_icon = load_icon_fx_from_jar(Objects.requireNonNull(get_instance()).get_up_icon_path(), icon_size);
        }
        return up_icon;
    }
*/








    /**********************************************************



                        CSS STYLE SECTION




     *///**********************************************************



    //**********************************************************
    public static void set_dialog_look(Dialog dialog) // Dialog is NOT a node, it is completely appart
    //**********************************************************
    {
        DialogPane dialog_pane = dialog.getDialogPane();
        Look_and_feel laf = get_instance();
        if (laf.style_sheet_url_string != null) {
            dialog_pane.getStylesheets().clear();
            dialog_pane.getStylesheets().add(laf.style_sheet_url_string);
            dialog_pane.getStyleClass().add("my_dialog");
        }
        //Font_size.set_preferred_font_size(dialog_pane,logger);
        Font_size.apply_font_size(dialog_pane,logger);
    }

    /*

                    NODE

     */


    //**********************************************************
    public static void give_button_a_directory_style(Node node)
    //**********************************************************
    {
        if (node instanceof Button button)
        {
            button.setAlignment(Pos.BASELINE_LEFT);
        }
        (get_instance()).set_directory_style(node);
    }
    //**********************************************************
    public static void give_button_a_file_style(Node node)
    //**********************************************************
    {
        if (node instanceof Button button)
        {
            button.setAlignment(Pos.BASELINE_LEFT);
        }
        (get_instance()).set_file_style(node);
    }
    //**********************************************************
    public static void give_button_a_selected_file_style(Node node)
    //**********************************************************
    {
        // a klik browser "button" has 2 graphical components
        // because there is a Label in the button
        // and "folders with icon" actually have a VBox and a label...
        //logger.log(Stack_trace_getter.get_stack_trace("give_button_a_selected_file_style"+node1+" "+node2 ));

        if ( node == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("node is null"));
            return;
        }
        if (node instanceof Button button)
        {
            button.setAlignment(Pos.BASELINE_LEFT);
        }
        (get_instance()).set_selected_file_style(node);
    }



    /*

                    REGION

     */


    // some regions are not affected by the global CSS
    // this is the case for sub windows and dialogs
    // but maybe also others? unclear
    //**********************************************************
    public static void set_region_look(Region region) // Region is a Node via Parent
    //**********************************************************
    {
        Look_and_feel laf = get_instance();
        if (laf.style_sheet_url_string != null) {
            region.getStylesheets().clear();
            region.getStylesheets().add(laf.style_sheet_url_string);
            region.getStyleClass().clear();
            region.getStyleClass().add("image-window");
        }
        //Font_size.set_preferred_font_size(region,logger);
        Font_size.apply_font_size(region,logger);
    }

/*
    //**********************************************************
    public static void set_label_look_for_folder(Label label) // Label is a Region
    //**********************************************************
    {
        //String s = Look_and_feel_manager.get_instance().get_folder_icon_path();
        //if (s == null) logger.log("WARNING: could not load folder icon");
        set_label_look(label);
    }
*/

    //**********************************************************
    public static void set_label_look(Label label)
    //**********************************************************
    {
        Look_and_feel laf = Look_and_feel_manager.get_instance();
        if (laf.style_sheet_url_string != null)
        {
            label.getStylesheets().clear();
            label.getStylesheets().add(laf.style_sheet_url_string);
            label.getStyleClass().add(Look_and_feel.LOOK_AND_FEEL_MENU_BUTTONS);
        }
        Font_size.apply_font_size(label,logger);
    }


    /*

                    Button

     */


    //**********************************************************
    public static void set_button_look_as_folder(Button button, double icon_height, Color color) // Button is a region
    //**********************************************************
    {
        if ( folder_icon == null) {
            folder_icon = get_folder_icon(icon_height);
            //logger.log("set_button_look_as_folder = "+icon_height);
            //String s = Look_and_feel_manager.get_instance().get_folder_icon_path();
            //if (s == null) logger.log("WARNING: could not load folder icon");
            //folder_icon = load_icon_fx_from_jar(s, icon_height);
            //if (folder_icon == null) logger.log("WARNING: could not load " + s);
        }
        set_button_and_image_look(button, folder_icon, icon_height, color,true);
    }

    //**********************************************************
    public static void set_context_menu_look(ContextMenu context_menu)
    //**********************************************************
    {
        context_menu.getStyleClass().add("context-menu");
        //Font_size.set_preferred_font_size(context_menu,logger);
        Font_size.apply_font_size(context_menu,logger);
    }


    //**********************************************************
    public static void set_CheckBox_look(CheckBox check_box)
    //**********************************************************
    {
        //check_box.getStyleClass().clear();
        //check_box.getStyleClass().add("check-box");
        Font_size.apply_font_size(check_box,logger);
        /*
        Look_and_feel laf = Look_and_feel_manager.get_instance();
        if (laf.style_sheet_url_string != null)
        {
            check_box.setBorder(new Border(new BorderStroke(laf.get_foreground_color(), BorderStrokeStyle.SOLID,new CornerRadii(5),new BorderWidths(1))));
        }
        */
    }


    //**********************************************************
    public static void set_TextField_look(TextField text_field)
    //**********************************************************
    {
        Font_size.apply_font_size(text_field,logger);
        Look_and_feel laf = Look_and_feel_manager.get_instance();
        if (laf.style_sheet_url_string != null)
        {
            text_field.setBorder(new Border(new BorderStroke(laf.get_foreground_color(), BorderStrokeStyle.SOLID,new CornerRadii(5),new BorderWidths(1))));
        }

    }


    //**********************************************************
    public static void set_button_and_image_look(Button button,
                                                 Image image,
                                                 double height,
                                                 Color color,
                                                 boolean is_dir) // Button is a Region
    //**********************************************************
    {
        Look_and_feel laf = Look_and_feel_manager.get_instance();
        if (laf.style_sheet_url_string != null)
        {
            button.getStylesheets().clear();
            button.getStylesheets().add(laf.style_sheet_url_string);
            button.getStyleClass().add(Look_and_feel.LOOK_AND_FEEL_MENU_BUTTONS);
        }

        if ( image != null) {
            ImageView image_view = new ImageView(image);
            image_view.setPreserveRatio(true);
            {
                //if (H < Static_application_properties.get_font_size()) H = Static_application_properties.get_font_size();
                image_view.setFitHeight(height);
            }
            if (color == null) {
                button.setGraphic(image_view);
            }
            else
            {
                HBox hbox = new HBox();
                Circle dot = new Circle(0,height,height/4,color);
                hbox.getChildren().add(dot);
                hbox.getChildren().add(image_view);
                button.setGraphic(hbox);
            }
        }

        if (look_dbg) logger.log(Stack_trace_getter.get_stack_trace("set_button_look"));
        if (is_dir)
        {
            give_button_a_directory_style(button);
        }
        else
        {
            give_button_a_file_style(button);
        }
    }

    //**********************************************************
    public static void set_button_look(Region r, boolean with_border) // Button is a Region
    //**********************************************************
    {
        Look_and_feel laf = get_instance();
        if ( laf.style_sheet_url_string !=null)
        {
            r.getStylesheets().clear();
            r.getStylesheets().add(laf.style_sheet_url_string);
            r.getStyleClass().add(Look_and_feel.LOOK_AND_FEEL_MENU_BUTTONS);
            if ( with_border)
            {
                r.setBorder(get_border());
                r.setStyle("-fx-padding: 0 2 0 2;");
            }
            //Font_size.set_preferred_font_size(button,logger);
            Font_size.apply_font_size(r,logger);

            if ( r instanceof Button button)
            {
                Node g = button.getGraphic();
                if ( g != null)
                {
                    // the button has a Label... must set the right text color
                    g.getStyleClass().add(Look_and_feel.LOOK_AND_FEEL_MENU_BUTTONS);
                }
            }

        }
    }


    //**********************************************************
    public static void set_box_look(Region r) // VBox is a region
    //**********************************************************
    {
        Look_and_feel laf = get_instance();
        Color color = laf.get_stroke_color_of_folder_items();
        r.setBorder(new Border(new BorderStroke(color, BorderStrokeStyle.SOLID, new CornerRadii(0), BorderWidths.DEFAULT)));
        r.getStylesheets().clear();
        r.getStylesheets().add(laf.style_sheet_url_string);
        r.getStyleClass().add(Look_and_feel.LOOK_AND_FEEL_MENU_BUTTONS);
    }

    //**********************************************************
    public static void set_hbox_look(HBox hbox,Color col) // VBox is a region
    //**********************************************************
    {
        Look_and_feel laf = get_instance();
        hbox.setBorder(new Border(new BorderStroke(col, BorderStrokeStyle.SOLID, new CornerRadii(0), BorderWidths.DEFAULT)));
        hbox.getStylesheets().clear();
        hbox.getStylesheets().add(laf.style_sheet_url_string);
        hbox.getStyleClass().add(Look_and_feel.LOOK_AND_FEEL_MENU_BUTTONS);
    }


    //**********************************************************
    public static void set_drag_look_for_pane(Region pane)
    //**********************************************************
    {
        Look_and_feel i = Look_and_feel_manager.get_look_and_feel_instance(logger);
        pane.setBackground(new Background(i.get_drag_fill()));

    }

    //**********************************************************
    public static BackgroundFill get_drag_fill()
    //**********************************************************
    {
        Look_and_feel laf = get_instance();
        return laf.get_drag_fill();
    }


    public static Border get_border()
    {
        Look_and_feel laf = get_instance();
        return new Border(new BorderStroke(laf.get_foreground_color(), BorderStrokeStyle.SOLID,new CornerRadii(5),new BorderWidths(1)));
    }
}
