package klik.look;

import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import klik.util.Logger;
import klik.util.Stack_trace_getter;

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

    protected String get_trash_icon_file_name()
    {
        return "images/wooden_trash.png";
    }
    protected String get_broken_icon_file_name()
    {
        return "images/broken.png";
    }
    protected String get_denied_icon_file_name()
    {
        return "images/denied.png";
    }

    protected String get_up_icon_file_name()
    {
        return "images/wooden_up_arrow2.png";
    }


    protected String get_default_image_file_name()
    {
        return "images/wooden_camera.png";
    }



    public static double get_top_button_height()
    {
        return Look_and_feel_manager.get_instance().get_top_height();
    }

    protected double get_top_height() {
        return 30;
    }

    public double get_dir_height() {
        return 30;
    }

    public double get_file_height() {
        return 30;
    }

    //public static double get_dir_button_height() {return Look_and_feel_manager.get_instance().get_dir_height();}


    //public static double get_file_button_height() {return Look_and_feel_manager.get_instance().get_file_height();}


    protected String get_folder_icon_file_name()
    {
        return "images/wooden_folder3.png";
    }


/*

    public void set_button_style(Button button, boolean front)
    {
        logger.log("set_button_style");
        Look_and_feel_manager.get_instance().set_button_look(button,front);
        button.setAlignment(Pos.BASELINE_LEFT);
    }

*/




    public static final String SIMPLE_GREY = "-fx-text-fill:black; " +
            "-fx-effect: dropshadow( three-pass-box , rgba(255,255,255,0.2) , 1, 0.0 , 0 , 1);" +
            //"-fx-padding: 15 30 15 30; " +
            "-fx-background-color: darkgrey, derive(darkgrey, 20%), derive(darkgrey, 25%), derive(darkgrey, 30%); " +
            "-fx-background-insets: 3,5,7,9; " +
            "-fx-background-radius: 9; " +
            "-fx-font-family: \"Helvetica\"; " +
            "-fx-font-size: 16px; " +
            "-fx-font-weight: bold; ";
    //"-fx-border-color: black;" +
    //"-fx-border-width: 3;"+

    public static final String HOVERED_SIMPLE_GREY = "-fx-text-fill:darkgrey; " +
            "-fx-effect: dropshadow( three-pass-box , rgba(255,255,255,0.2) , 1, 0.0 , 0 , 1);" +
            //"-fx-padding: 15 30 15 30; " +
            "-fx-font-family: \"Helvetica\"; " +
            "-fx-font-size: 16px; " +
            "-fx-font-weight: bold; " +
            "-fx-background-color: lightgrey; " +
            "-fx-background-insets: 0; " +
            "-fx-background-radius: 9; ";

    /*
    public void set_button_style_grey(Button button)
    {
        button.setStyle(SIMPLE_GREY);
        button.setOnMouseEntered(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                button.setStyle(HOVERED_SIMPLE_GREY);
                button.toFront();
            }
        });
        button.setOnMouseExited(e -> button.setStyle(SIMPLE_GREY));

        button.setAlignment(Pos.BASELINE_LEFT);

    }*/

    private static final String BASIC_BUTTON_STYLE = "-fx-base: white;"+
            "-fx-font-size: 16px; " +
            "-fx-font-weight: bold; ";
    private static final String BASIC_MOUSE_OVER_BUTTON_STYLE = "-fx-base: lightgrey;"+
            "-fx-font-size: 16px; " +
            "-fx-font-weight: bold; ";


/*    String saved_style;

    public final void set_button_look(Button button, boolean front)
    {
        button.setStyle(BASIC_BUTTON_STYLE);
        saved_style = BASIC_BUTTON_STYLE;
        button.setOnMouseEntered(new EventHandler<MouseEvent>()
        {
            @Override
            public void handle(MouseEvent event)
            {
                logger.log(Stack_trace_getter.get_stack_trace("Look_and_feel: setOnMouseEntered(Button)"));
                saved_style = button.getStyle();
                button.setStyle(BASIC_MOUSE_OVER_BUTTON_STYLE);
                if (front) button.toFront();
            }
        });
        //button.setOnMouseExited(e -> button.setStyle(saved_style));
        button.setOnMouseExited(new EventHandler<MouseEvent>()
        {
            @Override
            public void handle(MouseEvent event)
            {
                logger.log(Stack_trace_getter.get_stack_trace("Look_and_feel: setOnMouseExited(Button)"));
                button.setStyle(saved_style);
                if (front) button.toFront();
            }
        });

    }
*/
    public abstract void set_hovered_directory_style(Button button);
    /*
    {
        logger.log(Stack_trace_getter.get_stack_trace("Look_and_feel: set_hovered_directory_style(Button)"));
        button.setStyle("-fx-base: lightgrey;");

    }
    */

    public abstract void set_hovered_file_style(Button button);
     /*
    {
        button.setStyle("-fx-base: lightgrey;");

    }*/

    public abstract void set_directory_style(Button button);
    /*
    {
        button.setStyle("-fx-base: white;"+
                "-fx-font-size: 16px; " +
                "-fx-font-weight: bold; ");
    }*/

    public abstract void set_file_style(Button button);
    /*
    {

        button.setStyle("-fx-base: white;"+
                "-fx-font-size: 14px; ");
    }
    */



    /*


    public static void set_button_style_round_red(Button button) {

        button.setStyle("-fx-background-color: linear-gradient(#ff5400, #be1d00); " +
                "-fx-background-radius: 30; " +
                "-fx-background-insets: 0; " +
                "-fx-text-fill: white;");

        //button.setStyle(Constants.FX_BACKGROUND_COLOR_new);
        button.setFont(Font.font("Verdana", 15));
    }


    public static void set_button_style_iphone(Button button) {

        button.setStyle("-fx-background-color: #a6b5c9, " +
                "linear-gradient(#303842 0%, #3e5577 20%, #375074 100%), " +
                "linear-gradient(#768aa5 0%, #849cbb 5%, #5877a2 50%, #486a9a 51%, #4a6c9b 100%); " +
                "-fx-background-insets: 0 0 -1 0,0,1; " +
                "-fx-background-radius: 5,5,4; " +
                "-fx-padding: 7 30 7 30; " +
                "-fx-text-fill: #242d35; " +
                "-fx-font-family: \"Helvetica\"; " +
                "-fx-font-size: 16px; " +
                "-fx-text-fill: white;");
    }

    public static void set_button_style_lion(Button button) {

        button.setStyle("-fx-background-color: rgba(0,0,0,0.08), " +
                "linear-gradient(#9a9a9a, #909090)," +
                "linear-gradient(white 0%, #f3f3f3 50%, #ececec 51%, #f2f2f2 100%); " +
                "-fx-background-insets: 0 0 -1 0,0,1; " +
                "-fx-background-radius: 5,5,4; " +
                "-fx-padding: 3 30 3 30; " +
                "-fx-text-fill: #242d35; " +
                "-fx-font-size: 14px;");
    }
*/
}
