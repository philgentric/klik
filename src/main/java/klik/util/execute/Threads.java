//SOURCES ./My_action.java
package klik.util.execute;

import klik.System_info;
import klik.util.log.Logger;
import klik.util.log.Logger_factory;
import klik.util.log.Stack_trace_getter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

//**********************************************************
public class Threads
//**********************************************************
{
    public static final boolean use_virtual_threads = true;
    private static LinkedBlockingQueue<Runnable> lbq;
    private static ExecutorService executor;
    private final static int pool_max = 500;


    //**********************************************************
    public static ExecutorService get_executor_service(Logger logger)
    //**********************************************************
    {
        if (executor == null) init(logger);
        return executor;
    }

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
            logger.log(Stack_trace_getter.get_stack_trace("WARNING: thread not started"+e));
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
            //logger.log("FATAL: cannot use virtual threads");
        }
        else
        {
            int n_threads = 10 * System_info.how_many_cores();
            logger.log("using a pool of "+n_threads+" 'classic' threads, with a max of "+pool_max);
            lbq = new LinkedBlockingQueue<>();
            executor = new ThreadPoolExecutor(n_threads, pool_max, 100, TimeUnit.SECONDS, lbq);
        }
    }


    //**********************************************************
    public static <V> Future<V> submit(Callable<V> c, Logger logger)
    //**********************************************************
    {
        if (executor == null) init(logger);
        return executor.submit(c);
    }

    //**********************************************************
    public static <V> void execute_all(Collection<Callable<V>> callables, My_action<V> action, Logger logger)
    //**********************************************************
    {
        if (executor == null) init(logger);
        CompletionService<V> executor_local = new ExecutorCompletionService<>(executor);
        int n = callables.size();
        List<Future<V>> futures = new ArrayList<Future<V>>(n);
        try {
            for (Callable<V> s : callables) {
                futures.add(executor_local.submit(s));
            }
            for (int i = 0; i < n; ++i) {
                try {
                    V r = executor_local.take().get();
                    if (r != null) {
                        action.use(r);
                    }
                } catch (ExecutionException e) {
                    logger.log("exception in execute_all: " + e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        finally {
            for (Future<V> f : futures)
                f.cancel(true);
        }
    }

    public static void main(String args[])
    {
        Logger logger = Logger_factory.get("Threads test");

        class My_result
        {
            public final String s;

            My_result(String s) {
                this.s = s;
            }
        }
        Random r = new Random();
        Collection<Callable<My_result>> callables = new ArrayList<>();
        for (int i = 0; i < 10; i++)
        {
            final int final_i = i;
            callables.add(() -> {
                Thread.sleep(r.nextInt(1000));
                return new My_result("result "+final_i);
            });
        }

        My_action<My_result> action = new My_action<My_result>() {
            @Override
            public void use(My_result result) {
                logger.log("result: "+result.s);
            }
        };
        execute_all(callables, action, logger);


    }
}
