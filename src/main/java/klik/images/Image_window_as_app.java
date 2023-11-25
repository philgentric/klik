package klik.images;

import javafx.application.Application;
import javafx.stage.Stage;
import klik.browser.Browser;
import klik.browser.Browser_creation_context;
import klik.browser.My_Stage;
import klik.look.Look_and_feel_manager;
import klik.my_i18n.Language_manager;
import klik.util.Logger;
import klik.util.System_out_logger;

import java.nio.file.Path;

public class Image_window_as_app extends Application
{
    public static void main(String[] args)
    {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception
    {
        Logger logger = new System_out_logger();
        Browser browser = Browser_creation_context.first(new My_Stage(stage, logger), Path.of("."), logger);
        Look_and_feel_manager.init_Look_and_feel(logger);
        Language_manager.init_registered_languages(logger);

        Image_window image_stage = Image_window.get_Image_stage(null, Path.of("visit.png"), logger);
    }
}

