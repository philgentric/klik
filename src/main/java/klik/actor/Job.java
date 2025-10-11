package klik.actor;

import klik.util.log.Logger;

//**********************************************************
public class Job
//**********************************************************
{
    public final Actor actor;
    public final Message message;
    public final Logger logger;
    public final Job_termination_reporter termination_reporter; // is optional ie. maybe null

    //**********************************************************
    public Job(Actor actor, Message message, Job_termination_reporter termination_reporter_, Logger logger)
    //**********************************************************
    {
        this.logger = logger;
        if ( actor==null)
        {
            logger.log_stack_trace("FATAL actor cannot be null");
        }
        if ( message==null)
        {
            logger.log_stack_trace("FATAL error_message cannot be null");
        }
        if ( message.get_aborter()==null)
        {
            logger.log_stack_trace("FATAL error_message.Aborter cannot be null");
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
    public void has_failed(String message)
    //**********************************************************
    {
        if (termination_reporter !=null) termination_reporter.has_ended(message,this);
    }

    //**********************************************************
    public void cancel(String msg)
    //**********************************************************
    {
        message.get_aborter().abort(msg+" "+message.to_string());
    }

}
