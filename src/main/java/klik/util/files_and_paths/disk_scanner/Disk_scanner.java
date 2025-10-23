package klik.util.files_and_paths.disk_scanner;

import klik.util.execute.actor.Aborter;
import klik.util.execute.actor.Actor_engine;
import klik.browser.icons.Error_type;
import klik.util.execute.actor.Executor;
import klik.util.files_and_paths.Static_files_and_paths_utilities;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

//**********************************************************
public class Disk_scanner implements Runnable
//**********************************************************
{
    private static final boolean dbg = false;
    final Path path;
    final String origin;
    final File_payload file_payload;
    final Dir_payload dir_payload;
    final ConcurrentLinkedQueue<String> warning_payload;

    public final AtomicLong file_count_stop_counter;
    public final AtomicInteger folder_count_stop_counter;

    public final Aborter aborter;
    public final Logger logger;

    // this will BLOCK until the tree has been traversed
    //**********************************************************
    public static void process_folder(
            Path path,
            String origin,
            File_payload file_payload_,
            Dir_payload dir_payload_,
            ConcurrentLinkedQueue<String> warning_payload_,
            Aborter aborter,
            Logger logger)
    //**********************************************************
    {
        if ( !path.toFile().isDirectory())
        {
            logger.log(Stack_trace_getter.get_stack_trace(origin+" stupid: not a folder "+path));
            return;
        }
        if (Files.isSymbolicLink(path))
        {
            logger.log(origin+" WARNING: Disk_scanner not going down symbolic link for folder: "+path);
            return;
        }
        else
        {
            if( dbg) logger.log(origin+" Disk_scanner going down (not a symbolic links) on folder: "+path);
        }
        long start = 0L;
        if (dbg) start = System.currentTimeMillis();
        AtomicInteger folder_count_stop_counter = new AtomicInteger(0);
        AtomicLong file_count_stop_counter = new AtomicLong(0L);
        launch_folder_in_a_thread_(
                path,
                origin,
                file_count_stop_counter,
                folder_count_stop_counter,
                file_payload_,
                dir_payload_,
                warning_payload_,
                aborter,
                logger);

        // blocking part: we are sleeping/waiting until the tree is fully processed
        // this is a race condition if the jobs in the threads make the folder count go to zero, while the job is not yet finished
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            logger.log_stack_trace(origin+e.toString());
        }
        for(;;)
        {
            if (aborter.should_abort())
            {
                //logger.log("ABORTED: Disk_scanner monitoring for "+path);
                return;
            }
            try {
                Thread.sleep(10+folder_count_stop_counter.get());
                //logger.log("how_many_folders="+how_many_folders);
            } catch (InterruptedException e) {
                logger.log_stack_trace(origin+e.toString());
            }
            if (( folder_count_stop_counter.get() == 0) &&( file_count_stop_counter.get() == 0) )break;

        }
        if (dbg)
        {
            long end = System.currentTimeMillis();
            logger.log(origin+" file tree processing time: "+(end-start)+"ms");
        }

    }
    //**********************************************************
    private static void launch_folder_in_a_thread_(
            Path path,
            String origin,
            AtomicLong file_count_stop_counter,
            AtomicInteger folder_count_stop_counter,
            File_payload file_payload_,
            Dir_payload dir_payload_,
            ConcurrentLinkedQueue<String> warning_payload_,
            Aborter aborter_,
            Logger logger)
    //**********************************************************
    {
        folder_count_stop_counter.incrementAndGet();

        Runnable r = new Disk_scanner(path, origin, file_count_stop_counter, folder_count_stop_counter, file_payload_, dir_payload_, warning_payload_, aborter_, logger);
        Actor_engine.execute(r,"Disk scanner, scan folder: "+path,logger);
    }
    //**********************************************************
    private Disk_scanner(
            Path path_,
            String origin_,
            AtomicLong file_count_stop_counter_,
            AtomicInteger folder_count_stop_counter_,
            File_payload file_payload_,
            Dir_payload dir_payload_,
            ConcurrentLinkedQueue<String> warning_payload_,
            Aborter aborter_,
            Logger logger_)
    //**********************************************************
    {
        path = path_;
        origin = origin_;
        file_payload = file_payload_;
        dir_payload = dir_payload_;
        warning_payload = warning_payload_;
        file_count_stop_counter = file_count_stop_counter_;
        folder_count_stop_counter = folder_count_stop_counter_;
        logger = logger_;
        if ( aborter_ == null)
        {
            logger.log_stack_trace(origin+ " FATAL: aborter must not be null");
        }
        aborter = aborter_;
    }


    //**********************************************************
    @Override
    public void run()
    //**********************************************************
    {
        if (aborter.should_abort())
        {
            //logger.log("ABORTED1: Disk_scanner for "+path);
            return;
        }
        File[] all_files = path.toFile().listFiles();
        if ( all_files == null)
        {
            {
                logger.log (origin+ " Disk_scanner: listFiles() returns null for: "+path);
                Error_type error = Static_files_and_paths_utilities.explain_error(path,logger);
            }

            folder_count_stop_counter.decrementAndGet();
            return ;
        }
        for (File f : all_files)
        {
            if (aborter.should_abort())
            {
                //logger.log("ABORTED2: Disk_scanner for "+path);
                break;
            }
            if (f.isDirectory())
            {
                if ( Files.isSymbolicLink(f.toPath()))
                {
                    String x = origin+" warning: disk scanner not following symbolic link folder:" + f;
                    if ( dbg) logger.log(x);
                    if ( warning_payload!=null) warning_payload.add(x);
                }
                else
                {
                    if ( dir_payload != null) dir_payload.process_dir(f);
                    launch_folder_in_a_thread_(f.toPath(), origin, file_count_stop_counter, folder_count_stop_counter, file_payload, dir_payload, warning_payload, aborter, logger);
                }
            }
            else
            {
                if ( file_payload!= null)
                {
                    file_count_stop_counter.incrementAndGet();
                    if ((Executor.use_virtual_threads))
                    {
                        // with virtual threads we can afford to start one thread per file !
                        Actor_engine.execute(() -> file_payload.process_file(f,file_count_stop_counter), "Disk scanner, scan file: "+path,logger);
                    }
                    else
                    {
                        file_payload.process_file(f, file_count_stop_counter);
                    }
                }
            }
        }
        folder_count_stop_counter.decrementAndGet();
    }




}
