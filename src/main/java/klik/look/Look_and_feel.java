// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.look;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Window;
import klik.properties.Non_booleans_properties;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

//**********************************************************
public abstract class Look_and_feel
//**********************************************************
{
    /*
    https://docs.oracle.com/javase/8/javafx/api/javafx/scene/doc-files/cssref.html

    https://stackoverflow.com/questions/37689441/javafx-text-control-setting-the-fill-color
     */
    public static final String LOOK_AND_FEEL_GENERAL = "look_and_feel_general";
    public static final String LOOK_AND_FEEL_IMAGE_PLAYLIST = "look_and_feel_image_playlist";
    public static final String LOOK_AND_FEEL_DRAG = "look_and_feel_drag";
    public static final String LOOK_AND_FEEL_MY_BUTTON = "my_button";
    public static final String LOOK_AND_FEEL_MY_BUTTON_with_border = "my_button_with_border";
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
    private final BackgroundFill image_playlist_fill;

    //**********************************************************
    public Look_and_feel(String name, Window owner, Logger logger)
    //**********************************************************
    {
        this.logger = logger;
        this.name = name;
        URL style_sheet_url = get_CSS_URL(owner);
        if (style_sheet_url == null)
        {
            logger.log("❌ style:'" + name + "' Look_and_feel: BAD WARNING cannot load style sheet as style_sheet_url is null");
            style_sheet_url_string = null;
        }
        else
        {
            style_sheet_url_string = style_sheet_url.toExternalForm();
            logger.log("✅ Style:'" + name + "' loaded style sheet=" + style_sheet_url_string);
        }

        Look_and_feel_manager.reset();

        background_fill = getBackgroundFill(LOOK_AND_FEEL_GENERAL);
        all_dirs_fill = getBackgroundFill(LOOK_AND_FEEL_ALL_DIRS);
        all_files_fill = getBackgroundFill(LOOK_AND_FEEL_ALL_FILES);
        drag_fill = getBackgroundFill(LOOK_AND_FEEL_DRAG);
        image_playlist_fill = getBackgroundFill(LOOK_AND_FEEL_IMAGE_PLAYLIST);

    }

    //**********************************************************
    private BackgroundFill getBackgroundFill(String laf)
    //**********************************************************
    {
        // uses a temporary scene to create and capture the CSS style ...
        final BackgroundFill background_fill;
        Pane tmp_pane = new Pane();
        //tmp_pane.getStyleClass().clear();
        tmp_pane.getStyleClass().add(laf);
        Scene tmp_scene = new Scene(tmp_pane);
        tmp_scene.getStylesheets().clear();
        tmp_scene.getStylesheets().add(style_sheet_url_string);
        try {
            tmp_pane.applyCss();
        } catch (Exception e) {
            logger.log("❌ BAD WARNING cannot apply CSS style to pane");
            return new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY);
        }
        if (tmp_pane.getBackground() == null) {
            logger.log("❌ BAD WARNING cannot read BACKGROUND color from CSS file, are you sure the syntax is correct? :" + laf);
        }
        background_fill = tmp_pane.getBackground().getFills().get(0);
        return background_fill;
    }

    public abstract Look_and_feel_style get_look_and_feel_style();

    abstract public URL get_CSS_URL(Window owner);


    abstract public String get_sleeping_man_icon_path();

    abstract public String get_klik_icon_path();

    abstract public String get_trash_icon_path();

    abstract public String get_up_icon_path();

    abstract public String get_view_icon_path();

    abstract public String get_bookmarks_icon_path();

    abstract public String get_preferences_icon_path();

    abstract public String get_broken_icon_path();

    abstract public String get_default_icon_path();

    abstract public String get_music_icon_path();

    abstract public String get_slingshot_icon_path();

    abstract public String get_folder_icon_path();

    public String get_sky_icon_path()
    {
        return "icons/night_sky.png";

    }

    public String get_floor_icon_path()
    {
        return "icons/floor.png";
    }

    abstract public String get_selected_text_color();

    abstract public Color get_selection_box_color();

    abstract public Color get_background_color();

    abstract public Color get_foreground_color();

    //**********************************************************
    public String get_speaker_on_icon_path()
    //**********************************************************
    {
        return "icons/speaker_on.png";
    }
    //**********************************************************
    public String get_speaker_off_icon_path()
    //**********************************************************
    {
        return "icons/speaker_off.png";
    }



    //**********************************************************
    protected String get_dummy_icon_path()
    //**********************************************************
    {
        // dummy is a transparent icon 14 pixel wide by 256
        // it is used as a DEFAULT graphic in button for folders
        return "icons/dummy.png";
    }

    //**********************************************************
    protected String get_denied_icon_path()
    //**********************************************************
    {
        return "icons/denied.png";
    }

    //**********************************************************
    protected String get_unknown_error_icon_path()
    //**********************************************************
    {
        return "icons/unknown-error.png";
    }

    //**********************************************************
    protected String get_not_found_icon_path()
    //**********************************************************
    {
        return "icons/not-found.png";
    }

    //**********************************************************
    public String get_running_film_icon_path()
    //**********************************************************
    {
        return "icons/running_film.gif";
    }

    /*
    //**********************************************************
    public void set_hovered_directory_style(Node node, Window owner)
    //**********************************************************
    {
            }
*/

    //**********************************************************
    private static void set_text_color(Node node, String color)
    //**********************************************************
    {
        // color MUST be formatted as: "-fx-text-fill: #704040;"
        node.setStyle(color);
        if (node instanceof Button button) {
            //button.setBorder(new Border(new BorderStroke(Color.RED, BorderStrokeStyle.SOLID,new CornerRadii(5),new BorderWidths(2))));
            Node g = button.getGraphic();
            if (g instanceof Label label) {
                label.setStyle(color);
            }

        }
    }

    //**********************************************************
    protected void set_directory_style(Node node, Window owner)
    //**********************************************************
    {
        Font_size.apply_global_font_size_to_Node(node, owner, logger);
    }

    //**********************************************************
    protected void set_file_style(Node node, Window owner)
    //**********************************************************
    {
        //logger.log("set_file_style");
        Font_size.apply_global_font_size_to_Node(node, owner, logger);
    }

    //**********************************************************
    protected void set_selected_file_style(Node node, Window owner)
    //**********************************************************
    {
        //logger.log("set_selected_file_style");
        Font_size.apply_global_font_size_to_Node(node, owner, logger);
        set_text_color(node, get_selected_text_color());//"-fx-text-fill: #704040;");
    }

    public BackgroundFill get_background_fill() {
        return background_fill;
    }

    public BackgroundFill get_drag_fill() {
        return drag_fill;
    }

    public BackgroundFill get_all_files_fill() {
        return all_files_fill;
    }

    public BackgroundFill get_all_dirs_fill() {
        return all_dirs_fill;
    }


    public BackgroundFill get_image_playlist_fill() {
        return image_playlist_fill;
    }

    //**********************************************************
    public double estimate_text_width(String s)
    //**********************************************************
    {
        final Text text = new Text(s);
        Scene scene = new Scene(new Group(text));
        if (style_sheet_url_string != null) {
            scene.getStylesheets().clear();
            scene.getStylesheets().add(style_sheet_url_string);
            text.getStyleClass().add(LOOK_AND_FEEL_MY_BUTTON);
        }
        text.applyCss();
        double w = text.getLayoutBounds().getWidth();
        //System.out.println("\n\n\nWIDTH = "+ w);
        return w;
    }


    //**********************************************************
    protected boolean load_font(String file_name)
    //**********************************************************
    {
        InputStream in = Jar_utils.get_jar_InputStream_by_name("/fonts/"+file_name);
        Font font = Font.loadFont(in, 24);
        try {
            in.close();
        } catch (IOException e) {
            logger.log(""+e);
        }
        // the size is a convenience if we would use the font object
        // here, we just load the font in the javafx cache
        if ( font != null)
        {
            logger.log("✅ load_font: "+file_name+" loaded, resulting font name= "+font.getName()+" resulting font family= "+font.getFamily());
            return true;
        }
        else
        {
            logger.log("❌ ERROR: load_font: "+file_name+" not loaded");
            return false;
        }
    }
    //**********************************************************
    protected URL get_CSS_URL2(String css, Window owner)
    //**********************************************************
    {
        Path klik_trash = Non_booleans_properties.get_trash_dir(Path.of("").toAbsolutePath(),owner,logger);
        try {
            Path script_path = klik_trash.resolve("tmp.css");
            Files.write(script_path, css.getBytes());
            Files.setPosixFilePermissions(script_path, PosixFilePermissions.fromString("rwxr-xr-x"));
            return script_path.toUri().toURL();
        }
        catch (MalformedURLException e) {
            logger.log(Stack_trace_getter.get_stack_trace("" + e));
            return null;
        }
        catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace("Error with script file: " + e));
            return null;
        }
    }

    static Double estimated_text_label_height = null;

    //**********************************************************
    public void set_Button_look(
            Button b,
            double width,
            double icon_size,
            Look_and_feel_manager.Icon_type icon_type,
            Window owner, Logger logger)
    //**********************************************************
    {
        Look_and_feel_manager.set_button_look(b, true,owner,logger);
        b.setPrefWidth(width);
        b.setAlignment(Pos.CENTER);
        b.setTextAlignment(TextAlignment.CENTER);
        b.setMnemonicParsing(false);
        if ( icon_type != null)
        {
            FlowPane the_image_pane = new FlowPane();
            the_image_pane.setAlignment(Pos.BOTTOM_CENTER);
            the_image_pane.setMinWidth(icon_size);
            the_image_pane.setMaxWidth(icon_size);
            the_image_pane.setMinHeight(icon_size);
            the_image_pane.setMaxHeight(icon_size);
            b.setGraphic(the_image_pane);
            b.setContentDisplay(ContentDisplay.BOTTOM);
            ImageView the_image_view = new ImageView();
            the_image_pane.getChildren().add(the_image_view);


            String icon_path = Look_and_feel_manager.get_main_window_icon_path(this, icon_type);
            Image icon = Jar_utils.load_jfx_image_from_jar(icon_path, icon_size, owner,logger);

            the_image_view.setImage(icon);
            the_image_view.setPreserveRatio(true);

            if ( estimated_text_label_height == null)
            {
                double font_size = Non_booleans_properties.get_font_size(owner,logger);
                estimated_text_label_height = Look_and_feel.MAGIC_HEIGHT_FACTOR * font_size;
            }
            double h = icon_size + estimated_text_label_height;
            b.setPrefHeight(h);
            b.setMinHeight(h);
            b.setMaxHeight(h);
        }
    }



}
