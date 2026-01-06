// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.browser.virtual_landscape;

import javafx.geometry.Orientation;
import javafx.scene.control.Slider;
import javafx.scene.layout.Pane;
import javafx.stage.Window;
import klikr.util.execute.actor.Aborter;
import klikr.util.log.Logger;

//**********************************************************
public class Vertical_slider implements Landscape_height_listener, Scroll_to_listener
//**********************************************************
{
    public static final boolean dbg = false;
    public static final double slider_width = 40;
    final Slider the_Slider;
    Logger logger;
    private final Aborter aborter;
    Pane the_big_display_pane;
    //public static boolean inverted = Booleans.get_boolean(Advanced_feature.Invert_vectical_scroll.name());

    //**********************************************************
    public Vertical_slider(
            Window stage,
            Pane the_big_display_pane,
            Virtual_landscape virtual_landscape,
            Logger logger)
    //**********************************************************
    {
        this.aborter = virtual_landscape.aborter;
        this.logger = logger;
        this.the_big_display_pane = the_big_display_pane;


        // we set 100 as pixel_height is not known at slider creation time
        double min = 0;
        double max = 100;
        double val = 0;
        the_Slider = new Slider(min,max,val);//Virtual_landscape.landscape_height);

        the_Slider.setOrientation(Orientation.VERTICAL);
        //the_Slider.toFront();
        //the_Slider.setVisible(true);

        adapt_slider_to_scene(stage);

        the_Slider.valueProperty().addListener((ov, old_val_, new_val_) -> {
            double slider = new_val_.doubleValue();
            if ( Virtual_landscape.scroll_dbg) logger.log("LISTENER: slider property changed: OLD= "+ old_val_.doubleValue()+" ==> NEW= "+ slider);
            slider_moved_by_user(slider, virtual_landscape);
        });
    }

    //**********************************************************
    private void slider_moved_by_user(double slider, Virtual_landscape virtual_landscape)
    //**********************************************************
    {
        double pixel_height = get_pixel_height(virtual_landscape.get_virtual_landscape_height());
        double new_pixel = slider_to_pixels(slider, pixel_height);

        if ( Virtual_landscape.scroll_dbg)
        {
            String reason = "(normalized+inverted with pixel_height= "+pixel_height+") slider = "+ slider +"  ==> " +new_pixel;
            logger.log(reason);
        }
        virtual_landscape.move_absolute(new_pixel, "VIRTUAL LANDSCAPE move absolute = slider moved! ");
    }

    //**********************************************************
    private double get_pixel_height(double virtual_landscape_height)
    //**********************************************************
    {
        if ( dbg) logger.log("virtual_landscape_height="+virtual_landscape_height);
        if ( dbg) logger.log("pane_height="+ the_big_display_pane.getHeight());

        double pixel_height = virtual_landscape_height - the_big_display_pane.getHeight();
        if (pixel_height < 0)
        {
            // the virtual landscape height is smaller than the pane's height
            pixel_height = the_big_display_pane.getHeight();
        }
        if ( dbg)
            logger.log("pixel_height (slider SETMAX to) ="+pixel_height);
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
            logger.log("❌ PANIC: get_slider_max() == 0");
        }
        else
        {
            fraction = slider_value / the_Slider.getMax(); // normalize (0,1)
        }
        //if (inverted)
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
            logger.log("❌ PANIC pixel_height == 0 max="+the_Slider.getMax()+" min="+the_Slider.getMin());

            pixel_height = 42;
        }
        double tmp = pixels/pixel_height; // normalize (0,1)
        //if (inverted)
        {
            tmp = 1.0 - tmp;
        }
        if ( Virtual_landscape.scroll_dbg) logger.log("pixels_to_slider tmp="+tmp);
        if ( Virtual_landscape.scroll_dbg) logger.log("Slider.getMax()="+the_Slider.getMax());

        double new_slider= tmp * the_Slider.getMax();
        if ( Virtual_landscape.scroll_dbg) logger.log("new_slider="+new_slider);
        if ( Virtual_landscape.scroll_dbg) logger.log("pixels_to_slider (with pixel_height="+pixel_height+") gives: "+pixels+" ==> "+new_slider);
        return new_slider;
    }



    //**********************************************************
    @Override // Scroll_to_listener
    public void perform_scroll_to(double y_offset_in_pixels, Virtual_landscape virtual_landscape)
    //**********************************************************
    {
        double pixel_height = get_pixel_height(virtual_landscape.get_virtual_landscape_height());
        if ( Virtual_landscape.scroll_dbg) logger.log("y_offset_in_pixels = "+y_offset_in_pixels);
        if ( Virtual_landscape.scroll_dbg) logger.log("pixel_height = "+pixel_height);

        double slider = pixels_to_slider(y_offset_in_pixels,pixel_height);
        if ( Virtual_landscape.scroll_dbg) logger.log("scroll_absolute NEW slider value = "+slider);
        the_Slider.adjustValue(slider);
    }


    //**********************************************************
    public boolean request_scroll_relative(double dy)
    //**********************************************************
    {
        //if (inverted)
        {
            if ( Virtual_landscape.scroll_dbg) logger.log("scroll is inverted="+dy+" ==> "+(-dy));
            dy = -dy;
        }
        /*else
        {
            if ( Virtual_landscape.scroll_dbg) logger.log("scroll is not inverted="+dy);
        }*/
        double old_val = the_Slider.getValue();
        if ( dbg) logger.log("scroll_relative old_val="+old_val);
        if ( dbg) logger.log("scroll_relative dy="+dy);
        double new_val = old_val - dy;

        if (Virtual_landscape.scroll_dbg) logger.log("slider old val:"+old_val+" - scroll="+dy+" SETTING SLIDER VAL ="+new_val);


        if ( Virtual_landscape.scroll_dbg)
            logger.log("scroll_relative new slider value (user has scrolled) = "+new_val);
        the_Slider.adjustValue(new_val);

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
    public void browsed_landscape_height_has_changed(double new_landscape_height, double current_vertical_offset)
    //**********************************************************
    {
        if ( Virtual_landscape.scroll_dbg)
            logger.log("browsed_landscape_height_has_changed = "+new_landscape_height);
        double pixel_height = get_pixel_height(new_landscape_height);
        double slider = pixels_to_slider(current_vertical_offset,pixel_height);
        if ( Virtual_landscape.scroll_dbg) logger.log("browsed_landscape_height_has_changed(), new slider value = "+slider);
        //the_Slider.adjustValue(slider);
    }




    //**********************************************************
    public void adapt_slider_to_scene(Window stage)
    //**********************************************************
    {
        if ( dbg) logger.log("adapt_slider_to_scene stage.getWidth()="+stage.getWidth()+" h = "+stage.getHeight());
        double height = stage.getHeight() - 100;
        the_Slider.setPrefHeight(height);//2 * half_slider_width);

    }


}
