package klik.audio;

import javafx.stage.Window;
import klik.New_audio_window_context;
import klik.browser.Abstract_browser;
import klik.browser.virtual_landscape.Path_list_provider;
import klik.change.Change_receiver;
import klik.experimental.image_playlist.Path_list_provider_for_playlist;
import klik.util.files_and_paths.old_and_new.Old_and_new_Path;
import klik.util.log.Logger;

import java.util.List;

//**********************************************************
public class Song_playlist_browser extends Abstract_browser
//**********************************************************
{
    public final Path_list_provider_for_playlist path_list_provider;

    //**********************************************************
    public Song_playlist_browser(New_audio_window_context context, Logger logger)
    //**********************************************************
    {
        super(logger);
        logger.log("Song_playlist_browser\n");
        path_list_provider = new Path_list_provider_for_playlist(context.play_list_file_path, logger);

        logger.log("Song_playlist_browser created with path_list_provider: " + path_list_provider.get_name());

        Change_receiver cr = new Change_receiver()
        {
            @Override
            public void you_receive_this_because_a_file_event_occurred_somewhere(List<Old_and_new_Path> l, Window owner,
                                                                                 Logger logger2)
            {

            }

            @Override
            public String get_Change_receiver_string()
            {
                return "";
            }
        };
        init_abstract_browser(context.shutdown_target,context.rectangle, cr, "song_playlist");
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
    public void you_receive_this_because_a_file_event_occurred_somewhere(List<Old_and_new_Path> l, Window owner,
                                                                         Logger logger2)
    {

    }

    @Override
    public String get_Change_receiver_string()
    {
        return "";
    }
}
