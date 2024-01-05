package klik.animated_gifs_from_videos;

import javafx.stage.Stage;
import klik.actor.Aborter;
import klik.actor.Message;
import klik.util.Logger;

import java.nio.file.Path;

public class Animated_gif_generation_message implements Message {
    public final Stage owner;
    public final Path video_path;
    public final Path destination_gif_full_path;
    public final int dur;
    public final int start;
    public final Aborter aborter;

    public final Logger logger;

    public Animated_gif_generation_message(Stage owner, Path video_path, Path destination_gif_full_path, int dur, int start, Aborter aborter_, Logger logger) {
        this.owner = owner;
        this.video_path = video_path;
        this.destination_gif_full_path = destination_gif_full_path;
        this.dur = dur;
        this.start = start;
        this.logger = logger;
        aborter = aborter_;
    }

    @Override
    public String to_string() {
        return "Animated_gif_generation_message for "+video_path;
    }

    @Override
    public Aborter get_aborter() {
        return aborter;
    }
}
