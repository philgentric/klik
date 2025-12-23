package klik.audio;

import javafx.application.Platform;
import javafx.stage.Stage;
import klik.Launcher;
import klik.Shared_services;
import klik.System_info;
import klik.look.Look_and_feel_manager;
import klik.look.my_i18n.My_I18n;
import klik.properties.Non_booleans_properties;
import klik.util.execute.actor.Aborter;
import klik.util.execute.actor.Actor_engine;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import klik.util.tcp.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

//**********************************************************
public class Audio_player
//**********************************************************
{
    private static final boolean dbg = false;
    private Stage stage;
    private Logger logger;
    private static TCP_server tcp_server;

    //**********************************************************
    public Audio_player(Path path,  Logger logger)
    //**********************************************************
    {
        this.logger = logger;
        stage = new Stage();

        if (  start_server(Shared_services.aborter(),stage,logger))
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
    public static boolean start_server(Aborter aborter, Stage stage, Logger logger)
    //**********************************************************
    {
        // start the server to receive play requests via TCP
        // also listen for UI_CHANGED messages

        Session_factory session_factory = () -> new Session() {
            @Override
            public boolean on_client_connection(DataInputStream dis, DataOutputStream dos)
            {
                try {
                    String received = TCP_util.read_string(dis);
                    if (received.startsWith(Launcher.UI_CHANGED))
                    {
                        Non_booleans_properties.force_reload_from_disk(stage);
                        String change = received.split(" ")[1];
                        logger.log("Audio player: UI_CHANGED RECEIVED change is: "+change);
                        My_I18n.reset();
                        Look_and_feel_manager.reset();
                        Runnable r = () -> Platform.runLater(() -> UI_instance_holder.define_ui());
                        Actor_engine.execute(r,"Redefining UI upon TCP message",logger);
                    }
                    else
                    {
                        long start = System.currentTimeMillis();
                        TCP_util.write_string(Audio_player_gradle_start.PLAY_REQUEST_ACCEPTED, dos);
                        dos.flush();
                        UI_instance_holder.play_this(received, start, false, stage,logger);
                    }
                    logger.log("Audio_player_application server accepted file for playing");
                }
                catch (IOException e)
                {
                    logger.log(Stack_trace_getter.get_stack_trace(""+e));
                }

                return true;
            }

            @Override
            public String name() {
                return "";
            }
        };
        tcp_server = new TCP_server(session_factory,aborter, logger);
        return tcp_server.start(Audio_player_gradle_start.AUDIO_PLAYER_PORT,"Audio player listening for songs and playlists",false);
    }




    //**********************************************************
    public static void init(boolean as_app,Path path, Stage stage, Logger logger)
    //**********************************************************
    {
        logger.log("Audio_player starts");
        if ( as_app) {
            System_info.print();
            String music = My_I18n.get_I18n_string(Look_and_feel_manager.MUSIC, stage, logger);
            Look_and_feel_manager.set_icon_for_main_window(stage, music, Look_and_feel_manager.Icon_type.MUSIC, stage, logger);
        }
        Integer reply_port = extract_reply_port(logger);
        if ( reply_port == null)
        {
            logger.log("Audio_player_application, cannot send reply?");
        }
        else
        {
            TCP_client.send_in_a_thread("localhost",reply_port, Launcher.STARTED,logger);
        }

        //Window_provider window_provider = New_song_playlist_context.additional_no_past(null,stage,logger);

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
    public static Integer extract_reply_port(Logger logger)
    //**********************************************************
    {
        Path p = Path.of(System.getProperty("user.home"), Non_booleans_properties.CONF_DIR, Non_booleans_properties.FILENAME_FOR_PORT_TO_REPLY_ABOUT_START);
        try {
            if (java.nio.file.Files.exists(p)) {
                java.util.List<String> lines = java.nio.file.Files.readAllLines(p, StandardCharsets.UTF_8);
                if (lines.size() > 0) {
                    String s = lines.get(0);
                    int port = Integer.parseInt(s);
                    if ( dbg) logger.log("✅ Audio_player_application: extracted reply_port= " + port);
                    return port;
                }
            }
        } catch (IOException | NumberFormatException e) {
            logger.log("❗Warning: could not read reply_port "+p);
        }
        return null;
    }


    //**********************************************************
    public void die()
    //**********************************************************
    {
        logger.log("audio player die!");
        tcp_server.stop();
        UI_instance_holder.die();
        stage.close();
    }
}
