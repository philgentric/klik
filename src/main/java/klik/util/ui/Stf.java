package klik.util.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import klik.Shared_services;
import klik.look.Look_and_feel_manager;
import klik.util.log.Logger;
import klik.util.log.Simple_logger;

//**********************************************************
public class Stf extends Application
//**********************************************************
{
    //**********************************************************
    @Override
    public void start(Stage stage)
    //**********************************************************
    {
        Shared_services.init("stf", stage);
        Logger logger = new Simple_logger();
        Button button =  new Button();
        Scrollable_text_field stf = new Scrollable_text_field(
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit. "
                        + "Proin in felis sit amet libero tempor semper. "
                        + "Sed ut perspiciatis unde omnis iste natus error sit voluptatem.",
                null,
                button,
                stage,
                null,
                logger);

        // stf.setPrefSize(1000, 30); // Let layout manage width

        Scene scene = new Scene(new StackPane(stf), 1000, 100, Color.WHITE);

        Look_and_feel_manager.set_scene_look(scene, stage, logger);
        stage.setTitle("ScrollableTextField Demo");
        stage.setScene(scene);
        stage.show();
    }

    //**********************************************************
    public static void main(String[] args)
    //**********************************************************
    {
        launch(args);
    }
}