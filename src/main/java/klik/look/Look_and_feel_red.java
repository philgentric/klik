package klik.look;

import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import klik.util.Logger;

//**********************************************************
public class Look_and_feel_red extends Look_and_feel
//**********************************************************
{

    public Look_and_feel_red(Logger logger_)
    {
        super("red",logger_);

    }
    @Override
    protected String get_trash_icon_file_name()
    {
        return "/images/wooden_trash.png";
    }

    @Override
    protected String get_broken_icon_file_name()
    {
        return "images/broken.png";
    }

    @Override
    protected String get_up_icon_file_name()    {
        return "images/wooden_up_arrow2.png";
    }

    @Override
    protected String get_default_image_file_name()    {
        return "images/wooden_camera.png";
    }



    @Override
    protected String get_folder_icon_file_name()
    {
        return "/images/shiny_red_folder.jpg";
    }

    @Override
    public void set_hovered_directory_style(Button button) {
        button.setStyle(HOVERED_SHINY_red);

    }

    @Override
    public void set_hovered_file_style(Button button) {
        button.setStyle(HOVERED_SHINY_red);

    }

    @Override
    public void set_directory_style(Button button) {
        button.setStyle(SHINY_red);
        button.setAlignment(Pos.BASELINE_LEFT);

    }

    @Override
    public void set_file_style(Button button) {
        button.setStyle(SHINY_red);
        button.setAlignment(Pos.BASELINE_LEFT);

    }

    public static final String SHINY_red = "-fx-text-fill:white; " +
            "-fx-effect: dropshadow( three-pass-box , rgba(255,22,22,0.2) , 1, 0.0 , 0 , 1);" +
            //"-fx-padding: 15 30 15 30; " +
            "-fx-font-family: \"Helvetica\"; " +
            "-fx-font-size: 16px; " +
            "-fx-font-weight: bold; " +
            "-fx-background-color: " +
            "linear-gradient(#680000 0%, #230000 25%, #370000 75%, #750000 100%), " +
            "linear-gradient(#020b02, #3a3a3a), " +
            "linear-gradient(#9d9e9d 0%, #6b0000 20%, #340000 80%, #240000 100%); " +
            "-fx-background-insets: 0,1,4; " +
            "-fx-background-radius: 9,8,5;";
    public static final String HOVERED_SHINY_red = "-fx-text-fill:darkgrey; " +
            "-fx-effect: dropshadow( three-pass-box , rgba(255,22,22,0.2) , 1, 0.0 , 0 , 1);" +
            //"-fx-padding: 15 30 15 30; " +
            "-fx-font-family: \"Helvetica\"; " +
            "-fx-font-size: 16px; " +
            "-fx-font-weight: bold; " +
            "-fx-background-color: " +
            "linear-gradient(#680000 0%, #220000 25%, #440000 75%, #660000 100%), " +
            "linear-gradient(#020b02, #3a3a3a), " +
            "linear-gradient(#9d0000 0%, #6b0000 20%, #340000 80%, #240000 100%); " +
            "-fx-background-insets: 0,1,4; " +
            "-fx-background-radius: 9,8,5;";

    /*
    @Override
    public void set_button_look(Button button, boolean front) {
        button.setStyle(SHINY_red);
        button.setOnMouseEntered(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                button.setStyle(HOVERED_SHINY_red);
                if (front) button.toFront();
            }
        });
        button.setOnMouseExited(e -> button.setStyle(SHINY_red));
    }
    */

}
