package klik.audio;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import klik.Start_context;
import klik.actor.Aborter;
import klik.browser.Shared_services;
import klik.look.Look_and_feel_manager;
import klik.look.my_i18n.Language_manager;
import klik.look.my_i18n.My_I18n;
import klik.util.Sys_init;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import klik.util.tcp.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

//**********************************************************
public class Audio_player_application extends Application
//**********************************************************
{
   public static final int AUDIO_PLAYER_PORT = 34539;
    public static final String PLAY_REQUEST_ACCEPTED = "PLAY REQUEST ACCEPTED";

    //**********************************************************
    public static void main(String[] args) {launch(args);}
    //**********************************************************

    //**********************************************************
    @Override
    public void start(Stage stage) throws Exception
    //**********************************************************
    {
        Sys_init.init("Audio_player_application");
        Logger logger = Shared_services.shared_services_logger;
        Start_context context = Start_context.get_context_and_args(this);

        if (  !start_server(logger))
        {
            logger.log("failed to start server: this is normal if another instance exists already");
            Start_context.send_started_raw(context,logger);
            stage.close();
            Platform.exit();
            System.exit(0);
            return;
        }

        logger.log("Audio_player_application starts");


        Look_and_feel_manager.init_Look_and_feel(logger);
        Language_manager.init_registered_languages(logger);

        String music = My_I18n.get_I18n_string(Look_and_feel_manager.MUSIC,logger);

        Look_and_feel_manager.set_icon_for_main_window(stage, music, Look_and_feel_manager.Icon_type.MUSIC);

        String f = null;
        if ( context.path() != null)
        {
             logger.log("Audio_player_application, context.path()) "+context.path().toAbsolutePath());
             f = context.path().toAbsolutePath().toString();
             logger.log("Audio_player_application, opening audio file: "+f);
        }
        Start_context.send_started(context,logger);

        Audio_player.init(Shared_services.shared_services_aborter, logger);
        Audio_player.play_this_song(f,logger);
    }
    //**********************************************************
    private static boolean start_server(Logger logger)
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
                    String file_path = TCP_util.read_string(dis);
                    Audio_player.play_this_song(file_path,logger);
                    TCP_util.write_string(PLAY_REQUEST_ACCEPTED,dos);
                    dos.flush();
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
