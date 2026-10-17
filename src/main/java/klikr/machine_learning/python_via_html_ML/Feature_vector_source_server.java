// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.machine_learning.python_via_html_ML;

import klikr.machine_learning.feature_vector.Feature_vector_double;
import klikr.machine_learning.feature_vector.Feature_vector_source;
import klikr.util.Kontext;
import klikr.util.Shared_services;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.log.Logger;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.LongAdder;

//**********************************************************
public abstract class Feature_vector_source_server implements Feature_vector_source
//**********************************************************
{
    private static final boolean dbg = false;
    private static final boolean ultra_dbg = false;

    // get_random_port is actually going to start servers, when needed
    protected abstract int get_random_port(Kontext context);

    public static long start = System.nanoTime();
    static LongAdder tx_count = new LongAdder();
    static LongAdder SUM_dur_us = new LongAdder(); // microseconds

    static final boolean monitoring_on = false;

    //**********************************************************
    public Feature_vector_source_server(Kontext context)
    //**********************************************************
    {
        if ( monitoring_on)
        {
            start_monitoring();
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
    public Optional<Feature_vector_double> get_feature_vector_from_server(Path path, Kontext context)
    //**********************************************************
    {
        if(context.aborter() != null) {
            if (context.should_abort()) {
                context.log("aborting Feature_vector_source::get_feature_vector_from_server, reason: " + context.aborter().reason());
                return Optional.empty();
            }
        }

        long local_start = System.nanoTime();
        if ( path == null)
        {
            context.log_with_stack_trace("BAD!");
            return Optional.empty();
        }
        int random_port = get_random_port(context);

        if ( dbg) context.log("random_port="+random_port);
        if (random_port == -1) {
            context.log("No valid port from registry");
            return Optional.empty();
        }

        Optional<Feature_vector_double> op = Feature_vector_source_server.get_feature_vector_from_server_generic(path, random_port,context);

        if ( op.isEmpty())
        {
            if ( path.toFile().exists())
            {
                // The server failed to return a vector. the file may be corrupted?
                context.log(Logger.warning+" WARNING: File MAY BE CORRUPTED: "+path);
            }
            else
            {
                context.log("get_feature_vector_from_server_generic: FAILED because file does not exist: "+path);
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
    static Feature_vector_double parse_json(String response, Kontext context)
    //**********************************************************
    {
        //context.log("going to parse a JSON feature vector ->" + response + "<-");

        // expecting {"features":[0.1,0.2,0.3,...]}
        response = response.trim();
        if ( !response.startsWith("{") || !response.endsWith("}"))
        {
            context.log("json parsing failed: does not start with { or end with }");
            return null;
        }
        int features_index = response.indexOf("\"features\"");
        if ( features_index == -1)
        {
            context.log("json parsing failed: no \"features\" key found");
            return null;
        }
        int colon_index = response.indexOf(":", features_index);
        if ( colon_index == -1)
        {
            context.log("json parsing failed: no : after \"features\" key");
            return null;
        }
        int open_bracket_index = response.indexOf("[", colon_index);
        if ( open_bracket_index == -1)
        {
            context.log("json parsing failed: no [ after \"features\":");
            return null;
        }
        int close_bracket_index = response.indexOf("]", open_bracket_index);
        if ( close_bracket_index == -1)
        {
            context.log("json parsing failed: no ] after \"features\":[");
            return null;
        }
        String array_string = response.substring(open_bracket_index + 1, close_bracket_index).trim();
        if ( array_string.isEmpty())
        {
            context.log("json parsing failed: empty features array");
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
                context.log_with_stack_trace_from_throwable("parse_json: NumberFormatException for part="+parts[i]+" ",e);
                return null;
            }
        }
        Feature_vector_double fv = new Feature_vector_double(features,"from_json");
        if ( dbg) context.log("parsed a feature vector, length: " + fv.features.length);
        return fv;

    }

    //**********************************************************
    static Optional<Feature_vector_double> get_feature_vector_from_server_generic(Path path, int random_port, Kontext context)
    //**********************************************************
    {
        if ( context.aborter() != null) {
            if (context.should_abort()) {
                context.log("aborting(1) Feature_vector_source::get_feature_vector_from_server_generic reason: " + context.aborter().reason());
                return Optional.empty();
            }
        }

        if ( path == null)
        {
            context.log_with_stack_trace("BAD!");
            return Optional.empty();
        }

        String url_string = null;
        try {
            String encodedPath = URLEncoder.encode(path.toAbsolutePath().toString(), "UTF-8");
            url_string = "http://127.0.0.1:" + random_port + "/" + encodedPath;
        }
        catch( OutOfMemoryError oome)
        {
            context.log_with_stack_trace("OOM");
            return Optional.empty();
        }
        catch (UnsupportedEncodingException e) {
            context.log_with_stack_trace_from_throwable("get_feature_vector_from_server_generic (Error#1): ",e);
            return Optional.empty();
        }
        URL url = null;
        try {
            url = new URL(url_string);
        }
        catch( OutOfMemoryError oome)
        {
            context.log_with_stack_trace("OOM");
            return Optional.empty();
        }
        catch (MalformedURLException e) {
            context.log_with_stack_trace_from_throwable(url_string+": get_feature_vector_from_server_generic (Error#2): ",e);
            return Optional.empty();
        }
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
        }
        catch( OutOfMemoryError oome)
        {
            context.log_with_stack_trace("OOM");
            return Optional.empty();
        }
        catch (IOException e) {
            context.log_with_stack_trace_from_throwable(url_string+": get_feature_vector_from_server_generic (Error#3)",e);
            return Optional.empty();
        }
        try {
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(0); // infinite
        }
        catch( OutOfMemoryError oome)
        {
            context.log_with_stack_trace_from_throwable("OOM",oome);
            return Optional.empty();
        }
        catch (ProtocolException e) {
            context.log_with_stack_trace_from_throwable(url_string+": get_feature_vector_from_server_generic (Error#4): ",e);
            return Optional.empty();
        }

        if (context.should_abort()) {
            context.log("aborting(2) Feature_vector_source::get_feature_vector_from_server_generic reason: " + context.aborter().reason());
            return Optional.empty();
        }


        try {
            connection.connect();
        }
        catch( OutOfMemoryError oome)
        {
            context.log_with_stack_trace("OOM");
            return Optional.empty();
        }
        catch (IOException e) {
            //context.log_with_stack_trace(""+e));
            context.log_with_stack_trace_from_throwable(url_string+": get_feature_vector_from_server_generic (Error#5): ",e);
            return Optional.empty();
        }

        if (context.should_abort()) {
            context.log("aborting(3) Feature_vector_source::get_feature_vector_from_server_generic reason: " + context.abort_reason());
            return Optional.empty();
        }

        try {
            int response_code = connection.getResponseCode();
            //context.log("response code="+response_code);
        }
        catch( OutOfMemoryError oome)
        {
            context.log_with_stack_trace_from_throwable("OOM",oome);
            return Optional.empty();
        }
        catch (IOException e) {
            //context.log_with_stack_trace(""+e));
            context.log_with_stack_trace_from_throwable(url_string+": get_feature_vector_from_server_generic cannot get response code (Error#6):",e);
            return Optional.empty();
        }

        if (context.should_abort()) {
            context.log("aborting(4) Feature_vector_source::get_feature_vector_from_server_generic reason: " + context.abort_reason());
            return Optional.empty();
        }

        try {
            String response_message = connection.getResponseMessage();
            //context.log("response message="+response_message);
        }
        catch( OutOfMemoryError oome)
        {
            context.log_with_stack_trace("OOM");
            return Optional.empty();
        }
        catch (IOException e) {
            context.log_with_stack_trace_from_throwable(url_string+": get_feature_vector_from_server_generic (Error#7): ",e);
            return Optional.empty();
        }


        if (context.should_abort()) {
            context.log("aborting(5) Feature_vector_source::get_feature_vector_from_server_generic reason: " + context.abort_reason());
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
        }
        catch( OutOfMemoryError oome)
        {
            context.log_with_stack_trace("OOM");
            return Optional.empty();
        }
        catch (IOException e)
        {
            context.log_with_stack_trace_from_throwable("get_feature_vector_from_server_generic (8): ",e);
            return Optional.empty();
        }
        finally
        {
            connection.disconnect();
        }

        if (context.should_abort()) {
            context.log("aborting(6) Feature_vector_source::get_feature_vector_from_server_generic reason: " + context.abort_reason());
            return Optional.empty();
        }

        String json = sb.toString();
        //context.log("json ="+json);
        Feature_vector_double fv = Feature_vector_source_server.parse_json(json,context);
        if ( fv == null) {
            context.log("json parsing failed: feature vector is null");
            return Optional.empty();
        }
        else {
            //context.log("GOT a feature vector of length:"+fv.features.length);
        }
        return Optional.of(fv);
    }


}
