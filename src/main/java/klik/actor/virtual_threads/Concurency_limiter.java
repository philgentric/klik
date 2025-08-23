package klik.actor.virtual_threads;

import klik.System_info;
import klik.util.log.Logger;

import java.util.concurrent.Semaphore;

public class Concurency_limiter
{
    // it some use cases, it may be useful to
    // make sure not too many threads are running at the same time when using virtual threads
    // however, a good alternative is to select a worker-based Actor engine

    private final Semaphore concurrency_limiter;

    /*
    the reason is:
    - with OS threads (worker-based actor engine) we have a fixed set of N workers
    (N normally being "reasonable" count) using a thread pool T >~ N
    when submitting a job to the engine, it is QUEUED, and the N workers are pulling on that queue,
    so we have max N jobs executed in parallel
    - with virtual threads the actor engine just instantly starts one thread per submitted actor-job
    ... so there is no limit to the number of threads that could try to execute in parallel
    ... and it can be a huge number
    for example, the number of animated gifs we are making at the same time,
    when making 600 x 5 seconds animated gifs from a movie
    (virtual threads are cheap but the gif-making process is not cheap!)
    */

    public Concurency_limiter(String origin, double max_number_of_threads_per_core, Logger logger)
    {
        int permits = (int)(max_number_of_threads_per_core*(double)(System_info.how_many_cores()));
        if ( permits < 2) permits = 2;
        //logger.log(Stack_trace_getter.get_stack_trace("CREATING NEW Concurency_limiter for:"+origin+" has "+permits+" permits"));
        concurrency_limiter = new Semaphore(permits);
    }

    public void acquire() throws InterruptedException {
        concurrency_limiter.acquire();
    }

    public void release() {
        concurrency_limiter.release();
    }
}
