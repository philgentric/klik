// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.util.execute.ram_and_threads_meter;

import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import klikr.look.Look_and_feel_manager;
import klikr.util.log.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//**********************************************************
public class Graph_for_histograms
//**********************************************************
{
    String name;
    VBox vbox;
    Color color;
    Logger logger;
    int x_offset;
    static int how_many_rectangles = 100;
    private static double w = 10;
    private static double gap = 0;
    private static final double LEFT = 20;
    private static final double ADJUST = 20;
    private final static double WIDTH = how_many_rectangles *(w+gap)+LEFT;

    //HashMap<Integer, Rectangle> rectangles_of_the_curve = new HashMap<>();
    List<Rectangle> v_scale_bars = new ArrayList<>();
    List<Text> v_scale_texts = new ArrayList<>();
    Rectangle min_x = new Rectangle();
    Text min_x_text = new Text();
    Rectangle max_x = new Rectangle();
    Text max_x_text = new Text();

    HBox the_hbox;

    //**********************************************************
    public Graph_for_histograms(
            String name_,
            List<Double> values,
            int x_offset_,
            Color color_,
            Stage stage,
            Logger logger_)
    //**********************************************************
    {
        name = name_;
        color = color_;
        logger = logger_;
        x_offset = x_offset_;

        Collections.sort(values);
        double min = values.get(0);
        logger.log("min similarity is "+min);
        double max = values.get(values.size()-1);

        int[] bin_count = new int[how_many_rectangles];

        for ( double val : values)
        {
            int i = val_to_bin(val, min, max,how_many_rectangles);
            bin_count[i] = bin_count[i] +1;
        }
        int largest_count = 0;
        for ( int i = 0 ; i < bin_count.length; i++)
        {
            if ( bin_count[i] > largest_count) largest_count = bin_count[i];
        }
        vbox = new VBox();
        {
            the_hbox = new HBox();
            double x = x_offset+LEFT+ADJUST;
            for (int i = 0; i < how_many_rectangles; i++)
            {
                double y = val_to_pix(largest_count, bin_count[i]);
                Rectangle r = new Rectangle(x, RAM_and_threads_meters_stage.DISPLAY_PIXEL_HEIGHT-y, w, y);
                r.setManaged(false);
                x += w + gap;
                r.setFill(color);
                //rectangles_of_the_curve.put(Integer.valueOf(i), r);
                the_hbox.getChildren().add(r);
            }
            create_v_scale(largest_count,stage);
            create_h_scale(min,max,largest_count,stage);

            vbox.getChildren().add(the_hbox);
        }
        double yy = 20;
        {
            Text l = new Text(name);
            l.setManaged(false);
            l.setX(x_offset);
            l.setY(yy);
            vbox.getChildren().add(l);
        }

    }

    private void create_h_scale(double min, double max, int largest_count, Stage stage)
    {

        the_hbox.getChildren().remove(min_x);
        double y = val_to_pix(largest_count, 0);
        min_x = new Rectangle(x_offset+LEFT+ADJUST, RAM_and_threads_meters_stage.DISPLAY_PIXEL_HEIGHT-y, 3, 20);
        min_x.setManaged(false);
        min_x.setFill(Look_and_feel_manager.get_instance(stage,logger).get_foreground_color());
        the_hbox.getChildren().add(min_x);


        the_hbox.getChildren().remove(min_x_text);
        min_x_text = new Text(""+min);
        min_x_text.setManaged(false);
        min_x_text.setX(x_offset+LEFT+ADJUST);
        min_x_text.setY(RAM_and_threads_meters_stage.DISPLAY_PIXEL_HEIGHT-10);
        the_hbox.getChildren().add(min_x_text);


        the_hbox.getChildren().remove(max_x);
        max_x = new Rectangle(x_offset+LEFT+ADJUST +how_many_rectangles *(w+gap), RAM_and_threads_meters_stage.DISPLAY_PIXEL_HEIGHT-y, 3, 20);
        max_x.setManaged(false);
        max_x.setFill(Look_and_feel_manager.get_instance(stage,logger).get_foreground_color());
        the_hbox.getChildren().add(max_x);


        the_hbox.getChildren().remove(max_x_text);
        max_x_text = new Text(""+max);
        max_x_text.setManaged(false);
        max_x_text.setX(x_offset+LEFT+ADJUST +how_many_rectangles *(w+gap));
        max_x_text.setY(RAM_and_threads_meters_stage.DISPLAY_PIXEL_HEIGHT-10);
        the_hbox.getChildren().add(max_x_text);


    }

    private static double val_to_pix(int largest_count, int val)
    {
        return ((double) val)* RAM_and_threads_meters_stage.DISPLAY_PIXEL_HEIGHT/ (double)largest_count;
    }

    private int val_to_bin(double val, double min, double max, int howManyRectangles)
    {
        double w = (max-min)/(double)howManyRectangles;
        double x = val-min;
        int returned = (int) (x / w);
        if ( returned == howManyRectangles) returned--;
        return returned;
    }


    //**********************************************************
    private void create_v_scale(int largest_count, Stage stage)
    //**********************************************************
    {
        double inc = largest_count/10.0;
        if ( inc < 1) inc = 1;
        else if ( inc < 5) inc = 2;
        else if ( inc < 10) inc = 5;
        else if ( inc < 20) inc = 10;
        else if ( inc < 50) inc = 20;
        else if ( inc < 100) inc = 50;
        else if ( inc < 200) inc = 100;
        else if ( inc < 500) inc = 200;
        else if ( inc < 1_000) inc = 500;
        else if ( inc < 2_000) inc = 1_000;
        else if ( inc < 5_000) inc = 2_000;
        else if ( inc < 10_000) inc = 5_000;

        for (Rectangle r: v_scale_bars)
        {
            the_hbox.getChildren().remove(r);
        }
        for (Text t: v_scale_texts)
        {
            the_hbox.getChildren().remove(t);
        }


        for (double val = 0; val <= (double) largest_count; val += inc)
        {
            double ii = val_to_pix(largest_count, (int) val);
            Rectangle horizontal_line = new Rectangle(x_offset +LEFT, RAM_and_threads_meters_stage.DISPLAY_PIXEL_HEIGHT-ii, WIDTH, 1);
            v_scale_bars.add(horizontal_line);
            horizontal_line.setManaged(false);
            horizontal_line.setFill(Look_and_feel_manager.get_instance(stage,logger).get_foreground_color());
            the_hbox.getChildren().add(horizontal_line);
            Text text = new Text(""+(int)val);
            v_scale_texts.add(text);
            text.setManaged(false);
            text.setX(x_offset +0);
            text.setY(RAM_and_threads_meters_stage.DISPLAY_PIXEL_HEIGHT-ii-5);
            the_hbox.getChildren().add(text);
        }
    }


}
