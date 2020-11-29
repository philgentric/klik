package klik.look;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import klik.util.Logger;

//**********************************************************
public class Look_and_feel_b_and_w extends Look_and_feel
//**********************************************************
{

    public Look_and_feel_b_and_w(Logger logger_) {
        super("black and white",logger_);
    }

    @Override
    protected String get_broken_icon_file_name()
    {
        return "images/black_and_white/broken.png";
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

    }

    @Override
    public void set_file_style(Button button) {
        button.setAlignment(Pos.BASELINE_LEFT);

    }


}
