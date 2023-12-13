package klik.util;

import java.util.concurrent.*;

//**********************************************************
public class Threads
//**********************************************************
{
    public static final boolean use_fibers = false;
    private static LinkedBlockingQueue<Runnable> lbq;
    private static ExecutorService executor;
    private final static int pool_max = 500;


    //**********************************************************
    public static void execute(Runnable r, Logger logger)
    //**********************************************************
    {
        if (executor == null) init(logger);
        executor.execute(r);
    }

    //**********************************************************
    private static void init(Logger logger)
    //**********************************************************
    {
        if (use_fibers)
        {
            logger.log("using fibers aka virtual threads");
            executor = Executors.newVirtualThreadPerTaskExecutor();
        }
        else
        {
            int n_threads = 10 * Runtime.getRuntime().availableProcessors();
            logger.log("using a pool of "+n_threads+" threads, with a max of "+pool_max);
            lbq = new LinkedBlockingQueue<>();
            executor = new ThreadPoolExecutor(n_threads, pool_max, 100, TimeUnit.SECONDS, lbq);
        }
    }


}
