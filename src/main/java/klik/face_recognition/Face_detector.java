package klik.face_recognition;

import javafx.scene.image.Image;
import klik.util.Logger;
import klik.util.Popups;
import klik.util.Stack_trace_getter;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.file.Path;
import java.util.Random;

/*
this face detector reliess on a python server that uses the face detection library
with a super simple API: pass the full file path of an image,
it returns the extracted face as a byte array

there are 4 possible configurations

each is running on different servers, the selector is the port number see in python code:
    1: 'haarcascade_frontalface_default.xml',
    2: 'haarcascade_frontalface_alt.xml',
    3: 'haarcascade_frontalface_alt2.xml',
    4: 'haarcascade_frontalface_alt_tree.xml',

and see launch_server script:
(yes this is dirty distributed config... pffff)


 */
//**********************************************************
public class Face_detector
//**********************************************************
{

    static int[] port_type1 = {8090, 8091}; // haarcascade_frontalface_default.xml has higher recall,  more false positives
    static int[] port_type2 = {8100, 8101}; // haarcascade_frontalface_alt.xml
    static int[] port_type3 = {8110, 8111}; // haarcascade_frontalface_alt2.xml
    static int[] port_type4 = {8050, 8051, 8052, 8053, 8054, 8055, 8056, 8057, 8058, 8059}; // haarcascade_frontalface_alt_tree.xml has higher precision, detects less faces
    static Random  random = new Random();


    public enum Face_detection_type
    {
        high_precision, // type4
        false_positioves, // type1
        alt1,
        alt2,
    }
    //**********************************************************
    public static int get_random_port(Face_detection_type config)
    //**********************************************************
    {
        int[] port = null;
        switch (config)
        {
            case false_positioves:
                port = port_type1;
                break;
            case alt1:
                port = port_type2;
                break;
            case alt2:
                port = port_type3;
                break;
            default: // also high_precision
                port = port_type4;
                break;
        }
        int returned = random.nextInt(port[0],port[0]+port.length);
        return returned;
    }

    record Face_detection_result(Image image, Face_recognition_status status){}

    //**********************************************************
    public static Face_detection_result detect_face(Path path, Face_detection_type config, boolean verbose, Logger logger)
    //**********************************************************
    {
        int port = get_random_port(config);
        String url_string = null;
        try {
            String encodedPath = URLEncoder.encode(path.toAbsolutePath().toString(), "UTF-8");
            url_string = "http://localhost:"+port+"/" + encodedPath;
        } catch (UnsupportedEncodingException e) {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
            return new Face_detection_result(null,Face_recognition_status.error);
        }
        URL url = null;
        try {
            url = new URL(url_string);
        } catch (MalformedURLException e) {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
            return new Face_detection_result(null,Face_recognition_status.error);
        }
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
            return new Face_detection_result(null,Face_recognition_status.error);
        }
        logger.log("Connection established: "+connection.toString());
        // Send a GET request to the server
        try {
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(120_000);
        } catch (ProtocolException e) {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
            return new Face_detection_result(null,Face_recognition_status.error);
        }

        boolean done = false;
        long sleep_time = 100;
        for(int i =0; i < 100 ; i++)
        {
            try {
                connection.connect();
                done = true;
                break;
            } catch (IOException e) {
                //logger.log(Stack_trace_getter.get_stack_trace("" + e));
                logger.log(("                         Face detector: " + e));
            }
            logger.log(" connection to face detection server: going to sleep: "+sleep_time);
            try {
                Thread.sleep(sleep_time);
            } catch (InterruptedException e) {
                logger.log(Stack_trace_getter.get_stack_trace("" + e));
            }
            sleep_time *= 5.0;
            if ( sleep_time > 10_000) sleep_time =10_000;
        }
        if ( !done)
        {
            return new Face_detection_result(null, Face_recognition_status.server_not_reacheable);
        }
        // Get the response code and message
        try {
            int responseCode = connection.getResponseCode();
            //logger.log("Response Code: " + responseCode);
        } catch (IOException e) {
            //logger.log(Stack_trace_getter.get_stack_trace(""+e));
            //logger.log("face detection failed");
            return new Face_detection_result(null,Face_recognition_status.no_face_detected);
        }

        try {
            String responseMessage = connection.getResponseMessage();
            //logger.log("Response Message: " + responseMessage);
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
            return new Face_detection_result(null,Face_recognition_status.no_face_detected);
        }

        // Read the response from the server
        Image img = null;
        try (BufferedInputStream bufferedInputStream = new BufferedInputStream(connection.getInputStream())){
            img = new Image(bufferedInputStream);
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
            return new Face_detection_result(null,Face_recognition_status.no_face_detected);
        }
        // Convert the image data to a BufferedImage object

        if ( Math.abs(img.getHeight()-img.getWidth()) > 2)
        {
            logger.log("non square face discarded i.e. assume face detection failed");
            //Image big = Utils.get_image(path);
            //Utils.display(200,img,big,null,"non square face discarded","",logger);
            return new Face_detection_result(null,Face_recognition_status.no_face_detected);
        }
        return new Face_detection_result(img,Face_recognition_status.face_detected);
    }

    //**********************************************************
    public static void warn_about_face_detector_server(Logger logger)
    //**********************************************************
    {
        Popups.popup_warning(null,"Face detector server not found","You need to start the servers (face detection & face embbedings)",false,logger);
    }

    //**********************************************************
    public static void warn_about_no_face_detected(Logger logger)
    //**********************************************************
    {
        Popups.popup_warning(null,"No face detected","Could not find a face?",false,logger);
    }



}
