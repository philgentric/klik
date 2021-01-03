package klik.look;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.paint.Color;
import klik.util.Logger;

//**********************************************************
public class Look_and_feel_grey extends Look_and_feel
//**********************************************************
{
/*
    private static final String BUTTON_STYLE = "{-fx-background-color:" +
            "linear-gradient(#686868 0%, #232723 25%, #373837 75%, #757575 100%), " +
            "linear-gradient(#020b02, #3a3a3a), " +
            "linear-gradient(#b9b9b9 0%, #c2c2c2 20%, #afafaf 80%, #c8c8c8 100%), " +
            "linear-gradient(#f5f5f5 0%, #dbdbdb 50%, #cacaca 51%, #d7d7d7 100%); " +
            "-fx-background-insets: 0,1,4,5; " +
            "-fx-background-radius: 9,8,5,4; " +
            "-fx-padding: 2 2 2 2; " +
            "-fx-font-family: \"Helvetica\"; " +
            "-fx-font-size: 18px; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #333333; " +
            "-fx-effect: dropshadow( three-pass-box , rgba(255,255,255,0.2) , 1, 0.0 , 0 , 1);" +
            "}";
*/
    private static final String BUTTON_STYLE = "{-fx-background-color: " +
            "rgba(0,0,0,0.08)," +
            "linear-gradient(#9a9a9a, #909090)," +
            "linear-gradient(white 0%, #f3f3f3 50%, #ececec 51%, #f2f2f2 100%); " +
            "-fx-background-insets: 1,3,6; " +
            "-fx-background-radius: 5,5,4; " +
            "-fx-padding: 3; " +
            "-fx-text-fill: #242d35; " +
            "-fx-font-size: 14px;"+
            "}";

    public Look_and_feel_grey(Logger logger_) {
        super("grey",logger_);
    }
    public double get_top_height() {
        return 30;
    }

    public double get_dir_height() {
        return 30;
    }

    public double get_file_height() {
        return 25;
    }

    @Override
    protected String get_broken_icon_file_name()
    {
        return "images/black_and_white/broken.png";
    }

    @Override
    public Color get_background_color() {
        return Color.WHITE;
    }

    @Override
    protected String get_trash_icon_file_name()
    {
        return "images/black_and_white/trash.png";
    }

    @Override
    protected String get_up_icon_file_name()
    {
        return "images/black_and_white/up.png";
    }

    @Override
    protected String get_default_image_file_name()
    {
        return "images/black_and_white/image.png";
    }

    @Override
    protected String get_folder_icon_file_name()
    {
        return "images/black_and_white/folder.png";
    }

    @Override
    public void set_hovered_directory_style(Button button) {

    }

    @Override
    public void set_hovered_file_style(Button button) {

    }

    @Override
    public void set_directory_style(Button button) {
        button.setAlignment(Pos.BASELINE_LEFT);
        button.setStyle(BUTTON_STYLE);
    }

    @Override
    public void set_file_style(Button button) {
        button.setAlignment(Pos.BASELINE_LEFT);

    }


}
