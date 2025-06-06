package klik;

import javafx.application.Application;
import javafx.application.Platform;
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
import klik.audio.Audio_player_application;
import klik.browser.Shared_services;
import klik.look.Jar_utils;
import klik.look.Look_and_feel;
import klik.look.Look_and_feel_manager;
import klik.look.Look_and_feel_manager.Icon_type;
import klik.look.my_i18n.My_I18n;
import klik.properties.Non_booleans;
import klik.properties.features.String_change_target;
import klik.util.Sys_init;
import klik.util.execute.Execute_command;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import klik.util.tcp.*;
import klik.util.ui.Hourglass;
import klik.util.ui.Popups;
import klik.util.ui.Show_running_film_frame;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


//**********************************************************
public class Launcher extends Application
//**********************************************************
{

    public static final int WIDTH = 600;
    public static final int icon_size = 100;
    public static final String STARTED = "STARTED";
    public static final String LANGUAGE_CHANGED = "LANGUAGE_CHANGED";
    public static double estimated_text_label_height;

    private Stage stage;
    private Logger logger;
    private VBox vbox;
    private AtomicInteger klik_port;
    private AtomicInteger audio_player_port;

    //**********************************************************
    @Override
    public void start(Stage stage_) throws Exception
    //**********************************************************
    {
        stage = stage_;
        Sys_init.init("Launcher app");
        logger = Shared_services.shared_services_logger;


        Look_and_feel_manager.init_Look_and_feel(logger);
        vbox = new VBox();
        vbox.setAlignment(Pos.CENTER);
        Look_and_feel_manager.set_region_look(vbox);
        double font_size = Non_booleans.get_font_size(logger);
        estimated_text_label_height = klik.look.Look_and_feel.MAGIC_HEIGHT_FACTOR*font_size;

        String launcher = My_I18n.get_I18n_string(Look_and_feel_manager.LAUNCHER,logger);

        Look_and_feel_manager.set_icon_for_main_window(stage, launcher, Icon_type.LAUNCHER);



        klik_port = new AtomicInteger(12345);
        audio_player_port = new AtomicInteger(23456);
        define_UI( );


        Scene scene = new Scene(vbox);
        stage.setTitle("Klik "+launcher);
        stage.setScene(scene);
        stage.show();
    }

    //**********************************************************
    private void define_UI()
    //**********************************************************
    {
        Look_and_feel look_and_feel = Look_and_feel_manager.get_instance();

        vbox.getChildren().clear();
        {
            Button b = new Button(My_I18n.get_I18n_string("Launch_1_New_Klik_Application", logger));

            set_look(b, vbox, look_and_feel,Icon_type.IMAGE, logger);

            b.setOnAction(event -> {
                start_app_and_listen(this,"klik", klik_port.getAndIncrement(), stage, logger);
            });
        }
        {
            Button b = new Button(My_I18n.get_I18n_string("Launch_Music_Player", logger));
            set_look(b, vbox, look_and_feel,Icon_type.MUSIC, logger);
            b.setOnAction(event -> {
                start_app_and_listen(this,"audio_player", audio_player_port.getAndIncrement(), stage, logger);
            });
        }
        {
            Button b = new Button("Show version");
            set_look(b, vbox, look_and_feel,null, logger);
            b.setOnAction(event -> {
                show_version(stage, logger);
            });
        }
        {
            Button b = new Button(My_I18n.get_I18n_string("Get_Most_Recent_Version", logger));
            set_look(b, vbox, look_and_feel,null, logger);
            b.setOnAction(event -> {
                get_most_recent_version(stage, logger);
            });
        }
    }

    //**********************************************************
    private void show_version(Stage stage, Logger logger)
    //**********************************************************
    {
        Hourglass hourglass = Show_running_film_frame.show_running_film(stage,100,100,"Please wait ... starting",20 * 1000,new Aborter("dummy", logger), logger);

        String version_string = get_version_string(logger);
        logger.log("version: "+version_string);

        Popups.simple_alert("version is "+version_string);

        hourglass.close();
    }

    //**********************************************************
    private static String get_version_string(Logger logger)
    //**********************************************************
    {
        String version =get_version_from_gradle_build(logger);
        String commit_count =get_commit_count(logger);
        String version_string = version+"."+commit_count;
        return version_string;
    }

    //**********************************************************
    private static String get_commit_count(Logger logger)
    //**********************************************************
    {
        List<String> cmds = new ArrayList<>();
        cmds.add("git");
        cmds.add("rev-list");
        cmds.add("--count");
        cmds.add("HEAD");

        StringBuilder sb = null;//new StringBuilder();
        String commit_count = Execute_command.execute_command_list(cmds, new File("."), 20 * 1000, sb, logger);
        //logger.log(sb.toString());
        return commit_count;
    }

    //**********************************************************
    private static String get_version_from_gradle_build(Logger logger)
    //**********************************************************
    {
        List<String> cmds = new ArrayList<>();
        cmds.add("grep");
        cmds.add("version");
        cmds.add("build.gradle");

        StringBuilder sb = null;//new StringBuilder();
        String commit_count = Execute_command.execute_command_list(cmds, new File("."), 20 * 1000, sb, logger);
        String[] lines = commit_count.split("\n");
        for ( String s : lines)
        {
            if ( s.contains("application_version"))
            {
                // line is : version = "1.0" // application_version
                String[] parts = s.split("=");
                if ( parts.length == 2)
                {
                    // remove the end of the line:
                    // "1.0" // application_version
                    String[] parts2 = parts[1].split("//");
                    return parts2[0].trim().replaceAll("\"","");
                }
            }
        }
        return null;
    }



    //**********************************************************
    private void get_most_recent_version(Stage stage, Logger logger)
    //**********************************************************
    {
        Hourglass hourglass = Show_running_film_frame.show_running_film(stage,100,100,"Please wait ... starting",20 * 1000,new Aborter("dummy", logger), logger);

        if (Popups.popup_ask_for_confirmation(stage,"Are you sure you want to get the most recent version?","Developers: This will stash changes you made (if you made any changes),\n switch to the master branch (if you are on a different one)\nand get the most recent version from the repository\n\nIf you are not a developer, this is transparent, you just get the last and best",logger))
        {
            logger.log("version before:"+get_version_string(logger));
            {
                List<String> cmds = new ArrayList<>();
                cmds.add("git");
                cmds.add("stash");
                StringBuilder sb = new StringBuilder();
                Execute_command.execute_command_list(cmds, new File("."), 20 * 1000, sb, logger);
                logger.log(sb.toString());
            }
            {
                List<String> cmds = new ArrayList<>();
                cmds.add("git");
                cmds.add("checkout");
                cmds.add("master");

                StringBuilder sb = new StringBuilder();
                Execute_command.execute_command_list(cmds, new File("."), 20 * 1000, sb, logger);
                logger.log(sb.toString());
            }
            {
                List<String> cmds = new ArrayList<>();
                cmds.add("git");
                cmds.add("pull");

                StringBuilder sb = new StringBuilder();
                Execute_command.execute_command_list(cmds, new File("."), 20 * 1000, sb, logger);
                logger.log(sb.toString());
            }
            logger.log("version after:"+get_version_string(logger));
        }
        hourglass.close();
    }

    //**********************************************************
    private static void start_app_and_listen(Launcher launcher, String app_name, int port, Stage stage, Logger logger)
    //**********************************************************
    {
        Hourglass hourglass = Show_running_film_frame.show_running_film(stage,100,100,"Please wait ... starting",20 * 1000,new Aborter("dummy", logger), logger);

        List<String> cmds = new ArrayList<>();
        cmds.add("gradle");
        cmds.add(app_name);
        String arg =  "--args=\""+port+"\"";
        cmds.add(arg);

        AtomicBoolean failed = new AtomicBoolean(false);
        Runnable r = () -> {
            StringBuilder sb = new StringBuilder();
            Execute_command.execute_command_list_no_wait(cmds, new File("."), 20 * 1000, sb, logger);
            logger.log(sb.toString());
            if ( sb.toString().contains("BUILD FAILED"))
            {
                // this does not work because execute_command_list_no_wait does not write anything into the sb
                logger.log("\n\n\n\n\n\nBUILD FAILED\n\n\n\n\n\n");
                Platform.runLater(() -> Popups.simple_alert("BUILD FAILED"));
                failed.set(true);
                Start_context.send_started_raw(port,logger); // this is to unblock the hourglass
            }
        };
        Actor_engine.execute(r, logger);
        start_server_and_wait_for_reply(app_name,port,hourglass, launcher,logger);
    }

    //**********************************************************
    private static boolean start_server_and_wait_for_reply(String app_name, int port, Hourglass hourglass, Launcher launcher, Logger logger)
    //**********************************************************
    {
        // start the server to receive the "started" error_message and stop the hourglass
        // note the audio player, if already started, will send the "started" error_message
        // this is because in the launcher, after a audio_player
        // was started, it might have been killed ...

        logger.log("start_server_and_wait_for_reply for: "+app_name);
        Session_factory session_factory = () -> new Session() {
            @Override
            public void on_client_connection(DataInputStream dis, DataOutputStream dos)
            {
                try {
                    String msg = TCP_util.read_string(dis);
                    if ( msg.equals(STARTED))
                    {
                        logger.log("Launcher: STARTED RECEIVED for: "+app_name);
                        if ( hourglass != null) hourglass.close();
                    }
                    if (msg.startsWith(LANGUAGE_CHANGED))
                    {
                        Non_booleans.force_reload_from_disk();
                        String new_lang = msg.split(" ")[1];
                        logger.log("Launcher: LANGUAGE_CHANGED RECEIVED for: "+app_name+ " new lang is "+new_lang);
                        logger.log("Launcher: checking the language value is updated on disk: "+Non_booleans.get_language_key());
                        My_I18n.reset();
                        Platform.runLater(() -> launcher.define_UI());
                        // broken in the audio player: TCP_client.request("localhost", Audio_player_application.AUDIO_PLAYER_PORT,msg,logger);
                    }
                }
                catch (IOException e)
                {
                    logger.log(Stack_trace_getter.get_stack_trace(""+e));
                }

            }

            @Override
            public String name() {
                return "launcher for app: "+app_name;
            }
        };
        TCP_server tcp_server = new TCP_server(session_factory,new Aborter(app_name, logger), logger);
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
        if ( icon_type != null)
        {
            FlowPane the_image_pane = new FlowPane();
            the_image_pane.setAlignment(Pos.BOTTOM_CENTER);
            the_image_pane.setMinWidth(icon_size);
            the_image_pane.setMaxWidth(icon_size);
            the_image_pane.setMinHeight(icon_size);
            the_image_pane.setMaxHeight(icon_size);
            b.setGraphic(the_image_pane);
            b.setContentDisplay(ContentDisplay.BOTTOM);
            ImageView the_image_view = new ImageView();
            the_image_pane.getChildren().add(the_image_view);


            String icon_path = Look_and_feel_manager.get_main_window_icon_path(look_and_feel, icon_type);
            Image icon = Jar_utils.load_jfx_image_from_jar(icon_path, icon_size, logger);

            the_image_view.setImage(icon);
            the_image_view.setPreserveRatio(true);

            double h = icon_size + estimated_text_label_height;
            b.setPrefHeight(h);
            b.setMinHeight(h);
            b.setMaxHeight(h);
        }
    }

}
