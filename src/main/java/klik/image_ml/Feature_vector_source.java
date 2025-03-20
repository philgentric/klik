package klik.image_ml;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.stage.Stage;
import klik.actor.Actor_engine;
import klik.image_ml.image_similarity.Feature_vector_source_for_image_similarity;
import klik.util.execute.Execute_command;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import klik.util.log.System_out_logger;
import klik.util.ui.Jfx_batch_injector;
import klik.util.ui.Popups;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

//**********************************************************
public abstract class Feature_vector_source
//**********************************************************
{
    protected abstract int get_random_port();
    static long start = System.currentTimeMillis();
    static long tx_count = 0;
    static long SUM_dur = 0;
    static{
        Logger l = new System_out_logger("embeddings");
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

    static AtomicBoolean server_started = new AtomicBoolean(false);
    static AtomicBoolean popup_done = new AtomicBoolean(false);

    //**********************************************************
    static Feature_vector get_feature_vector_from_server_generic(Path path, int random_port, Logger logger)
    //**********************************************************
    {
        if (!server_started.get())
        {

            if ( check(logger))
            {
                server_started.set(true);
            }
            else
            {
                if ( !popup_done.get())
                {
                    popup_done.set(true);
                    popup(null,logger);
                }
                return null;
            }
        }

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
            server_started.set(false);
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

    //**********************************************************
    private static boolean check(Logger logger)
    //**********************************************************
    {
        List<String> list = new ArrayList<>();
        list.add("pgrep");
        list.add("-f");
        list.add("python");
        //list.add("python.*run_server.*");
        StringBuilder sb = new StringBuilder();
        File wd = new File (".");
        if (!Execute_command.execute_command_list(list, wd, 2000, sb, logger))
        {
            logger.log("failed:\n"+ sb );
            return false;
        }
        // scan sb looking for integers (PIDs)
        String result = sb.toString();
        String[] parts = result.split("\\s+"); // Split on non-digit sequences
        for (String part : parts)
        {
            try
            {
                int pid = Integer.parseInt(part);
                //System.out.println("found PID: " + part+" in:"+result);
                return true;
            }
            catch ( NumberFormatException e)
            {

            }
        }
        // no PIDs found
        return false;
    }
    //**********************************************************
    private static void popup(Stage owner, Logger logger)
    //**********************************************************
    {
        Jfx_batch_injector.inject(() -> Popups.popup_warning(owner,
                "Servers not started.",
                "This feature requires to start the Image Feature Extraction Servers\n" +
                        "Look in the menu 'preferences' for instructions",
                false,logger), logger);

    }
}
