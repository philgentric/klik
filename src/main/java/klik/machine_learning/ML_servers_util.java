// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.machine_learning;

import javafx.stage.Window;
import klik.machine_learning.image_similarity.Feature_vector_source_for_image_similarity;
import klik.util.execute.*;
import klik.util.log.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

//**********************************************************
public class ML_servers_util
//**********************************************************
{
    //**********************************************************
    public static void install_python_libs_for_ML(Window owner, Logger logger)
    //**********************************************************
    {
        Operating_system os = Guess_OS.guess(owner,logger);
        switch ( os)
        {
            case Windows ->
            {
                List<String> cmds = new ArrayList<>();
                cmds.add("winget install -e --id Python.Python.3");
                // create venv folder
                cmds.add("New-Item -ItemType Directory -Path \"$env:USERPROFILE\\.klik\\venv-metal\" -Force");
                // create the venv
                cmds.add("python -m venv %USERPROFILE%\\.klik\\venv-metal");
                // activate it
                cmds.add(".\\..klik\\venv-metal\\Scripts\\Activate.ps1");
                cmds.add("py -m pip install -r requirements.txt");
                Execute_windows_command.execute(cmds, true, true, owner, logger);
            }
            case MacOS, Linux ->
            {
                // we need to know if we are from source of from jar
                Execution_context execution_context = Execute_common.get_context("launch_image_similarity_servers.sh",owner,logger);
                if ( execution_context == null) return;

                for (String s : macOS_commands_to_install_python)
                {
                    Nix_execute_via_script_in_tmp_file.execute(execution_context.folder(),s, false, List.of(),true, false,owner, logger);
                }

                if ( execution_context.from_jar())
                {
                    // need to also copy the requirements.txt file
                    Execute_common.create_copy_in_trash("requirements.txt",owner,logger);
                }
                for (String s : macOS_commands_to_install_metal)
                {
                    Nix_execute_via_script_in_tmp_file.execute(execution_context.folder(),s, false,List.of(),true, false,owner, logger);
                }
            }

        }

    }




    //**********************************************************
    public static boolean start_image_similarity_servers(Window owner, Logger logger)
    //**********************************************************
    {
        Operating_system os = Guess_OS.guess(owner,logger);
        switch ( os)
        {

            case MacOS, Linux ->
            {
                Execution_context execution_context = Execute_common.get_context("launch_image_similarity_servers.sh",owner,logger);
                if ( execution_context == null) return false;
                if ( execution_context.from_jar())
                {
                    // need to also copy the python
                    Execute_common.create_copy_in_trash("MobileNet_embeddings_server.py",owner,logger);
                }

                List<String> ports = get_image_similarity_servers_ports(owner, logger);

                Nix_execute_via_script_in_tmp_file.execute(
                        execution_context.folder(),execution_context.cmd(),true,ports,
                        true, true,
                        owner, logger);
            }

            case Windows ->
            {
                List<String> ports = get_image_similarity_servers_ports(owner,logger);
                String exeDir = System.getProperty("user.dir"); // directory of java.exe that launched this VM
                Path ps1_script = Paths.get(exeDir, "scripts", "launch_image_similarity_servers.ps1");
                Execute_Windows_ps1_script.execute("Start image similarity servers",ps1_script,ports,true,true, owner,logger);
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
                        Execution_context execution_context = Execute_common.get_context("kill_image_similarity_servers.sh",owner,logger);
                        if ( execution_context == null) return ;
                        Nix_execute_via_script_in_tmp_file.execute(
                                execution_context.folder(),execution_context.cmd(),true,List.of(),
                                true, true,
                                owner, logger);

                    }

            case Windows ->
            {
                String exeDir = System.getProperty("user.dir"); // directory of java.exe that launched this VM
                Path ps1_script = Paths.get(exeDir, "scripts", "kill_image_similarity_servers.ps1");
                Execute_Windows_ps1_script.execute("Kill image similarity servers",ps1_script,List.of(),true,true, owner,logger);
            }
        }


    }

    //**********************************************************
    public static boolean start_face_recognition_servers(Window owner, Logger logger)
    //**********************************************************
    {
        Operating_system os = Guess_OS.guess(owner,logger);
        switch( os)
        {
            case MacOS, Linux ->
            {
                logger.log(os+" starting image similarity servers");
                Execution_context execution_context = Execute_common.get_context("launch_face_recognition_servers.sh",owner,logger);
                if ( execution_context == null) return false;
                if ( execution_context.from_jar())
                {
                    // need to also copy the python
                    Execute_common.create_copy_in_trash("FaceNet_embeddings_server.py",owner,logger);
                    Execute_common.create_copy_in_trash("MTCNN_face_detection_server.py",owner,logger);
                    Execute_common.create_copy_in_trash("haars_face_detection_server.py",owner,logger);
                    Execute_common.create_copy_in_trash("haarcascade_frontalface_alt_tree.xml",owner,logger);
                    Execute_common.create_copy_in_trash("haarcascade_frontalface_default.xml",owner,logger);
                    Execute_common.create_copy_in_trash("haarcascade_frontalface_alt.xml",owner,logger);
                    Execute_common.create_copy_in_trash("haarcascade_frontalface_alt2.xml",owner,logger);

                }
                List<String> ports = get_face_recognition_servers_ports(owner,logger);
                Nix_execute_via_script_in_tmp_file.execute(
                        execution_context.folder(),execution_context.cmd(), true,ports,
                        true, true,
                        owner, logger);
            }
            case Windows ->
            {
                logger.log(os+" starting image similarity servers");
                List<String> ports = get_face_recognition_servers_ports(owner,logger);
                String exeDir = System.getProperty("user.dir"); // directory of java.exe that launched this VM
                Path ps1_script = Paths.get(exeDir, "scripts", "launch_face_recognition_servers.ps1");
                Execute_Windows_ps1_script.execute("Start face recognition servers",ps1_script,ports,true,true, owner,logger);
            }
        }
        return true;
    }

    //**********************************************************
    private static List<String> get_face_recognition_servers_ports(Window owner, Logger logger)
    //**********************************************************
    {
        List<String> returned = new ArrayList<>();
        returned.add(String.valueOf(Embeddings_servers_monitor.get_servers_monitor_udp_port(owner, logger)));
        return returned;
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
                        Execution_context execution_context = Execute_common.get_context("kill_face_recognition_servers.sh",owner,logger);
                        if ( execution_context == null) return;
                        Nix_execute_via_script_in_tmp_file.execute(
                                execution_context.folder(),execution_context.cmd(),true,List.of(),
                                true, true,
                                owner, logger);                     }

            case Windows ->
            {
                {
                    String exeDir = System.getProperty("user.dir"); // directory of java.exe that launched this VM
                    Path ps1_script = Paths.get(exeDir, "scripts", "kill_face_recognition_servers.ps1");
                    Execute_Windows_ps1_script.execute("Kill face recognition servers",ps1_script,List.of(),true,true, owner,logger);
                }
            }
        }


    }
    //**********************************************************
    public static String venv_metal()
    //**********************************************************
    {
        return Paths.get(System.getProperty("user.home"), ".klik", "venv-metal").toAbsolutePath().toString();
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





    //**********************************************************
    static final String[] macOS_commands_to_install_python ={
            "brew install python@3.10",
            "/opt/homebrew/bin/python3.10 -m venv " + venv_metal(),
            "source " + venv_metal() + "/bin/activate;pip install -U pip"
    };
    //**********************************************************

    //**********************************************************
    static final String[] macOS_commands_to_install_metal ={
            "source " + venv_metal() + "/bin/activate;pip install tensorflow-macos tensorflow-metal",
            "source " + venv_metal() + "/bin/activate;cd ./python_for_ML;pip install -r requirements.txt"
    };
    //**********************************************************



}
