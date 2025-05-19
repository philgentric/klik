package klik.audio;

import javafx.application.Platform;
import klik.actor.Aborter;
import klik.browser.Shared_services;
import klik.util.execute.Execute_command;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import klik.util.tcp.*;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

//**********************************************************
public class Audio_player
//**********************************************************
{
    static Audio_player_FX_UI instance = null;
    public static final int AUDIO_PLAYER_PORT = 34539;
    public static final String PLAY_REQUEST_ACCEPTED = "PLAY REQUEST ACCEPTED";

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
    public static void init(Aborter aborter,Logger logger)
    //**********************************************************
    {
        instance = new Audio_player_FX_UI(aborter, logger);
    }
    //**********************************************************
    public static void play_playlist(File file, Logger logger)
    //**********************************************************
    {
        if ( instance == null)
        {
            init(Shared_services.shared_services_aborter,logger);
        }
        instance.play_playlist_internal(file);
    }

    //**********************************************************
    public static void play_this_song(File song,Logger logger)
    //**********************************************************
    {

        File finalSong = song;
        Runnable r = () ->
        {
            if (instance == null)
            {
                logger.log(Stack_trace_getter.get_stack_trace("FATAL: you must call Audio_player.init() before trying to play"));
            }

            instance.change_song(finalSong);
        };
        if (Platform.isFxApplicationThread()) r.run();
        else Platform.runLater(r);
    }

}
