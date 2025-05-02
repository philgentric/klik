package klik.audio;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import klik.actor.Aborter;
import klik.look.Look_and_feel_manager;
import klik.look.my_i18n.Language_manager;
import klik.look.my_i18n.My_I18n;
import klik.util.execute.Execute_command;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import klik.util.log.System_logger;
import klik.util.tcp.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

//**********************************************************
public class Audio_player extends Application
//**********************************************************
{
    private final static boolean dbg = true;
    static Audio_player_frame instance = null;
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
        Logger logger =  System_logger.get_system_logger("Audio_player_frame");

        if (    !start_server(logger))
        {
            logger.log("failed to start server: this is normal if another instance exists already");
            stage.close();
            Platform.exit();
            System.exit(0);
            return;
        }




        logger.log("Audio_player start");

        Parameters params = getParameters();
        List<String> list = params.getRaw();

        if ( dbg)
        {
            logger.log("parameters:"+list.size());
            for(String each : list){
                logger.log(each);
            }

        }

        Look_and_feel_manager.init_Look_and_feel(logger);
        Language_manager.init_registered_languages(logger);

        String music = My_I18n.get_I18n_string(Look_and_feel_manager.MUSIC,logger);

        Look_and_feel_manager.set_icon_for_main_window(stage, music, Look_and_feel_manager.Icon_type.MUSIC);

        File f = null;
        if ( list != null)
        {
            if ( list.size() > 0)
            {
                logger.log("Audio_player, list.get(0) "+list.get(0));
                f = new File (list.get(0));
                logger.log("Audio_player, opening audio file: "+f.getAbsolutePath());
            }
        }

        play_this_song(f,logger);

    }

    private static boolean start_server(Logger logger)
    {
        // start the server to receive subsequent play requests
        // this is to avoid the audio mess when several players
        // are automatically "mixed" by the OS

        Session_factory session_factory = () -> new Session() {
            @Override
            public void on_client_connection(DataInputStream dis, DataOutputStream dos)
            {
                try {
                    int size = dis.readInt();
                    byte buffer[] = new byte[size];
                    dis.read(buffer);
                    String file_path = new String(buffer, StandardCharsets.UTF_8);
                    File f1 = new File(file_path);
                    play_this_song(f1,logger);
                    String reply = PLAY_REQUEST_ACCEPTED;
                    buffer = reply.getBytes(StandardCharsets.UTF_8);
                    dos.writeInt(buffer.length);
                    dos.write(buffer);
                    logger.log("accepted file for playing");
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
        TCP_server tcp_server = new TCP_server(session_factory,new Aborter("audio", logger), logger);
        return tcp_server.start(AUDIO_PLAYER_PORT,false);
    }



    // entry #1
    //**********************************************************
    public static void play_song_in_separate_process(File song, Logger logger)
    //**********************************************************
    {
        // try to connect in case an audio player is already started
        TCP_client_out tco = TCP_client.request("localhost",AUDIO_PLAYER_PORT,song.getAbsolutePath().toString(),logger);
        if ( tco.status())
        {
            if ( tco.reply().equals(PLAY_REQUEST_ACCEPTED)) {
                logger.log("apparently the TCP server in the separate audio_player process accepted the song");
                return;
            }
            logger.log(Stack_trace_getter.get_stack_trace("SHOULD NOT HAPPEN"));
            logger.log("status: "+tco.status());
            logger.log("reply: "+tco.reply());
            logger.log("message: "+tco.message());
        }
        else
        {
            logger.log("there is no separate audio_player process, let us start one");
            start_new_process_to_play_song(song,logger);
        }
    }

    //**********************************************************
    public static void start_new_process_to_play_song(File song, Logger logger)
    //**********************************************************
    {
        List<String> cmds = new ArrayList<>();
        logger.log("start_new_process_to_play_song()");
        cmds.add("gradle");
        cmds.add("audio_player");
        String path =  "--args=\""+song.getAbsolutePath()+"\"";
        cmds.add(path);

        StringBuilder sb = new StringBuilder();
        Execute_command.execute_command_list_no_wait(cmds,new File("."),20*1000,sb,logger);
        logger.log(sb.toString());
    }

    //**********************************************************
    public static void start_new_process_to_browse(Path folder, Logger logger)
    //**********************************************************
    {
        List<String> cmds = new ArrayList<>();
        logger.log("start_new_process_to_browse()");
        cmds.add("gradle");
        cmds.add("klik");
        String path =  "--args=\""+folder.toAbsolutePath()+"\"";
        cmds.add(path);

        StringBuilder sb = new StringBuilder();
        Execute_command.execute_command_list_no_wait(cmds,new File("."),20*1000,sb,logger);
        logger.log(sb.toString());
    }



    //**********************************************************
    public static void play_playlist(File file, Logger logger)
    //**********************************************************
    {
        if ( instance == null)
        {
            instance = new Audio_player_frame(logger);
        }


        instance.play_playlist_internal(file);
    }

    //**********************************************************
    public static void play_this_song(File song, Logger logger)
    //**********************************************************
    {

        Runnable r = () ->
        {
            if (instance == null)
            {
                instance = new Audio_player_frame(logger);
            }

            instance.change_song(song);
        };
        if (Platform.isFxApplicationThread()) r.run();
        else Platform.runLater(r);
    }
}
