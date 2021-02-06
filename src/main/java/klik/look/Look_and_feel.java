package klik.look;

import javafx.scene.control.Button;
import javafx.scene.paint.Color;
import klik.util.Logger;

//**********************************************************
public abstract class Look_and_feel
//**********************************************************
{
    public static final boolean dbg = false;
    public final String name;
    public final Logger logger;

    public Look_and_feel(String name_, Logger logger_)
    {
        name = name_;
        logger = logger_;
        Look_and_feel_manager.reset();
    }

    public abstract Color get_background_color();
    protected abstract String get_trash_icon_file_name();
    protected abstract String get_broken_icon_file_name();
    protected String get_denied_icon_file_name()
    {
        return "denied.png";
    }


    protected abstract String get_up_icon_file_name();

    protected abstract String get_default_image_file_name();

    public double get_top_height() {
        return 30;
    }
    public double get_dir_height() {
        return 30;
    }
    public double get_file_height() {
        return 30;
    }

    protected String get_folder_icon_file_name()
    {
        return "wooden_folder3.png";
    }
    public abstract void set_hovered_directory_style(Button button);
    public abstract void set_hovered_file_style(Button button);
    public abstract void set_directory_style(Button button);
    public abstract void set_file_style(Button button);
}
