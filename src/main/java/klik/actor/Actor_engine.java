package klik.actor;

import klik.actor.virtual_threads.Actor_engine_with_virtual_threads;
import klik.actor.workers.Actor_engine_based_on_workers;
import klik.util.Logger;

import java.util.concurrent.ConcurrentLinkedQueue;

//**********************************************************
public class Actor_engine // is a singleton
//**********************************************************
{
    private static Actor_engine_interface instance;
    public static final boolean use_virtual_threads = true;

    //**********************************************************
    public static Actor_engine_interface get(Logger logger)
    //**********************************************************
    {
        if ( instance != null) return instance;
        if (use_virtual_threads)
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
    public static Job run(Actor actor, Message message, Job_termination_reporter tr, Logger logger)
    //**********************************************************
    {
        if ( instance == null) instance = get(logger);
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
    public static void cancel_one(Job job)
    //**********************************************************
    {
        if ( instance == null) return;
        instance.cancel_one(job);
    }
}
