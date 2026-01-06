// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.look.styles;

import javafx.scene.paint.Color;
import javafx.stage.Window;
import klikr.look.Jar_utils;
import klikr.look.Look_and_feel;
import klikr.look.Look_and_feel_style;
import klikr.util.log.Logger;

import java.net.URL;

//**********************************************************
public class Look_and_feel_dark extends Look_and_feel
//**********************************************************
{
    public Look_and_feel_style get_look_and_feel_style(){return Look_and_feel_style.dark;}

    public Look_and_feel_dark(Window owner, Logger logger_) {
        super("Dark",owner,logger_);
    }


    @Override
    public String get_sleeping_man_icon_path() {
        return "icons/lazy.png";
    }

    public String get_broken_icon_path()
    {
        return "icons/dark/broken_image.png";
    }

    @Override
    public String get_trash_icon_path()
    {
        return "icons/dark/trash.png";
    }

    @Override
    public String get_back_icon_path() {return "icons/dark/back.png";}

    @Override
    public String get_up_icon_path() {return "icons/dark/up.png";}

    @Override
    public String get_klik_icon_path() {return "icons/klik.png";}

    @Override
    public String get_default_icon_path()
    {
        return "icons/dark/image.png";
    }

    @Override
    public String get_music_icon_path()
    {
        return "icons/dark/music.png";
    }

    @Override
    public String get_slingshot_icon_path()
    {
        return "icons/slingshot.png";
    }

    @Override
    public String get_folder_icon_path()
    {
        return "icons/dark/folder.png";
    }

    @Override
    public URL get_CSS_URL(Window owner) {
        return Jar_utils.get_URL_by_name("css/dark.css");
    }

    @Override
    public String get_view_icon_path() {
        return "icons/dark/view.png";
    }


    @Override
    public String get_bookmarks_icon_path() {
        return "icons/dark/bookmarks.png";
    }

    @Override
    public String get_preferences_icon_path() {
        return "icons/dark/preferences.png";
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

    /*
    @Override
    public Color get_stroke_color_of_folder_items() {
        return Color.WHITE;
    }
    */


}
