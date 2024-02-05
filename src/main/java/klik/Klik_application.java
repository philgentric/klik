package klik;

import javafx.application.Application;
import javafx.stage.Stage;
import klik.actor.Aborter;
import klik.browser.Browser;
import klik.browser.Browser_creation_context;
import klik.browser.My_Stage;
import klik.change.history.History_auto_clean;
import klik.look.Look_and_feel_manager;
import klik.look.my_i18n.Language_manager;
import klik.properties.Static_application_properties;
import klik.util.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Properties;

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

        Print_system_info.print();

        //setUserAgentStylesheet(STYLESHEET_MODENA);

        Logger logger = new System_out_logger();


        Language_manager.init_registered_languages(logger);

        new Monitor(new Aborter("klik",logger),logger).start();

        Exceptions_in_threads_catcher.set_exceptions_in_threads_catcher(logger);
        Look_and_feel_manager.init_Look_and_feel(logger);

        Path path = (new File(System.getProperty(Static_application_properties.USER_HOME))).toPath();
        Browser_creation_context.first(new My_Stage(primary_stage,logger),path,logger);
    }




}
