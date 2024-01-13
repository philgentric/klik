package klik.level2.deduplicate;

import javafx.application.Platform;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.files_and_paths.*;
import klik.util.Threads;
import klik.browser.Browser;
import klik.level2.deduplicate.console.Deduplication_console_interface;
import klik.level2.deduplicate.console.Deduplication_console_window;
import klik.level2.deduplicate.manual.Againor;
import klik.level2.deduplicate.manual.Deduplicate_popup;
import klik.level2.deduplicate.manual.N_image_stage;
import klik.properties.Static_application_properties;
import klik.util.Logger;
import klik.util.Popups;
import klik.util.Stack_trace_getter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

//**********************************************************
public class Deduplication_engine implements Againor
//**********************************************************
{
    Browser browser;
    Logger logger;
    BlockingQueue<File_pair> same_file_pairs_input_queue = new LinkedBlockingQueue<>();
    //int n_threads;
    AtomicInteger remaining_threads = new AtomicInteger(0);
    AtomicInteger duplicates_found = new AtomicInteger(0);
    File target_dir;
    boolean files_are_sorted_by_size = false;
    private final Aborter aborter;
    private Runnable_for_finding_duplicate_file_pairs finder;
    Deduplication_console_window console_window;
    boolean end_reported = false;


    //**********************************************************
    public Deduplication_engine(Browser b_, File target_dir_, Logger logger_)
    //**********************************************************
    {
        browser = b_;
        target_dir = target_dir_;
        logger = logger_;
        aborter = new Aborter();
    }


    //**********************************************************
    public void do_your_job(boolean auto)
    //**********************************************************
    {
        logger.log("Deduplication::look_for_all_files()");
        Deduplication_engine local_engine = this;

        console_window = new Deduplication_console_window("Looking for duplicated files in:" + target_dir.getAbsolutePath(),  800, 800, false, aborter, logger);

        Runnable r = new Runnable() {
            @Override
            public void run() {
                runnable_deduplication(local_engine, auto);
            }
        };
        Actor_engine.execute(r,logger);
        logger.log("Deduplication::look_for_all_files() runnable_deduplication thread launched");
    }


    //**********************************************************
    public void abort()
    //**********************************************************
    {
        logger.log("Deduplication::kill()");
        aborter.abort();
        console_window.set_end_deleted();
        console_window.abort();
        if (finder != null) finder.abort();
    }
    //**********************************************************
    private void runnable_deduplication(Deduplication_engine local_deduplication, boolean auto)
    //**********************************************************
    {
        List<My_File> files = scan();
        //for(My_File mf : files) logger.log(mf.file.getAbsolutePath());

        console_window.get_interface().set_status_text("Found " + files.size() + " files ... comparison for identity started...");

        // launch actor (feeder) in another tread
        finder = new Runnable_for_finding_duplicate_file_pairs(local_deduplication, files, same_file_pairs_input_queue, aborter, logger);

        Actor_engine.execute(finder,logger);

        logger.log("Deduplication::look_for_all_files() Duplicate_file_pairs_finder thread launched");

        if (!wait_for_finder_to_find_something()) return;

        if (auto) {
            logger.log("\n\n\nAUTO MODE!\n\n\n");
            deduplicate_auto();
        } else {
            logger.log("\n\n\nMANUAL MODE: ask_user_about_each_pair\n\n\n");
            again(false);
        }

    }


    //**********************************************************
    private void deduplicate_auto()
    //**********************************************************
    {
        logger.log("deduplicate ALL: starting, in its own thread");

        int erased = 0;
        for (;;)
        {
            if (aborter.should_abort()) {
                logger.log("Deduplicator::deduplicate_all abort");
                return;
            }
            File_pair p = null;
            try {
                p = same_file_pairs_input_queue.poll(300, TimeUnit.MILLISECONDS);
            }
            catch (InterruptedException e) {
                logger.log(Stack_trace_getter.get_stack_trace("" + e));
                return;
            }
            if (p == null)
            {
                if ( finder.is_finished())
                {
                    console_window.set_end_examined();
                    console_window.set_end_deleted();
                    logger.log("Deletor: nothing left to delete");
                    return;
                }
                logger.log(remaining_threads.get() + " alive threads + empty queue, retrying");
                continue;
            }

            File to_be_deleted = which_one_to_delete(p);
            // if there are more than 2 copies, strange things happen
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

            boolean unsafe = true; // unsafe = we dont move the duplicate into the trash folder, we delete it for good
            if (unsafe) {
                Old_and_new_Path oanp = new Old_and_new_Path(to_be_deleted.toPath(), null, Command_old_and_new_Path.command_delete_forever, Status_old_and_new_Path.before_command);
                Files_and_Paths.unsafe_delete_file(browser.my_Stage.the_Stage,oanp, aborter, logger);
            } else {
                Old_and_new_Path oanp = new Old_and_new_Path(to_be_deleted.toPath(), null, Command_old_and_new_Path.command_move_to_trash, Status_old_and_new_Path.before_command);
                Files_and_Paths.safe_delete_file(browser.my_Stage.the_Stage, oanp, aborter, logger);
            }
            erased++;
            get_interface().increment_deleted();

            if (erased % 10 == 0) console_window.get_interface().set_status_text("Erased files =" + erased);

        }

        //Popups.popup_warning("End of automatic de-duplication for :" + target_dir.getAbsolutePath(), erased + " pairs de-duplicated", false, logger);

    }

    //**********************************************************
    private File which_one_to_delete(File_pair p)
    //**********************************************************
    {
        if ( p.f1.to_be_deleted)
        {
            if ( p.f2.to_be_deleted)
            {
                logger.log("FATAL: both files in pair should be deleted ?");
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
                logger.log("FATAL: No file in pair should be deleted ?");
                return null;
            }
        }
    }

    //**********************************************************
    private boolean wait_for_finder_to_find_something()
    //**********************************************************
    {
        // wait for feeder to find something, initially
        // so that the pump does not early stop
        // max 200 seconds
        for (int i = 0; i < 2000; i++)
        {
            if (aborter.should_abort()) {
                logger.log("Deduplicator::deduplicate_all abort");
                return false;
            }
            File_pair p = same_file_pairs_input_queue.peek();
            if (p != null) return true;

            if ( finder.is_finished())
            {
                abort();
                return true;
            }
            logger.log("wait_for_finder_to_find_something: sleep ");

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                logger.log("deduplicate ALL: sleep interrupted");
            }

        }
        return false;
    }


    //**********************************************************
    private void ask_user_about_a_duplicate_pair(File_pair p)
    //**********************************************************
    {
        if (!p.f1.my_file.file.exists()) {
            logger.log("giving up:" + p.f1.my_file.file.getAbsolutePath() + " does not exist anymore");
            again(true);
            return;
        }
        if (!p.f2.my_file.file.exists()) {
            logger.log("giving up:" + p.f2.my_file.file.getAbsolutePath() + " does not exist anymore");
            again(true);
            return;
        }
        My_File_and_status files[] = new My_File_and_status[2];
        files[0] = p.f1;
        files[1] = p.f2;


        logger.log("deduplicate:" + p.f1.my_file.file.getAbsolutePath() + "-" + p.f2.my_file.file.getAbsolutePath() + " is_image=" + p.is_image);

        Againor local_againor = this;
        if (p.is_image) {
            Platform.runLater(() -> { N_image_stage is = new N_image_stage(browser, files, local_againor, logger);});
        } else {
            List<String> given_keywords_list = new ArrayList<String>();
            given_keywords_list.add(p.f1.my_file.file.getAbsolutePath());
            given_keywords_list.add(p.f2.my_file.file.getAbsolutePath());
            int size = (int) (p.f1.my_file.file.length() / 1000L);
            Platform.runLater(() ->{Deduplicate_popup pop =
                    new Deduplicate_popup(
                            browser.my_Stage.the_Stage,"these files are identical !" + size + "(kB)",
                            given_keywords_list, 800, 200, local_againor, logger);});
        }
    }


    //**********************************************************
    void sort_pairs_by_file_size()
    //**********************************************************
    {
        Comparator<File_pair> comp = new Comparator<File_pair>() {

            @Override
            public int compare(File_pair o1, File_pair o2) {
                return Long.valueOf(o1.f1.my_file.file.length()).compareTo(Long.valueOf(o2.f1.my_file.file.length()));
            }
        };
        List<File_pair> same_in_pairs2 = new ArrayList<File_pair>(same_file_pairs_input_queue);
        Collections.sort(same_in_pairs2, comp);
        Collections.reverse(same_in_pairs2);
        same_file_pairs_input_queue.clear();
        same_file_pairs_input_queue.addAll(same_in_pairs2);
        files_are_sorted_by_size = true;
    }


    //**********************************************************
    @Override
    public void again(boolean previous_file_deleted)
    //**********************************************************
    {
        if ( previous_file_deleted) get_interface().increment_deleted();

        logger.log("manual deduplicator: again called !");
        // this call should not block
        Runnable r = new Runnable() {
            @Override
            public void run() {
                //for(;;)
                {
                    File_pair p = same_file_pairs_input_queue.poll();
                    if (p != null) {
                        logger.log("manual deduplicator: ask_user_about_a_duplicate_pair called !");

                        ask_user_about_a_duplicate_pair(p);
                    }
                    else
                    {
                        logger.log("manual deduplicator: nothing to do...");
                        if (finder.is_finished())
                        {
                            logger.log("\nduplicate finder is finished !!");
                            if ( !end_reported)
                            {
                                Popups.popup_warning(browser.my_Stage.the_Stage,"Search for duplicates ENDED", "(no duplicates found)", true, logger);
                                end_reported = true;
                            }
                            console_window.set_end_examined();
                            return;
                        }

                        try {
                            Thread.sleep(300);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        };
        Actor_engine.execute(r,logger);

    }


    //**********************************************************
    public void count(boolean b)
    //**********************************************************
    {
        logger.log("Deduplication::count()");
        console_window = new Deduplication_console_window("Looking for duplicated files in:" + target_dir.getAbsolutePath(),  800, 800, true, aborter, logger);

        Runnable r = new Runnable() {
            @Override
            public void run() {
                just_count();
            }
        };
        Actor_engine.execute(r,logger);
        logger.log("Deduplication::count() runnable_deduplication thread launched");
    }

    //**********************************************************
    private void just_count()
    //**********************************************************
    {
        Deduplication_engine local_deduplication = this;
        List<My_File> files = scan();
        console_window.get_interface().set_status_text("Found " + files.size() + " files ... comparison for identity started...");
        // launch actor (feeder) in another tread
        finder = new Runnable_for_finding_duplicate_file_pairs(local_deduplication, files, same_file_pairs_input_queue, aborter, logger);
        Threads.execute(finder,logger);
        logger.log("Deduplication::look_for_all_files() Duplicate_file_pairs_finder thread launched");

        int count = 0;
        for (;;) {
            File_pair p = null;
            try {
                p = same_file_pairs_input_queue.poll(300, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (p == null)
            {
                if ( finder.is_finished())
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
    private List<My_File> scan()
    //**********************************************************
    {
        console_window.get_interface().set_status_text("Scanning directories");
        boolean also_hidden_files = Static_application_properties.get_show_hidden_files(logger);

        List<My_File> files = Files_and_Paths.get_all_files_down(target_dir, console_window.get_interface(), also_hidden_files, logger);
        //Collections.sort(files, by_path_length);
        return files;
    }

    //**********************************************************
    public Deduplication_console_interface get_interface()
    //**********************************************************
    {
        return console_window.get_interface();
    }
}
