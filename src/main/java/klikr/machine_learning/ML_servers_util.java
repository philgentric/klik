// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.machine_learning;

import javafx.stage.Window;
import klikr.machine_learning.face_recognition.Face_detection_type;
import klikr.machine_learning.feature_vector.UDP_traffic_monitor;
import klikr.properties.Non_booleans_properties;
import klikr.properties.String_constants;
import klikr.properties.boolean_features.Feature;
import klikr.properties.boolean_features.Feature_cache;
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

        Operating_system os = Guess_OS.guess(owner,logger);
        switch ( os)
        {
            case Windows ->
            {
                Tmp_file_in_trash.create_copy_in_trash("create_venv_for_windows.ps1",owner,logger);
                List<String> cmds = List.of(".\\"+"create_venv_for_windows.ps1");
                Script_executor.execute(cmds,trash(owner,logger),dbg, owner,logger);
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
                Script_executor.execute(cmds,trash(owner,logger),dbg, owner,logger);
            }
        }
    }


    //**********************************************************
    public static int start_some_image_similarity_servers(int num_servers, Window owner, Logger logger)
    //**********************************************************
    {
        boolean dbg = Feature_cache.get(Feature.Enable_ML_server_debug);

        Tmp_file_in_trash.create_copy_in_trash("MobileNet_embeddings_server.py",owner,logger);
        int udp_port = UDP_traffic_monitor.get_servers_monitor_udp_port(owner, logger);
        Operating_system os = Guess_OS.guess(owner,logger);

        // Launch multiple servers, each will bind to an ephemeral port (0)
        for (int i = 0; i < num_servers; i++)
        {
            logger.log("launching image similarity server #" + i);
            Actor_engine.execute(() -> {
                switch (os) {
                    case MacOS, Linux -> {
                        List<String> cmds = new ArrayList<>();
                        cmds.add(macOS_commands_to_activate_venv);
                        // Pass 0 for TCP port - server will bind to ephemeral port
                        cmds.add("nohup python3 MobileNet_embeddings_server.py " + udp_port+ " &");
                        Script_executor.execute(cmds, trash(owner, logger), dbg,owner, logger);
                    }
                    case Windows -> {
                        List<String> cmds = new ArrayList<>();
                        Path venv_path = venv();
                        Path activate_script = venv_path.resolve("Scripts").resolve("Activate.ps1");
                        cmds.add("& " + "\"" + activate_script.toAbsolutePath() + "\"");
                        cmds.add("cmd /k python MobileNet_embeddings_server.py " + udp_port);
                        Script_executor.execute(cmds, trash(owner, logger), dbg, owner, logger);
                    }
                }
            },"launching image similarity server #" + i, logger);
        }
        return num_servers;
    }




    //**********************************************************
    public static void stop_image_similarity_servers(Window owner, Logger logger)
    //**********************************************************
    {
        boolean dbg = Feature_cache.get(Feature.Enable_ML_server_debug);
        Operating_system os = Guess_OS.guess(owner,logger);
        switch ( os)
        {
            case MacOS, Linux ->
            {
                // kill every “MobileNet_embeddings_server” process
                String cmd1 = "pids=$(pgrep -f MobileNet_embeddings_server  || true)";
                String cmd2 = "if [[ -n $pids ]]; then kill -9 $pids; fi";
                Script_executor.execute(List.of(cmd1, cmd2),trash(owner,logger),dbg, owner,logger);
            }

            case Windows ->
            {
                 List<String> cmds = new ArrayList<>();
                 cmds.add("$procList = Get-CimInstance -ClassName Win32_Process | Where-Object { $_.CommandLine -match 'MobileNet_embeddings_server' }");
                 cmds.add("if ($procList) { Stop-Process -Id $procList.ProcessId -Force -ErrorAction SilentlyContinue }");
                 Script_executor.execute(cmds,trash(owner,logger),dbg, owner,logger);
            }
        }

        ML_registry_discovery.all_servers_killed(new ML_service_type(ML_server_type.MobileNet,null));
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
        Operating_system os = Guess_OS.guess(owner,logger);
        Actor_engine.execute(() -> {
            List<String> cmds = new ArrayList<>();
            String argsStr = String.join(" ", args);

            switch(os) {
                case MacOS, Linux -> {
                    cmds.add(macOS_commands_to_activate_venv);
                    // execute the script directly using python3
                    cmds.add("nohup python3 " + scriptName + " " + argsStr+" &");
                    Script_executor.execute(cmds, trash(owner, logger), dbg, owner, logger);
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
                    Script_executor.execute(cmds, trash(owner, logger), dbg, owner, logger);
                }
            }
        }, "launching " + scriptName, logger);
    };

    //**********************************************************
    public static int start_haars_face_detection_servers(Window owner, Logger logger)
    //**********************************************************
    {
        Tmp_file_in_trash.create_copy_in_trash("haars_face_detection_server.py",owner,logger);
        Tmp_file_in_trash.create_copy_in_trash("haarcascade_frontalface_alt_tree.xml",owner,logger);
        Tmp_file_in_trash.create_copy_in_trash("haarcascade_frontalface_alt_default.xml",owner,logger);
        Tmp_file_in_trash.create_copy_in_trash("haarcascade_frontalface_alt1.xml",owner,logger);
        Tmp_file_in_trash.create_copy_in_trash("haarcascade_frontalface_alt2.xml",owner,logger);
        int udp_monitoring_port = UDP_traffic_monitor.get_servers_monitor_udp_port(owner, logger);

        Operating_system os = Guess_OS.guess(owner,logger);
        logger.log(os+" starting 4 haars_face_detection servers");

        //  alt_tree
        launcher(
                "haars_face_detection_server.py",
                new String[]{
                        "'haarcascade_frontalface_alt_tree.xml'",
                String.valueOf(udp_monitoring_port)},
                owner, logger);

        // alt_default
        launcher(
                "haars_face_detection_server.py",
            new String[]{
                    "'haarcascade_frontalface_alt_default.xml'",
                    String.valueOf(udp_monitoring_port)},
                owner, logger);

        // Alt1
        launcher("haars_face_detection_server.py",
                new String[]{
                        "'haarcascade_frontalface_alt1.xml'",
                        String.valueOf(udp_monitoring_port)}
        , owner, logger);

        // Alt2
        launcher("haars_face_detection_server.py",
            new String[]{
                    "'haarcascade_frontalface_alt2.xml'", String.valueOf(udp_monitoring_port)}
        , owner, logger);


        return 4;
    }

    //**********************************************************
    public static int start_MTCNN_face_detection_servers(Window owner, Logger logger)
    //**********************************************************
    {
        Tmp_file_in_trash.create_copy_in_trash("MTCNN_face_detection_server.py",owner,logger);
        int udp_monitoring_port = UDP_traffic_monitor.get_servers_monitor_udp_port(owner, logger);

        Operating_system os = Guess_OS.guess(owner,logger);
        logger.log(os+" starting 1 MTCNN face recognition server");

        launcher("MTCNN_face_detection_server.py",
                    new String[]{String.valueOf(udp_monitoring_port)},
                    owner, logger);
        return 1;
    }

    //**********************************************************
    public static int start_face_embeddings_servers(Window owner, Logger logger)
    //**********************************************************
    {
        Tmp_file_in_trash.create_copy_in_trash("FaceNet_embeddings_server.py",owner,logger);
        int udp_monitoring_port = UDP_traffic_monitor.get_servers_monitor_udp_port(owner, logger);

        Operating_system os = Guess_OS.guess(owner,logger);
        logger.log(os+" starting FaceNet servers");

        // Launch FaceNet Embeddings Servers
        int actual = 3;
        for (int i = 0; i < actual; i++) {
            launcher("FaceNet_embeddings_server.py",
                    new String[]{String.valueOf(udp_monitoring_port)},
                    owner, logger);
        }

        return actual;
    }

    //**********************************************************
    public static void stop_face_recognition_servers(Window owner, Logger logger)
    //**********************************************************
    {
        boolean dbg = Feature_cache.get(Feature.Enable_ML_server_debug);
        Operating_system os = Guess_OS.guess(owner,logger);
        switch ( os)
        {
            case MacOS, Linux ->
            {
                 List<String> cmds = new ArrayList<>();
                 for (String name : List.of("MTCNN_face_detection_server", "haars_face_detection_server", "FaceNet_embeddings_server")) {
                     cmds.add("pids=$(pgrep -f " + name + " || true)");
                     cmds.add("if [[ -n $pids ]]; then kill -9 $pids; fi");
                 }
                 Script_executor.execute(cmds, trash(owner, logger), dbg, owner, logger);
           }

            case Windows ->
            {
                List<String> cmds = new ArrayList<>();
                for (String name : List.of("MTCNN_face_detection_server", "haars_face_detection_server", "FaceNet_embeddings_server")) {
                    cmds.add("$procList = Get-CimInstance -ClassName Win32_Process | Where-Object { $_.CommandLine -match '" + name + "' }");
                    cmds.add("if ($procList) { Stop-Process -Id $procList.ProcessId -Force -ErrorAction SilentlyContinue }");
                }
                Script_executor.execute(cmds, trash(owner, logger), dbg, owner, logger);
            }
        }
        ML_registry_discovery.all_servers_killed(new ML_service_type(ML_server_type.MTCNN,null));
        ML_registry_discovery.all_servers_killed(new ML_service_type(ML_server_type.FaceNet,null));
        ML_registry_discovery.all_servers_killed(new ML_service_type(ML_server_type.Haars,Face_detection_type.alt1));
        ML_registry_discovery.all_servers_killed(new ML_service_type(ML_server_type.Haars,Face_detection_type.alt2));
        ML_registry_discovery.all_servers_killed(new ML_service_type(ML_server_type.Haars,Face_detection_type.alt_default));
        ML_registry_discovery.all_servers_killed(new ML_service_type(ML_server_type.Haars,Face_detection_type.alt_tree));

    }

    //**********************************************************
    private static Path trash(Window owner,Logger logger)
    //**********************************************************
    {
        // works on all OSES
        return Static_files_and_paths_utilities.get_trash_dir(Path.of(""),owner,logger);
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
        start_face_embeddings_servers(owner, logger);
        start_MTCNN_face_detection_servers(owner, logger);
        start_haars_face_detection_servers(owner, logger);
    }
}
