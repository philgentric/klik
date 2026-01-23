// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.machine_learning;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//**********************************************************
public class ML_servers_monitor implements AutoCloseable
//**********************************************************
{
    private static final boolean dbg = false;
    private static final boolean ultra_dbg = false;
    private static ML_servers_monitor instance;

    private Stage stage;
    private VBox vbox;
    private volatile boolean running = true;
    private Logger logger;
    private Aborter aborter;
    private static final int DEFAULT_sleep_between_network_scans_ms = 180_000; // 3 minutes
    private static int sleep_between_network_scans_ms = DEFAULT_sleep_between_network_scans_ms;
    private static int limit;

    private Map<ML_server,HBox> hboxes = new HashMap<>();

    //**********************************************************
   public static void make_faster_network_scans()
   //**********************************************************
   {
       sleep_between_network_scans_ms = 100;
       limit = 1800; // the fast scan will last 1800*100ms = 3 minutes
   }

    // used when we start servers
    //**********************************************************
    public static void start_ML_servers_monitor(Window owner, Logger logger)
    //**********************************************************
    {
        if ( instance == null )
        {
            instance = new ML_servers_monitor();
            Platform.runLater(()->instance.init(owner,logger));
        }
    }

    //**********************************************************
    private void init(Window owner, Logger logger_)
    //**********************************************************
    {
        aborter = Shared_services.aborter();
        logger = logger_;

        stage = new Stage();
        stage.setTitle("Live ML servers");
        stage.setMinWidth(800);
        stage.setMinHeight(800);
        vbox = new VBox();
        ScrollPane sp = new ScrollPane(vbox);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        Scene scene = new Scene(sp);
        stage.setScene(scene);
        stage.show();

        Actor_engine.execute(()-> for_ever_ask_servers_using_http_health(owner),"Pole ML servers",logger);
    }

    //**********************************************************
    public static void refresh_add(ML_server ml_server, Window owner, Logger logger)
    //**********************************************************
    {
        if ( instance == null ) return;
        Platform.runLater(()->instance. make_one_line(ml_server));
    }

    //**********************************************************
    public static void refresh_remove(ML_server ml_server, Window owner, Logger logger)
    //**********************************************************
    {
        if ( instance == null ) return;
        Platform.runLater(()-> instance.hboxes.remove(ml_server));
    }

    //**********************************************************
    private void make_one_line(ML_server ml_server)
    //**********************************************************
    {
        if ( hboxes.containsKey(ml_server) ) /* ignore duplicates*/ return;

        HBox hbox = new HBox();
        hbox.setSpacing(10);
        {
            Button port_button = new Button(""+ml_server.port());
            Look_and_feel_manager.set_button_look(port_button, true, stage, logger);
            hbox.getChildren().add(port_button);
            port_button.setOnAction(event -> {
                Stage stage = new Stage();
                TextArea ta = new TextArea();
                Look_and_feel_manager.set_region_look(ta, stage, logger);
                Scene scene = new Scene(ta);
                ta.setEditable(false);
                ta.setWrapText(true);
                StringBuilder sb = new StringBuilder();
                if (is_server_alive_2(ml_server,sb, aborter,logger))
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
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Look_and_feel_manager.set_region_look(spacer, stage, logger);
            hbox.getChildren().add(spacer);
        }

        {
            Label l = new Label(ml_server.name());
            Look_and_feel_manager.set_region_look(l, stage, logger);
            hbox.getChildren().add(l);
        }
        {
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Look_and_feel_manager.set_region_look(spacer, stage, logger);
            hbox.getChildren().add(spacer);
        }
        if ( ml_server.sub_type() != null){
            if ( dbg) logger.log("ML_server sub-type: ->" + ml_server.sub_type());
            Label l = new Label(ml_server.sub_type());
            Look_and_feel_manager.set_region_look(l, stage, logger);
            hbox.getChildren().add(l);
            {
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                Look_and_feel_manager.set_region_look(spacer, stage, logger);
                hbox.getChildren().add(spacer);
            }
        }


        {
            Label l = new Label(ml_server.uuid());
            Look_and_feel_manager.set_region_look(l, stage, logger);
            hbox.getChildren().add(l);
        }
        hboxes.put(ml_server,hbox);
        vbox.getChildren().add(hbox);
    }

    //**********************************************************
    public void for_ever_ask_servers_using_http_health(Window owner)
    //**********************************************************
    {
        while (running) {
            try {
                read_registry_files_and_update_UI(owner);
                // re-check every ...
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
            } catch (Exception e) {
                logger.log(""+e);
            }
        }
    }


    //**********************************************************
    private void read_registry_files_and_update_UI(Window owner)
    //**********************************************************
    {
        Map<String, List<ML_server>> server_ports = ML_registry_discovery.scan_all_registry(owner, logger);
        if ( server_ports == null ) return;
        for ( Map.Entry<String, List<ML_server>> entry : server_ports.entrySet() )
        {
            List<ML_server> list = entry.getValue();
            if ( dbg) logger.log("server type :->" + entry.getKey()+ "<- "+list.size()+ " instances");
            for ( ML_server ml_server : list )
            {
                if ( dbg) logger.log("query_server_health for:" + ml_server.to_string());
                if (is_server_alive_2(ml_server,null, aborter,logger))
                {
                    Platform.runLater(()->make_one_line(ml_server));
                }
                else
                {
                    Platform.runLater(()-> hboxes.remove(ml_server));
                }
            }
        }
    }

    //**********************************************************
    public boolean is_server_alive_2(
            ML_server ml_server,
            StringBuilder sb_out,
            Aborter aborter, Logger logger)
    //**********************************************************
    {
        String url_string = "http://127.0.0.1:" + ml_server.port()+"/health";
        URL url = null;
        try {
            url = new URL(url_string);
        } catch (MalformedURLException e) {
            logger.log(Stack_trace_getter.get_stack_trace(url_string+" (Error#2) "+e));
            return false;
        }
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(url_string+" (Error#3)"+e));
            return false;
        }
        try {
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(0); // infinite
        } catch (ProtocolException e) {
            logger.log(Stack_trace_getter.get_stack_trace(url_string+" (Error#4) "+e));
            return false;
        }
        if (aborter.should_abort())
        {
            logger.log("aborting(2) Feature_vector_source:: reason: "+aborter.reason());
            return false;
        }
        try {
            connection.connect();
        } catch (IOException e) {
            //logger.log(Stack_trace_getter.get_stack_trace(""+e));
            if ( sb_out != null) sb_out.append("Connection failed !");
            logger.log((url_string+" (Error#5) "+e));
            return false;
        }
        try {
            int response_code = connection.getResponseCode();
            if ( sb_out != null) sb_out.append("response code=").append(response_code).append("\n");
            //logger.log("response code="+response_code);
        } catch (IOException e) {
            //logger.log(Stack_trace_getter.get_stack_trace(""+e));
            logger.log((url_string+" (Error#6):"+e));
            return false;
        }
        try {
            String response_message = connection.getResponseMessage();
            //logger.log("response message="+response_message);
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(url_string+" (Error#7) "+e));
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
        } catch (IOException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace(" (Error#8) "+e));
            return false;
        }

        if ( sb_out != null) sb_out.append(sb.toString());

        // perform some CHECKS
        String json = sb.toString();
        {
            String port_s = Simple_json_parser.read_key(json,"port",logger);
            if ( port_s == null)
            {
                logger.log(" WTF ?? port == null");
            }
            else {
                try {
                    int port = Integer.parseInt(port_s);
                    if (port != ml_server.port()) {
                        logger.log("WTF ? ports dont match ?? from /health=" + port + " from file=" + ml_server.port());
                        return false;
                    }
                } catch (NumberFormatException e) {
                    logger.log(e + " ->" + port_s + "<-");
                }
            }
        }
        {
            String uuid = Simple_json_parser.read_key(json,"uuid",logger);
            if ( !uuid.equals(ml_server.uuid()))
            {
                logger.log("WTF ? uuids dont match ?? from /health="+uuid+" from file="+ml_server.uuid());
                return false;
            }
        }
        {
            String name = Simple_json_parser.read_key(json,"name",logger);
            if ( !name.equals(ml_server.name()))
            {
                logger.log("WTF ? names dont match ?? from /health="+name+" from file="+ml_server.name());
                return false;
            }
        }

        if ( dbg)
        {
            String sub_type = Simple_json_parser.read_key(json,"sub-type",logger);
            if ( sub_type != null)
            {
                if (ml_server.sub_type() != null)
                {
                    if ( !sub_type.equals(ml_server.sub_type()))
                    {
                        logger.log("WTF ? sub-types dont match ?? from /health="+sub_type+" from file="+ml_server.sub_type());
                    }
                }
            }
            else
            {
                if (ml_server.sub_type() != null)
                {
                    logger.log("WTF ? sub-types dont match ?? from /health="+sub_type+" from file="+ml_server.sub_type());
                }
            }

            logger.log("HTTP health scan: Found 1 live server: "+ml_server.to_string());
            String status = Simple_json_parser.read_key(json,"status",logger);
            if ( status == null)
            {
                logger.log("No status found");
            }
            else
            {
                if( dbg) logger.log("Status:"+status);
            }

            String diagnostics = Simple_json_parser.read_key(json,"diagnostics",logger);
            if ( diagnostics == null)
            {
                logger.log("No diagnostics found");
            }
            else
            {
                 logger.log("Diagnostics:"+diagnostics);
            }

            String runtime = Simple_json_parser.read_key(json,"runtime",logger);
            if ( runtime == null)
            {
                logger.log("No runtime found");
            }
            else
            {
                logger.log("Runtime:"+runtime);
            }
        }
        return true;
    }


    //**********************************************************
    @Override
    public void close()
    //**********************************************************
    {
        running = false;
    }
}
