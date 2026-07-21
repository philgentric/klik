// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.machine_learning.monitoring;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.Window;
import klikr.machine_learning.*;
import klikr.util.Check_remaining_RAM;
import klikr.util.Shared_services;
import klikr.look.Look_and_feel_manager;
import klikr.util.Simple_json_parser;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.*;
import java.util.*;

//**********************************************************
public class ML_servers_monitor //implements AutoCloseable
//**********************************************************
{
    private static final boolean dbg = false;
    private static final boolean ultra_dbg = false; // char level debug!
    private static volatile ML_servers_monitor instance;
    private static  double SMALL_WIDTH = 20;
    private static final double SMALL_HEIGHT = 20;
    private static final double DISPLAY_PIXEL_WIDTH = 800;

    private Stage stage;
    private VBox the_vbox;
    private Logger logger;
    private Aborter aborter;
    private static final int DEFAULT_sleep_between_network_scans_ms = 3_000;
    private static int sleep_between_network_scans_ms = DEFAULT_sleep_between_network_scans_ms;
    private static int limit;

    // UI items
    private final Map<String,HBox> uuid_to_big_hbox = new HashMap<>();
    private final Map<String, HBox> uuid_to_small_hbox = new HashMap<>();


    //**********************************************************
   public static void make_faster_network_scans()
   //**********************************************************
   {
       sleep_between_network_scans_ms = 1000;
       limit = 1800; // the fast scan will last 1800*100ms = 3 minutes
   }

    // used when we start servers
    //**********************************************************
    public static void start_ML_servers_monitor(Window owner, Logger logger)
    //**********************************************************
    {
        if (Check_remaining_RAM.low_memory.get()) return;
        if (instance == null)
        {
            synchronized (ML_servers_monitor.class)
            {
                if (instance == null)
                {
                    instance = new ML_servers_monitor();
                    Platform.runLater(()->instance.init(owner,logger));
                }
            }
        }
    }

    //**********************************************************
    private void init(Window owner, Logger logger_)
    //**********************************************************
    {
        UDP_traffic_monitor.set_monitoring_reception_frame(this);
        aborter = Shared_services.aborter();
        logger = logger_;

        stage = new Stage();
        stage.setTitle("Live ML servers");
        stage.setMinWidth(800);
        stage.setMinHeight(800);
        the_vbox = new VBox();
        ScrollPane sp = new ScrollPane(the_vbox);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        Scene scene = new Scene(sp);
        stage.setScene(scene);
        stage.show();

        Actor_engine.execute(()-> for_ever_ask_servers_using_http_health(owner),"ML servers health check",logger);
    }


    //**********************************************************
    public static void refresh_remove(ML_server ml_server, Window owner, Logger logger)
    //**********************************************************
    {
        if ( instance == null ) return;
        Platform.runLater(()-> instance.remove_one_line(ml_server));
    }


    //**********************************************************
    synchronized private void remove_one_line(ML_server ml_server)
    //**********************************************************
    {
        HBox big_box = uuid_to_big_hbox.get(ml_server.uuid());
        if ( big_box != null)
        {
            uuid_to_big_hbox.remove(ml_server.uuid());
            the_vbox.getChildren().remove(big_box);
        }
    }

    //**********************************************************
    public static void refresh_add(ML_server ml_server, Window owner, Logger logger)
    //**********************************************************
    {
        if ( instance == null ) return;
        Platform.runLater(()->instance.make_one_line(ml_server));
    }

    //**********************************************************
    synchronized private void make_one_line(ML_server ml_server)
    //**********************************************************
    {
        HBox big_box = uuid_to_big_hbox.get(ml_server.uuid());
        if ( big_box != null)
        {
            // ignore duplicates coming from ML_registry_discovery
            return;
        }

        HBox big_hbox = new HBox();
        uuid_to_big_hbox.put(ml_server.uuid(),big_hbox);
        the_vbox.getChildren().add(big_hbox);

        //big_hbox.setSpacing(10);
        {
            Button port_button = new Button(""+ml_server.port());
            Look_and_feel_manager.set_region_look(port_button, true, stage, logger);
            big_hbox.getChildren().add(port_button);
            port_button.setOnAction(event -> {
                Stage stage = new Stage();
                TextArea ta = new TextArea();
                Look_and_feel_manager.set_region_look(ta, stage, logger);
                Scene scene = new Scene(ta);
                ta.setEditable(false);
                ta.setWrapText(true);
                StringBuilder sb = new StringBuilder();
                if (is_server_alive_heavy_version(ml_server,sb,logger))
                {
                    ta.setText(sb.toString());
                }
                else
                {
                    ta.setText("HTTP failed for some reason?\nYou can try this in a terminal:\ncurl http://127.0.0.1:"+ml_server.port()+"/health\n"+ml_server.to_string());
                }
                stage.setScene(scene);
                stage.setTitle("HTTP health for ML server @ "+ml_server.port());
                stage.setMinWidth(400);
                stage.setMinHeight(400);
                stage.show();
            });
        }

        {
            Label l = new Label(ml_server.type());
            Look_and_feel_manager.set_region_look(l, stage, logger);
            big_hbox.getChildren().add(l);
        }


        {
            HBox small_hbox = new HBox();
            big_hbox.getChildren().add(small_hbox);
            uuid_to_small_hbox.put(ml_server.uuid(), small_hbox);
        }
        {
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Look_and_feel_manager.set_region_look(spacer, stage, logger);
            big_hbox.getChildren().add(spacer);
        }


    }



    //**********************************************************
    public void for_ever_ask_servers_using_http_health(Window owner)
    //**********************************************************
    {
        for(;;)
        {
            try {
                if ( dbg) logger.log("for_ever_ask_servers_using_http_health .. updating");

                if( aborter.should_abort() ) return;

                read_registry_files_and_update_UI(owner);
                Thread.sleep(sleep_between_network_scans_ms);
                if ( sleep_between_network_scans_ms < DEFAULT_sleep_between_network_scans_ms)
                {
                    limit--;
                    if ( limit < 0 )
                    {
                        // reset to default
                        sleep_between_network_scans_ms=DEFAULT_sleep_between_network_scans_ms;
                    }
                }
            }
            catch (InterruptedException e)
            {
                logger.log("for_ever_ask_servers_using_http_health "+e);
            }
        }
    }


    //**********************************************************
    private void read_registry_files_and_update_UI(Window owner)
    //**********************************************************
    {
        Map<String, List<ML_server>> server_ports = ML_registry.scan_all_registry(owner, logger);
        if ( server_ports == null )
        {
            // should not happen
            logger.log("???? Server ports not found");
            return;
        }
        for ( Map.Entry<String, List<ML_server>> entry : server_ports.entrySet() )
        {
            List<ML_server> list = entry.getValue();
            if ( dbg) logger.log("server type :->" + entry.getKey()+ "<- "+list.size()+ " instances");
            for ( ML_server ml_server : list )
            {
                if ( dbg) logger.log("query_server_health for:" + ml_server.to_string());
                if (is_server_alive_heavy_version(ml_server,null,logger))
                {
                    ML_servers_monitor.refresh_add(ml_server,owner,logger);
                }
                else
                {
                    ML_servers_monitor.refresh_remove(ml_server,owner,logger);
                }
            }
        }
    }

    //**********************************************************
    public boolean is_server_alive_heavy_version(
            ML_server ml_server,
            StringBuilder sb_out, Logger logger)
    //**********************************************************
    {
        String url_string = "http://127.0.0.1:" + ml_server.port()+"/health";
        URL url = null;
        try {
            url = new URL(url_string);
        } catch (MalformedURLException e) {
            logger.log(Stack_trace_getter.get_stack_trace(url_string+" ML server alive check, (Error#1) "+e));
            return false;
        }
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(url_string+" ML server alive check, (Error#2)"+e));
            return false;
        }
        try {
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(0); // infinite
        } catch (ProtocolException e) {
            logger.log(Stack_trace_getter.get_stack_trace(url_string+" ML server alive check, (Error#3) "+e));
            return false;
        }

        try {
            connection.connect();
        } catch (IOException e) {
            //logger.log(Stack_trace_getter.get_stack_trace(""+e));
            if ( sb_out != null) sb_out.append("Connection failed !");
            logger.log((url_string+" ML server alive check, (Error#4) "+e));
            return false;
        }
        try {
            int response_code = connection.getResponseCode();
            if ( sb_out != null) sb_out.append("response code=").append(response_code).append("\n");
            //logger.log("response code="+response_code);
        } catch (IOException e) {
            //logger.log(Stack_trace_getter.get_stack_trace(""+e));
            logger.log((url_string+" ML server alive check, (Error#5):"+e));
            return false;
        }
        try {
            String response_message = connection.getResponseMessage();
            //logger.log("response message="+response_message);
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(url_string+"ML server alive check, (Error#6) "+e));
            return false;
        }

        // Read the JSON response one character at a time
        StringBuffer sb = new StringBuffer();
        try(BufferedInputStream bufferedInputStream = new BufferedInputStream(connection.getInputStream()))
        {
            if ( ultra_dbg) logger.log("ML server monitor ");
            for(;;)
            {
                int c = bufferedInputStream.read();
                if ( c == -1) break;
                if ( ultra_dbg) logger.log(""+(char)c);
                sb.append((char)c);
            }
        }
        catch (IOException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace("ML server alive check, (Error#7) "+e));
            return false;
        }
        finally {
            connection.disconnect();
        }

        if ( sb_out != null) sb_out.append(sb.toString());

        // perform some CHECKS
        String json = sb.toString();
        {
            String port_s = Simple_json_parser.read_key(json,"port",logger);
            if ( port_s == null)
            {
                logger.log("❌  port == null for server: "+ml_server.to_string());
                return false;
            }
            else
            {
                try {
                    int port = Integer.parseInt(port_s);
                    if (port != ml_server.port())
                    {
                        logger.log("❌  ports dont match ?? from /health=" + port + " port=" + ml_server.port());
                        return false;
                    }
                } catch (NumberFormatException e) {
                    logger.log(e + " ->" + port_s + "<-");
                }
            }
        }
        {
            String uuid = Simple_json_parser.read_key(json,"uuid",logger);

            if ( uuid == null)
            {
                logger.log("❌  uuid from /health == null for server: "+ml_server.to_string());
                return false;
            }
            if ( !uuid.equals(ml_server.uuid()))
            {
                logger.log("❌  uuid from /health dont match "+uuid+" for server: "+ml_server.to_string());
                return false;
            }
        }
        {
            String type = Simple_json_parser.read_key(json,"type",logger);
            if( type == null)
            {
                logger.log("❌  type from /health == null for server: "+ml_server.to_string());
                return false;
            }
            if ( !type.equals(ml_server.type()))
            {
                logger.log("❌  types from /health dont match "+type+" for server: "+ml_server.to_string());
                return false;
            }
        }



        if ( dbg) logger.log("HTTP health scan: Found 1 live server: "+ml_server.to_string());
        String status = Simple_json_parser.read_key(json,"status",logger);
        boolean returned = true;
        if ( status == null)
        {
            logger.log("❌ No status found from in ML server");
            returned = false;
        }
        else
        {
            if( dbg) logger.log("Status:"+status);
            if ( status.equals("critical_failure"))
            {
                logger.log("❌ critical_failure in ML server");
                returned = false;
            }
        }

        String diagnostics = Simple_json_parser.read_key(json,"diagnostics",logger);
        if ( diagnostics == null)
        {
            logger.log("No diagnostics found");
        }
        else
        {
            if ( !returned)
            {
                logger.log("Diagnostics:"+diagnostics);
            }
        }

        String runtime = Simple_json_parser.read_key(json,"runtime",logger);
        if ( runtime == null)
        {
            logger.log("No runtime found");
        }
        else
        {
            if ( !returned) logger.log("Runtime:"+runtime);
        }
        return returned;
    }

