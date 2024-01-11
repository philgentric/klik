package klik.files_and_paths;

import klik.change.Change_gang;
import klik.util.Scheduled_thread_pool;
import klik.util.Logger;

import java.io.File;
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


    //**********************************************************
    public boolean init(
            Path path,
            Filesystem_modification_reporter reporter,
            boolean abort_on_change,
            int timeout_in_minutes,
            Logger logger)
    //**********************************************************
    {

        final Filesystem_item_signature[] signature = new Filesystem_item_signature[1];
        signature[0] = new Filesystem_item_signature(logger);
        if (!signature[0].init(path))
        {
            logger.log("signature failed for :"+path);
            return false;
        }

        //ScheduledFuture<?> finalT = t;
        Runnable r = () -> {
                Filesystem_item_signature possibly_new_signature = new Filesystem_item_signature(logger);
                if ( !possibly_new_signature.init(path) )
                {
                    t.cancel(true);
                    return;
                }
                if (!possibly_new_signature.is_same(signature[0]))
                {
                    if ( dbg) logger.log("Filesystem_item_modification_watcher, change detected for: "+path.toAbsolutePath());
                    // yes it's new ! the file has changed (or the folder content has changed)
                    reporter.report_modified();
                    if ( abort_on_change) t.cancel(true); // abort watch if changed
                    signature[0] = possibly_new_signature; // update the signature to avoid false positives !
                }
        };
        // check every 1 second
        t = Scheduled_thread_pool.execute(r, 1, TimeUnit.SECONDS);

        // use another task to monitor the timeout
        Runnable r2 = () -> t.cancel(true);
        Scheduled_thread_pool.execute(r2,timeout_in_minutes,TimeUnit.MINUTES);
        if (dbg) logger.log("Filesystem_item_modification_watcher init done for:"+path);
        return true;
    }

    //**********************************************************
    public void cancel()
    {
        t.cancel(true);
    }
    //**********************************************************


    //**********************************************************
    public static Filesystem_item_modification_watcher monitor_folder(Path folder_path, int timeout_in_minutes, Logger logger)
    //**********************************************************
    {
        Filesystem_modification_reporter reporter = () -> {
            List<Old_and_new_Path> oanps = new ArrayList<>();
            Command_old_and_new_Path cmd = Command_old_and_new_Path.command_move;
            Old_and_new_Path oan = new Old_and_new_Path(folder_path, folder_path, cmd, Status_old_and_new_Path.a_change_occured_in_this_folder);
            oanps.add(oan);
            if (dbg) logger.log("Filesystem_item_modification_watcher event:"+oan.get_string());

            Change_gang.report_changes(oanps);
        };
        Filesystem_item_modification_watcher fimw = new Filesystem_item_modification_watcher();
        if ( fimw.init(folder_path,reporter,false,timeout_in_minutes,logger))
        {
            return fimw;
        }
        return null;
    }

    //**********************************************************
    public static boolean is_this_folder_showing_external_drives(Path displayed_folder_path, Logger logger)
    //**********************************************************
    {
        String OS_name = System.getProperty("os.name");

        if ( OS_name.contains("Mac OS"))
        {
            if (displayed_folder_path.toAbsolutePath().toString().equals("/Volumes"))
            {
                return true;
            }
            return false;
        }
        if ( OS_name.contains("Linux"))
        {
            if (displayed_folder_path.toAbsolutePath().toString().equals("/dev"))
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
