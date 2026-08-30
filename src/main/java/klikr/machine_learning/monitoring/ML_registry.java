// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.machine_learning.monitoring;

import javafx.stage.Window;
import klikr.machine_learning.*;
import klikr.settings.boolean_features.Feature;
import klikr.settings.boolean_features.Feature_cache;
import klikr.util.Simple_json_parser;
import klikr.util.execute.Execute_command;
import klikr.util.execute.Execute_result;
import klikr.util.execute.Guess_OS;
import klikr.util.execute.Operating_system;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;

//**********************************************************
public class ML_registry
//**********************************************************
{
    private final static boolean dbg = false;
    private final static boolean ultra_dbg = false;

    private static volatile ML_registry instance;
    private final Map<String,List<ML_server>> servers_from_file = new ConcurrentHashMap<>();

    //**********************************************************
    private static ML_registry get_instance()
    //**********************************************************
    {
        if (instance == null)
        {
            synchronized (ML_registry.class)
            {
                if (instance == null)
                {
                    instance = new ML_registry();
                }
            }
        }
        return  instance;
    }
    //**********************************************************
    public static Map<String,List<ML_server>> scan_all_registry(Window owner, Logger logger)
    //**********************************************************
    {
        if (instance == null) instance = get_instance();
        return instance.scan_all_registry_internal(owner, logger);
    }
    //**********************************************************
    private String all_servers_from_file_to_string()
    //**********************************************************
    {

        StringBuilder sb = new StringBuilder();
        for ( Map.Entry<String, List<ML_server>> entry : instance.servers_from_file.entrySet() )
        {
            sb.append("server (key) :").append(entry.getKey()).append("\n");
            for ( ML_server ml_server : entry.getValue() )
            {
                sb.append(ml_server.to_string()).append("\n");
            }
        }
        return sb.toString();
    }

    //**********************************************************
    public static void all_servers_killed(ML_server_type st)
    //**********************************************************
    {
        if (instance == null) instance = get_instance();
        instance.servers_from_file.remove(st.name());
    }

    //**********************************************************
    public static List<ML_server> get_servers_of_type(ML_server_type server_type, Window owner, Logger logger)
    //**********************************************************
    {
        if (instance == null) instance = get_instance();

        // try to find some server that would be already running
        // by reading the file registry (this is fast)
        return instance.scan_file_registry_for(server_type, owner, logger);
    }

/*
    //**********************************************************
    private List<ML_server> get_from_registry_file_count_of_servers_of_type(ML_server_type server_type, Window owner, Logger logger)
    //**********************************************************
    {
        if ( servers_from_file.get(server_type.name()) == null) return new ArrayList<>();
        else return servers_from_file.get(server_type.name());
    }
*/



    //**********************************************************
    private Map<String,List<ML_server>> scan_all_registry_internal(Window owner, Logger logger)
    //**********************************************************
    {
        for (ML_server_type st : ML_server_type.values())
        {
            scan_file_registry_for(st,owner,logger);

        }
        return servers_from_file;
    }

    // returns 'new servers' count
    //**********************************************************
    private List<ML_server> scan_file_registry_for(ML_server_type target_server_type, Window owner, Logger logger)
    //**********************************************************
    {
        List<ML_server> list = servers_from_file.computeIfAbsent(target_server_type.name(), k -> new ArrayList<>());

        boolean live_dbg = Feature_cache.get(Feature.Enable_ML_server_debug);
        if ( dbg) logger.log("for " + target_server_type.name() + " SCANNING registry: "+target_server_type.registry_path(owner, logger));
        try
        {
            File[] files = (target_server_type.registry_path(owner, logger).toFile()).listFiles();
            if ( files == null)
            {
                if (dbg) logger.log(" registry directory is empty");
                return list;
            }
            if ( files.length == 0)
            {
                if (dbg) logger.log(" registry directory is empty");
                return list;
            }
            for ( File f : files )
            {
                if ( dbg) logger.log("considering registry FILE: " + f.getAbsolutePath());
                // the file content is like this:
                // {"type": "MobileNet",
                // "port": 54225,
                // "uuid": "072ff019-5884-44f9-8b40-fe4ea967d4f8"}
                String content = Files.readString(f.toPath());

                String type = Simple_json_parser.read_key(content,"type",logger);
                if ( dbg) logger.log("looking for " + target_server_type.name() + " found instance type: " + type);
                if ( type == null)
                {
                    // assume NOT a registry file
                    // do not delete !!!
                    continue;
                }

                if ( !target_server_type.name().equals(type))
                {
                    // not what we are looking for
                    if ( dbg) logger.log("->"+target_server_type.name()+ "<- does not match ->" + type+ "<-");
                    continue;
                }
                if ( dbg) logger.log("OK ->"+target_server_type.name()+ "<- matches ->" + type+ "<-");

                String port_s = Simple_json_parser.read_key(content,"port",logger);
                if ( port_s == null)
                {
                    // assume invalid registry file
                    if ( dbg) logger.log(" port not found in json: deleting invalid registry file " + f.getAbsolutePath());
                    Files.delete(f.toPath());
                    continue;
                }
                if ( dbg) logger.log("for " + target_server_type.name() + " found PORT: " + port_s);
                int port = Integer.parseInt(port_s);

                String uuid = Simple_json_parser.read_key(content,"uuid",logger);
                if ( uuid == null)
                {
                    // assume invalid registry file
                    if ( dbg) logger.log(" uuid not found in json: deleting invalid registry file " + f.getAbsolutePath());
                    Files.delete(f.toPath());
                    continue;
                }
                if ( dbg) logger.log("for " + target_server_type.name() + " found uuid: " + uuid);

                // is this server alive?
                ML_server ml_server = new ML_server(port,uuid,type);

                if ( is_server_alive_light_version(port, logger))
                {
                    if ( !list.contains(ml_server) )
                    {
                        list.add(ml_server);
                        if (dbg) logger.log(Logger.ok+" " + target_server_type.name() + " detected a live server at port " + port);
                    }
                    ML_servers_monitor.refresh_add(ml_server, owner, logger);
                }
                else
                {
                    logger.log(Logger.error+"" + target_server_type.name() + " server at port " + port + " is not responding to health check.");
                    remove(list, f, ml_server,live_dbg,  owner, logger);
                }
            }
        } catch (IOException e) {
            logger.log("Error reading registry directory: " + e);
        }

        return list;
    }

    //**********************************************************
    private void remove(List<ML_server> list, File f, ML_server ml_server, boolean live_dbg, Window owner, Logger logger)
    //**********************************************************
    {
        if ( list != null) list.remove(ml_server);
        ML_servers_monitor.refresh_remove(ml_server, owner, logger);
        try
        {
            Files.delete(f.toPath());
            if (live_dbg) logger.log("Deleted stale registry file: " + f.getAbsolutePath());
        }
        catch (IOException e)
        {
            logger.log("Failed to delete stale registry file: " + f.getAbsolutePath() + " Error: " + e);
        }
    }


    //**********************************************************
    private static boolean is_server_alive_light_version(int port, Logger logger)
    //**********************************************************
    {
        if (port == -1)
        {
            logger.log(Stack_trace_getter.get_stack_trace("Invalid port -1"));
            return false;
        }
        HttpURLConnection connection = null;
        try {
            URI uri = URI.create("http://127.0.0.1:" + port + "/health");
            connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.connect();
            int responseCode = connection.getResponseCode();
            if (responseCode == 200)
            {
                String content = connection.getContent().toString();
                if ( dbg) logger.log("Health check response code: " + responseCode + ", content: " + content);
                try (InputStream is = connection.getInputStream();
                     BufferedReader br = new BufferedReader(new InputStreamReader(is,
                             StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                    String content2 = sb.toString();   // now you have the JSON
                    if (dbg) logger.log("Health check response code: " + responseCode + ", content: "
                            + content2);
                }
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
        finally {
            if (connection != null) connection.disconnect();
        }
    }

    static Semaphore limit = new Semaphore(1);



    //**********************************************************
    public int check_processes(String server_python_name, Logger logger)
    //**********************************************************
    {
        try {
            limit.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        List<String> list = new ArrayList<>();
        if (Guess_OS.guess(logger)== Operating_system.Windows)
        {
            list.add("powershell.exe");
            list.add("-Command");
            // Extract just the Id
            list.add("Get-Process | Where-Object {$_.ProcessName -like '*"+server_python_name+"*'} | Select-Object -ExpandProperty Id");
        }
        else
        {
            // ps aux | grep MobileNet_embeddings_server
            list.add("sh");
            list.add("-c");
            // Use -f to match pattern in full command line, but do NOT use -a, so output is just PIDs
            list.add("pgrep -f "+server_python_name);
        }
        StringBuilder sb = new StringBuilder();
        File wd = new File (".");
        Execute_result er = Execute_command.execute_command_list(list, wd, 2000, sb, logger);
        if (!er.status())
        {
            // logger.log("WARNING, checking if servers are running => failed(1)" );
            limit.release();
            return 0;
        }
        String result = er.output().trim();
        if ( result.isEmpty()) {
            limit.release();
            return 0;
        }
        logger.log("checking if servers are running check():->" + result+"<-");
        String[] parts = result.split("\\r?\\n"); // Split on new lines
        int count = 0;
        for ( String p : parts)
        {
            try {
                int pid = Integer.parseInt(p);
                logger.log("found matching pid:" + pid+ " for servers named: "+server_python_name);
                count++;
            }
            catch (NumberFormatException e)
            {
                logger.log(Logger.error+"WARNING, checking if servers named like "+server_python_name+" are running => failed, non integer found in pgrep reply:"+p );
                limit.release();
                return 0;
            }
        }
        logger.log(Logger.ok+"  OK, found "+count+" PIDs for servers named like "+server_python_name);
        limit.release();
        return count;
    }
}

