//SOURCES ../browser/Drag_and_drop.java
//SOURCES ./styles/*.java
package klik.look;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import klik.browser.Drag_and_drop;
import klik.look.styles.Look_and_feel_dark;
import klik.look.styles.Look_and_feel_light;
import klik.look.styles.Look_and_feel_wood;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;

import java.util.ArrayList;
import java.util.List;

//**********************************************************
public class Look_and_feel_manager
//**********************************************************
{
    // https://docs.oracle.com/javafx/2/api/javafx/scene/doc-files/cssref.html


    public static final boolean icon_load_dbg = true;
    public static final boolean look_dbg = false;
    public static Logger logger;

    private static Look_and_feel instance = null;
    public static List<Look_and_feel> registered = new ArrayList<>();
    private static Image default_icon = null;
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
            logger.log(Stack_trace_getter.get_stack_trace("BAD WARNING: Look_and_feel_manager.get_instance() returns null?"));
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
        instance = Look_and_feel.read_look_and_feel_from_properties_file(logger_);
    }

    //**********************************************************
    public static void set_look_and_feel(Look_and_feel instance_)
    //**********************************************************
    {
        //logger.log(Stack_trace_getter.get_stack_trace("setting style = " + style.name));
        logger.log(("setting style = " + instance_.name));
        instance = instance_;
        Look_and_feel.set_style(instance_,logger);
        reset();
    }

    //**********************************************************
    public static void reset()
    //**********************************************************
    {
        default_icon = null;
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
    public static Image get_dummy_icon(double icon_size)
    //**********************************************************
    {
        if (dummy_icon != null)
        {
            if ( dummy_icon.getHeight() == icon_size) return dummy_icon;
        }
        Look_and_feel local_instance = get_instance();
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
        dummy_icon = Jar_utils.load_jfx_image_from_jar(path, icon_size, logger);
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
        Look_and_feel local_instance = get_instance();
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
        folder_icon = Jar_utils.load_jfx_image_from_jar(path, icon_size, logger);
        return folder_icon;
    }

    //**********************************************************
    public static Image get_speaker_icon()
    //**********************************************************
    {
        Look_and_feel local_instance = get_instance();
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
        return Jar_utils.load_jfx_image_from_jar(path, 256, logger);
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
    public static Image get_default_icon(double icon_size)
    //**********************************************************
    {
        if (default_icon != null)
        {
            if ( default_icon.getHeight() == icon_size) return default_icon;
        }
        Look_and_feel local_instance = get_instance();
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
        default_icon = Jar_utils.load_jfx_image_from_jar(path, icon_size, logger);
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
        Look_and_feel local_instance = get_instance();
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
        denied_icon = Jar_utils.load_jfx_image_from_jar(path, icon_size, logger);
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
        Look_and_feel local_instance = get_instance();
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
        trash_icon = Jar_utils.load_jfx_image_from_jar(path, icon_size, logger);
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
        Look_and_feel local_instance = get_instance();
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
        up_icon = Jar_utils.load_jfx_image_from_jar(path, icon_size, logger);
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
        Look_and_feel local_instance = get_instance();
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
        bookmarks_icon = Jar_utils.load_jfx_image_from_jar(path, icon_size,logger);
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
        Look_and_feel local_instance = get_instance();
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
        view_icon = Jar_utils.load_jfx_image_from_jar(path, icon_size, logger);
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
        Look_and_feel local_instance = get_instance();
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
        preferences_icon = Jar_utils.load_jfx_image_from_jar(path, icon_size, logger);
        return preferences_icon;
    }




    //**********************************************************
    public static Image get_not_found_icon(double icon_size)
    //**********************************************************
    {
        if (not_found_icon != null)
        {
            if ( not_found_icon.getHeight() == icon_size) return not_found_icon;
        }
        Look_and_feel local_instance = get_instance();
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
        not_found_icon = Jar_utils.load_jfx_image_from_jar(path, icon_size, logger);
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
        Look_and_feel local_instance = get_instance();
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
        unknown_error_icon = Jar_utils.load_jfx_image_from_jar(path, icon_size, logger);
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
        if ( folder_icon == null)
        {
            folder_icon = get_folder_icon(icon_height);
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
        Look_and_feel laf = get_instance();
        if (laf.style_sheet_url_string != null)
        {
            check_box.getStylesheets().clear();
            check_box.getStylesheets().add(laf.style_sheet_url_string);
            check_box.getStyleClass().clear();
            check_box.getStyleClass().add("check-box");
        }
        Font_size.apply_font_size(check_box,logger);
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
    public static void set_background_for_setOnDragEntered(Node node, Logger logger)
    //**********************************************************
    {
        BackgroundFill background_fill = Look_and_feel_manager.get_drag_fill();
        if (Drag_and_drop.drag_and_drop_dbg) logger.log("Item_folder_with_icon OnDragOver color = "+background_fill);
        Look_and_feel_manager.set_background(node, background_fill);
    }

    //**********************************************************
    public static void set_background_for_setOnDragOver(Node node, Logger logger)
    //**********************************************************
    {
        set_background_for_setOnDragEntered(node, logger);
    }

    //**********************************************************
    public static void set_background_for_setOnDragExited(Node node, Logger logger)
    //**********************************************************
    {
        Look_and_feel i = Look_and_feel_manager.get_instance();
        BackgroundFill color = i.get_background_fill();
        if (Drag_and_drop.drag_and_drop_dbg) logger.log("Item_folder_with_icon setOnDragExited color = "+color);
        Look_and_feel_manager.set_background(node, color);

    }
    //**********************************************************
    public static void set_background(Node n, BackgroundFill background_fill)
    //**********************************************************
    {
        if ( n instanceof Button button)
        {
            button.setBackground(new Background(background_fill));
            Node node = button.getGraphic();
            if (node instanceof Label label)
            {
                Look_and_feel_manager.set_label_look(label);
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


    //**********************************************************
    public static Border get_border()
    //**********************************************************
    {
        Look_and_feel laf = get_instance();
        return new Border(new BorderStroke(laf.get_foreground_color(), BorderStrokeStyle.SOLID,new CornerRadii(5),new BorderWidths(1)));
    }

    //**********************************************************
    public static Image get_running_film_icon()
    //**********************************************************
    {
        Look_and_feel i = get_instance();
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
        return Jar_utils.load_jfx_image_from_jar(path, 600, logger);
    }


    //**********************************************************
    public static Image get_sleeping_man_icon()
    //**********************************************************
    {
        Look_and_feel i = get_instance();
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
        return Jar_utils.load_jfx_image_from_jar(path, 600, logger);
    }
}
