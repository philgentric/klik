// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ./manual/Stage_with_2_images.java
//SOURCES ./console/Deduplication_console_window.java
//SOURCES ./manual/Againor.java
//SOURCES ./File_pair_deduplication.java
//SOURCES ./Runnable_for_finding_duplicate_file_pairs.java
//SOURCES ./Abortable.java

package klik.experimental.deduplicate;

import javafx.stage.Window;
import klik.System_info;
import klik.util.execute.actor.Aborter;
import klik.util.execute.actor.Actor_engine;
import klik.browser.virtual_landscape.Path_comparator_source;
import klik.path_lists.Path_list_provider;
import klik.properties.Non_booleans_properties;
import klik.properties.boolean_features.Feature;
import klik.properties.boolean_features.Feature_cache;
import klik.util.files_and_paths.*;
import klik.experimental.deduplicate.manual.Stage_with_2_images;
import klik.experimental.deduplicate.console.Deduplication_console_window;
import klik.experimental.deduplicate.manual.Againor;
import klik.util.files_and_paths.old_and_new.Command;
import klik.util.files_and_paths.old_and_new.Old_and_new_Path;
import klik.util.files_and_paths.old_and_new.Status;
import klik.util.ui.Jfx_batch_injector;
import klik.util.log.Logger;
import klik.util.ui.Popups;
import klik.util.log.Stack_trace_getter;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

