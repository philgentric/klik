package klik.actor;

import klik.util.log.Logger;

import java.util.concurrent.ConcurrentLinkedQueue;

//**********************************************************
public interface Actor_engine_interface
//**********************************************************
{
    Job run(Actor actor, Message message, Job_termination_reporter tr, Logger logger); // error_message and/or tr may be null
    //void cancel_job(Job canceled);
    void stop();



    //**********************************************************
    default void cancel_jobs(ConcurrentLinkedQueue<Job> jobs)
    //**********************************************************
    {
        for ( Job j : jobs)  j.cancel();
    }

    //**********************************************************
    default Job execute_internal(Runnable r,  String job_description, Logger logger)
    //**********************************************************
    {
        Actor actor = new Actor() {
            //**********************************************************
            @Override
            public String run(Message m)
            //**********************************************************
            {
                r.run();
                return "DONE";
            }

            //**********************************************************
            @Override
            public String name()
            //**********************************************************
            {
                return job_description;
            }

        };
        Message message = new Message() {
            @Override
            public String to_string() {
                return ""; //this is a dummy
            }

            @Override
            public Aborter get_aborter() {
                return new Aborter("thread for "+job_description,logger);
            }
        };


        return run(actor,message,null,logger);
    }





}

