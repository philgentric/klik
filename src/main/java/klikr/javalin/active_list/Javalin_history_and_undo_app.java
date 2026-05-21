package klikr.javalin.active_list;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import klikr.change.undo.Undo_item;
import klikr.change.old_and_new.Old_and_new_Path;
import klikr.util.log.Logger;
import klikr.util.log.Simple_logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Unit-test application for Javalin_history_and_undo
 * Demonstrates both history and undo list viewing in browser.
 */

public class Javalin_history_and_undo_app extends Application {
    private TextArea the_TextArea;
    private WebView webView;
    private Label statusLabel;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        Logger logger = new Simple_logger();

        // 1. Setup JavaFX UI
        VBox root = new VBox(10);
        root.setPadding(new Insets(20));

        Label titleLabel = new Label("Javalin History & Undo Viewer - Test App");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Label instructions = new Label(
            "Click buttons below to test:\n" +
            "1. Show history (sample paths)\n" +
            "2. Show undo (sample undo operations)\n" +
            "3. View browser for results"
        );
        instructions.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

        Button historyButton = new Button("🚀 Show History");
        historyButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-padding: 10 20;");
        historyButton.setOnAction(e -> showHistoryTest(logger));

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

        root.getChildren().addAll(titleLabel, instructions, historyButton, undoButton, clearButton, statusLabel);

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

            // Generate sample history items
            List<Path> paths = new ArrayList<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault());

            for (int i = 0; i < 50; i++) {
                Path path = Paths.get("/Users/test/documents/work", "project_" + i, "file_" + i + ".txt");
                paths.add(path);
            }

            statusLabel.setText("Opening history viewer...");

            Javalin_for_history_and_undo.show_history(
                this,
                paths,
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

    private void showUndoTest(Logger logger) {
        Platform.runLater(() -> {
            the_TextArea.appendText("\n=== Test 2: Undo History ===\n");

            // Generate sample undo items
            List<Undo_item> undoItems = new ArrayList<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault());

            for (int i = 0; i < 30; i++) {
                List<Old_and_new_Path> oans = new ArrayList<>();

                // Create sample old and new paths
                Path oldPath = Paths.get("/Users/test/documents/work/project", "file_" + i + ".txt");
                Path newPath = Paths.get("/Users/test/downloads/archive", "file_" + i + "_moved_" + i + ".txt");

                Old_and_new_Path oan = new Old_and_new_Path(oldPath, newPath,
                        klikr.change.old_and_new.Command.command_move, null, false);

                oans.add(oan);

                Undo_item undoItem = new Undo_item(
                    oans,
                    LocalDateTime.now().minusHours(i),
                    UUID.randomUUID(),
                    logger
                );

                undoItems.add(undoItem);
            }

            statusLabel.setText("Opening undo viewer...");

            Javalin_for_history_and_undo.show_undo(
                this,
                undoItems,
                (Undo_item clickedUndo) -> {
                    Platform.runLater(() -> {
                        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                                .withZone(ZoneId.systemDefault());
                        String time = df.format(LocalDateTime.now());
                        the_TextArea.appendText("[" + time + "] Undo item clicked: " + clickedUndo.signature() + "\n");
                        statusLabel.setText("Undo item clicked!");
                    });
                },
                logger
            );
        });
    }
}