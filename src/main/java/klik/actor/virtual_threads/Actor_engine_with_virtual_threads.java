package klik.actor.virtual_threads;

import klik.actor.*;
import klik.util.Logger;

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
        Job job = new Job(actor,message,tr);
        Runnable r = () -> {
            String msg = job.actor.run(job.message);
            if ( job.termination_reporter != null) job.termination_reporter.has_ended(msg, job);
        };
        job.thread = Thread.ofVirtual().start(r);
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
    public void cancel_one(Job job)
    //**********************************************************
     {
         if (job==null) return;
         job.message.get_aborter().abort();
         if ( job.thread != null) job.thread.interrupt();

         //logger.log("virtual threads engine has cancelled: "+job.to_string());
         job.has_ended("Engine received cancel for "+job.to_string()+" (virtual threads)");
    }


}