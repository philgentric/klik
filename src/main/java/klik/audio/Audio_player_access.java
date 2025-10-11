package klik.audio;

import klik.util.execute.Execute_command;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import klik.util.tcp.TCP_client;
import klik.util.tcp.TCP_client_out;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

//**********************************************************
public class Audio_player_access
//**********************************************************
{
    public static final String PLAY_REQUEST_ACCEPTED = "PLAY_REQUEST_ACCEPTED";

    // this is the system wide port where audio play requests are sent
    // the audio player listens to this port
    // also a new instance will fail to start if this port is already taken
    // making sure that only one audio player is running at a time
    // to avoid cacophony
    public static final int AUDIO_PLAYER_PORT = 34539;

    // these static calls can be used by any app, typically the klik browser,
    // to ask the audio player to play a song or a playlist
    //**********************************************************
    public static void play_song_in_separate_process(File song, Logger logger)
    //**********************************************************
    {
        // try to connect in case an audio player is already started
        TCP_client_out tco = TCP_client.request("localhost",AUDIO_PLAYER_PORT,song.getAbsolutePath().toString(),logger);
        if ( tco.status())
        {
            if ( tco.reply().equals(PLAY_REQUEST_ACCEPTED))
            {
                logger.log("apparently the TCP server in the separate audio_player process accepted the song");
                return;
            }
            logger.log(Stack_trace_getter.get_stack_trace("SHOULD NOT HAPPEN"));
            logger.log("status: "+tco.status());
            logger.log("reply: "+tco.reply());
            logger.log("error_message: "+tco.error_message());
        }
        else
        {
            logger.log("there is no separate audio_player process, let us start one");
            start_new_process_to_play(song,logger);
        }
    }

    //**********************************************************
    public static void play_play_list_in_separate_process(File song, Logger logger)
    //**********************************************************
    {
        // try to connect in case an audio player is already started
        TCP_client_out tco = TCP_client.request("localhost",AUDIO_PLAYER_PORT,song.getAbsolutePath().toString(),logger);
        if ( tco.status())
        {
            if ( tco.reply().equals(PLAY_REQUEST_ACCEPTED))
            {
                logger.log("apparently the TCP server in the separate audio_player process accepted the song");
                return;
            }
            logger.log(Stack_trace_getter.get_stack_trace("SHOULD NOT HAPPEN"));
            logger.log("status: "+tco.status());
            logger.log("reply: "+tco.reply());
            logger.log("error_message: "+tco.error_message());
        }
        else
        {
            logger.log("there is no separate audio_player process, let us start one");
            start_new_process_to_play(song,logger);
        }
    }


    //**********************************************************
    public static void start_new_process_to_play(File song, Logger logger)
    //**********************************************************
    {
        List<String> cmds = new ArrayList<>();
        logger.log("start_new_process_to_play()");
        cmds.add("gradle");
        cmds.add("audio_player");
        String path =  "--args=\""+song.getAbsolutePath()+"\"";
        cmds.add(path);

        StringBuilder sb = new StringBuilder();
        Execute_command.execute_command_list_no_wait(cmds,new File("."),20*1000,sb,logger);
        logger.log(sb.toString());
    }

}
