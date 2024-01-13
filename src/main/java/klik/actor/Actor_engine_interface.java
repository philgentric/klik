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
    int how_many_threads_are_in_flight();

    //**********************************************************
    default void cancel_all(ConcurrentLinkedQueue<Job> jobs)
    //**********************************************************
    {
        for ( Job j : jobs)
        {
            cancel_one(j);
        }
    }

    //**********************************************************
    default Job execute_internal(Runnable r, Logger logger)
    //**********************************************************
    {
        Actor actor = new Actor() {
            @Override
            public String run(Message m) {
                r.run();
                return "DONE";
            }
        };
        Message message = new Message() {
            @Override
            public Aborter get_aborter() {
                return new Aborter();
            }
        };
        return run(actor,message,null,logger);
    }
}

