//SOURCES ../browser/Drag_and_drop.java
//SOURCES ./styles/*.java
package klik.look;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.stage.Window;
import klik.browser.Drag_and_drop;
import klik.browser.icons.JavaFX_to_Swing;
import klik.look.styles.Look_and_feel_custom_color;
import klik.look.styles.Look_and_feel_light;
import klik.properties.Non_booleans_properties;
import klik.properties.boolean_features.Feature_cache;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;

import java.awt.*;
import java.awt.image.BufferedImage;

import static java.awt.Taskbar.Feature.ICON_IMAGE;

//**********************************************************
public class Look_and_feel_manager
//**********************************************************
{
    // https://docs.oracle.com/javafx/2/api/javafx/scene/doc-files/cssref.html
    private static final boolean dbg = false;

    public static final String MUSIC = "Music";
    public static final String LAUNCHER = "Launcher";

    public static final boolean look_dbg = false;

    //private static Look_and_feel instance = null;
    //public static List<Look_and_feel> registered = new ArrayList<>();
    private static Image default_icon = null;
    private static Image music_icon = null;
    public static Image denied_icon = null;
    public static Image folder_icon = null;
    public static Image trash_icon = null;
    public static Image bookmarks_icon = null;
    public static Image view_icon = null;
    public static Image up_icon = null;
    public static Image preferences_icon = null;
    public static Image not_found_icon = null;
    public static Image unknown_error_icon = null;
    public static Image dummy_icon = null;

    private static Look_and_feel instance;

    //**********************************************************
    public static Look_and_feel get_instance(Window owner, Logger logger)
    //**********************************************************
    {
        if ( instance == null ) instance = read_look_and_feel_from_properties_file(owner,logger);
        return instance;
    }

    //**********************************************************
    public static Look_and_feel read_look_and_feel_from_properties_file(Window owner, Logger logger)
    //**********************************************************
    {
        //logger.log(Stack_trace_getter.get_stack_trace("\n\n\n\n\n\nLook_and_feel_manager.read_look_and_feel_from_properties_file()"));
        Look_and_feel_style look_and_feel_style = null;
        String style_s = Non_booleans_properties.get_main_properties_manager(owner).get(Non_booleans_properties.STYLE_KEY);
        boolean and_save = false;
        if (style_s == null)
        {
            // DEFAULT STYLE, first time klik is launched on the platform
            look_and_feel_style = Look_and_feel_style.light;
            and_save = true;
        }
        else
        {
            for (Look_and_feel_style laf : Look_and_feel_style.values()) {
                if (laf.name().equals(style_s)) {
                    look_and_feel_style = laf;
                    break;
                }
            }
            if ( look_and_feel_style == null)
            {
                // the style is not known, so we set it to light
                look_and_feel_style = Look_and_feel_style.light;
                and_save = true;
            }
        }

        if ( and_save) Non_booleans_properties.get_main_properties_manager(owner).set(Non_booleans_properties.STYLE_KEY, look_and_feel_style.name());
        if (dbg) logger.log("read_look_and_feel_from_properties_file: using style " + look_and_feel_style.name());
        return  switch (look_and_feel_style)
        {
            default -> new Look_and_feel_light(owner,logger);
            case dark -> new klik.look.styles.Look_and_feel_dark(owner,logger);
            case wood ->new klik.look.styles.Look_and_feel_wood(owner,logger);
            case custom_color -> new Look_and_feel_custom_color(owner,logger);
        };
    }

    //**********************************************************
    public static void set_drag_look_for_pane(Region pane, Window owner, Logger logger)
    //**********************************************************
    {
        Look_and_feel i = get_instance(owner,logger);
        pane.setBackground(new Background(i.get_drag_fill()));

    }


    //**********************************************************
    public static void set_look_and_feel(Look_and_feel_style style,  Window owner, Logger logger)
    //**********************************************************
    {
        //logger.log(Stack_trace_getter.get_stack_trace("setting style = " + style.name));
        logger.log(("setting style = " + style.name()));
        Feature_cache.update_string(Non_booleans_properties.STYLE_KEY,style.name(),owner,logger);
        reset();
    }

    //**********************************************************
    public static void reset()
    //**********************************************************
    {
        instance = null;
        default_icon = null;
        music_icon = null;
        Jar_utils.broken_icon = null;
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
    public static Image get_dummy_icon(double icon_size, Window owner,Logger logger)
    //**********************************************************
    {
        if (dummy_icon != null)
        {
            if ( dummy_icon.getHeight() == icon_size) return dummy_icon;
        }
        Look_and_feel local_instance = get_instance(owner,logger);
        if (local_instance == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BAD WARNING: cannot get look and feel instance"));
            return null;
        }
        String path = local_instance.get_dummy_icon_path();
        if (path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BAD WARNING: cannot get dummy icon path"));
            return null;
        }
        dummy_icon = Jar_utils.load_jfx_image_from_jar(path, icon_size, owner,logger);
        return dummy_icon;
    }
    //**********************************************************
    public static Image get_folder_icon(double icon_size, Window owner,Logger logger)
    //**********************************************************
    {
        if (folder_icon != null)
        {
            if ( folder_icon.getHeight() == icon_size) return folder_icon;
        }
        Look_and_feel local_instance = get_instance(owner,logger);
        if (local_instance == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BAD WARNING: cannot get look and feel instance"));
            return null;
        }
        String path = local_instance.get_folder_icon_path();
        if (path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BAD WARNING: cannot get folder icon path"));
            return null;
        }
        folder_icon = Jar_utils.load_jfx_image_from_jar(path, icon_size, owner,logger);
        return folder_icon;
    }

    //**********************************************************
    public static Image get_speaker_icon(Window owner, Logger logger)
    //**********************************************************
    {
        Look_and_feel local_instance = get_instance(owner,logger);
        if (local_instance == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BAD WARNING: cannot get look and feel instance"));
            return null;
        }
        String path = local_instance.get_speaker_icon_path();
        if (path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BAD WARNING: cannot get folder icon path"));
            return null;
        }
        return Jar_utils.load_jfx_image_from_jar(path, 256, owner,logger);
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
            logger.log(Stack_trace_getter.get_stack_trace("BAD WARNING: cannot get look and feel instance"));
            return null;
        }
        String path = i.get_folder_icon_path();
        if (path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BAD WARNING: cannot get folder icon path"));
            return null;
        }
        large_folder_icon = Jar_utils.load_icon_fx_from_jar(path, icon_size);
        return large_folder_icon;
    }



    //**********************************************************
    public static Path get_folder_icon_path()
    //**********************************************************
    {
        Look_and_feel i = get_instance();
        if (i == null) {
            logger.log(Stack_trace_getter.get_stack_trace("BAD WARNING: cannot get look and feel instance"));
            return null;
        }
        String path = i.get_folder_icon_path();
        if (path == null) {
            logger.log(Stack_trace_getter.get_stack_trace("BAD WARNING: cannot get folder icon path"));
            return null;
        }
        return Path.of(path);
    }
    */

    //**********************************************************
    public static Image get_default_icon(double icon_size, Window owner,Logger logger)
    //**********************************************************
    {
        if (default_icon != null)
        {
            if ( default_icon.getHeight() == icon_size) return default_icon;
        }
        Look_and_feel local_instance = get_instance(owner,logger);
        if (local_instance == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BAD WARNING: cannot get look and feel instance"));
            return null;
        }
        String path = local_instance.get_default_icon_path();
        if (path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BAD WARNING: cannot get default icon path"));
            return null;
        }
        default_icon = Jar_utils.load_jfx_image_from_jar(path, icon_size, owner,logger);
        return default_icon;
    }


    //**********************************************************
    public static Image get_music_icon(double icon_size, Window owner, Logger logger)
    //**********************************************************
    {
        if (music_icon != null)
        {
            if ( music_icon.getHeight() == icon_size) return music_icon;
        }
        Look_and_feel local_instance = get_instance(owner,logger);
        if (local_instance == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BAD WARNING: cannot get look and feel instance"));
            return null;
        }
        String path = local_instance.get_music_icon_path();
        if (path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BAD WARNING: cannot get music icon path"));
            return null;
        }
        music_icon = Jar_utils.load_jfx_image_from_jar(path, icon_size, owner,logger);
        return music_icon;
    }


    //**********************************************************
    public static Image get_denied_icon(double icon_size, Window owner, Logger logger)
    //**********************************************************
    {
        if (denied_icon != null)
        {
            if ( denied_icon.getHeight() == icon_size) return denied_icon;
        }
        Look_and_feel local_instance = get_instance(owner,logger);
        if (local_instance == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BAD WARNING: cannot get look and feel instance"));
            return null;
        }
        String path = local_instance.get_denied_icon_path();
        if (path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BAD WARNING: cannot get denied icon path"));
            return null;
        }
        denied_icon = Jar_utils.load_jfx_image_from_jar(path, icon_size, owner,logger);
        return denied_icon;
    }


    //**********************************************************
    public static Image get_trash_icon(double icon_size, Window owner, Logger logger)
    //**********************************************************
    {
        if (trash_icon != null)
        {
            if ( trash_icon.getHeight() == icon_size) return trash_icon;
        }
        Look_and_feel local_instance = get_instance(owner,logger);
        if (local_instance == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BAD WARNING: cannot get look and feel instance"));
            return null;
        }
        String path = local_instance.get_trash_icon_path();
        if (path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BAD WARNING: cannot get trash icon path"));
            return null;
        }
        trash_icon = Jar_utils.load_jfx_image_from_jar(path, icon_size, owner,logger);
        return trash_icon;
    }


    //**********************************************************
    public static Image get_up_icon(double icon_size, Window owner, Logger logger)
    //**********************************************************
    {
        if (up_icon != null)
        {
            if ( up_icon.getHeight() == icon_size) return up_icon;
        }
        Look_and_feel local_instance = get_instance(owner,logger);
        if (local_instance == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BAD WARNING: cannot get look and feel instance"));
            return null;
        }
        String path = local_instance.get_up_icon_path();
        if (path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BAD WARNING: cannot get up icon path"));
            return null;
        }
        up_icon = Jar_utils.load_jfx_image_from_jar(path, icon_size, owner,logger);
        return up_icon;
    }


    //**********************************************************
    public static Image get_bookmarks_icon(double icon_size, Window owner, Logger logger)
    //**********************************************************
    {
        if (bookmarks_icon != null)
        {
            if ( bookmarks_icon.getHeight() == icon_size) return bookmarks_icon;
        }
        Look_and_feel local_instance = get_instance(owner,logger);
        if (local_instance == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BAD WARNING: cannot get look and feel instance"));
            return null;
        }
        String path = local_instance.get_bookmarks_icon_path();
        if (path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BAD WARNING: cannot get up bookmarks path"));
            return null;
        }
        bookmarks_icon = Jar_utils.load_jfx_image_from_jar(path, icon_size,owner,logger);
        return bookmarks_icon;
    }

    //**********************************************************
    public static Image get_view_icon(double icon_size, Window owner, Logger logger)
    //**********************************************************
    {
        if (view_icon != null)
        {
            if ( view_icon.getHeight() == icon_size) return view_icon;
        }
        Look_and_feel local_instance = get_instance(owner,logger);
        if (local_instance == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BAD WARNING: cannot get look and feel instance"));
            return null;
        }
        String path = local_instance.get_view_icon_path();
        if (path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BAD WARNING: cannot get up view icon path"));
            return null;
        }
        view_icon = Jar_utils.load_jfx_image_from_jar(path, icon_size, owner,logger);
        return view_icon;
    }

    //**********************************************************
    public static Image get_preferences_icon(double icon_size, Window owner, Logger logger)
    //**********************************************************
    {
        if (preferences_icon != null)
        {
            if ( preferences_icon.getHeight() == icon_size) return preferences_icon;
        }
        Look_and_feel local_instance = get_instance(owner,logger);
        if (local_instance == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BAD WARNING: cannot get look and feel instance"));
            return null;
        }
        String path = local_instance.get_preferences_icon_path();
        if (path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BAD WARNING: cannot get up preferences icon path"));
            return null;
        }
        preferences_icon = Jar_utils.load_jfx_image_from_jar(path, icon_size, owner,logger);
        return preferences_icon;
    }




    //**********************************************************
    public static Image get_not_found_icon(double icon_size, Window owner, Logger logger)
    //**********************************************************
    {
        if (not_found_icon != null)
        {
            if ( not_found_icon.getHeight() == icon_size) return not_found_icon;
        }
        Look_and_feel local_instance = get_instance(owner,logger);
        if (local_instance == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BAD WARNING: cannot get look and feel instance"));
            return null;
        }
        String path = local_instance.get_not_found_icon_path();
        if (path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BAD WARNING: cannot get not_found icon path"));
            return null;
        }
        not_found_icon = Jar_utils.load_jfx_image_from_jar(path, icon_size, owner,logger);
        return not_found_icon;
    }


    //**********************************************************
    public static Image get_unknown_error_icon(double icon_size, Window owner, Logger logger)
    //**********************************************************
    {
        if (unknown_error_icon != null)
        {
            if ( unknown_error_icon.getHeight() == icon_size) return unknown_error_icon;
        }
        Look_and_feel local_instance = get_instance(owner,logger);
        if (local_instance == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BAD WARNING: cannot get look and feel instance"));
            return null;
        }
        String path = local_instance.get_unknown_error_icon_path();
        if (path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BAD WARNING: cannot get unknown_error icon path"));
            return null;
        }
        unknown_error_icon = Jar_utils.load_jfx_image_from_jar(path, icon_size, owner,logger);
        return unknown_error_icon;
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
        default_icon = Jar_utils.load_icon_fx_from_jar(Objects.requireNonNull(get_instance()).get_default_image_path(), icon_size);
    }

    //**********************************************************
    private static Image load_denied_icon(double icon_size)
    //**********************************************************
    {
        denied_icon = Jar_utils.load_icon_fx_from_jar(Objects.requireNonNull(get_instance()).get_denied_icon_path(), icon_size);
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
        not_found_icon = Jar_utils.load_icon_fx_from_jar(Objects.requireNonNull(get_instance()).get_not_found_icon_path(), icon_size);
        return not_found_icon;
    }


    //**********************************************************
    public static Image get_default_trash_icon(double icon_size)
    //**********************************************************
    {
        if (trash_icon == null)
        {
            trash_icon = Jar_utils.load_icon_fx_from_jar(Objects.requireNonNull(get_instance()).get_trash_icon_path(), icon_size);
        }
        return trash_icon;
    }




    //**********************************************************
    private static Image load_unknown_error_icon(double icon_size)
    //**********************************************************
    {
        unknown_error_icon = Jar_utils.load_icon_fx_from_jar(Objects.requireNonNull(get_instance()).get_unknown_error_icon_path(), icon_size);
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
        broken_icon = Jar_utils.load_icon_fx_from_jar(Objects.requireNonNull(get_instance()).get_broken_icon_path(), icon_size);
    }

    //**********************************************************
    public static Image get_default_up_icon(double icon_size)
    //**********************************************************
    {
        if (up_icon == null)
        {
            up_icon = Jar_utils.load_icon_fx_from_jar(Objects.requireNonNull(get_instance()).get_up_icon_path(), icon_size);
        }
        return up_icon;
    }
*/








    /**********************************************************



                        CSS STYLE SECTION




     *///**********************************************************



    //**********************************************************
    public static void set_dialog_look(Dialog dialog, Window owner, Logger logger) // Dialog is NOT a node, it is completely appart
    //**********************************************************
    {
        DialogPane dialog_pane = dialog.getDialogPane();
        Look_and_feel laf = get_instance(owner,logger);
        if (laf.style_sheet_url_string != null) {
            dialog_pane.getStylesheets().clear();
            dialog_pane.getStylesheets().add(laf.style_sheet_url_string);
            dialog_pane.getStyleClass().add("my_dialog");
        }
        //Font_size.set_preferred_font_size(dialog_pane,logger);
        Font_size.apply_font_size(dialog_pane,owner,logger);
    }

    /*

                    NODE

     */


    //**********************************************************
    public static void give_button_a_directory_style(Node node, Window owner, Logger logger)
    //**********************************************************
    {
        if (node instanceof Button button)
        {
            button.setAlignment(Pos.BASELINE_LEFT);
        }
        (get_instance(owner,logger)).set_directory_style(node,owner);
    }
    //**********************************************************
    public static void give_button_a_file_style(Node node, Window owner, Logger logger)
    //**********************************************************
    {
        if (node instanceof Button button)
        {
            button.setAlignment(Pos.BASELINE_LEFT);
        }
        (get_instance(owner,logger)).set_file_style(node,owner);
    }
    //**********************************************************
    public static void give_button_a_selected_file_style(Node node, Window owner, Logger logger)
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
        (get_instance(owner,logger)).set_selected_file_style(node,owner);
    }



    /*

                    REGION

     */


    // some regions are not affected by the global CSS
    // this is the case for sub windows and dialogs
    // but maybe also others? unclear
    //**********************************************************
    public static void set_region_look(Region region, Window owner, Logger logger) // Region is a Node via Parent
    //**********************************************************
    {
        Look_and_feel laf = get_instance(owner,logger);
        if (laf.style_sheet_url_string != null) {
            region.getStylesheets().clear();
            region.getStylesheets().add(laf.style_sheet_url_string);
            region.getStyleClass().clear();
            region.getStyleClass().add("image-window");
        }
        Font_size.apply_font_size(region,owner,logger);
    }


    //**********************************************************
    public static void set_label_look(Label label, Window owner, Logger logger)
    //**********************************************************
    {
        Look_and_feel laf = get_instance(owner,logger);
        if (laf.style_sheet_url_string != null)
        {
            label.getStylesheets().clear();
            label.getStylesheets().add(laf.style_sheet_url_string);
            label.getStyleClass().add(Look_and_feel.LOOK_AND_FEEL_MENU_BUTTONS);
        }
        Font_size.apply_font_size(label,owner,logger);
    }


    /*

                    Button

     */


    //**********************************************************
    public static void set_button_look_as_folder(Button button, double icon_height, Color color, Window owner, Logger logger) // Button is a region
    //**********************************************************
    {
        if ( folder_icon == null)
        {
            folder_icon = get_folder_icon(icon_height,owner,logger);
        }
        set_button_and_image_look(button, folder_icon, icon_height, color,true,owner,logger);
    }

    //**********************************************************
    public static void set_context_menu_look(ContextMenu context_menu, Window owner, Logger logger)
    //**********************************************************
    {
        context_menu.getStyleClass().add("context-menu");
        //Font_size.set_preferred_font_size(context_menu,logger);
        Font_size.apply_font_size(context_menu,owner,logger);
    }


    //**********************************************************
    public static void set_CheckBox_look(CheckBox check_box, Window owner, Logger logger)
    //**********************************************************
    {
        Look_and_feel laf = get_instance(owner,logger);
        if (laf.style_sheet_url_string != null)
        {
            check_box.getStylesheets().clear();
            check_box.getStylesheets().add(laf.style_sheet_url_string);
            check_box.getStyleClass().clear();
            check_box.getStyleClass().add("check-box");
        }
        Font_size.apply_font_size(check_box,owner,logger);
    }


    //**********************************************************
    public static void set_TextField_look(TextField text_field, Window owner, Logger logger)
    //**********************************************************
    {
        Font_size.apply_font_size(text_field,owner,logger);
        Look_and_feel laf = get_instance(owner,logger);
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
                                                 boolean is_dir,
                                                 Window owner, Logger logger) // Button is a Region
    //**********************************************************
    {
        Look_and_feel laf = get_instance(owner,logger);
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
                //if (H < Non_booleans_properties.get_font_size()) H = Non_booleans_properties.get_font_size();
                image_view.setFitHeight(height);
            }
            if (color == null) {
                button.setGraphic(image_view);
            }
            else
            {
                HBox hbox = new HBox();
                Circle dot = new Circle(height/4,color);
                dot.setTranslateY(height/4);
                hbox.getChildren().add(dot);
                hbox.getChildren().add(image_view);
                button.setGraphic(hbox);
            }
        }

        if (look_dbg) logger.log(Stack_trace_getter.get_stack_trace("set_button_look"));
        if (is_dir)
        {
            give_button_a_directory_style(button,owner,logger);
        }
        else
        {
            give_button_a_file_style(button,owner,logger);
        }
    }

    //**********************************************************
    public static void set_button_look(Region region, boolean with_border, Window owner, Logger logger) // Button is a Region
    //**********************************************************
    {
        Look_and_feel laf = get_instance(owner,logger);
        if ( laf.style_sheet_url_string !=null)
        {
            region.getStylesheets().clear();
            region.getStylesheets().add(laf.style_sheet_url_string);
            region.getStyleClass().add(Look_and_feel.LOOK_AND_FEEL_MENU_BUTTONS);
            if ( with_border)
            {
                region.setBorder(get_border(owner,logger));
                region.setStyle("-fx-padding: 0 2 0 2;");
            }
            //Font_size.set_preferred_font_size(button,logger);
            Font_size.apply_font_size(region,owner,logger);

            if ( region instanceof Button button)
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
    public static void set_background_for_setOnDragEntered(Node node, Window owner, Logger logger)
    //**********************************************************
    {
        BackgroundFill background_fill = Look_and_feel_manager.get_drag_fill(owner,logger);
        
        Look_and_feel_manager.set_background(node, background_fill,owner,logger);
    }

    //**********************************************************
    public static void set_background_for_setOnDragOver(Node node, Window owner, Logger logger)
    //**********************************************************
    {
        set_background_for_setOnDragEntered(node, owner,logger);
    }

    //**********************************************************
    public static void set_background_for_setOnDragExited(Node node, Window owner, Logger logger)
    //**********************************************************
    {
        Look_and_feel i = get_instance(owner,logger);
        BackgroundFill color = i.get_background_fill();
        if (Drag_and_drop.drag_and_drop_dbg) logger.log("Item_folder_with_icon setOnDragExited color = "+color);
        Look_and_feel_manager.set_background(node, color,owner,logger);

    }
    //**********************************************************
    public static void set_background(Node n, BackgroundFill background_fill, Window owner, Logger logger)
    //**********************************************************
    {
        if ( n instanceof Button button)
        {
            button.setBackground(new Background(background_fill));
            Node node = button.getGraphic();
            if (node instanceof Label label)
            {
                Look_and_feel_manager.set_label_look(label,owner,logger);
            }
        }
        else if ( n instanceof FlowPane flow_pane)
        {
            flow_pane.setBackground(new Background(background_fill));
        }
    }

    /*
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
*/

    //**********************************************************
    public static BackgroundFill get_drag_fill(Window owner, Logger logger)
    //**********************************************************
    {
        Look_and_feel laf = get_instance(owner,logger);
        return laf.get_drag_fill();
    }


    //**********************************************************
    public static Border get_border(Window owner, Logger logger)
    //**********************************************************
    {
        Look_and_feel laf = get_instance(owner,logger);
        return new Border(new BorderStroke(laf.get_foreground_color(), BorderStrokeStyle.SOLID,new CornerRadii(5),new BorderWidths(1)));
    }

    //**********************************************************
    public static Image get_running_film_icon(Window owner, Logger logger)
    //**********************************************************
    {
        Look_and_feel i = get_instance(owner,logger);
        if (i == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BAD WARNING: cannot get look and feel instance"));
            return null;
        }
        String path = i.get_running_film_icon_path();
        if (path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BAD WARNING: cannot get running man icon path"));
            return null;
        }
        return Jar_utils.load_jfx_image_from_jar(path, 600, owner,logger);
    }


    //**********************************************************
    public static Image get_the_end_icon(Window owner, Logger logger)
    //**********************************************************
    {
        Look_and_feel i = get_instance(owner,logger);
        if (i == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BAD WARNING: cannot get look and feel instance"));
            return null;
        }
        String path = i.get_sleeping_man_icon_path();
        if (path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BAD WARNING: cannot get slipping_man icon path"));
            return null;
        }
        return Jar_utils.load_jfx_image_from_jar(path, 600, owner,logger);
    }


    public enum Icon_type {KLIK, MUSIC, IMAGE,LAUNCHER};

    //**********************************************************
    public static String get_main_window_icon_path(Look_and_feel look_and_feel,Icon_type icon_type)
    //**********************************************************
    {
        switch (icon_type)
        {
            case KLIK:
                return look_and_feel.get_klik_icon_path();
            case MUSIC:
                return look_and_feel.get_music_icon_path();
            case LAUNCHER:
                return look_and_feel.get_slingshot_icon_path();
            case IMAGE:
                return look_and_feel.get_default_icon_path();

        }
        return null;
    }
     //**********************************************************
    public static void set_icon_for_main_window(Stage stage, String badge_text, Icon_type icon_type, Window owner, Logger logger)
    //**********************************************************
    {
        Look_and_feel look_and_feel = get_instance(owner,logger);
        if (look_and_feel == null) {
            logger.log("BAD WARNING: cannot get look and feel instance");
        }

        stage.getIcons().clear();
        Image taskbar_icon = null;
        int[] icon_sizes = {16, 32, 64, 128};
        String icon_path = get_main_window_icon_path(look_and_feel, icon_type);
        for (int s : icon_sizes)
        {
            Image icon = Jar_utils.load_jfx_image_from_jar(icon_path, s, owner,logger);
            if (icon != null) {
                stage.getIcons().add(icon);
                taskbar_icon = icon;
                stage.getIcons().add(icon);
            }
        }
        if (taskbar_icon != null) {
            if (Taskbar.isTaskbarSupported()) {
                Taskbar task_bar = Taskbar.getTaskbar();
                if (task_bar.isSupported(ICON_IMAGE)) {
                    BufferedImage bim = JavaFX_to_Swing.fromFXImage(taskbar_icon, null, logger);
                    task_bar.setIconImage(bim);
                }
                if (task_bar.isSupported(Taskbar.Feature.ICON_BADGE_TEXT)) {
                    task_bar.setIconBadge(badge_text);
                }
            }
        }
    }

}
