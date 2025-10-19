package klik.actor.virtual_threads;

import klik.actor.*;
import klik.util.log.Logger;
import klik.util.execute.Threads;
import klik.util.log.Stack_trace_getter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;

//**********************************************************
public class Actor_engine_with_virtual_threads implements Actor_engine_interface
//**********************************************************
{
    /*
    this actor engine does use any worker nor a job queue
    it simply creates a thread for each job!
    (so we can have as many actors that sleep as you want, we never risk deadlock!)
     */
    private final Logger logger;
    LinkedBlockingQueue<Job> job_queue = new LinkedBlockingQueue<>();

    //**********************************************************
    public Actor_engine_with_virtual_threads(Logger logger_)
    //**********************************************************
    {
        logger = logger_;
    }
    //**********************************************************
    public LinkedBlockingQueue<Job> get_job_queue()
    //**********************************************************
    {
        return job_queue;
    }


    //**********************************************************
    @Override
    public Job run(Actor actor, Message message, Job_termination_reporter tr, Logger logger)
    //**********************************************************
    {
        Job job = new Job(actor,message,tr,logger);
        job_queue.add(job);
        Runnable r = () -> {
            int now = Actor_engine.threads_in_flight.incrementAndGet();
            //logger.log("Actor_engine_with_virtual_threads: "+now+" threads in flight");
            if ( now > Actor_engine.recent_max_threads) Actor_engine.recent_max_threads = now;
            String msg = job.actor.run(job.message);
            job.has_ended(msg);
            Actor_engine.threads_in_flight.decrementAndGet();
            job_queue.remove(job);
        };
        ExecutorService executor_service = Threads.get_executor_service(logger);
        try
        {
            executor_service.execute(r);
        }
        catch (RejectedExecutionException e)
        {
            job.has_failed(""+e);
            logger.log(Stack_trace_getter.get_stack_trace("WARNING: thread not started"+e));
        }
        return job;
    }


    //**********************************************************
    public void stop()
    //**********************************************************
    {
        logger.log("Actor_engine_with_virtual_threads stop requested, is NOP with virtual threads");
    }

    //**********************************************************
    @Override
    public void cancel_job(Job job)
    //**********************************************************
     {
         if (job==null) return;
         job.cancel("virtual thread job cancelled");

         if( Actor_engine.cancel_dbg) logger.log("virtual threads engine has cancelled: "+job.to_string());
         job.has_ended("Engine received cancel for "+job.to_string()+" (virtual threads)");
    }


}
