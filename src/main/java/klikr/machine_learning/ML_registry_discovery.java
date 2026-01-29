// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.machine_learning;

import javafx.stage.Window;
import klikr.machine_learning.face_recognition.Face_detection_type;
import klikr.properties.boolean_features.Feature;
import klikr.properties.boolean_features.Feature_cache;
import klikr.util.Simple_json_parser;
import klikr.util.cache.Cache_folder;
import klikr.util.execute.Execute_command;
import klikr.util.execute.Execute_result;
import klikr.util.execute.Guess_OS;
import klikr.util.execute.Operating_system;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.files_and_paths.Static_files_and_paths_utilities;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;
import klikr.util.mmap.Mmap;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

//**********************************************************
public class ML_registry_discovery
//**********************************************************
{
    private final static boolean dbg = false;
    private final Random random = new Random();
    private static volatile ML_registry_discovery instance;

    private final AtomicBoolean server_pump_started = new AtomicBoolean(false);
    private final BlockingQueue<ML_service_type> request_queue = new ArrayBlockingQueue<>(1);
    private final Map<String,List<ML_server>> servers_from_file = new ConcurrentHashMap<>();
    private Aborter pump_aborter;
    private final Map<String,Integer> running = new ConcurrentHashMap<>();


    //**********************************************************
    private static String all_servers_from_file_to_string()
    //**********************************************************
    {
        if (instance == null) return null;
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
    public static void all_servers_killed(ML_service_type st)
    //**********************************************************
    {
        if (instance == null) return;
        instance.servers_from_file.remove(get_key(st));
    }

    //**********************************************************
    private static String get_key(ML_service_type t)
    //**********************************************************
    {
        if ( t.face_detection_type() == null) return t.ml_server_type().name();
        return t.ml_server_type().name() + "_with_" + t.face_detection_type().name();
    }
    //**********************************************************
    private List<ML_server> get_from_registry_file_count_of_servers_of_type(ML_service_type st)
    //**********************************************************
    {
        return servers_from_file.computeIfAbsent(get_key(st), k -> new ArrayList<>());
    }

    //**********************************************************
    public static ML_servers_status find_active_servers(ML_service_type st, Window owner, Logger logger)
    //**********************************************************
    {
        if (instance == null)
        {
            synchronized (ML_registry_discovery.class)
            {
                if (instance == null)
                {
                    instance = new ML_registry_discovery();
                }
            }
        }
        return instance.find_active_servers_internal(st, owner, logger);
    }

    // the returned status is for the request type
    //**********************************************************
    private ML_servers_status find_active_servers_internal(ML_service_type st, Window owner, Logger logger)
    //**********************************************************
    {
        List<ML_server> servers_of_type = get_from_registry_file_count_of_servers_of_type(st);
        ML_server_launch_status status = ML_server_launch_status.RUNNING;
        if (servers_of_type.size() <st.ml_server_type().quota(owner))
        {
            // send a request
            status = ML_server_launch_status.STARTING;
            request_queue.offer(st);
            if (!server_pump_started.get()) start_server_creation_job(owner, logger);
        }
        List<Integer> active_servers = new ArrayList<>();
        for ( ML_server ml_server : servers_of_type ) active_servers.add(ml_server.port());
        return new ML_servers_status(active_servers, status);
    }

    //**********************************************************
    private void start_server_creation_job(Window owner, Logger logger)
    //**********************************************************
    {
        boolean live_dbg = Feature_cache.get(Feature.Enable_ML_server_debug);
        pump_aborter = new Aborter("server_creation_job_aborter",logger);
        server_pump_started.set(true);
        Runnable r = () -> {
            for(;;) {
                try {
                    ML_service_type st = request_queue.poll(60, TimeUnit.SECONDS);
                    if (st == null) {
                        // timeout
                        if (pump_aborter.should_abort()) {
                            server_pump_started.set(false);
                            return;
                        }
                        continue;
                    }
                    // try to find some server that would be already running
                    // by reading the file registry (this is fast)
                    scan_file_registry(st, owner, logger);
                    if ( dbg) logger.log("After scanning the file registry:\n:"+ all_servers_from_file_to_string());

                    List<ML_server> servers_of_type = get_from_registry_file_count_of_servers_of_type(st);
                    if (servers_of_type.size() >= st.ml_server_type().quota(owner))
                    {
                        //logger.log("No need to start servers of type: "+st.ml_server_type().name()+ " running: "+servers_of_type.length()+ " quota: "+st.ml_server_type().quota(owner));
                        continue;
                    }
                    // need to launch some servers
                    Integer running_i = running.get(get_key(st));
                    if ( running_i == null)
                    {
                        running.put(get_key(st),0);
                        running_i = 0;
                    }
                    int full_quota = st.ml_server_type().quota(owner);
                    int more = full_quota - running_i;
                    if ( more > 0) {
                        // launched ones are considered already running to avoid a fork bomb
                        running.put(get_key(st), full_quota);
                        logger.log("✅ Going to spawn " + more + " new servers of type: " + st.ml_server_type().name());
                        ML_servers_monitor.make_faster_network_scans();
                        start_some_server(st.ml_server_type(), more, owner, logger);
                    }
                    else
                    {
                        logger.log("✅ Not going to spawn new servers of type: " + st.ml_server_type().name()+ " because: quota=" + full_quota + " running=" + running_i);
                    }
                } catch (InterruptedException e) {
                    logger.log("Interrupted while waiting for servers to start");
                }
            }
        };
        Actor_engine.execute(r,"ML_server_launch",logger);
    }



    // the number of servers to be started is hard-coded, except for image embeddings
    //**********************************************************
    private int start_some_server(ML_server_type type, int target_count, Window owner, Logger logger)
    //**********************************************************
    {
        switch( type)
        {
            case MobileNet:
                return ML_servers_util.start_some_image_similarity_servers(target_count,owner, logger);
            case FaceNet:
                return ML_servers_util.start_face_embeddings_servers(owner, logger);
            case Haars:
                return ML_servers_util.start_haars_face_detection_servers(owner, logger);
            case MTCNN:
                return ML_servers_util.start_MTCNN_face_detection_servers(owner, logger);
        }
        return 0;
    }

    //**********************************************************
    public static Map<String,List<ML_server>> scan_all_registry(Window owner, Logger logger)
    //**********************************************************
    {
        if ( instance == null ) return null;
        return instance.scan_all_registry_internal(owner, logger);
    }
    //**********************************************************
    private Map<String,List<ML_server>> scan_all_registry_internal(Window owner, Logger logger)
    //**********************************************************
    {
        for (ML_server_type st : ML_server_type.values())
        {
            for (Face_detection_type std : Face_detection_type.values())
            {
                ML_service_type mst = new ML_service_type(st,std);
                scan_file_registry(mst,owner,logger);
            }
        }
        return servers_from_file;
    }

    // returns 'new servers' count
    //**********************************************************
    private int scan_file_registry(ML_service_type st, Window owner, Logger logger)
    //**********************************************************
    {
        boolean live_dbg = Feature_cache.get(Feature.Enable_ML_server_debug);
        if ( dbg) logger.log("for " + st.ml_server_type().name() + " SCANNING registry: "+st.ml_server_type().registry_path(owner, logger));
        int returned = 0;
        try
        {
            File[] files = (st.ml_server_type().registry_path(owner, logger).toFile()).listFiles();
            if ( files == null)
            {
                //logger.log(" registry directory is empty");
                return 0;
            }
            if ( files.length == 0)
            {
                //logger.log(" registry directory is empty");
                return 0;
            }
            for ( File f : files )
            {
                if ( dbg) logger.log("considering registry FILE: " + f.getAbsolutePath());
                // the file content is like this:
                // {"name": "MobileNet_embeddings_server",
                // "port": 54225,
                // "uuid": "072ff019-5884-44f9-8b40-fe4ea967d4f8"}
                String content = Files.readString(f.toPath());

                String name = Simple_json_parser.read_key(content,"name",logger);
                if ( dbg) logger.log("looking for " + st.ml_server_type().name() + " found instance name: " + name);
                if ( !st.ml_server_type().name().contains(name))
                {
                    // not what we are looking for
                    if ( dbg) logger.log("->"+st.ml_server_type().name()+ "<- does not contain ->" + name+ "<-");
                    continue;
                }

                String sub_type = Simple_json_parser.read_key(content, "sub-type",logger);
                if ( dbg) logger.log("sub-type=" + sub_type);
                if ( sub_type != null)
                {
                    if ( dbg) logger.log(" found sub-type: " + sub_type);
                }

                String port_s = Simple_json_parser.read_key(content,"port",logger);
                if ( dbg) logger.log("for " + st.ml_server_type().name() + " found PORT: " + port_s);
                int port = Integer.parseInt(port_s);

                String uuid = Simple_json_parser.read_key(content,"uuid",logger);
                if ( dbg) logger.log("for " + st.ml_server_type().name() + " found uuid: " + uuid);

                List<ML_server> list = servers_from_file.computeIfAbsent(get_key(st), k -> new ArrayList<>());
                // is this server alive?
                ML_server ml_server = new ML_server(port,uuid,name,sub_type);
                if ( is_server_alive(port, logger))
                {
                    if ( !list.contains(ml_server) ) {
                        list.add(ml_server);
                        returned++;

                        ML_servers_monitor.refresh_add(ml_server, owner, logger);
                        if (dbg) logger.log("✅ " + st.ml_server_type().name() + " detected a live server at port " + port);
                    }
                }
                else
                {
                    logger.log("❌ " + st.ml_server_type().name() + " server at port " + port + " is not responding to health check.");
                    if ( list.remove(ml_server))
                    {
                        ML_servers_monitor.refresh_remove(ml_server, owner, logger);
                    }
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
            }
        } catch (IOException e) {
            logger.log("Error reading registry directory: " + e);
        }
        return returned;

    }

    //**********************************************************
    public static int get_random_active_port(ML_service_type st, Window owner, Logger logger)
    //**********************************************************
    {
        if ( instance == null) instance = new ML_registry_discovery();
        return instance.get_random_active_port_internal(st, owner, logger);
    }
    //**********************************************************
    private int get_random_active_port_internal(ML_service_type st, Window owner, Logger logger)
    //**********************************************************
    {
        boolean live_dbg = Feature_cache.get(Feature.Enable_ML_server_debug);
        ML_servers_status status = find_active_servers(st, owner, logger);
        if (status.available_ports().isEmpty())
        {
            if ( status.launch_status() == ML_server_launch_status.STARTING )
            {
                logger.log("Servers are starting (no servers are active yet) for: " + st.ml_server_type().name());
                return -1;
            }
            logger.log(Stack_trace_getter.get_stack_trace("❌ FATAL for: " + st.ml_server_type().name()));
            return -1;
        }
        //logger.log("✅ Found "+status.available_ports().length()+" active servers for: " + st.ml_server_type().name());

        // Return a random port for load balancing
        return status.available_ports().get(random.nextInt(status.available_ports().size()));
    }

    
    //**********************************************************
    private static boolean is_server_alive(int port, Logger logger)
    //**********************************************************
    {
        if (port == -1)
        {
            logger.log(Stack_trace_getter.get_stack_trace("Invalid port -1"));
            return false;
        }
        try {
            URI uri = URI.create("http://127.0.0.1:" + port + "/health");
            HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(2000);
            connection.setReadTimeout(2000);
            connection.connect();
            int responseCode = connection.getResponseCode();
            if (responseCode == 200)
            {
                String content = connection.getContent().toString();
                if ( dbg) logger.log("Health check response code: " + responseCode + ", content: " + content);
                connection.disconnect();
                return true;
            }
            connection.disconnect();
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    static Semaphore limit = new Semaphore(1);



    //**********************************************************
    public int check_processes(String server_python_name, Window owner, Logger logger)
    //**********************************************************
    {
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
                logger.log("❌ WARNING, checking if servers named like "+server_python_name+" are running => failed, non integer found in pgrep reply:"+p );
                limit.release();
                return 0;
            }
        }
        logger.log("✅  OK, found "+count+" PIDs for servers named like "+server_python_name);
        limit.release();
        return count;
    }
}

