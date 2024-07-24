package klik.browser;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.util.Duration;
import klik.actor.Aborter;
import klik.util.log.Logger;

//**********************************************************
public class Scan_show
//**********************************************************
{
    private static final boolean dbg = false;
    public static final double FAC = 1.3;
    public final double MAX_SPEED;
    public final double MIN_SPEED;
    private Timeline scan_show_animation_timeline;
    long inter_frame_ms = 3;
    private final Scan_show_slave scan_show_slave;
    private final Logger logger;
    private final Aborter aborter;
    private double dy;

    //**********************************************************
    Scan_show(Scan_show_slave scan_show_slave_, Vertical_slider slider, Aborter aborter, Logger logger_)
    //**********************************************************
    {
        this.aborter = aborter;
        scan_show_slave = scan_show_slave_;
        logger = logger_;
        int number_of_rows = scan_show_slave.how_many_rows();
        double slider_unit_per_row = slider.get_slider_max()/(double)number_of_rows;
        if ( dbg) logger.log("slider_unit_per_row="+slider_unit_per_row);
        MIN_SPEED = slider_unit_per_row/3000.0;
        if ( dbg) logger.log("MIN_SPEED="+MIN_SPEED);
        MAX_SPEED = slider_unit_per_row/30.0;
        if ( dbg) logger.log("MAX_SPEED="+MAX_SPEED);
        dy = slider_unit_per_row/500.0;
        start_the_show();
    }

    //**********************************************************
    public void invert_scan_direction()
    //**********************************************************
    {
        dy = -dy; // invert scan !
    }
    //**********************************************************
    private void start_the_show()
    //**********************************************************
    {
        if ( aborter.should_abort()) return;
        if (scan_show_animation_timeline != null)
        {
            scan_show_animation_timeline.stop();
        }
        EventHandler<ActionEvent> eventHandler = e ->
        {
            if ( aborter.should_abort()) return;
            if ( scan_show_slave.scroll_a_bit(dy))
            {
                if ( dbg) logger.log("scan scroll done = "+dy);
            }
            else
            {
                if ( dbg) logger.log("inverting scan direction !");
                invert_scan_direction();
            }
        };
        scan_show_animation_timeline = new Timeline();
        scan_show_animation_timeline.getKeyFrames().add(new KeyFrame(Duration.millis(inter_frame_ms), eventHandler));
        scan_show_animation_timeline.setCycleCount(Timeline.INDEFINITE);
        scan_show_animation_timeline.play();
        if ( dbg) logger.log("scan show start " + inter_frame_ms);

    }

    //**********************************************************
    void stop_the_show()
    //**********************************************************
    {
        if (scan_show_animation_timeline != null)
        {
            scan_show_animation_timeline.stop();
            scan_show_animation_timeline = null;

            if ( dbg) logger.log("scan show stop");
        }
    }


    //**********************************************************
    public void slow_down()
    //**********************************************************
    {
        dy /= FAC;
        if (dy > 0)
        {
            if (dy < MIN_SPEED) dy = MIN_SPEED;
        }
        else
        {
            if (dy > -MIN_SPEED) dy = -MIN_SPEED;
        }
        if ( dbg) logger.log("new scan show speed:"+dy);
        start_the_show();

    }

    //**********************************************************
    public void hurry_up()
    //**********************************************************
    {
        dy *= FAC;
        if (dy > 0)
        {
            if (dy > MAX_SPEED) dy = MAX_SPEED;
        }
        else
        {
            if (dy < -MAX_SPEED) dy = -MAX_SPEED;
        }
        if ( dbg) logger.log("new scan show speed:"+dy);
        start_the_show();
    }

    public int get_speed()
    {
        return (int)(dy*100.0);
    }
}
