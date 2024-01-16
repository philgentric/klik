package klik.level2.backup;

import klik.actor.Aborter;
import klik.actor.Actor;
import klik.actor.Actor_engine;
import klik.actor.Message;
import klik.change.Change_gang;
import klik.files_and_paths.*;
import klik.util.Logger;

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
    private static final boolean verbose = false;
    public static final int ONGOING_FILES = 2*Runtime.getRuntime().availableProcessors();// 2000;
    public final Logger logger;
    private Per_folder_mini_console mini_console;
    public final Backup_stats stats;
    public final ConcurrentLinkedQueue<String> reports;
    public final Aborter aborter;

    //**********************************************************
    public Backup_actor_for_one_folder(Backup_stats stats_, ConcurrentLinkedQueue<String> reports_,
                                       Aborter aborter_,
                                       Logger logger_)
    //**********************************************************
    {
        stats = stats_;
        reports = reports_;
        logger = logger_;
        aborter = aborter_;


    }

    //**********************************************************
    @Override
    public String run(Message m)
    //**********************************************************
    {
        Directory_backup_job_request request = (Directory_backup_job_request) m;
        return do_one_folder(request);
    }


    private void on_error(String message)
    {
        logger.log(message);
        //file_termination_reporter.has_ended(message,null);
        //subfolder_termination_reporter.has_ended(message,null);
    }

    //**********************************************************
    private String do_one_folder(Directory_backup_job_request request)
    //**********************************************************
    {
        logger.log("Doing 1 folder:" + request.source_dir.getAbsolutePath());

        if (request.get_aborter().should_abort()) {
            logger.log("abort0 from duplicate_internal()");
            on_error("aborted");
            request.finished = true;
            return "abort0 from duplicate_internal()";
        }

        // ok, we will really have something to backup
        stats.target_dir_count.incrementAndGet();
        mini_console = new Per_folder_mini_console(logger);
        mini_console.create();
        mini_console.init(request);


        boolean enable_check_for_same_file_different_name = true;
        if (!request.destination_dir.exists()) {
            if (request.destination_dir.mkdir()) {
                logger.log("created folder: " + request.destination_dir);
                // first time backup, no need to check for previous files
                enable_check_for_same_file_different_name = false;
            } else {
                logger.log("FATAL ! could not create folder: " + request.destination_dir);
                on_error("FATAL");
                request.finished = true;
                return "FATAL ! could not create folder: " + request.destination_dir;
            }
        }
        if (verbose) {
            logger.log("******************************");
            logger.log("will copy all content of dir: " + request.source_dir);
            logger.log("                    into dir: " + request.destination_dir);
            logger.log("******************************");
        }


        File[] all_source_files = request.source_dir.listFiles();
        if (all_source_files == null) {
            logger.log("Empty folder: " + request.source_dir);
            on_error("Empty folder");
            request.finished = true;
            return "Empty folder: " + request.source_dir;
        }

        int count = 0;
        for (File file_to_be_copied : all_source_files)
        {
            if (request.get_aborter().should_abort()) {
                logger.log("abort2 from duplicate_internal()");
                break;
            }
            if (file_to_be_copied.isDirectory()) continue;
            if ( Guess_file_type.ignore(file_to_be_copied.toPath())) continue;

            /*
            Actor_engine.run(
                    new Backup_actor_for_one_file(stats, logger), // need on actor instance per task because the file comparator is not reentrant
                    new File_backup_job_request(request.destination_dir, file_to_be_copied, mini_console, enable_check_for_same_file_different_name, request.aborter,logger),
                    null,
                    logger
            );
            */
            new Backup_actor_for_one_file(stats, logger).run(new File_backup_job_request(request.destination_dir, file_to_be_copied, mini_console, enable_check_for_same_file_different_name, request.aborter,logger));
            count++;
        }
        logger.log("Folder "+request.source_dir.getAbsolutePath()+" "+count+" files backups launched in threads");




        for (File sub_dir_to_be_copied : all_source_files)
        {
            if (request.get_aborter().should_abort()) {
                logger.log("abort1 from duplicate_internal()");
                break;
            }
            if (!sub_dir_to_be_copied.isDirectory()) continue;
            if ( Files.isSymbolicLink(sub_dir_to_be_copied.toPath()))
            {
                logger.log("backup warning: symbolic link not followed "+sub_dir_to_be_copied);
                continue;
            }


            // create new job for this subfolder
            Directory_backup_job_request directory_backup_job_request = new Directory_backup_job_request(sub_dir_to_be_copied, new File(request.destination_dir, sub_dir_to_be_copied.getName()), request.aborter,logger);
            // dont feed the beast too fast
            // as we open one virtual thread per file
            while (Backup_actor_for_one_file.ongoing.get() >= ONGOING_FILES)
            {

                try {
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    logger.log_stack_trace(e.toString());
                    return e.toString();
                }
                if (request.get_aborter().should_abort()) {
                    logger.log("abort3 from duplicate_internal()");
                    break;
                }
                logger.log("ONGOING files = " + Backup_actor_for_one_file.ongoing.get());
            }
            (new Backup_actor_for_one_folder(stats, reports, aborter,logger)).do_one_folder(directory_backup_job_request);
            if (mini_console != null) mini_console.show_progress();

        }


        logger.log("Folder "+request.source_dir.getAbsolutePath()+" DONE");

        // tell above folder that this subfolder is done
        stats.done_dir_count.incrementAndGet();

        // since the files are done in threads, this does not work:
        //long size = Files_and_Paths.get_size_on_disk_excluding_sub_folders(request.source_dir.toPath(), logger);
        //stats.number_of_bytes_processed.addAndGet(size);

        if ( mini_console != null)
        {
            String final_report = mini_console.make_final_report();
            reports.add(final_report);
            mini_console.show_progress();
            //mini_console.close();
            //logger.log("\n\n\n closing miniconcole for: "+request.source_dir.getAbsolutePath());
        }
        List<Old_and_new_Path> l = new ArrayList<>();
        Old_and_new_Path oanp = new Old_and_new_Path(request.source_dir.toPath(), request.destination_dir.toPath(), Command_old_and_new_Path.command_copy, Status_old_and_new_Path.copy_done);
        l.add(oanp);
        Change_gang.report_changes(l);
        request.finished = true;
        return"Backup_actor_for_one_folder OK for:"+request.source_dir.getAbsolutePath();
    }




}
