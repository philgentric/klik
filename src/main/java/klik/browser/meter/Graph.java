package klik.browser.meter;

import javafx.application.Platform;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import klik.look.Look_and_feel_manager;
import klik.util.Fx_batch_injector;
import klik.util.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

//**********************************************************
public class Graph
//**********************************************************
{
    String name;
    Runnable runnable;
    VBox vbox;
    double the_scale_max;
    Value_getter value_getter;
    Real_to_pixel real_to_pixel;
    Color color;
    Logger logger;
    int x_offset;
    static int how_many_rectangles = 100;
    private static double w = 0.8;
    private static double min = 1;
    private static double gap = 0;
    private static final double LEFT = 20;
    private static final double ADJUST = 20;
    private final static double WIDTH = how_many_rectangles *(w+gap)+LEFT;

    int the_max=0;
    int[] values = new int[how_many_rectangles];
    HashMap<Integer, Rectangle> rectangles_of_the_curve = new HashMap<>();
    List<Rectangle> scale_bars = new ArrayList<>();
    List<Text> scale_texts = new ArrayList<>();
    HBox the_hbox;

    //**********************************************************
    public Graph(String name_, double the_scale_max_, Value_getter value_getter_, Real_to_pixel real_to_pixel_, int x_offset_, Color color_, Logger logger_)
    //**********************************************************
    {
        name = name_;
        the_scale_max = the_scale_max_;
        value_getter = value_getter_;
        real_to_pixel = real_to_pixel_;
        color = color_;
        logger = logger_;
        x_offset = x_offset_;


        vbox = new VBox();
        {
            the_hbox = new HBox();
            double x = x_offset+LEFT+ADJUST;
            for (int i = 0; i < how_many_rectangles; i++) {
                Rectangle r = new Rectangle(x, Meters_stage.DISPLAY_PIXEL_HEIGHT, w, min);
                r.setManaged(false);
                x += w + gap;
                r.setFill(color);
                rectangles_of_the_curve.put(i, r);
                the_hbox.getChildren().add(r);
            }
            create_scale();

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
        yy += 20;
        Text max_text = new Text("xxxxxxxxxxx");
        {
            Text l = new Text("Max:");
            l.setManaged(false);
            l.setX(x_offset);
            l.setY(yy);
            vbox.getChildren().add(l);
            max_text.setManaged(false);
            max_text.setX(x_offset+50);
            max_text.setY(yy);
            vbox.getChildren().add(max_text);
        }
        Text last_text = new Text("yyyyyyyyyy");
        yy += 20;
        {
            HBox hbox = new HBox();
            Text l = new Text("Last:");
            l.setManaged(false);
            l.setX(x_offset);
            l.setY(yy);
            hbox.getChildren().add(l);
            last_text.setManaged(false);
            last_text.setX(x_offset+50);
            last_text.setY(yy);
            hbox.getChildren().add(last_text);
            vbox.getChildren().add(hbox);
        }
        runnable = new Runnable() {
            @Override
            public void run() {
                int x = value_getter.get_val();
                x += 1;// for this one!
                update(max_text,last_text,values, rectangles_of_the_curve, x,real_to_pixel);
            }
        };
    }

    //**********************************************************
    private void create_scale()
    //**********************************************************
    {
        double inc = the_scale_max/10.0;
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

        for (Rectangle r: scale_bars)
        {
            the_hbox.getChildren().remove(r);
        }
        for (Text t: scale_texts)
        {
            the_hbox.getChildren().remove(t);
        }


        for (double val = 0; val <= the_scale_max; val += inc)
        {
            double ii = real_to_pixel.val_to_pixel(val, the_scale_max);
            Rectangle horizontal_line = new Rectangle(x_offset +LEFT, Meters_stage.DISPLAY_PIXEL_HEIGHT-ii, WIDTH, 1);
            scale_bars.add(horizontal_line);
            horizontal_line.setManaged(false);
            horizontal_line.setFill(Look_and_feel_manager.get_instance().get_foreground_color());
            the_hbox.getChildren().add(horizontal_line);
            Text text = new Text(""+(int)val);
            scale_texts.add(text);
            text.setManaged(false);
            text.setX(x_offset +0);
            text.setY(Meters_stage.DISPLAY_PIXEL_HEIGHT-ii-5);
            the_hbox.getChildren().add(text);
        }
    }

    //**********************************************************
    private void update(Text max_text, Text last_text, int[] values, HashMap<Integer, Rectangle> rectangles, int value, Real_to_pixel real_to_pixel)
    //**********************************************************
    {
        for (int i =0;i< how_many_rectangles-1;i++)
        {
            values[i]=values[i+1];
        }
        values[how_many_rectangles-1] = value;

        Runnable rr = new Runnable() {
            @Override
            public void run() {
                if ( value > the_max)
                {
                    the_max = value;
                    max_text.setText(""+value);
                    if (the_max>the_scale_max)
                    {
                        the_scale_max = the_max;
                        create_scale();
                    }
                }
                last_text.setText(""+value);
                for (int i = 0; i < how_many_rectangles;i++)
                {
                    double pixels = real_to_pixel.val_to_pixel(values[i],the_scale_max);
                    Rectangle r = rectangles.get(i);
                    r.setHeight(pixels);
                    r.setY(Meters_stage.DISPLAY_PIXEL_HEIGHT -pixels);
                }
            }
        };
        Fx_batch_injector.inject(rr,logger);
    }

    public double get_width() {
        return WIDTH+2*LEFT;
    }
}
