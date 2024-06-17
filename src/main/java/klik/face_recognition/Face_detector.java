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
    private static final boolean dbg = false;

    // MTCNN face detector servers
    static int[] port_MTCNN = {8040, 8041, 8042, 8043, 8044, 8045, 8046, 8047, 8048, 8049};

    // haarcascade_frontalface_alt_tree.xml has higher precision, detects less faces
    static int[] port_haars_high_precision = {8080, 8081};//, 8082, 8083, 8084};//, 8085, 8086, 8087, 8088, 8089};

    // haarcascade_frontalface_default.xml has higher recall,  more false positives
    static int[] port_haars_false_positives = {8090, 8091};

    // haarcascade_frontalface_alt.xml
    static int[] port_haars_alt1 = {8100, 8101};

    // haarcascade_frontalface_alt2.xml
    static int[] port_haars_alt2 = {8110, 8111};
    static Random  random = new Random();

    static final int MINIMUM_ACCEPTABLE_FACE_SIZE = 200;

    //**********************************************************
    public static int get_random_port(Face_detection_type config)
    //**********************************************************
    {
        int[] port = null;
        switch (config)
        {
            case haars_false_positioves:
                port = port_haars_false_positives;
                break;
            case haars_alt1:
                port = port_haars_alt1;
                break;
            case haars_alt2:
                port = port_haars_alt2;
                break;
            case haars_high_precision:
                port = port_haars_high_precision;
                break;
            case MTCNN:
                port = port_MTCNN;
        }
        int returned = random.nextInt(port[0],port[0]+port.length);
        return returned;
    }

    record Face_detection_result(Image image, Face_recognition_status status){}


    static long start;
    static long total_server_ns = 0;
    static long count =0;

    //**********************************************************
    public static Face_detection_result detect_face(Path path, Face_detection_type face_detection_type, boolean verbose, Logger logger)
    //**********************************************************
    {
        start = System.nanoTime();
        int port = get_random_port(face_detection_type);
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
        long effectively_slept =0;
        long sleep_time = 100;
        for(;;)
        {
            try {
                connection.connect();
                done = true;
                break;
            } catch (IOException e) {
                //logger.log(Stack_trace_getter.get_stack_trace("" + e));
                if ( dbg) logger.log(("                         Face detector: " + e));
                //Popups.popup_warning(null,"ohoh","connection to face detection server failed, did you start it?",false,logger);
            }
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
            }
        }



        if ( !done)
        {
            return new Face_detection_result(null, Face_recognition_status.server_not_reacheable);
        }
        // Get the response code and message
        try {
            int response_code = connection.getResponseCode();
        } catch (IOException e) {
            //logger.log(Stack_trace_getter.get_stack_trace(""+e));
            //logger.log("face detection failed");
            return new Face_detection_result(null,Face_recognition_status.no_face_detected);
        }

        try {
            String response_message = connection.getResponseMessage();
            //logger.log("Response Message: " + responseMessage);
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
            return new Face_detection_result(null,Face_recognition_status.no_face_detected);
        }

        // Read the response from the server
        Image face_image = null;
        try (BufferedInputStream bufferedInputStream = new BufferedInputStream(connection.getInputStream())){
            face_image = new Image(bufferedInputStream);
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
            return new Face_detection_result(null,Face_recognition_status.no_face_detected);
        }

        if ( face_detection_type == Face_detection_type.MTCNN)
        {
            if ((face_image.getHeight() < MINIMUM_ACCEPTABLE_FACE_SIZE) || (face_image.getWidth() < MINIMUM_ACCEPTABLE_FACE_SIZE) )
            {
                logger.log("things smaller than "+ MINIMUM_ACCEPTABLE_FACE_SIZE +" pixels are discarded i.e. we assume face detection failed");
                //Image big = Utils.get_image(path);
                //Utils.display(200,img,big,null,"face discarded as too small","",logger);
                report_time(logger,effectively_slept);
                return new Face_detection_result(null, Face_recognition_status.no_face_detected);
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
                return new Face_detection_result(null, Face_recognition_status.no_face_detected);
            }
        }

        report_time(logger, effectively_slept);
        return new Face_detection_result(face_image,Face_recognition_status.face_detected);
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
