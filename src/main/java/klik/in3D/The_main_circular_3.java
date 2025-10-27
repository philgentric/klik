package klik.in3D;

import javafx.application.Application;
import javafx.scene.*;
import javafx.stage.Stage;
import klik.Shared_services;
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
        Shared_services.init("main circular 3D");
        Logger logger = new Simple_logger();
        int icon_size = 512;
        Exceptions_in_threads_catcher.set_exceptions_in_threads_catcher(logger);
        Perf.monitor(logger);


        Path p = Path.of(System.getProperty("user.home"));
        Hourglass hourglass = Circle_3D.get_hourglass(primaryStage,logger);
        Circle_3D circle_3D = new Circle_3D(p,icon_size,primaryStage,logger);
        Scene scene = circle_3D.get_scene(hourglass);
        primaryStage.setTitle("3D Circular Corridor Walk v2");
        primaryStage.setScene(scene);
        primaryStage.show();

    }
}
