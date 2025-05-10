package klik.browser;

import klik.browser.virtual_landscape.Path_list_provider;
import klik.util.files_and_paths.Old_and_new_Path;
import klik.util.log.Logger;

import java.util.List;

//**********************************************************
public class Image_playlist extends Abstract_browser
//**********************************************************
{

    public final Playlist_path_list_provider path_list_provider;

    //**********************************************************
    public Image_playlist(Browser_creation_context context, Logger logger)
    //**********************************************************
    {
        super(logger);
        path_list_provider = new Playlist_path_list_provider(context.target, logger);
        init(context,"playlist");
    }
    //**********************************************************
    @Override
    protected String get_name()
    //**********************************************************
    {
        return path_list_provider.get_name();
    }

    //**********************************************************
    @Override
    public Path_list_provider get_Path_list_provider()
    //**********************************************************
    {
        return path_list_provider;
    }

    //**********************************************************
    @Override
    public String signature()
    //**********************************************************
    {
        return path_list_provider.get_name();
    }

    //**********************************************************
    @Override
    void monitor()
    //**********************************************************
    {

    }

    //**********************************************************
    @Override
    public void set_title()
    //**********************************************************
    {
        my_Stage.the_Stage.setTitle(path_list_provider.get_name());
    }

    //**********************************************************
    @Override
    public void go_full_screen()
    //**********************************************************
    {

    }

    //**********************************************************
    @Override
    public void stop_full_screen()
    //**********************************************************
    {

    }

    //**********************************************************
    @Override
    public void shutdown()
    //**********************************************************
    {

    }

    //**********************************************************
    @Override // Change_receiver
    public void you_receive_this_because_a_file_event_occurred_somewhere(List<Old_and_new_Path> l, Logger logger2)
    //**********************************************************
    {
        for (Old_and_new_Path oanp : l)
        {
            if (oanp.new_Path != null)
            {
                String s = path_list_provider.the_play_list_file_path.toAbsolutePath().toString();
                if( oanp.new_Path.toAbsolutePath().toString().equals(s))
                {
                    virtual_landscape.redraw_fx("change gang for dir: " + path_list_provider.the_play_list_file_path);
                }
            }
        }
    }

    //**********************************************************
    @Override
    public String get_Change_receiver_string()
    //**********************************************************
    {
        return "";
    }
}
