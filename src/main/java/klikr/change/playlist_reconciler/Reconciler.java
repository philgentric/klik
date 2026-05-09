package klikr.change.playlist_reconciler;

import klikr.change.old_and_new.Old_and_new_Path;
import klikr.util.log.Logger;

import java.util.ArrayList;
import java.util.List;

// when the user changes a path, renaming a file or moving it
// this component will scan all playlists
// and patch the ones that are affected
//**********************************************************
public class Reconciler
//**********************************************************
{
    private static Reconciler instance;
    private final Logger logger;
    private final List<Playlist> playlists = new ArrayList<>();
    //**********************************************************
    public Reconciler(Logger logger)
    //**********************************************************
    {
        this.logger = logger;
    }

    //**********************************************************
    public static void add_playlist(Playlist playlist, Logger logger)
    //**********************************************************
    {
        init(logger);
        instance.playlists.add(playlist);
    }
    //**********************************************************
    public static void receive_change(Old_and_new_Path old_and_new_path, Logger logger)
    //**********************************************************
     {
         init(logger);
         instance.receive_change_internal(old_and_new_path);
     }

    //**********************************************************
    private static void init(Logger logger)
    //**********************************************************
    {
        synchronized(Reconciler.class)
        {
            if (instance == null)
            {
                instance = new Reconciler(logger);
            }
        }
    }

    //**********************************************************
    private void receive_change_internal(Old_and_new_Path oanp)
    //**********************************************************
    {
        for ( Playlist playlist : playlists)
        {
            playlist.update_playlist_if_needed(oanp, logger);
        }
    }
}
