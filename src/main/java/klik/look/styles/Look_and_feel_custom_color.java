package klik.look.styles;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import klik.look.Jar_utils;
import klik.look.Look_and_feel;
import klik.look.Look_and_feel_style;
import klik.util.log.Logger;

import java.net.URL;

//**********************************************************
public class Look_and_feel_custom_color extends Look_and_feel
//**********************************************************
{
    public Look_and_feel_style get_look_and_feel_style(){return Look_and_feel_style.custom_color;}

    public Look_and_feel_custom_color(Logger logger) {
        super("Custom color",logger);
    }

    @Override
    public String get_sleeping_man_icon_path() {
        return "lazy.png";
    }

    @Override
    public URL get_CSS_URL() {
        return Jar_utils.get_URL_by_name("custom_color/custom_color.css");
    }

    @Override
    public String get_view_icon_path() {
        return "light/view.png";
    }

       @Override
    public String get_bookmarks_icon_path() {
        return "light/bookmarks.png";
    }

    @Override
    public String get_preferences_icon_path() {
        return "light/preferences.png";
    }

    @Override
    public String get_trash_icon_path()
    {
        return "light/trash.png";
    }
    @Override
    public String get_up_icon_path()
    {
        return "light/up.png";
    }

    @Override
    public String get_klik_icon_path() {
        return "light/camera.png";
    }

    @Override
    public String get_broken_icon_path()
    {
        return "light/broken.png";
    }

    @Override
    public String get_default_icon_path()
    {
        return "light/camera.png";
    }

    @Override
    public String get_music_icon_path()
    {
        return "music.png";
    }


    @Override
    public String get_slingshot_icon_path()
    {
        return "slingshot.png";
    }

    @Override
    public String get_folder_icon_path() {return "light/folder.png";}


    @Override
    public String get_selected_text_color() {return "-fx-text-fill: #FF4040;";}


    @Override
    public Color get_selection_box_color() {return Color.RED;}


    @Override
    public void set_custom_color(Scene scene, Color background, Color hover)
    {

        String css = String.format("""
                .menu_buttons {
                    -fx-text-fill: black;
                    -fx-font: 20 Helvetica;
                    -fx-font-weight: bold;
                    -fx-alignment: top-left;
                    -fx-background-color: %s;
                    -fx-padding: 0 2 0 2;
                    -fx-background-radius: 20px;
                    -fx-border-radius: 20px;
                }
                .menu_buttons:hover {
                    -fx-background-color: %s;
                }
                """, background.toString().replace("0x","#"), hover.toString().replace("0x","#"));

        System.out.println("Setting custom color CSS: " + css);
        Platform.runLater(() -> {
            scene.getStylesheets().clear();
            scene.setUserAgentStylesheet(null);
            scene.getRoot().setStyle(css);
        });
    }
    @Override
    public Color get_background_color() {
        return Color.WHITE;
    }

    @Override
    public Color get_foreground_color() {
        return Color.valueOf("#1f78ed");
    }

}
