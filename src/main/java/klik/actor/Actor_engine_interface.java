package klik.actor;

import klik.util.Logger;

import java.util.concurrent.ConcurrentLinkedQueue;

//**********************************************************
public interface Actor_engine_interface
//**********************************************************
{

    Job run(Actor actor, Message message, Job_termination_reporter tr, Logger logger); // message and/or tr may be null
    void cancel_one(Job canceled);
    void stop();

    //**********************************************************
    default void cancel_all(ConcurrentLinkedQueue<Job> jobs)
    //**********************************************************
    {
        for ( Job j : jobs)
        {
            cancel_one(j);
        }
    }
}

