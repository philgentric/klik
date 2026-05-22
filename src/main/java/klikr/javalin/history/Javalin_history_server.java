package klikr.javalin.history;

import io.javalin.Javalin;
import io.javalin.websocket.WsContext;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Window;
import klikr.Window_type;
import klikr.browser_core.virtual_landscape.Shutdown_target;
import klikr.browser_core.virtual_landscape.Virtual_landscape_menus;
import klikr.change.history.History_engine;
import klikr.change.history.History_item;
import klikr.javalin.Javalin_common;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.log.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Unit-test application for Javalin_history_and_undo
 * Demonstrates both history and undo list viewing in browser.
 */

public class Javalin_history_server
{

    private static final boolean ultra_dbg = true;
    private Javalin javalin;
    private final int port_number;
    private final Logger logger;
    private final Window owner;
    private final Window_type window_type;
    private final Aborter aborter;
    private final Application application;
    private final Shutdown_target shutdown_target;
    private final AtomicReference<List<History_item>> history_items = new AtomicReference<>(new ArrayList<>());
    private final Set<WsContext> connected_clients = Collections.newSetFromMap(
            new ConcurrentHashMap<>()
    );

    public Javalin_history_server(Application application, Shutdown_target shutdown_target, Window_type window_type, Window owner, Aborter aborter, Logger logger) {
        this.aborter = aborter;
        this.window_type = window_type;
        this.owner = owner;
        this.logger = logger;
        this.application = application;
        this.shutdown_target = shutdown_target;
        port_number = Javalin_common.find_free_port(logger);
        start_javalin_server();
        show_history(
                application,
                shutdown_target,
                window_type,
                owner,
                aborter,
                logger
        );
    }

    //**********************************************************
    public void show_history(Application application, Shutdown_target shutdown_target, Window_type window_type, Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        History_engine engine = History_engine.get(owner);
        List<History_item> all = engine.get_all_history_items();


        history_items.set(all);


        Javalin_common.open_browser(application, true, "History", port_number, logger);
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
                        "/history",
                        io.javalin.http.staticfiles.Location.CLASSPATH
                );
            }).start(port_number);

            // WebSocket Endpoint
            javalin.ws("/monaco-ws", ws -> {
                ws.onConnect(ctx ->
                {
                    ctx.session.setIdleTimeout(Duration.ofMillis(3600000L)); // 1 hour timeout
                    logger.log("Javalin_history WebSocket connected");
                    connected_clients.add(ctx);

                    // Send current history items to newly connected client
                    send_history_items(ctx);
                });
                ws.onMessage(ctx ->
                {
                    String msg = ctx.message();

                    if ("REQUEST_INIT".equals(msg))
                    {
                        // Client is fresh and wants the current text
                        send_history_items(ctx);
                        return;
                    }

                    if (msg.startsWith("SELECT:")) {
                        // Browser selected a path - trigger the click callback
                        String selectedPath = msg.substring("SELECT:".length());
                        logger.log("Selected path from browser: " + selectedPath);
                        handle_click(selectedPath);
                        return;
                    }

                    if ( ultra_dbg) logger.log("Received from browser: " + msg);

                });
                ws.onClose(ctx -> {
                    logger.log("Javalin_history server disconnected");
                    connected_clients.remove(ctx);
                });
            });

            started.countDown();
        };

        Actor_engine.execute(r,"Javalin_history server",logger);
        try {
            started.await();
        } catch (InterruptedException e) {
            logger.log("Javalin_history server interrupted"+e);
            return;
        }
        logger.log("Javalin_history server started on port " + port_number);
    }

    //**********************************************************
    private void send_history_items(WsContext ctx)
    //**********************************************************
    {
        List<History_item> items = history_items.get();
        if (items == null || items.isEmpty()) {
            ctx.send("[]");
            return;
        }

        // Build JSON array of history items
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            History_item item = items.get(i);
            if (i > 0) json.append(",");
            // Escape the value field for JSON
            String escapedValue = escape_json(item.value);
            String escapedTimestamp = item.time_stamp.toString().replace("\"", "\\\"");
            json.append(String.format(
                "{\"value\":\"%s\",\"time_stamp\":\"%s\",\"uuid\":\"%s\",\"available\":%s}",
                escapedValue,
                escapedTimestamp,
                item.uuid.toString(),
                item.available ? "true" : "false"
            ));
        }
        json.append("]");
        ctx.send(json.toString());
    }

    //**********************************************************
    private void handle_click(String selected_path)
    //**********************************************************
    {

        Platform.runLater(() -> {
            logger.log("CLICK !!! History Item: " + selected_path);
            Virtual_landscape_menus.on_history_item_clicked(
                    Path.of(selected_path),
                    application,
                    shutdown_target,
                    window_type,
                    owner,
                    aborter,
                    logger);
        });

    }

    //**********************************************************
    private String escape_json(String s)
    //**********************************************************
    {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    //**********************************************************
     void stop_server()
    //**********************************************************
    {
        javalin.stop();

    }

}