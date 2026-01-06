// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.look.styles;

import javafx.application.Application;
import javafx.scene.paint.Color;
import javafx.stage.Window;
import klikr.look.Jar_utils;
import klikr.look.Look_and_feel;
import klikr.look.Look_and_feel_style;
import klikr.util.log.Logger;

import java.net.URL;

//**********************************************************
public class Look_and_feel_modena extends Look_and_feel
//**********************************************************
{
    public Look_and_feel_style get_look_and_feel_style(){return Look_and_feel_style.modena;}

    public Look_and_feel_modena(Window owner, Logger logger)
    {
        super("Modena (default JavaFX look and feel)",owner,logger);
    }

    @Override
    public String get_sleeping_man_icon_path() {
        return "icons/lazy.png";
    }

    //**********************************************************
    @Override
    public URL get_CSS_URL(Window owner)
    //**********************************************************
    {
        Application.setUserAgentStylesheet(Application.STYLESHEET_MODENA);
        return Jar_utils.get_URL_by_name("css/modena.css");
    }

    @Override
    public String get_view_icon_path() {
        return "icons/light/view.png";
    }

       @Override
    public String get_bookmarks_icon_path() {
        return "icons/light/bookmarks.png";
    }

    @Override
    public String get_preferences_icon_path() {
        return "icons/light/preferences.png";
    }

    @Override
    public String get_trash_icon_path()
    {
        return "icons/light/trash.png";
    }
    @Override
    public String get_up_icon_path()
    {
        return "icons/light/up.png";
    }


    @Override
    public String get_back_icon_path() {return "icons/light/back.png";}

    @Override
    public String get_klik_icon_path() {
        return "icons/light/camera.png";
    }

    @Override
    public String get_broken_icon_path()
    {
        return "icons/light/broken_image.png";
    }

    @Override
    public String get_default_icon_path()
    {
        return "icons/light/camera.png";
    }

    @Override
    public String get_music_icon_path()
    {
        return "icons/light/music.png";
    }


    @Override
    public String get_slingshot_icon_path()
    {
        return "icons/slingshot.png";
    }

    @Override
    public String get_folder_icon_path() {return "icons/light/folder.png";}


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
        return Color.valueOf("#b8d4fe");
    }

}
