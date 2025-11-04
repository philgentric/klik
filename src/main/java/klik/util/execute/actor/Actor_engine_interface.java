// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.util.execute.actor;

import klik.util.execute.actor.workers.Actor_engine_based_on_workers;
import klik.util.log.Logger;

import java.util.concurrent.ConcurrentLinkedQueue;

//**********************************************************
public interface Actor_engine_interface
//**********************************************************
{
    Job run(Actor actor, Message message, Job_termination_reporter tr, Logger logger); // error_message and/or tr may be null
    void stop();

    //**********************************************************
    default void cancel_jobs(ConcurrentLinkedQueue<Job> jobs)
    //**********************************************************
    {
        for ( Job j : jobs)
        {
            if ( this instanceof Actor_engine_based_on_workers aebow)
            {
                aebow.cancel_queued_job(j);
            }
            j.cancel();
        }
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
            public String to_string() {return "";}

            @Override
            public Aborter get_aborter() {
                return new Aborter("thread for "+job_description,logger);
            }
        };
        return run(actor,message,null,logger);
    }
}

