package klik.audio;

import javafx.application.Platform;
import klik.Shared_services;
import klik.actor.Aborter;
import klik.util.execute.Execute_command;
import klik.util.files_and_paths.Guess_file_type;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

//**********************************************************
public class UI_instance_holder
//**********************************************************
{
    private static Audio_player_FX_UI ui = null;


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
    public static void init_ui(Aborter aborter, Logger logger)
    //**********************************************************
    {
        ui = new Audio_player_FX_UI(aborter, logger);
        define_ui();
    }

    //**********************************************************
    public static void define_ui()
    //**********************************************************
    {
        ui.define_ui();
        ui.playlist_init();
        ui.set_selected();
    }


    //**********************************************************
    public static void play_this(String file, long start, boolean first_time, Logger logger)
    //**********************************************************
    {
        if ( file == null)
        {
            play_this_song(null,start,first_time,logger);
            return;
        }

        if (Guess_file_type.is_this_path_a_music(Path.of(file)))
        {
            logger.log("audio player going to play song:"+file);
            play_this_song(file,start,first_time,logger);
            return;
        }
        if (Guess_file_type.is_this_path_an_audio_playlist(Path.of(file)))
        {
            logger.log("audio player going to play playlist:"+file);
            play_playlist(new File(file),logger);
            return;
        }
        logger.log("audio player ignoring this:"+file);

    }
    //**********************************************************
    private static void play_playlist(File file, Logger logger)
    //**********************************************************
    {
        if ( ui == null)
        {
            init_ui(Shared_services.aborter,logger);
        }
        ui.play_playlist_internal(file);
    }

    //**********************************************************
    private static void play_this_song(String song, long start, boolean first_time, Logger logger)
    //**********************************************************
    {
        String finalSong = song;
        Runnable r = () ->
        {
            if (ui == null)
            {
                logger.log(Stack_trace_getter.get_stack_trace("FATAL: you must call Audio_player2.init() before trying to play"));
            }

            ui.change_song(finalSong, start, first_time);
        };
        if (Platform.isFxApplicationThread()) r.run();
        else Platform.runLater(r);
    }

    public static void set_null()
    {
        ui =null;
    }
}
