package klik.images;

import javafx.application.Application;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import klik.browser.Browser;
import klik.browser.Browser_creation_context;
import klik.look.Look_and_feel_manager;
import klik.look.my_i18n.Language_manager;
import klik.util.log.Exceptions_in_threads_catcher;
import klik.util.log.Logger;
import klik.util.log.System_out_logger;

import java.nio.file.Path;
import java.util.List;

//**********************************************************
public class Image_viewer_application extends Application
//**********************************************************
{
    public static void main(String[] args) {launch(args);}

    //**********************************************************
    @Override
    public void start(Stage stage) throws Exception
    //**********************************************************
    {
        Logger logger = new System_out_logger("Image_viewer");
        Exceptions_in_threads_catcher.set_exceptions_in_threads_catcher(logger);

        Look_and_feel_manager.init_Look_and_feel(logger);
        Language_manager.init_registered_languages(logger);

        Parameters params = getParameters();
        List<String> list = params.getRaw();
        Path path;
        if ( list.isEmpty())
        {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Open Image File");
            path = fileChooser.showOpenDialog(stage).toPath();
        }
        else
        {
            path = Path.of(list.get(0));
        }


        Browser browser = Browser_creation_context.first(stage,path.getParent(),logger);
        browser.my_Stage.the_Stage.hide();
        Image_window image_stage = Image_window.get_Image_window(browser, path, logger);
    }
}

