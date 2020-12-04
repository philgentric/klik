package klik.look;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import klik.util.Logger;

//**********************************************************
public class Look_and_feel_blue extends Look_and_feel
//**********************************************************
{

    public Look_and_feel_blue(Logger logger_)
    {
        super("blue",logger_);

    }
    @Override
    protected String get_trash_icon_file_name()
    {
        return "/images/blue_trash.png";
    }


    @Override
    protected String get_folder_icon_file_name()
    {
        return "/images/blue_folder_icon.png";
    }
    @Override
    protected String get_up_icon_file_name()
    {
        return "/images/blue_up.png";
    }

    @Override
    public void set_directory_style(Button button) {
        button.setStyle(SHINY_blue);
        button.setAlignment(Pos.BASELINE_LEFT);

    }

    @Override
    public void set_hovered_directory_style(Button button) {
        button.setStyle(HOVERED_SHINY_blue);

    }

    @Override
    public void set_file_style(Button button) {
        button.setStyle(SHINY_blue);
        button.setAlignment(Pos.BASELINE_LEFT);

    }
    @Override
    public void set_hovered_file_style(Button button) {
        button.setStyle(HOVERED_SHINY_blue);

    }



    public static final String SHINY_blue = "-fx-text-fill:white; " +
            //"-fx-effect: dropshadow( three-pass-box , rgba(68,85,90,0.2) , 1, 0.0 , 0 , 1);" +
            "-fx-font-family: \"Helvetica\"; " +
            "-fx-font-size: 16px; " +
            "-fx-font-weight: bold; " +
            "-fx-background-color: #3383ff; "// +
            //"linear-gradient(#000088 0%, #708550 25%, #708570 75%, #708590 100%), " +
            //"linear-gradient(#000088, #70850f), " +
            //"linear-gradient(#0000ff 0%, #0000dd 20%, #0000aa 80%, #000099 100%); " +
            +"-fx-background-insets: 1,1,4,5; " ;
             //       "-fx-background-radius: 9,8,5,3;";
    public static final String HOVERED_SHINY_blue = "-fx-text-fill:black; " +
            "-fx-effect: dropshadow( three-pass-box , rgba(22,22,200,0.2) , 1, 0.0 , 0 , 1);" +
            //"-fx-padding: 15 30 15 30; " +
            "-fx-font-family: \"Helvetica\"; " +
            "-fx-font-size: 16px; " +
            "-fx-font-weight: bold; " +
            "-fx-background-color: white" +
           // "linear-gradient(#680000 0%, #220000 25%, #440000 75%, #660000 100%), " +
            //"linear-gradient(#020b02, #3a3a3a), " +
            //"linear-gradient(#9d0000 0%, #6b0000 20%, #340000 80%, #240000 100%); " +
            "-fx-background-insets: 0,1,4; " +
            "-fx-background-radius: 9,8,5;";


}
