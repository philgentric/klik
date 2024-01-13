package klik.browser;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import klik.actor.Actor_engine;
import klik.look.Look_and_feel_manager;
import klik.util.Logger;
import klik.util.Threads;

import java.util.HashMap;

public class Meters_stage
{

    private static final int LEFT = 100;
    private static int w = 4;
    private static int DISPLAY_PIXEL_HEIGHT = 1000;
    private static int gap = 1;
    private static int min = 1;
    private static int how_many_rectangles = 100;
    private final static long HEARTH_BEAT = 40; //ms
    private final static int WIDTH = how_many_rectangles *(w+gap)+2*LEFT;
    private static final int[] values = new int[how_many_rectangles];
    private static final HashMap<Integer, Rectangle> rectangles = new HashMap<>();
    private static final Label max_label = new Label("0");
    private static int max_threads;

    //**********************************************************
    public static void show_stage(Logger logger)
    //**********************************************************
    {
        Stage stage = new Stage();
        VBox vbox = new VBox();
        {
            HBox hbox = new HBox();
            int x = LEFT;
            for (int i = 0; i < how_many_rectangles; i++) {
                Rectangle r = new Rectangle(x, DISPLAY_PIXEL_HEIGHT, w, min);
                r.setManaged(false);
                x += w + gap;
                r.setFill(Color.RED);
                rectangles.put(i, r);
                hbox.getChildren().add(r);
            }
            for (int threads = 0; threads <= 1000; threads += 100)
            {
                double ii = threads_to_pixels(threads);
                Rectangle r = new Rectangle(LEFT, ii, WIDTH, 1);
                r.setManaged(false);
                r.setFill(Look_and_feel_manager.get_instance().get_foreground_color());
                hbox.getChildren().add(r);

                Label l = new Label(""+threads);
                l.setManaged(false);
                l.setLayoutX(0);
                l.setLayoutY(ii);
                hbox.getChildren().add(l);
            }
            vbox.getChildren().add(hbox);
        }
        {
            HBox hbox = new HBox();
            Label l = new Label("Max= ");
            hbox.getChildren().add(l);
            hbox.getChildren().add(max_label);
            vbox.getChildren().add(hbox);
        }

        Scene scene = new Scene(vbox, Look_and_feel_manager.get_instance().get_background_color());
        stage.setScene(scene);
        double context_length = Math.round((double)HEARTH_BEAT*(double)how_many_rectangles/100.0)/10.0;
        stage.setTitle("Vroom... vroom, threads running in the last "+context_length+"seconds");
        stage.setMinWidth(WIDTH+2*LEFT);
        stage.setMinHeight(DISPLAY_PIXEL_HEIGHT+100);
        stage.show();

        logger.log("SHIW");

        Runnable r = new Runnable() {
            @Override
            public void run() {

                double y = 0;
                for(;;)
                {
                    try {
                        Thread.sleep(HEARTH_BEAT);
                    } catch (InterruptedException e) {
                        logger.log(""+e);
                    }
                    int x = Actor_engine.how_many_threads_are_in_flight(logger);

                    x += 1;// for this one!
                    update(x);
                    logger.log("threads: "+x);


                }

            }
        };
//        Actor_engine.execute(r,logger);
        Threads.execute(r,logger);

    }

    private static void update(int x) {


        for (int i =0;i< how_many_rectangles-1;i++)
        {
            values[i]=values[i+1];
        }
        values[how_many_rectangles-1] = x;

        Runnable rr = new Runnable() {
            @Override
            public void run() {
                if ( x > max_threads)
                {
                    max_threads = x;
                    max_label.setText(""+max_threads);
                }
                for (int i = 0; i < how_many_rectangles;i++)
                {
                    Rectangle r = rectangles.get(i);
                    int threads = values[i];
                    double yy = threads_to_pixels(threads);
                    r.setHeight(yy);
                    r.setY(DISPLAY_PIXEL_HEIGHT -yy);
                }
            }
        };
        Platform.runLater(rr);
    }

    private static double threads_to_pixels(int threads)
    {
        if ( threads == 0) return 0;
        //double v = 100*Math.log((double)(threads+1));
        double v = (double) threads;
        if ( v < 0) return 0;
        return v;
    }

}
