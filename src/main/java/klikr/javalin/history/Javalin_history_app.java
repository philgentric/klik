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
import klikr.Start_context;
import klikr.Window_type;
import klikr.change.history.History_engine;
import klikr.change.history.History_item;
import klikr.javalin.Javalin_common;
import klikr.util.Shared_services;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.http.Klikr_communicator;
import klikr.util.log.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Unit-test application for Javalin_history_and_undo
 * Demonstrates both history and undo list viewing in browser.
 */

public class Javalin_history_app extends Application {

    private static final boolean ultra_dbg = true;
    private TextArea the_TextArea;
    private Label statusLabel;
    private Javalin javalin;
    private int port_number;
    Stage stage;
    Logger logger;
    Javalin_history_server javalin_history_server;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primary_stage) {

        Shared_services.init("history test app",primary_stage);
        logger = Shared_services.logger();

        stage = primary_stage;
        Start_context context = Start_context.get_context_and_args(this);
        Klikr_communicator.build(context,primary_stage,logger);

        javalin_history_server = new Javalin_history_server(this,
                null, Window_type.File_system_2D,primary_stage,Shared_services.aborter(),logger);


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
        primary_stage.setTitle("Javalin History Viewer - Test App");
        primary_stage.setScene(scene);
        primary_stage.show();

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
        });
    }

    


}