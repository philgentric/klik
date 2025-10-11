package klik.experimental.work_in_progress.video;

//import com.google.common.net.UrlEscapers;
import klik.util.log.Logger;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.io.File;
import java.io.IOException;

//**********************************************************
public class Experimental_player
//**********************************************************
{

    private static MediaPlayer the_static_player;


    //**********************************************************
    public static void play_one(File f, Logger logger)
    //**********************************************************
    {
        if (the_static_player != null)
        {
            the_static_player.stop();
            the_static_player.dispose();
        }
        logger.log("music.Player =>"+f.getAbsolutePath());

        Media sound;
        try {

            String encodedString = "file://"+f.getCanonicalPath();
            encodedString = encodedString.replaceAll(" ","%20");
            //UrlEscapers.urlFragmentEscaper().escape("file://"+f.getCanonicalPath());

            sound = new Media( encodedString  );
            the_static_player = new MediaPlayer(sound);
            the_static_player.play();
            System.out.println("play() start: "+the_static_player.getCurrentTime());
            System.out.println("volume="+the_static_player.getVolume());
            System.out.println("status="+the_static_player.getStatus());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    //**********************************************************
    public static MediaPlayer prepare_for_this(File f, Logger logger)
    //**********************************************************
    {

        logger.log(("music.Player =>"+f.getAbsolutePath()));

        try {

            String encodedString = "file://"+f.getCanonicalPath();
            encodedString = encodedString.replaceAll(" ","%20");
            //UrlEscapers.urlFragmentEscaper().escape("file://"+f.getCanonicalPath());

            Media sound = new Media( encodedString  );
            MediaPlayer local = new MediaPlayer(sound);
            local.setCycleCount(1);
            local.setOnStalled(new Runnable(){
                public void run()
                {
                    logger.log("\n\nWARNING player is stalling !!");
                }
            });
            return local;
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return null;
    }
}
