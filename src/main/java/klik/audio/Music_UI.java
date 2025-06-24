package klik.audio;

import java.util.List;

public interface Music_UI
{
    void add_song(Song song);
    void add_songs(List<Song> songs);

    void remove_song(Song b);
    void remove_all_songs();
    void scroll_to(String f);
    void set_status(String s);
    void stop_current_media();
    void set_title(String s);
    void set_total_duration(String s);

    void play_song_with_new_media_player(String new_song, Integer current_time_s);
    void set_playlist_name_display(String playlistName);
}
