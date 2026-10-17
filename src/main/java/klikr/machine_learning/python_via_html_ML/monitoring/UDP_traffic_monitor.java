// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.machine_learning.python_via_html_ML.monitoring;

import klikr.util.Kontext;
import klikr.util.execute.actor.Actor_engine;

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
    private static volatile UDP_traffic_monitor instance;

    private DatagramSocket socket;
    private final byte[] buffer = new byte[1024];
    private volatile boolean running = true;
    private final Kontext context;
    public final int port = 65123;
    private static ML_servers_monitor monitoring_frame; // may be null

    // used when we start servers
    //**********************************************************
    public static int get_servers_monitor_udp_port(Kontext context)
    //**********************************************************
    {
        start_servers_monitoring(context);
        return instance.port;
    }

    // used when we start servers
    //**********************************************************
    public static void start_servers_monitoring(Kontext context)
    //**********************************************************
    {
        if (instance == null)
        {
            synchronized (UDP_traffic_monitor.class)
            {
                if (instance == null)
                {
                    instance = new UDP_traffic_monitor(context);
                    ML_servers_monitor.start_ML_servers_monitor(context);
                    context.log("ML servers monitoring is running");
                }
            }
        }

    }

    //**********************************************************
    private UDP_traffic_monitor(Kontext context)
    //**********************************************************
    {

        //int port_tmp;
        this.context = context;
        //port_tmp = -1;
        socket = null;
        try {
            // find FREE UDP port
            socket = new DatagramSocket(port);
            //port_tmp = socket.getLocalPort();
            context.log("Servers monitor started on UDP port: "+port);
        } catch (SocketException e) {
            context.log_with_stack_trace_from_throwable("WARNING: UDP socket failed",e);
            running = false;
            return;
        }
/*        Platform.runLater(() -> {
                //monitoring_frame = new UDP_traffic_monitoring_stage(owner, logger);
            });
*/
        Actor_engine.execute(this::receive_messages,"Receive embedding server UDP monitoring packets", context.logger());
    }

    public static void set_monitoring_reception_frame(ML_servers_monitor ml_servers_monitor)
    {
        monitoring_frame = ml_servers_monitor;
    }

    //**********************************************************
    private void receive_messages()
    //**********************************************************
    {
        if ( socket == null) return;

        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                if ( dbg) context.log("Waiting for UDP packet...");
                socket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                if ( dbg) context.log("UDP packet received: "+message);
                process_message(message);
            } catch (Exception e) {
                context.log_with_stack_trace_from_throwable("",e);
            }
        }
    }



    //**********************************************************
    private void process_message(String message)
    //**********************************************************
    {
        if ( dbg) context.log("Embeddings servers monitor received->"+message+"<-");
        String[] parts = message.split(",");
        if (parts.length == 4)
        {

            String server_uuid = parts[0];
            String model_name = parts[1];
            String image_path = URLDecoder.decode(parts[2], StandardCharsets.UTF_8);
            double processing_time_ms = Double.parseDouble(parts[3]);

            UDP_report report = new UDP_report(server_uuid, model_name, image_path, processing_time_ms);

            if (dbg) context.log("UDP ML servers traffic monitoring Server: "+server_uuid+" Model: "+model_name+" processed "+image_path+" in "+processing_time_ms+" milliseconds");
            if ( monitoring_frame!=null) monitoring_frame.inject(report);
        }
        else
        {
            context.log("Invalid message format, expecting 4 parts got this:->"+message+"<-");
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
