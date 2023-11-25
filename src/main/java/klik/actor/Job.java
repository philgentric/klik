package klik.actor;

import java.util.Objects;

//**********************************************************
public class Job
//**********************************************************
{
    public final Actor actor;
    public final Message message;
    public final Job_termination_reporter termination_reporter; // is optional ie. maybe null
    public Thread thread = null; // depending on the engine, job don't get a thread, or late

    //**********************************************************
    public Job(Actor actor, Message message, Job_termination_reporter termination_reporter_)
    //**********************************************************
    {
        Objects.requireNonNull(actor,"FATAL actor cannot be null");
        Objects.requireNonNull(message,"FATAL message cannot be null");
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
        message.get_aborter().abort();
        if ( thread != null) thread.interrupt();
    }
}
