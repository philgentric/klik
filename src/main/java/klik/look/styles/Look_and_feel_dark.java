package klik.look.styles;

import javafx.scene.paint.Color;
import klik.look.Jar_utils;
import klik.look.Look_and_feel;
import klik.util.log.Logger;

import java.net.URL;

//**********************************************************
public class Look_and_feel_dark extends Look_and_feel
//**********************************************************
{

    public Look_and_feel_dark(Logger logger_) {
        super("Dark",logger_);
    }


    @Override
    public String get_sleeping_man_icon_path() {
        return "dark/lazy_dark.png";
    }

    public String get_broken_icon_path()
    {
        return "broken.png";
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
    public String get_klik_icon_path() {return "klik.png";}

    @Override
    public String get_default_icon_path()
    {
        return "dark/image.png";
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
        return Jar_utils.get_URL_by_name("dark/dark.css");
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
