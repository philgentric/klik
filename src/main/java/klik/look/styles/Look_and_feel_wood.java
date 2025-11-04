// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.look.styles;

import javafx.scene.paint.Color;
import javafx.stage.Window;
import klik.look.Jar_utils;
import klik.look.Look_and_feel;
import klik.look.Look_and_feel_style;
import klik.util.log.Logger;

import java.net.URL;

//**********************************************************
public class Look_and_feel_wood extends Look_and_feel
//**********************************************************
{
    public Look_and_feel_style get_look_and_feel_style(){return Look_and_feel_style.wood;}

    public Look_and_feel_wood(Window owner, Logger logger) {
        super("Wood",owner,logger);
    }

    @Override
    public String get_sleeping_man_icon_path() {
        return "icons/lazy.png";
    }

    @Override
    public URL get_CSS_URL(Window owner) {
        return Jar_utils.get_URL_by_name("css/wood.css");
    }

    @Override
    public String get_view_icon_path() {
        return "icons/wood/view.png";
    }

       @Override
    public String get_bookmarks_icon_path() {
        return "icons/wood/bookmarks.png";
    }

    @Override
    public String get_preferences_icon_path() {
        return "icons/wood/preferences.png";
    }

    @Override
    public String get_trash_icon_path()
    {
        return "icons/wood/trash.png";
    }

    @Override
    public String get_up_icon_path()
    {
        return "icons/wood/up.png";
    }

    @Override
    public String get_klik_icon_path() {
        return "icons/wood/camera.png";
    }

    @Override
    public String get_broken_icon_path()
    {
        return "icons/light/broken_image.png";
    }

    @Override
    public String get_default_icon_path()
    {
        return "icons/wood/camera.png";
    }

    @Override
    public String get_music_icon_path()
    {
        return "icons/wood/music.png";
    }

    @Override
    public String get_slingshot_icon_path()
    {
        return "icons/slingshot.png";
    }

    @Override
    public String get_folder_icon_path() {return "icons/wood/folder.png";}

    @Override
    public String get_selected_text_color() {return "-fx-text-fill: #FF4040;";}

    @Override
    public Color get_background_color() {
        return Color.valueOf("#3B3B3B");
    }

    @Override
    public Color get_foreground_color() {
        return Color.WHITE;
    }

    @Override
    public Color get_selection_box_color() {return Color.RED;}

/*
    public static String SHINY_DARK(Logger logger) {
        return "-fx-text-fill:white; " +
                "-fx-effect: dropshadow( three-pass-box , rgba(255,255,255,0.2) , 1, 0.0 , 0 , 1);" +
                //"-fx-padding: 15 30 15 30; " +
                "-fx-font-family: \"Helvetica\"; " +
                "-fx-font-weight: bold; " +
                "-fx-background-color: " +
                "linear-gradient(#686868 0%, #232723 25%, #373837 75%, #757575 100%), " +
                "linear-gradient(#020b02, #3a3a3a), " +
                "linear-gradient(#9d9e9d 0%, #6b6a6b 20%, #343534 80%, #242424 100%); " +
                "-fx-background-insets: 0,1,4,4; " +
                "-fx-background-radius: 9,8,5,3;" +
                Font_size.get_font_size(logger);
    }


    public static String HOVERED_SHINY_DARK(Logger logger)
    {
        return "-fx-text-fill:black; " +
                //"-fx-effect: dropshadow( three-pass-box , rgba(255,255,255,0.2) , 1, 0.0 , 0 , 1);" +
                //"-fx-padding: 15 30 15 30; " +
                "-fx-font-family: \"Helvetica\"; " +
                "-fx-font-weight: bold; " +
                "-fx-background-color: lightgrey;" +
                //"linear-gradient(#686868 0%, #232723 25%, #373837 75%, #757575 100%), " +
                //"linear-gradient(#020b02, #3a3a3a), " +
                //"linear-gradient(#9d9e9d 0%, #6b6a6b 20%, #343534 80%, #242424 100%); " +
                "-fx-background-insets: 0,1,4; " +
                "-fx-background-radius: 9,8,5;"+
                Font_size.get_font_size(logger);
    }

*/

}
