// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.util.animated_gifs;

import javafx.stage.Window;
import klik.util.execute.actor.Aborter;
import klik.util.execute.actor.Message;
import klik.util.log.Logger;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

//**********************************************************
public class Animated_gif_generation_message implements Message
//**********************************************************
{
    public final Window originator;
    public final Path video_path;
    public final int height;
    public final int fps;
    public final Path destination_gif_full_path;
    public final int dur;
    public final int start;
    public final Aborter aborter;
    public final AtomicBoolean abort_reported;

    public final Logger logger;

    //**********************************************************
    public Animated_gif_generation_message(Window owner, Path video_path, int height, int fps, Path destination_gif_full_path, int dur, int start,
                                           Aborter aborter, AtomicBoolean abort_reported, Logger logger)
    //**********************************************************
    {
        this.originator = owner;
        this.video_path = video_path;
        this.height = height;
        this.fps = fps;
        this.destination_gif_full_path = destination_gif_full_path;
        this.dur = dur;
        this.start = start;
        this.logger = logger;
        this.aborter = aborter;
        this.abort_reported = abort_reported;
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
