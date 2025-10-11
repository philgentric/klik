package klik.util.files_and_paths;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import klik.look.Jar_utils;
import klik.util.log.Logger;

import java.net.URL;

//**********************************************************
public class Ding
//**********************************************************
{
    //**********************************************************
    public static void play(String origin, Logger logger)
    //**********************************************************
    {

        Media clip = load_audio_from_jar("ding.mp3",logger);

        MediaPlayer local = new MediaPlayer(clip);
        local.setCycleCount(1);
        local.setOnStalled(() -> logger.log("\n\nWARNING player is stalling !!"));
        local.setOnReady(() -> {
            logger.log("DING! from:"+origin);
            local.play();
        });
    }


    //**********************************************************
    public static Media load_audio_from_jar(String file_path, Logger logger)
    //**********************************************************
    {
        URL url = Jar_utils.get_URL_by_name(file_path);
        if ( url == null )
        {
            logger.log("ERROR: cannot get URL for :"+file_path);
            return null;
        }

        Media audioClip = new Media(url.toString());

        return audioClip;
    }


}
