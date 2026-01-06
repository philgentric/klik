// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.util.execute.actor;

import klikr.util.log.Logger;

/*
a Job is a pair of code (Actor) + parameters (Message)
+ optionally a job termination reporter i.e.
some code that will be executed at the end of THAT Job
(which avoids to implement it in the Actor)
 */
//**********************************************************
public class Job
//**********************************************************
{
    public final Actor actor;
    public final Message message;
    public final Logger logger;
    public final Job_termination_reporter termination_reporter; // optional, maybe null

    //**********************************************************
    public Job(Actor actor, Message message, Job_termination_reporter termination_reporter_, Logger logger)
    //**********************************************************
    {
        this.logger = logger;
        if ( actor==null)
        {
            logger.log_stack_trace("❌❌❌ FATAL actor cannot be null");
        }
        if ( message==null)
        {
            logger.log_stack_trace("❌❌❌ FATAL message cannot be null");
        }
        if ( message.get_aborter()==null)
        {
            logger.log_stack_trace("❌❌❌ FATAL Aborter cannot be null");
            System.exit(-1);
        }
        this.actor = actor;
        this.message = message;
        termination_reporter = termination_reporter_;

    }

    //**********************************************************
    public String to_string()
    //**********************************************************
    {
        return actor.name()+"-"+message.to_string();
    }

    //**********************************************************
    public void has_ended(String message)
    //**********************************************************
    {
        if (termination_reporter !=null) termination_reporter.has_ended(message,this);
    }


    //**********************************************************
    public void cancel()
    //**********************************************************
    {
        // this is the reason why the aborter passed in the Message
        // constructor MUST NOT be null: we need it
        // to implement cancellation
        message.get_aborter().abort("Job cancelled, Actor: "+actor.name());
        has_ended(message.to_string()+" cancelled");
    }

}
