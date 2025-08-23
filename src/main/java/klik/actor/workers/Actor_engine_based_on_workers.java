//SOURCES ./Worker.java
package klik.actor.workers;

import klik.System_info;
import klik.actor.*;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

//**********************************************************
public class Actor_engine_based_on_workers implements Actor_engine_interface
//**********************************************************
{
    /*
    this actor engine has  N workers, each with its own thread
    each pumping jobs out of a single queue
    and executing the job on its own thread
    (so in case we have N actors that sleep/wait on another, we deadlock!)
     */

    private static final boolean dbg = false;
    private final Logger logger;
    ConcurrentLinkedQueue<Worker> runners = new ConcurrentLinkedQueue<>();
    LinkedBlockingQueue<Job> input_queue_single = new LinkedBlockingQueue<>();


    //**********************************************************
    public Actor_engine_based_on_workers(Logger logger_)
    //**********************************************************
    {
        logger = logger_;
        int number_of_runners = System_info.how_many_cores() -1;
        if ( number_of_runners < 1) number_of_runners = 1;
        logger.log("Actor_engine_based_on_workers starting with "+number_of_runners+" workers");

        for (int i = 0; i < number_of_runners; i++)
        {
            Worker r = new Worker("runner_"+i,input_queue_single, logger);
            runners.add(r);
        }
        start();
    }



    //**********************************************************
    @Override
    public Job run(Actor actor, Message message, Job_termination_reporter tr, Logger logger)
    //**********************************************************
    {
        Job j = new Job(actor,message,tr,logger);
        queue_job(j);
        //logger.log(Stack_trace_getter.get_stack_trace("Actor_engine_based_on_workers: "+input_queue_single.size()+" queued jobs"));
        return j;
    }

    //**********************************************************
    private void start()
    //**********************************************************
    {
        for ( Worker r : runners) r.start();
    }

    //**********************************************************
    private void queue_job(Job am)
    //**********************************************************
    {
        input_queue_single.add(am);
        if ( dbg) logger.log(am.to_string()+" scheduled for execution");
    }

    //**********************************************************
    public void stop()
    //**********************************************************
    {
        for ( Worker r : runners) r.stop();
    }

    //**********************************************************
    @Override
    public void cancel_job(Job job)
    //**********************************************************
    {
        if ( job == null) return;
        if ( input_queue_single.remove(job))
        {
            if ( Actor_engine.cancel_dbg) logger.log("Actor-Message removed from queue (canceled before start): "+job.to_string());
        }
        else
        {
            if ( Actor_engine.cancel_dbg) logger.log("Actor-Message NOT found, actor canceled after start: "+job.to_string());
            job.cancel("worked thread job cancelled");
        }
        job.has_ended("Engine received cancel for "+job.to_string());
    }


}
