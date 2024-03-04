package klik.look.styles;

import javafx.geometry.Insets;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import klik.Klik_application;
import klik.browser.Browser;
import klik.look.Look_and_feel;
import klik.util.Logger;

import java.net.URL;

//**********************************************************
public class Look_and_feel_dark extends Look_and_feel
//**********************************************************
{

    public Look_and_feel_dark(Logger logger_) {
        super("Dark",logger_);
    }


    @Override
    public String get_search_end_icon_path() {
        return "lazy_dark.png";
    }

    public String get_broken_icon_path()
    {
        return "light/broken.png";
    }

    @Override
    public String get_trash_icon_path()
    {
        return "dark/trash.png";
    }

    @Override
    public String get_up_icon_path()
    {
        return "dark/up.png";
    }

    @Override
    public String get_klik_image_path() {
        return "light/klik.jpg";
    }

    @Override
    public String get_default_icon_path()
    {
        return "dark/image.png";
    }

    @Override
    public String get_folder_icon_path()
    {
        return "dark/folder.png";
    }

    @Override
    public String get_selected_text_color() {return "-fx-text-fill: #FF4040;";}

    @Override
    public Color get_selection_box_color() {return Color.WHITE;}

    @Override
    public Color get_background_color() {return Color.BLACK;/*valueOf("#FF4040");*/}

    @Override
    public Color get_foreground_color() {
        return Color.WHITE;
    }

    @Override
    public URL get_CSS_URL() {
        return Klik_application.class.getResource("dark/dark.css");
    }

    @Override
    public Color get_stroke_color_of_folder_items() {
        return Color.WHITE;
    }

    @Override
    public String get_view_icon_path() {
        return "dark/view.png";
    }


    @Override
    public String get_bookmarks_icon_path() {
        return "dark/bookmarks.png";
    }

    @Override
    public String get_preferences_icon_path() {
        return "dark/preferences.png";
    }

}
