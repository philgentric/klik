//SOURCES ./Value_getter.java
//SOURCES ./Real_to_pixel.java
//SOURCES ./Graph_for_meters.java
package klik.browser.ram_and_threads_meter;

import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.Window;
import klik.actor.Actor_engine;
import klik.look.Look_and_feel_manager;
import klik.util.log.Logger;
import klik.util.execute.Scheduled_thread_pool;
import klik.util.ui.Menu_items;

import java.util.concurrent.TimeUnit;

//**********************************************************
public class RAM_and_threads_meters_stage
//**********************************************************
{
    public static final double DISPLAY_PIXEL_HEIGHT = 600;
    private final static long HEARTH_BEAT = 50; //ms
    private static Stage instance;

    //**********************************************************
    public static void show_stage(Window originator, Logger logger)
    //**********************************************************
    {
        if ( instance == null) instance = create_stage(originator,logger);
        else instance.show();
    }

    //**********************************************************
    private static Stage create_stage(Window originator,Logger logger)
    //**********************************************************
    {
        Stage stage = new Stage();
        //stage.initOwner(originator);
        stage.initOwner(null);

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
            Graph_for_meters graph = new Graph_for_meters("Threads",the_scale_max, value_getter, real_to_pixel, x_offset,Color.RED, stage,logger);
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
            Graph_for_meters graph = new Graph_for_meters("MB RAM", the_scale_max, value_getter, real_to_pixel, x_offset,Color.BLUE, stage,logger);

            hbox.getChildren().add(graph.vbox);
            Scheduled_thread_pool.execute(graph.runnable, HEARTH_BEAT, TimeUnit.MILLISECONDS);
        }


        Scene scene = new Scene(hbox, Look_and_feel_manager.get_instance(stage,logger).get_background_color());
        stage.setScene(scene);
        Look_and_feel_manager.set_scene_look(scene,stage, logger);
        double context_length = Math.round((double)HEARTH_BEAT*(double)(Graph_for_meters.how_many_rectangles)/100.0)/10.0;
        stage.setTitle("Last "+context_length+" seconds");
        stage.setMinWidth(width);
        stage.setMinHeight(DISPLAY_PIXEL_HEIGHT+100);
        stage.show();

        ContextMenu context_menu = new ContextMenu();
        Menu_items.add_menu_item("Call_GC",
                    event -> {
                System.gc();
                logger.log("Garbage collector was called");
            },context_menu,stage,logger);
        Menu_items.add_menu_item("List_threads",
                event -> {
                    Actor_engine.list_jobs(logger);
                },context_menu,stage,logger);
        scene.setOnMouseClicked(event -> {
            context_menu.show(stage, event.getScreenX(), event.getScreenY());
        });

        return stage;
    }


}
