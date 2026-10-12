// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.util.execute.actor.workers;

import klikr.util.Kontext;
import klikr.util.execute.actor.*;
import klikr.util.log.Logger;

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
    private final Kontext context;
    private Job worker_job;
    private final String name;
    //**********************************************************
    public Worker(String name, LinkedBlockingQueue<Job> input_queue, Kontext context)
    //**********************************************************
    {
        this.name = name;
        this.engine_input_queue = input_queue;
        this.context = context;
    }

    //**********************************************************
    public void start()
    //**********************************************************
    {
        My_Runnable r = () -> {
            for(;;)
            {
                try
                {
                    Job job = engine_input_queue.poll(10, TimeUnit.SECONDS);
                    if (job == null)
                    {
                        if (context.should_abort())
                        {
                            if ( dbg) context.log("Worker "+name+" stops");
                            // accounting for the WORKER's own thread
                            Actor_engine.threads_in_flight.decrementAndGet();
                            Actor_engine.jobs_in_flight.remove(worker_job);
                            return;
                        }
                        continue;
                    }
                    if (job.actor == null)
                    {
                        context.log(Logger.error+"BAD BAD null actor in error_message :"+job.to_string());
                        continue;
                    }
                    String msg = job.actor.run(job.message);
                    if ( job.termination_reporter != null) job.termination_reporter.has_ended(msg, job);
                }
                catch (InterruptedException e) {
                    context.log_with_stack_trace_from_throwable(e.toString(),e);
                    ;
                }
            }

        };
        Executor.execute(r, name, context.logger());
        // accounting for the worker thread:
        Actor_engine.threads_in_flight.incrementAndGet();
        worker_job = new Job(get_dummy_actor(name), get_dummy_message(),null,context.logger());
        Actor_engine.jobs_in_flight.add(worker_job); // dummy job to count the worker thread

    }

    //**********************************************************
    private Message get_dummy_message()
    //**********************************************************
    {
        return new Message() {
            @Override
            public String thread_name() {
                return "worker thread for "+name;
            }

            @Override
            public Aborter get_aborter() {
                return new Aborter("thread accounting of "+name,context.logger());
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
        context.abort("Worker "+name+" shall stop");
        if ( dbg) context.log(Logger.ok+" Worker "+name+" stop requested");

    }

}
