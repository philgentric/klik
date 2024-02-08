package klik.actor.workers;

import klik.actor.*;
import klik.util.Logger;

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
    (so in case we have N actors that sleep we deadlock!)
     */

    private static final boolean dbg = false;
    private static final int MULTIPLICATION_FACTOR_FOR_THREAD_COUNT = 4;
    private final Logger logger;
    private final Aborter aborter;
    ConcurrentLinkedQueue<Worker> runners = new ConcurrentLinkedQueue<>();
    LinkedBlockingQueue<Job> input_queue_single = new LinkedBlockingQueue<>();
    private final AtomicInteger threads_in_flight = new AtomicInteger(0);


    //**********************************************************
    public Actor_engine_based_on_workers(Aborter aborter, Logger logger_)
    //**********************************************************
    {
        this.aborter = aborter;
        int number_of_runners = MULTIPLICATION_FACTOR_FOR_THREAD_COUNT*Runtime.getRuntime().availableProcessors();

        logger = logger_;
        for (int i = 0; i < number_of_runners; i++)
        {
            Worker r = new Worker("runner_"+i,input_queue_single,threads_in_flight,aborter, logger);
            runners.add(r);
        }
        start();
    }
    //**********************************************************
    @Override
    public Aborter get_aborter()
    //**********************************************************
    {
        return aborter;
    }

    //**********************************************************
    @Override
    public Job run(Actor actor, Message message, Job_termination_reporter tr, Logger logger)
    //**********************************************************
    {
        Job j = new Job(actor,message,tr,logger);
        run_job(j);
        return j;
    }

    //**********************************************************
    private void start()
    //**********************************************************
    {
        for ( Worker r : runners) r.start();
    }

    //**********************************************************
    private void run_job(Job am)
    //**********************************************************
    {
        input_queue_single.add(am);
        threads_in_flight.incrementAndGet();
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
    public int how_many_threads_are_in_flight()
    //**********************************************************
    {
        return threads_in_flight.get();
    }

    //**********************************************************
    @Override
    public void cancel_one(Job job)
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
            job.cancel();
        }
        job.has_ended("Engine received cancel for "+job.to_string());
        threads_in_flight.decrementAndGet();
    }


}
