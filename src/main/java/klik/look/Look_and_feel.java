package klik.look;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.Text;
import klik.properties.Static_application_properties;
import klik.util.Logger;

import java.net.URL;

//**********************************************************
public abstract class Look_and_feel
//**********************************************************
{
    /*
    https://docs.oracle.com/javase/8/javafx/api/javafx/scene/doc-files/cssref.html

    https://stackoverflow.com/questions/37689441/javafx-text-control-setting-the-fill-color
     */
    public static final String LOOK_AND_FEEL_GENERAL = "look_and_feel_general";
    public static final String LOOK_AND_FEEL_DRAG = "look_and_feel_drag";
    public static final String LOOK_AND_FEEL_MENU_BUTTONS = "menu_buttons";
    public static final String LOOK_AND_FEEL_ALL_FILES = "look_and_feel_all_files";
    public static final String LOOK_AND_FEEL_ALL_DIRS = "look_and_feel_all_dirs";


    public static final boolean dbg = false;
    private static final double BORDER_WIDTH = 2;
    private static final double BORDER_RADII = 7;
    public static final double MAGIC_HEIGHT_FACTOR = 2;

    public final String style_sheet_url_string;
    public final String name;
    public final Logger logger;
    private final BackgroundFill all_dirs_fill; 
    private final BackgroundFill all_files_fill; 
    private final BackgroundFill drag_fill; 
    private final BackgroundFill background_fill;

    //**********************************************************
    public Look_and_feel(String name_, Logger logger_)
    //**********************************************************
    {
        logger = logger_;
        URL style_sheet_url = get_CSS_URL();
        if (style_sheet_url == null)
        {
            style_sheet_url_string = null;
            logger.log("BADBADBAD cannot load style sheet");
        }
        else
        {
            style_sheet_url_string = style_sheet_url.toExternalForm();
            //logger.log("loaded style sheet=" + style_sheet_url_string);
        }

        name = name_;
        Look_and_feel_manager.reset();

        {
            Pane tmp_pane = new Pane();
            tmp_pane.getStyleClass().add(Look_and_feel.LOOK_AND_FEEL_GENERAL);
            Scene tmp_scene = new Scene(tmp_pane);
            tmp_scene.getStylesheets().add(style_sheet_url_string);
            tmp_pane.applyCss();
            if ( tmp_pane.getBackground() == null)
            {
                logger.log("BADBADBAD cannot read BACKGROUND color from CSS file, are you sure the syntax is correct? :"+Look_and_feel.LOOK_AND_FEEL_GENERAL);
            }
            background_fill = tmp_pane.getBackground().getFills().get(0);
        }
        {
            Pane tmp_pane = new Pane();
            tmp_pane.getStyleClass().add(Look_and_feel.LOOK_AND_FEEL_ALL_DIRS);
            Scene tmp_scene = new Scene(tmp_pane);
            tmp_scene.getStylesheets().add(style_sheet_url_string);
            tmp_pane.applyCss();
            if ( tmp_pane.getBackground() == null)
            {
                logger.log("BADBADBAD cannot read BACKGROUND color from CSS file, are you sure the syntax is correct? :"+Look_and_feel.LOOK_AND_FEEL_ALL_DIRS);
            }
            all_dirs_fill = tmp_pane.getBackground().getFills().get(0);
        }
        {
            Pane tmp_pane = new Pane();
            tmp_pane.getStyleClass().add(Look_and_feel.LOOK_AND_FEEL_ALL_FILES);
            Scene tmp_scene = new Scene(tmp_pane);
            tmp_scene.getStylesheets().add(style_sheet_url_string);
            tmp_pane.applyCss();
            if ( tmp_pane.getBackground() == null)
            {
                logger.log("BADBADBAD cannot read BACKGROUND color from CSS file, are you sure the syntax is correct? :"+Look_and_feel.LOOK_AND_FEEL_ALL_FILES);
            }
            all_files_fill= tmp_pane.getBackground().getFills().get(0);
        }
        {
            Pane tmp_pane = new Pane();
            tmp_pane.getStyleClass().add(Look_and_feel.LOOK_AND_FEEL_DRAG);
            Scene tmp_scene = new Scene(tmp_pane);
            tmp_scene.getStylesheets().add(style_sheet_url_string);
            tmp_pane.applyCss();
            if ( tmp_pane.getBackground() == null)
            {
                logger.log("BADBADBAD cannot read BACKGROUND  color from CSS file, are you sure the syntax is correct? :"+Look_and_feel.LOOK_AND_FEEL_DRAG);
            }
            drag_fill= tmp_pane.getBackground().getFills().get(0);
        }
    }

    abstract public URL get_CSS_URL();

    abstract public String get_klik_image_path();
    abstract public String get_trash_icon_path();
    abstract public String get_up_icon_path();
    abstract public String get_view_icon_path();
    abstract public String get_bookmarks_icon_path();
    abstract public String get_preferences_icon_path();
    abstract public String get_broken_icon_path();
    abstract public String get_default_icon_path();
    abstract public String get_folder_icon_path();
    protected String get_dummy_icon_path()
    {
        return "dummy.png";
    }
    protected String get_denied_icon_path()
    {
        return "denied.png";
    }
    protected String get_unknown_error_icon_path()
    {
        return "unknown-error.png";
    }
    protected String get_not_found_icon_path() { return "not-found.png";}

    public void set_hovered_directory_style(Node button){
        System.out.println("Look_and_feel::set_hovered_directory_style");
        Font_size.apply_font_size(button, logger);
    }
    protected void set_directory_style(Node node){
        Font_size.apply_font_size(node,logger);
    }
    protected void set_file_style(Node node){

        if (node instanceof Button button)
        {
            button.setFont(Font.font("Monaco", FontPosture.ITALIC, Static_application_properties.get_font_size( logger)));
        }
        else {
            Font_size.set_preferred_font_style(node,logger);

        }
        //button.setFont(Font.font("Verdana", FontPosture.ITALIC, Static_application_properties.get_font_size( logger)));
    }
    protected void set_selected_file_style(Node button){
        logger.log("set_selected_file_style");
        set_hovered_directory_style(button);
    }


    public Color get_stroke_color_of_folder_items() {
        return Color.BLACK;
    }

    /*
    public void set_dragged_over_directory_style(Button button) {
        button.setStyle(Browser_UI.LOOK_AND_FEEL_MENU_BUTTONS_DRAG_OVER);
    }
   */


    public BackgroundFill get_background_fill()
    {
        return background_fill;
    }

    public BackgroundFill get_drag_fill()
    {
        return drag_fill;
    }

    public BackgroundFill get_all_files_fill() {
        return all_files_fill;
    }

    public BackgroundFill get_all_dirs_fill() {
        return all_dirs_fill;
    }

    //**********************************************************
    public double estimate_text_width(String s)
    //**********************************************************
    {
        final Text text = new Text(s);
        Scene scene = new Scene(new Group(text));
        if (style_sheet_url_string != null)
        {
            scene.getStylesheets().clear();
            scene.getStylesheets().add(style_sheet_url_string);
            text.getStyleClass().add(LOOK_AND_FEEL_MENU_BUTTONS);
        }
        text.applyCss();
        double w =  text.getLayoutBounds().getWidth();
        //System.out.println("\n\n\nWIDTH = "+ w);
        return w;
    }
}