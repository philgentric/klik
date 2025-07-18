package klik;

//SOURCES ./audio/Audio_player_access.java

import com.sun.management.OperatingSystemMXBean;
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
import javafx.stage.Window;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.audio.Audio_player_access;
import klik.browser.Shared_services;
import klik.look.Jar_utils;
import klik.look.Look_and_feel;
import klik.look.Look_and_feel_manager;
import klik.look.Look_and_feel_manager.Icon_type;
import klik.look.my_i18n.My_I18n;
import klik.properties.Non_booleans;
import klik.properties.boolean_features.Booleans;
import klik.properties.boolean_features.Feature;
import klik.properties.boolean_features.Feature_cache;
import klik.util.Sys_init;
import klik.util.execute.Execute_command;
import klik.util.log.File_logger;
import klik.util.log.Logger;
import klik.util.log.Logger_factory;
import klik.util.log.Stack_trace_getter;
import klik.util.tcp.*;
import klik.util.ui.Hourglass;
import klik.util.ui.Popups;
import klik.util.ui.Show_running_film_frame;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

// the launcher can start applications (the image browser klik, the audio player)
// they are started in new VMs i.e. in new processes using a call to gradle
// this means that the application may be recompiled before launching, if the code has changed
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
// which then propagates them to the audio player, on the audio player port (345639)

