package klik.actor;

// this enables to have a different method called per job
// instead of adding it in the actor code when the job is finished
// which may involve ugly tests about the job nature etc...

//**********************************************************
public interface Job_termination_reporter
//**********************************************************
{
    void has_ended(String message, Job job);
    default void has_failed(String message, Job job)
    {
        job.logger.log("thread job has failed "+message+" "+job.to_string());
    }
}
