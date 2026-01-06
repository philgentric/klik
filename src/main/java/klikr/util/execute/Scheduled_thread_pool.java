// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.util.execute;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

//**********************************************************
public class Scheduled_thread_pool
//**********************************************************
{
    private static ScheduledExecutorService executor;

    //**********************************************************
    public static ScheduledFuture<?>  execute(Runnable r, long delay, TimeUnit tu)
    //**********************************************************
    {
        if (executor == null) create_executor();
        return executor.scheduleWithFixedDelay(r,delay,delay,tu);
    }


    //**********************************************************
    private static void create_executor()
    //**********************************************************
    {
        executor = Executors.newScheduledThreadPool(40);
    }

}
