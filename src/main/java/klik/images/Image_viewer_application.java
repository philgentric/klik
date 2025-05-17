package klik.images;

import javafx.application.Application;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import klik.actor.Aborter;
import klik.browser.classic.Folder_path_list_provider;
import klik.look.Look_and_feel_manager;
import klik.look.my_i18n.Language_manager;
import klik.properties.Non_booleans;
import klik.properties.Properties_manager;
import klik.util.Sys_init;
import klik.util.log.Exceptions_in_threads_catcher;
import klik.util.log.Logger;
import klik.util.log.System_logger;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

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
        Sys_init sys_init = Sys_init.get("Image_viewer");
        Logger logger = sys_init.logger();
        Aborter aborter = sys_init.aborter();

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


        //Browser browser = New_window_context.first(path.getParent().toString(),logger);
        //browser.my_Stage.the_Stage.hide();
        Image_window image_stage = Image_window.get_Image_window(path, new Folder_path_list_provider(path.getParent()), Optional.empty(),aborter,logger);
    }
}

