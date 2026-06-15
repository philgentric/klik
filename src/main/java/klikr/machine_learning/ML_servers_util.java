// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.machine_learning;

import javafx.stage.Window;
import klikr.machine_learning.monitoring.ML_registry;
import klikr.machine_learning.monitoring.UDP_traffic_monitor;
import klikr.settings.String_constants;
import klikr.settings.boolean_features.Feature;
import klikr.settings.boolean_features.Feature_cache;
import klikr.util.execute.*;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.files_and_paths.Static_files_and_paths_utilities;
import klikr.util.log.Logger;
import klikr.util.log.Tmp_file_in_trash;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


//**********************************************************
public class ML_servers_util
//**********************************************************
{
    private static final String MACOS_PYTHON = "/opt/homebrew/bin/python3.10";
    private static final String LINUX_PYTHON = "/usr/bin/python3";
    private static final String WINDOWS_PYTHON = "python.exe";

    static final String macOS_commands_to_install_python = "brew install python@3.10";
    static final String macOS_commands_to_create_venv = MACOS_PYTHON+" -m venv " + venv();
    static final String macOS_commands_to_activate_venv ="source " + venv() + "/bin/activate";
    static final String macOS_commands_to_pip ="pip install -U pip";
    static final String macOS_commands_to_install_tensorflow = "pip install tensorflow-macos tensorflow-metal";
    static final String macOS_commands_to_install_requirements = "pip install -r requirements.txt";

    //**********************************************************
    public static void install_python_libs_for_ML(Window owner, Logger logger)
    //**********************************************************
    {
        boolean dbg = Feature_cache.get(Feature.Enable_ML_server_debug);
        Tmp_file_in_trash.create_copy_in_trash("requirements.txt",owner,logger);

        Operating_system os = Guess_OS.guess(logger);
        switch ( os)
        {
            case Windows ->
            {
                Tmp_file_in_trash.create_copy_in_trash("create_venv_for_windows.ps1",owner,logger);
                List<String> cmds = List.of(".\\"+"create_venv_for_windows.ps1");
                Script_executor.execute(cmds,dbg,owner, logger);
            }
            case MacOS, Linux ->
            {
                List<String> cmds = List.of(
                        macOS_commands_to_install_python,
                        macOS_commands_to_create_venv,
                        macOS_commands_to_activate_venv,
                        macOS_commands_to_pip,
                        macOS_commands_to_install_tensorflow,
                        macOS_commands_to_install_requirements
                );
                Script_executor.execute(cmds,dbg,owner, logger);
            }
        }
    }



    //**********************************************************
    public static void start_N_ML_servers(int actual, ML_server_type type, Window owner, Logger logger)
    //**********************************************************
    {
        Tmp_file_in_trash.create_copy_in_trash(type.python_file_name(),owner,logger);
        if ( type.get_xml_file_name() != null)
        {
            Tmp_file_in_trash.create_copy_in_trash(type.get_xml_file_name(), owner,logger);
        }
        int udp_monitoring_port = UDP_traffic_monitor.get_servers_monitor_udp_port(owner, logger);

        Operating_system os = Guess_OS.guess(logger);
        logger.log(os+" starting "+actual+" "+type.name()+"servers");

        String[] args = switch (type) {
            case MobileNet, MTCNN, FaceNet -> new String[]{type.name(),String.valueOf(udp_monitoring_port)};
            case Haar_alt1, Haar_alt2, Haar_default, Haar_tree ->
                    new String[]{type.name(),type.get_xml_file_name(),String.valueOf(udp_monitoring_port)};
        };
        for (int i = 0; i < actual; i++)
        {
            launcher(type.python_file_name(),
                    args,
                    owner, logger);
        }
    }


    //**********************************************************
    private static void launcher(
            String scriptName,
            String[] args,
            Window owner,
            Logger logger)
    //**********************************************************
    {
        boolean dbg = Feature_cache.get(Feature.Enable_ML_server_debug);
        Operating_system os = Guess_OS.guess(logger);
        Actor_engine.execute(() -> {
            List<String> cmds = new ArrayList<>();
            String argsStr = String.join(" ", args);

            switch(os) {
                case MacOS, Linux -> {
                    cmds.add(macOS_commands_to_activate_venv);
                    // execute the script directly using python3
                    cmds.add("nohup python3 " + scriptName + " " + argsStr+" &");
                    Script_executor.execute(cmds,  dbg, owner, logger);
                }
                case Windows -> {
                    // Activate venv
                    Path venv_path = venv();
                    Path activate_script = venv_path.resolve("Scripts").resolve("Activate.ps1");
                    cmds.add("& " + "\"" + activate_script.toAbsolutePath() + "\"");
                    // Run python script
                    // `/K` keeps the command prompt open,
                    // preventing the JVM from sending a termination signal to the child.
                    cmds.add("cmd /k python " + scriptName + " " + argsStr);
                    Script_executor.execute(cmds,  dbg, owner, logger);
                }
            }
        }, "launching " + scriptName, logger);
    };



    //**********************************************************
    public static void stop_image_similarity_servers(Window owner, Logger logger)
    //**********************************************************
    {
        // kill every “MobileNet_embeddings_server” process
        boolean dbg = Feature_cache.get(Feature.Enable_ML_server_debug);
        Operating_system os = Guess_OS.guess(logger);
        switch ( os)
        {
            case MacOS, Linux ->
            {
                String cmd1 = "pids=$(pgrep -f MobileNet_embeddings_server  || true)";
                String cmd2 = "if [[ -n $pids ]]; then kill -9 $pids; fi";
                Script_executor.execute(List.of(cmd1, cmd2),dbg,owner, logger);
            }

            case Windows ->
            {
                List<String> cmds = new ArrayList<>();
                cmds.add("$procList = Get-CimInstance -ClassName Win32_Process | Where-Object { $_.CommandLine -match 'MobileNet_embeddings_server' }");
                cmds.add("if ($procList) { Stop-Process -Id $procList.ProcessId -Force -ErrorAction SilentlyContinue }");
                Script_executor.execute(cmds,dbg,owner, logger);
            }
        }

        ML_registry.all_servers_killed(ML_server_type.MobileNet);
    }

    //**********************************************************
    public static void stop_face_recognition_servers(Window owner, Logger logger)
    //**********************************************************
    {
        boolean dbg = Feature_cache.get(Feature.Enable_ML_server_debug);
        Operating_system os = Guess_OS.guess(logger);
        switch ( os)
        {
            case MacOS, Linux ->
            {
                 List<String> cmds = new ArrayList<>();
                 for (String name : List.of("MTCNN_face_detection_server", "Haar_face_detection_server", "FaceNet_embeddings_server")) {
                     cmds.add("pids=$(pgrep -f " + name + " || true)");
                     cmds.add("if [[ -n $pids ]]; then kill -9 $pids; fi");
                 }
                 Script_executor.execute(cmds,  dbg, owner, logger);
           }

            case Windows ->
            {
                List<String> cmds = new ArrayList<>();
                for (String name : List.of("MTCNN_face_detection_server", "Haar_face_detection_server", "FaceNet_embeddings_server")) {
                    cmds.add("$procList = Get-CimInstance -ClassName Win32_Process | Where-Object { $_.CommandLine -match '" + name + "' }");
                    cmds.add("if ($procList) { Stop-Process -Id $procList.ProcessId -Force -ErrorAction SilentlyContinue }");
                }
                Script_executor.execute(cmds, dbg, owner, logger);
            }
        }
        ML_registry.all_servers_killed(ML_server_type.MTCNN);
        ML_registry.all_servers_killed(ML_server_type.FaceNet);
        ML_registry.all_servers_killed(ML_server_type.Haar_alt1);
        ML_registry.all_servers_killed(ML_server_type.Haar_alt2);
        ML_registry.all_servers_killed(ML_server_type.Haar_default);
        ML_registry.all_servers_killed(ML_server_type.Haar_tree);

    }



    //**********************************************************
    public static Path venv()
    //**********************************************************
    {
        // works on all OSES
        return Paths.get(System.getProperty(String_constants.USER_HOME), String_constants.CONF_DIR, "venv").toAbsolutePath();
    }


    //**********************************************************
    public static void start_face_recognition_servers(Window owner, Logger logger)
    //**********************************************************
    {
        start_N_ML_servers(3,ML_server_type.FaceNet,owner, logger);
        start_N_ML_servers(3,ML_server_type.MTCNN,owner, logger);
        start_N_ML_servers(1,ML_server_type.Haar_tree,owner, logger);
        start_N_ML_servers(1,ML_server_type.Haar_default,owner, logger);
        start_N_ML_servers(1,ML_server_type.Haar_alt1,owner, logger);
        start_N_ML_servers(1,ML_server_type.Haar_alt2,owner, logger);

    }
}
