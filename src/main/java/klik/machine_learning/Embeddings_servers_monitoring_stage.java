package klik.machine_learning;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import klik.look.Look_and_feel_manager;
import klik.util.log.Logger;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

//**********************************************************
public class Embeddings_servers_monitoring_stage
//**********************************************************
{
    Stage stage;
    //private final Map<String, List<Report>> uuid_to_record= new HashMap<>();
    private final Map<String, VBox> uuid_to_vbox= new HashMap<>();
    private HBox hits_hbox;
    private final Logger logger;
    private static final double DISPLAY_PIXEL_HEIGHT = 800;
    private static final double SMALL_WIDTH = 20;
    private static double SMALL_HEIGHT = 20;
    private Label rate_label;
    private Label duration_label;
    //private double LAST_X = 3;
    //**********************************************************
    public Embeddings_servers_monitoring_stage(Logger logger)
    //**********************************************************
    {
        this.logger = logger;
        stage = new Stage();
        stage.setTitle("Embeddings servers monitoring");
        stage.setMinWidth(1000);
        stage.setHeight(DISPLAY_PIXEL_HEIGHT);

        HBox hbox = new HBox();
        hits_hbox = new HBox();
        hbox.getChildren().add(hits_hbox);
        VBox vbox = new VBox();
        hbox.getChildren().add(vbox);
        rate_label = new Label("Rate: 0.0 embeddings/s");
        Look_and_feel_manager.set_label_look(rate_label,stage,logger);
        vbox.getChildren().add(rate_label);
        duration_label = new Label("Average processing duration: 0.0 ms");
        Look_and_feel_manager.set_label_look(duration_label,stage,logger);
        vbox.getChildren().add(duration_label);
        
        Scene scene = new Scene(hbox);
        stage.setScene(scene);
        stage.show();
    }

    //**********************************************************
    public void inject(Report report)
    //**********************************************************
    {
        Runnable r = () -> {
            inject_report(report);
        };
        Platform.runLater(r);

    }

    private long hit_count =0;
    private double total_duration   =0;
    private double max_hit_for_a_server = 0;
    //**********************************************************
    private void inject_report(Report report)
    //**********************************************************
    {
        hit_count++;
        total_duration += report.processing_time();
        double average_duration = total_duration / hit_count;
        duration_label.setText("Average embeddings processing time: " + String.format("%.2f", average_duration) + " ms");
        // Update rate
        double rate = calculate_rate();
        rate_label.setText(String.format("Rate: %.1f embeddings/s on last 10s", rate));



        String server_uuid = report.server_uuid();
        VBox vbox = uuid_to_vbox.get(server_uuid);
        if ( vbox == null )
        {
            logger.log("First record for server UUID: " + server_uuid);
            vbox = new VBox();
            hits_hbox.getChildren().add(vbox);
            uuid_to_vbox.put(server_uuid, vbox);
        }
        else
        {
            logger.log("Adding record for server UUID: " + server_uuid+ vbox.getChildren().size());
        }

        Rectangle r = new Rectangle(SMALL_WIDTH, SMALL_HEIGHT);
        r.setFill(get_random_color());
        vbox.getChildren().add(r);
        if ( vbox.getChildren().size() > max_hit_for_a_server )
        {
            max_hit_for_a_server = vbox.getChildren().size();
            if ( max_hit_for_a_server*SMALL_HEIGHT > DISPLAY_PIXEL_HEIGHT *0.8 )
            {
                // decrease the height of the rectangles
                SMALL_HEIGHT = 0.7*SMALL_HEIGHT;
                for (VBox box : uuid_to_vbox.values())
                {
                    for (int i = 0; i < box.getChildren().size(); i++)
                    {
                        Rectangle rect = (Rectangle) box.getChildren().get(i);
                        rect.setHeight(SMALL_HEIGHT);
                    }
                }
            }
        }
    }

    Random r = new Random();
    //**********************************************************
    private Paint get_random_color()
    //**********************************************************
    {
        double red = r.nextDouble();
        double green = r.nextDouble();
        double blue = r.nextDouble();
        return Color.color(red, green, blue);
    }

    private final ArrayDeque<Long> time_window = new ArrayDeque<>();
    private static final long WINDOW_SIZE_S = 10;
    private static final long WINDOW_SIZE_MS = WINDOW_SIZE_S*1000;

    //**********************************************************
    private double calculate_rate()
    //**********************************************************
    {
        long currentTime = System.currentTimeMillis();

        // Remove timestamps older than 10 seconds
        while (!time_window.isEmpty() && currentTime - time_window.peekFirst() > WINDOW_SIZE_MS) {
            time_window.removeFirst();
        }
        time_window.addLast(currentTime);
        return time_window.size() / WINDOW_SIZE_S;
    }
}
