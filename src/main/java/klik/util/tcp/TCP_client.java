package klik.util.tcp;

//SOURCES ./TCP_server.java

import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import klik.util.log.System_logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;


//**********************************************************
public class TCP_client
//**********************************************************
{
    private final static boolean dbg = true;


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
            byte[] buffer = request.getBytes(StandardCharsets.UTF_8);
            dos.writeInt(buffer.length);
            dos.write(buffer);
            dos.flush();
            if ( dbg) logger.log("write done");
            int size_in = dis.readInt();
            if ( dbg) logger.log("got "+size_in);
            buffer = new byte[size_in];
            dis.read(buffer);
            String reply = new String(buffer,StandardCharsets.UTF_8);
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
        Logger logger = System_logger.get_system_logger("TCP client test");
        TCP_client_out tco = TCP_client.request("localhost",TCP_server.TEST_PORT, "hello", logger);

        logger.log("status: "+tco.status());
        logger.log("reply: "+tco.reply());
        logger.log("message: "+tco.message());
    }

}
