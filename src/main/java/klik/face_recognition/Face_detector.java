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
 */
//**********************************************************
public class Face_detector
//**********************************************************
{
    int port[] = {8002,8003,8004,8005};
    static Random  random = new Random();
    public static int get_random_port()
    {
        int port = random.nextInt(8002,8006);
        return port;
    }

    record Face_detection_result(Image image, Face_recognition_status status){}

    //**********************************************************
    public static Face_detection_result detect_face(Path path, boolean verbose, Logger logger)
    //**********************************************************
    {
        int port = get_random_port();
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
        for(int i =0; i < 100 ; i++) {
            try {
                connection.connect();
                done = true;
                break;
            } catch (IOException e) {
                logger.log(Stack_trace_getter.get_stack_trace("" + e));
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                logger.log(Stack_trace_getter.get_stack_trace("" + e));
            }
        }
        if ( !done)
        {
            return new Face_detection_result(null, Face_recognition_status.server_not_reacheable);
        }
        // Get the response code and message
        try {
            int responseCode = connection.getResponseCode();
            logger.log("Response Code: " + responseCode);
        } catch (IOException e) {
            //logger.log(Stack_trace_getter.get_stack_trace(""+e));
            logger.log("face detection failed");
            return new Face_detection_result(null,Face_recognition_status.no_face_detected);
        }

        try {
            String responseMessage = connection.getResponseMessage();
            logger.log("Response Message: " + responseMessage);
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
            return new Face_detection_result(null,Face_recognition_status.no_face_detected);
        }

        // Read the response from the server
        BufferedInputStream bufferedInputStream = null;
        try {
            bufferedInputStream = new BufferedInputStream(connection.getInputStream());
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
            return new Face_detection_result(null,Face_recognition_status.no_face_detected);
        }
        // Convert the image data to a BufferedImage object
        Image img = new Image(bufferedInputStream);
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
