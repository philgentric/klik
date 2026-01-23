package klikr.util.http;

import com.sun.net.httpserver.HttpServer;
import javafx.stage.Window;
import klikr.properties.Non_booleans_properties;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.execute.actor.Executor;
import klikr.util.files_and_paths.Static_files_and_paths_utilities;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

//**********************************************************
public class Klikr_communicator
//**********************************************************
{
    private static Path REGISTRY_DIR;


    private HttpServer server;
    private int the_port;
    private final String my_UUID = UUID.randomUUID().toString();
    private Path registry_file;

    private final Logger logger;
    public final String app_name;

    // Callbacks for when we receive messages
    private Runnable on_ping;
    private Consumer<String> on_appearance_changed;
    private List<Runnable> on_started_received = new ArrayList<>();
    private Runnable on_play_received;

    //**********************************************************
    public void set_on_play_received(Runnable on_play_received)
    //**********************************************************
    {
        this.on_play_received = on_play_received;
    }



    //**********************************************************
    public void set_on_appearance_changed(Consumer<String> on_appearance_changed)
    //**********************************************************
    {
        this.on_appearance_changed = on_appearance_changed;
    }





    //**********************************************************
    public void register_on_started_received(Runnable on_started_received)
    //**********************************************************
    {
        this.on_started_received.add(on_started_received);
    }
    //**********************************************************
    public void deregister_on_started_received(Runnable on_started_received)
    //**********************************************************
    {
        this.on_started_received.remove(on_started_received);
    }


    //**********************************************************
    public int get_port()
    //**********************************************************
    {
        return the_port;
    }


    //**********************************************************
    public Klikr_communicator(String app_name, Window owner, Logger logger)
    //**********************************************************
    {
        this.app_name = app_name;
        this.logger = logger;
        REGISTRY_DIR = Static_files_and_paths_utilities.get_absolute_hidden_dir_on_user_home("registry", false,owner, logger);
        if ( REGISTRY_DIR == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("❌ Fatal REGISTRY_DIR == null"));
        }
    }

    // --- Mode Singleton Startup ---
    // Returns false if another instance is already running.
    //**********************************************************
    public boolean start_as_singleton()
    //**********************************************************
    {
        Path singleton_path = REGISTRY_DIR.resolve(app_name+".json");

        if (Files.exists(singleton_path)) 
        {
            // Potential conflict. Verify if it's actually alive.
            int existingPort = read_port(singleton_path);
            logger.log(app_name+" singleton port from file " + existingPort);

            if (is_port_alive(existingPort)) 
            {
                logger.log(app_name+" singleton is already running on " + existingPort + ". Exiting.");
                return false; // Another instance exists and is healthy
            } else {
                logger.log(app_name+ " found stale singleton lock file. Taking over.");
                try {
                    Files.delete(singleton_path); // It's dead, we can overwrite
                } catch (IOException e) {
                    logger.log(""+e);
                    return false;
                }
            }
        }

        // We are clear to start
        start_server();
        write_registry_file(singleton_path);
        return true;
    }

    // --- Mode 2: App B (Multi-Instance) Startup ---
    //**********************************************************
    public void start_as_multi_instance()
    //**********************************************************
    {
        start_server();
        // Use a unique name so we never conflict
        Path myPath = REGISTRY_DIR.resolve( "app_" + my_UUID + ".json");
        write_registry_file(myPath);
    }

    //**********************************************************
    private void start_server()
    //**********************************************************
    {

        // CRITICAL FIX: Do NOT use: new InetSocketAddress(0)
        // This binds 0.0.0.0 (All Interfaces) and cause FW issues in Windows
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            logger.log(""+e);
        }


        the_port = server.getAddress().getPort();

        logger.log(app_name+" HTTP server started on port"+the_port);


        // Endpoint for newly started app to confirm they started
        server.createContext("/started", ex -> {
            logger.log(app_name+" HTTP server 'started' request received");
            for (Runnable r : on_started_received ) r.run();
            String resp = "OK";
            ex.sendResponseHeaders(200, resp.length());
            ex.getResponseBody().write(resp.getBytes());
            ex.close();
        });

