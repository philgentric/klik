package klik.util.tcp;

//SOURCES ./Session_factory.java


import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import klik.util.log.System_logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

//**********************************************************
public class TCP_server
//**********************************************************
{
    public static final int TEST_PORT = 4567;
    private final static boolean dbg = false;
    private final Session_factory session_factory;
    private final Aborter aborter;
    private final Logger logger;
    private int port_number;


    //**********************************************************
    public TCP_server(Session_factory session_factory, Aborter aborter, Logger logger)
    //**********************************************************
    {
        this.session_factory = session_factory;
        this.aborter = aborter;
        this.logger = logger;
    }

    //**********************************************************
    public boolean start(int port_number_, boolean stack_trace_bind_exception)
    //**********************************************************
    {
        AtomicBoolean is_started_ok = new AtomicBoolean(false);
        // wait until server is started
        CountDownLatch cdl = new CountDownLatch(1);
        port_number = port_number_;
        Runnable r = () ->
        {
            try(ServerSocket welcome_socket = new ServerSocket(port_number)) {
                logger.log("TCP server starting on port: " + port_number);
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
                if ( stack_trace_bind_exception)logger.log(Stack_trace_getter.get_stack_trace("server error "+e));
                cdl.countDown();
                is_started_ok.set(false);
            }
            catch (IOException e)
            {
                logger.log(Stack_trace_getter.get_stack_trace("server error "+e));
                cdl.countDown();
                is_started_ok.set(false);
            }
        };
        Actor_engine.execute(r,logger);

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
            else logger.log(("server started OK "));
        }
        else
        {
            if ( stack_trace_bind_exception) logger.log(Stack_trace_getter.get_stack_trace("server error "));
        }
        return is_started_ok.get();
    }


    //**********************************************************
    private boolean wait_and_serve(ServerSocket welcome_socket)
    //**********************************************************
    {
        // blocks until a client connects
        try
        {
            if ( aborter.should_abort() )
            {
                if ( dbg) logger.log("server closing down on abort");
                return false;
            }
            if ( dbg) logger.log("server accepting connection");

            Socket socket = welcome_socket.accept();
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

            Runnable r = () -> {
                Session session = session_factory.make_session();
                session.on_client_connection(dis,dos);
            };
            Actor_engine.execute(r,logger);
        }
        catch (IOException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace("server error "+e));
            return false;
        }
        return true;
    }

    //**********************************************************
    public static void main( String []args)
    //**********************************************************
    {
        Logger logger = System_logger.get_system_logger("TCP server test");

        CountDownLatch cdl = new CountDownLatch(1);
        Session_factory the_session_factory = () -> new Session()
        {

            @Override
            public void on_client_connection(DataInputStream dis, DataOutputStream dos)
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
                    buffer = answer.getBytes(StandardCharsets.UTF_8);
                    logger.log("sending back ->"+answer+"<-");

                    dos.writeInt(buffer.length);
                    dos.write(buffer);
                }
                catch (IOException e)
                {
                    logger.log(Stack_trace_getter.get_stack_trace(""+e));
                }
            }

            @Override
            public String name() {
                return "test server";
            }
        };

        TCP_server tcp_server = new TCP_server(the_session_factory,new Aborter("test",logger),logger);

        tcp_server.start(TEST_PORT, true);

        try {
            cdl.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
