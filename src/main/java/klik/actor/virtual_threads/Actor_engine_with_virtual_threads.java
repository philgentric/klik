package klik.actor.virtual_threads;

import klik.actor.*;
import klik.util.log.Logger;
import klik.util.execute.Threads;

import java.util.concurrent.ExecutorService;

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

    //**********************************************************
    public Actor_engine_with_virtual_threads(Logger logger_)
    //**********************************************************
    {
        logger = logger_;
    }

    //**********************************************************
    @Override
    public Job run(Actor actor, Message message, Job_termination_reporter tr, Logger logger)
    //**********************************************************
    {
        Job job = new Job(actor,message,tr,logger);
        Runnable r = () -> {
            int now = Actor_engine.threads_in_flight.incrementAndGet();
            //if ( now > recent_max_threads) recent_max_threads = now;
            String msg = job.actor.run(job.message);
            job.has_ended(msg);
            Actor_engine.threads_in_flight.decrementAndGet();
        };
        ExecutorService executor_service = Threads.get_executor_service(logger);
        executor_service.execute(r);
        return job;
    }


    //**********************************************************
    @Override
    public int how_many_threads_are_in_flight()
    //**********************************************************
    {
        //return recent_max_threads;
        return Actor_engine.threads_in_flight.get();
/*
        int returned = Actor_engine.threads_in_flight.get();
        if ( returned != 0) return returned;
        returned = recent_max_threads;
        recent_max_threads = 0;
        return returned;

 */
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
