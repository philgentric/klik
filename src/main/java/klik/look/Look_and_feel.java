package klik.look;

import javafx.css.PseudoClass;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.geometry.Insets;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.Text;
import klik.properties.Static_application_properties;
import klik.util.Logger;
import klik.Klik_application;

import java.net.URL;
import java.io.InputStream;

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
    //private static final double BORDER_WIDTH = 2;
    //private static final double BORDER_RADII = 7;
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
        URL style_sheet_url2 = get_CSS_URL();
        //System.out.println("style:"+name_+" CSS URL="+style_sheet_url2);
        if (style_sheet_url2 == null)
        {
            logger.log("style:"+name_+"Look_and_feel: BAD WARNING cannot load style sheet as style_sheet_url2 is null");
            style_sheet_url_string = null;
        }
        else
        {
            style_sheet_url_string = style_sheet_url2.toExternalForm();
            //logger.log("loaded style sheet=" + style_sheet_url_string);
        }

        name = name_;
        Look_and_feel_manager.reset();

        background_fill = getBackgroundFill(LOOK_AND_FEEL_GENERAL);
        all_dirs_fill = getBackgroundFill(LOOK_AND_FEEL_ALL_DIRS);
        all_files_fill = getBackgroundFill(LOOK_AND_FEEL_ALL_FILES);
        drag_fill = getBackgroundFill(LOOK_AND_FEEL_DRAG);

    }

    //**********************************************************
    private BackgroundFill getBackgroundFill(String laf)
    //**********************************************************
    {
        // uses a temporary scene to create and capture the CSS style ...
        final BackgroundFill background_fill;
        Pane tmp_pane = new Pane();
        tmp_pane.getStyleClass().add(laf);
        Scene tmp_scene = new Scene(tmp_pane);
        tmp_scene.getStylesheets().add(style_sheet_url_string);
        try
        {
            tmp_pane.applyCss();
        }
        catch ( Exception e)
        {
            logger.log("BAD WARNING cannot apply CSS style to pane");
            return new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY);

        }
        if ( tmp_pane.getBackground() == null)
        {
            logger.log("BADBADBAD cannot read BACKGROUND color from CSS file, are you sure the syntax is correct? :"+laf);
        }
        background_fill = tmp_pane.getBackground().getFills().get(0);
        return background_fill;
    }

    abstract public URL get_CSS_URL();

    //**********************************************************
    public static URL get_URL_by_name(String name)
    //**********************************************************
    {
        // this scheme works with Jbang
        ClassLoader class_loader = Thread.currentThread().getContextClassLoader();
        //System.out.println("get_URL_by_name trying with class_loader : "+class_loader+" ...");
        URL url = class_loader.getResource(name);
        if (url != null)
        {
            //System.out.println("... worked!");
            return url;
        }
        // this scheme works with Gradle
        return Klik_application.class.getResource(name);
    }

    //**********************************************************
    public static InputStream get_InputStream_by_name(String name)
    //**********************************************************
    {
        // this scheme works with Jbang
        ClassLoader class_loader = Thread.currentThread().getContextClassLoader();
        System.out.println("get_InputStream_by_name trying with class_loader : "+class_loader+ " ...");
        InputStream s = class_loader.getResourceAsStream(name);
        if (s != null)
        {
            System.out.println("... worked");
            return s;
        }
        System.out.println("Thread.currentThread().getContextClassLoader().getResourceAsStream DID NOT work");
        // this scheme works with Gradle
        return Klik_application.class.getResourceAsStream(name);
    }




    abstract public String get_search_end_icon_path();
    abstract public String get_klik_image_path();
    abstract public String get_trash_icon_path();
    abstract public String get_up_icon_path();
    abstract public String get_view_icon_path();
    abstract public String get_bookmarks_icon_path();
    abstract public String get_preferences_icon_path();
    abstract public String get_broken_icon_path();
    abstract public String get_default_icon_path();
    public String get_speaker_icon_path(){return "speaker.png";}
    abstract public String get_folder_icon_path();
    abstract public String get_selected_text_color();
    abstract public Color get_selection_box_color();

    abstract public Color get_background_color();
    abstract public Color get_foreground_color();


    //**********************************************************
    protected String get_dummy_icon_path()
    //**********************************************************
    {
        // dummy is a transparent icon 14 pixel wide by 256
        // it is used as a DEFAULT graphic in button for folders
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

    //public static final PseudoClass pseudo_css_class_for_selection = PseudoClass.getPseudoClass("selected_item");
    //**********************************************************
    public void set_hovered_directory_style(Node node)
    //**********************************************************
    {
        //System.out.println("Look_and_feel::set_hovered_directory_style");
        Font_size.apply_font_size(node, logger);

        set_text_color(node,get_selected_text_color());//"-fx-text-fill: #704040;");
        //PseudoClass pseudo_css_class_for_selection = PseudoClass.getPseudoClass("selected_item");
        //button.pseudoClassStateChanged(pseudo_css_class_for_selection,true);
    }


    //**********************************************************
    private static void set_text_color(Node node, String color)
    //**********************************************************
    {
        // color MUST be formatted as: "-fx-text-fill: #704040;"
        node.setStyle(color);
        if ( node instanceof Button)
        {
            Button button = (Button) node;
            //button.setBorder(new Border(new BorderStroke(Color.RED, BorderStrokeStyle.SOLID,new CornerRadii(5),new BorderWidths(2))));
            Node g = button.getGraphic();
            if ( g instanceof Label)
            {
                Label t = (Label) g;
                t.setStyle(color);
            }

        }
    }

    //**********************************************************
    protected void set_directory_style(Node node)
    //**********************************************************
    {
        Font_size.apply_font_size(node,logger);
    }

    //**********************************************************
    protected void set_file_style(Node node)
    //**********************************************************
    {
        //logger.log("set_file_style");
        //Font_size.set_preferred_font_size(node,logger);
        Font_size.apply_font_size(node,logger);
/*
        if (node instanceof Button button)
        {
            button.setFont(Font.font("Monaco", FontPosture.ITALIC, Static_application_properties.get_font_size( logger)));
            set_text_color(node,"-fx-text-fill: #404040;");
        }
        else
        {
            Font_size.set_preferred_font_size(node,logger);
        }
        //button.setFont(Font.font("Verdana", FontPosture.ITALIC, Static_application_properties.get_font_size( logger)));

 */
    }
    //**********************************************************
    protected void set_selected_file_style(Node button)
    //**********************************************************
    {
        //logger.log("set_selected_file_style");
        set_hovered_directory_style(button);
    }


    //**********************************************************
    public Color get_stroke_color_of_folder_items()
    //**********************************************************
    {
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

    public String get_search_icon_path() {
        return "running_man.gif";
    }

}

