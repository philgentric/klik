//SOURCES ./Actor_engine_interface.java
//SOURCES ./virtual_threads/Actor_engine_with_virtual_threads.java
//SOURCES ./workers/Actor_engine_based_on_workers.java
//SOURCES ../util/execute/Threads.java
package klik.actor;

import klik.actor.virtual_threads.Actor_engine_with_virtual_threads;
import klik.actor.workers.Actor_engine_based_on_workers;
import klik.util.log.Logger;
import klik.util.execute.Threads;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

//**********************************************************
public class Actor_engine // is a singleton
//**********************************************************
{
    public static final boolean cancel_dbg = false;
    private static Actor_engine_interface instance;
    public static final AtomicInteger threads_in_flight = new AtomicInteger(0);
    public static int recent_max_threads = 0;


    //**********************************************************
    public static Actor_engine_interface create(Logger logger)
    //**********************************************************
    {
        if ( instance != null) return instance;
        if (Threads.use_virtual_threads)
        {
            instance = new Actor_engine_with_virtual_threads(logger);
        }
        else
        {
            instance = new Actor_engine_based_on_workers(logger);
        }
        return instance;

    }
    //**********************************************************
    public static Actor_engine_interface get_instance()
    //**********************************************************
    {
        return instance;
    }

    //**********************************************************
    public static int how_many_threads_are_in_flight(Logger logger)
    //**********************************************************
    {
        return threads_in_flight.get();
    }
    public static int max_threads_in_flight(Logger logger)
    {
        if ( instance == null) return 0;
        return recent_max_threads;
    }

    //**********************************************************
    public static Job run(Actor actor, Message message, Job_termination_reporter tr, Logger logger)
    //**********************************************************
    {
        if ( instance == null) instance = create(logger);
        return instance.run(actor,message,tr,logger);
    }

    //**********************************************************
    public static void cancel_all(ConcurrentLinkedQueue<Job> jobs)
    //**********************************************************
    {
        if ( instance == null) return;
        instance.cancel_all(jobs);
    }

    //**********************************************************
    public static void cancel_job(Job job)
    //**********************************************************
    {
        if ( instance == null) return;
        instance.cancel_job(job);
    }

    //**********************************************************
    public static Job execute(Runnable r, String id, Logger logger)
    //**********************************************************
    {
        if ( instance == null) instance = create(logger);
        return instance.execute_internal(r, id,logger);
    }
}
