package klik.image_ml;

import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import klik.image_ml.image_similarity.Feature_vector_source_for_image_similarity;
import klik.look.Look_and_feel_manager;
import klik.util.execute.Execute_command;
import klik.util.log.Logger;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

//**********************************************************
public class Ml_servers_util
//**********************************************************
{
    public static boolean venv_activated = false;
    public static boolean face_reco_servers_started = false;
    public static boolean image_similarity_servers_started = false;

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
        boolean status = Execute_command.execute_command_list(ll,wd,Integer.MAX_VALUE,sb,logger);
        logger.log(sb.toString());

        logger.log("MobileNetV2 image similarity servers started");

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
        boolean status = Execute_command.execute_command_list(ll,wd,20000,sb,logger);
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
        boolean status = Execute_command.execute_command_list(ll,wd,20000,sb,logger);
        logger.log(sb.toString());
        return status;
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
        boolean status = Execute_command.execute_command_list(ll,wd,20000,sb,logger);
        logger.log(sb.toString());
        return status;
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
        boolean status = Execute_command.execute_command_list(ll,wd,20000,sb,logger);
        logger.log(sb.toString());
        return status;
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
        boolean status =   Execute_command.execute_command_list(ll,wd,20000,sb,logger);
        logger.log(sb.toString());
        return status;
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
        boolean status =   Execute_command.execute_command_list(ll,wd,20000,sb,logger);
        logger.log(sb.toString());
        venv_activated = status;
        return status;
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
        boolean status =   Execute_command.execute_command_list(ll,wd,20000,sb,logger);
        logger.log(sb.toString());
        return status;
    }


    static String[] lines =
    {
        "For Image ML to work e.g. search by similarity",
        "Feature vector servers must be first installed (once) see manual",
        "To start the image similarity servers copy paste this line in a terminal:",
        "",

    };

    static String[] lines2 =
            {
                    "For face recognition",
                    "Face detection and specific feature vector servers must be first installed (once) see manual",
                    "To start the face recognition servers copy paste this line in a terminal:",
                    "",

            };

    public static void show_manual()
    {
        Stage stage = new Stage();
        VBox vb = new VBox();
        for ( String l : lines)
        {
            TextField tf = new TextField(l);
            Look_and_feel_manager.set_region_look(tf);
            tf.setEditable(false);
            vb.getChildren().add(tf);
        }

        Path p = Paths.get("");
        {
            String list_of_ports = "";
            for ( int port : Feature_vector_source_for_image_similarity.ports)
            {
                list_of_ports += port + " ";
            }
            String cmd = "source ~/venv-metal/bin/activate; cd "+p.toAbsolutePath().toString()+"/python_for_image_ML; ./launch_MobileNet_servers "+list_of_ports;
            TextField tf = new TextField(cmd);
            Look_and_feel_manager.set_region_look(tf);
            tf.setEditable(false);
            vb.getChildren().add(tf);
        }
        for ( String l : lines2)
        {
            TextField tf = new TextField(l);
            Look_and_feel_manager.set_region_look(tf);
            tf.setEditable(false);
            vb.getChildren().add(tf);
        }
        {
            String cmd = "source ~/venv-metal/bin/activate; cd "+p.toAbsolutePath().toString()+"/python_for_image_ML; ./launch_face_servers ";
            TextField tf = new TextField(cmd);
            Look_and_feel_manager.set_region_look(tf);
            tf.setEditable(false);
            vb.getChildren().add(tf);
        }


        Scene scene = new Scene(vb);
        stage.setScene(scene);
        stage.setWidth(1000);
        stage.setHeight(1000);
        stage.show();
    }
}