//**********************************************************
public class Launcher extends Application implements UI_change
//**********************************************************
{

    private final static String name = "Launcher";
    public static final int WIDTH = 600;
    public static final int icon_size = 100;
    public static final String STARTED = "STARTED";
    public static final String NOT_STARTED = "NOT_STARTED";
    public static double estimated_text_label_height;
    public static int ui_change_listening_port; // this is the port on which the launcher listens for UI changes

    private Stage stage;
    private Logger logger;
    private VBox vbox;
    private static ConcurrentLinkedQueue<Integer> propagate_to = new ConcurrentLinkedQueue<>();

    //**********************************************************
    @Override
    public void start(Stage stage_) throws Exception
    //**********************************************************
    {

        stage = stage_;
        Sys_init.init(name,stage);
        logger = Logger_factory.get(name);

        logger.log("Launcher starting");

        vbox = new VBox();
        vbox.setAlignment(Pos.CENTER);
        Look_and_feel_manager.set_region_look(vbox,stage,logger);
        double font_size = Non_booleans.get_font_size(stage,logger);
        estimated_text_label_height = klik.look.Look_and_feel.MAGIC_HEIGHT_FACTOR*font_size;

        String launcher = My_I18n.get_I18n_string(Look_and_feel_manager.LAUNCHER,stage,logger);

        Look_and_feel_manager.set_icon_for_main_window(stage, launcher, Icon_type.LAUNCHER,stage,logger);


        define_UI( );


        ui_change_listening_port = UI_change.start_UI_change_server(propagate_to,this,"Launcher",stage,logger);


        Scene scene = new Scene(vbox);
        stage.setTitle("Klik "+launcher);
        stage.setScene(scene);
        stage.show();

        OperatingSystemMXBean b = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        System.out.println("\nPhysical RAM on this machine: "+b.getTotalPhysicalMemorySize()/1_000_000_000.0+" GBytes");


        long current = Non_booleans.get_java_VM_max_RAM(stage,logger);

        if ( current > b.getTotalPhysicalMemorySize()/1_000_000_000 )
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
        if (Booleans.get_boolean(Feature.max_RAM_is_defined_by_user.name(), stage))
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
        OperatingSystemMXBean b = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        long current = b.getTotalPhysicalMemorySize() * 80 / 100;
        current = current / 1_000_000_000;
        Non_booleans.save_java_VM_max_RAM((int)current, stage, logger);
        logger.log("Setting the max RAM to 80% of the physical RAM on this machine: "+current+" GBytes");
    }

    //**********************************************************
    @Override // UI_change
    public void define_UI()
    //**********************************************************
    {
        Look_and_feel look_and_feel = Look_and_feel_manager.get_instance(stage,logger);

        vbox.getChildren().clear();
        {
            Button b = new Button(My_I18n.get_I18n_string("Launch_1_New_Klik_Application", stage,logger));
            set_look(b, vbox, look_and_feel,Icon_type.IMAGE, stage,logger);
            b.setOnAction(event -> {
                start_app_in_new_VM_and_listen("klik", stage, logger);
            });
        }
        {
            Button b = new Button(My_I18n.get_I18n_string("Launch_Music_Player", stage,logger));
            set_look(b, vbox, look_and_feel,Icon_type.MUSIC, stage,logger);
            b.setOnAction(event -> {
                start_app_in_new_VM_and_listen("audio_player", stage, logger);
                propagate_to.add(Audio_player_access.AUDIO_PLAYER_PORT);
            });
        }
        {
            Button b = new Button("Show version");
            set_look(b, vbox, look_and_feel,null, stage,logger);
            b.setOnAction(event -> {
                show_version(stage, logger);
            });
        }
        {
            Button b = new Button(My_I18n.get_I18n_string("Get_Most_Recent_Version", stage,logger));
            set_look(b, vbox, look_and_feel,null, stage,logger);
            b.setOnAction(event -> {
                get_most_recent_version(stage, logger);
            });
        }
    }

    //**********************************************************
    private void show_version(Window owner, Logger logger)
    //**********************************************************
    {
        Hourglass local_hourglass = Show_running_film_frame.show_running_film(owner,100,100,"Please wait ... starting",20 * 1000,new Aborter("dummy", logger), logger);

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
        Hourglass local_hourglass = Show_running_film_frame.show_running_film(stage,100,100,"Please wait ... getting version",20 * 1000,new Aborter("dummy", logger), logger);

        if (Popups.popup_ask_for_confirmation("Are you sure you want to get the most recent version?","Developers: This will stash changes you made (if you made any changes),\n switch to the master branch (if you are on a different one)\nand get the most recent version from the repository\n\nIf you are not a developer, this is transparent, you just get the last and best",stage,logger))
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
    private static void start_app_in_new_VM_and_listen(
            String app_name,
            Stage stage, Logger logger)
    //**********************************************************
    {
        Hourglass local_hourglass = Show_running_film_frame.show_running_film(stage,100,100,"Please wait ... starting "+app_name,20 * 1000,new Aborter("launcher", logger), logger);

        int port_to_reply_about_start = start_launch_status_server(app_name, local_hourglass, stage,logger);

        List<String> cmds = new ArrayList<>();
        cmds.add("gradle");
        cmds.add(app_name);
        String arg =  "--args=\""+port_to_reply_about_start+" "+ ui_change_listening_port  +"\"";
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
                Platform.runLater(() -> Popups.simple_alert("BUILD FAILED",stage,logger));
                failed.set(true);
                TCP_client.send("localhost",port_to_reply_about_start,Launcher.NOT_STARTED,logger); // this is to unblock the hourglass
            }
        };
        Actor_engine.execute(r, logger);

    }

    //**********************************************************
    private static int start_launch_status_server(String app_name, Hourglass local_hourglass, Window owner, Logger logger)
    //**********************************************************
    {
        // start the server to receive the "started" or "not_started" error_message and stop the hourglass

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
                            Platform.runLater(()->Popups.popup_warning("Warning",app_name+" could not be started.\n" +
                                    "Since there is already another instance playing", false,owner, logger));

                        }
                        else
                        {
                            logger.log("Launcher: NOT_STARTED received for "+app_name+", this should not happen");
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
    private static void set_look(Button b, VBox vbox, Look_and_feel look_and_feel, Icon_type icon_type, Window owner,Logger logger)
    //**********************************************************
    {
        Look_and_feel_manager.set_button_look(b, true,owner,logger);
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
            Image icon = Jar_utils.load_jfx_image_from_jar(icon_path, icon_size, owner,logger);

            the_image_view.setImage(icon);
            the_image_view.setPreserveRatio(true);

            double h = icon_size + estimated_text_label_height;
            b.setPrefHeight(h);
            b.setMinHeight(h);
            b.setMaxHeight(h);
        }
    }

}
