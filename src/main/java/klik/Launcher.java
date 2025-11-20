// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik;

//SOURCES ./audio/Audio_player_access.java
//SOURCES ./image_ml/ML_servers_util.java
//SOURCES ./image_ml/Embeddings_servers_monitor.java
//SOURCES ./image_ml/Embeddings_servers_monitoring_stage.java
//SOURCES ./util/execute/Execute_via_script_in_tmp_file.java

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import klik.properties.boolean_features.Preferences_stage;
import klik.util.execute.Execute_result;
import klik.util.execute.Guess_OS;
import klik.util.execute.actor.Aborter;
import klik.util.execute.actor.Actor_engine;
import klik.audio.Audio_player_access;
import klik.machine_learning.ML_servers_util;
import klik.look.Look_and_feel;
import klik.look.Look_and_feel_manager;
import klik.look.Look_and_feel_manager.Icon_type;
import klik.look.my_i18n.My_I18n;
import klik.properties.Non_booleans_properties;
import klik.properties.boolean_features.Booleans;
import klik.properties.boolean_features.Feature;
import klik.util.execute.Execute_command;
import klik.util.execute.Execute_via_script_in_tmp_file;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import klik.util.tcp.*;
import klik.util.ui.progress.Hourglass;
import klik.util.ui.Popups;
import klik.util.ui.progress.Progress_window;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

// the launcher can start applications (the image browser klik, the audio player)
// they are started as new processes
// (using a call to gradle to start a new JVM
// or a native application if compiled with gluon
// this may mean that the application may be recompiled
// before launching, if the code has changed)
//
// the launcher passes a reply port number to the application which has 2 uses:
// 1. at start time, the application can send a message to the launcher to say it has started, or not started
// 2. at any time, the application can send a message to the launcher to say that the UI has changed
//
// to enforce the fact that there is only 1 instance of the music player, the launcher has to rely on
// trying to start a new music player instance: if there is already a music player running,
// the music player will fail when it tries to attach a server on the audio player port (port 34539),
// (which the audio player listen to for requests to play songs or playlists),
// then it sends a NOT_STARTED message to the launcher, and the launcher pops up a warning..
//
// launching a new klik instance is not supposed to fail (it may not be a good idea in the sense that
// klik can have as many windows as one wants, but launching a new instance of klik has advantages;
// for example it is immune to the agressive use of ESC by the user)
//
// UI changes are originating from one klik browser instance, they are sent to the launcher,
// which then propagates them to the audio player, on the audio player port

