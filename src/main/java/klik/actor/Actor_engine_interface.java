package klik.actor;

import klik.util.log.Logger;

import java.util.concurrent.ConcurrentLinkedQueue;

//**********************************************************
public interface Actor_engine_interface
//**********************************************************
{
    Job run(Actor actor, Message message, Job_termination_reporter tr, Logger logger); // error_message and/or tr may be null
    void cancel_job(Job canceled);
    void stop();

    //**********************************************************
    default void cancel_all(ConcurrentLinkedQueue<Job> jobs)
    //**********************************************************
    {
        for ( Job j : jobs)  cancel_job(j);
    }

    //**********************************************************
    default Job execute_internal(Runnable r,  Logger logger)
    //**********************************************************
    {
        Actor actor = new Actor() {
            @Override
            public String run(Message m) {
                r.run();
                return "DONE";
            }
        };
        Message message = () -> new Aborter("default thread",logger);
        return run(actor,message,null,logger);
    }





}

