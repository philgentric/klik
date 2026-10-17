// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.machine_learning.python_via_html_ML;

import javafx.scene.image.Image;
import klikr.machine_learning.face_recognition.Face_recognition_in_image_status;
import klikr.machine_learning.python_via_html_ML.monitoring.UDP_traffic_monitor;
import klikr.settings.boolean_features.Feature;
import klikr.settings.boolean_features.Feature_cache;
import klikr.util.Kontext;
import klikr.util.log.Logger;
import klikr.util.ui.Popups;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.file.Path;

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
    static final int MINIMUM_ACCEPTABLE_FACE_SIZE = 200;

    public record Face_detection_result(Image image, Face_recognition_in_image_status status){}

    static long start;
    static long total_server_ns = 0;
    static long count =0;

    //**********************************************************
    public static Face_detection_result detect_face(Path path, ML_server_type face_detection_type, Kontext context)
    //**********************************************************
    {

        if ( Feature_cache.get(Feature.Enable_ML_server_debug))
        {
            UDP_traffic_monitor.start_servers_monitoring(context);
        }

        start = System.nanoTime();
        int port =  Load_balancer.get_random_active_port(face_detection_type, context);
        if ( port == -1 )
        {
             context.log("Warning: could not find 1 active server for "+face_detection_type);
             context.log("PLEASE WAIT ! A Request has been made for "+face_detection_type+" servers to be started");
            return new Face_detection_result(null, Face_recognition_in_image_status.error);
        }
        String url_string = null;
        try {
            String encodedPath = URLEncoder.encode(path.toAbsolutePath().toString(), "UTF-8");
            url_string = "http://127.0.0.1:"+port+"/" + encodedPath;
        } catch (UnsupportedEncodingException e) {
             context.log_with_stack_trace_from_throwable("",e);
            return new Face_detection_result(null, Face_recognition_in_image_status.error);
        }
        URL url = null;
        try {
            url = new URL(url_string);
        } catch (MalformedURLException e) {
             context.log_with_stack_trace_from_throwable("",e);
            return new Face_detection_result(null, Face_recognition_in_image_status.error);
        }
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
             context.log_with_stack_trace_from_throwable("",e);
            return new Face_detection_result(null, Face_recognition_in_image_status.error);
        }
        if ( dbg)  context.log("Face detection client: connection ready: "+connection.toString());
        // Send a GET request to the server
        try {
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(0);// infinite
        } catch (ProtocolException e) {
             context.log_with_stack_trace_from_throwable("",e);
            return new Face_detection_result(null, Face_recognition_in_image_status.server_not_reacheable);
        }


        boolean done = false;
        long effectively_slept =0;
        try {
            connection.connect();
            done = true;
            //break;
        } catch (IOException e) {
            if ( dbg)  context.log(("                         Face detector: " + e));
        }




        if ( !done)
        {
             context.log("Face detection: connection to server failed for "+face_detection_type.name());

            return new Face_detection_result(null, Face_recognition_in_image_status.server_not_reacheable);
        }
        // Get the response code and error_message
        try {
            int response_code = connection.getResponseCode();
        } catch (IOException e) {
            // context.log_with_stack_trace(""+e));
            // context.log("face detection failed");
            return new Face_detection_result(null, Face_recognition_in_image_status.no_face_detected);
        }

        try {
            String response_message = connection.getResponseMessage();
            // context.log("Response Message: " + responseMessage);
        } catch (IOException e) {
             context.log_with_stack_trace_from_throwable("",e);
            return new Face_detection_result(null, Face_recognition_in_image_status.no_face_detected);
        }

        // Read the response from the server
        Image face_image = null;
        try (BufferedInputStream bufferedInputStream = new BufferedInputStream(connection.getInputStream())){
            face_image = new Image(bufferedInputStream);
        }
        catch (IOException e)
        {
             context.log_with_stack_trace_from_throwable("",e);
            return new Face_detection_result(null, Face_recognition_in_image_status.no_face_detected);
        }
        finally
        {
            connection.disconnect();
        }

        if ( face_detection_type == ML_server_type.MTCNN)
        {
            if ((face_image.getHeight() < MINIMUM_ACCEPTABLE_FACE_SIZE) || (face_image.getWidth() < MINIMUM_ACCEPTABLE_FACE_SIZE) )
            {
                 context.log("things smaller than "+ MINIMUM_ACCEPTABLE_FACE_SIZE +" pixels are discarded i.e. we assume face detection failed with "+face_detection_type.name());
                //Image big = Utils.get_image(path);
                //Utils.display(200,img,big,null,"face discarded as too small","",logger);
                report_time(context.logger(), effectively_slept);
                return new Face_detection_result(null, Face_recognition_in_image_status.no_face_detected);
            }
        }
        else
        {
            // Haars
            if (Math.abs(face_image.getHeight() - face_image.getWidth()) > 2)
            {
                 context.log("non square face discarded i.e. we assume face detection failed for "+face_detection_type.name());
                //Image big = Utils.get_image(path);
                //Utils.display(200,img,big,null,"non square face discarded","",logger);
                report_time(context.logger(), effectively_slept);
                return new Face_detection_result(null, Face_recognition_in_image_status.no_face_detected);
            }
        }

        report_time(context.logger(), effectively_slept);
         context.log("Face detector: face detected by "+face_detection_type.name());

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
    public static void warn_about_face_detector_server(Kontext context)
    //**********************************************************
    {
        Popups.popup_warning(Logger.warning+" Face detector server not found","Need to start the servers",true, context);
    }

    //**********************************************************
    public static void warn_about_no_face_detected(Kontext  context)
    //**********************************************************
    {
        Popups.popup_warning(Logger.warning+" No face detected","Could not find a face?",true, context);
    }



}
