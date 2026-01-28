// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ./Actor_engine_interface.java
//SOURCES ./virtual_threads/Actor_engine_with_virtual_threads.java
//SOURCES ./workers/Actor_engine_based_on_workers.java
//SOURCES ../util/execute/Executor.java
package klikr.util.execute.actor;

import javafx.application.Platform;
import klikr.util.cache.Cache_folder;
import klikr.util.execute.actor.virtual_threads.Actor_engine_with_virtual_threads;
import klikr.util.files_and_paths.Static_files_and_paths_utilities;
import klikr.util.log.Logger;
import klikr.util.mmap.Mmap;
import klikr.util.ui.Text_frame;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/*
there are 2 ways to execute something (on a thread)
- run: passing an Actor (which defines the code) and a Message (which defines the parameters)
- execute : passing a Runnable (code without parameters)
both return a Job that can be used for cancellation
 */
//**********************************************************
public class Actor_engine // is a singleton
//**********************************************************
{
    public static final boolean cancel_dbg = false;
    private static volatile Actor_engine_interface instance;

    // accounting:
    public static final AtomicInteger threads_in_flight = new AtomicInteger(0);
    public static LinkedBlockingQueue<Job> jobs_in_flight = new LinkedBlockingQueue<>();
    //public static int recent_max_threads = 0;

    //**********************************************************
    public static Actor_engine_interface get_instance()
    //**********************************************************
    {
        return instance;
    }


    //**********************************************************
    public static Job run(Actor actor, Message message, Job_termination_reporter tr, Logger logger)
    //**********************************************************
    {
        if (instance == null)
        {
            synchronized (Actor_engine.class)
            {
                if (instance == null)
                {
                    instance = create(logger);
                }
            }
        }
        return instance.run(actor,message,tr,logger);
    }

    //**********************************************************
    public static Job execute(Runnable r, String id, Logger logger)
    //**********************************************************
    {
        if ( instance == null) instance = create(logger);
        return instance.execute_internal(r, id,logger);
    }
    //**********************************************************
    public static void cancel_jobs(ConcurrentLinkedQueue<Job> jobs)
    //**********************************************************
    {
        if ( instance == null) return;
        instance.cancel_jobs(jobs);
    }

    //**********************************************************
    public static void list_jobs(Logger logger)
    //**********************************************************
    {
        List<String> job_list = new ArrayList<>();
        for ( Job job : jobs_in_flight)
        {
            logger.log("Job: "+job.to_string());
            job_list.add(job.to_string());
        }
        Platform.runLater(()-> Text_frame.show(job_list, logger));
    }
    //**********************************************************
    public static int how_many_threads_are_in_flight(Logger logger)
    //**********************************************************
    {
        return threads_in_flight.get();
    }


    //**********************************************************
    private static Actor_engine_interface create(Logger logger)
    //**********************************************************
    {
        if (instance == null)
        {
            synchronized (Actor_engine_interface.class)
            {
                if (instance == null)
                {
                    instance = new Actor_engine_with_virtual_threads(logger);
                }
            }
        }
        return instance;
    }




    /*

//**********************************************************
    public static int recent_max_threads_in_flight(Logger logger)
    //**********************************************************
    {
        if ( instance == null) return 0;
        return recent_max_threads;
    }


    //**********************************************************
    public static void cancel_job(Job job)
    //**********************************************************
    {
        if ( instance == null) return;
        instance.cancel_job(job);
    }
    */

}
