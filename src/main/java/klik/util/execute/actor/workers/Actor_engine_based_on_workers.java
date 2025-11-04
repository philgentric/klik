// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ./Worker.java
package klik.util.execute.actor.workers;

import klik.System_info;
import klik.util.execute.actor.*;
import klik.util.log.Logger;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

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
    private final Aborter cleanup_aborter;
    private final Logger logger;
    ConcurrentLinkedQueue<Worker> runners = new ConcurrentLinkedQueue<>();
    LinkedBlockingQueue<Job> input_queue_single = new LinkedBlockingQueue<>();


    //**********************************************************
    public Actor_engine_based_on_workers(String purpose, Aborter actor_engine_cleanup_aborter, Logger logger)
    //**********************************************************
    {
        this.logger = logger;
        this.cleanup_aborter = actor_engine_cleanup_aborter;
        int number_of_runners = System_info.how_many_cores() -1;
        if ( number_of_runners < 1) number_of_runners = 1;
        if ( dbg) logger.log("✅ Actor_engine_based_on_workers starting with "+number_of_runners+" workers");

        for (int i = 0; i < number_of_runners; i++)
        {
            Worker r = new Worker("worker_"+i+" of "+purpose,input_queue_single, cleanup_aborter, logger);
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
        if ( dbg) logger.log("✅ "+am.to_string()+" scheduled for execution");
    }

    //**********************************************************
    public void stop()
    //**********************************************************
    {
        for ( Worker r : runners) r.stop();
    }


    //**********************************************************
    public void cancel_queued_job(Job job)
    //**********************************************************
    {
        if ( input_queue_single.remove(job))
        {
            if ( Actor_engine.cancel_dbg) logger.log("✅ Actor-Message removed from queue (canceled before start): "+job.to_string());
        }
    }


}
