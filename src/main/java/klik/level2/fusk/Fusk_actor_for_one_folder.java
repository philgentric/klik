package klik.level2.fusk;

import klik.actor.*;
import klik.util.Logger;
import klik.util.Threads;

import java.io.File;
import java.util.concurrent.ConcurrentLinkedQueue;

//**********************************************************
public class Fusk_actor_for_one_folder implements Actor
//**********************************************************
{
    private final Aborter aborter;
    public final Logger logger;
    ConcurrentLinkedQueue<Job> jobs = new ConcurrentLinkedQueue<>();

    //**********************************************************
    Fusk_actor_for_one_folder(Aborter aborter_,Logger logger_)
    //**********************************************************
    {
        aborter = aborter_;
        logger = logger_;
    }


    //**********************************************************
    @Override
    public String run(Message m)
    //**********************************************************
    {
        Fusk_message fm = (Fusk_message)m;
        File[] files = fm.target_dir.listFiles();
        if ( files ==null) return null;
        File destination_folder = fm.destination_folder;
        if ( !destination_folder.exists())
        {
            if (destination_folder.mkdir())
            {
                logger.log("created folder: " + destination_folder);
            }
            else
            {
                logger.log("FATAL ! could not create folder: " + destination_folder);
                return "FATAL ! could not create folder: " + destination_folder;
            }
        }
        for ( File f : files)
        {
            if ( aborter.should_abort()) return null;
            if ( f.isDirectory())
            {
                // generate a new actor job
                jobs.add(fusk_this_folder(f,destination_folder, aborter,logger));
            }
            else
            {
                if ( Actor_engine.use_virtual_threads)
                {
                    Runnable r = () -> Fusk_static_core.fusk_file(f.toPath(), destination_folder.toPath(), logger);
                    Threads.execute(r,logger);
                }
                else {
                    Fusk_static_core.fusk_file(f.toPath(), destination_folder.toPath(), logger);
                }
            }
        }
        return null;
    }

    //**********************************************************
    public static Job fusk_this_folder(File target_dir, File destination_folder, Aborter aborter, Logger logger)
    //**********************************************************
    {
        logger.log("going to fusk:"+target_dir);
        return Actor_engine.run(
                new Fusk_actor_for_one_folder(aborter,logger),
                new Fusk_message(target_dir,new File(destination_folder, target_dir.getName())),
                null,
                logger);
    }

    //**********************************************************
    public void abort()
    //**********************************************************
    {
        aborter.abort();
        Actor_engine.get(logger).cancel_all(jobs);
    }
}