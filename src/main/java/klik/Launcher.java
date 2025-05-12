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
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.look.Jar_utils;
import klik.look.Look_and_feel;
import klik.look.Look_and_feel_manager;
import klik.look.Look_and_feel_manager.Icon_type;
import klik.look.my_i18n.Language_manager;
import klik.look.my_i18n.My_I18n;
import klik.properties.Non_booleans;
import klik.util.Sys_init;
import klik.util.execute.Execute_command;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import klik.util.log.System_logger;
import klik.util.tcp.Session;
import klik.util.tcp.Session_factory;
import klik.util.tcp.TCP_server;
import klik.util.ui.Hourglass;
import klik.util.ui.Show_running_film_frame;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


//**********************************************************
public class Launcher extends Application
//**********************************************************
{

    public static final int WIDTH = 600;
    public static final int icon_size = 100;
    public static final String STARTED = "STARTED";
    public static double estimated_text_label_height;
    //**********************************************************
    @Override
    public void start(Stage stage) throws Exception
    //**********************************************************
    {

        Sys_init sys_init = Sys_init.get("Launcher");
        Logger logger = sys_init.logger();
        Aborter aborter = sys_init.aborter();
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


        AtomicInteger klik_port = new AtomicInteger(12345);
        AtomicInteger audio_player_port = new AtomicInteger(23456);
        {
            Button b = new Button(My_I18n.get_I18n_string("Launch_1_New_Klik_Application",logger));

            set_look(b, vbox,look_and_feel,Icon_type.IMAGE,logger);

            b.setOnAction(event -> {
                start_app_and_listen("klik", klik_port.getAndIncrement(),stage, logger);
            });
        }
        {
            Button b = new Button(My_I18n.get_I18n_string("Launch_Music_Player",logger));
            set_look(b, vbox,look_and_feel,Icon_type.MUSIC,logger);
            b.setOnAction(event -> {
                start_app_and_listen("audio_player", audio_player_port.getAndIncrement(),stage,logger);
            });
        }

        Scene scene = new Scene(vbox);
        stage.setTitle("Klik "+launcher);
        stage.setScene(scene);
        stage.show();
    }

    //**********************************************************
    private static void start_app_and_listen(String tag, int port, Stage stage, Logger logger)
    //**********************************************************
    {
        Hourglass hourglass = Show_running_film_frame.show_running_film(stage,100,100,"Please wait ... starting",20 * 1000,new Aborter("dummy", logger), logger);

        List<String> cmds = new ArrayList<>();
        cmds.add("gradle");
        cmds.add(tag);
        String arg =  "--args=\""+port+"\"";
        cmds.add(arg);

        Runnable r = () -> {
            StringBuilder sb = new StringBuilder();
            Execute_command.execute_command_list_no_wait(cmds, new File("."), 20 * 1000, sb, logger);
            logger.log(sb.toString());
        };
        Actor_engine.execute(r, logger);
        start_server_and_wait_for_reply(tag,port,hourglass, logger);
    }

    //**********************************************************
    private static boolean start_server_and_wait_for_reply(String tag, int port, Hourglass hourglass, Logger logger)
    //**********************************************************
    {
        // start the server to receive the "started" message and stop the hourglass
        // note the audio player, if already started, will send the "started" message
        // this is because in the launcher, after a audio_player
        // was started, it might have been killed ...

        Session_factory session_factory = () -> new Session() {
            @Override
            public void on_client_connection(DataInputStream dis, DataOutputStream dos)
            {
                try {
                    int size = dis.readInt();
                    byte buffer[] = new byte[size];
                    dis.read(buffer);
                    String msg = new String(buffer, StandardCharsets.UTF_8);
                   if ( msg.equals(STARTED))
                    {
                        logger.log("STARTED RECEIVED for: "+tag);
                        if ( hourglass != null) hourglass.close();
                    }

                }
                catch (IOException e)
                {
                    logger.log(Stack_trace_getter.get_stack_trace(""+e));
                }

            }

            @Override
            public String name() {
                return "launcher for app: "+tag;
            }
        };
        TCP_server tcp_server = new TCP_server(session_factory,new Aborter(tag, logger), logger);
        Runnable r = () -> tcp_server.start(port,false);
        Actor_engine.execute(r, logger);

        return true;
    }

    //**********************************************************
    private static void set_look(Button b, VBox vbox, Look_and_feel look_and_feel, Icon_type icon_type, Logger logger)
    //**********************************************************
    {
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
