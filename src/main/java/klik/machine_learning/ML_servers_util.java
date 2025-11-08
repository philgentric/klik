// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.machine_learning;

import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import klik.machine_learning.image_similarity.Feature_vector_source_for_image_similarity;
import klik.look.Look_and_feel_manager;
import klik.machine_learning.song_similarity.Feature_vector_source_for_song_similarity;
import klik.properties.Non_booleans_properties;
import klik.util.execute.Execute_via_script_in_tmp_file;
import klik.util.log.Logger;

import java.nio.file.Paths;

//**********************************************************
public class ML_servers_util
//**********************************************************
{

    //**********************************************************
    static String[] image_similarity_lines =
    //**********************************************************
    {
        "For image similarity to work",
        "Feature vector servers must first be installed (once) see manual",
        "Then, to start the image similarity servers copy paste this line in a terminal:",

    };

    //**********************************************************
    static String[] Enable_face_recognition_lines =
    //**********************************************************
    {

            "For face recognition to work",
            "Face detection and specific feature vector servers must be first installed (once) see manual",
            "Then to start the face recognition servers copy paste this line in a terminal:",


    };

    //**********************************************************
    public static void show_face_recognition_manual(Window owner, Logger logger)
    //**********************************************************
    {
        Stage stage = new Stage();
        stage.initOwner(owner);
        VBox vb = new VBox();
        for ( String l : Enable_face_recognition_lines)
        {
            TextField tf = new TextField(l);
            Look_and_feel_manager.set_region_look(tf,owner,logger);
            tf.setEditable(false);
            vb.getChildren().add(tf);
        }

        //Path p = Paths.get("");
        {
            String cmd = get_command_string_to_start_face_recognition_servers(logger);
            TextField tf = new TextField(cmd);
            Look_and_feel_manager.set_region_look(tf,owner,logger);
            tf.setEditable(false);
            vb.getChildren().add(tf);
        }

        Scene scene = new Scene(vb);
        stage.setScene(scene);
        stage.setWidth(1000);
        stage.setHeight(1000);
        stage.show();
    }

    //**********************************************************
    public static void show_image_similarity_manual(Window owner,Logger logger)
    //**********************************************************
    {
        Stage stage = new Stage();
        stage.initOwner(owner);
        VBox vb = new VBox();
        for ( String l : image_similarity_lines)
        {
            TextField tf = new TextField(l);
            Look_and_feel_manager.set_region_look(tf,owner,logger);
            tf.setEditable(false);
            vb.getChildren().add(tf);
        }

        {
            String cmd = get_command_string_to_start_image_similarity_servers(logger);
            TextArea tf = new TextArea(cmd);
            Look_and_feel_manager.set_region_look(tf,owner,logger);
            tf.setEditable(false);
            tf.setWrapText(true);
            vb.getChildren().add(tf);
        }

        Scene scene = new Scene(vb);
        stage.setScene(scene);
        stage.setWidth(1000);
        stage.setHeight(1000);
        stage.show();
    }

    //**********************************************************
    public static String get_command_string_to_start_image_similarity_servers(Logger logger)
    //**********************************************************
    {
        // if not already started, start the servers monitor
        int udp_port = Embeddings_servers_monitor.get_servers_monitor_udp_port(logger);

        String list_of_ports = "";
        for ( int port : Feature_vector_source_for_image_similarity.ports)
        {
            list_of_ports += port + " ";
        }
        return "source ~/venv-metal/bin/activate; cd "+Paths.get("").toAbsolutePath()+"/python_for_ML; ./launch_image_similarity_servers "+ udp_port +" "+ list_of_ports;
    }
    //**********************************************************
    public static String get_command_string_to_stop_image_similarity_servers(Logger logger)
    //**********************************************************
    {
        return "cd "+Paths.get("").toAbsolutePath()+"/python_for_ML; ./kill_image_similarity_servers";
    }
/*
    //**********************************************************
    public static String get_command_string_to_start_song_similarity_servers(Logger logger)
    //**********************************************************
    {
        // if not already started, start the servers monitor
        int udp_port = Embeddings_servers_monitor.get_servers_monitor_udp_port(logger);

        String list_of_ports = "";
        for ( int port : Feature_vector_source_for_song_similarity.ports)
        {
            list_of_ports += port + " ";
        }
        return "source ~/venv-metal/bin/activate; cd "+Paths.get("").toAbsolutePath()+"/python_for_ML; ./launch_song_similarity_servers "+ udp_port +" "+ list_of_ports;
    }
    //**********************************************************
    public static String get_command_string_to_stop_song_similarity_servers(Logger logger)
    //**********************************************************
    {
        return "cd "+Paths.get("").toAbsolutePath()+"/python_for_ML; ./kill_song_similarity_servers";
    }
*/



    //**********************************************************
    public static String get_command_string_to_start_face_recognition_servers(Logger logger)
    //**********************************************************
    {
        // if not already started, start the servers monitor
        int udp_port = Embeddings_servers_monitor.get_servers_monitor_udp_port(logger);

        String cmd = "source ~/venv-metal/bin/activate; cd "+Paths.get("").toAbsolutePath()+"/python_for_ML; ./launch_face_recognition_servers "+ udp_port;
        logger.log("start_face_recognition_servers with command: "+ cmd);
        return cmd;
    }

    //**********************************************************
    public static String get_command_string_to_stop_face_recognition_servers(Logger logger)
    //**********************************************************
    {
        return "cd "+Paths.get("").toAbsolutePath()+"/python_for_ML; ./kill_face_recognition_servers";
    }

    static final String[] commands_to_install_python ={
            "brew install python@3.10",
            "/opt/homebrew/bin/python3.10 -m venv ~/venv-metal",
            "source ~/venv-metal/bin/activate;pip install -U pip"
    };
    static final String[] commands_to_install_metal ={
            "source ~/venv-metal/bin/activate;pip install tensorflow-macos tensorflow-metal",
            "source ~/venv-metal/bin/activate;cd ./python_for_ML;pip install -r requirements.txt"
    };

    //**********************************************************
    public static void install_python_libs_for_ML(Window owner, Logger logger)
    //**********************************************************
    {
        //String home = System.getProperty(Non_booleans_properties.USER_HOME);
        {
            for (String s : commands_to_install_python)
            {
                Execute_via_script_in_tmp_file.execute(s, true, true,owner, logger);
            }
        }
        {
            for (String s : commands_to_install_metal) {
                Execute_via_script_in_tmp_file.execute(s, true, true,owner, logger);
            }
        }
    }




    /*
    public static boolean venv_activated = false;
    //**********************************************************
    public static boolean init_face_reco(Logger logger)
    //**********************************************************
    {
        if (face_reco_servers_started) return true;
        face_reco_servers_started = true;
        if (!init_venv(logger)) {
            logger.log("failed to init python venv");
            return false;
        }

        if ( !install_requirements(logger))
        {
            logger.log("failed to install requirements");
            return false;
        }

        stop_face_detection_servers(logger);

        if (!start_face_detection_servers(logger)) {
            logger.log("failed to init face detection");
            return false;
        }
        return true;
    }

    //**********************************************************
    public static boolean init_image_similarity(Logger logger)
    //**********************************************************
    {
        if( image_similarity_servers_started) return true;
        image_similarity_servers_started = true;

        logger.log("MobileNetV2 image similarity servers INIT");

        String list_of_ports = "";
        for ( int port : Feature_vector_source_for_image_similarity.ports)
        {
            list_of_ports += port + " ";
        }

        List<String> ll = new ArrayList<>();
        ll.add("/bin/bash");
        ll.add("-c");
        //ll.add("\"source ~/venv-metal/bin/activate; ./python_for_face_reco/launch_MobileNetV2_servers\"");
        ll.add("nohup bash -c 'source ~/venv-metal/bin/activate; ./python_for_face_reco/launch_MobileNetV2_servers "+list_of_ports+"' &");
        File wd = new File("./python_for_face_reco");
        StringBuilder sb = new StringBuilder();
        Execute_command.execute_command_list(ll,wd,Integer.MAX_VALUE,sb,logger);
        logger.log(sb.toString());

        logger.log("MobileNetV2 image similarity servers started");

        return true;
    }

     //**********************************************************
    private static boolean init_venv(Logger logger)
    //**********************************************************
    {
        if ( venv_activated)
        {
            logger.log("venv already activated");
            return true;
        }
        File venv_dir = new File("./python_for_face_reco/venv");
        if ( !venv_dir.exists() ) create_venv(logger);

        // special trick c.f. stackOverFlow
        List<String> ll = new ArrayList<>();
        ll.add("/bin/bash");
        ll.add("-ch");
        ll.add("source venv/bin/activate");
        File wd = new File("./python_for_face_reco");
        StringBuilder sb = new StringBuilder();
        if( Execute_command.execute_command_list(ll,wd,20000,sb,logger) == null)
        {
            logger.log("failed to activate venv");
            venv_activated = false;
            return false;
        }
        logger.log(sb.toString());
        venv_activated = true;
        return true;
    }

    //**********************************************************
    private static boolean install_requirements(Logger logger)
    //**********************************************************
    {
        List<String> ll = new ArrayList<>();
        ll.add("pip3");
        ll.add("install");
        ll.add("-r");
        ll.add("requirements.txt");
        File wd = new File("./python_for_face_reco");
        StringBuilder sb = new StringBuilder();
        if (Execute_command.execute_command_list(ll,wd,20000,sb,logger) ==null)
        {
            logger.log("failed to install requirements");
            return false;
        }
        logger.log(sb.toString());
        return true;
    }


    //**********************************************************
    private static boolean start_face_detection_servers(Logger logger)
    //**********************************************************
    {
        List<String> ll = new ArrayList<>();
        ll.add("/bin/bash");
        ll.add("-ch");
        ll.add("launch_face_servers");
        File wd = new File("./python_for_face_reco");
        StringBuilder sb = new StringBuilder();
        boolean status;
        if (Execute_command.execute_command_list(ll,wd,20000,sb,logger)==null)
        {
            status = false;
        }
        else
        {
            status = true;
        }
        logger.log(sb.toString());
        return status;
    }

    //**********************************************************
    private static boolean stop_face_detection_servers(Logger logger)
    //**********************************************************
    {
        List<String> ll = new ArrayList<>();
        ll.add("/bin/bash");
        ll.add("-ch");
        ll.add("kill_face_servers");
        File wd = new File("./python_for_face_reco");
        StringBuilder sb = new StringBuilder();
        if(Execute_command.execute_command_list(ll,wd,20000,sb,logger)==null)
        {
            logger.log("failed to stop face detection servers");
            return false;
        }
        logger.log(sb.toString());
        return true;
    }

    //**********************************************************
    private static boolean start_image_similarity_servers(Logger logger)
    //**********************************************************
    {
        List<String> ll = new ArrayList<>();
        ll.add("/bin/bash");
        ll.add("-ch");
        ll.add("launch_vgg19_servers");
        File wd = new File("./python_for_face_reco");

        StringBuilder sb = new StringBuilder();
        if (Execute_command.execute_command_list(ll,wd,20000,sb,logger) ==null)
        {
            logger.log("failed to start image similarity servers");
            return false;
        }
        logger.log(sb.toString());
        return true;
    }

    //**********************************************************
    private static boolean stop_image_similarity_servers(Logger logger)
    //**********************************************************
    {
        List<String> ll = new ArrayList<>();
        ll.add("/bin/bash");
        ll.add("-ch");
        ll.add("kill_vgg19_servers");
        File wd = new File("./python_for_face_reco");

        StringBuilder sb = new StringBuilder();
        if(Execute_command.execute_command_list(ll,wd,20000,sb,logger)==null)
        {
            logger.log("failed to stop image similarity servers");
            return false;
        }
        logger.log(sb.toString());
        return true;
    }


    //**********************************************************
    private static boolean create_venv(Logger logger)
    //**********************************************************
    {
        //     python3 -m venv path/to/venv
        List<String> ll = new ArrayList<>();
        ll.add("python3");
        ll.add("-m");
        ll.add("venv");
        ll.add("venv");
        File wd = new File("./python_for_face_reco");
        StringBuilder sb = new StringBuilder();
        if( Execute_command.execute_command_list(ll,wd,20000,sb,logger) == null)
        {
            logger.log("failed to create venv");
            return false;
        }
        logger.log(sb.toString());
        return true;
    }

*/

}
