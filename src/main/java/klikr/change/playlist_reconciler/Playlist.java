package klikr.change.playlist_reconciler;

import klikr.change.old_and_new.Old_and_new_Path;
import klikr.util.log.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

//**********************************************************
public class Playlist
//**********************************************************
{
    public final Logger logger;
    public HashSet<String> paths;
    private final List<Runnable> on_changes = new ArrayList<>();
    //**********************************************************
    public Playlist(Path playlist_file, Logger logger)
    //**********************************************************
    {
        this.logger = logger;
        try {
            List<String> local_paths = Files.readAllLines(playlist_file);
            paths = new HashSet<>(local_paths);
        } catch (IOException e) {
            logger.log(""+e);
            paths = new HashSet<>();
        }
    }
    //**********************************************************
    public void add_on_change(Runnable on_change)
    //**********************************************************
    {
        on_changes.add(on_change);
    }

    //**********************************************************
    void update_playlist_if_needed(Old_and_new_Path oanp, Logger logger)
    //**********************************************************
    {
        if (paths.contains(oanp.old_Path.toAbsolutePath().toString()))
        {
            paths.remove(oanp.old_Path.toAbsolutePath().toString());
            paths.add(oanp.new_Path.toAbsolutePath().toString());
            for ( Runnable r : on_changes) r.run();
        }
    }

}
