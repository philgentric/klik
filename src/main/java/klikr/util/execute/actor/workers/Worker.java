// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.util.execute.actor.workers;

import klikr.util.execute.actor.*;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

//**********************************************************
public class Worker
//**********************************************************
{
    // a worker (aka runner) is owner of ONE thread
    // and uses it to run Jobs
    // extracted from the engine queue
    private static final boolean dbg = false;
    public final LinkedBlockingQueue<Job> engine_input_queue;
    public final Logger logger;
    public final String name;
    private final Aborter cleanup_aborter;
    private Job worker_job;

    //**********************************************************
    public Worker(String name, LinkedBlockingQueue<Job> input_queue, Aborter cleanup_aborter, Logger logger)
    //**********************************************************
    {
        this.name = name;
        this.engine_input_queue = input_queue;
        this.cleanup_aborter = cleanup_aborter;
        this.logger = logger;
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
                        if (cleanup_aborter.should_abort())
                        {
                            if ( dbg) logger.log("Worker "+name+" stops");
                            // accounting for the WORKER's own thread
                            Actor_engine.threads_in_flight.decrementAndGet();
                            Actor_engine.jobs_in_flight.remove(worker_job);
                            return;
                        }
                        continue;
                    }
                    if (job.actor == null)
                    {
                        logger.log("❌ BAD BAD null actor in error_message :"+job.to_string());
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
        Executor.execute(r,logger);
        // accounting for the worker thread:
        Actor_engine.threads_in_flight.incrementAndGet();
        worker_job = new Job(get_dummy_actor(name), get_dummy_message(),null,logger);
        Actor_engine.jobs_in_flight.add(worker_job); // dummy job to count the worker thread

    }

    //**********************************************************
    private Message get_dummy_message()
    //**********************************************************
    {
        return new Message() {
            @Override
            public String to_string() {
                return "worker thread for "+name;
            }

            @Override
            public Aborter get_aborter() {
                return new Aborter("thread accounting of "+name,logger);
            }
        };
    }

    //**********************************************************
    private Actor get_dummy_actor(String name)
    //**********************************************************
    {
        return new Actor() {
            @Override
            public String run(Message m) {
                return "worker thread for "+name;
            }

            @Override
            public String name() {
                return "";
            }
        };
    }

    //**********************************************************
    public void stop()
    //**********************************************************
    {
        cleanup_aborter.abort("Worker "+name+" shall stop");
        if ( dbg) logger.log("✅ Worker "+name+" stop requested");

    }

}
