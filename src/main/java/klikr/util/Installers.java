package klikr.util;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import klikr.Klikr_application;
import klikr.look.Look_and_feel;
import klikr.machine_learning.python_via_html_ML.ML_server_type;
import klikr.machine_learning.python_via_html_ML.ML_servers_util;
import klikr.settings.Non_booleans_properties;
import klikr.settings.boolean_features.Feature;
import klikr.settings.boolean_features.Feature_cache;
import klikr.util.execute.Execute_command;
import klikr.util.execute.Execute_result;
import klikr.util.execute.Script_executor;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.log.Logger;
import klikr.util.ui.Items_with_explanation;
import klikr.util.ui.Popups;
import klikr.util.ui.progress.Hourglass;
import klikr.util.ui.progress.Progress_window;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;
import java.util.List;
import java.util.Optional;

//**********************************************************
public class Installers
//**********************************************************
{

    //**********************************************************
    public static HBox make_ui_to_start_image_similarity_servers(double width, double icon_size, Look_and_feel look_and_feel, VBox vbox, Kontext context)
    //**********************************************************
    {
        String key = "Start_Image_Similarity_Servers";
        int num_servers = Non_booleans_properties.get_number_of_image_similarity_servers();

        EventHandler<ActionEvent> handler = e -> {
            Actor_engine.execute(()->ML_servers_util.start_N_ML_servers(num_servers, ML_server_type.MobileNet,context), "Starting image similarity servers", context.logger());
        };
        HBox hb = Items_with_explanation.make_hbox_with_button_and_explanation(
                key,
                handler,
                width,
                icon_size,
                look_and_feel,
                context);
        vbox.getChildren().add(hb);
        return hb;
    }

    //**********************************************************
    public static HBox make_ui_to_stop_image_similarity_servers(double width, double icon_size, Look_and_feel look_and_feel, VBox vbox, Kontext context)
    //**********************************************************
    {
        String key = "Stop_Image_Similarity_Servers";
        EventHandler<ActionEvent> handler = e -> ML_servers_util.stop_image_similarity_servers(context);
        HBox hb = Items_with_explanation.make_hbox_with_button_and_explanation(
                key,
                handler,
                width,
                icon_size,
                look_and_feel,
                context);
        vbox.getChildren().add(hb);
        return hb;
    }

    //**********************************************************
    public static HBox make_ui_to_start_face_recognition_servers(double width, double icon_size, Look_and_feel look_and_feel, VBox vbox, Kontext context)
    //**********************************************************
    {
        String key = "Start_Face_Recognition_Servers";
        EventHandler<ActionEvent> handler = e -> ML_servers_util.start_face_recognition_servers(context);;
        HBox hb = Items_with_explanation.make_hbox_with_button_and_explanation(
                key,
                handler,
                width,
                icon_size,
                look_and_feel,
                context);
        vbox.getChildren().add(hb);
        return hb;
    }

    //**********************************************************
    public static HBox make_ui_to_stop_face_recognition_servers(double width, double icon_size, Look_and_feel look_and_feel, VBox vbox, Kontext context)
    //**********************************************************
    {
        String key = "Stop_Face_Recognition_Servers";
        EventHandler<ActionEvent> handler = e -> ML_servers_util.stop_face_recognition_servers(context);
        HBox hb = Items_with_explanation.make_hbox_with_button_and_explanation(
                key,
                handler,
                width,
                icon_size,
                look_and_feel,
                context);
        vbox.getChildren().add(hb);
        return hb;
    }

    //**********************************************************
    public static void make_ui_to_install_python_libs_for_ML(double width, double icon_size, Look_and_feel look_and_feel, VBox vbox, Kontext context)
    //**********************************************************
    {
        String key = "Install_Python_Libs_For_ML";
        EventHandler<ActionEvent> handler = e -> ML_servers_util.install_python_libs_for_ML(context);
        HBox hb = Items_with_explanation.make_hbox_with_button_and_explanation(
                key,
                handler,
                width,
                icon_size,
                look_and_feel,
                context);
        vbox.getChildren().add(hb);
    }

    //**********************************************************
    public static void make_ui_to_install_all_apps(double width, double icon_size, Look_and_feel look_and_feel, VBox vbox, Kontext context)
    //**********************************************************
    {
        for(External_application app :External_application.values())
        {
            if ( app==External_application.Ffprobe)
            {
                // assume ffprobe is installed with ffmpeg
                continue;
            }
            HBox hb = app.get_button(width, icon_size, look_and_feel, context);
            vbox.getChildren().add(hb);
        }
    }



    //**********************************************************
    public static void make_ui_to_install_everything(boolean also_python_ML_libs,double width, double icon_size, Look_and_feel look_and_feel, VBox vbox, Kontext context)
    //**********************************************************
    {
        String key = "Install_All_Tools";
        EventHandler<ActionEvent> handler = e -> Installers.install_everything(also_python_ML_libs,context);
        HBox hb = Items_with_explanation.make_hbox_with_button_and_explanation(
                key,
                handler,
                width,
                icon_size,
                look_and_feel,
                context);
        vbox.getChildren().add(hb);
    }


    //**********************************************************
    private static void install_everything(boolean also_python_ML_libs,Kontext context)
    //**********************************************************
    {
        Actor_engine.execute(() -> install_everything_in_a_thread(also_python_ML_libs,context), "Installing all tools", context.logger());
    }

    //**********************************************************
    private static void install_everything_in_a_thread(boolean also_python_ML_libs, Kontext context)
    //**********************************************************
    {
        if ( also_python_ML_libs) ML_servers_util.install_python_libs_for_ML(context);
        for (External_application app : External_application.values())
        {
            if ( app==External_application.Ffprobe) continue; // installs with ffmpeg
            String cmd = app.get_command_string_to_install(context);
            if (cmd == null) continue;
            boolean dbg = Feature_cache.get(Feature.Enable_install_debug);
            // must exec in trash

            Script_executor.execute(List.of(cmd), dbg, context);
        }
    }


    //**********************************************************
    public static void make_ui_to_show_version(double width, double icon_size, Look_and_feel look_and_feel, VBox vbox, Kontext context)
    //**********************************************************
    {
        String key = "Show_Version";
        EventHandler<ActionEvent> handler =e -> show_version(context);
        HBox hb = Items_with_explanation.make_hbox_with_button_and_explanation(
                key,
                handler,
                width,
                icon_size,
                look_and_feel,
                context);
        vbox.getChildren().add(hb);
    }

    //**********************************************************
    public static void make_ui_get_most_recent_version(double width, double icon_size, Look_and_feel look_and_feel, VBox vbox, Kontext context)
    //**********************************************************
    {
        String key = "Get_Most_Recent_Version";
        EventHandler<ActionEvent> handler =e -> get_most_recent_version(context);
        HBox hb = Items_with_explanation.make_hbox_with_button_and_explanation(
                key,
                handler,
                width,
                icon_size,
                look_and_feel,
                context);
        vbox.getChildren().add(hb);
    }

    //**********************************************************
    public static void show_version(Kontext context)
    //**********************************************************
    {
        Optional<Hourglass> local_hourglass = Progress_window.show(
                "Please wait ... getting version",
                30*60,
                context);

        // 1. version written at build time by gradle (works for installed apps too)
        String version_string = get_version_from_build_properties(context);
        context.log("build_version_string: "+version_string);
        if ( version_string == null)
        {
            // 2. running from the source folder: ask git directly
            version_string = get_version_string(context);
            context.log("git_version_string: "+version_string);
        }
        if ( version_string == null)
        {
            // 3. last resort: jar manifest
            version_string = Klikr_application.class.getPackage().getImplementationVersion();
            context.log("code_version_string: "+version_string);
        }

        Popups.simple_alert("version is "+version_string,context);

        local_hourglass.ifPresent(Hourglass::close);

    }

    //**********************************************************
    // reads /klikr/version.properties, generated by the gradle task generateVersionProps
    // returns e.g. "1.0.203 (cc4308a)" or null if the file is missing
    public static String get_version_from_build_properties(Kontext context)
    //**********************************************************
    {
        try (InputStream in = Installers.class.getResourceAsStream("/klikr/version.properties"))
        {
            if (in == null) return null;
            Properties props = new Properties();
            props.load(in);
            String version = props.getProperty("version", "?");
            String count = props.getProperty("commit_count", "unknown");
            String hash = props.getProperty("commit_hash", "unknown");
            return version + "." + count + " (" + hash + ")";
        }
        catch (IOException e)
        {
            context.log(Logger.warning + "cannot read version.properties: " + e);
            return null;
        }
    }

    //**********************************************************
    private static String get_version_string(Kontext context)
    //**********************************************************
    {
        String version =get_version_from_gradle_build(context);
        if ( version == null) return null;
        String commit_count =get_commit_count(context);
        String version_string = version+"."+commit_count;
        return version_string;
    }

    //**********************************************************
    private static String get_commit_count(Kontext context)
    //**********************************************************
    {
        List<String> cmds = new ArrayList<>();
        cmds.add("git");
        cmds.add("rev-list");
        cmds.add("--count");
        cmds.add("HEAD");

        Execute_result res = Execute_command.execute_command_list(cmds, new File("."), 20 * 1000, null, context);
        if ( !res.status())
        {
            context.log(Logger.warning+"Warning cannot get commit count, is git installed ?");
            return Logger.warning+"Warning cannot get commit count, is git installed ?";
        }
        String commit_count = res.output();
        return commit_count;
    }

    //**********************************************************
    private static String get_version_from_gradle_build(Kontext context)
    //**********************************************************
    {
        List<String> cmds = new ArrayList<>();
        cmds.add("grep");
        cmds.add("version");
        cmds.add("build.gradle");

        StringBuilder sb = null;//new StringBuilder();
        Execute_result res = Execute_command.execute_command_list(cmds, new File("."), 20 * 1000, sb, context);
        if ( !res.status())
        {
            context.log(Logger.warning+"Warning cannot get version from build.gradle");
            return Logger.warning+"Warning cannot get version from build.gradle";
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
                    return parts2[0].trim().replaceAll("'","").replaceAll("\"","");
                }
            }
        }
        return null;
    }



    //**********************************************************
    private static void get_most_recent_version(Kontext context)
    //**********************************************************
    {
        Optional<Hourglass> local_hourglass = Progress_window.show(
                "Please wait ... getting version",
                30*60,
                context);


        if (Popups.popup_ask_for_confirmation(Logger.warning+" Are you sure you want to get the most recent version?","Developers: This will stash changes you made (if you made any changes),\n switch to the master branch (if you are on a different one)\nand get the most recent version from the repository\n\nIf you are not a developer, this is transparent, you just get the last and best, but of course, things need to be restarted for changes to take effect",context))
        {
            context.log("version before:"+get_version_string(context));
            {
                List<String> cmds = new ArrayList<>();
                cmds.add("git");
                cmds.add("stash");
                StringBuilder sb = new StringBuilder();
                Execute_command.execute_command_list(cmds, new File("."), 20 * 1000, sb, context);
                context.log(sb.toString());
            }
            {
                List<String> cmds = new ArrayList<>();
                cmds.add("git");
                cmds.add("checkout");
                cmds.add("master");

                StringBuilder sb = new StringBuilder();
                Execute_command.execute_command_list(cmds, new File("."), 20 * 1000, sb, context);
                context.log(sb.toString());
            }
            {
                List<String> cmds = new ArrayList<>();
                cmds.add("git");
                cmds.add("pull");

                StringBuilder sb = new StringBuilder();
                Execute_command.execute_command_list(cmds, new File("."), 20 * 1000, sb, context);
                context.log(sb.toString());
            }
            context.log("version after:"+get_version_string(context));
        }
        local_hourglass.ifPresent(Hourglass::close);
    }

}
