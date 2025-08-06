package klik.look.styles;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Window;
import klik.look.Jar_utils;
import klik.look.Look_and_feel;
import klik.look.Look_and_feel_style;
import klik.properties.Non_booleans_properties;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Base64;

//**********************************************************
public class Look_and_feel_custom_color extends Look_and_feel
//**********************************************************
{
    public Look_and_feel_style get_look_and_feel_style(){return Look_and_feel_style.custom_color;}

    public Look_and_feel_custom_color(Window owner, Logger logger)
    {
        super("Custom color",owner,logger);
    }

    @Override
    public String get_sleeping_man_icon_path() {
        return "lazy.png";
    }

    //**********************************************************
    @Override
    public URL get_CSS_URL(Window owner)
    //**********************************************************
    {
        Color background = Non_booleans_properties.get_custom_color(owner);

        Color hover =  background.darker();

        String background_s = background.toString().replace("0x","#");
        String hover_s = hover.toString().replace("0x","#");

        // special case: we create a data-URL
        String css = String.format("""
                .look_and_feel_general {
                    -fx-text-fill: #1f78ed	; // color of text
                    -fx-font:20 Helvetica; //papyrus;
                    -fx-font-weight: bold;
                    -fx-alignment: top-left;
                    -fx-background-color: white;
                    //-fx-padding: -1 -1 -1 -1;
                }
                
                .menu_buttons {
                    -fx-text-fill: black; // color of text
                    -fx-font:20 Helvetica; //papyrus;
                    -fx-font-weight: bold;
                    -fx-alignment: top-left;
                    -fx-background-color: %s;
                    -fx-padding: 0 2 0 2; // top right bottom left
                    -fx-background-radius: 20px;
                    -fx-border-radius: 20px;
                }
                .menu_buttons:hover {
                  -fx-background-color: %s;
                }
                
                
                .context-menu {
                    -fx-text-fill: #1f78ed	; // color of text
                    //-fx-alignment: top-left;
                    -fx-background-color: white;
                }
                
                .image-window {
                    -fx-text-fill: #1f78ed	; // color of text
                    -fx-font:20 Helvetica; //papyrus;
                    -fx-font-weight: bold;
                    //-fx-alignment: center;
                    -fx-background-color: white;
                    //-fx-padding: -1 -1 -1 -1;
                }
                
                
                
                
                .check-box .box {
                
                -fx-background-color: white;
                -fx-border-color:grey;
                -fx-border-radius:3px;
                -fx-mark-color:blue;
                
                }
                
                .check-box:selected .mark {
                
                -fx-background-color: white;
                -fx-mark-color:red;
                }
                
                .check-box:selected .box {
                -fx-background-color: blue;
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
                    -fx-font-size: 14px;
                    -fx-font-weight: bold;
                }
                .my_dialog:header *.header-panel{
                    -fx-background-color: white;
                }
                .my_dialog:header *.header-panel *.label{
                    -fx-font-size: 18px;
                    -fx-font-style: italic;
                    -fx-fill: #404040; // dark grey
                }
                    """,background_s,hover_s);
        // Encode the content using Base64
        //String encoded_css = Base64.getEncoder().encodeToString(css.getBytes());

        // Create a tmp file for the CSS, in klik_trash folder
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


        //return Jar_utils.get_URL_by_name("custom_color/custom_color.css");
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
    public Color get_background_color() {
        return Color.WHITE;
    }

    @Override
    public Color get_foreground_color() {
        return Color.valueOf("#b8d4fe");
    }

}
