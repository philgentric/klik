package klikr.javalin.history;

import io.javalin.Javalin;
import io.javalin.websocket.WsContext;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import klikr.change.history.History_engine;
import klikr.change.history.History_item;
import klikr.javalin.Javalin_common;
import klikr.util.Shared_services;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.log.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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

public class old_Javalin_history_app extends Application {

    private static final boolean ultra_dbg = true;
    private TextArea the_TextArea;
    private Label statusLabel;
    private Javalin javalin;
    private int port_number;
    Stage stage;
    Logger logger;
    
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {

        Shared_services.init("history test app",primaryStage);
        logger = Shared_services.logger();
        port_number = Javalin_common.find_free_port(logger);
        stage = primaryStage;

        start_javalin_server();
        
        // 1. Setup JavaFX UI
        VBox root = new VBox(10);
        root.setPadding(new Insets(20));

        Label titleLabel = new Label("Javalin History & Undo Viewer - Test App");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Label instructions = new Label(
            "Click buttons below to test:\n" +
            "1. Show history (sample paths)\n" +
            "3. View browser for results"
        );
        instructions.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

        Button historyButton = new Button("🚀 Show History");
        historyButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-padding: 10 20;");
        historyButton.setOnAction(e -> showHistoryTest(logger));


        Button clearButton = new Button("Clear Browser");
        clearButton.setOnAction(e -> {
            Platform.runLater(() -> {
                the_TextArea.clear();
                the_TextArea.appendText("Browser closed - server still running\n");
            });
        });

        statusLabel = new Label("Ready to test");
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #2ecc71;");

        root.getChildren().addAll(titleLabel, instructions, historyButton, clearButton, statusLabel);

        Scene scene = new Scene(root, 600, 450);
        primaryStage.setTitle("Javalin History & Undo Viewer - Test App");
        primaryStage.setScene(scene);
        primaryStage.show();

        the_TextArea = new TextArea();
        the_TextArea.setEditable(false);
        the_TextArea.setWrapText(true);
        the_TextArea.setStyle("-fx-font-family: monospace; -fx-font-size: 11px;");

        Platform.runLater(() ->
            the_TextArea.appendText("Javalin History & Undo Viewer Test App\n" +
            "========================================\n\n" +
            "1. Click 'Show History' to see browser with folder paths\n" +
            "2. Click 'Show Undo History' to see browser with undo operations\n" +
            "3. The browser opens with a list - try scrolling, clicking, switching views\n\n" +
            "Server runs in background - no need to keep this app open!\n\n" +
            "Click 'Clear Browser' to close the browser window.\n\n" +
            "Press Ctrl+C to stop the test app.\n")
        );
    }

    private void showHistoryTest(Logger logger) {
        Platform.runLater(() -> {
            the_TextArea.appendText("\n=== Test 1: History ===\n");



            statusLabel.setText("Opening history viewer...");

            show_history(
                (Path clickedPath) -> {
                    Platform.runLater(() -> {
                        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                                .withZone(ZoneId.systemDefault());
                        String time = df.format(LocalDateTime.now());
                        the_TextArea.appendText("[" + time + "] History item clicked: " + clickedPath.toAbsolutePath() + "\n");
                        statusLabel.setText("History item clicked!");
                    });
                },
                logger
            );
        });
    }

    private final AtomicReference<List<History_item>> history_items = new AtomicReference<>(new ArrayList<>());
    private final AtomicReference<Consumer<History_item>> onItemClick = new AtomicReference<>(null);
    private final Set<WsContext> connected_clients = Collections.newSetFromMap(
            new ConcurrentHashMap<>()
    );
    
    public void show_history(Consumer<Path> on_click, Logger logger) {


        History_engine engine = History_engine.get(stage);

        List<History_item> all = engine.get_all_history_items();


        history_items.set(all);
        onItemClick.set(item -> {
            if (on_click != null) {
                try {
                    on_click.accept(Paths.get(item.value));
                } catch (Exception e) {
                    logger.log("Failed to convert path: " + e.getMessage());
                }
            }
        });

        Javalin_common.open_browser(this, true, "History", port_number, logger);
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
                    sendHistoryItems(ctx);
                });
                ws.onMessage(ctx ->
                {
                    String msg = ctx.message();

                    if ("REQUEST_INIT".equals(msg))
                    {
                        // Client is fresh and wants the current text
                        sendHistoryItems(ctx);
                        return;
                    }

                    if (msg.startsWith("SELECT:")) {
                        // Browser selected a path - trigger the click callback
                        String selectedPath = msg.substring("SELECT:".length());
                        logger.log("Selected path from browser: " + selectedPath);
                        handlePathClick(selectedPath);
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
    private void sendHistoryItems(WsContext ctx)
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
            String escapedValue = escapeJson(item.value);
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
    private void handlePathClick(String selectedPath)
    //**********************************************************
    {
        Consumer<History_item> callback = onItemClick.get();
        if (callback != null) {
            // Find the item with the selected path and trigger the callback
            List<History_item> items = history_items.get();
            if (items != null) {
                for (History_item item : items) {
                    if (item.value.equals(selectedPath)) {
                        callback.accept(item);
                        break;
                    }
                }
            }
        }
    }

    //**********************************************************
    private String escapeJson(String s)
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