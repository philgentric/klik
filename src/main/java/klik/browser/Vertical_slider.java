package klik.browser;

import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Slider;
import javafx.scene.layout.Pane;
import klik.browser.icons.Virtual_landscape;
import klik.properties.Static_application_properties;
import klik.util.log.Logger;

//**********************************************************
public class Vertical_slider implements Landscape_height_listener, Scroll_to_listener
//**********************************************************
{
    public static final boolean dbg = false;
    public static final double half_slider_width = 20;
    final Slider the_Slider;
    Logger logger;
    Pane pane;

    //**********************************************************
    public Vertical_slider(Scene scene, Pane pane_, double pane_height, Virtual_landscape icon_manager, Logger logger_)
    //**********************************************************
    {
        logger = logger_;
        pane = pane_;


        // we set 1000 as pixel_height is not known at slider creation time
        double min = 0;
        double max = 100000;
        double val = 0;
        the_Slider = new Slider(min,max,val);//icon_manager.landscape_height);

        pane.getChildren().add(the_Slider);

        the_Slider.setOrientation(Orientation.VERTICAL);
        the_Slider.toFront();
        the_Slider.setVisible(true);
        adapt_slider_to_scene(scene, pane_height);

        the_Slider.valueProperty().addListener((ov, old_val_, new_val_) -> {
            double slider = new_val_.doubleValue();
            //if ( Virtual_landscape.scroll_dbg)
                logger.log("slider property changed: OLD= "+ old_val_.doubleValue()+" ==> NEW= "+ slider);
            slider_moved_by_user(slider, icon_manager, pane_height);
        });
    }

    //**********************************************************
    private void slider_moved_by_user(double slider, Virtual_landscape icon_manager, double pane_height)
    //**********************************************************
    {
        double pixel_height = get_pixel_height(icon_manager.get_virtual_landscape_height(), pane_height);
        double new_pixel = slider_to_pixels(slider, pixel_height);

        String reason = "";
        if ( Virtual_landscape.scroll_dbg)
        {
            reason = "(normalized+inverted with pixel_height= "+pixel_height+") slider = "+ slider +"  ==> " +new_pixel;
            logger.log(reason);
        }
        icon_manager.move_absolute(pane, new_pixel, "move absolute = slider moved! " +reason);
    }

    //**********************************************************
    private double get_pixel_height(double virtual_landscape_height, double pane_height)
    //**********************************************************
    {
        logger.log("virtual_landscape_height="+virtual_landscape_height);
        pane_height = 0;
        logger.log("pane_height="+pane_height);

        double pixel_height = virtual_landscape_height - pane_height;
        if (pixel_height < 0)
        {
            // the virtual landscape height is smaller than the pane's height
            pixel_height = pane_height;
        }
        logger.log("pixel_height (slider set max to) ="+pixel_height);
        the_Slider.setMax(pixel_height); // when the pixel height is very large this is key to get good manual (mouse/trackpad) scroll accuracy
        return pixel_height;
    }

    //**********************************************************
    private double slider_to_pixels(double slider_value, double pixel_height)
    //**********************************************************
    {
        double fraction = 1;
        if (the_Slider.getMax() == 0)
        {
            logger.log("PANIC: get_slider_max() == 0");
        }
        else
        {
            fraction = slider_value / the_Slider.getMax(); // normalize (0,1)
        }
        boolean inverted = Static_application_properties.get_vertical_scroll_inverted(logger);
        if (inverted)
        {
            fraction = 1.0 - fraction;
        }
        if ( dbg) logger.log("pixel_height="+pixel_height);
        if ( dbg) logger.log("slider_to_pixels tmp="+fraction);
        double new_pixel = pixel_height * fraction;
        if ( dbg) logger.log("new_pixel="+new_pixel);
        if ( Virtual_landscape.scroll_dbg) logger.log("slider_to_pixels (with pixel_height="+pixel_height+") gives: "+slider_value+" ==> "+new_pixel);

        return new_pixel;
    }

    //**********************************************************
    private double pixels_to_slider(double pixels, double pixel_height)
    //**********************************************************
    {
        if ( pixel_height == 0)
        {
            logger.log("PANIC pixel_height == 0 max="+the_Slider.getMax()+" min="+the_Slider.getMin());

            pixel_height = 42;
        }
        double tmp = pixels/pixel_height; // normalize (0,1)
        boolean inverted = Static_application_properties.get_vertical_scroll_inverted(logger);
        if (inverted)
        {
            tmp = 1.0 - tmp;
        }
        if ( dbg) logger.log("pixels_to_slider tmp="+tmp);
        if ( dbg) logger.log("Slider.getMax()="+the_Slider.getMax());

        double new_slider= tmp * the_Slider.getMax();
        if ( dbg) logger.log("new_slider="+new_slider);
        if ( Virtual_landscape.scroll_dbg) logger.log("pixels_to_slider (with pixel_height="+pixel_height+") gives: "+pixels+" ==> "+new_slider);
        return new_slider;
    }

    //**********************************************************
    public void scroll_absolute(double pixels, Virtual_landscape icon_manager, double pane_height)
    //**********************************************************
    {
        double pixel_height = get_pixel_height(icon_manager.get_virtual_landscape_height(), pane_height);
        logger.log("pixels = "+pixels);
        logger.log("pixel_height = "+pixel_height);

        double slider = pixels_to_slider(pixels,pixel_height);
        logger.log("slider value = "+slider);
        the_Slider.setValue(slider);
    }


    //**********************************************************
    public boolean scroll_relative(double dy)
    //**********************************************************
    {
        boolean inverted = Static_application_properties.get_vertical_scroll_inverted(logger);
        if (inverted)
        {
            if ( Virtual_landscape.scroll_dbg) logger.log("scroll is inverted="+dy+" ==> "+(-dy));
            dy = -dy;
        }
        else
        {
            if ( Virtual_landscape.scroll_dbg) logger.log("scroll is not inverted="+dy);
        }
        double old_val = the_Slider.getValue();
        if ( dbg) logger.log("scroll_relative old_val="+old_val);
        if ( dbg) logger.log("scroll_relative dy="+dy);
        double new_val = old_val - dy;

        if (Virtual_landscape.scroll_dbg) logger.log("slider old val:"+old_val+" - scroll="+dy+" SETTING SLIDER VAL ="+new_val);


        if ( Virtual_landscape.scroll_dbg)
            logger.log("new slider value (user has scrolled) = "+new_val);
        the_Slider.setValue(new_val);

        if ( (new_val < the_Slider.getMin()) || (new_val > the_Slider.getMax())  )
        {
            // no change
            //if (Vertical_slider.dbg)  logger.log("NO scroll dy=" + dy+" min="+slider.getMin()+ " old="+old_val+ "new="+new_val+" max="+slider.getMax());
            return false;
        }
        else
        {
            //if (Vertical_slider.dbg) logger.log("scroll dy=" + dy+" min="+slider.getMin()+ " old="+old_val+ "new="+new_val+" max="+slider.getMax());
            return true;
        }

    }


    //**********************************************************
    @Override
    public void browsed_landscape_height_has_changed(double new_landscape_height, double pane_height, double current_vertical_offset)
    //**********************************************************
    {
        if ( Virtual_landscape.scroll_dbg)
            logger.log("browsed_landscape_height_has_changed = "+new_landscape_height);
        double pixel_height = get_pixel_height(new_landscape_height, pane_height);
        double slider = pixels_to_slider(current_vertical_offset,pixel_height);
        //logger.log("new slider value 3 = "+slider);
        the_Slider.setValue(slider);
    }




    //**********************************************************
    public void adapt_slider_to_scene(Scene scene, double pane_height)
    //**********************************************************
    {
        //logger.log("adapt_slider_to_scene scene.getWidth() "+scene.getWidth());
        the_Slider.setTranslateX(scene.getWidth() - half_slider_width);//vertical.getWidth());
        the_Slider.setTranslateY(half_slider_width);//vertical.getWidth());
        double height = pane_height - 200;
        if ( height < 300) height = 300;
        the_Slider.setPrefHeight(height);//2 * half_slider_width);


    }

    @Override // Scroll_to_listener
    public void perform_scroll_to(double y_offset, Virtual_landscape icon_manager, double pane_height)
    {

        //logger.log("got a scroll_to  target y offset = "+y_offset);

        scroll_absolute(y_offset, icon_manager, pane_height);
    }


}
