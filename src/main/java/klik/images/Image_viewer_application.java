package klik.images;

import javafx.application.Application;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import klik.actor.Aborter;
import klik.Shared_services;
import klik.path_lists.Path_list_provider_for_file_system;
import klik.util.Sys_init;
import klik.util.log.Exceptions_in_threads_catcher;
import klik.util.log.Logger;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

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

        Sys_init.init("Image_viewer app",stage);
        Logger logger = Shared_services.logger;
        Aborter aborter = Shared_services.aborter;


        Exceptions_in_threads_catcher.set_exceptions_in_threads_catcher(logger);

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


        //Browser browser = New_file_browser_context.first(path.getParent().toString(),logger);
        //browser.my_Stage.the_Stage.hide();
        Image_window image_stage = Image_window.get_Image_window(path, new Path_list_provider_for_file_system(path.getParent()), Optional.empty(),stage,aborter,logger);
    }
}

