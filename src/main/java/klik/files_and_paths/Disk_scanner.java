package klik.files_and_paths;

import klik.actor.Aborter;
import klik.actor.virtual_threads.Concurency_limiter;
import klik.browser.icons.Error_type;
import klik.util.Logger;
import klik.util.Stack_trace_getter;
import klik.util.System_out_logger;
import klik.util.Threads;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

//**********************************************************
public class Disk_scanner implements Runnable
//**********************************************************
{
    private static final boolean verbose = false;
    final Path path;
    final File_payload file_payload;
    final Dir_payload dir_payload;
    public final AtomicInteger folder_count_stop_counter;
    public final Aborter aborter;
    public final Logger logger;
    private final static Concurency_limiter limiter = new Concurency_limiter("Disk scanner",0.5,new System_out_logger());


    // this will BLOCK until the tree has been traversed
    //**********************************************************
    public static void process_folder(
            Path path,
            File_payload file_payload_,
            Dir_payload dir_payload_,
            Aborter aborter,
            Logger logger)
    //**********************************************************
    {
        if ( !path.toFile().isDirectory())
        {
            logger.log(Stack_trace_getter.get_stack_trace("stupid: not a folder "+path));
            return;
        }
        long start = System.currentTimeMillis();
        AtomicInteger folder_count_stop_counter = new AtomicInteger(0);
        launch_folder_in_a_thread_(
                path,
                folder_count_stop_counter,
                file_payload_,
                dir_payload_,
                aborter,
                logger);

        // blocking part: we are sleep/waiting until the tree is fully processed
        // using one thread per sub folder
        for(;;)
        {
            if (aborter.should_abort())
            {
                //logger.log("ABORTED3: Disk_scanner monitoring for "+path);
                return;
            }
            try {
                Thread.sleep(10);
                //logger.log("how_many_folders="+how_many_folders);
            } catch (InterruptedException e) {
                logger.log(e.toString());
            }
            if ( folder_count_stop_counter.get() == 0) break;

        }
        long end = System.currentTimeMillis();
        if (verbose) logger.log("file tree processing time: "+(end-start)+"ms");

    }
    //**********************************************************
    private static void launch_folder_in_a_thread_(
            Path path,
            AtomicInteger folder_count_stop_counter,
            File_payload file_payload_,
            Dir_payload dir_payload_,
            Aborter aborter_,
            Logger logger)
    //**********************************************************
    {
        folder_count_stop_counter.incrementAndGet();

        Runnable r = new Disk_scanner(path, folder_count_stop_counter, file_payload_, dir_payload_, aborter_, logger);
        Threads.execute(r,logger);
    }
    //**********************************************************
    private Disk_scanner(
            Path path_,
            AtomicInteger folder_count_stop_counter_,
            File_payload file_payload_,
            Dir_payload dir_payload_,
            Aborter aborter_,
            Logger logger_)
    //**********************************************************
    {
        path = path_;
        file_payload = file_payload_;
        dir_payload = dir_payload_;
        folder_count_stop_counter = folder_count_stop_counter_;
        aborter = Objects.requireNonNull(aborter_);
        logger = logger_;
    }


    //**********************************************************
    @Override
    public void run()
    //**********************************************************
    {
        try {
            limiter.acquire();
        } catch (InterruptedException e) {
            logger.log_exception(Stack_trace_getter.get_stack_trace("Disk scanner"),e);
            return;
        }
        if (aborter.should_abort())
        {
            //logger.log("ABORTED1: Disk_scanner for "+path);
            limiter.release();
            return;
        }
        File[] all_files = path.toFile().listFiles();
        if ( all_files == null)
        {
            Error_type error = Files_and_Paths.explain_error(path,logger);
            logger.log(error+ " Disk_scanner: cannot scan folder "+path);

            folder_count_stop_counter.decrementAndGet();
            limiter.release();
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
                if ( dir_payload != null) dir_payload.process_dir(f);
                launch_folder_in_a_thread_(f.toPath(), folder_count_stop_counter, file_payload, dir_payload, aborter, logger);
            }
            else
            {
                if ( file_payload!= null) file_payload.process_file(f);
            }
        }
        folder_count_stop_counter.decrementAndGet();
        limiter.release();
    }


}
