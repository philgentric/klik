package klik.audio_new;

import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.media.EqualizerBand;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Window;
import javafx.util.Duration;
import klik.actor.Aborter;
import klik.properties.Non_booleans_properties;
import klik.util.log.Logger;


public class Media_instance
{
    private static final boolean dbg = true;
    private static Media_instance instance = null;
    private final Logger logger;
    private MediaPlayer the_media_player;
    private Aborter aborter;


    public static Song_play_status play_this(String encoded, Zozo zozo, Window owner, Logger logger)
    {
        if ( instance == null) create_media_instance(logger);
        if (instance.aborter != null)
            instance.aborter.abort("another song to play");
        return instance.play_this(encoded, zozo, owner);
    }

    public static void play()
    {
        if ( instance == null) return;
        instance.play_internal();
    }

    public static MediaPlayer.Status get_status()
    {
        if ( instance == null) return null;
        return instance.get_status_internal();
    }

    public static void pause()
    {
        if ( instance == null) return;
        instance.pause_internal();
    }

    private void pause_internal()
    {
        if ( the_media_player == null) return;
        the_media_player.pause();
    }

    private MediaPlayer.Status get_status_internal()
    {
        if ( the_media_player == null) return null;
        return  the_media_player.getStatus();
    }

    private void play_internal() {
        if ( the_media_player == null) return;
        the_media_player.play();
    }


    private Media_instance(Logger logger)
    {
        this.logger = logger;
    }

    public static void add_current_time_listener(ChangeListener<Duration> x)
    {
        if ( instance == null) return;
        instance.add_current_time_listener_internal(x);
    }

    public static Duration get_stop_time() {
        if ( instance == null) return null;
        return instance.get_stop_time_internal();
    }

    private Duration get_stop_time_internal()
    {
        if ( the_media_player == null ) return null;
        return the_media_player.getStopTime();
    }

    private void add_current_time_listener_internal(ChangeListener<Duration> x)
    {
        if ( the_media_player == null) return;

        the_media_player.currentTimeProperty().addListener(x);
    }

    private static void create_media_instance(Logger logger)
    {
        if ( instance != null) return;
        instance= new Media_instance(logger);
    }

    public static void set_balance(double balance)
    {
        if ( instance == null) return;
        instance.set_balance_internal(balance);

    }

    public static boolean toggle_mute() {
        if ( instance == null) return true;
        return instance.toggle_mute_internal();
    }

    public static void set_volume(double volume)
    {
        if ( instance == null) return;
        instance.set_volume_internal(volume);
    }

    public static void seek(Duration target) {
        if ( instance == null) return;
        instance.seek_internal(target);
    }

    public static void stop()
    {
        if ( instance == null) return;
        instance.stop_internal();
    }

    public static ObservableList<EqualizerBand> get_bands() {
        if ( instance == null) return null;
        return instance.get_bands_internal();

    }

    private ObservableList<EqualizerBand> get_bands_internal()
    {
        if ( the_media_player == null) return null;
        return the_media_player.getAudioEqualizer().getBands();

    }

    private void stop_internal()
    {
        if ( the_media_player == null) return;
        the_media_player.stop();
    }

    public static void dispose()
    {
        if ( instance == null) return;
        instance.dispose_internal();

    }

    private void dispose_internal()
    {
        if ( the_media_player == null) return;
        the_media_player.dispose();
        the_media_player = null;
    }

    private void seek_internal(Duration target)
    {
        if ( the_media_player == null) return;
        the_media_player.seek(target);
    }

    private void set_volume_internal(double volume)
    {
        if ( the_media_player == null) return;
        the_media_player.setVolume(volume);
    }

    private boolean toggle_mute_internal()
    {
        if ( the_media_player == null) return true;

        if ( the_media_player.isMute())
        {
            the_media_player.setMute(false);
            return false;
        }
        else
        {
            the_media_player.setMute(true);
            return true;
        }
    }

    private void set_balance_internal(double balance)
    {
        if ( the_media_player != null) the_media_player.setBalance(balance);
    }


    private synchronized Song_play_status play_this(String song, Zozo zozo, Window owner)
    {
        aborter = new Aborter("song",logger);
        Media sound;
        try
        {
            sound = new Media(song);
        }
        catch (IllegalArgumentException e)
        {
            logger.log("invalid media NAME or PATH: "+song);
            logger.log(""+e);
            //playlist.remove(new_song);
            return Song_play_status.song_should_be_removed_from_playlist_as_path_is_invalid;
        }
        catch (MediaException e)
        {
            logger.log("\n\nInvalid media, unlisted: "+song+"\n\n");
            //playlist.remove(new_song);
            return Song_play_status.song_should_be_removed_from_playlist_as_path_is_invalid;
        }
        if ( aborter.should_abort()) return Song_play_status.aborted;
        if ( the_media_player !=null)
        {
            the_media_player.stop();
            the_media_player.dispose();
        }
        the_media_player = new MediaPlayer(sound);
        if ( aborter.should_abort()) return Song_play_status.aborted;
        the_media_player.setCycleCount(1);
        the_media_player.setOnStalled(() -> logger.log("\n\nWARNING player is stalling !!"));
        the_media_player.setOnReady(() -> zozo.on_player_ready());
        if ( aborter.should_abort()) return Song_play_status.aborted;
        the_media_player.setOnEndOfMedia(() -> zozo.on_end_of_media());
        if ( aborter.should_abort()) return Song_play_status.aborted;
        the_media_player.setOnPlaying(() -> {
            Integer current_time_s = get_current_time(song,  owner);
            if ( dbg) logger.log("seeking to "+current_time_s);
            Duration target = Duration.seconds(current_time_s);
            the_media_player.seek(target);
        });
        return Song_play_status.ok;
    }

    Integer get_current_time(String song, Window owner)
    {
        if ( song != null)
        {
            Integer current_time_s = Non_booleans_properties.get_current_time_in_song(owner,logger);
            if ( current_time_s != null) return current_time_s;
        }
        return Integer.valueOf(0);
    }

}