//**********************************************************
public class Deduplication_engine implements Againor, Abortable
//**********************************************************
{

    private final Window owner;
    Logger logger;
    BlockingQueue<File_pair_deduplication> same_file_pairs_input_queue = new LinkedBlockingQueue<>();
    LongAdder threads_in_flight = new LongAdder();
    LongAdder duplicates_found = new LongAdder();
    File target_dir;
    Deduplication_console_window console_window;
    boolean end_reported = false;
    private Aborter private_aborter = new Aborter("Deduplication_engine",logger);
    Stage_with_2_images stage_with_2_images;
    Path_list_provider path_list_provider;
    Path_comparator_source path_comparator_source;
    //**********************************************************
    public Deduplication_engine(
            Window owner,
            File target_dir_, Path_list_provider path_list_provider, Path_comparator_source path_comparator_source,Logger logger_)
    //*************************************w********************
    {
        this.owner = owner;
        this.path_list_provider = path_list_provider;
        this.path_comparator_source = path_comparator_source;
        target_dir = target_dir_;
        logger = logger_;
    }


    //**********************************************************
    public void do_your_job(boolean auto)
    //**********************************************************
    {
        logger.log("Deduplication::look_for_all_files()");
        Deduplication_engine local_engine = this;


        console_window = new Deduplication_console_window(this,"Looking for duplicated files in:" + target_dir.getAbsolutePath(),  800, 800, false, owner, private_aborter, logger);

        Runnable r = () -> runnable_deduplication(local_engine, auto);
        Actor_engine.execute(r,"Deduplicate",logger);
        logger.log("Deduplication::look_for_all_files() runnable_deduplication thread launched");
    }


    //**********************************************************
    @Override // Abortable
    public void abort()
    //**********************************************************
    {
        logger.log("Deduplication::abort()");
        console_window.set_end_deleted();
        private_aborter.abort("Deduplication::abort()");
        if ( stage_with_2_images!=null) stage_with_2_images.close();
    }

    //**********************************************************
    private void runnable_deduplication(Deduplication_engine local_deduplication, boolean auto)
    //**********************************************************
    {
        find_duplicate_pairs(local_deduplication);

        if (!wait_for_finder_to_find_something())
        {
            logger.log("wait_for_finder_to_find_something returns false");
            return;
        }

        if (auto)
        {
            logger.log("\n\n\nAUTO MODE!\n\n\n");
            deduplicate_auto();
        }
        else
        {
            logger.log("\n\n\nMANUAL MODE: ask_user_about_each_pair\n\n\n");
            again();
        }

    }

    //**********************************************************
    private void find_duplicate_pairs(Deduplication_engine local_deduplication)
    //**********************************************************
    {
        List<File_with_a_few_bytes> files = scan();
        //for(File_with_a_few_bytes mf : files) logger.log(mf.file.getAbsolutePath());
        logger.log("Deduplication::runnable_deduplication found a total of "+files.size()+ " files");

        console_window.set_status_text("Found " + files.size() + " files ... comparison for bit-size identity started...");
        console_window.total_files_to_be_examined.add(files.size());

        long pairs = (long)files.size()*((long)files.size()-1L);
        pairs /= 2L;
        console_window.total_pairs_to_be_examined.add(pairs);

        // launch N threads

        int number_of_threads = System_info.how_many_cores() -1;
        if (number_of_threads < 1) number_of_threads = 1;
        int inc = files.size()/number_of_threads;
        if ( inc == 0) inc = 1;
        int i_min = 0;
        boolean end = false;
        for(;;)
        {
            int i_max = i_min + inc;
            if ( i_max >= files.size())
            {
                end = true;
                i_max =  files.size();
            }

            // launch actor (feeder) in another tread
            Runnable_for_finding_duplicate_file_pairs duplicate_finder = new Runnable_for_finding_duplicate_file_pairs(local_deduplication, files, i_min, i_max, same_file_pairs_input_queue, private_aborter, logger);
            Actor_engine.execute(duplicate_finder,"Deduplicate (2)",logger);

            logger.log("Deduplication::runnable_deduplication thread launched on i_min="+i_min+ " i_max="+i_max);
            if ( end) break;
            i_min = i_max;

        }
    }

    //**********************************************************
    private void deduplicate_auto()
    //**********************************************************
    {
        logger.log("deduplicate ALL: starting, in its own thread");

        int erased = 0;
        List<Old_and_new_Path> ll = new ArrayList<>();
        for (;;)
        {
            if (private_aborter.should_abort()) {
                logger.log("Deduplicator::deduplicate_all abort");
                return;
            }
            File_pair_deduplication p = null;
            try {
                p = same_file_pairs_input_queue.poll(300, TimeUnit.MILLISECONDS);
            }
            catch (InterruptedException e) {
                logger.log(Stack_trace_getter.get_stack_trace("" + e));
                return;
            }
            if (p == null)
            {
                if ( are_threaded_finders_finished())
                {
                    console_window.set_end_examined();
                    console_window.set_end_deleted();
                    logger.log("going to actually delete!");
                    break;
                }
                logger.log(threads_in_flight.doubleValue() + " alive threads + empty queue, retrying");
                continue;
            }

            File to_be_deleted = which_one_to_delete(p);
            // if there are more than 2 copies, strange things happen??
            if (to_be_deleted == null)
            {
                logger.log("deduplicating:\n\t"
                        + p.f1.my_file.file.getAbsolutePath() + "\n\t"
                        + p.f2.my_file.file.getAbsolutePath() + "\n\t"
                        + "not done (1)!");
                continue;
            }
            if (!to_be_deleted.exists()) {
                logger.log("deduplicating:\n\t"
                        + p.f1.my_file.file.getAbsolutePath() + "\n\t"
                        + p.f2.my_file.file.getAbsolutePath() + "\n\t"
                        + "not done (2)!");
                continue;
            }
            logger.log("deduplicating:\n\t"
                    + p.f1.my_file.file.getAbsolutePath() + "\n\t"
                    + p.f2.my_file.file.getAbsolutePath() + "\n\t"
                    + "going to delete:\n\t" + to_be_deleted.getAbsolutePath());


            Path trash_dir = Non_booleans_properties.get_trash_dir(to_be_deleted.toPath(),owner,logger);
            Path new_Path = (Paths.get(trash_dir.toString(), to_be_deleted.getName()));
            Old_and_new_Path oanp = new Old_and_new_Path(to_be_deleted.toPath(), new_Path, Command.command_move_to_trash, Status.before_command,false);
            ll.add(oanp);
            erased++;
            console_window.count_deleted.increment();

            if (erased % 10 == 0) console_window.set_status_text("Erased files =" + erased);

        }
        double x = owner.getX()+100;
        double y = owner.getY()+100;
        Moving_files.safe_delete_files(ll, x,y,owner, private_aborter,logger);

        //Popups.popup_warning("End of automatic de-duplication for :" + target_dir.getAbsolutePath(), erased + " pairs de-duplicated", false, logger);

    }

    //**********************************************************
    private boolean are_threaded_finders_finished()
    //**********************************************************
    {
        if ( threads_in_flight.doubleValue() == 0) return true;
        return false;
    }

    //**********************************************************
    private File which_one_to_delete(File_pair_deduplication p)
    //**********************************************************
    {
        if ( p.f1.to_be_deleted)
        {
            if ( p.f2.to_be_deleted)
            {
                logger.log("❌ FATAL: both files in pair should be deleted ?");
                return null;
            }
            else
            {
                return p.f1.my_file.file;
            }
        }
        else
        {
            if ( p.f2.to_be_deleted)
            {
                return p.f2.my_file.file;
            }
            else
            {
                logger.log("❌ FATAL: No file in pair should be deleted ?");
                return null;
            }
        }
    }

    //**********************************************************
    private boolean wait_for_finder_to_find_something()
    //**********************************************************
    {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            logger.log("deduplicate ALL: sleep interrupted");
        }
        // wait for feeder to find something, initially
        // so that the pump does not early stop
        // max 200 seconds
        for (int i = 0; i < 2000; i++)
        {
            if (private_aborter.should_abort()) {
                logger.log("Deduplicator::deduplicate_all abort");
                return false;
            }
            File_pair_deduplication p = same_file_pairs_input_queue.peek();
            if (p != null) return true;

            if ( are_threaded_finders_finished())
            {
                logger.log("wait_for_finder_to_find_something: FINISHED ????? ");

                abort();
                return false;
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                logger.log("deduplicate ALL: sleep interrupted");
            }

        }
        logger.log("wait_for_finder_to_find_something: done ");
        return true;
    }


    //**********************************************************
    private void ask_user_about_a_duplicate_pair(File_pair_deduplication file_pair, Path_list_provider path_list_provider, Path_comparator_source path_comparator_source)
    //**********************************************************
    {
        //My_File_and_status files[] = new My_File_and_status[2];
        //files[0] = file_pair.f1;
        //files[1] = file_pair.f2;

        logger.log("deduplicate:" + file_pair.f1.my_file.file.getAbsolutePath() + "-" + file_pair.f2.my_file.file.getAbsolutePath() + " is_image=" + file_pair.is_image);

        String title = "Deduplication";
        Againor local_againor = this;
        Jfx_batch_injector.inject(() -> {
            File_pair local = new File_pair(file_pair.f1.my_file.file, file_pair.f2.my_file.file);
            if ( stage_with_2_images == null) stage_with_2_images = new Stage_with_2_images(title, owner, local, local_againor, console_window.count_deleted,path_list_provider,path_comparator_source,private_aborter, logger);
            else stage_with_2_images.set_pair(title,local,path_list_provider,path_comparator_source,private_aborter);
        },logger);
    }


    //**********************************************************
    @Override
    public void again()
    //**********************************************************
    {
        // "again" is called after user action in a window:
        // it is intended to catch 1 File_pair_deduplication and call ONCE
        // ask_user_about_a_duplicate_pair
        // so the forever loop in the thread is just here
        // to manage (1) the end and (2) aborting
        // leveraging the 3s timeout on the queue
        if ( private_aborter.should_abort()) return;

        logger.log("manual deduplicator: again called !");
        Runnable r = () -> {

            for(;;) // just retry relative to the 3 second timeout
            {
                if (private_aborter.should_abort()) return;

                File_pair_deduplication p;
                try {
                    p = same_file_pairs_input_queue.poll(3, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    logger.log("" + e);
                    return;
                }
                if (p != null)
                {
                    if (!p.f1.my_file.file.exists()) {
                        logger.log("skipping result because " + p.f1.my_file.file.getAbsolutePath() + " does not exist anymore");
                        again();
                        return;
                    }
                    if (!p.f2.my_file.file.exists()) {
                        logger.log("skipping result because " + p.f2.my_file.file.getAbsolutePath() + " does not exist anymore");
                        again();
                        return;
                    }

                    logger.log("manual deduplicator: ask_user_about_a_duplicate_pair called !");
                    ask_user_about_a_duplicate_pair(p, path_list_provider,path_comparator_source);
                    return;
                }
                // p == null means timeout
                if (are_threaded_finders_finished())
                {
                    logger.log("\nduplicate finder is finished !!");
                    if (!end_reported)
                    {
                        Popups.popup_warning( "❗ Search for duplicates ENDED", "(no duplicates found)", true, owner,logger);
                        end_reported = true;
                        return;
                    }
                    console_window.set_end_examined();
                }
                logger.log("manual deduplicator: nothing to do at this time but finder threads are still running");
            }
        };
        Actor_engine.execute(r,"Deduplicate (3)",logger);

    }


    //**********************************************************
    public void count(boolean b)
    //**********************************************************
    {
        logger.log("Deduplication::count()");
        console_window = new Deduplication_console_window(this,"Looking for duplicated files in:" + target_dir.getAbsolutePath(),  800, 800, true, owner, private_aborter, logger);

        Runnable r = () -> just_count();
        Actor_engine.execute(r,"Show count",logger);
        logger.log("Deduplication::count() runnable_deduplication thread launched");
    }

    //**********************************************************
    private void just_count()
    //**********************************************************
    {
        Deduplication_engine local_deduplication = this;
        /*
        List<File_with_a_few_bytes> files = scan();
        console_window.get_interface().set_status_text("Found " + files.size() + " files ... comparison for identity started...");
        // launch actor (feeder) in another tread
        finder2 = new Runnable_for_finding_duplicate_file_pairs2(local_deduplication, files, same_file_pairs_input_queue, browser_aborter, logger);
        Actor_engine.execute(finder2,browser_aborter,logger);
        logger.log("Deduplication::look_for_all_files() Duplicate_file_pairs_finder thread launched");
        */
        find_duplicate_pairs(local_deduplication);
        int count = 0;
        for (;;) {
            File_pair_deduplication p = null;
            try {
                p = same_file_pairs_input_queue.poll(300, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (p == null)
            {
                if ( are_threaded_finders_finished())
                {
                    console_window.set_end_examined();
                    break;
                }
            }
            count++;
        }
        logger.log("found "+count+" identical file pairs");
        //Popups.popup_warning("Duplicate file count",""+count,false,logger);
    }

    //**********************************************************
    private List<File_with_a_few_bytes> scan()
    //**********************************************************
    {
        console_window.set_status_text("Scanning directories");

        List<File_with_a_few_bytes> files = Deduplication_console_window.get_all_files_down(target_dir, console_window, Feature_cache.get(Feature.Show_hidden_files), logger);
        //Collections.sort(files, by_path_length);
        logger.log("deduplication scan done "+files.size()+" files found");

        Collections.shuffle(files);
        return files;
    }

}
