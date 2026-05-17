// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.browsers;

import javafx.stage.Window;
import javafx.scene.paint.Color;
import klikr.Window_builder;
import klikr.audio.player.The_audio_player;
import klikr.browser_core.Abstract_browser;
import klikr.browser_core.Window_manager;
import klikr.path_lists.Path_list_provider;
import klikr.path_lists.Path_list_provider_for_file_system;
import klikr.path_lists.Path_list_provider_for_playlist;
import klikr.util.execute.actor.Aborter;
import klikr.change.old_and_new.Old_and_new_Path;
import klikr.util.log.Logger;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

//**********************************************************
public class Browser_for_song_playlist extends Abstract_browser
//**********************************************************
{
    public Path_list_provider_for_playlist path_list_provider;

    //**********************************************************
    public Browser_for_song_playlist(Window_builder window_builder, Logger logger)
    //**********************************************************
    {
        super(Color.PINK, logger);
        logger.log("Browser_for_song_playlist\n");
        if (window_builder.path_list_provider instanceof Path_list_provider_for_file_system)
        {
            logger.log("Browser_for_song_playlist FATAL, need a Path_list_provider_for_playlist\n");
            return;
        }
        aborter = new Aborter("Abstract_browser for: " + get_name(), logger);

        path_list_provider = (Path_list_provider_for_playlist) window_builder.path_list_provider;

        logger.log("Browser_for_song_playlist created with path_list_provider: " + path_list_provider.get_key());


        init_abstract_browser(window_builder, this, "song_playlist",aborter);

        my_Stage.the_Stage.setOnCloseRequest(event ->
            {
                Window_manager.unregister(ID,logger);
                The_audio_player.set_browser_is_null();
            });
    }


    //**********************************************************    @Override
    protected String get_name()
    //**********************************************************
    {
        return "Browser_for_song_playlist" ;
    }

    //**********************************************************    @Override
    protected String get_path_for_history()
    //**********************************************************
    {
        return get_Path_list_provider().get_key();
    }


    //*******************************************************
    @Override // File_comparator_provider
    public Comparator<? super Path> get_file_comparator()
    //*******************************************************
    {
        return virtual_landscape.other_file_comparator;
    }

    //**********************************************************
    @Override
    protected Path_list_provider get_Path_list_provider()
    //**********************************************************
    {
        return path_list_provider;
    }

    //**********************************************************
    @Override
    protected String signature()
    //**********************************************************
    {
        return "";
    }

    //**********************************************************
    @Override
    protected void monitor_current_path_list_source()
    //**********************************************************
    {
        logger.log("Browser_for_song_playlist monitor_current_path_list_source NOT IMPLEMENTED");
    }

    //**********************************************************
    @Override
    public void set_title()
    //**********************************************************
    {
        my_Stage.the_Stage.setTitle("SONG PLAYLIST:" + path_list_provider.the_playlist_file_path.getFileName().toString()+"(this is NOT a folder!)");

    }

    //**********************************************************
    @Override // Change_receiver
    public void you_receive_this_because_a_file_event_occurred_somewhere(List<Old_and_new_Path> l, Window owner, Logger logger)
    //**********************************************************
    {
        logger.log("Browser_for_song_playlist you_receive_this_because_a_file_event_occurred_somewhere "+ l);
        virtual_landscape.redraw_fx(true,"change received",false);
    }

    //**********************************************************
    @Override
    public String get_Change_receiver_string()
    //**********************************************************
    {
        return "";
    }

}
