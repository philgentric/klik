package klik.face_recognition;

import klik.util.Logger;
import klik.util.execute.Execute_command;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

//**********************************************************
public class Launch_python_servers
//**********************************************************
{
    public static boolean is_initialized = false;

    //**********************************************************
    public static boolean init(Logger logger)
    //**********************************************************
    {
        if ( !init_python_env(logger))
        {
            logger.log("failed to init python env");
            return false;
        }
        if ( !init_face_detection(logger))
        {
            logger.log("failed to init face detection");
            return false;
        }
        if ( !init_image_embeddings(logger))
        {
            logger.log("failed to init image embbedings");
            return false;
        }
        is_initialized = true;
        logger.log("OK: init face detection done");

        return true;
    }

    //**********************************************************
    private static boolean init_face_detection(Logger logger)
    //**********************************************************
    {
        List<String> ll = new ArrayList<>();
        ll.add("python3");
        ll.add("face_detection_server.py");
        File wd = new File("./python_for_face_reco");

        StringBuilder sb = new StringBuilder();
        boolean status = Execute_command.execute_command_list(ll,wd,20000,sb,logger);
        logger.log(sb.toString());
        return status;
    }

    //**********************************************************
    private static boolean init_image_embeddings(Logger logger)
    //**********************************************************
    {
        List<String> ll = new ArrayList<>();
        ll.add("python3");
        ll.add("embeddings_server.py");
        File wd = new File("./python_for_face_reco");
        StringBuilder sb = new StringBuilder();
        boolean status =   Execute_command.execute_command_list(ll,wd,20000,sb,logger);
        logger.log(sb.toString());
        return status;
    }

    //**********************************************************
    private static boolean init_python_env(Logger logger)
    //**********************************************************
    {
        logger.log("init_python_env");
        List<String> ll = new ArrayList<>();
        ll.add("/bin/bash");
        ll.add("-ch");
        ll.add("python3");
        ll.add("-m");
        ll.add("venv");
        ll.add("../../venv;");
        ll.add("source ../../venv/bin/activate;");
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


    //**********************************************************
    private static boolean launch_venv(Logger logger)
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
        // special trick c.f. stackOverFlow
        List<String> ll = new ArrayList<>();
        ll.add("/bin/bash");
        ll.add("-ch");
        ll.add("source venv/bin/activate");
        File wd = new File("./python_for_face_reco");
        StringBuilder sb = new StringBuilder();
        boolean status =   Execute_command.execute_command_list(ll,wd,20000,sb,logger);
        logger.log(sb.toString());
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


}
