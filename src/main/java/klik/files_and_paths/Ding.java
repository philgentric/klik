package klik.files_and_paths;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import klik.Klik_application;
import klik.look.Look_and_feel_manager;
import klik.util.Logger;

import java.io.InputStream;
import java.net.URL;

public class Ding {
    public static void play(String origin, Logger logger) {

        Media clip = load_audio_from_jar("ding.mp3",logger);

        MediaPlayer local = new MediaPlayer(clip);
        local.setCycleCount(1);
        local.setOnStalled(() -> logger.log("\n\nWARNING player is stalling !!"));
        local.setOnReady(() -> {
            logger.log("DING! from:"+origin);
            local.play();
        });
    }

    private static final boolean audioclip_load_dbg = false;

    //**********************************************************
    public static Media load_audio_from_jar(String file_path, Logger logger)
    //**********************************************************
    {

        if (audioclip_load_dbg)
        {
            logger.log("looking for audio clip->" + file_path + "<-");
            {
                String path = "";
                URL url1 = Klik_application.class.getResource(path);
                if (url1 == null)
                {
                    logger.log("Method1 fails: Klik_application.class.getResource(" + path + ");  failed");
                }
                else
                {
                    logger.log("Method1 works: Klik_application.class.getResource(" + path + ");" + url1.getPath());
                }
            }
            {
                String path = ".";
                URL url2 = Klik_application.class.getResource(path);
                if (url2 == null)
                {
                    logger.log("Method2 fails: Klik_application.class.getResource(" + path + ");  failed");
                }
                else
                {
                    logger.log("Method2 works: Klik_application.class.getResource(" + path + ")" + url2.getPath());
                }
            }
            {
                String path = "../";
                URL url3 = Klik_application.class.getResource(path);
                if (url3 == null)
                {
                    logger.log("Method3 fails: Klik_application.class.getResource(" + path + ");  failed");
                }
                else
                {
                    logger.log("Method3 works: Klik_application.class.getResource(" + path + "); " + url3.getPath());
                }
            }
            {
                String classpath = System.getProperty("java.class.path");
                URL url5 = Klik_application.class.getResource(classpath);
                if (url5 == null)
                {
                    logger.log("Method5 failed");// this is a long string to print
                    // : classpath->"+classpath+"<-");
                }
                else
                {
                    logger.log("Method5 works: classpath " + url5.getPath());
                }
            }
        }

        /*
        this gives the original source path: not the one being deployed
        URL url_loader = Klik_application.class.getProtectionDomain().getCodeSource().getLocation();
        logger.log("===Klik_application.class.getProtectionDomain().getCodeSource().getLocation()====" + url_loader.toString() );
        logger.log("===getProtectionDomain().getCodeSource().getLocation().getPath()====" + url_loader.getPath() );
        */

        URL url4 = Klik_application.class.getResource(file_path);
        if (url4 == null)
        {
            logger.log("Method4 failed :Klik_application.class.getResource(" + file_path + ");  failed");
            InputStream input_stream = Klik_application.class.getResourceAsStream(file_path);
            if (input_stream == null)
            {
                logger.log("Method4 bis failed");
                return null;
            }
            logger.log("Method4 bis worked");

            return null;
        }
        if (audioclip_load_dbg) logger.log("Method4 works :Klik_application.class.getResource(" + file_path + ") path:" + url4.getPath());

        if (audioclip_load_dbg) logger.log("path=" + url4.getPath());

        Media audioClip = new Media(url4.toString());

        return audioClip;
    }


}