/*
    //**********************************************************
    @Override
    public void close()
    //**********************************************************
    {
        running = false;
    }
*/







    //**********************************************************
    public void inject(UDP_report report)
    //**********************************************************
    {
        Runnable r = () -> {
            inject_report(report);
        };
        Platform.runLater(r);

    }

    private long hit_count =0;
    private double total_duration_ms =0;
    private double max_hit_for_a_server = 0;
    //**********************************************************
    private void inject_report(UDP_report report)
    //**********************************************************
    {
        hit_count++;
        total_duration_ms += report.processing_time();
        double average_duration_ms = total_duration_ms / hit_count;
        //duration_label.setText("Average embeddings processing time: " + String.format("%.2f", average_duration_ms) + " ms");
        // Update rate
        double rate = calculate_rate();
        //rate_label.setText(String.format("Rate: %.1f embeddings/s on last 10s", rate));



        String server_uuid = report.server_uuid();
        HBox hb = uuid_to_small_hbox.get(server_uuid);
        if ( hb == null )
        {
            logger.log(Stack_trace_getter.get_stack_trace("Getting record for UNREGISTERED server UUID: " + server_uuid));
            return;
        }


        Rectangle r = new Rectangle(SMALL_WIDTH, SMALL_HEIGHT);
        r.setFill(get_random_color());
        hb.getChildren().add(r);
        if ( hb.getChildren().size() > max_hit_for_a_server )
        {
            max_hit_for_a_server = hb.getChildren().size();
            if ( max_hit_for_a_server*SMALL_WIDTH > DISPLAY_PIXEL_WIDTH *0.8 )
            {
                // decrease the width of ALL the rectangles
                SMALL_WIDTH = 0.7*SMALL_WIDTH;
                for (HBox small_box : uuid_to_small_hbox.values())
                {
                    for (int i = 0; i < small_box.getChildren().size(); i++)
                    {
                        Rectangle rect = (Rectangle) small_box.getChildren().get(i);
                        rect.setWidth(SMALL_WIDTH);
                    }
                }
            }
        }
        stage.show();
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
        long now = System.currentTimeMillis();

        // Remove timestamps older than 10 seconds
        while (!time_window.isEmpty() && now - time_window.peekFirst() > WINDOW_SIZE_MS) {
            time_window.removeFirst();
        }
        time_window.addLast(now);
        return time_window.size() / WINDOW_SIZE_S;
    }


    // the returned status is for the request type




}
