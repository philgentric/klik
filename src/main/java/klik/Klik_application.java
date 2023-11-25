package klik;

import javafx.application.Application;
import javafx.stage.Stage;
import klik.browser.Browser;
import klik.browser.Browser_creation_context;
import klik.browser.My_Stage;
import klik.look.Look_and_feel_manager;
import klik.my_i18n.Language_manager;
import klik.properties.Static_application_properties;
import klik.util.Logger;
import klik.util.System_out_logger;

import java.io.File;
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

        System.out.println("JRE:    "+Runtime.version());
        System.out.println("JavaFX: "+System.getProperty("javafx.runtime.version"));

        //Font_name.print_all_font_families();
/*
        Map<String,String> env = System.getenv();
        for (String key : env.keySet())
        {
            String value = env.get(key);
            System.out.println(key+" = "+value);
        }
*/

        Properties p = System.getProperties();
        for ( String name : p.stringPropertyNames())
        {
            System.out.println(name+" = "+p.getProperty(name));
        }
        setUserAgentStylesheet(STYLESHEET_MODENA);
        Logger logger = new System_out_logger();
        Look_and_feel_manager.init_Look_and_feel(logger);
        Language_manager.init_registered_languages(logger);

        Path path = (new File(System.getProperty(Static_application_properties.USER_HOME))).toPath();
        Browser browser = Browser_creation_context.first(new My_Stage(primary_stage,logger),path,logger);
    }




}
