// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.machine_learning;

import javafx.stage.Window;
import klikr.machine_learning.image_similarity.Feature_vector_source_for_image_similarity;
import klikr.properties.Non_booleans_properties;
import klikr.util.execute.*;
import klikr.util.log.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static klikr.properties.Non_booleans_properties.CONF_DIR;

//**********************************************************
public class ML_servers_util
//**********************************************************
{

    static final String macOS_commands_to_install_python = "brew install python@3.10";
    static final String macOS_commands_to_create_venv = "/opt/homebrew/bin/python3.10 -m venv " + venv();
    static final String macOS_commands_to_activate_venv ="source " + venv() + "/bin/activate";
    static final String macOS_commands_to_pip ="pip install -U pip";
    static final String macOS_commands_to_install_tensorflow = "pip install tensorflow-macos tensorflow-metal";
    static final String macOS_commands_to_install_requirements = "pip install -r requirements.txt";

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
                cmds.add("New-Item -ItemType Directory -Path \"$env:USERPROFILE\\.klikr\\venv\" -Force");
                // create the venv
                cmds.add("python -m venv %USERPROFILE%\\.klikr\\venv");
                // activate it
                cmds.add(".\\..klikr\\venv\\Scripts\\Activate.ps1");
                cmds.add("py -m pip install -r requirements.txt");
                Execute_windows_command.execute(cmds, true, true, owner, logger);
            }
            case MacOS, Linux ->
            {
                // we need to know if we are from source of from jar
                //Execution_context execution_context = Execute_common.get_context("requirements.txt",owner,logger);
                //if ( execution_context == null) return;
                Execute_common.create_copy_in_trash("requirements.txt",owner,logger);
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
        Operating_system os = Guess_OS.guess(owner,logger);
        switch ( os)
        {

            case MacOS, Linux ->
            {
                String script = "launch_image_similarity_servers.sh";
                Path p = Execute_common.create_copy_in_trash(script,owner,logger);
                if ( !Execute_common.make_executable(p,logger)) return false;

                Execute_common.create_copy_in_trash("MobileNet_embeddings_server.py",owner,logger);
                List<String> ports = get_image_similarity_servers_ports(owner, logger);
                StringBuilder cmd = new StringBuilder();
                cmd.append("./").append(script);
                for ( String s : ports) cmd.append(" ").append(s);
                Script_executor.execute(List.of(cmd.toString(),"echo end-of-script"),trash(owner,logger),owner,logger);

                //Nix_execute_via_script_in_tmp_file.execute(execution_context.folder(),execution_context.cmd(),true,ports,true, true, owner, logger);
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
                        //Execution_context execution_context = Execute_common.get_context("kill_image_similarity_servers.sh",owner,logger);
                        //if ( execution_context == null) return ;

                        String cmd = "kill_image_similarity_servers.sh";
                        Path p = Execute_common.create_copy_in_trash(cmd,owner,logger);
                        if ( !Execute_common.make_executable(p,logger)) return;
                        Script_executor.execute(List.of("./"+cmd),trash(owner,logger),owner,logger);

/*                        Nix_execute_via_script_in_tmp_file.execute(
                                execution_context.folder(),execution_context.cmd(),true,List.of(),
                                true, true,
                                owner, logger);
*/
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
        Execute_common.create_copy_in_trash("FaceNet_embeddings_server.py",owner,logger);
        Execute_common.create_copy_in_trash("MTCNN_face_detection_server.py",owner,logger);
        Execute_common.create_copy_in_trash("haars_face_detection_server.py",owner,logger);
        Execute_common.create_copy_in_trash("haarcascade_frontalface_alt_tree.xml",owner,logger);
        Execute_common.create_copy_in_trash("haarcascade_frontalface_default.xml",owner,logger);
        Execute_common.create_copy_in_trash("haarcascade_frontalface_alt.xml",owner,logger);
        Execute_common.create_copy_in_trash("haarcascade_frontalface_alt2.xml",owner,logger);
        int udp_monitoring_port = Embeddings_servers_monitor.get_servers_monitor_udp_port(owner, logger);


        Operating_system os = Guess_OS.guess(owner,logger);
        switch( os)
        {
            case MacOS, Linux ->
            {
                logger.log(os+" starting face recognition servers");
                String cmd = "launch_face_recognition_servers.sh";
                Path p = Execute_common.create_copy_in_trash(cmd,owner,logger);
                if ( !Execute_common.make_executable(p,logger)) return false;
                Script_executor.execute(List.of("./"+cmd+" "+udp_monitoring_port),trash(owner,logger),owner,logger);
            }
            case Windows ->
            {
                logger.log(os+" starting image similarity servers");
                String exeDir = System.getProperty("user.dir"); // directory of java.exe that launched this VM
                Path ps1_script = Paths.get(exeDir, "scripts", "launch_face_recognition_servers.ps1 ");
                Execute_Windows_ps1_script.execute("Start face recognition servers",ps1_script,List.of(String.valueOf(udp_monitoring_port)),true,true, owner,logger);
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
                Path p = Execute_common.create_copy_in_trash(cmd,owner,logger);
                if ( !Execute_common.make_executable(p,logger)) return;


                Script_executor.execute(List.of("./"+cmd),trash(owner,logger),owner,logger);

               /* Nix_execute_via_script_in_tmp_file.execute(
                        execution_context.folder(),execution_context.cmd(),true,List.of(),
                        true, true,
                        owner, logger);*/
           }

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
    private static Path trash(Window owner,Logger logger)
    //**********************************************************
    {
        return Non_booleans_properties.get_trash_dir(Path.of("").toAbsolutePath(),owner,logger);
    }

    //**********************************************************
    public static Path venv()
    //**********************************************************
    {
        return Paths.get(System.getProperty("user.home"), CONF_DIR, "venv").toAbsolutePath();
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
