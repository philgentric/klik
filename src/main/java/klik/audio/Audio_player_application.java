package klik.audio;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import klik.Launcher;
import klik.Start_context;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.browser.Shared_services;
import klik.look.Look_and_feel_manager;
import klik.look.my_i18n.My_I18n;
import klik.properties.Non_booleans;
import klik.util.Sys_init;
import klik.util.files_and_paths.Guess_file_type;
import klik.util.log.Logger;
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
   public static final int AUDIO_PLAYER_PORT = 34539;
    public static final String PLAY_REQUEST_ACCEPTED = "PLAY REQUEST ACCEPTED";
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
        Sys_init.init("Audio_player_application");
        logger = Shared_services.shared_services_logger;
        Start_context context = Start_context.get_context_and_args(this);

        if (  !start_server())
        {
            logger.log("failed to start server: this is normal if another instance exists already");
            Start_context.send_started_raw(context.port(),logger);
            stage.close();
            Platform.exit();
            System.exit(0);
            return;
        }

        init(context);
    }

    //**********************************************************
    private void init(Start_context context)
    //**********************************************************
    {
        logger.log("Audio_player_application starts");

        Look_and_feel_manager.init_Look_and_feel(logger);

        String music = My_I18n.get_I18n_string(Look_and_feel_manager.MUSIC, logger);

        Look_and_feel_manager.set_icon_for_main_window(stage, music, Look_and_feel_manager.Icon_type.MUSIC);

        String f = null;
        if ( context != null)
        {
            if (context.path() != null)
            {
                logger.log("Audio_player_application, context.path()) " + context.path().toAbsolutePath());
                f = context.path().toAbsolutePath().toString();
                logger.log("Audio_player_application, opening audio file: " + f);
            }
            Start_context.send_started(context, logger);
        }
        Audio_player.init_ui(Shared_services.shared_services_aborter, logger);
        Audio_player.play_this(f, logger);
    }

    //**********************************************************
    private boolean start_server()
    //**********************************************************
    {
        // start the server to receive subsequent play requests
        // this is to avoid the audio mess when several players
        // are automatically "mixed" by the OS

        Session_factory session_factory = () -> new Session() {
            @Override
            public void on_client_connection(DataInputStream dis, DataOutputStream dos)
            {
                try {
                    String received = TCP_util.read_string(dis);
                    if (received.startsWith(Launcher.LANGUAGE_CHANGED))
                    {
                        Non_booleans.force_reload_from_disk();
                        String new_lang = received.split(" ")[1];
                        logger.log("Audio player: LANGUAGE_CHANGED RECEIVED new lang is "+new_lang);
                        logger.log("Audio player: checking the language value is updated on disk: "+Non_booleans.get_language_key());
                        My_I18n.reset();
                        Runnable r = () -> {
                            Platform.runLater(() ->
                            {
                                //init(null);
                                Audio_player.init_ui(Shared_services.shared_services_aborter, logger);
                            }
                            );
                        };
                        Actor_engine.execute(r,logger);
                    }
                    else
                    {
                        TCP_util.write_string(PLAY_REQUEST_ACCEPTED, dos);
                        dos.flush();
                        Audio_player.play_this(received, logger);
                    }
                    logger.log("Audio_player_application server accepted file for playing");
                }
                catch (IOException e)
                {
                    logger.log(Stack_trace_getter.get_stack_trace(""+e));
                }

            }

            @Override
            public String name() {
                return "";
            }
        };
        TCP_server tcp_server = new TCP_server(session_factory,new Aborter("Audio_player_application TCP server", logger), logger);
        return tcp_server.start(AUDIO_PLAYER_PORT,false);
    }


}
