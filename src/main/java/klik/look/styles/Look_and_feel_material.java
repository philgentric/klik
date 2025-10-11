package klik.look.styles;

import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Window;
import klik.System_info;
import klik.look.Font_size;
import klik.look.Jar_utils;
import klik.look.Look_and_feel;
import klik.look.Look_and_feel_style;
import klik.properties.Non_booleans_properties;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

//**********************************************************
public class Look_and_feel_material extends Look_and_feel
//**********************************************************
{
    public Look_and_feel_style get_look_and_feel_style(){return Look_and_feel_style.material;}

    //**********************************************************
    public Look_and_feel_material(Window owner, Logger logger)
    //**********************************************************
    {
        super("Material",owner,logger);
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
        return Jar_utils.get_URL_by_name("css/material.css");
/*
        //logger.log(Stack_trace_getter.get_stack_trace("get_CSS_URL"));

        int font_size = (int)Non_booleans_properties.get_font_size(owner,logger);

        Font_size.init(logger);

        System_info.print_all_font_families();

        //Color background = Non_booleans_properties.get_custom_color(owner);
        //Color hover =  background.darker();
        //String background_s = background.toString().replace("0x","#");
        //String hover_s = hover.toString().replace("0x","#");

        // NOT THIS: the javafx doc https://docs.oracle.com/javafx/2/api/javafx/scene/doc-files/cssref.html
        // says in the 'limitations' section that javafx CSS does NOT support
        // coma separated font list
        //-fx-font-family: %s, Arial, sans-serif;

        // special case: we create a data-URL
        String css = String.format("""

.root {
-fx-text-fill: #202124;
-fx-font-family: %s;
-fx-font-size: %spx;
-fx-font-weight: 500;
}

.look_and_feel_general {
-fx-background-color: #F5F5F5;
-fx-padding: 16px;
-fx-background-radius: 16px;
-fx-effect: dropshadow(gaussian, #B0BEC5, 8, 0.2, 0, 2);
}
                
.my_button {
-fx-background-color: #90CAF9;
-fx-background-radius: 24px;
-fx-border-radius: 24px;
-fx-alignment: CENTER_LEFT;
-fx-padding: 0 24 0 24;
-fx-effect: dropshadow(gaussian, #B0BEC5, 4, 0.1, 0, 1);
-fx-border-color: #E0E0E0;
-fx-border-width: 1px;
}

.my_button:hover {
-fx-background-color: #1976D2; // Material V3 blue highlight
-fx-effect: dropshadow(gaussian, #1976D2, 8, 0.2, 0, 2);
-fx-background-radius: 24px;                
}

.my_context_menu {
-fx-background-radius: 12px;
-fx-effect: dropshadow(gaussian, #B0BEC5, 8, 0.2, 0, 2);
}
                   
.my_context_menu, .combo-box-popup {
-fx-background-radius: 12px;
-fx-effect: dropshadow(gaussian, #B0BEC5, 8, 0.2, 0, 2);
-fx-padding: 8px;
-fx-border-color: #E0E0E0;
-fx-border-radius: 12px;
-fx-border-width: 1px;
}
                               
.menu-item, .list-cell {
-fx-background-color: transparent;
-fx-padding: 8 16 8 16;
}


.menu-item:hover, .list-cell:hover {
-fx-background-color: #E3F2FD;
}

.menu-item:focused, .list-cell:focused {
-fx-background-color: #90CAF9;
}                               
                               
.image-window {
//-fx-background-color: #FFFFFF;
-fx-background-radius: 16px;
-fx-effect: dropshadow(gaussian, #B0BEC5, 8, 0.2, 0, 2);
}
    
.check-box .box {
//-fx-background-color: #FFFFFF;
-fx-border-color: #B0BEC5;
-fx-border-radius: 6px;
-fx-mark-color: #90CAF9;
-fx-effect: dropshadow(gaussian, #B0BEC5, 2, 0.1, 0, 1);                
}
                
.check-box:selected .mark {
//-fx-background-color: #FFFFFF;
 -fx-mark-color: #D32F2F;
}
                
.check-box:selected .box {
-fx-background-color: #90CAF9;
}                

.look_and_feel_drag {
-fx-background-color: #88ffff;
}
                
.look_and_feel_all_files {
-fx-background-color: #1f78ed;
}
                                
.look_and_feel_all_dirs {
-fx-background-color: #aaffff;
}                
                
.look_and_feel_image_playlist {
-fx-background-color: pink;
}
                                
my_dialog{
-fx-background-color: white;
}

.my_dialog > *.button-bar > *.container{
-fx-background-color: white;
}

.my_dialog > *.label.content{

-fx-font-weight: bold;
}

.my_dialog:header *.header-panel{
-fx-background-color: white;
}

.my_dialog:header *.header-panel *.label{

-fx-font-style: italic;
-fx-fill: #404040; // dark grey
}
                    """
                ,Font_size.font_family,font_size
        );

        //logger.log("CSS\n"+css);
        return get_CSS_URL2(css,owner);

 */
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
