package klik;

import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import klik.I18N.Local_manager;
import klik.browser.Browser;
import klik.look.Look_and_feel_manager;
import klik.util.Constants;
import klik.util.Logger;
import klik.util.System_out_logger;

import java.io.File;
import java.nio.file.Path;

//**********************************************************
public class Klik_application extends Application
//**********************************************************
{

    //**********************************************************
    public static void main(String[] args)
    {
        launch(args);
    }
    //**********************************************************

    //**********************************************************
    @Override
    public void start(Stage primary_stage) throws Exception
    //**********************************************************
    {
        // there is something in the disruptor lib that gluon does not like ?
        //Logger logger = new Disruptor_logger("klik.log");
        Logger logger = new System_out_logger();

        setUserAgentStylesheet(STYLESHEET_MODENA);

        Look_and_feel_manager.init_Look_and_feel(logger);
        Local_manager.init_Locals(logger);
        Image icon = Look_and_feel_manager.get_default_icon(300);
        if ( icon != null) primary_stage.getIcons().add(icon);

        Path dir = (new File(System.getProperty(Constants.USER_HOME))).toPath();
        Browser.create_browser(null,true, dir,false,logger);

    }




}
