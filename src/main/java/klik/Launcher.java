package klik;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import klik.look.Jar_utils;
import klik.look.Look_and_feel;
import klik.look.Look_and_feel_manager;
import klik.look.Look_and_feel_manager.Icon_type;
import klik.look.my_i18n.Language_manager;
import klik.look.my_i18n.My_I18n;
import klik.properties.Non_booleans;
import klik.util.execute.Execute_command;
import klik.util.log.Logger;
import klik.util.log.System_logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


//**********************************************************
public class Launcher extends Application
//**********************************************************
{

    public static final int WIDTH = 600;
    public static final int icon_size = 100;
    public static double estimated_text_label_height;
    //**********************************************************
    @Override
    public void start(Stage stage) throws Exception
    //**********************************************************
    {

        Logger logger = System_logger.get_system_logger("Launcher");
        Look_and_feel_manager.init_Look_and_feel(logger);
        VBox vbox = new VBox();
        vbox.setAlignment(Pos.CENTER);
        Look_and_feel_manager.set_region_look(vbox);
        double font_size = Non_booleans.get_font_size(logger);
        estimated_text_label_height = klik.look.Look_and_feel.MAGIC_HEIGHT_FACTOR*font_size;

        Language_manager.init_registered_languages(logger);

        String launcher = My_I18n.get_I18n_string(Look_and_feel_manager.LAUNCHER,logger);

        Look_and_feel_manager.set_icon_for_main_window(stage, launcher, Icon_type.LAUNCHER);

        Look_and_feel look_and_feel = Look_and_feel_manager.get_instance();



        {
            Button b = new Button(My_I18n.get_I18n_string("Launch_1_New_Klik_Application",logger));

            set_look(b, vbox,look_and_feel,Icon_type.IMAGE,logger);

            b.setOnAction(event -> {

                List<String> cmds = new ArrayList<>();
                cmds.add("gradle");
                cmds.add("klik");

                StringBuilder sb = new StringBuilder();
                Execute_command.execute_command_list_no_wait(cmds, new File("."), 20 * 1000, sb, logger);
                logger.log(sb.toString());

            });
        }
        {
            Button b = new Button(My_I18n.get_I18n_string("Launch_Music_Player",logger));
            set_look(b, vbox,look_and_feel,Icon_type.MUSIC,logger);
            b.setOnAction(event -> {

                List<String> cmds = new ArrayList<>();
                cmds.add("gradle");
                cmds.add("audio_player");

                StringBuilder sb = new StringBuilder();
                Execute_command.execute_command_list_no_wait(cmds, new File("."), 20 * 1000, sb, logger);
                logger.log(sb.toString());

            });
        }



        Scene scene = new Scene(vbox);
        stage.setTitle("Klik "+launcher);
        stage.setScene(scene);
        stage.show();
    }

    private static void set_look(Button b, VBox vbox, Look_and_feel look_and_feel, Icon_type icon_type, Logger logger) {
        Look_and_feel_manager.set_button_look(b, true);
        b.setPrefWidth(WIDTH);
        b.setAlignment(Pos.CENTER);
        b.setTextAlignment(TextAlignment.CENTER);
        b.setMnemonicParsing(false);
        vbox.getChildren().add(b);
        FlowPane the_image_pane = new FlowPane();//)StackPane();
        the_image_pane.setAlignment(Pos.BOTTOM_CENTER);
        the_image_pane.setMinWidth(icon_size);
        the_image_pane.setMaxWidth(icon_size);
        the_image_pane.setMinHeight(icon_size);
        the_image_pane.setMaxHeight(icon_size);
        b.setGraphic(the_image_pane);
        b.setContentDisplay(ContentDisplay.BOTTOM);
        ImageView the_image_view = new ImageView();
        the_image_pane.getChildren().add(the_image_view);

        String icon_path = Look_and_feel_manager.get_main_window_icon_path(look_and_feel ,icon_type);
        Image icon = Jar_utils.load_jfx_image_from_jar(icon_path, icon_size, logger);

        the_image_view.setImage(icon);
        the_image_view.setPreserveRatio(true);

        double h = icon_size+ estimated_text_label_height;
        b.setPrefHeight(h);
        b.setMinHeight(h);
        b.setMaxHeight(h);
    }
}
