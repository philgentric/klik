// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr;

//SOURCES ./audio/Audio_player_access.java
//SOURCES ./image_ml/ML_servers_util.java
//SOURCES ./image_ml/UDP_traffic_monitor.java
//SOURCES ./image_ml/UDP_traffic_monitoring_stage.java
//SOURCES ./util/execute/Execute_via_script_in_tmp_file.java

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import klikr.util.Installers;
import klikr.util.Shared_services;
import klikr.util.execute.Script_executor;
import klikr.util.execute.actor.Aborter;
import klikr.look.Look_and_feel;
import klikr.look.Look_and_feel_manager;
import klikr.look.Look_and_feel_manager.Icon_type;
import klikr.look.my_i18n.My_I18n;
import klikr.properties.Non_booleans_properties;
import klikr.util.http.Klikr_communicator;
import klikr.util.log.Logger;
import klikr.util.ui.progress.Hourglass;
import klikr.util.ui.progress.Progress_window;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

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
public class Launcher extends Application implements UI_change_target
//**********************************************************
{
    // set gluon to true to compile native with gluon
    public static final boolean gluon = false;
    private final static String name = "Launcher";
    public static final int WIDTH = 600;
    public static final int icon_size = 100;
    public static final String STARTED = "STARTED";
    public static final String NOT_STARTED = "NOT_STARTED";

    private Stage stage;
    private Aborter aborter;

    private Logger logger;
    private HBox  main = new HBox();

    //private static ConcurrentLinkedQueue<Integer> propagate_to = new ConcurrentLinkedQueue<>();
    private Klikr_communicator com;
    Hourglass hourglass;
    Runnable on_started_received;

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
        System_info.print(stage,logger);

        Consumer<String> on_appearance_changed = msg -> {
            define_UI();
        };
        com = new Klikr_communicator("Launcher",stage,logger);
        com.set_on_appearance_changed(on_appearance_changed);
        com.start_as_singleton();
        on_started_received = () ->
        {
            logger.log("started received from newly started app");
            hourglass.close();
            com.deregister_on_started_received(on_started_received);
        };
        com.register_on_started_received(on_started_received);

        String launcher = My_I18n.get_I18n_string(Look_and_feel_manager.LAUNCHER,stage,logger);
        Look_and_feel_manager.set_icon_for_main_window(stage, launcher, Icon_type.LAUNCHER,stage,logger);

        Scene scene = new Scene(main);
        define_UI();

        stage.setTitle("Klik "+launcher);
        stage.setScene(scene);
        stage.show();
        stage.requestFocus(); // trying to make sure it comes on top

        long current = Non_booleans_properties.get_java_VM_max_RAM(stage,logger);

        if ( current > 0.8*System_info.get_total_machine_RAM_in_GBytes(stage,logger).orElse(4) )
        {
            // not realistic
            use_default_max_RAM(stage,logger);
            return;

        }
        if ( current == 1 )
        {
            // stupid default
            use_default_max_RAM(stage,logger);
        }
        
    }

    //**********************************************************
    private void use_default_max_RAM(Stage stage, Logger logger)
    //**********************************************************
    {
        long current = System_info.get_total_machine_RAM_in_GBytes(stage,logger).orElse(4);
        current = (current * 8) / 10; // use 80% of the physical RAM
        if ( current < 1) current = 1; // minimum 1GB
        Non_booleans_properties.save_java_VM_max_RAM((int)current, stage, logger);
        logger.log("Setting the max RAM to 80% of the physical RAM on this machine: "+current+" GBytes");
    }

    //**********************************************************
    @Override // UI_change_target
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
                Button b = new Button(My_I18n.get_I18n_string("Launch_1_New_Klikr_Application", stage, logger));
                left.getChildren().add(b);
                look_and_feel.set_Button_look(b, WIDTH, icon_size, Icon_type.IMAGE, stage, logger);
                b.setOnAction(event -> {
                    if (Launcher.gluon) {
                        start_app_with_gradle_and_listen("nativeRun", stage, logger);
                    } else {
                        start_app_with_gradle_and_listen("klikr", stage, logger);
                    }
                });
            }
            {
                Button b = new Button(My_I18n.get_I18n_string("Launch_Music_Player", stage, logger));
                right.getChildren().add(b);
                look_and_feel.set_Button_look(b, WIDTH, icon_size, Icon_type.MUSIC, stage, logger);
                b.setOnAction(event -> {
                    start_app_with_gradle_and_listen("audio_player", stage, logger);
                    //propagate_to.add(Audio_player_gradle_start.AUDIO_PLAYER_PORT);
                });
            }

        }
        {
            left.getChildren().add(new Separator());
            right.getChildren().add(new Separator());
        }
        Installers.make_ui_to_start_image_similarity_servers(WIDTH,icon_size,look_and_feel, left, stage, logger);
        Installers.make_ui_to_stop_image_similarity_servers(WIDTH,icon_size,look_and_feel, left, stage, logger);
        {
            left.getChildren().add(new Separator());
        }
        Installers.make_ui_to_start_face_recognition_servers(WIDTH,icon_size,look_and_feel, left, stage, logger);
        Installers.make_ui_to_stop_face_recognition_servers(WIDTH,icon_size,look_and_feel, left, stage, logger);
        {
            left.getChildren().add(new Separator());
        }
        Installers.make_ui_to_show_version(WIDTH,icon_size,look_and_feel, left, stage, logger);
        Installers.make_ui_get_most_recent_version(WIDTH,icon_size,look_and_feel, left, stage, logger);

        Installers.make_ui_to_install_everything(true,WIDTH,icon_size,look_and_feel, right, stage, logger);
        Installers.make_ui_to_install_python_libs_for_ML(WIDTH,icon_size,look_and_feel, right, stage, logger);
        Installers.make_ui_to_install_all_apps(WIDTH,icon_size,look_and_feel, right, stage, logger);
    }


    //**********************************************************
    private void start_app_with_gradle_and_listen(
            String app_name,
            Stage stage, Logger logger)
    //**********************************************************
    {
        hourglass = Progress_window.show(
                false,
                "Please wait ... starting "+app_name,
                30*60,
                stage.getX()+100,
                stage.getY()+100,
                stage,
                logger).orElse(null);

        String cmd = "gradle "+app_name+ " "+com.get_port();
        Script_executor.execute(List.of(cmd),Path.of("."),true,stage,logger);

    }


}
