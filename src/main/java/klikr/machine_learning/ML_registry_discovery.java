// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.machine_learning;

import javafx.stage.Window;
import klikr.machine_learning.face_recognition.Face_detection_type;
import klikr.util.execute.Execute_command;
import klikr.util.execute.Execute_result;
import klikr.util.execute.Guess_OS;
import klikr.util.execute.Operating_system;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

//**********************************************************
public class ML_registry_discovery
//**********************************************************
{
    private final static boolean dbg = false;
    private static final Random random = new Random();

    private static final AtomicBoolean server_pump_started = new AtomicBoolean(false);

    private static final BlockingQueue<ML_service_type> request_queue = new ArrayBlockingQueue<>(1);
    private static final Map<String,Set<Integer>> server_ports= new ConcurrentHashMap<>();

    //**********************************************************
    public static void print_all(Logger logger)
    //**********************************************************
    {
        for ( Map.Entry<String, Set<Integer>> entry : server_ports.entrySet() )
        {
            logger.log("server:" + entry.getKey());
            for ( Integer port : entry.getValue() )
            {
                logger.log("   port:" + port);
            }
        }

    }

    //**********************************************************
    public static void invalidate(ML_service_type st)
    //**********************************************************
    {
        server_ports.remove(get_key(st));
    }

    //**********************************************************
    private static String get_key(ML_service_type t)
    //**********************************************************
    {
        if ( t.face_detection_type() == null) return t.ml_server_type().name();
        return t.ml_server_type().name() + "_" + t.face_detection_type().name();
    }
    //**********************************************************
    private static Set<Integer> get_ports(ML_service_type st)
    //**********************************************************
    {
        return server_ports.computeIfAbsent(get_key(st), k -> new HashSet<>());
    }

    //**********************************************************
    public static List<Integer> find_active_servers(ML_service_type st, Window owner, Logger logger)
    //**********************************************************
    {
        Set<Integer> active_ports = get_ports(st);
        int missing = st.ml_server_type().target_server_count(owner) - active_ports.size();
        if (missing > 0) {
            // new items are discarded if the queue already has one
            request_queue.offer(st);
            if  (!server_pump_started.get()) start_server_pump(owner, logger);
        }
        return new ArrayList<>(active_ports);
    }

    private static Aborter pump_aborter;
    private static AtomicInteger in_flight = new AtomicInteger(0);
    //**********************************************************
    private static void start_server_pump(Window owner, Logger logger)
    //**********************************************************
    {
        pump_aborter = new Aborter("server_pump_aborter",logger);
        server_pump_started.set(true);
        Runnable r = () -> {
            for(;;) {
                try {
                    ML_service_type st = request_queue.poll(20, TimeUnit.SECONDS);
                    if (st == null) {
                        // timeout
                        if (pump_aborter.should_abort()) {
                            server_pump_started.set(false);
                            return;
                        }
                        continue;
                    }

                    // try to find some server that would be already running (this is fast)
                    int new_servers_found = scan_registry(st, owner, logger);

                    logger.log("We found " + new_servers_found + " active ports for " + st.ml_server_type().name());

                    Set<Integer> active_ports = get_ports(st);

                    if (active_ports.size() >= st.ml_server_type().target_server_count(owner)) {
                        server_pump_started.set(false);
                        return;
                    }
                    // need to launch some servers
                    int revised_target_count = st.ml_server_type().target_server_count(owner) - active_ports.size();
                    if ( in_flight.get() > revised_target_count)
                    {
                        Thread.sleep(1000);
                        continue;
                    }
                    in_flight.addAndGet(revised_target_count);
                    logger.log("Going to spawn " + revised_target_count + " new servers of type: " + st.ml_server_type().name());
                    start_some_server(st.ml_server_type(), revised_target_count, owner, logger);
                } catch (InterruptedException e) {
                    logger.log("Interrupted while waiting for servers to start");
                }
            }
        };
        Actor_engine.execute(r,"ML_server_pump",logger);
    }


    //**********************************************************
    public static int wait_for_at_least_N_servers_started(ML_service_type st, int target_count, int timeout_ms, Window owner, Logger logger)
    //**********************************************************
    {
        long start = System.currentTimeMillis();
        int poll_interval_ms = 2000;

        logger.log("Waiting for at least " + target_count + " " + st.ml_server_type().name()+ " server(s) to register...");
        Set<Integer> active_ports = get_ports(st);
        while (System.currentTimeMillis() - start < timeout_ms)
        {
            try {
                Thread.sleep(poll_interval_ms);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return active_ports.size();
            }

            int new_servers_found = ML_registry_discovery.scan_registry(st, owner, logger);

            if ( new_servers_found >= target_count) break;

            if (active_ports.size() >= st.ml_server_type().target_server_count(owner)) {
                logger.log("✅ Found " + active_ports.size() + " active " + st.ml_server_type().name() + " server(s) " );
                return active_ports.size();
            }

        }

        logger.log("❌ Timeout waiting for " + st.ml_server_type().name() + " servers");
        return active_ports.size();
    }

    //**********************************************************
    private static void start_some_server(ML_server_type type, int target_count, Window owner, Logger logger)
    //**********************************************************
    {
        switch( type)
        {
            case MobileNet_image_similarity_embeddings_server:
                ML_servers_util.start_some_image_similarity_servers(target_count,owner, logger);
                break;
            case FaceNet_similarity_embeddings_server:
                ML_servers_util.start_face_embeddings_servers(owner, logger);
                break;
            case Haars_face_detection_server:
                ML_servers_util.start_haars_face_detection_servers(owner, logger);
                break;
            case MTCNN_face_detection_server:
                ML_servers_util.start_MTCNN_face_detection_servers(owner, logger);
                break;
        }
    }

    // returns NEW servers
    //**********************************************************
    static int scan_registry(ML_service_type st, Window owner, Logger logger)
    //**********************************************************
    {
        logger.log("for " + st.ml_server_type().name() + " SCANNING registry: "+st.ml_server_type().registry_path(owner, logger));
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
                logger.log("considering registry file: " + f.getAbsolutePath());
                // the file content is like this:
                // {"name": "MobileNet_embeddings_server",
                // "port": 54225,
                // "uuid": "072ff019-5884-44f9-8b40-fe4ea967d4f8"}
                String content = Files.readString(f.toPath());

                String name = read_key(content,"name");
                logger.log("looking for " + st.ml_server_type().name() + " found instance name: " + name);
                if ( !st.ml_server_type().name().contains(name))
                {
                    logger.log("->"+name+ "<- does not contain ->" + st.ml_server_type().name()+ "<-");
                    continue;
                }

                if ( st.face_detection_type() == null)
                {
                    logger.log("st.face_detection_type() == null " );
                }
                else
                {
                    if ( st.face_detection_type() != Face_detection_type.MTCNN)
                    {
                        String sub_type = read_key(content, "sub_type");
                        logger.log("looking for " + st.face_detection_type().name() + " found  sub_type: " + sub_type);
                        if (!sub_type.equals(st.face_detection_type().name())) {
                            logger.log("->"+sub_type + "<- does not match ->" + st.face_detection_type().name()+ "<-");
                            continue;
                        }

                    }
                }
                String port_s = read_key(content,"port");
                logger.log("PORT PORT !!! for " + st.ml_server_type().name() + " found PORT: " + port_s);
                int port = Integer.parseInt(port_s);

                String uuid = read_key(content,"uuid");
                logger.log("for " + st.ml_server_type().name() + " found uuid: " + uuid);

                Set<Integer> set = server_ports.computeIfAbsent(get_key(st), k -> new HashSet<>());
                // is this server alive?
                if ( is_port_alive(port, logger))
                {
                    boolean was_new = set.add(port);
                    if ( was_new) returned++;
                    logger.log("✅ " + st.ml_server_type().name() + " server is alive at port " + port);
                }
                else
                {
                    set.remove(port);
                    logger.log("❌ " + st.ml_server_type().name() + " server at port " + port + " is not responding to health check.");
                    try {
                        Files.delete(f.toPath());
                        logger.log("Deleted stale registry file: " + f.getAbsolutePath());
                    } catch (IOException e) {
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
        List<Integer> active_ports = find_active_servers(st, owner, logger);
        if (active_ports.isEmpty()) {
            logger.log("No active servers found for: " + st.ml_server_type().name());
            return -1;
        }
        if ( dbg) logger.log("Found "+active_ports.size()+" active servers for: " + st.ml_server_type().name());
        if (active_ports.size() >= st.ml_server_type().target_server_count(owner))
        {
            if ( pump_aborter != null) pump_aborter.abort("Enough active servers found for: " + st.ml_server_type().name());
        }

        // Return a random port for load balancing
        return active_ports.get(random.nextInt(active_ports.size()));
    }

    //**********************************************************
    private static String read_key(String content, String key)
    //**********************************************************
    {
        // the file content is like this:
        // {"name": "MobileNet_embeddings_server",
        // "port": 54225,
        // "uuid": "072ff019-5884-44f9-8b40-fe4ea967d4f8"}
        try {
            String value = content.split("\"" + key + "\"")[1]
                                  .split(":")[1]
                                  .split(",")[0]
                                  .replaceAll("[\", ]", "");
            return value;
        } catch (Exception e) {
            return "";
        }
    }

    //**********************************************************
    private static boolean is_port_alive(int port, Logger logger)
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
                logger.log("Health check response code: " + responseCode + ", content: " + content);
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

