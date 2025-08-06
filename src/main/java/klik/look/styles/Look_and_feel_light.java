package klik.look.styles;

import javafx.scene.paint.Color;
import javafx.stage.Window;
import klik.look.Jar_utils;
import klik.look.Look_and_feel;
import klik.look.Look_and_feel_style;
import klik.util.log.Logger;

import java.net.URL;

//**********************************************************
public class Look_and_feel_light extends Look_and_feel
//**********************************************************
{
    public Look_and_feel_style get_look_and_feel_style(){return Look_and_feel_style.light;}

    public Look_and_feel_light(Window owner, Logger logger_)
    {
        super("Light",owner,logger_);
    }

    @Override
    public String get_sleeping_man_icon_path() {
        return "lazy.png";
    }

    @Override
    public URL get_CSS_URL(Window owner) {
        return Jar_utils.get_URL_by_name("light/light.css");
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
        return "broken.png";
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
    public String get_klik_icon_path() {return "klik.png";}

    @Override
    public String get_default_icon_path() {return "light/image.png";}

    @Override
    public String get_music_icon_path() {return "music.png";}


    @Override
    public String get_slingshot_icon_path()
    {
        return "slingshot.png";
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


    @Override
    public Color get_background_color() {
        return Color.WHITE;
    }

    @Override
    public Color get_foreground_color() {
        return Color.BLACK;
    }

}
