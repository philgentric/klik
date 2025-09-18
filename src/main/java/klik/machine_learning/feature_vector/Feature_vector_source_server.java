package klik.machine_learning.feature_vector;

import javafx.stage.Window;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.util.execute.Execute_command;
import klik.util.log.Logger;
import klik.util.log.Logger_factory;
import klik.util.log.Stack_trace_getter;
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
public abstract class Feature_vector_source_server implements Feature_vector_source
//**********************************************************
{

    static AtomicBoolean server_started = new AtomicBoolean(false);
    static AtomicBoolean popup_done = new AtomicBoolean(false);

    protected final Aborter aborter;
    public Feature_vector_source_server(Aborter aborter)
    {
        this.aborter = aborter;
    }

    protected abstract int get_random_port();
    static long start = System.currentTimeMillis();
    static long tx_count = 0;
    static long SUM_dur = 0;
    static{
        Logger l = Logger_factory.get("embeddings");
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
    public Feature_vector get_feature_vector_from_server(Path path, Window owner, Logger logger)
    //**********************************************************
    {
        if ( aborter.should_abort())
        {
            logger.log("aborting Feature_vector_source::get_feature_vector_from_server, reason: "+aborter.reason());
            return null;
        }
        long local_start = System.currentTimeMillis();
        if ( path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BAD!"));
            return null;
        }
        int random_port = get_random_port();
        Feature_vector x = Feature_vector_source_server.get_feature_vector_from_server_generic(path, random_port, owner, aborter,logger);

        if ( x==null)
        {
            logger.log("get_feature_vector_from_server_generic: FAILED for:->"+path+"<- random_port="+random_port);
            if ( path.toFile().exists())
            {
                logger.log("file exists, "+path.toFile().length()+" but feature vector is null");
            }
            else logger.log("file does not exist: "+path);
        }
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
        logger.log("feature vector TX_rate="+(double)tx_count/(double)dur_s+" tx/s (tx_count="+tx_count+" for: "+dur_s+" secconds)");
        logger.log("total server call time="+ SUM_dur/1000 +"s, average concurency="+(double) SUM_dur /(double)local_dur);
    }

    //**********************************************************
    static Feature_vector parse_json(String response, Logger logger)
    //**********************************************************
    {
        //logger.log("going to parse a JSON feature vector ->" + response + "<-");

        // expecting {"features":[0.1,0.2,0.3,...]}
        response = response.trim();
        if ( !response.startsWith("{") || !response.endsWith("}"))
        {
            logger.log("json parsing failed: does not start with { or end with }");
            return null;
        }
        int features_index = response.indexOf("\"features\"");
        if ( features_index == -1)
        {
            logger.log("json parsing failed: no \"features\" key found");
            return null;
        }
        int colon_index = response.indexOf(":", features_index);
        if ( colon_index == -1)
        {
            logger.log("json parsing failed: no : after \"features\" key");
            return null;
        }
        int open_bracket_index = response.indexOf("[", colon_index);
        if ( open_bracket_index == -1)
        {
            logger.log("json parsing failed: no [ after \"features\":");
            return null;
        }
        int close_bracket_index = response.indexOf("]", open_bracket_index);
        if ( close_bracket_index == -1)
        {
            logger.log("json parsing failed: no ] after \"features\":[");
            return null;
        }
        String array_string = response.substring(open_bracket_index + 1, close_bracket_index).trim();
        if ( array_string.isEmpty())
        {
            logger.log("json parsing failed: empty features array");
            return null;
        }
        String[] parts = array_string.split(",");
        double[] features = new double[parts.length];
        for ( int i = 0; i < parts.length; i++)
        {
            try
            {
                features[i] = Double.parseDouble(parts[i].trim());
            }
            catch ( NumberFormatException e)
            {
                logger.log(Stack_trace_getter.get_stack_trace("parse_json: NumberFormatException for part="+parts[i]+" "+e));
                return null;
            }
        }
        Feature_vector_double fv = new Feature_vector_double(features);
        logger.log("parsed a feature vector, length: " + fv.features.length);
        return fv;

    }

    //**********************************************************
    static Feature_vector get_feature_vector_from_server_generic(Path path, int random_port, Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        if ( aborter.should_abort())
        {
            logger.log("aborting(1) Feature_vector_source::get_feature_vector_from_server_generic reason: "+aborter.reason());
            return null;
        }
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
                    popup(owner,logger);
                }
                logger.log(Stack_trace_getter.get_stack_trace("Feature_vector_source::get_feature_vector_from_server_generic SERVER NOT STARTED.."));
                //return null;
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
            logger.log(Stack_trace_getter.get_stack_trace("get_feature_vector_from_server_generic (1): "+e));
            return null;
        }
        URL url = null;
        try {
            url = new URL(url_string);
        } catch (MalformedURLException e) {
            logger.log(Stack_trace_getter.get_stack_trace("get_feature_vector_from_server_generic (2): "+e));
            return null;
        }
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace("get_feature_vector_from_server_generic (3)"+e));
            return null;
        }
        try {
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(0); // infinite
        } catch (ProtocolException e) {
            logger.log(Stack_trace_getter.get_stack_trace("get_feature_vector_from_server_generic (4): "+e));
            return null;
        }
        if (aborter.should_abort())
        {
            logger.log("aborting(2) Feature_vector_source::get_feature_vector_from_server_generic reason: "+aborter.reason());
            return null;
        }
        try {
            connection.connect();
        } catch (IOException e) {
            //logger.log(Stack_trace_getter.get_stack_trace(""+e));
            logger.log(("get_feature_vector_from_server_generic (5): "+e));
            server_started.set(false);
            return null;
        }
        try {
            int response_code = connection.getResponseCode();
            //logger.log("response code="+response_code);
        } catch (IOException e) {
            //logger.log(Stack_trace_getter.get_stack_trace(""+e));
            logger.log(("get_feature_vector_from_server_generic (6):"+e));
            return null;
        }
        try {
            String response_message = connection.getResponseMessage();
            //logger.log("response message="+response_message);
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace("get_feature_vector_from_server_generic (7): "+e));
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
                System.out.print((char)c);
                sb.append((char)c);
            }
        } catch (IOException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace("get_feature_vector_from_server_generic (8): "+e));
            return null;
        }

        // Use a JSON parser library (e.g., Jackson) to parse the JSON string
        String json = sb.toString();
        //logger.log("json ="+json);
        Feature_vector fv = Feature_vector_source_server.parse_json(json,logger);
        if ( fv == null) {
            logger.log("json parsing failed: feature vector is null");
        }
        else {
            //logger.log("GOT a feature vector of size:"+fv.features.length);
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
        list.add("Python");
        //list.add("Python.*run_server.*");
        StringBuilder sb = new StringBuilder();
        File wd = new File (".");
        if (Execute_command.execute_command_list(list, wd, 2000, sb, logger)==null)
        {
            logger.log("WARNING, checking if servers are running => failed(1):\n"+ sb );
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
        logger.log("WARNING, checking if servers are running => failed(2):\n"+ sb );
        return false;
    }
    //**********************************************************
    private static void popup(Window owner, Logger logger)
    //**********************************************************
    {
        Jfx_batch_injector.inject(() -> Popups.popup_warning(
                "Servers not started.",
                "This feature requires to start the Image Feature Extraction Servers\n" +
                        "Look in the menu 'preferences' for instructions",
                false,owner,logger), logger);

    }
}