// Endpoint for newly started app to confirm they started
        server.createContext("/play", ex -> {
            logger.log(app_name+" HTTP server 'play' request received");
            if ( on_play_received != null ) on_play_received.run();
            String resp = "OK";
            ex.sendResponseHeaders(200, resp.length());
            ex.getResponseBody().write(resp.getBytes());
            ex.close();
        });


        // Endpoint for others to check if I'm alive
        server.createContext("/health", ex -> {
            logger.log(app_name+" HTTP server 'health' request received");

            String resp = "OK";
            ex.sendResponseHeaders(200, resp.length());
            ex.getResponseBody().write(resp.getBytes());
            ex.close();
        });

        // Endpoint to receive appearance updates
        server.createContext("/appearance", ex -> {
            logger.log(app_name+" HTTP server 'appearance' request received");

            if ("POST".equalsIgnoreCase(ex.getRequestMethod())) {
                String body = new String(ex.getRequestBody().readAllBytes());
                // body might be "dark" or {"theme":"dark"}
                logger.log("Received appearance update: " + body);
                if (on_appearance_changed != null) {
                    on_appearance_changed.accept(body); // Update UI
                }
                ex.sendResponseHeaders(200, 0);
            } else {
                ex.sendResponseHeaders(405, 0);
            }
            ex.close();
        });

        server.setExecutor(Executor.executor);
        server.start();

        // Ensure cleanup on shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { Files.deleteIfExists(registry_file); } catch (Exception e) {}
        }));

        logger.log(app_name+ " OK, started HTTP Server on port: " + the_port);
    }

    // --- Broadcast Logic ---
    //**********************************************************
    public void broadcast(String msg)
    //**********************************************************
    {
        logger.log(app_name+" HTTP Server broadcasting " + msg);

        File folder = REGISTRY_DIR.toFile();
        File[] files = folder.listFiles((d, name) -> name.endsWith(".json"));

        if (files == null) return;

        for (File f : files) {
            // Don't message ourselves
            if (f.toPath().equals(registry_file)) continue;

            int targetPort = read_port(f.toPath());
            if (targetPort == -1) continue; // broken file

            boolean success = send_appearance_update(targetPort, msg);

            if (!success) {
                logger.log("Target at port " + targetPort + " is dead. Cleaning registry.");
                try { Files.delete(f.toPath()); } catch (IOException e) {}
            }
        }
    }


    //**********************************************************
    private void write_registry_file(Path path)
    //**********************************************************
    {
        String json = "{\"name\": \"" + app_name + "\", \"port\": " + the_port + ", \"uuid\": \"" + my_UUID + "\"}";
        try {
            Files.writeString(path, json);
        } catch (IOException e) {
            logger.log(""+e);
        }
        this.registry_file = path;
    }

    //**********************************************************
    private String read_name(Path path)
    //**********************************************************
    {
        try {
            String content = Files.readString(path);
            String[] s1 = content.split("\"name\"");
            if ( s1.length < 2) return null;
            String[] s2 = s1[1].split(":");
            if ( s2.length < 2) return null;
            String[] s3 = s2[1].split("\"");
            if ( s3.length < 2) return null;
            return s3[1];
        }
        catch (Exception e)
        {
            //logger.log(""+e);
            return null;
        }
    }

    // scans the registry for active servers with this name
    //**********************************************************
    public List<Integer> find_active_servers(String target_app_name)
    //**********************************************************
    {
        List<Integer> list = new ArrayList<>();
        if ( REGISTRY_DIR == null) return list;
        File folder = REGISTRY_DIR.toFile();
        File[] files = folder.listFiles((d, name) -> name.endsWith(".json"));

        if (files == null) return list;

        for (File f : files) {
            String name = read_name(f.toPath());
            if ( name != null && name.equals(target_app_name))
            {
                int port = read_port(f.toPath());
                if ( port > 0)
                {
                    if ( is_port_alive(port))
                    {
                        list.add(port);
                    }
                    else
                    {
                        // clean up dead registry file
                         try { Files.delete(f.toPath()); } catch (IOException e) {}
                    }
                }
            }
        }
        return list;
    }

    //**********************************************************
    private int read_port(Path path)
    //**********************************************************
    {
        try {
            String content = Files.readString(path);
            // Quick/Dirty parsing. Use checking logic strictly.
            String port_string = content.split("\"port\"")[1].split(":")[1].split(",")[0].replaceAll("[^0-9]", "");
            return Integer.parseInt(port_string);
        }
        catch (Exception e)
        {
            logger.log(""+e);
            return -1;
        }
    }

    //**********************************************************
    private boolean is_port_alive(int port)
    //**********************************************************
    {
        if (port == -1) return false;
        return send_request(port, "/health", "GET", null);
    }

    // Returns true if message delivered, false if connection refused
    //**********************************************************
    private boolean send_started(int port, String body)
    //**********************************************************
    {
        return send_request(port, "/started", "POST", body);
    }

    // Returns true if message delivered, false if connection refused
    //**********************************************************
    private boolean send_appearance_update(int port, String body)
    //**********************************************************
    {
        return send_request(port, "/appearance", "POST", body);
    }

    //**********************************************************
    public boolean send_request(int port, String endpoint, String method, String body)
    //**********************************************************
    {
        try {
            URL url = new URL("http://127.0.0.1:" + port + endpoint);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod(method);
            con.setConnectTimeout(100); // Fast fail is key for UI responsiveness
            con.setReadTimeout(100);

            if (body != null) {
                con.setDoOutput(true);
                try(OutputStream os = con.getOutputStream()) {
                    os.write(body.getBytes());
                }
            }

            int status = con.getResponseCode();
            return status == 200;
        }
        catch (IOException e) {
            return false; // "Stale File" detector
        }
    }

    //**********************************************************
    public void stop()
    //**********************************************************
    {
        server.stop(0);
    }

}
