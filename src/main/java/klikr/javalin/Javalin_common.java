package klikr.javalin;

import javafx.application.Application;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

//**********************************************************
public class Javalin_common
//**********************************************************
{
    private static final boolean dbg = false;
    //**********************************************************
    public static int find_free_port(Logger logger)
    //**********************************************************
    {
        try(ServerSocket socket = new ServerSocket(0))
        {
            return socket.getLocalPort();
        }
        catch (IOException e)
        {
            logger.log(""+e);
        }
        return -1;
    }

    //**********************************************************
    public static void open_browser(Application application, boolean read_only, String page_title, int port_number, Logger logger)
    //**********************************************************
    {
        try {
            // Double check index.html path:
            //
            // config.staticFiles.add("/javalin_monaco/monaco", Location.CLASSPATH);
            // So http://localhost:8080/index.html should work if index.html is directly in that folder.
            String encoded_title = URLEncoder.encode(
                    page_title,
                    StandardCharsets.UTF_8
            );

            URI uri = URI.create(
                    "http://localhost:" +
                            port_number +
                            "/index.html?v=" +
                            System.currentTimeMillis() +
                            "&title=" +
                            encoded_title + "&isReadOnly="+read_only
            );

            if (dbg) logger.log(Stack_trace_getter.get_stack_trace("Javalin_common opening browser: "+uri));
            application.getHostServices().showDocument(uri.toString());

            /*
            Desktop.getDesktop().browse(
                    URI.create(
                            "http://localhost:" +
                                    port_number +
                                    "/index.html?v=" +
                                    System.currentTimeMillis() +
                                    "&title=" +
                                    encoded_title
                    )
            );*/

        } catch (Exception e) {
            logger.log(""+e);
        }
    }

}
