package klik.fusk;

import klik.actor.*;
import klik.util.Logger;
import klik.util.Threads;

import java.io.File;
import java.util.concurrent.ConcurrentLinkedQueue;

//**********************************************************
public class Defusk_actor_for_one_folder implements Actor
//**********************************************************
{
    private final Aborter aborter;
    public final Logger logger;
    ConcurrentLinkedQueue<Job> jobs = new ConcurrentLinkedQueue<>();

    //**********************************************************
    Defusk_actor_for_one_folder(Aborter aborter_, Logger logger_)
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
        if ( files ==null)
        {
            return null;
        }
        File destination_folder = fm.destination_folder;

        for ( File f : files)
        {
            if (aborter.should_abort())
            {
                abort();
                return null;
            }

            if ( f.isDirectory())
            {
                jobs.add(defusk_this_folder(f,destination_folder,aborter,logger));
            }
            else
            {
                if ( Actor_engine.use_virtual_threads)
                {
                    Runnable r = () -> Fusk_static_core.defusk_file(f.toPath(), destination_folder.toPath(), aborter,logger);
                    Threads.execute(r,logger);
                }
                else {
                    Fusk_static_core.defusk_file(f.toPath(), destination_folder.toPath(), aborter, logger);
                }
            }
        }
        return null;
    }

    //**********************************************************
    public static Job defusk_this_folder(File target_dir, File destination_folder, Aborter aborter, Logger logger)
    //**********************************************************
    {
        return Actor_engine.run(
                new Defusk_actor_for_one_folder(aborter,logger), // need an instance for abort
                new Fusk_message(target_dir, destination_folder),
                null,
                logger);
    }

    //**********************************************************
    public void abort()
    //**********************************************************
    {
        aborter.abort();
        Actor_engine.cancel_all(jobs);
    }

}
