package klik.in3D;

import javafx.application.Application;
import javafx.scene.*;
import javafx.stage.Stage;
import klik.Context_type;
import klik.New_context;
import klik.Shared_services;
import klik.path_lists.Path_list_provider_for_file_system;
import klik.util.log.Exceptions_in_threads_catcher;
import klik.util.log.Logger;
import klik.util.log.Simple_logger;
import klik.util.perf.Perf;
import klik.util.ui.Hourglass;

import java.nio.file.Path;

//*******************************************************
public class The_main_circular_3 extends Application
//*******************************************************
{

    //*******************************************************
    static void main(String[] args) {
        Application.launch(args);
    }
    //*******************************************************

    //*******************************************************
    @Override
    public void start(Stage primaryStage)
    //*******************************************************
    {
        Shared_services.init("main circular 3D", primaryStage);
        Logger logger = new Simple_logger();
        Exceptions_in_threads_catcher.set_exceptions_in_threads_catcher(logger);
        Perf.monitor(logger);


        Path p = Path.of(System.getProperty("user.home"));
        New_context.additional_no_past(Context_type.File_system_3D,new Path_list_provider_for_file_system(p),primaryStage,logger);

    }
}
