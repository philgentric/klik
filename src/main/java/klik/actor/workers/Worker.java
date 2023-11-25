package klik.actor.workers;

import klik.actor.Aborter;
import klik.actor.Job;
import klik.util.Logger;
import klik.util.Stack_trace_getter;
import klik.util.Threads;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

//**********************************************************
public class Worker
//**********************************************************
{
    // a worker (aka runner) is owner of ONE thread and uses it to run actor-message pairs from the engine queue
    LinkedBlockingQueue<Job> engine_input_queue;
    Logger logger;
    String name;
    private final Aborter aborter = new Aborter();
    //**********************************************************
    public Worker(String name_, LinkedBlockingQueue<Job> input_queue_, Logger logger_)
    //**********************************************************
    {
        engine_input_queue = input_queue_;
        logger = logger_;
        name = name_;
    }

    //**********************************************************
    public void start()
    //**********************************************************
    {
        Runnable r = () -> {
            for(;;)
            {
                try
                {
                    Job job = engine_input_queue.poll(10, TimeUnit.SECONDS);
                    if (job == null)
                    {
                        if (aborter.should_abort())
                        {
                            logger.log("Worker "+name+" aborted");
                            return;
                        }
                        continue;
                    }
                    if (job.actor == null)
                    {
                        logger.log("BAD BAD null actor in message :"+job.to_string());
                        continue;
                    }
                    String msg = job.actor.run(job.message);
                    if ( job.termination_reporter != null) job.termination_reporter.has_ended(msg, job);
                }
                catch (InterruptedException e) {
                    logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
                }
            }

        };
        Threads.execute(r,logger);
    }

    public void stop()
    {
        aborter.abort();
        logger.log("Worker "+name+" abort requested");

    }

}
