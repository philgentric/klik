// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.util.tcp;

//SOURCES ./TCP_server.java
//SOURCES ../../properties/Properties_server.java

import klikr.util.execute.actor.Actor_engine;
import klikr.util.log.File_logger;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;


//**********************************************************
public class TCP_client
//**********************************************************
{
    private final static boolean dbg = false;

    //**********************************************************
    public static void send_in_a_thread(String host, int port_number, String msg, Logger logger)
    //**********************************************************
    {
        if ( port_number < 0)
        {
            logger.log("TCP_client.send_in_a_thread called with negative port number "+port_number+" for host "+host+" message: "+msg);
            return;
        }
        Actor_engine.execute(() -> send(host, port_number, msg, logger), "TCP send",logger);
    }
    //**********************************************************
    public static void send(String host, int port_number, String msg, Logger logger)
    //**********************************************************
    {
        try( Socket client_socket = new Socket(host,port_number);
             DataInputStream dis = new DataInputStream(client_socket.getInputStream());
             DataOutputStream dos = new DataOutputStream(client_socket.getOutputStream())
        )
        {
            //client_socket.setKeepAlive(false);
            if ( dbg) logger.log("TCP client connected on "+host+" "+port_number+" sending ->"+msg+"<-");
            TCP_util.write_string(msg,dos);
            dos.flush();
        }
        catch (UnknownHostException e)
        {
            if ( dbg) logger.log(Stack_trace_getter.get_stack_trace(""+e));
        }
        catch (ConnectException e)
        {
            if ( dbg) logger.log(Stack_trace_getter.get_stack_trace(e+" Cannot connect is a server at "+host+":"+port_number+" started?"));
        }
        catch (IOException e)
        {
            if ( dbg) logger.log(Stack_trace_getter.get_stack_trace(""+e));
        }

    }

    //**********************************************************
    public static TCP_client_out request(String host, int port_number, String request, Logger logger)
    //**********************************************************
    {
        try( Socket client_socket = new Socket(host,port_number);
             DataInputStream dis = new DataInputStream(client_socket.getInputStream());
             DataOutputStream dos = new DataOutputStream(client_socket.getOutputStream())
        )
        {
            //client_socket.setKeepAlive(false);
            if ( dbg) logger.log("TCP client connected on "+host+" "+port_number+" sending ->"+request+"<-");
            TCP_util.write_string(request,dos);
            dos.flush();
            String reply = TCP_util.read_string(dis);
            if ( dbg) logger.log("got ->"+reply+"<-");
            return new TCP_client_out(true,reply,"");
        }
        catch (UnknownHostException e)
        {
            if ( dbg) logger.log(Stack_trace_getter.get_stack_trace(""+e));
            return new TCP_client_out(false,"",""+e);

        }
        catch (ConnectException e)
        {
            if ( dbg) logger.log(Stack_trace_getter.get_stack_trace(e+" Cannot connect is a server at "+host+":"+port_number+" started?"));
            return new TCP_client_out(false,"","Cannot connect! Is a server at "+host+":"+port_number+" started?");

        }
        catch (IOException e)
        {
            if ( dbg) logger.log(Stack_trace_getter.get_stack_trace(""+e));
            return new TCP_client_out(false,"",""+e);

        }

    }


    //**********************************************************
    public static TCP_client_out request2(String host, int port_number, String request1, String request2, Logger logger)
    //**********************************************************
    {
        try( Socket client_socket = new Socket(host,port_number);
             DataInputStream dis = new DataInputStream(client_socket.getInputStream());
             DataOutputStream dos = new DataOutputStream(client_socket.getOutputStream())
        )
        {
            //client_socket.setKeepAlive(false);
            if ( dbg) logger.log("TCP client connected on "+host+" "+port_number+" sending ->"+request1+"<-");
            TCP_util.write_string(request1,dos);
            if ( dbg) logger.log("TCP client connected on "+host+" "+port_number+" sending ->"+request2+"<-");
            TCP_util.write_string(request2,dos);
            dos.flush();
            if ( dbg) logger.log("write done");
            String reply = TCP_util.read_string(dis);
            if ( dbg) logger.log("got ->"+reply+"<-");
            return new TCP_client_out(true,reply,"");
        }
        catch (UnknownHostException e)
        {
            if ( dbg) logger.log(Stack_trace_getter.get_stack_trace(""+e));
            return new TCP_client_out(false,"",""+e);

        }
        catch (ConnectException e)
        {
            if ( dbg) logger.log(Stack_trace_getter.get_stack_trace(e+" Cannot connect is a server at "+host+":"+port_number+" started?"));
            return new TCP_client_out(false,"","Cannot connect! Is a server at "+host+":"+port_number+" started?");

        }
        catch (IOException e)
        {
            if ( dbg) logger.log(Stack_trace_getter.get_stack_trace(""+e));
            return new TCP_client_out(false,"",""+e);

        }

    }






    //**********************************************************
    public static void main( String []args)
    //**********************************************************
    {
        Logger logger = new File_logger("TCP client test");
        TCP_client_out tco = TCP_client.request("127.0.0.1",TCP_server.TEST_PORT, "hello", logger);

        logger.log("status: "+tco.status());
        logger.log("reply: "+tco.reply());
        logger.log("error_message: "+tco.error_message());
    }

}
