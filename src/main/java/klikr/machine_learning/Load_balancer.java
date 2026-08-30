package klikr.machine_learning;

import javafx.stage.Window;
import klikr.machine_learning.monitoring.ML_registry;
import klikr.machine_learning.monitoring.ML_servers_monitor;
import klikr.settings.boolean_features.Feature;
import klikr.settings.boolean_features.Feature_cache;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

//**********************************************************
public class Load_balancer
//**********************************************************
{
    private static Load_balancer instance;
    private final static boolean dbg = false;
    private final Random random = new Random();
    private Aborter pump_aborter;
    private final AtomicBoolean server_pump_started = new AtomicBoolean(false);
    private final BlockingQueue<ML_server_type> request_queue = new LinkedBlockingQueue<>();
    // key is the type name
    private final Map<String,Integer> how_many_servers_are_requested = new ConcurrentHashMap<>();
    private final Map<String,Integer> how_many_servers_are_running_for_type = new ConcurrentHashMap<>();


    //**********************************************************
    public static int get_random_active_port(ML_server_type server_type, Window owner, Logger logger)
    //**********************************************************
    {
        if ( instance == null) instance = new Load_balancer();
        return instance.get_random_active_port_internal(server_type, owner, logger);
    }


    //**********************************************************
    private int get_random_active_port_internal(ML_server_type server_type, Window owner, Logger logger)
    //**********************************************************
    {
        //boolean live_dbg = Feature_cache.get(Feature.Enable_ML_server_debug);
        ML_servers_status status = find_active_servers(server_type, owner, logger);
        if (status.available_ports().isEmpty())
        {
            if ( status.launch_status() == ML_server_launch_status.STARTING )
            {
                logger.log("Servers are starting (no servers are active yet) for: " + server_type.name());
                return -1;
            }
            logger.log(Stack_trace_getter.get_stack_trace(Logger.error+"FATAL for: " + server_type.name()));
            return -1;
        }
        //logger.log(Logger.ok+" Found "+status.available_ports().length()+" active servers for: " + st.name());

        // Return a random port for load balancing
        return status.available_ports().get(random.nextInt(status.available_ports().size()));
    }
    //**********************************************************
    private ML_servers_status find_active_servers(ML_server_type server_type, Window owner, Logger logger)
    //**********************************************************
    {
        List<ML_server> servers_of_type = ML_registry.get_servers_of_type(server_type, owner, logger);
        ML_server_launch_status status = ML_server_launch_status.RUNNING;
        if (servers_of_type.size() <server_type.quota(owner))
        {
            if ( dbg) logger.log("\n\nfind_active_servers_internal server type under quota "+server_type.name());
            if (!server_pump_started.get())
            {
                if ( dbg) logger.log("find_active_servers_internal starting server creation pump");
                start_server_creation_job(owner, logger);
            }
            // send a request for more servers
            status = ML_server_launch_status.STARTING;
            if ( request_queue.offer(server_type))
            {
                if ( dbg) logger.log("find_active_servers_internal request sent");
            }
            else
            {
                if ( dbg) logger.log("find_active_servers_internal request denied (queue is full)");
            }

        }
        List<Integer> active_servers_ports = new ArrayList<>();
        for ( ML_server ml_server : servers_of_type ) active_servers_ports.add(ml_server.port());
        if ( dbg) logger.log("find_active_servers_internal "+active_servers_ports.size()+" active servers detected");
        return new ML_servers_status(active_servers_ports, status);
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
                    ML_server_type server_type = request_queue.poll(60, TimeUnit.SECONDS);
                    if (server_type == null) {
                        // timeout
                        if (pump_aborter.should_abort()) {
                            server_pump_started.set(false);
                            return;
                        }
                        continue;
                    }

                    List<ML_server> servers_of_type = ML_registry.get_servers_of_type(server_type, owner, logger);
                    if (servers_of_type.size() >= server_type.quota(owner))
                    {
                        if ( live_dbg) logger.log("\n\n"+ Logger.ok+" No need to start servers of type: "+server_type.name()+ " running: "+servers_of_type.size()+ " quota: "+server_type.quota(owner));
                        continue;
                    }
                    // need to launch some servers
                    Integer running = how_many_servers_are_running_for_type.get(server_type.name());
                    if ( running == null)
                    {
                        how_many_servers_are_running_for_type.put(server_type.name(),0);
                        running = 0;
                    }
                    int full_quota = server_type.quota(owner);
                    Integer requested = how_many_servers_are_requested.get(server_type.name());
                    if ( requested == null )
                    {
                        how_many_servers_are_requested.put(server_type.name(),0);
                        requested = 0;
                    }
                    logger.log("\n\n\n\nML servers of type: " + server_type.name()+
                            "\n quota=" + full_quota +
                            "\n requested=" + requested +
                            "\n running=" + running);

                    int more = full_quota - running - requested;
                    if ( more > 0)
                    {
                        how_many_servers_are_requested.put(server_type.name(), requested+more);
                        if ( live_dbg) logger.log("\n\n"+ Logger.ok+" Going to spawn " + more + " new servers of type: " + server_type.name());
                        ML_servers_monitor.make_faster_network_scans();
                        ML_servers_util.start_N_ML_servers(more, server_type, owner, logger);
                    }
                    else
                    {
                        if ( live_dbg) logger.log("\n\n"+ Logger.ok+"  Not going to spawn new servers of type: " + server_type.name()+ " because: quota=" + full_quota + " running=" + running);
                    }
                    if ( running >= full_quota)
                    {
                        how_many_servers_are_requested.put(server_type.name(), 0);
                    }
                } catch (InterruptedException e) {
                    logger.log("Interrupted while waiting for servers to start");
                }
            }
        };
        Actor_engine.execute(r,"ML_server_launch",logger);
    }



}
