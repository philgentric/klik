package klik.audio;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import klik.*;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.look.Look_and_feel_manager;
import klik.look.my_i18n.My_I18n;
import klik.properties.Non_booleans_properties;
import klik.util.Sys_init;
import klik.util.log.Logger;
import klik.util.log.Logger_factory;
import klik.util.log.Stack_trace_getter;
import klik.util.tcp.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Path;

//**********************************************************
public class Audio_player_application extends Application
//**********************************************************
{
    private final static String name = "Audio_player_application";
    private Stage stage;
    Logger logger;
    //**********************************************************
    public static void main(String[] args) {launch(args);}
    //**********************************************************

    //**********************************************************
    @Override
    public void start(Stage stage_) throws Exception
    //**********************************************************
    {
        this.stage = stage_;
        Sys_init.init(name,stage_);
        logger = Logger_factory.get(name);
        Start_context context = Start_context.get_context_and_args(this);

        if (  start_server())
        {
            init(context);
        }
        else
        {
            logger.log("AUDIO PLAYER: Aborting start!\n" +
                    "(reason: failed to start server)\n" +
                    "This is normal if the audio player is already running\n" +
                    "Since in general having 2 player playing is just cacophonie :-)");
            // send not_started to unblock the launcher server
            int reply_port = extract_reply_port();
            // blocking call otherwise exit will prevent the reply from flying out
            TCP_client.send("localhost", reply_port, Launcher.NOT_STARTED, logger);


            stage.close();
            Platform.exit();
            System.exit(0);

        }
    }

    //**********************************************************
    private Integer extract_reply_port()
    //**********************************************************
    {
        Path p = Path.of(System.getProperty("user.home"), Non_booleans_properties.CONF_DIR, Non_booleans_properties.FILENAME_FOR_PORT_TO_REPLY_ABOUT_START);
        try {
            if (java.nio.file.Files.exists(p)) {
                java.util.List<String> lines = java.nio.file.Files.readAllLines(p);
                if (lines.size() > 0) {
                    String s = lines.get(0);
                    int port = Integer.parseInt(s);
                    logger.log("Audio_player_application: extracted reply_port= " + port);
                    return port;
                }
            }
        } catch (IOException | NumberFormatException e) {
            logger.log("WARNING: could not read reply_port "+p);
        }
        return null;
    }

    //**********************************************************
    private void init(Start_context context)
    //**********************************************************
    {
        logger.log("Audio_player_application starts");
        System_info.print(Audio_player_application.class);

        String music = My_I18n.get_I18n_string(Look_and_feel_manager.MUSIC, stage,logger);

        Look_and_feel_manager.set_icon_for_main_window(stage, music, Look_and_feel_manager.Icon_type.MUSIC,stage,logger);

        Integer reply_port = extract_reply_port();
        if ( reply_port == null)
        {
            logger.log("Audio_player_application, cannot send reply?");
        }
        else
        {
            TCP_client.send_in_a_thread("localhost",reply_port, Launcher.STARTED,logger);
        }

        //Window_provider window_provider = New_audio_window_context.additional_no_past(null,stage,logger);

        UI_instance_holder.init_ui(Shared_services.aborter, logger);
        if ( context != null)
        {
            Path path = context.extract_path();
            if (path == null)
            {
                logger.log("Audio_player_application, NO audio file found");
            }
            else
            {
                long start = System.currentTimeMillis();
                UI_instance_holder.play_this(path.toAbsolutePath().toString(), start,true,logger);
                logger.log("Audio_player_application, opening audio file = " + path.toAbsolutePath());
            }
        }
    }

    //**********************************************************
    private boolean start_server()
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
                        TCP_util.write_string(Audio_player_access.PLAY_REQUEST_ACCEPTED, dos);
                        dos.flush();
                        UI_instance_holder.play_this(received, start, false, logger);
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
        TCP_server tcp_server = new TCP_server(session_factory,new Aborter("Audio_player_application TCP server", logger), logger);
        return tcp_server.start(Audio_player_access.AUDIO_PLAYER_PORT,"Audio player listening for songs and playlists",false);
    }


}
