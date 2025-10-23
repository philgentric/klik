//SOURCES ./Per_folder_mini_console.java
//SOURCES ./Backup_actor_for_one_file.java
//SOURCES ./Directory_backup_job_request.java
package klik.experimental.backup;

import javafx.stage.Window;
import klik.util.execute.actor.Aborter;
import klik.util.execute.actor.Actor;
import klik.util.execute.actor.Message;
import klik.util.execute.actor.workers.Actor_engine_based_on_workers;
import klik.change.Change_gang;
import klik.util.files_and_paths.*;
import klik.util.files_and_paths.old_and_new.Command;
import klik.util.files_and_paths.old_and_new.Old_and_new_Path;
import klik.util.files_and_paths.old_and_new.Status;
import klik.util.log.Logger;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

//**********************************************************
public class Backup_actor_for_one_folder implements Actor
//**********************************************************
{
    //File_comparator file_comparator;
    private static final boolean dbg = false;
    public final Logger logger;
    private Per_folder_mini_console mini_console;
    public final Backup_stats stats;
    public final ConcurrentLinkedQueue<String> reports;
    public final Aborter aborter;
    Backup_actor_for_one_file file_actor;
    Backup_actor_for_one_folder folder_actor;
    public final boolean deep_byte_check;
    public final boolean check_for_same_file_different_name;
    private Actor_engine_based_on_workers actor_engine_based_on_workers;
    private Window owner;


    //**********************************************************
    public Backup_actor_for_one_folder(Backup_stats stats_,
                                       boolean check_for_same_file_different_name,
                                       boolean deep_byte_check,
                                       ConcurrentLinkedQueue<String> reports,
                                       Actor_engine_based_on_workers actor_engine_based_on_workers,
                                        Window owner,
                                       Aborter aborter,
                                       Logger logger)
    //**********************************************************
    {
        this.check_for_same_file_different_name = check_for_same_file_different_name;
        this.deep_byte_check = deep_byte_check;
        stats = stats_;
        this.reports = reports;
        this.logger = logger;
        this.aborter = aborter;
        this.actor_engine_based_on_workers = actor_engine_based_on_workers;
        this.owner = owner;

        // allocate a dedicated actor per folder since a folder maybe in its own thread and file comparator is not re-entrant
        file_actor = new Backup_actor_for_one_file(stats, logger);

    }



    //**********************************************************
    @Override
    public String name()
    //**********************************************************
    {
        return "Backup_actor_for_one_folder";
    }
    //**********************************************************
    @Override
    public String run(Message m)
    //**********************************************************
    {
        Directory_backup_job_request request = (Directory_backup_job_request) m;
        String returned = do_one_folder(request);
        return  returned;
    }


    //**********************************************************
    private void on_error(String message)
    //**********************************************************
    {
        logger.log(message);
        //file_termination_reporter.has_ended(error_message,null);
        //subfolder_termination_reporter.has_ended(error_message,null);
    }

    //**********************************************************
    private String do_one_folder(Directory_backup_job_request request)
    //**********************************************************
    {
        if ( dbg) logger.log("Doing 1 folder:" + request.source_dir.getAbsolutePath());

        if (request.get_aborter().should_abort()) {
            return my_abort(request,"abort0");

        }

        // ok, we will really have something to backup
        stats.target_dir_count.incrementAndGet();
        boolean slow = false;
        if (( check_for_same_file_different_name)||(deep_byte_check))
        {
            slow = true;
            mini_console = new Per_folder_mini_console(aborter,logger);
            mini_console.create();
            mini_console.init(request);
        }

        boolean local_check_for_same_file_different_name = check_for_same_file_different_name;
        if (!request.destination_dir.exists()) {
            if (request.destination_dir.mkdir()) {
                logger.log("created folder: " + request.destination_dir);
                // first time backup, no need to check for previous files
                local_check_for_same_file_different_name = false;
            } else {
                return my_abort(request,"FATAL ! could not create folder: " + request.destination_dir);
            }
        }
        if (dbg) {
            logger.log("******************************");
            logger.log("will copy all content of dir: " + request.source_dir);
            logger.log("                    into dir: " + request.destination_dir);
            logger.log("******************************");
        }


        File[] all_source_files = request.source_dir.listFiles();
        if (all_source_files == null) {
            return my_abort(request,"Empty source folder: " + request.source_dir);
        }

        int count = 0;
        for (File file_to_be_copied : all_source_files)
        {
            if (request.get_aborter().should_abort()) {
                return my_abort(request,"abort2 from duplicate_internal()");
            }
            if (file_to_be_copied.isDirectory()) continue;
            if ( Guess_file_type.should_ignore(file_to_be_copied.toPath())) continue;

            /*
            //this is to do 1 thread per file, benchmarks indicate this is bad on small machines and/or slow (e.g. external) disks
            Actor_engine.run(
                    new Backup_actor_for_one_file(stats, logger), // need on actor instance per task because the file comparator is not reentrant
                    new File_backup_job_request(request.destination_dir, file_to_be_copied, mini_console, check_for_same_file_different_name, request.aborter,logger),
                    null,
                    logger
            );
            so instead we do it on the current thread:
            */
            file_actor.run(new File_backup_job_request(request.destination_dir, file_to_be_copied, mini_console, local_check_for_same_file_different_name, deep_byte_check,request.aborter,logger));
            count++;
        }
        if ( dbg) logger.log("Folder "+request.source_dir.getAbsolutePath()+" "+count+" files backups DONE");



        int threads_launched = 0;
        for (File sub_dir_to_be_copied : all_source_files)
        {
            if (request.get_aborter().should_abort()) {
                return my_abort(request,"abort1 from duplicate_internal()");
            }
            if (!sub_dir_to_be_copied.isDirectory()) continue;
            if ( Files.isSymbolicLink(sub_dir_to_be_copied.toPath()))
            {
                logger.log("backup warning: symbolic link not followed "+sub_dir_to_be_copied);
                continue;
            }

            // create new job for this subfolder
            Directory_backup_job_request directory_backup_job_request = new Directory_backup_job_request(sub_dir_to_be_copied, new File(request.destination_dir, sub_dir_to_be_copied.getName()), request.aborter,logger);

            boolean launch_in_thread = true;
            if ( slow)
            {
                if ( threads_launched >=1) launch_in_thread = false;
            }
            if ( launch_in_thread)
            {
                actor_engine_based_on_workers.run(new Backup_actor_for_one_folder(stats, check_for_same_file_different_name,deep_byte_check,reports, actor_engine_based_on_workers,owner,aborter, logger), directory_backup_job_request, null, logger);
                threads_launched++;
            }
            else
            {
                if ( folder_actor == null) folder_actor = new Backup_actor_for_one_folder(stats, check_for_same_file_different_name,deep_byte_check,reports, actor_engine_based_on_workers,owner,aborter, logger);
                folder_actor.do_one_folder(directory_backup_job_request);
            }
            if (mini_console != null) mini_console.show_progress();

        }
        if ( dbg) logger.log("Folder "+request.source_dir.getAbsolutePath()+" DONE");

        // tell above folder that this subfolder is done
        stats.done_dir_count.incrementAndGet();

        // since the files are done in threads, this does not work:
        //long size = Static_files_and_paths_utilities.get_size_on_disk_excluding_sub_folders(request.source_dir.toPath(), logger);
        //stats.number_of_bytes_processed.addAndGet(size);

        if ( mini_console != null)
        {
            String final_report = mini_console.make_final_report();
            reports.add(final_report);
            mini_console.close();
            //logger.log("\n\n\n closing miniconcole for: "+request.source_dir.getAbsolutePath());
            List<Old_and_new_Path> l = new ArrayList<>();
            Old_and_new_Path oanp = new Old_and_new_Path(request.source_dir.toPath(), request.destination_dir.toPath(), Command.command_copy, Status.copy_done,false);
            l.add(oanp);
            Change_gang.report_changes(l,owner);
        }

        request.finished = true;
        return"Backup_actor_for_one_folder OK for:"+request.source_dir.getAbsolutePath();
    }

    private String my_abort(Directory_backup_job_request request, String msg) {
        logger.log(msg);
        on_error("aborted");
        request.finished = true;
        if ( mini_console != null)
        {
            String final_report = mini_console.make_final_report();
            reports.add(final_report);
            mini_console.close();
        }
        return msg;
    }


}
