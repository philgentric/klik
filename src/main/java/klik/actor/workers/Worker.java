package klik.actor.workers;

import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.actor.Job;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import klik.util.execute.Threads;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

//**********************************************************
public class Worker
//**********************************************************
{
    // a worker (aka runner) is owner of ONE thread and uses it to run actor-error_message pairs from the engine queue
    LinkedBlockingQueue<Job> engine_input_queue;
    Logger logger;
    String name;
    private final Aborter aborter;
    private Job worker_job;

    //**********************************************************
    public Worker(String name_, LinkedBlockingQueue<Job> input_queue_, Logger logger_)
    //**********************************************************
    {
        engine_input_queue = input_queue_;
        logger = logger_;
        name = name_;
        this.aborter = new Aborter("Worker abort",logger);
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
                            // this is the worker thread:
                            Actor_engine.threads_in_flight.decrementAndGet();
                            return;
                        }
                        continue;
                    }
                    if (job.actor == null)
                    {
                        logger.log("BAD BAD null actor in error_message :"+job.to_string());
                        continue;
                    }
                    Actor_engine.threads_in_flight.incrementAndGet();
                    String msg = job.actor.run(job.message);
                    if ( job.termination_reporter != null) job.termination_reporter.has_ended(msg, job);
                    Actor_engine.threads_in_flight.decrementAndGet();
                    Actor_engine.jobs_in_flight.remove(worker_job);
                }
                catch (InterruptedException e) {
                    logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
                }
            }

        };
        Threads.execute(r,logger);
        // this is the worker thread:
        Actor_engine.threads_in_flight.incrementAndGet();
        worker_job = new Job(null,null,null,logger);
        Actor_engine.jobs_in_flight.add(worker_job); // dummy job to count the worker thread

    }

    //**********************************************************
    public void stop()
    //**********************************************************
    {
        aborter.abort("Worker "+name+" stop");
        logger.log("Worker "+name+" aborted");

    }

}
