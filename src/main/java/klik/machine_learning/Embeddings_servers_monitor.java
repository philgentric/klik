// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.machine_learning;

import javafx.application.Platform;
import javafx.stage.Window;
import klik.properties.boolean_features.Feature;
import klik.properties.boolean_features.Feature_cache;
import klik.util.execute.actor.Actor_engine;
import klik.util.log.Logger;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

record Report(String server_uuid, String model_name, String image_path, double processing_time) {}

//**********************************************************
public class Embeddings_servers_monitor implements AutoCloseable
//**********************************************************
{
    private static Embeddings_servers_monitor instance;

    private DatagramSocket socket;
    private final byte[] buffer = new byte[1024];
    private volatile boolean running = true;
    private final Logger logger;
    public final int port;
    private Embeddings_servers_monitoring_stage monitoring_frame;

    //**********************************************************
    public static int get_servers_monitor_udp_port(Window owner, Logger logger)
    //**********************************************************
    {
        if ( instance == null)
        {
            instance = new Embeddings_servers_monitor(owner,logger);
        }
        logger.log("Embeddings servers monitor listening on UDP port: "+instance.port);
        return instance.port;
    }

    //**********************************************************
    private Embeddings_servers_monitor(Window owner, Logger logger)
    //**********************************************************
    {
        int port_tmp;
        this.logger = logger;
        port_tmp = -1;
        try {
            // find FREE UDP port
            socket = new DatagramSocket(0);
            port_tmp = socket.getLocalPort();
            logger.log("Servers monitor started on UDP port: "+port_tmp);
        } catch (SocketException e) {
            logger.log(""+e);
        }
        port = port_tmp;
        if (Feature_cache.get(Feature.Enable_feature_vector_monitoring)) {
            Platform.runLater(() -> {
                monitoring_frame = new Embeddings_servers_monitoring_stage(owner, logger);
            });
        }
        Actor_engine.execute(() -> receive_messages(),"Receive embedding server UDP monitoring packets",logger);
    }

    //**********************************************************
    private void receive_messages()
    //**********************************************************
    {
        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                //logger.log("Waiting for UDP packet...");
                socket.receive(packet);
                //logger.log("UDP packet received");
                String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
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
        //logger.log("Embeddings servers monitor received->"+message+"<-");
        String[] parts = message.split(",");
        if (parts.length == 4)
        {

            String server_uuid = parts[0];
            String model_name = parts[1];
            String image_path = URLDecoder.decode(parts[2], StandardCharsets.UTF_8);
            double processing_time = Double.parseDouble(parts[3]);

            Report report = new Report(server_uuid, model_name, image_path, processing_time);

            monitoring_frame.inject(report);
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
