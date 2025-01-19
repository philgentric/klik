package klik.image_ml;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import klik.actor.Actor_engine;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import klik.util.log.System_out_logger;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.file.Path;

//**********************************************************
public abstract class Feature_vector_source
//**********************************************************
{
    //Feature_vector get_feature_vector_from_server(Path path, Logger logger);

    protected abstract int get_random_port();


    static long start = System.currentTimeMillis();
    static long tx_count = 0;
    static long SUM_dur = 0;
    static{
        Logger l = new System_out_logger();
        Runnable r = () ->
        {
            for(;;)
            {
                try {
                    Thread.sleep(60_000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                get_embeddings_stats(l);
            }
        };
        Actor_engine.execute(r, l);
    }


    //**********************************************************
    public Feature_vector get_feature_vector_from_server(Path path, Logger logger)
    //**********************************************************
    {
        long local_start = System.currentTimeMillis();
        if ( path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BAD!"));
            return null;
        }
        //Ml_servers_util.init_image_similarity(logger);
        int random_port = get_random_port();
        Feature_vector x = Feature_vector_source.get_feature_vector_from_server_generic(path, random_port, logger);
        long local_end = System.currentTimeMillis();
        long local_dur = local_end - local_start;
        SUM_dur += local_dur;
        tx_count++;
        return x;
    }

    //**********************************************************
    public static void get_embeddings_stats(Logger logger)
    //**********************************************************
    {
        long end = System.currentTimeMillis();
        long local_dur = end - start;
        double dur_s = (double)local_dur/1_000.0;
        logger.log("TX_rate="+(double)tx_count/(double)dur_s+" tx/s (tx_count="+tx_count+" for: "+dur_s+" secconds)");
        logger.log("total server call time="+ SUM_dur/1000 +"s, average concurency="+(double) SUM_dur /(double)local_dur);
    }

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
