package klik.audio;

import javafx.scene.control.Button;

import java.io.File;

public interface Song_adding_receiver
{
    void add(Button song);

    void remove(Button b);

    void scroll_to(File f);

    void set_status(String s);

    void clean_up();

    void set_title(String s);

    void play_song_with_new_media_player(File new_song, Integer current_time_s);

    void set_playlist_name_display(String playlistName);
}
