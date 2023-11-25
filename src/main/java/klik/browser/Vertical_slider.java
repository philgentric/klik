package klik.browser;

import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Slider;
import javafx.scene.layout.Pane;
import klik.browser.icons.Icon_manager;
import klik.properties.Static_application_properties;
import klik.util.Logger;
import klik.util.Stack_trace_getter;

//**********************************************************
public class Vertical_slider implements Landscape_height_listener
//**********************************************************
{
    public static final double half_slider_width = 20;
    final Slider the_Slider;
    Logger logger;
    Pane pane;

    //**********************************************************
    public Vertical_slider(Scene scene, Pane pane_, Icon_manager icon_manager, Logger logger_)
    //**********************************************************
    {
        logger = logger_;
        pane = pane_;


        // we set 1000 as pixel_height is not known at slider creation time
        double min = 0;
        double max = 1000;
        double val = 0;
        the_Slider = new Slider(min,max,val);//icon_manager.landscape_height);

        pane.getChildren().add(the_Slider);

        the_Slider.setOrientation(Orientation.VERTICAL);
        the_Slider.toFront();
        the_Slider.setVisible(true);
        adapt_slider_to_scene(scene, pane);

        the_Slider.valueProperty().addListener((ov, old_val_, new_val_) -> {
            double slider = new_val_.doubleValue();
            if ( Icon_manager.dbg_scroll)
                logger.log("slider property changed: OLD= "+ old_val_.doubleValue()+" ==> NEW= "+ slider);
            slider_moved_by_user(slider, icon_manager);
        });
    }

    //**********************************************************
    private void slider_moved_by_user(double slider, Icon_manager icon_manager)
    //**********************************************************
    {
        double pixel_height = get_pixel_height(icon_manager.get_landscape_height());
        double new_pixel = slider_to_pixels(slider, pixel_height);

        String reason = "";
        if ( Icon_manager.dbg_scroll)
        {
            reason = "(normalized+inverted with pixel_height= "+pixel_height+") slider = "+ slider +"  ==> " +new_pixel;
            logger.log(reason);
        }
        icon_manager.move_absolute(pane, new_pixel, "move absolute = slider moved! " +reason);
    }

    //**********************************************************
    private double get_pixel_height(double icon_manager)
    //**********************************************************
    {
        double pixel_height = icon_manager - pane.getHeight();
        if (pixel_height < 0)
        {
            // the virtual landscape height is smaller than ther pane's height
            pixel_height = pane.getHeight();
        }
        the_Slider.setMax(pixel_height); // when the pixel height is very large this is key to get good manual (mouse/trackpad) scroll accuracy
        return pixel_height;
    }

    //**********************************************************
    private double slider_to_pixels(double slider_value, double pixel_height)
    //**********************************************************
    {
        double tmp = slider_value/get_slider_max(); // normalize (0,1)
        boolean inverted = Static_application_properties.get_vertical_scroll_direction(logger);
        if (inverted)
        {
            tmp = 1.0 - tmp;
        }
        double new_pixel = pixel_height * tmp;
        if ( Icon_manager.dbg_scroll) logger.log("slider_to_pixels (with pixel_height="+pixel_height+") gives: "+slider_value+" ==> "+new_pixel);

        return new_pixel;
    }

    //**********************************************************
    private double pixels_to_slider(double pixels, double pixel_height)
    //**********************************************************
    {
        double tmp = pixels/pixel_height; // normalize (0,1)
        boolean inverted = Static_application_properties.get_vertical_scroll_direction(logger);
        if (inverted)
        {
            tmp = 1.0 - tmp;
        }
        double new_slider= tmp * get_slider_max();
        if ( Icon_manager.dbg_scroll) logger.log("pixels_to_slider (with pixel_height="+pixel_height+") gives: "+pixels+" ==> "+new_slider);
        return new_slider;
    }


    //**********************************************************
    public void transform_pixel_value(double pixels, Icon_manager icon_manager)
    //**********************************************************
    {
        double pixel_height = get_pixel_height(icon_manager.get_landscape_height());
        if ( Icon_manager.dbg_scroll) logger.log(Stack_trace_getter.get_stack_trace("\n\n\nset_absolute_value (with pixel_height="+pixel_height+")  pixels="+pixels+"\n"));
        the_Slider.setValue(pixels_to_slider(pixels,pixel_height ));

    }


    //**********************************************************
    public boolean scroll(double dy)
    //**********************************************************
    {
        boolean inverted = Static_application_properties.get_vertical_scroll_direction(logger);
        if (inverted)
        {
            if ( Icon_manager.dbg_scroll) logger.log("scroll is inverted="+dy+" ==> "+(-dy));
            dy = -dy;
        }
        else
        {
            if ( Icon_manager.dbg_scroll) logger.log("scroll is not inverted="+dy);
        }
        double old_val = the_Slider.getValue();
        double new_val = old_val - dy;

        if (Icon_manager.dbg_scroll) logger.log("slider old val:"+old_val+" - scroll="+dy+" SETTING SLIDER VAL ="+new_val);

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
    public void browsed_landscape_height_has_changed(double new_landscape_height, double current_vertical_offset)
    //**********************************************************
    {

        if ( Icon_manager.dbg_scroll)
            logger.log("browsed_landscape_height_has_changed = "+new_landscape_height);
        double pixel_height = get_pixel_height(new_landscape_height);
        the_Slider.setValue(pixels_to_slider(current_vertical_offset,pixel_height));
    }

    //**********************************************************
    public double get_slider_max()
    //**********************************************************
    {
        return the_Slider.getMax();
    }

    //**********************************************************
    public void adapt_slider_to_scene(Scene scene, Pane pane)
    //**********************************************************
    {
        the_Slider.setTranslateX(scene.getWidth() - half_slider_width);//vertical.getWidth());
        the_Slider.setTranslateY(half_slider_width);//vertical.getWidth());
        double height = pane.getHeight() - 200;
        if ( height < 300) height = 300;
        the_Slider.setPrefHeight(height);//2 * half_slider_width);


    }


}
