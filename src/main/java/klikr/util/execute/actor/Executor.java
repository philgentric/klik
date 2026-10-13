// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.util.execute.actor;

import klikr.System_info;
import klikr.util.log.Logger;

import java.util.concurrent.*;

//**********************************************************
public class Executor
//**********************************************************
{
    // if someone wants to try going back to an old JDK
    // that does not have virtual threads, one can disable here
    public static final boolean use_virtual_threads = true;


    // make this true ONLY if you have problems tracing an exception
    // in a thread, because there is a compute-expensive operation involved
    // see below
    public static final boolean full_thread_stack_dbg = true;
    private static boolean warning_issued  = false;




    public static ExecutorService executor;

    // used only for non-virtual threads
    private final static int pool_max = 500;
    private static LinkedBlockingQueue<Runnable> lbq;

    //**********************************************************
    private static <T extends Throwable> RuntimeException My_Throw(Throwable t) throws T
    //**********************************************************
    {
        throw (T)t;
    }

    // do NOT use this, it is RESERVED for Actor_engines
    // with the nice side effect of accountability i.e. can count threads
    // and list jobs "with names"
    //**********************************************************
    public static void execute(My_Runnable r, String thread_name, Logger logger)
    //**********************************************************
    {
        if (use_virtual_threads)
        {
            Throwable where_the_thread_was_started_from;
            if ( full_thread_stack_dbg)
            {
                if ( !warning_issued)
                {
                    warning_issued = true;
                    logger.log(Logger.warning+" full_thread_stack_dbg is enabled");
                }
                // this is the expensive operation up to 10 microseconds
                // not much... but in some use cases Klikr starts thousands
                // of virtual threads ...
                // creating a Throwable records the call stack BEFORE launching the thread
                where_the_thread_was_started_from = new Throwable("started from here");
            }
            else
            {
                where_the_thread_was_started_from = null;
            }

            // create the thread but don't start it yet
            Thread t = Thread.ofVirtual().name(thread_name).unstarted(
                    ()->{
                        try
                        {
                            r.run();
                        }
                        catch(Throwable throwable)
                        {
                            if ( where_the_thread_was_started_from ==null)
                            {
                                logger.log_with_stack_trace_from_throwable("", throwable);
                            }
                            else {
                                // if available add the full stack trace
                                // that was recorded BEFORE the thread was started
                                throwable.addSuppressed(where_the_thread_was_started_from);
                                throw My_Throw(throwable);
                            }
                        }
                    });
            // because we need to add the UncaughtExceptionHandler
            t.setUncaughtExceptionHandler((thread, e) ->
                {
                logger.log_with_stack_trace_from_throwable(Logger.error + "full stack trace of exception:\n",e);
                }
            );
            t.start();

            return;
        }

        /*
        // good old worker thread pool
        try
        {
            executor.execute(r);
        }
        catch (RejectedExecutionException e)
        {
            logger.log_with_stack_trace_from_throwable("WARNING: execute failed ",e);
        }

         */
    }


    //**********************************************************
    private static void init(Logger logger)
    //**********************************************************
    {
        if (use_virtual_threads)
        {
            logger.log("Using virtual threads");
            executor = null;
            // not using
            // executor = Executors.newVirtualThreadPerTaskExecutor();
            // as it allows to give a name to each thread
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
