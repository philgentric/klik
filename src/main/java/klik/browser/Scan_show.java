package klik.browser;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.util.Duration;
import klik.util.Logger;

//**********************************************************
public class Scan_show
//**********************************************************
{
    private Timeline scan_show_animation_timeline;
    long inter_frame_ms = 10;
    private Scan_show_slave browser;
    private Logger logger;
    private double dy = 1;
    private boolean can_check_y;
    private double last_y = -1;

    //**********************************************************
    Scan_show(Scan_show_slave browser_, Logger logger_)
    //**********************************************************
    {
        browser = browser_;
        logger = logger_;
        can_check_y = false;
        start_the_show();
    }

    //**********************************************************
    private void start_the_show()
    //**********************************************************
    {
        if (scan_show_animation_timeline != null)
        {
            scan_show_animation_timeline.stop();
        }
        EventHandler<ActionEvent> eventHandler = e ->
        {
            browser.scroll_a_bit(dy);
            double y = ((Browser) browser).icon_manager.get_y_offset();
            //logger.log("====>>>>> eventHandler executed from scan show thread:"+y+" vs "+last_y);

            if (can_check_y)
            {
                if (y == last_y)
                {
                    logger.log("inverting scan direction !");
                    dy = -dy; // invert scan !
                    can_check_y = false;
                }
            }
            else
            {
                can_check_y = true; // for next time
            }
            last_y = y;

        };
        scan_show_animation_timeline = new Timeline();
        scan_show_animation_timeline.getKeyFrames().add(new KeyFrame(Duration.millis(inter_frame_ms), eventHandler));
        scan_show_animation_timeline.setCycleCount(Timeline.INDEFINITE);
        scan_show_animation_timeline.play();
        logger.log("scan show start " + inter_frame_ms);

    }

    //**********************************************************
    void stop_the_show()
    //**********************************************************
    {
        if (scan_show_animation_timeline != null)
        {
            scan_show_animation_timeline.stop();
            scan_show_animation_timeline = null;

            logger.log("scan show stop");
        }
    }


    //**********************************************************
    public void slow_down()
    //**********************************************************
    {
        dy /= 2;
        if (dy > 0)
        {
            if (dy < 0.1) dy = 0.1;
        }
        else
        {
            if (dy > -0.1) dy = -0.1;
        }

        start_the_show();

    }

    //**********************************************************
    public void hurry_up()
    //**********************************************************
    {
        dy *= 2;
        if (dy > 0)
        {
            if (dy > 100) dy = 100;
        }
        else
        {
            if (dy < -100) dy = -100;
        }
        start_the_show();
    }
}
