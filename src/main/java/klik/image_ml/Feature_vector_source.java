package klik.image_ml;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.file.Path;

//**********************************************************
public interface Feature_vector_source
//**********************************************************
{
    Feature_vector get_feature_vector_from_server(Path path, Logger logger);


    //**********************************************************
    static Feature_vector parse_json(String response, Logger logger)
    //**********************************************************
    {
        Gson gson = new GsonBuilder().create();
        try
        {
            Feature_vector fv = gson.fromJson(response, Feature_vector.class);
            //logger.log("parsed a feature vector, length: " + fv.features.length);
            return fv;
        }
        catch (com.google.gson.JsonSyntaxException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace("parse_json: "+e));
            return null;
        }
    }

    //**********************************************************
    static Feature_vector get_feature_vector_from_server_generic(Path path, int random_port, Logger logger)
    //**********************************************************
    {
        if ( path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BAD!"));
            return null;
        }

        String url_string = null;
        try {
            String encodedPath = URLEncoder.encode(path.toAbsolutePath().toString(), "UTF-8");
            url_string = "http://localhost:" + random_port + "/" + encodedPath;
        } catch (UnsupportedEncodingException e) {
            logger.log(Stack_trace_getter.get_stack_trace("get_feature_vector_from_server_generic: "+e));
            return null;
        }
        URL url = null;
        try {
            url = new URL(url_string);
        } catch (MalformedURLException e) {
            logger.log(Stack_trace_getter.get_stack_trace("get_feature_vector_from_server_generic: "+e));
            return null;
        }
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace("get_feature_vector_from_server_generic:"+e));
            return null;
        }
        try {
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(0); // infinite
        } catch (ProtocolException e) {
            logger.log(Stack_trace_getter.get_stack_trace("get_feature_vector_from_server_generic: "+e));
            return null;
        }
        try {
            connection.connect();
        } catch (IOException e) {
            //logger.log(Stack_trace_getter.get_stack_trace(""+e));
            logger.log(("get_feature_vector_from_server_generic: "+e));
            return null;
        }
        try {
            int response_code = connection.getResponseCode();
        } catch (IOException e) {
            //logger.log(Stack_trace_getter.get_stack_trace(""+e));
            logger.log(("get_feature_vector_from_server_generic:"+e));
            return null;
        }
        try {
            String response_message = connection.getResponseMessage();
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace("get_feature_vector_from_server_generic: "+e));
            return null;
        }

        // Read the JSON response one character at a time
        StringBuffer sb = new StringBuffer();
        try(BufferedInputStream bufferedInputStream = new BufferedInputStream(connection.getInputStream()))
        {
            for(;;)
            {
                int c = bufferedInputStream.read();
                if ( c == -1) break;
                //System.out.print((char)c);
                sb.append((char)c);
            }
        } catch (IOException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace("get_feature_vector_from_server_generic: "+e));
            return null;
        }

        // Use a JSON parser library (e.g., Jackson) to parse the JSON string
        String json = sb.toString();
        //logger.log("json ="+json);
        Feature_vector fv = Feature_vector_source.parse_json(json,logger);
        if ( fv == null) {
            logger.log("json parsing failed: feature vector is null");
        }
        else {
            //logger.log("feature vector size:"+fv.features.length);
        }

        return fv;
    }
}
