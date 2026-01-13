// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.util.execute.actor;

import klikr.System_info;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;

import java.util.concurrent.*;

//**********************************************************
public class Executor
//**********************************************************
{
    // if someone wants to try going back to an old JDK
    // that does not have virtual threads, one can disable here
    public static final boolean use_virtual_threads = true;
    public static ExecutorService executor;

    // used only for non-virtual threads
    private final static int pool_max = 500;
    private static LinkedBlockingQueue<Runnable> lbq;

    // do NOT use this, it is RESERVED for Actor_engines
    // with the nice side effect of accountability i.e. can count threads
    // and list jobs "with names"
    //**********************************************************
    public static void execute(Runnable r, Logger logger)
    //**********************************************************
    {
        if (executor == null) init(logger);
        try
        {
            executor.execute(r);
        }
        catch (RejectedExecutionException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace("WARNING: execute failed "+e));
        }
    }


    //**********************************************************
    private static void init(Logger logger)
    //**********************************************************
    {
        if (use_virtual_threads)
        {
            logger.log("Using virtual threads");
            executor = Executors.newVirtualThreadPerTaskExecutor();
        }
        else
        {
            int n_threads = 10 * System_info.how_many_cores();
            logger.log("using a pool of "+n_threads+" 'classic' threads, with a max of "+pool_max);
            lbq = new LinkedBlockingQueue<>();
            executor = new ThreadPoolExecutor(n_threads, pool_max, 100, TimeUnit.SECONDS, lbq);
        }
    }
}
