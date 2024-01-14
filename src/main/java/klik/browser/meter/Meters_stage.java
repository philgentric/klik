package klik.browser.meter;

import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import klik.actor.Actor_engine;
import klik.look.Look_and_feel_manager;
import klik.util.Logger;
import klik.util.Scheduled_thread_pool;

import java.util.concurrent.TimeUnit;

//**********************************************************
public class Meters_stage
//**********************************************************
{

    public static final double DISPLAY_PIXEL_HEIGHT = 600;
    private final static long HEARTH_BEAT = 50; //ms

    //**********************************************************
    public static void show_stage(Logger logger)
    //**********************************************************
    {
        Stage stage = new Stage();
        HBox hbox = new HBox();
        int width = 0;
        int x_offset = 5;
        {
            Value_getter value_getter = new Value_getter() {
                @Override
                public int get_val() {
                    return Actor_engine.how_many_threads_are_in_flight(logger);
                }
            };
            double the_scale_max = 10;
            Real_to_pixel real_to_pixel = new Real_to_pixel() {
                @Override
                public double val_to_pixel(double val,double max_val) {
                    return  0.8*DISPLAY_PIXEL_HEIGHT*val/max_val;
                }
            };
            Graph graph = new Graph("Threads",the_scale_max, value_getter, real_to_pixel, x_offset,Color.RED, logger);
            hbox.getChildren().add(graph.vbox);
            width+= graph.get_width();
            Scheduled_thread_pool.execute(graph.runnable, HEARTH_BEAT, TimeUnit.MILLISECONDS);

        }
        {
            Value_getter value_getter = new Value_getter() {
                @Override
                public int get_val() {
                    return (int)((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())/1_000_000.0);
                }
            };
            double the_scale_max = 10;
            Real_to_pixel real_to_pixel = new Real_to_pixel() {
                @Override
                public double val_to_pixel(double val,double max_val) {
                    return  0.8*DISPLAY_PIXEL_HEIGHT*val/max_val;
                }
            };
            x_offset += width;
            Graph graph = new Graph("MB RAM", the_scale_max, value_getter, real_to_pixel, x_offset,Color.BLUE, logger);

            hbox.getChildren().add(graph.vbox);
            Scheduled_thread_pool.execute(graph.runnable, HEARTH_BEAT, TimeUnit.MILLISECONDS);
        }
        Scene scene = new Scene(hbox, Look_and_feel_manager.get_instance().get_background_color());
        stage.setScene(scene);
        double context_length = Math.round((double)HEARTH_BEAT*(double)(Graph.how_many_rectangles)/100.0)/10.0;
        stage.setTitle("Last "+context_length+" seconds");
        stage.setMinWidth(width);
        stage.setMinHeight(DISPLAY_PIXEL_HEIGHT+100);
        stage.show();
    }


}
