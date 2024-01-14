package klik.files_and_paths;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import klik.look.Look_and_feel_manager;
import klik.util.Logger;

public class Ding {
    public static void play(Logger logger) {

        Media clip = Look_and_feel_manager.load_audio_from_jar("ding.mp3");

        MediaPlayer local = new MediaPlayer(clip);
        local.setCycleCount(1);
        local.setOnStalled(() -> logger.log("\n\nWARNING player is stalling !!"));
        local.setOnReady(() -> {
            logger.log("player READY!");
            local.play();
        });
    }
}
