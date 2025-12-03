package klik.util;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import klik.External_application;
import klik.look.Look_and_feel;
import klik.machine_learning.ML_servers_util;
import klik.properties.More_settings_stage;
import klik.util.execute.Execute_command;
import klik.util.execute.Execute_result;
import klik.util.execute.Execute_via_script_in_tmp_file;
import klik.util.execute.actor.Actor_engine;
import klik.util.log.Logger;
import klik.util.ui.Items_with_explanation;
import klik.util.ui.Popups;
import klik.util.ui.progress.Hourglass;
import klik.util.ui.progress.Progress_window;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

//**********************************************************
public class Installers
//**********************************************************
{

    //**********************************************************
    public static HBox make_ui_to_start_image_similarity_servers(double width, double icon_size, Look_and_feel look_and_feel, VBox vbox, Window owner, Logger logger)
    //**********************************************************
    {
        String key = "Start_Image_Similarity_Servers";
        EventHandler<ActionEvent> handler = e -> Execute_via_script_in_tmp_file.execute(ML_servers_util.get_command_string_to_start_image_similarity_servers(owner,logger), false, true, owner, logger);
        HBox hb = Items_with_explanation.make_hbox_with_button_and_explanation(
                key,
                handler,
                width,
                icon_size,
                look_and_feel,
                owner,
                logger);
        vbox.getChildren().add(hb);
        return hb;
    }

    //**********************************************************
    public static HBox make_ui_to_stop_image_similarity_servers(double width, double icon_size, Look_and_feel look_and_feel, VBox vbox, Window stage, Logger logger)
    //**********************************************************
    {
        String key = "Stop_Image_Similarity_Servers";
        EventHandler<ActionEvent> handler = e -> Execute_via_script_in_tmp_file.execute(ML_servers_util.get_command_string_to_stop_image_similarity_servers(logger), false, true, stage, logger);
        HBox hb = Items_with_explanation.make_hbox_with_button_and_explanation(
                key,
                handler,
                width,
                icon_size,
                look_and_feel,
                stage,
                logger);
        vbox.getChildren().add(hb);
        return hb;
    }

    //**********************************************************
    public static HBox make_ui_to_start_face_recognition_servers(double width, double icon_size, Look_and_feel look_and_feel, VBox vbox, Window stage, Logger logger)
    //**********************************************************
    {
        String key = "Start_Face_Recognition_Servers";
        EventHandler<ActionEvent> handler = e -> Execute_via_script_in_tmp_file.execute(ML_servers_util.get_command_string_to_start_face_recognition_servers(stage, logger), false, true, stage, logger);
        HBox hb = Items_with_explanation.make_hbox_with_button_and_explanation(
                key,
                handler,
                width,
                icon_size,
                look_and_feel,
                stage,
                logger);
        vbox.getChildren().add(hb);
        return hb;
    }

    //**********************************************************
    public static HBox make_ui_to_stop_face_recognition_servers(double width, double icon_size, Look_and_feel look_and_feel, VBox vbox, Window stage, Logger logger)
    //**********************************************************
    {
        String key = "Stop_Face_Recognition_Servers";
        EventHandler<ActionEvent> handler = e -> Execute_via_script_in_tmp_file.execute(ML_servers_util.get_command_string_to_stop_face_recognition_servers(logger), false, true, stage, logger);
        HBox hb = Items_with_explanation.make_hbox_with_button_and_explanation(
                key,
                handler,
                width,
                icon_size,
                look_and_feel,
                stage,
                logger);
        vbox.getChildren().add(hb);
        return hb;
    }

    //**********************************************************
    public static void make_ui_to_install_python_libs_for_ML(int width, int icon_size, Look_and_feel look_and_feel, VBox vbox, Stage stage, Logger logger)
    //**********************************************************
    {
        String key = "Install_Python_Libs_For_ML";
        EventHandler<ActionEvent> handler = e -> ML_servers_util.install_python_libs_for_ML(stage, logger);
        HBox hb = Items_with_explanation.make_hbox_with_button_and_explanation(
                key,
                handler,
                width,
                icon_size,
                look_and_feel,
                stage,
                logger);
        vbox.getChildren().add(hb);
    }

    //**********************************************************
    public static void make_ui_to_install_all_apps(int width, int icon_size, Look_and_feel look_and_feel, VBox vbox, Stage stage, Logger logger)
    //**********************************************************
    {
        for(External_application app :External_application.values())
        {
            HBox hb = app.get_button(width, icon_size, look_and_feel, stage, logger);
            vbox.getChildren().add(hb);
        }
    }



    //**********************************************************
    public static void make_ui_to_install_everything(int width, int icon_size, Look_and_feel look_and_feel, VBox vbox, Stage stage, Logger logger)
    //**********************************************************
    {
        String key = "Install_All_Tools";
        EventHandler<ActionEvent> handler = e -> Installers.install_everything(stage, logger);
        HBox hb = Items_with_explanation.make_hbox_with_button_and_explanation(
                key,
                handler,
                width,
                icon_size,
                look_and_feel,
                stage,
                logger);
        vbox.getChildren().add(hb);
    }


    //**********************************************************
    private static void install_everything(Window owner, Logger logger)
    //**********************************************************
    {
        Actor_engine.execute(() -> install_everything_in_a_thread(owner, logger), "Installing all tools", logger);
    }

    //**********************************************************
    private static void install_everything_in_a_thread(Window owner, Logger logger)
    //**********************************************************
    {
        ML_servers_util.install_python_libs_for_ML(owner, logger);
        for (External_application app : External_application.values()) {
            String cmd = app.get_command_string_to_install(owner, logger);
            if (cmd == null) continue;
            Execute_via_script_in_tmp_file.execute(cmd, true, false, owner, logger);
        }
    }


    //**********************************************************
    public static void make_ui_to_show_version(int width, int icon_size, Look_and_feel look_and_feel, VBox vbox, Stage stage, Logger logger)
    //**********************************************************
    {
        String key = "Show_Version";
        EventHandler<ActionEvent> handler =e -> show_version(stage, logger);
        HBox hb = Items_with_explanation.make_hbox_with_button_and_explanation(
                key,
                handler,
                width,
                icon_size,
                look_and_feel,
                stage,
                logger);
        vbox.getChildren().add(hb);
    }

    //**********************************************************
    public static void make_ui_get_most_recent_version(int width, int icon_size, Look_and_feel look_and_feel, VBox vbox, Stage stage, Logger logger)
    //**********************************************************
    {
        String key = "Get_Most_Recent_Version";
        EventHandler<ActionEvent> handler =e -> get_most_recent_version(stage, logger);
        HBox hb = Items_with_explanation.make_hbox_with_button_and_explanation(
                key,
                handler,
                width,
                icon_size,
                look_and_feel,
                stage,
                logger);
        vbox.getChildren().add(hb);
    }

    //**********************************************************
    private static void show_version(Window owner, Logger logger)
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
    private static void get_most_recent_version(Stage stage, Logger logger)
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

}
