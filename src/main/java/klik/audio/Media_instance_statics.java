package klik.audio;

import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.media.EqualizerBand;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Window;
import javafx.util.Duration;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.util.log.Logger;

import java.util.concurrent.ConcurrentLinkedQueue;


//**********************************************************
public class Media_instance_statics
//**********************************************************
{
    private static final boolean dbg = true;
    private static ConcurrentLinkedQueue<Aborter> aborters = new ConcurrentLinkedQueue<>();
    private static Media_instance instance;

    //**********************************************************
    public static void play_this(String encoded, Media_callbacks media_callbacks, boolean first_time, Window owner, Logger logger)
    //**********************************************************
    {
        for(;;)
        {
            Aborter aborter = aborters.poll();
            if ( aborter == null)
            {
                break;
            }

            //logger.log("aborting older player");;
            aborter.abort("another song to play");
        }

        //logger.log("no more aborters");
        Aborter aborter = new Aborter("playing "+encoded,logger);
        aborters.add(aborter);
        instance = new Media_instance(aborter,logger);
        Actor_engine.execute(() -> instance.play_this(encoded, media_callbacks,first_time,owner),logger);
    }

    //**********************************************************
    public static void play()
    //**********************************************************
    {
        if ( instance == null) return;
        instance.play_internal();
    }

    //**********************************************************
    public static void pause()
    //**********************************************************
    {
        if ( instance == null) return;
        instance.pause_internal();
    }

    //**********************************************************
    public static void add_current_time_listener(ChangeListener<Duration> x)
    //**********************************************************
    {
        if ( instance == null) return;
        instance.add_current_time_listener_internal(x);
    }

    //**********************************************************
    public static Duration get_stop_time()
    //**********************************************************
    {
        if ( instance == null) return null;
        return instance.get_stop_time_internal();
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

    public static void dispose()
    {
        if ( instance == null) return;
        instance.dispose_internal();

    }


    public static MediaPlayer.Status get_status() {
        if ( instance == null) return MediaPlayer.Status.DISPOSED;
        return instance.get_status_internal();

    }
}
