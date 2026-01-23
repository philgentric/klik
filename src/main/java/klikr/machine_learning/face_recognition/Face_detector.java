// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.machine_learning.face_recognition;

import javafx.scene.image.Image;
import javafx.stage.Window;
import klikr.machine_learning.ML_registry_discovery;
import klikr.machine_learning.ML_server_type;
import klikr.machine_learning.ML_servers_status;
import klikr.machine_learning.ML_service_type;
import klikr.machine_learning.feature_vector.UDP_traffic_monitor;
import klikr.properties.boolean_features.Feature;
import klikr.properties.boolean_features.Feature_cache;
import klikr.util.log.Logger;
import klikr.util.ui.Popups;
import klikr.util.log.Stack_trace_getter;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;

/*
this face detector relies on a python server that uses the face detection library
with a super simple API: pass the full file path of an image,
it returns the extracted face as a byte array

there are several possible configurations each is running on different servers,
the selector is the port number see below
 */
//**********************************************************
public class Face_detector
//**********************************************************
{
    private static final boolean dbg = false;
    private static final boolean ultra_dbg = false;
    static Random  random = new Random();
    static final int MINIMUM_ACCEPTABLE_FACE_SIZE = 200;

    //**********************************************************
    public static int get_random_port(Face_detection_type face_detection_type, Window owner, Logger logger)
    //**********************************************************
    {
        ML_server_type ml_server_type = ML_server_type.MTCNN;
        switch (face_detection_type)
        {
            case alt_default, alt1, alt2, alt_tree:
                ml_server_type = ML_server_type.Haars;
                break;
        }
        ML_servers_status status = ML_registry_discovery.find_active_servers(new ML_service_type(ml_server_type, face_detection_type), owner, logger);
        if ( status.available_ports().isEmpty())
        {
            if (dbg) logger.log("No active face detection servers found for :"+face_detection_type);
            return -1;
        }

        return status.available_ports().get(random.nextInt(status.available_ports().size()));
    }


    record Face_detection_result(Image image, Face_recognition_in_image_status status){}


    static long start;
    static long total_server_ns = 0;
    static long count =0;

    //**********************************************************
    public static Face_detection_result detect_face(Path path, Face_detection_type face_detection_type, Window owner, Logger logger)
    //**********************************************************
    {

        if ( Feature_cache.get(Feature.Enable_ML_server_debug))
        {
            UDP_traffic_monitor.start_servers_monitoring(owner, logger);
        }

        start = System.nanoTime();
        int port = get_random_port(face_detection_type,owner,logger);
        if ( port == -1 )
        {
            logger.log("Warning: could not find 1 active server for "+face_detection_type);
            ML_server_type ml_server_type = ML_server_type.MTCNN;
            if ( face_detection_type != Face_detection_type.MTCNN)
            {
                ml_server_type = ML_server_type.Haars;
            }
            logger.log("PLEASE WAIT ! A Request has been made for "+face_detection_type+" servers to be started");
            ML_registry_discovery.find_active_servers(new ML_service_type(ml_server_type,face_detection_type), owner,logger);
            return new Face_detection_result(null, Face_recognition_in_image_status.error);
        }
        String url_string = null;
        try {
            String encodedPath = URLEncoder.encode(path.toAbsolutePath().toString(), "UTF-8");
            url_string = "http://127.0.0.1:"+port+"/" + encodedPath;
        } catch (UnsupportedEncodingException e) {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
            return new Face_detection_result(null, Face_recognition_in_image_status.error);
        }
        URL url = null;
        try {
            url = new URL(url_string);
        } catch (MalformedURLException e) {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
            return new Face_detection_result(null, Face_recognition_in_image_status.error);
        }
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
            return new Face_detection_result(null, Face_recognition_in_image_status.error);
        }
        if ( dbg) logger.log("Face detection client: connection ready: "+connection.toString());
        // Send a GET request to the server
        try {
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(0);// infinite
        } catch (ProtocolException e) {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
            return new Face_detection_result(null, Face_recognition_in_image_status.server_not_reacheable);
        }

        boolean done = false;
        long effectively_slept =0;
        long sleep_time = 100;
        //for(;;)
        {
            try {
                connection.connect();
                done = true;
                //break;
            } catch (IOException e) {
                if ( dbg) logger.log(("                         Face detector: " + e));
            }
            /*
            if ( dbg) logger.log(" connection to face detection server: going to sleep: "+sleep_time);
            try {
                effectively_slept += sleep_time;
                Thread.sleep(sleep_time);
            } catch (InterruptedException e) {
                logger.log(Stack_trace_getter.get_stack_trace("" + e));
            }
            sleep_time *= 5.0;
            if ( sleep_time > 10_000) sleep_time =10_000;
            if ( effectively_slept > 10*60*1000) // 10 minutes
            {
                logger.log("Face detection: giving up, server not reachable");
                break;
            }*/
        }



        if ( !done)
        {
            logger.log("Face detection: failed");

            return new Face_detection_result(null, Face_recognition_in_image_status.server_not_reacheable);
        }
        // Get the response code and error_message
        try {
            int response_code = connection.getResponseCode();
        } catch (IOException e) {
            //logger.log(Stack_trace_getter.get_stack_trace(""+e));
            //logger.log("face detection failed");
            return new Face_detection_result(null, Face_recognition_in_image_status.no_face_detected);
        }

        try {
            String response_message = connection.getResponseMessage();
            //logger.log("Response Message: " + responseMessage);
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
            return new Face_detection_result(null, Face_recognition_in_image_status.no_face_detected);
        }

        // Read the response from the server
        Image face_image = null;
        try (BufferedInputStream bufferedInputStream = new BufferedInputStream(connection.getInputStream())){
            face_image = new Image(bufferedInputStream);
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
            return new Face_detection_result(null, Face_recognition_in_image_status.no_face_detected);
        }

        if ( face_detection_type == Face_detection_type.MTCNN)
        {
            if ((face_image.getHeight() < MINIMUM_ACCEPTABLE_FACE_SIZE) || (face_image.getWidth() < MINIMUM_ACCEPTABLE_FACE_SIZE) )
            {
                logger.log("things smaller than "+ MINIMUM_ACCEPTABLE_FACE_SIZE +" pixels are discarded i.e. we assume face detection failed");
                //Image big = Utils.get_image(path);
                //Utils.display(200,img,big,null,"face discarded as too small","",logger);
                report_time(logger,effectively_slept);
                return new Face_detection_result(null, Face_recognition_in_image_status.no_face_detected);
            }
        }
        else
        {
            if (Math.abs(face_image.getHeight() - face_image.getWidth()) > 2)
            {
                logger.log("non square face discarded i.e. we assume face detection failed");
                //Image big = Utils.get_image(path);
                //Utils.display(200,img,big,null,"non square face discarded","",logger);
                report_time(logger,effectively_slept);
                return new Face_detection_result(null, Face_recognition_in_image_status.no_face_detected);
            }
        }

        report_time(logger, effectively_slept);
        logger.log("face detected");

        return new Face_detection_result(face_image, Face_recognition_in_image_status.face_detected);
    }

    //**********************************************************
    private static void report_time(Logger logger, long tot_sleep)
    //**********************************************************
    {
        long end =  System.nanoTime();
        total_server_ns += (end-start);
        count++;
        logger.log("\n==>face detection took "+String.format("%.2f",(end-start)/1_000_000.0)
                +"milliseconds, including sleep="+tot_sleep+", average = "+String.format("%.2f",total_server_ns/count/1_000_000.0));
    }

    //**********************************************************
    public static void warn_about_face_detector_server(Window owner, Logger logger)
    //**********************************************************
    {
        Popups.popup_warning("❗ Face detector server not found","Need to start the servers",true,owner,logger);
    }

    //**********************************************************
    public static void warn_about_no_face_detected(Window owner,Logger logger)
    //**********************************************************
    {
        Popups.popup_warning("❗ No face detected","Could not find a face?",true,owner,logger);
    }



}
