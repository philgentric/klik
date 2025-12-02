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
    public static String venv_metal()
    //**********************************************************
    {
        return "~/.klik/venv-metal";
    }
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
            String cmd = get_command_string_to_start_face_recognition_servers(owner,logger);
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
            String cmd = get_command_string_to_start_image_similarity_servers(owner,logger);
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
    public static String get_command_string_to_start_image_similarity_servers(Window owner,Logger logger)
    //**********************************************************
    {
        // if not already started, start the servers monitor
        int udp_port = Embeddings_servers_monitor.get_servers_monitor_udp_port(owner, logger);

        String list_of_ports = "";
        for ( int port : Feature_vector_source_for_image_similarity.ports)
        {
            list_of_ports += port + " ";
        }
        return "source " + venv_metal() + "/bin/activate; cd " +Paths.get("").toAbsolutePath()+"/python_for_ML; ./launch_image_similarity_servers "+ udp_port +" "+ list_of_ports;
    }
    //**********************************************************
    public static String get_command_string_to_stop_image_similarity_servers(Logger logger)
    //**********************************************************
    {
        return "cd "+Paths.get("").toAbsolutePath()+"/python_for_ML; ./kill_image_similarity_servers";
    }



    //**********************************************************
    public static String get_command_string_to_start_face_recognition_servers(Window owner, Logger logger)
    //**********************************************************
    {
        // if not already started, start the servers monitor
        int udp_port = Embeddings_servers_monitor.get_servers_monitor_udp_port(owner,logger);

        String cmd = "source " + venv_metal() + "/bin/activate; cd " +Paths.get("").toAbsolutePath()+"/python_for_ML; ./launch_face_recognition_servers "+ udp_port;
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
            "/opt/homebrew/bin/python3.10 -m venv " + venv_metal(),
            "source " + venv_metal() + "/bin/activate;pip install -U pip"
    };
    static final String[] commands_to_install_metal ={
            "source " + venv_metal() + "/bin/activate;pip install tensorflow-macos tensorflow-metal",
            "source " + venv_metal() + "/bin/activate;cd ./python_for_ML;pip install -r requirements.txt"
    };

    //**********************************************************
    public static void install_python_libs_for_ML(Window owner, Logger logger)
    //**********************************************************
    {
        //String home = System.getProperty(Non_booleans_properties.USER_HOME);
        {
            for (String s : commands_to_install_python)
            {
                Execute_via_script_in_tmp_file.execute(s, true, false,owner, logger);
            }
        }
        {
            for (String s : commands_to_install_metal) {
                Execute_via_script_in_tmp_file.execute(s, true, false,owner, logger);
            }
        }
    }


}
