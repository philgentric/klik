// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ./Filesystem_modification_reporter.java
//SOURCES ./Filesystem_item_signature.java

package klik.util.files_and_paths.modifications;

import javafx.stage.Window;
import klik.util.execute.actor.Aborter;
import klik.change.Change_gang;
import klik.util.execute.Scheduled_thread_pool;
import klik.util.files_and_paths.old_and_new.Command;
import klik.util.files_and_paths.old_and_new.Old_and_new_Path;
import klik.util.files_and_paths.old_and_new.Status;
import klik.util.log.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

//**********************************************************
public class Filesystem_item_modification_watcher
//**********************************************************
{
    private final static boolean dbg = false;
    private ScheduledFuture<?> t = null;
    private Aborter aborter;


    //**********************************************************
    public File_status init(
            Path path,
            Filesystem_modification_reporter reporter,
            boolean abort_on_change,
            int timeout_in_minutes,
            Aborter aborter,
            Logger logger)
    //**********************************************************
    {
        this.aborter = aborter;
        final Filesystem_item_signature[] signature = new Filesystem_item_signature[1];
        signature[0] = new Filesystem_item_signature(logger);
        File_status file_status = signature[0].init(path);
        if (file_status != File_status.OK)
        {
            logger.log("WARNING: signature failed for :"+path);
            return file_status;
        }

        Runnable r = () ->
        {
            if ( aborter.should_abort())
            {
                t.cancel(true);
                logger.log("Filesystem_item_modification_watcher for "+path+" aborted");
                return;
            }
            Filesystem_item_signature local = new Filesystem_item_signature(logger);
            File_status file_status2 = local.init(path);
            if ( file_status2 != File_status.OK )
            {
                t.cancel(true);
                return;
            }
            //logger.log("Filesystem_item_modification_watcher for "+path+" init done "+local.file_signature_array.length);
            if (!local.is_same(signature[0]))
            {
                if ( dbg) logger.log("Filesystem_item_modification_watcher, change detected for: "+path.toAbsolutePath());
                // yes it's new ! the file has changed (or the folder content has changed)
                reporter.report_modified();
                if ( abort_on_change) t.cancel(true); // abort watch if changed
                signature[0] = local; // update the signature to avoid multiple false positives !
            }
        };
        // check every 1 second
        t = Scheduled_thread_pool.execute(r, 1, TimeUnit.SECONDS);

        // use another task to monitor the timeout
        Runnable r2 = () -> t.cancel(true);
        Scheduled_thread_pool.execute(r2,timeout_in_minutes,TimeUnit.MINUTES);
        //if (dbg) logger.log("Filesystem_item_modification_watcher init done for:"+path);
        return File_status.OK;
    }

    //**********************************************************
    public void cancel()
    {
        t.cancel(true);
    }
    //**********************************************************


    //**********************************************************
    public static Filesystem_item_modification_watcher monitor_folder(Path folder_path, int timeout_in_minutes, Window owner, Aborter monitoring_aborter, Logger logger)
    //**********************************************************
    {
        Filesystem_modification_reporter reporter = () ->
        {
            List<Old_and_new_Path> oanps = new ArrayList<>();
            Command cmd = Command.command_move;
            Old_and_new_Path oan = new Old_and_new_Path(folder_path, folder_path, cmd, Status.a_change_occurred_in_this_folder,false);
            oanps.add(oan);
            if (dbg) logger.log("Filesystem_item_modification_watcher event:"+oan.to_string());

            Change_gang.report_changes(oanps,owner);
        };
        Filesystem_item_modification_watcher fimw = new Filesystem_item_modification_watcher();
        if ( fimw.init(folder_path,reporter,false,timeout_in_minutes,monitoring_aborter,logger) == File_status.OK)
        {
            return fimw;
        }
        return null;
    }

    //**********************************************************
    public static boolean is_this_folder_showing_external_drives(Path path, Logger logger)
    //**********************************************************
    {
        String OS_name = System.getProperty("os.name");

        if ( OS_name.contains("Mac OS"))
        {
            if (path.toAbsolutePath().toString().equals("/Volumes"))
            {
                return true;
            }
            return false;
        }
        if ( OS_name.contains("Linux"))
        {
            if (path.toAbsolutePath().toString().equals("/dev"))
            {
                return true;
            }
            return false;
        }
        return false;
        // not easy on windows
//        if ( OS_name.contains("Windows"))
    }

}
