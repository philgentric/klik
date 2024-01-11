package klik.files_and_paths;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import klik.actor.Aborter;
import klik.actor.virtual_threads.Concurency_limiter;
import klik.browser.Browser;
import klik.browser.icons.Error_type;
import klik.browser.items.Item_button;
import klik.look.Font_size;
import klik.look.my_i18n.I18n;
import klik.util.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

//**********************************************************
public class Disk_scanner implements Runnable
//**********************************************************
{
    private static final boolean verbose = false;
    final Path path;
    final File_payload file_payload;
    final Dir_payload dir_payload;
    final ConcurrentLinkedQueue<String> warning_payload;

    public final AtomicInteger folder_count_stop_counter;
    public final Aborter aborter;
    public final Logger logger;
    //private final static Concurency_limiter limiter = new Concurency_limiter("Disk scanner",2,new System_out_logger());


    // this will BLOCK until the tree has been traversed
    //**********************************************************
    public static void process_folder(
            Path path,
            File_payload file_payload_,
            Dir_payload dir_payload_,
            ConcurrentLinkedQueue<String> warning_payload_,
            Aborter aborter,
            Logger logger)
    //**********************************************************
    {
        if ( !path.toFile().isDirectory())
        {
            logger.log(Stack_trace_getter.get_stack_trace("stupid: not a folder "+path));
            return;
        }
        if (Files.isSymbolicLink(path))
        {
            logger.log("WARNING: Disk_scanner not going down symbolic link for folder: "+path);
            return;
        }
        else
        {
            logger.log("Disk_scanner going down (not a symbolic links) on folder: "+path);
        }
        long start = System.currentTimeMillis();
        AtomicInteger folder_count_stop_counter = new AtomicInteger(0);
        launch_folder_in_a_thread_(
                path,
                folder_count_stop_counter,
                file_payload_,
                dir_payload_,
                warning_payload_,
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
                logger.log_stack_trace(e.toString());
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
            ConcurrentLinkedQueue<String> warning_payload_,
            Aborter aborter_,
            Logger logger)
    //**********************************************************
    {
        folder_count_stop_counter.incrementAndGet();

        Runnable r = new Disk_scanner(path, folder_count_stop_counter, file_payload_, dir_payload_, warning_payload_, aborter_, logger);
        Threads.execute(r,logger);
    }
    //**********************************************************
    private Disk_scanner(
            Path path_,
            AtomicInteger folder_count_stop_counter_,
            File_payload file_payload_,
            Dir_payload dir_payload_,
            ConcurrentLinkedQueue<String> warning_payload_,
            Aborter aborter_,
            Logger logger_)
    //**********************************************************
    {
        path = path_;
        file_payload = file_payload_;
        dir_payload = dir_payload_;
        warning_payload = warning_payload_;
        folder_count_stop_counter = folder_count_stop_counter_;
        logger = logger_;
        if ( aborter_ == null)
        {
            logger.log_stack_trace("FATAL: aborter must not be null");
        }
        aborter = aborter_;
    }


    //**********************************************************
    @Override
    public void run()
    //**********************************************************
    {
        /*
        try {
            limiter.acquire();
        } catch (InterruptedException e) {
            logger.log_exception(Stack_trace_getter.get_stack_trace("Disk scanner"),e);
            return;
        }*/
        if (aborter.should_abort())
        {
            //logger.log("ABORTED1: Disk_scanner for "+path);
           // limiter.release();
            return;
        }
        File[] all_files = path.toFile().listFiles();
        if ( all_files == null)
        {
            logger.log (" Disk_scanner: listFiles() returns null for: "+path);
            Error_type error = Files_and_Paths.explain_error(path,logger);

            folder_count_stop_counter.decrementAndGet();
            //limiter.release();
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
                    String x = "warning: disk scanner not following symbolic link folder:"+f;
                    logger.log(x);
                    if ( warning_payload!=null) warning_payload.add(x);
                }
                else {
                    if ( dir_payload != null) dir_payload.process_dir(f);
                    launch_folder_in_a_thread_(f.toPath(), folder_count_stop_counter, file_payload, dir_payload, warning_payload, aborter, logger);
                }
            }
            else
            {
                if ( file_payload!= null) file_payload.process_file(f);
            }
        }
        folder_count_stop_counter.decrementAndGet();
        //limiter.release();
    }




}
