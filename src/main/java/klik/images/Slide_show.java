package klik.images;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.util.Duration;
import klik.util.log.Logger;

//**********************************************************
public class Slide_show
//**********************************************************
{
    private static final boolean dbg = false;
    private Timeline slide_show_animation_timeline;
    long inter_frame_ms = 2000;
    private final Slide_show_slave image_stage;
    private final Logger logger;
    final boolean ultim_mode;

    //**********************************************************
    public Slide_show(Slide_show_slave image_stage_, boolean ultim_mode_, Logger logger_)
    //**********************************************************
    {
        image_stage = image_stage_;
        ultim_mode = ultim_mode_;
        logger = logger_;
        start_the_show();
    }
    //**********************************************************
    private void start_the_show()
    //**********************************************************
    {
        if (slide_show_animation_timeline != null)
        {
            slide_show_animation_timeline.stop();
        }
        EventHandler<ActionEvent> eventHandler = e ->
        {
            image_stage.change_image_relative(1, ultim_mode);
            //logger.log("====>>>>> eventHandler executed from slide show thread:"+ic.f.getName());
        };
        slide_show_animation_timeline = new Timeline();
        slide_show_animation_timeline.getKeyFrames().add(new KeyFrame(Duration.millis(inter_frame_ms), eventHandler));
        slide_show_animation_timeline.setCycleCount(Timeline.INDEFINITE);
        slide_show_animation_timeline.play();
        if (dbg) logger.log("slide show start " + inter_frame_ms);
        image_stage.set_title();

    }

    //**********************************************************
    public void stop_the_show()
    //**********************************************************
    {
        if (slide_show_animation_timeline != null)
        {
            slide_show_animation_timeline.stop();
            slide_show_animation_timeline = null;

            if (dbg) logger.log("slide show stop");
        }
    }

    //**********************************************************
    public void slow_down()
    //**********************************************************
    {
        inter_frame_ms *= 2;
        if ( inter_frame_ms > 60000) inter_frame_ms = 60000;
        start_the_show();

    }

    //**********************************************************
    public void hurry_up()
    //**********************************************************
    {
        inter_frame_ms /= 2;
        if ( inter_frame_ms < 100) inter_frame_ms = 100;
        start_the_show();

    }

}
