package klik.look;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.paint.Color;
import klik.util.Logger;

//**********************************************************
public class Look_and_feel_orange extends Look_and_feel
//**********************************************************
{

    private static final String BUTTON_STYLE = "-fx-background-color:" +
            "        linear-gradient(#ffd65b, #e68400)," +
            "        linear-gradient(#ffef84, #f2ba44)," +
            "        linear-gradient(#ffea6a, #efaa22)," +
            "        linear-gradient(#ffe657 0%, #f8c202 50%, #eea10b 100%)," +
            "        linear-gradient(from 0% 0% to 15% 50%, rgba(255,255,255,0.9), rgba(255,255,255,0));" +
            "    -fx-background-radius: 10;" +
            "    -fx-background-insets: 0,1,2,3,0;" +
            "    -fx-text-fill: #654b00;" +
            "    -fx-font-weight: bold;" +
            "    -fx-font-size: 18px;" +
            "    -fx-padding: 2;" +
            "}";
    private static final String BUTTON_STYLE_hover = "-fx-background-color:#eea10b;" +
            "    -fx-background-radius: 10;" +
            "    -fx-text-fill: #000000;" +
            "    -fx-font-weight: bold;" +
            "    -fx-font-size: 18px;" +
            "    -fx-padding: 2;" +
            "}";
    private static final String FILE_STYLE = "-fx-background-color:#f7c634;" +
            "    -fx-background-radius: 10;" +
            "    -fx-text-fill: #000000;" +
            "    -fx-font-weight: bold;" +
            "    -fx-font-size: 18px;" +
            "    -fx-padding: 2;" +
            "}";
    public Look_and_feel_orange(Logger logger_) {
        super("orange",logger_);
    }
    public double get_top_height() { return 40; }
    public double get_dir_height() {
        return 40;
    }
    public double get_file_height() {
        return 30;
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
        button.setAlignment(Pos.BASELINE_LEFT);
        button.setStyle(BUTTON_STYLE_hover);

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
        button.setStyle(FILE_STYLE);

    }


}
