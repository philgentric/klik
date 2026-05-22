package klikr.javalin.undos;

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
import klikr.change.old_and_new.Old_and_new_Path;
import klikr.change.undo.Undo_for_moves;
import klikr.change.undo.Undo_item;
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
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Unit-test application for viewing undo operations in a browser.
 * Shows a list of undo operations with their details.
 */

public class Javalin_undos_app extends Application {

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
        Shared_services.init("undos test app", primaryStage);
        logger = Shared_services.logger();
        port_number = Javalin_common.find_free_port(logger);
        stage = primaryStage;

        start_javalin_server();

        // Setup JavaFX UI
        VBox root = new VBox(10);
        root.setPadding(new Insets(20));

        Label titleLabel = new Label("Javalin Undo Viewer - Test App");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Label instructions = new Label(
            "Click buttons below to test:\n" +
            "1. Show undo history (sample operations)\n" +
            "2. View browser for results"
        );
        instructions.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

        Button undoButton = new Button("↩️ Show Undo History");
        undoButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-padding: 10 20;");
        undoButton.setOnAction(e -> showUndoTest(logger));

        Button clearButton = new Button("Clear Browser");
        clearButton.setOnAction(e -> {
            Platform.runLater(() -> {
                the_TextArea.clear();
                the_TextArea.appendText("Browser closed - server still running\n");
            });
        });

        statusLabel = new Label("Ready to test");
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #2ecc71;");

        root.getChildren().addAll(titleLabel, instructions, undoButton, clearButton, statusLabel);

        Scene scene = new Scene(root, 600, 450);
        primaryStage.setTitle("Javalin Undo Viewer - Test App");
        primaryStage.setScene(scene);
        primaryStage.show();

        the_TextArea = new TextArea();
        the_TextArea.setEditable(false);
        the_TextArea.setWrapText(true);
        the_TextArea.setStyle("-fx-font-family: monospace; -fx-font-size: 11px;");

