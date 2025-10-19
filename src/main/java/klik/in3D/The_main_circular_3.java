package klik.in3D;

import javafx.application.Application;
import javafx.scene.*;
import javafx.stage.Stage;
import klik.util.log.Logger;
import klik.util.log.Simple_logger;

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
        Logger logger = new Simple_logger();
        int icon_size = 512;

        Image_source image_source = new Image_source_from_files( Path.of("."),icon_size);
        //image_source = new Dummy_text_image_source(icon_size,30000);

        Circle_3D circle_3D = new Circle_3D(icon_size,image_source);
        Scene scene = circle_3D.get_scene(primaryStage,logger);
        primaryStage.setTitle("3D Circular Corridor Walk v2");
        primaryStage.setScene(scene);
        primaryStage.show();

    }
}
