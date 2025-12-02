// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.machine_learning.feature_vector;

import javafx.application.Platform;
import javafx.stage.Window;
import klik.Shared_services;
import klik.util.execute.Guess_OS;
import klik.util.execute.Operating_system;
import klik.util.execute.actor.Aborter;
import klik.util.execute.actor.Actor_engine;
import klik.util.execute.Execute_command;
import klik.util.log.Logger;
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
import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

//**********************************************************
public abstract class Feature_vector_source_server implements Feature_vector_source
//**********************************************************
{
    private static final boolean dbg = false;
    private static final boolean ultra_dbg = false;

    protected abstract int get_random_port();
    protected abstract String get_server_python_name();
    protected abstract boolean get_server_started();
    protected abstract boolean start_servers(Window owner, Logger logger);
    protected abstract void set_server_started(boolean b);
    public static long start = System.nanoTime();
    static LongAdder tx_count = new LongAdder();
    static LongAdder SUM_dur_us = new LongAdder(); // microseconds

    static final boolean monitoring_on = false;

    //**********************************************************
    public Feature_vector_source_server(Window owner,  Logger logger)
    //**********************************************************
    {
        if ( monitoring_on)
        {
            start_monitoring();
        }
        // start servers if needed
        if (get_server_started()) return ;

        if ( check(owner,logger))
        {
            set_server_started(true);
        }
        else
        {
            if ( start_servers(owner,logger))
            {
                set_server_started(true);
            }
            else
            {
                logger.log(Stack_trace_getter.get_stack_trace("Feature_vector_source::get_feature_vector_from_server_generic SERVER NOT STARTED.."));
            }
        }
    }
    //**********************************************************
    private void start_monitoring()
    //**********************************************************
    {
        Logger l = Shared_services.logger();
        Runnable r = () ->
        {
            for(;;)
            {
                if ( Shared_services.aborter().should_abort()) return;
                try {
                    Thread.sleep(60_000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                print_embeddings_stats(l);
            }
        };
        Actor_engine.execute(r, "Monitor embeddings stats",l);
    }

    //**********************************************************
    public Optional<Feature_vector> get_feature_vector_from_server(Path path, Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        if ( aborter.should_abort())
        {
            logger.log("aborting Feature_vector_source::get_feature_vector_from_server, reason: "+aborter.reason());
            return Optional.empty();
        }
        if ( !get_server_started())
        {
            return Optional.empty();
        }


        long local_start = System.nanoTime();
        if ( path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BAD!"));
            return Optional.empty();
        }
        int random_port = get_random_port();
        Optional<Feature_vector> op = Feature_vector_source_server.get_feature_vector_from_server_generic(path, random_port, owner, aborter,logger);

        if ( op.isEmpty())
        {
            if ( path.toFile().exists())
            {
                logger.log("get_feature_vector_from_server_generic: FAILED but file exists, "+path.toFile().length()+" but feature vector is null");
            }
            else
            {
                logger.log("get_feature_vector_from_server_generic: FAILED because file does not exist: "+path);
            }
        }
        long local_end = System.nanoTime();
        long local_dur_us = (local_end - local_start)/1000;
        SUM_dur_us.add(local_dur_us);
        tx_count.add(1);
        return op;
    }

    //**********************************************************
    public static void print_embeddings_stats(Logger logger)
    //**********************************************************
    {
        long end = System.nanoTime();
        double dur_us = (double)(end - start)/1_000.0;
        logger.log("feature vector TX_rate="+1_000_000*tx_count.doubleValue()/(double)dur_us+" tx/s (tx_count="+tx_count+" for: "+dur_us/1_000_000+" seconds)");
        logger.log("total server call time="+ SUM_dur_us.doubleValue()/1_000_000 +"s, average concurrency="+SUM_dur_us.doubleValue() /dur_us);
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
        if ( dbg) logger.log("parsed a feature vector, length: " + fv.features.length);
        return fv;

    }

    //**********************************************************
    static Optional<Feature_vector> get_feature_vector_from_server_generic(Path path, int random_port, Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        if ( aborter.should_abort())
        {
            logger.log("aborting(1) Feature_vector_source::get_feature_vector_from_server_generic reason: "+aborter.reason());
            return Optional.empty();
        }


        if ( path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BAD!"));
            return Optional.empty();
        }

        String url_string = null;
        try {
            String encodedPath = URLEncoder.encode(path.toAbsolutePath().toString(), "UTF-8");
            url_string = "http://localhost:" + random_port + "/" + encodedPath;
        } catch (UnsupportedEncodingException e) {
            logger.log(Stack_trace_getter.get_stack_trace("get_feature_vector_from_server_generic (1): "+e));
            return Optional.empty();
        }
        URL url = null;
        try {
            url = new URL(url_string);
        } catch (MalformedURLException e) {
            logger.log(Stack_trace_getter.get_stack_trace("get_feature_vector_from_server_generic (2): "+e));
            return Optional.empty();
        }
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace("get_feature_vector_from_server_generic (3)"+e));
            return Optional.empty();
        }
        try {
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(0); // infinite
        } catch (ProtocolException e) {
            logger.log(Stack_trace_getter.get_stack_trace("get_feature_vector_from_server_generic (4): "+e));
            return Optional.empty();
        }
        if (aborter.should_abort())
        {
            logger.log("aborting(2) Feature_vector_source::get_feature_vector_from_server_generic reason: "+aborter.reason());
            return Optional.empty();
        }
        try {
            connection.connect();
        } catch (IOException e) {
            //logger.log(Stack_trace_getter.get_stack_trace(""+e));
            logger.log(("get_feature_vector_from_server_generic (5): "+e));
            //server_started.set(false);
            return Optional.empty();
        }
        try {
            int response_code = connection.getResponseCode();
            //logger.log("response code="+response_code);
        } catch (IOException e) {
            //logger.log(Stack_trace_getter.get_stack_trace(""+e));
            logger.log(("get_feature_vector_from_server_generic (6):"+e));
            return Optional.empty();
        }
        try {
            String response_message = connection.getResponseMessage();
            //logger.log("response message="+response_message);
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace("get_feature_vector_from_server_generic (7): "+e));
            return Optional.empty();
        }

        // Read the JSON response one character at a time
        StringBuffer sb = new StringBuffer();
        try(BufferedInputStream bufferedInputStream = new BufferedInputStream(connection.getInputStream()))
        {
            if ( ultra_dbg) System.out.print("DBG feature vector ");
            for(;;)
            {
                int c = bufferedInputStream.read();
                if ( c == -1) break;
                if ( ultra_dbg) System.out.print((char)c);
                sb.append((char)c);
            }
        } catch (IOException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace("get_feature_vector_from_server_generic (8): "+e));
            return Optional.empty();
        }

        String json = sb.toString();
        //logger.log("json ="+json);
        Feature_vector fv = Feature_vector_source_server.parse_json(json,logger);
        if ( fv == null) {
            logger.log("json parsing failed: feature vector is null");
            return Optional.empty();
        }
        else {
            //logger.log("GOT a feature vector of size:"+fv.features.length);
        }

        return Optional.of(fv);
    }
    Semaphore limit = new Semaphore(1);
    //**********************************************************
    public boolean check(Window owner,Logger logger)
    //**********************************************************
    {
        if ( !get_server_started()) return false;
        try {
            limit.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        List<String> list = new ArrayList<>();
        if (Guess_OS.guess(owner,logger)== Operating_system.Windows)
        {
            list.add("powershell.exe");
            list.add("-Command");
            list.add("Get-Process | Where-Object {$_.ProcessName -like '*"+get_server_python_name()+"*'}");
        }
        else
        {
            // ps aux | grep MobileNet_embeddings_server
            list.add("sh");
            list.add("-c");
            list.add("pgrep -af "+get_server_python_name());
        }
        StringBuilder sb = new StringBuilder();
        File wd = new File (".");
        if (! Execute_command.execute_command_list(list, wd, 2000, sb, logger).status())
        {
            logger.log("WARNING, checking if servers are running => failed(1)" );
            set_server_started(false);
            limit.release();
            return false;
        }
        String result = sb.toString();
        logger.log("check():->" + result+"<-");
        String[] parts = result.split("\\r?\\n"); // Split on new lines
        if ( parts.length > 2)
        {
            logger.log(get_server_python_name()+" server name keyword found");
            set_server_started(true);
            limit.release();
            return true;
        }
        logger.log("WARNING, checking if "+get_server_python_name()+" are running => failed(2)" );
        set_server_started(false);
        limit.release();
        return false;
    }

/*
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
        if (! Execute_command.execute_command_list(list, wd, 2000, sb, logger).status())
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

 */
}
