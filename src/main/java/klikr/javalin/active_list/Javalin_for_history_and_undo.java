package klikr.javalin.active_list;

import io.javalin.Javalin;
import io.javalin.websocket.WsContext;
import javafx.application.Application;
import klikr.change.old_and_new.Old_and_new_Path;
import klikr.change.undo.Undo_item;
import klikr.javalin.Javalin_common;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.log.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class Javalin_for_history_and_undo {
    private static final boolean ultra_dbg = false;
    private static Javalin_for_history_and_undo instance = null;
    private final Application application;
    private final Logger logger;
    private Javalin javalin;
    private final int port_number;

    private enum ListType {
        HISTORY,
        UNDO
    }

    private static class ListItem {
        final String id;
        final String text;
        final String timestamp;
        final String type;
        final String details;

        ListItem(String id, String text, String timestamp, String type, String details) {
            this.id = id;
            this.text = text;
            this.timestamp = timestamp;
            this.type = type;
            this.details = details;
        }

        public String toJson() {
            return String.format("{\"id\":\"%s\",\"text\":\"%s\",\"timestamp\":\"%s\",\"type\":\"%s\",\"details\":\"%s\"}",
                    escapeJson(id),
                    escapeJson(text),
                    escapeJson(timestamp),
                    escapeJson(type),
                    escapeJson(details));
        }
    }

    private final AtomicReference<ListType> currentListType = new AtomicReference<>(ListType.HISTORY);
    private final AtomicReference<List<ListItem>> currentItems = new AtomicReference<>(new ArrayList<>());
    private final AtomicReference<Consumer<ListItem>> onItemClick = new AtomicReference<>(null);

    public static void show_history(Application application, List<Path> paths, Consumer<Path> on_click, Logger logger) {
        init(application, logger);
        instance.currentListType.set(ListType.HISTORY);

        List<ListItem> items = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                .withLocale(Locale.getDefault())
                .withZone(ZoneId.systemDefault());

        for (Path path : paths) {
            String timestamp = formatter.format(Instant.now());
            items.add(new ListItem(String.valueOf(items.size()),
                    path.toAbsolutePath().toString(),
                    timestamp,
                    "history",
                    null));
        }

        instance.currentItems.set(items);
        instance.onItemClick.set(item -> {
            if (on_click != null) {
                try {
                    on_click.accept(Paths.get(item.text));
                } catch (Exception e) {
                    logger.log("Failed to convert path: " + e.getMessage());
                }
            }
        });

        Javalin_common.open_browser(application, true, "History", instance.port_number, logger);
    }

    public static void show_undo(
            Application application,
            List<Undo_item> undo_items,
            Consumer<Undo_item> on_click,
            Logger logger) {

        init(application, logger);
        instance.currentListType.set(ListType.UNDO);

        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                .withLocale(Locale.getDefault())
                .withZone(ZoneId.systemDefault());

        List<ListItem> items = new ArrayList<>();
        for (Undo_item undo_item : undo_items) {
            String timestamp = formatter.format(undo_item.time_stamp);

            StringBuilder details = new StringBuilder();
            for (Old_and_new_Path oan : undo_item.oans) {
                details.append(oan.old_Path.toAbsolutePath())
                        .append(" → ")
                        .append(oan.new_Path.toAbsolutePath());
                if (oan.cmd != null && oan.cmd.toString().contains("MOVE")) {
                    // Check if filename changed
                    if (!oan.old_Path.getFileName().toString().equals(oan.new_Path.getFileName().toString())) {
                        details.append(" (file renamed)");
                    }
                }
                details.append("\n");
            }
            if (details.length() > 0) {
                details.setLength(details.length() - 1); // Remove trailing newline
            }

            items.add(new ListItem(String.valueOf(items.size()),
                    timestamp,  // Use timestamp as identifier for undo items
                    timestamp,
                    "undo",
                    details.toString()));
        }

        instance.currentItems.set(items);
        instance.onItemClick.set(item -> {
            if (on_click != null) {
                try {
                    // Find the corresponding undo_item
                    for (Undo_item undo_item : undo_items) {
                        if (formatter.format(undo_item.time_stamp).equals(item.text)) {
                            on_click.accept(undo_item);
                            break;
                        }
                    }
                } catch (Exception e) {
                    logger.log("Failed to find undo item: " + e.getMessage());
                }
            }
        });

        Javalin_common.open_browser(application, true, "Undo History", instance.port_number, logger);
    }

    private static void init(Application application, Logger logger) {
        synchronized (Javalin_for_history_and_undo.class) {
            if (instance == null) {
                instance = new Javalin_for_history_and_undo(application, logger);
                instance.start_javalin_server();
            }
        }
    }

    private Javalin_for_history_and_undo(Application application, Logger logger) {
        this.application = application;
        this.logger = logger;
        this.port_number = Javalin_common.find_free_port(logger);
    }

    private void start_javalin_server() {
        CountDownLatch started = new CountDownLatch(1);
        Runnable r = () -> {
            javalin = Javalin.create(config -> {
                config.staticFiles.add("/javalin_history_and_undo/history",
                        io.javalin.http.staticfiles.Location.CLASSPATH);
                config.staticFiles.add("/javalin_history_and_undo/undo",
                        io.javalin.http.staticfiles.Location.CLASSPATH);
            }).start(port_number);

            // Handle /index.html - serve based on current list type
            javalin.get("/index.html", ctx -> {
                ListType type = currentListType.get();
                String htmlPath = (type == ListType.HISTORY)
                    ? "/javalin_history_and_undo/history/index.html"
                    : "/javalin_history_and_undo/undo/index.html";

                try {
                    java.io.InputStream is = getClass().getClassLoader().getResourceAsStream(
                        htmlPath.substring(1).replace('/', '.').replace(".html", "")
                    );
                    if (is != null) {
                        java.util.Scanner scanner = new java.util.Scanner(is, "UTF-8").useDelimiter("\\A");
                        String content = scanner.hasNext() ? scanner.next() : "";
                        ctx.result(content);
                        is.close();
                    }
                } catch (Exception e) {
                    logger.log("Error serving index.html: " + e.getMessage());
                    ctx.status(404);
                }
            });

            javalin.ws("/history-undo-ws", ws -> {
                ws.onConnect(ctx -> {
                    ctx.session.setIdleTimeout(java.time.Duration.ofMillis(3600000L));
                    logger.log("Javalin_history_and_undo WebSocket connected");
                });
                ws.onMessage(ctx -> {
                    String msg = ctx.message();

                    if ("REQUEST_INIT".equals(msg)) {
                        List<ListItem> items = currentItems.get();
                        List<String> jsonItems = new ArrayList<>();
                        for (ListItem item : items) {
                            jsonItems.add(item.toJson());
                        }
                        ctx.send(String.format("[%s]", String.join(",", jsonItems)));
                        logger.log("Javalin_history_and_undo sending " + jsonItems.size() + " items");
                        return;
                    }

                    if ("TYPE_CHANGED".equals(msg)) {
                        ListType newType = ListType.valueOf(msg.substring("TYPE_CHANGED:".length()));
                        currentListType.set(newType);
                        logger.log("Javalin_history_and_undo type changed to: " + newType);
                        return;
                    }

                    if (msg.startsWith("CLICK:")) {
                        String id = msg.substring("CLICK:".length());
                        logger.log("Javalin_history_and_undo clicked item: " + id);
                        List<ListItem> items = currentItems.get();
                        for (ListItem item : items) {
                            if (item.id.equals(id)) {
                                Consumer<ListItem> callback = onItemClick.get();
                                if (callback != null) {
                                    callback.accept(item);
                                }
                                break;
                            }
                        }
                    }
                });
                ws.onClose(ctx -> {
                    if (ultra_dbg) logger.log("Javalin_history_and_undo WebSocket disconnected");
                });
            });

            started.countDown();
        };

        Actor_engine.execute(r, "Javalin_history_and_undo server", logger);
        try {
            started.await();
        } catch (InterruptedException e) {
            logger.log("Javalin_history_and_undo server interrupted: " + e.getMessage());
            return;
        }
        logger.log("Javalin_history_and_undo server started on port " + port_number);
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public static void update_data(List<ListItem> items, Logger logger) {
        if (instance != null) {
            instance.currentItems.set(items);
            instance.broadcast_to_browser(String.format("{\"type\":\"UPDATE\",\"count\":%d}", items.size()));
        }
    }

    void broadcast_to_browser(String message) {
        for (WsContext ctx : new ArrayList<>(connected_clients)) {
            if (ctx.session.isOpen()) {
                try {
                    ctx.send(message);
                } catch (Exception e) {
                    logger.log("Failed to send to client: " + e.getMessage());
                    connected_clients.remove(ctx);
                }
            }
        }
    }

    private final Set<WsContext> connected_clients = Collections.newSetFromMap(new ConcurrentHashMap<>());
}