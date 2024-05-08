package klik.face_recognition;

import javafx.scene.image.Image;
import klik.actor.Aborter;
import klik.actor.Actor;
import klik.actor.Actor_engine;
import klik.actor.Message;
import klik.properties.Static_application_properties;
import klik.util.Logger;
import klik.util.Stack_trace_getter;

import java.io.*;
import java.net.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

//**********************************************************
public class Prototype_adder_actor implements Actor
//**********************************************************
{
    public static final boolean dbg = false;

    public final static String EXTENSION_FOR_EP = "prototype";
    public static final int K_of_KNN = 5;
    private final Face_recognition_service service;

    //**********************************************************
    Prototype_adder_actor(Face_recognition_service service_)
    //**********************************************************
    {
        service = service_;
    }


    //**********************************************************
    @Override
    public String run(Message m)
    //**********************************************************
    {
        Prototype_adder_message pam = (Prototype_adder_message)m;
        Face_recognition_status frs = add_prototype_image_face(pam.face,pam.label,true);
        return frs.name();
    }


    //**********************************************************
    public Face_recognition_status add_prototype_image_face(Image face, String label, boolean and_save)
    //**********************************************************
    {
        String name = label+ "_"+ UUID.randomUUID();
        Path path = Face_recognition_service.write_tmp_image(face, service.face_recognizer_path, name,service.logger);
        Feature_vector fv = get_image_embeddings(path, service.logger);
        if ( fv ==null)
        {
            service.logger.log("FATAL: prototype not added as the feature vector is null");
            return Face_recognition_status.no_feature_vector;
        }

        Embeddings_prototype ep = new Embeddings_prototype(face, fv, label, name);
        service.names_to_embeddings.put(name,ep);
        service.embeddings_prototypes.add(ep);
        if ( !service.labels.contains(label)) service.labels.add(label);
        if ( and_save) save_ep(ep);
        service.logger.log("added prototype image face with label ="+label);
        //display(face,label);
        return Face_recognition_status.feature_vector_ready;
    }


    //**********************************************************
    public static Feature_vector get_image_embeddings(Path path, Logger logger)
    //**********************************************************
    {
        String url_string = null;
        try {
            String encodedPath = URLEncoder.encode(path.toAbsolutePath().toString(), "UTF-8");
            url_string = "http://localhost:8001/" + encodedPath;
        } catch (UnsupportedEncodingException e) {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
            return null;
        }
        URL url = null;
        try {
            url = new URL(url_string);
        } catch (MalformedURLException e) {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
            return null;
        }
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
            return null;
        }
        logger.log("Connection established: "+connection.toString());
        // Send a GET request to the server
        try {
            connection.setRequestMethod("GET");
        } catch (ProtocolException e) {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
            return null;
        }
        try {
            connection.connect();
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
            return null;
        }

        // Get the response code and message
        try {
            int responseCode = connection.getResponseCode();
            logger.log("Response Code: " + responseCode);
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
            return null;
        }

        try {
            String responseMessage = connection.getResponseMessage();
            logger.log("Response Message: " + responseMessage);
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
            return null;
        }

        // Read the response from the server
        BufferedInputStream bufferedInputStream = null;
        try {
            bufferedInputStream = new BufferedInputStream(connection.getInputStream());
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
            return null;
        }

        StringBuffer sb = new StringBuffer();
        for(;;)
        {
            try {
                int c = bufferedInputStream.read();
                if ( c == -1) break;
                //System.out.print((char)c);
                sb.append((char)c);
            } catch (IOException e) {
                logger.log(Stack_trace_getter.get_stack_trace(""+e));
                return null;
            }
        }

        // Use a JSON parser library (e.g., Jackson) to parse the JSON string
        String json = sb.toString();
        //logger.log("json ="+json);
        Feature_vector fv = Feature_vector.parse_json(json,logger);
        if ( fv == null) {
            logger.log("feature vector is null");
        }
        else {
            //logger.log("feature vector ="+fv.to_string());
            logger.log("feature vector size:"+fv.features.length);
        }

        return fv;
    }



    //**********************************************************
    private void save_ep(Embeddings_prototype prototype)
    //**********************************************************
    {
        String filename = Face_recognition_service.make_prototype_path(service.face_recognizer_path, prototype.name()).toAbsolutePath().toString();
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename)))
        {
            writer.println(prototype.label());
            writer.println(prototype.feature_vector().features.length );
            for ( double d : prototype.feature_vector().features)
            {
                writer.println(d);
            }
        }
        catch (IOException e)
        {
            service.logger.log(Stack_trace_getter.get_stack_trace(""+e));
        }
    }
}