//**********************************************************
public class Launcher extends Application implements UI_change
//**********************************************************
{
    // set gluon to true to compile native with gluon
    public static final boolean gluon = false;
    private final static String name = "Launcher";
    public static final int WIDTH = 600;
    public static final int icon_size = 100;
    public static final String STARTED = "STARTED";
    public static final String NOT_STARTED = "NOT_STARTED";
    private static boolean use_script_in_tmp;

    private Stage stage;
    private Aborter aborter;

    private Logger logger;
    private HBox  main = new HBox();

    private static ConcurrentLinkedQueue<Integer> propagate_to = new ConcurrentLinkedQueue<>();


    //**********************************************************
    public static void main(String[] args)
    //**********************************************************
    {
        launch(args);
    }

    //**********************************************************
    @Override
    public void start(Stage stage_) throws Exception
    //**********************************************************
    {
        stage = stage_;
        Shared_services.init(name, stage_);
        logger = Shared_services.logger();
        aborter = Shared_services.aborter();

        logger.log("Launcher starting");
        System_info.print();

        switch(Guess_OS.guess(stage,logger))
        {
            case Windows -> {
                use_script_in_tmp = true;
            }
            default -> {use_script_in_tmp = false;}

        }


        String launcher = My_I18n.get_I18n_string(Look_and_feel_manager.LAUNCHER,stage,logger);
        Look_and_feel_manager.set_icon_for_main_window(stage, launcher, Icon_type.LAUNCHER,stage,logger);

        Scene scene = new Scene(main);
        define_UI();


        int ui_change_listening_port = UI_change.start_UI_change_server(propagate_to,this,"Launcher",aborter,stage,logger);


        write_UI_change_listening_port_to_file(ui_change_listening_port,logger);
        stage.setTitle("Klik "+launcher);
        stage.setScene(scene);
        stage.show();
        stage.requestFocus(); // trying to make sure it comes on top




        long current = Non_booleans_properties.get_java_VM_max_RAM(stage,logger);

        if ( current > 0.8*System_info.get_total_machine_RAM_in_GBytes() )
        {
            // not realistic
            use_default_max_RAM(stage,logger);
            return;

        }
        if ( current == 1 )
        {
            // stupid default
            use_default_max_RAM(stage,logger);
            return;
        }
        if (Booleans.get_boolean(Feature.max_RAM_is_defined_by_user.name()))
        {
            logger.log("Using the max RAM defined by the user: "+current+" GBytes");
        }
        else
        {
            use_default_max_RAM(stage,logger);
        }


    }

    //**********************************************************
    private void use_default_max_RAM(Stage stage, Logger logger)
    //**********************************************************
    {
        long current = System_info.get_total_machine_RAM_in_GBytes();
        current = (current * 8) / 10; // use 80% of the physical RAM
        if ( current < 1) current = 1; // minimum 1GB
        Non_booleans_properties.save_java_VM_max_RAM((int)current, stage, logger);
        logger.log("Setting the max RAM to 80% of the physical RAM on this machine: "+current+" GBytes");
    }

    //**********************************************************
    @Override // UI_change
    public void define_UI()
    //**********************************************************
    {
        logger.log("Launcher define_UI");
        Look_and_feel look_and_feel = Look_and_feel_manager.get_instance(stage,logger);

        Look_and_feel_manager.set_region_look(main,stage,logger);

        main.getChildren().clear();
        VBox left = new VBox();
        main.getChildren().add(left);
        VBox right = new VBox();
        main.getChildren().add(right);

        {
            {
                Button b = new Button(My_I18n.get_I18n_string("Launch_1_New_Klik_Application", stage, logger));
                left.getChildren().add(b);
                look_and_feel.set_Button_look(b, WIDTH, icon_size, Icon_type.IMAGE, stage, logger);
                b.setOnAction(event -> {
                    if (Launcher.gluon) {
                        start_app_with_gradle_and_listen("nativeRun", stage, logger);
                    } else {
                        start_app_with_gradle_and_listen("klik", stage, logger);
                    }
                });
            }
            {
                Button b = new Button(My_I18n.get_I18n_string("Launch_Music_Player", stage, logger));
                right.getChildren().add(b);
                look_and_feel.set_Button_look(b, WIDTH, icon_size, Icon_type.MUSIC, stage, logger);
                b.setOnAction(event -> {
                    start_app_with_gradle_and_listen("audio_player", stage, logger);
                    propagate_to.add(Audio_player_access.AUDIO_PLAYER_PORT);
                });
            }

        }
        {
            Separator separator = new Separator();
            left.getChildren().add(separator);
            right.getChildren().add(separator);
        }

        {
            String key = "Start_Image_Similarity_Servers";
            EventHandler<ActionEvent> handler =e -> Execute_via_script_in_tmp_file.execute(ML_servers_util.get_command_string_to_start_image_similarity_servers(logger), false,true,stage, logger);
            HBox hb = Preferences_stage.make_hbox_with_button_and_explanation(
                    key,
                    handler,
                    WIDTH,
                    icon_size,
                    look_and_feel,
                    stage,
                    logger);
            left.getChildren().add(hb);
        }
        {
            String key = "Stop_Image_Similarity_Servers";
            EventHandler<ActionEvent> handler =e -> Execute_via_script_in_tmp_file.execute(ML_servers_util.get_command_string_to_stop_image_similarity_servers(logger), false,true,stage, logger);
            HBox hb = Preferences_stage.make_hbox_with_button_and_explanation(
                    key,
                    handler,
                    WIDTH,
                    icon_size,
                    look_and_feel,
                    stage,
                    logger);
            left.getChildren().add(hb);
        }


        {
            Separator separator = new Separator();
            left.getChildren().add(separator);
        }


        {
            String key = "Start_Face_Recognition_Servers";
            EventHandler<ActionEvent> handler =e -> Execute_via_script_in_tmp_file.execute(ML_servers_util.get_command_string_to_start_face_recognition_servers(logger), false,true,stage, logger);
            HBox hb = Preferences_stage.make_hbox_with_button_and_explanation(
                key,
                handler,
                WIDTH,
                icon_size,
                look_and_feel,
                stage,
                logger);
            left.getChildren().add(hb);
        }

        {
            String key = "Stop_Face_Recognition_Servers";
            EventHandler<ActionEvent> handler =e -> Execute_via_script_in_tmp_file.execute(ML_servers_util.get_command_string_to_stop_face_recognition_servers(logger), false,true,stage, logger);
            HBox hb = Preferences_stage.make_hbox_with_button_and_explanation(
                key,
                handler,
                WIDTH,
                icon_size,
                look_and_feel,
                stage,
                logger);
            left.getChildren().add(hb);
        }

        {
            Separator separator = new Separator();
            left.getChildren().add(separator);
        }
        {
            String key = "Show_Version";
            EventHandler<ActionEvent> handler =e -> show_version(stage, logger);
            HBox hb = Preferences_stage.make_hbox_with_button_and_explanation(
                    key,
                    handler,
                    WIDTH,
                    icon_size,
                    look_and_feel,
                    stage,
                    logger);
            left.getChildren().add(hb);
        }
        {
            String key = "Get_Most_Recent_Version";
            EventHandler<ActionEvent> handler =e -> get_most_recent_version(stage, logger);
            HBox hb = Preferences_stage.make_hbox_with_button_and_explanation(
                    key,
                    handler,
                    WIDTH,
                    icon_size,
                    look_and_feel,
                    stage,
                    logger);
            left.getChildren().add(hb);
        }

        {
            {
                String key = "Install_All_Tools";
                EventHandler<ActionEvent> handler = e -> install_all(stage, logger);
                HBox hb = Preferences_stage.make_hbox_with_button_and_explanation(
                        key,
                        handler,
                        WIDTH,
                        icon_size,
                        look_and_feel,
                        stage,
                        logger);
                right.getChildren().add(hb);
            }

            {
                String key = "Install_Python_Libs_For_ML";
                EventHandler<ActionEvent> handler = e -> ML_servers_util.install_python_libs_for_ML(stage,logger);
                HBox hb = Preferences_stage.make_hbox_with_button_and_explanation(
                        key,
                        handler,
                        WIDTH,
                        icon_size,
                        look_and_feel,
                        stage,
                        logger);
                right.getChildren().add(hb);
            }
            for (External_application app : External_application.values())
            {
                HBox hb = app.get_button(WIDTH, icon_size, look_and_feel, stage, logger);
                right.getChildren().add(hb);
            }
        }
    }

    //**********************************************************
    private void install_all(Window owner, Logger logger)
    //**********************************************************
    {
       Actor_engine.execute(()->install_all_in_a_thread(owner,logger),"Installing all tools",logger);
    }
    //**********************************************************
    private void install_all_in_a_thread(Window owner, Logger logger)
    //**********************************************************
    {
        ML_servers_util.install_python_libs_for_ML(owner,logger);
        for (External_application app : External_application.values())
        {
            String cmd = app.get_command_string_to_install(owner,logger);
            if ( cmd == null) continue;
            Execute_via_script_in_tmp_file.execute(cmd, true, false,owner, logger);
        }
    }

    //**********************************************************
    private void show_version(Window owner, Logger logger)
    //**********************************************************
    {
        Hourglass local_hourglass = Progress_window.show(
                false,
                "Please wait ... getting version",
                30*60,
                owner.getX()+100,
                owner.getY()+100,
                owner,
                logger);

        String version_string = get_version_string(logger);
        logger.log("version: "+version_string);

        Popups.simple_alert("version is "+version_string,owner,logger);

        local_hourglass.close();
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

        Execute_result res = Execute_command.execute_command_list(cmds, new File("."), 20 * 1000, null, logger);
        if ( !res.status())
        {
            logger.log("❗Warning cannot get commit count, is git installed ?");
            return "❗Warning cannot get commit count, is git installed ?";
        }
        String commit_count = res.output();
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
        Execute_result res = Execute_command.execute_command_list(cmds, new File("."), 20 * 1000, sb, logger);
        if ( !res.status())
        {
            logger.log("❗Warning cannot get version from build.gradle");
            return "❗Warning cannot get version from build.gradle";
        }
        String version_string = res.output();
        String[] lines = version_string.split("\n");
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
        Hourglass local_hourglass = Progress_window.show(
                false,
                "Please wait ... getting version",
                30*60,
                stage.getX()+100,
                stage.getY()+100,
                stage,
                logger);


        if (Popups.popup_ask_for_confirmation("❗ Are you sure you want to get the most recent version?","Developers: This will stash changes you made (if you made any changes),\n switch to the master branch (if you are on a different one)\nand get the most recent version from the repository\n\nIf you are not a developer, this is transparent, you just get the last and best, but of course, things need to be restarted for changes to take effect",stage,logger))
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
        local_hourglass.close();
    }

    //**********************************************************
    private static void start_app_with_gradle_and_listen(
            String app_name,
            Stage stage, Logger logger)
    //**********************************************************
    {
        Hourglass local_hourglass = Progress_window.show(
                false,
                "Please wait ... starting "+app_name,
                30*60,
                stage.getX()+100,
                stage.getY()+100,
                stage,
                logger);

        int port_to_reply_about_start = start_launch_status_server(app_name, local_hourglass, stage,logger);
        write_port_to_reply_about_start(port_to_reply_about_start,logger);

        if ( use_script_in_tmp)
        {
            String cmd = "gradle "+app_name;
            Execute_via_script_in_tmp_file.execute(cmd,false, true,stage,logger);
        }
        else
        {
            List<String> cmds = new ArrayList<>();
            cmds.add("gradle");
            cmds.add(app_name);
            Actor_engine.execute(
                    () -> Execute_command.execute_command_list_no_wait(cmds, new File("."), logger),
                    "gradle " + app_name,
                    logger);
        }

    }



    //**********************************************************
    private static int start_launch_status_server(String app_name, Hourglass local_hourglass, Window owner, Logger logger)
    //**********************************************************
    {
        // start the server to receive the "started" or "not_started" error_message
        // and in any case, stop the hourglass

        logger.log("Launcher: start_server_and_wait_for_reply for: "+app_name);
        Session_factory session_factory = () -> new Session() {
            @Override
            public boolean on_client_connection(DataInputStream dis, DataOutputStream dos)
            {
                try {
                    close_hourglass(local_hourglass);
                    String msg = TCP_util.read_string(dis);
                    if ( msg.equals(STARTED))
                    {
                        logger.log("Launcher: STARTED RECEIVED from: "+app_name);
                        return false; // and stop the server
                    }
                    if ( msg.equals(NOT_STARTED))
                    {
                        logger.log("Launcher: NOT_STARTED RECEIVED from: "+app_name);
                        if ( app_name.equals("audio_player"))
                        {
                            Platform.runLater(()->Popups.popup_warning("❗ Warning",app_name+" could not be started.\n" +
                                    "Since there is already another instance playing", false,owner, logger));

                        }
                        else
                        {
                            logger.log("❌ Launcher: NOT_STARTED received for "+app_name+", this should not happen");
                        }
                        return false; // and stop the server
                    }
                }
                catch (IOException e)
                {
                    logger.log(Stack_trace_getter.get_stack_trace(""+e));
                }

                return false; // and stop the server
            }

            @Override
            public String name() {
                return "launcher for app: "+app_name;
            }
        };
        TCP_server tcp_server = new TCP_server(session_factory,new Aborter(app_name, logger), logger);
        int local_port = tcp_server.start_zero("(ephemeral) listening for 'started' or 'not started' application reply upon launch",false);
        if (local_port < 1)
        {
            logger.log("Launcher: ERROR starting TCP server for "+app_name);
            close_hourglass(local_hourglass);
        }

        return local_port;
    }

    //**********************************************************
    private static void close_hourglass(Hourglass local_hourglass)
    //**********************************************************
    {
        if ( local_hourglass != null)
        {
            local_hourglass.close();
            local_hourglass = null;
        }
    }

    //**********************************************************
    private void write_UI_change_listening_port_to_file(int ui_change_listening_port, Logger logger)
    //**********************************************************
    {
        Path p = Path.of(System.getProperty("user.home"), Non_booleans_properties.CONF_DIR,Non_booleans_properties.FILENAME_FOR_UI_CHANGE_REPORT_PORT_AT_LAUNCHER);
        try
        {
            try ( BufferedWriter writer = java.nio.file.Files.newBufferedWriter(p, StandardCharsets.UTF_8))
            {
                logger.log("ui change port is :"+ui_change_listening_port);
                writer.write(""+ui_change_listening_port);
                writer.newLine();
            }
        }
        catch (IOException e)
        {
            logger.log("❗Warning: Launcher::write_UI_change_listening_port_to_file: cannot write to file "+p);
        }
    }


    //**********************************************************
    private static void write_port_to_reply_about_start(int port_to_reply_about_start, Logger logger)
    //**********************************************************
    {
        Path p = Path.of(System.getProperty("user.home"), Non_booleans_properties.CONF_DIR,Non_booleans_properties.FILENAME_FOR_PORT_TO_REPLY_ABOUT_START);
        try
        {
            try ( BufferedWriter writer = java.nio.file.Files.newBufferedWriter(p,StandardCharsets.UTF_8))
            {
                writer.write(""+port_to_reply_about_start);
                writer.newLine();
            }
        }
        catch (IOException e)
        {
            logger.log("❗Warning: Launcher::port_to_reply_about_start: cannot write to file "+p);
        }
    }
}
