// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.machine_learning.feature_vector;

import javafx.application.Platform;
import javafx.stage.Window;
import klikr.machine_learning.ML_servers_monitor;
import klikr.properties.boolean_features.Feature;
import klikr.properties.boolean_features.Feature_cache;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.log.Logger;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;


//**********************************************************
public class UDP_traffic_monitor implements AutoCloseable
//**********************************************************
{
    private static final boolean dbg = false;
    private static UDP_traffic_monitor instance;

    private DatagramSocket socket;
    private final byte[] buffer = new byte[1024];
    private volatile boolean running = true;
    private final Logger logger;
    public final int port = 65123;
    private UDP_traffic_monitoring_stage monitoring_frame; // may be null

    // used when we start servers
    //**********************************************************
    public static int get_servers_monitor_udp_port(Window owner, Logger logger)
    //**********************************************************
    {
        start_servers_monitoring(owner, logger);
        return instance.port;
    }

    // used when we start servers
    //**********************************************************
    public static void start_servers_monitoring(Window owner, Logger logger)
    //**********************************************************
    {
        if ( instance == null)
        {
            instance = new UDP_traffic_monitor(owner,logger);
            ML_servers_monitor.start_ML_servers_monitor(owner, logger);
            logger.log("ML servers monitoring is activated");
        }


    }

    //**********************************************************
    private UDP_traffic_monitor(Window owner, Logger logger)
    //**********************************************************
    {

        //int port_tmp;
        this.logger = logger;
        //port_tmp = -1;
        socket = null;
        try {
            // find FREE UDP port
            socket = new DatagramSocket(port);
            //port_tmp = socket.getLocalPort();
            logger.log("Servers monitor started on UDP port: "+port);
        } catch (SocketException e) {
            logger.log("WARNING: UDP socket failed"+e);
            running = false;
            return;
        }
        Platform.runLater(() -> {
                monitoring_frame = new UDP_traffic_monitoring_stage(owner, logger);
            });
        Actor_engine.execute(this::receive_messages,"Receive embedding server UDP monitoring packets",logger);
    }

    //**********************************************************
    private void receive_messages()
    //**********************************************************
    {
        if ( socket == null) return;

        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                if ( dbg) logger.log("Waiting for UDP packet...");
                socket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                if ( dbg) logger.log("UDP packet received: "+message);
                process_message(message);
            } catch (Exception e) {
                logger.log(""+e);
            }
        }
    }



    //**********************************************************
    private void process_message(String message)
    //**********************************************************
    {
        if ( dbg) logger.log("Embeddings servers monitor received->"+message+"<-");
        String[] parts = message.split(",");
        if (parts.length == 4)
        {

            String server_uuid = parts[0];
            String model_name = parts[1];
            String image_path = URLDecoder.decode(parts[2], StandardCharsets.UTF_8);
            double processing_time = Double.parseDouble(parts[3]);

            UDP_report report = new UDP_report(server_uuid, model_name, image_path, processing_time);

            if ( monitoring_frame!=null) monitoring_frame.inject(report);
            logger.log("Server: "+server_uuid+" Model: "+model_name+" processed "+image_path+" in "+processing_time+" seconds%n");
        }
        else
        {
            logger.log("Invalid message format, expecting 4 parts got this:->"+message+"<-");
        }
    }

    //**********************************************************
    @Override
    public void close()
    //**********************************************************
    {
        running = false;
        socket.close();
    }
}
