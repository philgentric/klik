package klik.level2.backup;

import klik.actor.Aborter;
import klik.actor.Actor;
import klik.actor.Message;
import klik.files_and_paths.Moving_files;
import klik.files_and_paths.My_File;
import klik.util.Logger;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

//**********************************************************
public class Backup_actor_for_one_file implements Actor
//**********************************************************
{

    File_comparator file_comparator;
    private static final boolean verbose = false;
    Logger logger;
    Backup_stats stats;
    long last;

    public static AtomicInteger ongoing = new AtomicInteger(0);
    //**********************************************************
    public Backup_actor_for_one_file(Backup_stats stats, Logger logger)
    //**********************************************************
    {
        this.logger = logger;
        this.stats = stats;
    }


    //static Concurency_limiter file_concurency_limiter = new Concurency_limiter(4);

    //**********************************************************
    @Override
    public String run(Message m)
    //**********************************************************
    {
        ongoing.incrementAndGet();
        last = System.currentTimeMillis();
        File_backup_job_request fbjr = (File_backup_job_request) m;

        if ( file_comparator==null) file_comparator = new File_comparator(fbjr.aborter,logger);

        /*if (Threads.use_fibers)
        {
            try {
                file_concurency_limiter.acquire();
                do_one_file(fbjr);
                file_concurency_limiter.release();
            } catch (InterruptedException e) {
                logger.log("" + e);
            }
        }
        else
        */{

            do_one_file(fbjr);
        }
        if ( fbjr.mini_console != null)
        {
            long now = System.currentTimeMillis();
            if (now - last > 300)
            {
                last = now;
                fbjr.mini_console.show_progress();
            }
        }
        ongoing.decrementAndGet();

        return "";
    }

    //**********************************************************
    private void do_one_file(File_backup_job_request fbjr)
    //**********************************************************
    {
        if (verbose) logger.log("           Doing the backup of file: "+fbjr.file_to_be_copied.getAbsolutePath());

        Dir_results local = duplicate_a_file(fbjr);
        if (!local.was_copied)
        {
            if ( fbjr.aborter.should_abort())
            {
                logger.log("duplicate_a_file status is false, giving up");
                stats.done_dir_count.incrementAndGet();
                return;
            }
        }
        //fbjr.recently_done.add(fbjr.file_to_be_copied);
        stats.bytes_copied.addAndGet(local.bytes_copied);
        stats.files_checked.addAndGet(local.files_processed);
        stats.files_copied.addAndGet(local.files_copied);
        stats.files_skipped.addAndGet(local.files_skipped);
        stats.number_of_bytes_processed.addAndGet(local.bytes_in_context);
    }


    //**********************************************************
    private Dir_results duplicate_a_file(File_backup_job_request file_backup_job_request)
    //**********************************************************
    {
        Dir_results returned = new Dir_results();
        returned.bytes_in_context = file_backup_job_request.file_to_be_copied.length();

        if (verbose ) logger.log("Dealing with file ->" + file_backup_job_request.file_to_be_copied.getName());
        String target_name = file_backup_job_request.file_to_be_copied.getName();

        /*
        // special hack for MAC OSX ;-) avoid copying .DS_Store files, they polute copies
        // but on the other hand if the user checks the produced folder the file count will be different...
        if (target_name.equals(".DS_Store") == true)
        {
            returned.status = true;
            returned.files_processed = 1;
            returned.files_skipped = 1;
            if ( file_backup_job_request.mini_console!=null)
            {
                file_backup_job_request.mini_console.increment_file_count();
                file_backup_job_request.mini_console.show_progress();
            }
            return returned;
        }
        */

        File destination_file;
        destination_file = new File(file_backup_job_request.destination_dir, target_name);
        if (destination_file.exists() == true)
        {
            Similarity_result status = process_in_case_destination_exists(file_backup_job_request.file_to_be_copied, target_name, destination_file, file_comparator,file_backup_job_request.mini_console);
            if ( status == Similarity_result.aborted)
            {
                returned.was_copied = false;
                if ( file_backup_job_request.mini_console!=null)
                {
                    file_backup_job_request.mini_console.increment_file_count();
                    file_backup_job_request.mini_console.show_progress();
                }
                return returned;
            }
            if ( status == Similarity_result.same)
            {
                // file content is same = nothing to do
                returned.was_copied = true;
                returned.files_processed = 1;
                returned.files_skipped = 1;
                if ( file_backup_job_request.mini_console!=null)
                {
                    file_backup_job_request.mini_console.increment_file_count();
                    file_backup_job_request.mini_console.increment_skipped_files();
                    file_backup_job_request.mini_console.show_progress();
                }
                return returned;
            }
            else
            {
                // the destination has been renamed, we must now copy
            }
        }
        else
        {
            if (verbose == true)
                logger.log("CREATE: no such file in archive yet, will create destination: " + destination_file.getAbsolutePath());
            if ( file_backup_job_request.mini_console != null)
            {
                file_backup_job_request.mini_console.add_to_last_news(file_backup_job_request.file_to_be_copied.getName() + "\nhas been CREATED (was not in destination)");
                file_backup_job_request.mini_console.show_progress();
            }
            // the destination needs to be created, we must now copy
        }

        if ( file_backup_job_request.get_aborter().should_abort()) return returned;
        // last check: there might already be a copy as a file with a different name

        if ( file_backup_job_request.enable_check_for_same_file_different_name)
        {

            Path obsolete_name = check_for_same_file_different_name(
                    file_backup_job_request.file_to_be_copied,
                    file_backup_job_request.destination_dir,
                    file_backup_job_request.get_aborter(),
                    logger);
            if (obsolete_name != null) {
                // give the file in the destination the NEW name:
                Path source = destination_file.toPath();
                try {
                    //Files.move(obsolete_name, source.resolveSibling(file_backup_job_request.file_to_be_copied.getName()));
                    FileUtils.moveFile(obsolete_name.toFile(), source.resolveSibling(file_backup_job_request.file_to_be_copied.getName()).toFile());
                } catch (IOException e) {
                    logger.log("WARNING: attempt to rename failed " + e);
                    logger.log("file_to_be_copied name =  " + file_backup_job_request.file_to_be_copied.getName());
                    logger.log("destination_file name =  " + destination_file.getName());
                    returned.was_copied = true;
                    if ( file_backup_job_request.mini_console!=null) file_backup_job_request.mini_console.increment_file_count();
                    return returned;
                }
                logger.log("updated the NAME of the file in the destination folder as it had the same content\n     old name :" +
                        obsolete_name.toFile().getName() +
                        "\n    new name: " + file_backup_job_request.file_to_be_copied.getName());

                returned.was_copied = true;
                returned.files_processed = 1;
                returned.files_skipped = 1;
                if (file_backup_job_request.mini_console != null)
                {
                    file_backup_job_request.mini_console.increment_file_count();
                    file_backup_job_request.mini_console.increment_skipped_files();
                    file_backup_job_request.mini_console.show_progress();
                }
            }
        }

        if ( file_backup_job_request.get_aborter().should_abort()) return returned;

        // perform the copy
        try
        {
            long b = File_copier.copy_file_NIO(destination_file, file_backup_job_request.file_to_be_copied);
            if ( file_backup_job_request.mini_console != null) file_backup_job_request.mini_console.add_to_copied_bytes(b);
        }
        catch (Exception eee) {
            logger.log("EXCEPTION IN COPY: " + eee);
            if ( file_backup_job_request.mini_console != null)
            {
                file_backup_job_request.mini_console.add_to_last_news("source file name :\n" + file_backup_job_request.file_to_be_copied.getAbsolutePath());
                file_backup_job_request.mini_console.add_to_last_news("\ndestination file name :\n" + destination_file.getAbsolutePath());
                file_backup_job_request.mini_console.add_to_last_news("\nERROR: copy failed " + eee);
                file_backup_job_request.mini_console.show_progress();
            }
            returned.was_copied = false;
            return returned;
        }

        returned.was_copied = true;
        returned.files_processed = 1;
        returned.files_copied = 1;
        returned.bytes_copied = returned.bytes_in_context;
        if ( file_backup_job_request.mini_console != null)
        {
            file_backup_job_request.mini_console.increment_file_count();
            file_backup_job_request.mini_console.increment_copied_files();
            file_backup_job_request.mini_console.show_progress();
        }
        return returned;
    }


    //**********************************************************
    private Path check_for_same_file_different_name(File file_to_be_copied, File destination_dir,
                                                    Aborter aborter, Logger logger)
    //**********************************************************
    {
        My_File fr = new My_File(file_to_be_copied,logger);
        for ( File f : destination_dir.listFiles())
        {

            My_File ff = new My_File(f,logger);

            if ( My_File.files_have_same_content(fr,ff, aborter, logger))
            {
                logger.log("Found 2 files with same content:\n   "+file_to_be_copied.getAbsolutePath()+"\n   "+f.getAbsolutePath());
                return ff.file.toPath();
            }
        }
        return null;
    }


    //**********************************************************
    private Similarity_result process_in_case_destination_exists(
            File file_to_be_copied,
            String target_name,
            File destination_file,
            File_comparator file_comparator,
            Per_folder_mini_console mini_console)
    //**********************************************************
    {
        switch (file_comparator.files_are_same(file_to_be_copied, destination_file))
        {
            default:
            case aborted:
                if (verbose == true) logger.log("aborted: " + target_name);
                return Similarity_result.aborted;
            case same:
                if (verbose == true) logger.log("SKIPPED: the SAME file is already there: " + target_name);
                return Similarity_result.same;
            case not_same:
                if (verbose == true)
                    logger.log("OHOH ??: name exists, but files are DIFFERENT, destination renamed: " + target_name);
                // actual copy occurs below
                if ( !rename_destination_file(destination_file))
                {
                    logger.log("WARNING: rename failed for:" + destination_file.getAbsolutePath());
                }
                mini_console.increment_renamed_files();
                mini_console.add_to_renamed_files_names(destination_file.getAbsolutePath());
                return Similarity_result.not_same;
        }
    }


    //**********************************************************
    private boolean rename_destination_file(File destination_file)
    //**********************************************************
    {
        for ( int i = 0; i < 33; i++)
        {
            Path x = Moving_files.generate_new_candidate_name(destination_file.toPath(), "RENAMED_", ""+i, logger);
            if ( x.toFile().exists()) continue;
            try {
                FileUtils.moveFile(destination_file,x.toFile());
            } catch (IOException e) {
                logger.log_stack_trace(e.toString());
                return false;
            }
            return true;
        }
        return false;

    }

}
