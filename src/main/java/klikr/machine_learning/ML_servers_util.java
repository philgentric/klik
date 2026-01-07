// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.machine_learning;

import javafx.stage.Window;
import klikr.machine_learning.image_similarity.Feature_vector_source_for_image_similarity;
import klikr.properties.Non_booleans_properties;
import klikr.util.execute.*;
import klikr.util.log.Logger;
import klikr.util.log.Tmp_file_in_trash;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static klikr.properties.Non_booleans_properties.CONF_DIR;
import static klikr.properties.Non_booleans_properties.USER_HOME;

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
        Tmp_file_in_trash.create_copy_in_trash("requirements.txt",owner,logger);

        Operating_system os = Guess_OS.guess(owner,logger);
        switch ( os)
        {
            case Windows ->
            {
                Tmp_file_in_trash.create_copy_in_trash("create_venv_for_windows.ps1",owner,logger);
                List<String> cmds = List.of("create_venv_for_windows.ps1");
                Script_executor.execute(cmds,trash(owner,logger),owner,logger);

                /*
                List<String> cmds = new ArrayList<>();
                cmds.add("winget install -e --id Python.Python.3");
                // create venv folder
                cmds.add("New-Item -ItemType Directory -Path \"$env:USERPROFILE\\.klikr\\venv\" -Force");
                // create the venv
                cmds.add("python -m venv %USERPROFILE%\\.klikr\\venv");
                // activate it
                cmds.add(".\\..klikr\\venv\\Scripts\\Activate.ps1");
                cmds.add("py -m pip install -r requirements.txt");
                Execute_windows_command.execute(cmds, true, true, owner, logger);
                */
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
                Script_executor.execute(cmds,trash(owner,logger),owner,logger);
            }
        }
    }


    //**********************************************************
    public static boolean start_image_similarity_servers(Window owner, Logger logger)
    //**********************************************************
    {
        Tmp_file_in_trash.create_copy_in_trash("MobileNet_embeddings_server.py",owner,logger);
        List<String> ports = get_image_similarity_servers_ports(owner,logger);

        Operating_system os = Guess_OS.guess(owner,logger);
        switch ( os)
        {
            case MacOS, Linux ->
            {
                String script = "launch_image_similarity_servers.sh";
                Path p = Tmp_file_in_trash.create_copy_in_trash(script,owner,logger);
                if (!Script_executor.make_executable(p, logger)) return false;
                StringBuilder cmd = new StringBuilder();
                cmd.append("./").append(script);
                for ( String s : ports) cmd.append(" ").append(s);
                Script_executor.execute(List.of(cmd.toString()),trash(owner,logger),owner,logger);
            }
            case Windows ->
            {
                String script = "launch_image_similarity_servers.ps1";
                Tmp_file_in_trash.create_copy_in_trash(script,owner,logger);
                StringBuilder cmd = new StringBuilder();
                cmd.append(script);
                for ( String s : ports) cmd.append(" ").append(s);
                Script_executor.execute(List.of(cmd.toString()),trash(owner,logger),owner,logger);
            }

        }
        return true;
    }

    //**********************************************************
    public static void stop_image_similarity_servers(Window owner, Logger logger)
    //**********************************************************
    {
        Operating_system os = Guess_OS.guess(owner,logger);
        switch ( os)
        {
            case MacOS, Linux ->
            {
                String cmd = "kill_image_similarity_servers.sh";
                Path p = Tmp_file_in_trash.create_copy_in_trash(cmd,owner,logger);
                if (!Script_executor.make_executable(p, logger)) return;
                Script_executor.execute(List.of("./"+cmd),trash(owner,logger),owner,logger);
            }

            case Windows ->
            {
                String cmd = "kill_image_similarity_servers.ps1";
                Tmp_file_in_trash.create_copy_in_trash(cmd,owner,logger);
                Script_executor.execute(List.of(cmd),trash(owner,logger),owner,logger);
            }
        }


    }

    //**********************************************************
    public static boolean start_face_recognition_servers(Window owner, Logger logger)
    //**********************************************************
    {
        Tmp_file_in_trash.create_copy_in_trash("FaceNet_embeddings_server.py",owner,logger);
        Tmp_file_in_trash.create_copy_in_trash("MTCNN_face_detection_server.py",owner,logger);
        Tmp_file_in_trash.create_copy_in_trash("haars_face_detection_server.py",owner,logger);
        Tmp_file_in_trash.create_copy_in_trash("haarcascade_frontalface_alt_tree.xml",owner,logger);
        Tmp_file_in_trash.create_copy_in_trash("haarcascade_frontalface_default.xml",owner,logger);
        Tmp_file_in_trash.create_copy_in_trash("haarcascade_frontalface_alt.xml",owner,logger);
        Tmp_file_in_trash.create_copy_in_trash("haarcascade_frontalface_alt2.xml",owner,logger);
        int udp_monitoring_port = Embeddings_servers_monitor.get_servers_monitor_udp_port(owner, logger);

        Operating_system os = Guess_OS.guess(owner,logger);
        logger.log(os+" starting face recognition servers");
        switch( os)
        {
            case MacOS, Linux ->
            {
                String cmd = "launch_face_recognition_servers.sh";
                Path p = Tmp_file_in_trash.create_copy_in_trash(cmd,owner,logger);
                if (!Script_executor.make_executable(p, logger)) return false;
                Script_executor.execute(List.of("./"+cmd+" "+udp_monitoring_port),trash(owner,logger),owner,logger);
            }
            case Windows ->
            {
                String cmd = "launch_face_recognition_servers.ps1";
                Tmp_file_in_trash.create_copy_in_trash(cmd,owner,logger);
                Script_executor.execute(List.of(cmd+" "+udp_monitoring_port),trash(owner,logger),owner,logger);
            }
        }
        return true;
    }



    //**********************************************************
    public static void stop_face_recognition_servers(Window owner, Logger logger)
    //**********************************************************
    {
        Operating_system os = Guess_OS.guess(owner,logger);
        switch ( os)
        {
            case MacOS, Linux ->
            {
                String cmd = "kill_face_recognition_servers.sh";
                Path p = Tmp_file_in_trash.create_copy_in_trash(cmd,owner,logger);
                if (!Script_executor.make_executable(p, logger)) return;
                Script_executor.execute(List.of("./"+cmd),trash(owner,logger),owner,logger);
           }

            case Windows ->
            {
                String cmd = "kill_image_similarity_servers.ps1";
                Tmp_file_in_trash.create_copy_in_trash(cmd,owner,logger);
                Script_executor.execute(List.of(cmd),trash(owner,logger),owner,logger);
            }
        }


    }

    // works on all OSES
    //**********************************************************
    private static Path trash(Window owner,Logger logger)
    //**********************************************************
    {
        return Non_booleans_properties.get_trash_dir(Path.of(""),owner,logger);
    }

    // works on all OSES
    //**********************************************************
    public static Path venv()
    //**********************************************************
    {
        return Paths.get(System.getProperty(USER_HOME), CONF_DIR, "venv").toAbsolutePath();
    }

    // this is the WHOLE list, starting with the udp monitoring port
    //**********************************************************
    private static List<String> get_image_similarity_servers_ports(Window owner, Logger logger)
    //**********************************************************
    {
        List<String> returned = new ArrayList<>();
        returned.add(String.valueOf(Embeddings_servers_monitor.get_servers_monitor_udp_port(owner, logger)));
        for ( int port : Feature_vector_source_for_image_similarity.ports)
        {
            returned.add(String.valueOf(port));
        }
        return returned;
    }




}
