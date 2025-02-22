package klik.audio;

import javafx.application.Application;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import klik.look.Look_and_feel_manager;
import klik.util.log.Logger;
import klik.util.log.System_out_logger;

import java.io.File;

public class Audio_player_application extends Application {
    public static void main(String[] args) {launch(args);}

    @Override
    public void start(Stage stage) throws Exception {

        Logger logger =  new System_out_logger();
        Look_and_feel_manager.init_Look_and_feel(logger);
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Audio File");
        File f = fileChooser.showOpenDialog(stage);

        Audio_player.play_song(f, logger);

    }
}
