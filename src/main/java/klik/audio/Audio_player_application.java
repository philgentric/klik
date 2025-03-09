package klik.audio;

import javafx.application.Application;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import klik.look.Look_and_feel_manager;
import klik.util.log.Logger;
import klik.util.log.System_out_logger;

import java.io.File;
import java.util.List;

public class Audio_player_application extends Application
{
    public static void main(String[] args) {launch(args);}

    @Override
    public void start(Stage stage) throws Exception {

        System.out.println("Audio_player_application start");

        Parameters params = getParameters();
        List<String> list = params.getRaw();
        System.out.println("parameters:"+list.size());
        for(String each : list){
            System.out.println(each);
        }


        Logger logger =  new System_out_logger();
        Look_and_feel_manager.init_Look_and_feel(logger);
        //FileChooser fileChooser = new FileChooser();
        //fileChooser.setTitle("Open Audio File");
        //File f = fileChooser.showOpenDialog(stage);

        File f = new File (list.get(0));
        logger.log("Audio_player_application, opening audio file: "+f.getAbsolutePath());

        Audio_player.play_song_same_process(f, logger);

    }
}
