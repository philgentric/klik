package klikr.javalin.monaco;

import io.javalin.Javalin;
import io.javalin.websocket.WsContext;
import javafx.application.Application;
import klikr.javalin.Javalin_common;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.log.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A Javalin (websocket) bridge for a Monaco Editor running in a local browser.
 */
//**********************************************************
public class Javalin_monaco
//**********************************************************
{
    private final static boolean ultra_dbg = false;
    private static Javalin_monaco instance = null;
    private final Application application;
    private final Logger logger;

    private Javalin javalin;
    private final int port_number;
    private final Set<WsContext> connected_clients = Collections.newSetFromMap(
            new ConcurrentHashMap<>()
    );
    private String page_title = "Text editor";

    private final static AtomicReference<Supplier<String>> atomic_text_source = new AtomicReference<>(null);
    private final static AtomicReference<Consumer<String>> atomic_text_sink = new AtomicReference<>(null);

    //**********************************************************
    public static void edit(Application application, Path path, Logger logger)
    //**********************************************************
    {
        init(application, logger);
        instance.open_write_allowed(path);
    }

    //**********************************************************
    public static void read_only(Application application, Path path, Logger logger)
    //**********************************************************
    {
        init(application, logger);
        instance.open_read_only(path);
    }

    //**********************************************************
    public static void show2(Application application, Supplier<String> text_source, Consumer<String> text_sink, Logger logger)
    //**********************************************************
    {
        init(application, logger);
        instance.set_source_and_sink(text_source,text_sink);
    }

    //**********************************************************
    private static void init(Application application, Logger logger)
    //**********************************************************
    {
        synchronized (Javalin_monaco.class)
        {
            if (instance == null)
            {
                instance = new Javalin_monaco(application, logger);
                instance.start_javalin_server();
            }
        }
    }

    //**********************************************************
    private Javalin_monaco(Application application, Logger logger)
    //**********************************************************
    {
        this.application = application;
        this.logger = logger;
        this.port_number = Javalin_common.find_free_port(logger);
    }




    //**********************************************************
    private void open_write_allowed(Path path)
    //**********************************************************
    {
        page_title = "Editing: "+path.getFileName().toString();
        Supplier<String> text_source = ()->
        {
            try
            {
                List<String> lines = Files.readAllLines(path);
                StringBuilder builder = new StringBuilder();
                for ( String line : lines)
                {
                    builder.append(line).append("\n");
                }
                return  builder.toString();
            }
            catch (IOException e)
            {
                logger.log(""+e);
            }
            return "Javalin_Monaco error cannot read: "+path;
        };

        Consumer<String> text_sink = (String s) ->
        {
            try {
                Files.writeString(path,s);
            } catch (IOException e) {
                logger.log(""+e);
            }
        };
        logger.log(("Javalin_Monaco EDITOR mode"));
        set_source_and_sink(text_source,text_sink);
        Javalin_common.open_browser(application,false,page_title,port_number,logger);
    }

    //**********************************************************
    private void open_read_only(Path path)
    //**********************************************************
    {
        page_title = "Viewing: "+path.getFileName().toString();
        Supplier<String> text_source = ()->
        {
            try
            {
                List<String> lines = Files.readAllLines(path);
                StringBuilder builder = new StringBuilder();
                for ( String line : lines)
                {
                    builder.append(line).append("\n");
                }
                return  builder.toString();
            }
            catch (IOException e)
            {
                logger.log(""+e);
            }
            return "Javalin_Monaco error cannot read: "+path;
        };

        logger.log("Javalin_Monaco READ-ONLY mode");
        set_source_and_sink(text_source,null);
        Javalin_common.open_browser(application,true,page_title,port_number,logger);
    }



    //**********************************************************
    private void set_source_and_sink(Supplier<String> text_source, Consumer<String> text_sink)
    //**********************************************************
    {
        atomic_text_source.set(text_source);
        atomic_text_sink.set(text_sink);
    }


    //**********************************************************
    private void start_javalin_server()
    //**********************************************************
    {
        CountDownLatch started = new CountDownLatch(1);
        Runnable r = () -> {
            javalin = Javalin.create(config ->
                {
                    // Serve the static HTML file
                    config.staticFiles.add(
                        "/javalin_monaco",
                        io.javalin.http.staticfiles.Location.CLASSPATH
                    );
                }).start(port_number);

            // WebSocket Endpoint
            javalin.ws("/monaco-ws", ws -> {
                ws.onConnect(ctx ->
                {
                    ctx.session.setIdleTimeout(Duration.ofMillis(3600000L)); // 1 hour timeout
                    logger.log("Javalin_Monaco WebSocket connected");
                    connected_clients.add(ctx);
                });
                ws.onMessage(ctx ->
                {
                    String msg = ctx.message();

                    Supplier<String> source = atomic_text_source.get();
                    if ( source == null)
                    {
                        logger.log("Javalin_monaco ERROR: No source provided");
                        return;
                    }
                    if ("REQUEST_INIT".equals(msg))
                    {
                        // Client is fresh and wants the current text

                        ctx.send(source.get());
                        return;
                    }

                    if ( ultra_dbg) logger.log("Received from browser: " + msg);
                    if (source.get().equals(msg))
                    {
                        // already up to date
                        if ( ultra_dbg) logger.log("Javalin_monaco source already has: " + msg);
                    }
                    else
                    {
                        if ( ultra_dbg) logger.log("Javalin_monaco updating sink with: " + msg);

                        Consumer<String> sink = atomic_text_sink.get();
                        if ( sink == null)
                        {
                            logger.log("Javalin_monaco ERROR: No sink provided");
                            return;
                        }
                        sink.accept(msg);
                    }
                });
                ws.onClose(ctx -> {
                    if ( ultra_dbg) logger.log("Javalin_monaco server disconnected");
                    connected_clients.remove(ctx);
                });
            });

            started.countDown();
        };

        Actor_engine.execute(r,"Javalin_Monaco server",logger);
        try {
            started.await();
        } catch (InterruptedException e) {
            logger.log("Javalin_Monaco server interrupted"+e);
            return;
        }
        logger.log("Javalin_Monaco server started on port " + port_number);
    }

    //**********************************************************
    static void stop_server()
    //**********************************************************
    {
        if (instance != null)
        {
            instance.javalin.stop();
        }
    }

    //**********************************************************
    void broadcast_to_browser(String text)
    //**********************************************************
    {
        for (WsContext ctx : connected_clients)
        {
            if (ctx.session.isOpen())
            {
                ctx.send(text);
            }
        }
    }




}
