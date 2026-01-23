package klikr.audio;

import javafx.application.Platform;
import javafx.stage.Stage;
import klikr.util.Shared_services;
import klikr.System_info;
import klikr.look.Look_and_feel_manager;
import klikr.look.my_i18n.My_I18n;
import klikr.properties.Non_booleans_properties;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.http.Klikr_communicator;
import klikr.util.log.Logger;

import java.nio.file.Path;

//**********************************************************
public class Audio_player
//**********************************************************
{
    private static final boolean dbg = false;
    private Stage stage;
    private Logger logger;
    Klikr_communicator klikr_communicator;

    //**********************************************************
    public Audio_player(Path path,  Logger logger)
    //**********************************************************
    {
        this.logger = logger;
        stage = new Stage();

        klikr_communicator = new Klikr_communicator("Audio_player",stage,logger);
        if (klikr_communicator.start_as_singleton())
        {
            init(false,path,stage,logger);
        }
        else
        {
            logger.log("AUDIO PLAYER: Not starting!\n" +
                    "(reason: failed to start server)\n" +
                    "This is normal if the audio player is already running\n" +
                    "Since in general having 2 player playing is just cacophony :-)");

            stage.close();
        }
    }

    //**********************************************************
    public static void on_ui_changed(String received, Aborter aborter, Stage stage, Logger logger)
    //**********************************************************
    {
        Non_booleans_properties.force_reload_from_disk(stage);
        String change = received.split(" ")[1];
        logger.log("Audio player: UI_CHANGED RECEIVED change is: " + change);
        My_I18n.reset();
        Look_and_feel_manager.reset();
        Runnable r = () -> Platform.runLater(() -> UI_instance_holder.define_ui());
        Actor_engine.execute(r, "Redefining UI upon TCP message", logger);
    }

    //**********************************************************
    public static void on_play(String received, Aborter aborter, Stage stage, Logger logger)
    //**********************************************************
    {
        long start = System.currentTimeMillis();
        UI_instance_holder.play_this(received, start, false, stage,logger);
    }





    //**********************************************************
    public static void init(boolean as_app,Path path, Stage stage, Logger logger)
    //**********************************************************
    {
        logger.log("Audio_player starts");
        if ( as_app) {
            System_info.print(stage,logger);
            String music = My_I18n.get_I18n_string(Look_and_feel_manager.MUSIC, stage, logger);
            Look_and_feel_manager.set_icon_for_main_window(stage, music, Look_and_feel_manager.Icon_type.MUSIC, stage, logger);
        }

        UI_instance_holder.init_ui(Shared_services.aborter(), logger);
        long start = System.currentTimeMillis();
        if (path == null)
        {
            logger.log("✅ Audio_player, NO audio file found in context");
            UI_instance_holder.play_this(null, start,true,stage, logger);
        }
        else
        {
            logger.log("✅ Audio_player, opening audio file = " + path.toAbsolutePath());
            UI_instance_holder.play_this(path.toAbsolutePath().toString(), start,true,stage, logger);
        }
    }




    //**********************************************************
    public void die()
    //**********************************************************
    {
        logger.log("audio player die!");
        klikr_communicator.stop();
        UI_instance_holder.die();
        stage.close();
    }
}
