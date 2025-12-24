// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.util.tcp;

//SOURCES ./Session_factory.java


import klik.Launcher;
import klik.util.execute.actor.Aborter;
import klik.util.execute.actor.Actor_engine;
import klik.util.log.File_logger;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

//**********************************************************
public class TCP_server
//**********************************************************
{
    public static final int TEST_PORT = 4567;
    public static final int SOCKET_TIMEOUT = 1000;

    private final static boolean dbg = false;
    private final Session_factory session_factory;
    private final Aborter aborter;
    private final Logger logger;
    private int port_number;
    AtomicBoolean stopped_clean = new AtomicBoolean(false);


    //**********************************************************
    public TCP_server(Session_factory session_factory, Aborter aborter, Logger logger)
    //**********************************************************
    {
        this.session_factory = session_factory;
        this.aborter = aborter;
        this.logger = logger;
    }

    //**********************************************************
    public boolean start(int port_number_, String purpose, boolean log_stack_trace_on_bind_exception)
    //**********************************************************
    {
        AtomicBoolean is_started_ok = new AtomicBoolean(false);
        // wait until server is started
        CountDownLatch cdl = new CountDownLatch(1);
        port_number = port_number_;
        Runnable r = () ->
        {
            try(ServerSocket welcome_socket = new ServerSocket(port_number)) {
                welcome_socket.setSoTimeout(SOCKET_TIMEOUT); // 10s timeout
                logger.log("TCP server starting on port: " + port_number+ " purpose: "+purpose);
                is_started_ok.set(true);
                cdl.countDown();
                for (;;) {
                    if (!wait_and_serve(welcome_socket))
                    {
                        if ( dbg) logger.log("server closing down");
                        return;
                    }
                }
            }
            catch (BindException e)
            {
                if ( log_stack_trace_on_bind_exception)logger.log(Stack_trace_getter.get_stack_trace("server error "+e));
                cdl.countDown();
                is_started_ok.set(false);
            }
            catch (SocketException e)
            {
                if ( !stopped_clean.get())
                {
                    logger.log(Stack_trace_getter.get_stack_trace("server error "+e));
                    cdl.countDown();
                    is_started_ok.set(false);
                }
            }

            catch (IOException e)
            {
                logger.log(Stack_trace_getter.get_stack_trace("server error "+e));
                cdl.countDown();
                is_started_ok.set(false);
            }
        };
        Actor_engine.execute(r,"TCP server for: "+purpose+" on: "+port_number,logger);

        try
        {
            cdl.await();
        }
        catch (InterruptedException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace("server error "+e));
        }
        if ( is_started_ok.get())
        {
            if (dbg) logger.log(Stack_trace_getter.get_stack_trace("server started OK "));
            else logger.log(("✅ server started OK "));
        }
        else
        {
            if ( log_stack_trace_on_bind_exception) logger.log(Stack_trace_getter.get_stack_trace("server error "));
        }
        return is_started_ok.get();
    }



    // start a server on a random (free) port, return the port number
    //**********************************************************
    public int start_zero(String purpose,  boolean log_stack_trace_on_bind_exception)
    //**********************************************************
    {
        AtomicInteger is_started_on = new AtomicInteger(-1);
        // wait until server is started
        CountDownLatch cdl = new CountDownLatch(1);
        Runnable r = () ->
        {
            try(ServerSocket welcome_socket = new ServerSocket(0))
            {
                welcome_socket.setSoTimeout(SOCKET_TIMEOUT); // timeout
                port_number = welcome_socket.getLocalPort();
                logger.log("TCP server starting on port : " + port_number+ " purpose: "+purpose);
                is_started_on.set(port_number);
                cdl.countDown();
                for (;;) {
                    if (!wait_and_serve(welcome_socket))
                    {
                        if ( dbg) logger.log("server closing down");
                        return;
                    }
                }
            }
            catch (BindException e)
            {
                if ( log_stack_trace_on_bind_exception)logger.log(Stack_trace_getter.get_stack_trace("server error "+e));
                cdl.countDown();
            }
            catch (IOException e)
            {
                logger.log(Stack_trace_getter.get_stack_trace("server error "+e));
                cdl.countDown();
            }
        };
        Actor_engine.execute(r,"TCP server for: "+purpose,logger);

        try
        {
            cdl.await();
        }
        catch (InterruptedException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace("server error "+e));
        }
        if ( is_started_on.get() >0)
        {
            if (dbg) logger.log(Stack_trace_getter.get_stack_trace("server started OK "));
            else logger.log(("✅ server started OK "));
        }
        else
        {
            if ( log_stack_trace_on_bind_exception) logger.log(Stack_trace_getter.get_stack_trace("server error "));
        }
        return is_started_on.get();
    }


    //**********************************************************
    public void stop()
    //**********************************************************
    {
        aborter.abort("stop requested");
        // send message to unblock
        TCP_client.send_in_a_thread("127.0.0.1", port_number, "stop", logger);
    }


    //**********************************************************
    private boolean wait_and_serve(ServerSocket welcome_socket)
    //**********************************************************
    {
        // blocks until a client connects
        try
        {
             if ( dbg) logger.log("server accepting connection");

            Socket socket = welcome_socket.accept();
            if ( aborter.should_abort() )
            {
                if ( dbg) logger.log("server closing down on abort");
                return false;
            }
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

            Runnable r = () -> {
                Session session = session_factory.make_session();
                boolean status = session.on_client_connection(dis,dos);
                if ( !status)
                {
                    aborter.abort("on_client_connection returns false, closing server");
                    stopped_clean.set(true);
                    try {
                        welcome_socket.close();
                    } catch (IOException e) {
                        logger.log(Stack_trace_getter.get_stack_trace("server error "+e));
                    }
                }
            };
            Actor_engine.execute(r,"TCP server, 1 client session",logger);
        }
        catch (SocketTimeoutException e)
        {
            if (aborter.should_abort())
            {
                if ( dbg) logger.log("TCP server closing down on abort");
                try {
                    welcome_socket.close();
                } catch (IOException ex) {
                    logger.log("server closing error "+ex);
                }
                return false;
            }
            return true;
        }
        catch (IOException e)
        {
            if (! stopped_clean.get()) logger.log(Stack_trace_getter.get_stack_trace("server error "+e));
            return false;
        }
        return true;
    }

    //**********************************************************
    public static void main( String []args)
    //**********************************************************
    {
        Logger logger = new File_logger("TCP server test");

        CountDownLatch cdl = new CountDownLatch(1);
        Session_factory the_session_factory = () -> new Session()
        {

            @Override
            public boolean on_client_connection(DataInputStream dis, DataOutputStream dos)
            {
                logger.log("on client connection");
                int size = 0;
                try {
                    size = dis.readInt();
                    logger.log("size ->"+size+"<-");
                }
                catch (IOException e)
                {
                    logger.log(Stack_trace_getter.get_stack_trace(""+e));
                }
                try
                {
                    byte buffer[] = new byte[size];
                    dis.read(buffer);
                    String got = new String(buffer, StandardCharsets.UTF_8);
                    logger.log("got ->"+got+"<-");
                    String answer = "server got this ->"+got+"<-";
                    TCP_util.write_string(answer,dos);
                    dos.flush();
                    logger.log("sending back ->"+answer+"<-");
                }
                catch (IOException e)
                {
                    logger.log(Stack_trace_getter.get_stack_trace(""+e));
                }
                return false;
            }

            @Override
            public String name() {
                return "test server";
            }
        };

        TCP_server tcp_server = new TCP_server(the_session_factory,new Aborter("test",logger),logger);

        tcp_server.start(TEST_PORT, "test",true);

        try {
            cdl.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
