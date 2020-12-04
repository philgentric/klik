package klik.look;

import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import klik.util.Logger;

//**********************************************************
public class Look_and_feel_shiny_black extends Look_and_feel
//**********************************************************
{

    public Look_and_feel_shiny_black(Logger logger_) {
        super("shiny black", logger_);
    }

    @Override
    protected double get_top_height() {
        return 50;
    }

    @Override
    public double get_dir_height() { return 40; }

    @Override
    public double get_file_height() {
        return 30;
    }

    @Override
    public void set_hovered_directory_style(Button button) {
        button.setStyle(HOVERED_SHINY_DARK);
        //button.toFront();
    }

    @Override
    public void set_hovered_file_style(Button button) {
        button.setStyle(HOVERED_SHINY_DARK);
        button.toFront();
    }

    @Override
    public void set_directory_style(Button button) {
        button.setStyle(SHINY_DARK);
        button.setAlignment(Pos.BASELINE_LEFT);

    }

    @Override
    public void set_file_style(Button button) {
        button.setStyle(SHINY_DARK);
        button.setAlignment(Pos.BASELINE_LEFT);

    }

    public static final String SHINY_DARK = "-fx-text-fill:white; " +
            "-fx-effect: dropshadow( three-pass-box , rgba(255,255,255,0.2) , 1, 0.0 , 0 , 1);" +
            //"-fx-padding: 15 30 15 30; " +
            "-fx-font-family: \"Helvetica\"; " +
            "-fx-font-size: 16px; " +
            "-fx-font-weight: bold; " +
            "-fx-background-color: " +
            "linear-gradient(#686868 0%, #232723 25%, #373837 75%, #757575 100%), " +
            "linear-gradient(#020b02, #3a3a3a), " +
            "linear-gradient(#9d9e9d 0%, #6b6a6b 20%, #343534 80%, #242424 100%); " +
            "-fx-background-insets: 0,1,4,4; " +
            "-fx-background-radius: 9,8,5,3;";
    public static final String HOVERED_SHINY_DARK = "-fx-text-fill:darkgrey; " +
            //"-fx-effect: dropshadow( three-pass-box , rgba(255,255,255,0.2) , 1, 0.0 , 0 , 1);" +
            //"-fx-padding: 15 30 15 30; " +
            "-fx-font-family: \"Helvetica\"; " +
            "-fx-font-size: 16px; " +
            "-fx-font-weight: bold; " +
            "-fx-background-color: white;" +
            //"linear-gradient(#686868 0%, #232723 25%, #373837 75%, #757575 100%), " +
            //"linear-gradient(#020b02, #3a3a3a), " +
            //"linear-gradient(#9d9e9d 0%, #6b6a6b 20%, #343534 80%, #242424 100%); " +
            "-fx-background-insets: 0,1,4; " +
            "-fx-background-radius: 9,8,5;";

}
