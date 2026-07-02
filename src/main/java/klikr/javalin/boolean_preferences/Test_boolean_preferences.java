package klikr.javalin.boolean_preferences;

import javafx.application.Application;
import javafx.stage.Stage;
import klikr.util.Shared_services;
import klikr.util.log.Logger;

/**
 * Test application for Boolean Preferences UI
 * Run this to test the web-based boolean preferences interface
 */
public class Test_boolean_preferences extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Initialize Shared_services - required for Feature_cache and My_I18n to work
        Shared_services.init("Boolean Preferences Test", primaryStage);
        Logger logger = Shared_services.logger();

        logger.log("Starting Boolean Preferences Test Application");

        // Show the boolean preferences UI
        Javalin_boolean_preferences.show(this, logger);

        // Keep the JavaFX application running
        primaryStage.setTitle("Boolean Preferences Test");
        primaryStage.setWidth(400);
        primaryStage.setHeight(200);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