        Platform.runLater(() ->
            the_TextArea.appendText("Javalin Undo Viewer Test App\n" +
            "============================\n\n" +
            "1. Click 'Show Undo History' to see browser with undo operations\n" +
            "2. The browser opens with a list - click items to view details\n\n" +
            "Server runs in background - no need to keep this app open!\n\n" +
            "Click 'Clear Browser' to close the browser window.\n\n" +
            "Press Ctrl+C to stop the test app.\n")
        );
    }

    private void showUndoTest(Logger logger) {
        Platform.runLater(() -> {
            the_TextArea.appendText("\n=== Test: Undo History ===\n");

            statusLabel.setText("Opening undo viewer...");

            show_undos(
                (Undo_item clickedItem) -> {
                    Platform.runLater(() -> {
                        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                                .withZone(ZoneId.systemDefault());
                        String time = df.format(LocalDateTime.now());
                        the_TextArea.appendText("[" + time + "] Undo item clicked\n");
                        the_TextArea.appendText(clickedItem.signature() + "\n");
                        statusLabel.setText("Undo item clicked!");
                    });
                },
                logger
            );
        });
    }

    private final AtomicReference<List<Undo_item>> undo_items = new AtomicReference<>(new ArrayList<>());
    private final AtomicReference<Consumer<Undo_item>> onItemClick = new AtomicReference<>(null);

    public void show_undos(Consumer<Undo_item> on_click, Logger logger) {
        // Generate some sample undo items for testing
        //List<Undo_item> items = generateSampleUndoItems(logger);

        Undo_for_moves ufm = Undo_for_moves.get_instance(stage, logger);
        Map<LocalDateTime, String> map = ufm.get_map_of_date_to_signature();
        List<LocalDateTime> keys = new ArrayList<>(map.keySet());
        Collections.sort(keys);
        Collections.reverse(keys);
        Map<String, Undo_item> mmm = ufm.get_signature_to_undo_item();
        List<Undo_item> undo_item_list = new ArrayList<>();
        for(LocalDateTime local_date_time_as_key : keys)
        {
            String signqture = map.get(local_date_time_as_key);
            Undo_item undo_item = mmm.get(signqture);
            undo_item_list.add(undo_item);

        }
        undo_items.set(undo_item_list);

        onItemClick.set(item -> {
            if (on_click != null) {
                on_click.accept(item);
            }
        });

        Javalin_common.open_browser(this, true, "Undo History", port_number, logger);
    }

    private List<Undo_item> generateSampleUndoItems(Logger logger) {
        List<Undo_item> items = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault());

        // Create various undo operations
        for (int i = 0; i < 20; i++) {
            List<Old_and_new_Path> oans = new ArrayList<>();

            Path oldPath = Paths.get("/Users/test/documents/work", "project_" + i, "file_" + i + ".txt");
            Path newPath = Paths.get("/Users/test/downloads/archive", "file_" + i + "_moved_" + i + ".txt");

            Old_and_new_Path oan = new Old_and_new_Path(
                oldPath,
                newPath,
                klikr.change.old_and_new.Command.command_move,
                null,
                false
            );

            oans.add(oan);

            Undo_item undoItem = new Undo_item(
                oans,
                LocalDateTime.now().minusHours(i * 2),
                UUID.randomUUID(),
                logger
            );

            items.add(undoItem);
        }

        // Add some items with deleted files (cannot be undone)
        for (int i = 0; i < 5; i++) {
            List<Old_and_new_Path> oans = new ArrayList<>();

            Path oldPath = Paths.get("/Users/test/documents/forever", "deleted_" + i + ".txt");
            Path newPath = null; // File was deleted forever

            Old_and_new_Path oan = new Old_and_new_Path(
                oldPath,
                newPath,
                klikr.change.old_and_new.Command.command_delete_forever,
                null,
                false
            );

            oans.add(oan);

            Undo_item undoItem = new Undo_item(
                oans,
                LocalDateTime.now().minusHours(50 + i),
                UUID.randomUUID(),
                logger
            );

            items.add(undoItem);
        }

        return items;
    }

    //**********************************************************
    private void start_javalin_server()
    //**********************************************************
    {
        CountDownLatch started = new CountDownLatch(1);
        Runnable r = () -> {
            javalin = Javalin.create(config ->
            {
                config.staticFiles.add(
                        "/undos",
                        io.javalin.http.staticfiles.Location.CLASSPATH
                );
            }).start(port_number);

            // WebSocket Endpoint
            javalin.ws("/monaco-ws", ws -> {
                ws.onConnect(ctx ->
                {
                    ctx.session.setIdleTimeout(Duration.ofMillis(3600000L)); // 1 hour timeout
                    logger.log("Javalin_undos WebSocket connected");

                    // Send current undo items to newly connected client
                    sendUndoItems(ctx);
                });
                ws.onMessage(ctx ->
                {
                    String msg = ctx.message();

                    if ("REQUEST_INIT".equals(msg))
                    {
                        sendUndoItems(ctx);
                        return;
                    }

                    if (msg.startsWith("SELECT:")) {
                        // Browser selected an undo item - trigger the click callback
                        String selectedId = msg.substring("SELECT:".length());
                        logger.log("Selected undo item from browser: " + selectedId);
                        handleUndoClick(selectedId);
                        return;
                    }

                    if ( ultra_dbg) logger.log("Received from browser: " + msg);

                });
                ws.onClose(ctx -> {
                    logger.log("Javalin_undos server disconnected");
                });
            });

            started.countDown();
        };

        Actor_engine.execute(r,"Javalin_undos server",logger);
        try {
            started.await();
        } catch (InterruptedException e) {
            logger.log("Javalin_undos server interrupted"+e);
            return;
        }
        logger.log("Javalin_undos server started on port " + port_number);
    }

    //**********************************************************
    private void sendUndoItems(WsContext ctx)
    //**********************************************************
    {
        List<Undo_item> items = undo_items.get();
        if (items == null || items.isEmpty()) {
            ctx.send("[]");
            return;
        }

        // Build JSON array of undo items
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            Undo_item item = items.get(i);
            if (i > 0) json.append(",");

            String escapedTimestamp = item.time_stamp.toString().replace("\"", "\\\"");

            // Build details from oans
            StringBuilder detailsBuilder = new StringBuilder();
            for (Old_and_new_Path oan : item.oans) {
                String oldPath = oan.old_Path != null ? escapeJson(oan.old_Path.toAbsolutePath().toString()) : "null";
                String newPath = oan.new_Path != null ? escapeJson(oan.new_Path.toAbsolutePath().toString()) : "null";
                String cmd = oan.cmd != null ? oan.cmd.toString() : "unknown";
                detailsBuilder.append(String.format("{\"old\":\"%s\",\"new\":\"%s\",\"cmd\":\"%s\"},",
                    oldPath, newPath, cmd));
            }
            if (detailsBuilder.length() > 0) {
                detailsBuilder.setLength(detailsBuilder.length() - 1); // Remove trailing comma
            }

            json.append(String.format(
                "{\"id\":\"%s\",\"timestamp\":\"%s\",\"details\":[%s],\"signature\":\"%s\"}",
                item.index.toString(),
                escapedTimestamp,
                detailsBuilder.toString(),
                escapeJson(item.signature())
            ));
        }
        json.append("]");
        ctx.send(json.toString());
    }

    //**********************************************************
    private void handleUndoClick(String selectedId)
    //**********************************************************
    {
        Consumer<Undo_item> callback = onItemClick.get();
        if (callback != null) {
            List<Undo_item> items = undo_items.get();
            if (items != null) {
                for (Undo_item item : items) {
                    if (item.index.toString().equals(selectedId)) {
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
