// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ./Backup_stats.java
//SOURCES ./Backup_console_window.java
//SOURCES ./Backup_actor_for_one_folder.java

package klikr.experimental.backup;

import javafx.stage.Window;
import klikr.util.Shared_services;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.execute.actor.Job;
import klikr.util.execute.actor.Job_termination_reporter;
import klikr.util.execute.actor.workers.Actor_engine_based_on_workers;
import klikr.util.files_and_paths.Static_files_and_paths_utilities;
import klikr.util.files_and_paths.Sizes;
import klikr.util.ui.Jfx_batch_injector;
import klikr.util.log.Logger;
import klikr.util.execute.Scheduled_thread_pool;

import java.nio.file.Path;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

//**********************************************************
public class Backup_engine
//**********************************************************
{
    public static final String LAST_SOURCE_DIR = "last_source_dir";
    public static final String LAST_DESTINATION_DIR = "last_destination_dir";
    public static final String LAST_SAVE_DATE = "last_save_date";
    public static final String LAST_STATUS = "last_status";
    //private static final boolean dbg = false;
    final Path source;
    final Path destination;
    final Logger logger;
    Backup_stats stats = new Backup_stats();
    ConcurrentLinkedQueue<String> reports = new ConcurrentLinkedQueue<>();
    ScheduledFuture<?> finalHandle = null;

    public final Aborter dedicated_backup_aborter;
    boolean is_finished = false;
    Backup_console_window backup_console_window;

    private long start;


    //**********************************************************
    public Backup_engine(Path source, Path destination, Logger logger_)
    //**********************************************************
    {
        this.source = source;
        this.destination = destination;
        logger = logger_;
        // we need a dedicated aborter for backup
        dedicated_backup_aborter = new Aborter("backup engine",logger);
    }



    //**********************************************************
    public void go(boolean deep, Window owner)
    //**********************************************************
    {

        backup_console_window = new Backup_console_window(this,stats,logger);
        update_properties(source.toAbsolutePath().toString(), destination.toAbsolutePath().toString());
        update_status(source.toAbsolutePath().toString(), destination.toAbsolutePath().toString(),"incomplete_backup");

        Runnable runnable = () -> launch_in_thread(deep, owner);
        Actor_engine.execute(runnable,"Run backup",logger);
    }

    //**********************************************************
    private void launch_in_thread(boolean deep, Window owner)
    //**********************************************************
    {
        start = System.currentTimeMillis();
        logger.log("Backup starts");
        Aborter actor_engine_cleanup_aborter = new Aborter("cleanup",logger);
        Sizes sizes= Static_files_and_paths_utilities.get_sizes_on_disk_deep(source, dedicated_backup_aborter,owner, logger);

        Actor_engine_based_on_workers actor_engine_based_on_workers = new Actor_engine_based_on_workers("backup engine",actor_engine_cleanup_aborter, logger);
        Job_termination_reporter tr = new Job_termination_reporter() {
            @Override
            public void has_ended(String message, Job job) {
                actor_engine_cleanup_aborter.abort("final cleanup");
            }
        };
        actor_engine_based_on_workers.run(
                new Backup_actor_for_one_folder(stats,deep,deep,reports, actor_engine_based_on_workers,owner,dedicated_backup_aborter,logger),
                new Directory_backup_job_request(source.toFile(), destination.toFile(), dedicated_backup_aborter,logger),
                        tr,logger);

        stats.source_byte_count = sizes.bytes();
        logger.log("monitoring starts");
        Runnable monitoring = () -> {
            if (dedicated_backup_aborter.should_abort())
            {
                logger.log("monitoring = ABORT");
                report_the_end("aborted");
                if ( finalHandle!=null) finalHandle.cancel(true);
                return;
            }

            double done_dirs = stats.done_dir_count.doubleValue();
            logger.log("monitoring, done_dirs ="+done_dirs);
            if ( done_dirs == 0)
            {
                logger.log("monitoring = nothing done");
                return;
            }
            double target_dirs = stats.target_dir_count.doubleValue();
            logger.log("monitoring, target_dirs ="+target_dirs);
            if (done_dirs == target_dirs)
            {
                if ( Backup_actor_for_one_file.ongoing.doubleValue() != 0)
                {
                    logger.log("not finished since there are = "+Backup_actor_for_one_file.ongoing.doubleValue()+" undone files");
                }
                else
                {
                    logger.log("monitoring, this is the END");
                    report_the_end("ended");
                    if (finalHandle != null) finalHandle.cancel(true);
                    return;
                }
            }
            else
            {
                logger.log("not finished since done dirs= "+done_dirs+" target dirs="+target_dirs);
            }
            backup_console_window.update_later();
        };
        finalHandle = Scheduled_thread_pool.execute(monitoring, 300, TimeUnit.MILLISECONDS);

    }

    //**********************************************************
    void report_the_end(String reason)
    //**********************************************************
    {
        logger.log("Backup ends, reason = "+reason);
        is_finished = true;
        //Non_booleans_properties.get_properties_manager(logger).store_properties();

        Jfx_batch_injector.inject(() -> {
            StringBuilder sbb = new StringBuilder();
            for ( String r : reports)
            {
                sbb.append(r);
                sbb.append("\n");
            }
            backup_console_window.textArea.setText(sbb.toString());
            backup_console_window.update_();

            long elapsed = System.currentTimeMillis()-start;
            String local = "FINISHED in ";
            if (elapsed < 1000)
            {
                local += elapsed+" ms";
            }
            else
            {
                int seconds = (int) (elapsed/ 1000);
                if ( seconds > 3600)
                {
                    int hours = seconds/3600;
                    local += hours+ " hours,";
                    seconds = seconds-hours*3600;
                }
                if ( seconds > 60)
                {
                    int minutes = seconds/60;
                    local += minutes+ " minutes,";
                    seconds = seconds-minutes*60;
                }
                local += seconds+ " seconds";

            }
            backup_console_window.remaining_time.setText(local);
        },logger);
    }

    //**********************************************************
    synchronized private void update_status(String source, String destination, String status)
    //**********************************************************
    {
        // first find the target
        for(int i = 0 ; i <= 12 ; i++)
        {
            String key = LAST_SOURCE_DIR +i;

            String s = (String) Shared_services.main_properties().get(key);
            if ( s == null ) break;
            if ( s.equals(source) == true)
            {
                key = LAST_DESTINATION_DIR +i;
                s = (String) Shared_services.main_properties().get(key);
                if ( s == null ) break;
                if ( s.equals(destination) == true)
                {
                    // FOUND
                    key = LAST_STATUS +i;
                    Shared_services.main_properties().set(key,status);
                }
            }
        }
    }

    //**********************************************************
    synchronized private void update_properties(String absolutePath_source, String absolutePath_destination)
    //**********************************************************
    {

        // one issue is to check if this pair is not already in the database

        String key;
        for(int j = 0 ; j < 12; j++)
        {
            key = LAST_SOURCE_DIR +j;
            String s = (String) Shared_services.main_properties().get(key);
            if ( s == null ) break;
            if ( s.equals(absolutePath_source) == true)
            {
                key = LAST_DESTINATION_DIR +j;
                String s2 = (String) Shared_services.main_properties().get(key);
                if ( s2 == null ) break;
                if ( s2.equals(absolutePath_destination) == true)
                {
                    //logger.log("no need to store:"+s+" -> " +s2);
                    // no need to re-store again !
                    // just update the date
                    key = LAST_SAVE_DATE +j;
                    Date d = new Date();
                    s = d.toString();
                    Shared_services.main_properties().set(key,s);
                    return;
                }
            }
        }

        // then we need to reshuffle

        for(int j = 11 ; j >= 0 ; j--)
        {
            key = LAST_SOURCE_DIR +j;
            String s = Shared_services.main_properties().get(key);
            if ( s == null ) continue;
            key = LAST_SOURCE_DIR +(j+1);
            Shared_services.main_properties().set(key,s);

            key = LAST_DESTINATION_DIR +j;
            s = Shared_services.main_properties().get(key);
            if ( s == null )
            {
                // this is BAD !break;
                //JOptionPane.showMessageDialog(null,"no destination","bad property file",JOptionPane.INFORMATION_MESSAGE);
                s = "Corrupted file record, do not use";
            }
            key = LAST_DESTINATION_DIR +(j+1);
            Shared_services.main_properties().set(key,s);

            key = LAST_SAVE_DATE +j;
            s = Shared_services.main_properties().get(key);
            if ( s == null )
            {
                // this is BAD !break;
                //JOptionPane.showMessageDialog(null,"no destination","bad property file",JOptionPane.INFORMATION_MESSAGE);
                s = "unknown date";
            }
            key = LAST_SAVE_DATE +(j+1);
            Shared_services.main_properties().set(key,s);

            key = LAST_STATUS +j;
            s = Shared_services.main_properties().get(key);
            if ( s == null )
            {
                // this is BAD !break;
                //JOptionPane.showMessageDialog(null,"no destination","bad property file",JOptionPane.INFORMATION_MESSAGE);
                s = "status unknown";
            }
            key = LAST_STATUS +(j+1);
            Shared_services.main_properties().set(key,s);

        }

        if (absolutePath_source == null ) return; // usefull for "clearing"
        Shared_services.main_properties().set(LAST_DESTINATION_DIR+"0", absolutePath_destination);
        Shared_services.main_properties().set(LAST_SOURCE_DIR+"0", absolutePath_source);
        Date d = new Date();
        String s = d.toString();
        Shared_services.main_properties().set(LAST_SAVE_DATE+"0", s);

        // NOTE: status is updated by dedicated routine

    }

/*
    //**********************************************************
    public static void remove_all_properties()
    //**********************************************************
    {
        File_storage pm = Shared_services.main_properties();
        for(int j = 0; j <=12 ; j++)
        {
            {
                String key = LAST_SOURCE_DIR + (j + 1);
                if (pm.get(key) != null) pm.remove(key);
            }
            {
                String key = LAST_DESTINATION_DIR + (j + 1);
                if (pm.get(key) != null) pm.remove(key);
            }
            {
                String key = LAST_STATUS + (j + 1);
                if (pm.get(key) != null) pm.remove(key);
            }
            {
                String key = LAST_SAVE_DATE + (j + 1);
                if (pm.get(key) != null) pm.remove(key);
            }
        }
    }


    //**********************************************************
    public void remove_property(int i)
    //**********************************************************
    {

        //  we need to reshuffle ...

        for(int j = i; j <=12 ; j++)
        {
            String key = LAST_SOURCE_DIR +(j+1);
            String s = (String) Shared_services.main_properties().get(key);
            if ( s == null )
            {
                // last one is j
                key = LAST_SOURCE_DIR +j;
                s = (String) Shared_services.main_properties().get(key);
                if ( s != null )
                {
                    Shared_services.main_properties().remove(key);
                }
                key = LAST_DESTINATION_DIR +j;
                s = (String) Shared_services.main_properties().get(key);
                if ( s != null )
                {
                    Shared_services.main_properties().remove(key);
                }
                key = LAST_SAVE_DATE +j;
                s = (String) Shared_services.main_properties().get(key);
                if ( s != null )
                {
                    Shared_services.main_properties().remove(key);
                }
                key = LAST_STATUS +j;
                s = (String) Shared_services.main_properties().get(key);
                if ( s != null )
                {
                    Shared_services.main_properties().remove(key);
                }
                break;
            }
            key = LAST_SOURCE_DIR +j;
            Shared_services.main_properties().set(key,s);

            key = LAST_DESTINATION_DIR +(j+1);
            s = (String) Shared_services.main_properties().get(key);
            if ( s == null ) break;
            key = LAST_DESTINATION_DIR +j;
            Shared_services.main_properties().set(key,s);

            key = LAST_SAVE_DATE +(j+1);
            s = (String) Shared_services.main_properties().get(key);
            if ( s == null ) break;
            key = LAST_SAVE_DATE +j;
            Shared_services.main_properties().set(key,s);

            key = LAST_STATUS +(j+1);
            s = (String) Shared_services.main_properties().get(key);
            if ( s == null ) break;
            key = LAST_STATUS +j;
            Shared_services.main_properties().set(key,s);
        }

    }
*/

    public void abort()
    {
        logger.log("Backup_engine::abort()");
        dedicated_backup_aborter.abort("Backup_engine::abort()");
    }

    public boolean is_finished() {
        return is_finished;
    }

    public String to_string() {
        return source.toAbsolutePath()+"=>"+ destination.toAbsolutePath();
    }
}
