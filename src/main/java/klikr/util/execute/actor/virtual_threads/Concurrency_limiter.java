// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.util.execute.actor.virtual_threads;

import klikr.System_info;
import klikr.util.log.Logger;

import java.util.concurrent.Semaphore;

//**********************************************************
public class Concurrency_limiter
//**********************************************************
{
    // even when using virtual threads, in some cases,
    // one must make sure not too many threads are running at the same time
    // an alternative is to use Actor_engine_based_on_workers

    private final Semaphore concurrency_limiter;

    /*
    the reason is:
    - With a Worker-based actor engine, we have a fixed set of N workers
    (N normally being "reasonable" count) with 1 worker = 1 thread
    when submitting a job to the engine, it is QUEUED,
    the N workers are pulling on that queue,
    so we have max N jobs executed in parallel
    - with virtual threads the actor engine just instantly starts one thread per submitted actor-job
    ... so there is virtually no limit to the number of threads that could try to execute in parallel
    for example,
    (a) operations on servers :
    if one has 1000 http calls to make (to get feature vectors)
    it is not a good idea to make them all at the same time, especially with
    python servers that are not too good at queuing ...
    (b) operations on files, like generating many animated gifs from a movie:
    1hour movie, 5s per gif ==> 720 files to create
    in parallel ? the pressure on the file system would be too large
    */

    //**********************************************************
    public Concurrency_limiter(String origin, double max_number_of_threads_per_core, Logger logger)
    //**********************************************************
    {
        int permits = (int)(max_number_of_threads_per_core*(double)(System_info.how_many_cores()));
        if ( permits < 2) permits = 2;
        //logger.log(Stack_trace_getter.get_stack_trace("CREATING NEW Concurrency_limiter for:"+origin+" has "+permits+" permits"));
        concurrency_limiter = new Semaphore(permits);
    }

    //**********************************************************
    public void acquire() throws InterruptedException
    //**********************************************************
    {
        concurrency_limiter.acquire();
    }

    //**********************************************************
    public void release() {
        concurrency_limiter.release();
    }
}
