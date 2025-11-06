// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.audio;

import javafx.scene.layout.Pane;
import javafx.stage.Window;
import klik.Window_type;
import klik.Instructions;
import klik.browser.Abstract_browser;
import klik.path_lists.Path_list_provider;
import klik.change.Change_receiver;
import klik.path_lists.Path_list_provider_for_playlist;
import klik.util.files_and_paths.old_and_new.Old_and_new_Path;
import klik.util.log.Logger;

import java.util.List;

//**********************************************************
public class Song_playlist_browser extends Abstract_browser
//**********************************************************
{
    public final Path_list_provider_for_playlist path_list_provider;

    //**********************************************************
    public Song_playlist_browser(Instructions context, Logger logger)
    //**********************************************************
    {
        super(logger);
        logger.log("Song_playlist_browser\n");
        path_list_provider = (Path_list_provider_for_playlist) context.path_list_provider;

        logger.log("Song_playlist_browser created with path_list_provider: " + path_list_provider.get_name());

        Change_receiver cr = new Change_receiver()
        {
            @Override
            public void you_receive_this_because_a_file_event_occurred_somewhere(List<Old_and_new_Path> l, Window owner, Logger logger)
            {

            }

            @Override
            public String get_Change_receiver_string()
            {
                return "";
            }
        };
        init_abstract_browser(Window_type.Song_playlist_1D,context.shutdown_target,context.rectangle, cr, "song_playlist");
    }

    @Override
    protected String get_name()
    {
        return "Song_playlist_browser";
    }

    @Override
    protected Path_list_provider get_Path_list_provider()
    {
        return path_list_provider;
    }

    @Override
    protected String signature()
    {
        return "";
    }

    @Override
    protected void monitor()
    {
    }

    @Override
    public void set_title()
    {

    }

    @Override
    public void you_receive_this_because_a_file_event_occurred_somewhere(List<Old_and_new_Path> l, Window owner, Logger logger)
    {

    }

    @Override
    public String get_Change_receiver_string()
    {
        return "";
    }

    @Override
    public void set_background_color(Pane thePane) {
        // no background color
    }
}
