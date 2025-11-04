// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.audio;

import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.media.EqualizerBand;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Window;
import javafx.util.Duration;
import klik.util.execute.actor.Aborter;
import klik.properties.Non_booleans_properties;
import klik.util.log.Logger;


//**********************************************************
public class Media_instance
//**********************************************************
{
    private final Logger logger;
    private MediaPlayer the_media_player;
    private final Aborter aborter;
    ChangeListener<Duration> the_change_listener;


    //**********************************************************
    Media_instance(Aborter aborter, Logger logger)
    //**********************************************************
    {
        this.logger = logger;
        this.aborter = aborter;
    }


    //**********************************************************
    void pause_internal()
    //**********************************************************
    {
        if ( the_media_player == null) return;
        the_media_player.pause();
    }

    //**********************************************************
    MediaPlayer.Status get_status_internal()
    //**********************************************************
    {
        if ( the_media_player == null) return null;
        return  the_media_player.getStatus();
    }

    //**********************************************************
    void play_internal()
    //**********************************************************
    {
        if ( the_media_player == null) return;
        the_media_player.play();
    }


    //**********************************************************
    Duration get_stop_time_internal()
    //**********************************************************
    {
        if ( the_media_player == null ) return null;
        return the_media_player.getStopTime();
    }

    //**********************************************************
    void add_current_time_listener_internal(ChangeListener<Duration> change_listener)
    //**********************************************************
    {
        if ( the_media_player == null) return;

        the_change_listener = change_listener;
        the_media_player.currentTimeProperty().addListener(the_change_listener);
    }


    //**********************************************************
    ObservableList<EqualizerBand> get_bands_internal()
    //**********************************************************
    {
        if ( the_media_player == null) return null;
        return the_media_player.getAudioEqualizer().getBands();

    }

    //**********************************************************
    void stop_internal()
    //**********************************************************
    {
        if ( the_media_player == null) return;
        //logger.log("Media_instance stop_internal");
        if ( the_change_listener != null) the_media_player.currentTimeProperty().removeListener(the_change_listener);

        the_media_player.stop();
    }

    //**********************************************************
    void dispose_internal()
    //**********************************************************
    {
        if ( the_media_player == null) return;
        //logger.log("Media_instance dispose_internal");
        the_media_player.dispose();
        the_media_player = null;
    }

    //**********************************************************
    void seek_internal(Duration target)
    //**********************************************************
    {
        if ( the_media_player == null) return;
        the_media_player.seek(target);
    }

    //**********************************************************
    void set_volume_internal(double volume)
    //**********************************************************
    {
        if ( the_media_player == null) return;
        the_media_player.setVolume(volume);
    }

    //**********************************************************
    boolean toggle_mute_internal()
    //**********************************************************
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

    //**********************************************************
    void set_balance_internal(double balance)
    //**********************************************************
    {
        if ( the_media_player != null) the_media_player.setBalance(balance);
    }


    //**********************************************************
    Song_play_status play_this(String song, Media_callbacks media_callbacks, boolean first_time, Window owner)
    //**********************************************************
    {
        //logger.log("\n\nplay_this : "+song);
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
        try
        {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if ( aborter.should_abort())
        {
            logger.log("player aborted before previous MediaPlayer dispose "+ aborter.reason());
            return Song_play_status.aborted;
        }
        if ( the_media_player !=null)
        {
            the_media_player.stop();
            the_media_player.dispose();
        }
        if ( aborter.should_abort())
        {
            logger.log("player aborted after previous MediaPlayer dispose "+ aborter.reason());
            return Song_play_status.aborted;
        }
        the_media_player = new MediaPlayer(sound);
        if ( aborter.should_abort())
        {
            dispose_internal();
            logger.log("❗ player aborted after new MediaPlayer "+ aborter.reason());
            return Song_play_status.aborted;
        }
        the_media_player.setCycleCount(1);
        the_media_player.setOnStalled(() -> logger.log("\n\n❗ WARNING player is stalling !!"));
        the_media_player.setOnReady(() -> {
            if ( aborter.should_abort())
            {
                dispose_internal();
                logger.log("❗ player aborted in setOnReady "+ aborter.reason());
                return;
            }
            media_callbacks.on_player_ready();
        });
        the_media_player.setOnEndOfMedia(() -> media_callbacks.on_end_of_media());
        the_media_player.setOnPlaying(() -> {
            if ( aborter.should_abort())
            {
                dispose_internal();
                logger.log("❗ player aborted in setOnPlaying "+ aborter.reason());
                return;
            }
            if ( first_time) {
                Integer current_time_s = get_current_time(song, owner);
                //if ( dbg)
                logger.log("✅ seeking to " + current_time_s);
                Duration target = Duration.seconds(current_time_s);
                the_media_player.seek(target);
            }
        });
        return Song_play_status.ok;
    }

    //**********************************************************
    Integer get_current_time(String song, Window owner)
    //**********************************************************
    {
        if ( song != null)
        {
            Integer current_time_s = Non_booleans_properties.get_current_time_in_song(owner,logger);
            if ( current_time_s != null) return current_time_s;
        }
        return Integer.valueOf(0);
    }

}
