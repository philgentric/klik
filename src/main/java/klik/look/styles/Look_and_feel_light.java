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
public class Look_and_feel_light extends Look_and_feel
//**********************************************************
{



    public Look_and_feel_light(Logger logger_)
    {
        super("Light",logger_);
    }

    @Override
    public URL get_CSS_URL() {
        return Klik_application.class.getResource("light/light.css");
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
    public String get_broken_icon_path()
    {
        return "light/broken.png";
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
    public String get_klik_image_path() {
        return "light/klik.jpg";
    }

    @Override
    public String get_default_icon_path()
    {
        return "light/image.png";
    }

    @Override
    public String get_folder_icon_path()
    {
        return "light/folder.png";
    }


    @Override
    public String get_selected_text_color() {return "-fx-text-fill: #FF4040;";}

    @Override
    public Color get_selection_box_color() {return Color.RED;}
}
