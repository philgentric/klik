// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.machine_learning;

import javafx.stage.Window;
import klik.machine_learning.image_similarity.Feature_vector_source_for_image_similarity;
import klik.util.execute.Execute_Windows_ps1_script;
import klik.util.execute.Nix_execute_via_script_in_tmp_file;
import klik.util.execute.Guess_OS;
import klik.util.execute.Operating_system;
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
                logger.log("install_python_libs_for_ML not implemented for Windows");
                return;
            }
            case MacOS, Linux ->
            {
                {
                    for (String s : macOS_commands_to_install_python)
                    {
                        Nix_execute_via_script_in_tmp_file.execute(s, true, false,owner, logger);
                    }
                }
                {
                    for (String s : macOS_commands_to_install_metal) {
                        Nix_execute_via_script_in_tmp_file.execute(s, true, false,owner, logger);
                    }
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
                String cmd = get_NIX_command_string_to_start_image_similarity_servers(owner,logger);
                Nix_execute_via_script_in_tmp_file.execute(cmd, true, true, owner, logger);
            }
            case Windows ->
            {
                List<Integer> ports = get_image_similarity_servers_ports(owner,logger);
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
            case MacOS, Linux -> Nix_execute_via_script_in_tmp_file.execute(ML_servers_util.get_NIX_command_string_to_stop_image_similarity_servers(owner,logger), false, true, owner, logger);

            case Windows ->
            {
                String exeDir = System.getProperty("user.dir"); // directory of java.exe that launched this VM
                Path ps1_script = Paths.get(exeDir, "scripts", "kill_image_similarity_servers.ps1");
                Execute_Windows_ps1_script.execute("Kill image similarity servers",ps1_script,List.of(),true,true, owner,logger);
            }
        }


    }

    //**********************************************************
    public static void start_face_recognition_servers(Window owner, Logger logger)
    //**********************************************************
    {
        Operating_system os = Guess_OS.guess(owner,logger);
        switch( os)
        {
            case MacOS, Linux -> Nix_execute_via_script_in_tmp_file.execute(ML_servers_util.get_NIX_command_string_to_start_face_recognition_servers(owner,logger), false, true, owner, logger);
            case Windows ->
            {
                List<Integer> ports = get_face_recognition_servers_ports(owner,logger);
                String exeDir = System.getProperty("user.dir"); // directory of java.exe that launched this VM
                Path ps1_script = Paths.get(exeDir, "scripts", "launch_face_recognition_servers.ps1");
                Execute_Windows_ps1_script.execute("Start face recognition servers",ps1_script,ports,true,true, owner,logger);
            }
        }

    }

    //**********************************************************
    private static List<Integer> get_face_recognition_servers_ports(Window owner, Logger logger)
    //**********************************************************
    {
        List<Integer> returned = new ArrayList<>();
        returned.add(Embeddings_servers_monitor.get_servers_monitor_udp_port(owner, logger));
        return returned;
    }

    //**********************************************************
    public static void stop_face_recognition_servers(Window owner, Logger logger)
    //**********************************************************
    {
        Operating_system os = Guess_OS.guess(owner,logger);
        switch ( os)
        {
            case MacOS, Linux -> Nix_execute_via_script_in_tmp_file.execute(ML_servers_util.get_NIX_command_string_to_stop_face_recognition_servers(owner,logger), false, true, owner, logger);

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

    //**********************************************************
    private static List<Integer> get_image_similarity_servers_ports(Window owner, Logger logger)
    //**********************************************************
    {
        List<Integer> returned = new ArrayList<>();
        returned.add(Embeddings_servers_monitor.get_servers_monitor_udp_port(owner, logger));
        for ( int port : Feature_vector_source_for_image_similarity.ports)
        {
            returned.add(port);
        }
        return returned;
    }
    //**********************************************************
    private static String get_NIX_command_string_to_start_image_similarity_servers(Window owner, Logger logger)
    //**********************************************************
    {
        // if not already started, start the servers monitor
        int udp_port = Embeddings_servers_monitor.get_servers_monitor_udp_port(owner, logger);

        String list_of_ports = "";
        for ( int port : Feature_vector_source_for_image_similarity.ports)
        {
            list_of_ports += port + " ";
        }

       return "source " + venv_metal() + "/bin/activate; cd " +Paths.get("").toAbsolutePath()+"/python_for_ML; ./launch_image_similarity_servers.sh "+ udp_port +" "+ list_of_ports;
    }
    //**********************************************************
    private static String get_NIX_command_string_to_stop_image_similarity_servers(Window owner,Logger logger)
    //**********************************************************
    {
        return "cd "+Paths.get("").toAbsolutePath()+"/python_for_ML; ./kill_image_similarity_servers.sh";
    }



    //**********************************************************
    public static String get_NIX_command_string_to_start_face_recognition_servers(Window owner, Logger logger)
    //**********************************************************
    {
        // if not already started, start the servers monitor
        int udp_port = Embeddings_servers_monitor.get_servers_monitor_udp_port(owner,logger);
        return "source " + venv_metal() + "/bin/activate; cd " +Paths.get("").toAbsolutePath()+"/python_for_ML; ./launch_face_recognition_servers.sh "+ udp_port;
    }

    //**********************************************************
    public static String get_NIX_command_string_to_stop_face_recognition_servers(Window owner, Logger logger)
    //**********************************************************
    {
        return "cd "+Paths.get("").toAbsolutePath()+"/python_for_ML; ./kill_face_recognition_servers.sh";
    }

    static final String[] macOS_commands_to_install_python ={
            "brew install python@3.10",
            "/opt/homebrew/bin/python3.10 -m venv " + venv_metal(),
            "source " + venv_metal() + "/bin/activate;pip install -U pip"
    };
    static final String[] macOS_commands_to_install_metal ={
            "source " + venv_metal() + "/bin/activate;pip install tensorflow-macos tensorflow-metal",
            "source " + venv_metal() + "/bin/activate;cd ./python_for_ML;pip install -r requirements.txt"
    };


}
