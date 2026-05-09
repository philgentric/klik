package klikr.javalin.list;

import io.javalin.Javalin;
import io.javalin.websocket.WsContext;
import javafx.application.Application;
import klikr.javalin.Javalin_common;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.log.Logger;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

// displays in the default browser a clickable list
//**********************************************************
public class Javalin_for_list
//**********************************************************
{
    private static final boolean ultra_dbg = false;
    private static Javalin_for_list instance;
    private final Logger logger;
    private Javalin javalin;
    private final int port_number;
    private final Set<WsContext> connected_clients = Collections.newSetFromMap(
            new ConcurrentHashMap<>()
    );
    //private String page_title = "Choose in list";
    private final AtomicReference<Consumer<String>> on_click_source = new AtomicReference<>();

    //**********************************************************
    public static void show(
            Application application,
            String page_title,
            List<List_item> items,
            Consumer<String> on_click,
            Logger logger)
    //**********************************************************
    {
        synchronized (Javalin_for_list.class)
        {
            if (instance == null)
            {
                instance = new Javalin_for_list(application, logger);
                instance.start_javalin_server(items);
            }
        }
        instance.on_click_source.set(on_click);
        Javalin_common.open_browser(application,true,page_title,instance.port_number,logger);
    }

    //**********************************************************
    private Javalin_for_list(Application application, Logger logger)
    //**********************************************************
    {
        this.logger = logger;
        this.port_number = Javalin_common.find_free_port(logger);
    }

    //**********************************************************
    private void start_javalin_server(List<List_item> items)
    //**********************************************************
    {
        CountDownLatch started = new CountDownLatch(1);
        Runnable r = () -> {
            javalin = Javalin.create(config ->
            {
                // Serve the static HTML file
                config.staticFiles.add(
                        "/javalin_list",
                        io.javalin.http.staticfiles.Location.CLASSPATH
                );
            }).start(port_number);

            // WebSocket Endpoint
            javalin.ws("/list-ws", ws -> {
                ws.onConnect(ctx ->
                {
                    ctx.session.setIdleTimeout(Duration.ofMillis(3600000L)); // 1 hour timeout
                    logger.log("Javalin_for_list WebSocket connected");
                    connected_clients.add(ctx);
                });
                ws.onMessage(ctx ->
                {
                    String msg = ctx.message();
                    
                    if ("REQUEST_INIT".equals(msg))
                    {
                        logger.log("Javalin_for_list REQUEST_INIT: "+items.size());

                        // Client is fresh and wants the current text

                        StringBuilder sb = new StringBuilder();
                        sb.append("[");
                        for ( int i = 0; i < items.size(); i++ ) 
                        {
                            if ( i> 9) sb.append(", ");
                            sb.append("{\"id\":\"").append(escape_json(items.get(i).ID()))
                                .append("{\"text\":\"").append(escape_json(items.get(i).text()))
                                .append("\"}");
                        }
                        sb.append("]");
                        ctx.send(sb.toString());
                        logger.log("Javalin_for_list sending: "+sb.toString());
                        return;
                    }

                    if ( msg.startsWith("CLICK:")) {
                        if (ultra_dbg) logger.log("Javalin_for_list Received from browser: " + msg);
                        String ID = msg.substring("CLICK:".length());
                        if (ultra_dbg) logger.log("Javalin_for_list ID Received from browser: " + ID);
                        Consumer<String> on_click = on_click_source.get();
                        if ( on_click == null)
                        {
                            logger.log("FATAL Javalin_for_list on_click == null");
                        }
                        else
                        {
                            on_click.accept(msg);
                        }
                        return;
                    }
                    if (ultra_dbg) logger.log("Javalin_for_list WARNING unknown message Received from browser: " + msg);


                });
                ws.onClose(ctx -> {
                    if ( ultra_dbg) logger.log("Javalin_monaco server disconnected");
                    connected_clients.remove(ctx);
                });
            });

            started.countDown();
        };

        Actor_engine.execute(r,"Javalin_for_list server",logger);
        try {
            started.await();
        } catch (InterruptedException e) {
            logger.log("Javalin_for_list server interrupted"+e);
            return;
        }
        logger.log("Javalin_for_list server started on port " + port_number);
    }

    //**********************************************************
    private String escape_json(String s)
    //**********************************************************
    {
        if ( s == null ) return "";
        return s.replace("\\","\\\\")
                .replace("\"","\\\"")
                .replace("\n","\\n")
                .replace("\r","\\r")
                .replace("\t","\\t");

    }
}
