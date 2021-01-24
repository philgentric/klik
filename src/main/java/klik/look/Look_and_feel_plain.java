package klik.look;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.paint.Color;
import klik.util.Logger;

//**********************************************************
public class Look_and_feel_plain extends Look_and_feel
//**********************************************************
{

    public Look_and_feel_plain(Logger logger_) {
        super("plain",logger_);
    }

    @Override
    public Color get_background_color() {
        return Color.WHITE;
    }

    @Override
    protected String get_trash_icon_file_name()
    {
        return "/images/alu_trash.png";
    }

    @Override
    protected String get_broken_icon_file_name()
    {
        return "images/broken.png";
    }

    @Override
    protected String get_up_icon_file_name()
    {
        return "/images/black_up.png";
    }


    @Override
    protected String get_folder_icon_file_name()
    {
        return "/images/black_and_white_folder.jpg";
    }



    @Override
    public void set_directory_style(Button button) {
        button.setStyle("-fx-base: white;"+
                "-fx-font-size: 16px; " +
                "-fx-font-weight: bold; ");
        button.setAlignment(Pos.BASELINE_LEFT);
    }
    @Override
    public void set_hovered_directory_style(Button button) {
        button.setStyle("-fx-base: lightgrey;");

    }

    @Override
    public void set_file_style(Button button) {
        button.setStyle("-fx-base: white;"+
                "-fx-font-size: 14px; ");
        button.setAlignment(Pos.BASELINE_LEFT);
    }
    @Override
    public void set_hovered_file_style(Button button) {
        button.setStyle("-fx-base: lightgrey;");

    }


    @Override
    protected String get_default_image_file_name()
    {
        return "/images/black_and_white_camera.jpg";
    }



}
