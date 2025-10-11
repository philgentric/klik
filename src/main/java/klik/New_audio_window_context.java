package klik;

import javafx.geometry.Rectangle2D;
import javafx.stage.Window;
import klik.audio.Song_playlist_browser;
import klik.browser.virtual_landscape.Browsing_caches;
import klik.browser.virtual_landscape.Shutdown_target;
import klik.util.log.Logger;

import java.nio.file.Path;

//**********************************************************
public class New_audio_window_context
//**********************************************************
{
    private static final boolean dbg = false;
    public final Path play_list_file_path; // this can be an absolute folder path or a image play list FILE  path
    public final Rectangle2D rectangle;
    public final Shutdown_target shutdown_target; // if null, there is no previous guy to shutdown
    public final Window originator;

    //**********************************************************
    private New_audio_window_context(
            Path play_list_file_path,
            Rectangle2D rectangle,
            Shutdown_target shutdown_target,
            Window originator)
    //**********************************************************
    {
        this.rectangle = rectangle;
        this.shutdown_target = shutdown_target;
        this.play_list_file_path = play_list_file_path;
        this.originator = originator;
    }

    //**********************************************************
    private String to_string()
    //**********************************************************
    {
        String returned = "shutdown_target="+shutdown_target;
        return returned;
    }


    //**********************************************************
    public static Window_provider additional_no_past(Path play_list_file_path, Window originator, Logger logger)
    //**********************************************************
    {
        New_audio_window_context context = new New_audio_window_context(
                play_list_file_path,
                null,
                null,
                originator);
        if ( dbg) logger.log(("\nadditional_no_past\n"+ context.to_string() ));
        return new Song_playlist_browser(context, logger);
    }

    //**********************************************************
    public static void replace_same_folder(
            Shutdown_target shutdown_target,
            Path play_list_file_path,
            Path top_left,
            Window originator,
            Logger logger)
    //**********************************************************
    {
        Browsing_caches.scroll_position_cache_write(play_list_file_path,top_left);

        Rectangle2D rectangle = new Rectangle2D(originator.getX(),originator.getY(),originator.getWidth(),originator.getHeight());
        New_audio_window_context context =  new New_audio_window_context(
                play_list_file_path,
                rectangle,
                shutdown_target,
                originator);
        if ( dbg) logger.log(("\nreplace_same_folder\n"+ context.to_string() ));
        new Song_playlist_browser(context, logger);
    }

    //**********************************************************
    public static void replace_different_folder(
            Shutdown_target shutdown_target,
            Path play_list_file_path,
            Window originator,
            Logger logger)
    //**********************************************************
    {
        if ( dbg) logger.log("replace_different_folder new path: " + play_list_file_path.toAbsolutePath());
        Rectangle2D rectangle = new Rectangle2D(originator.getX(),originator.getY(),originator.getWidth(),originator.getHeight());
        New_audio_window_context context =  new New_audio_window_context(
                play_list_file_path,
                rectangle,
                shutdown_target,
                originator);
        if ( dbg) logger.log(("\nreplace_different_folder\n"+ context.to_string() ));
        new Song_playlist_browser(context, logger);

    }

}
