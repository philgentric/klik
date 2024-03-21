package klik.level2.deduplicate;

import javafx.application.Platform;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.files_and_paths.*;
import klik.level2.deduplicate.manual.Stage_with_2_images;
import klik.browser.Browser;
import klik.level2.deduplicate.console.Deduplication_console_window;
import klik.level2.deduplicate.manual.Againor;
import klik.properties.Static_application_properties;
import klik.util.Logger;
import klik.util.Popups;
import klik.util.Stack_trace_getter;

import java.io.File;
import java.util.ArrayList;
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
    //private final Aborter browser_aborter;
    //private Runnable_for_finding_duplicate_file_pairs finder;
    //private Runnable_for_finding_duplicate_file_pairs duplicate_finder;
    Deduplication_console_window console_window;
    boolean end_reported = false;
    private Aborter private_aborter = new Aborter("Deduplication_engine",logger);
    Stage_with_2_images stage_with_2_images;

    //**********************************************************
    public Deduplication_engine(Browser b_, File target_dir_, Logger logger_)
    //**********************************************************
    {
        browser = b_;
        target_dir = target_dir_;
        logger = logger_;
        //browser_aborter = b_.aborter;
    }


    //**********************************************************
    public void do_your_job(boolean auto)
    //**********************************************************
    {
        logger.log("Deduplication::look_for_all_files()");
        Deduplication_engine local_engine = this;


        console_window = new Deduplication_console_window(this,"Looking for duplicated files in:" + target_dir.getAbsolutePath(),  800, 800, false, browser, private_aborter, logger);

        Runnable r = new Runnable() {
            @Override
            public void run() {
                runnable_deduplication2(local_engine, auto);
            }
        };
        Actor_engine.execute(r, private_aborter,logger);
        logger.log("Deduplication::look_for_all_files() runnable_deduplication thread launched");
    }


    //**********************************************************
    public void abort()
    //**********************************************************
    {
        logger.log("Deduplication::abort()");
        console_window.set_end_deleted();
        private_aborter.abort();
        if ( stage_with_2_images!=null) stage_with_2_images.close();
    }
    /*
    //**********************************************************
    private void runnable_deduplication(Deduplication_engine local_deduplication, boolean auto)
    //**********************************************************
    {
        List<My_File> files = scan();
        //for(My_File mf : files) logger.log(mf.file.getAbsolutePath());
        logger.log("Deduplication::runnable_deduplication found a total of "+files.size()+ " files");

        console_window.get_interface().set_status_text("Found " + files.size() + " files ... comparison for identity started...");

         {
            // launch actor (feeder) in another tread
            finder = new Runnable_for_finding_duplicate_file_pairs(local_deduplication, files, same_file_pairs_input_queue, browser_aborter, logger);
            Actor_engine.execute(finder, browser_aborter,logger);

            logger.log("Deduplication::runnable_deduplication thread launched on "+files.size()+ " files");

        }

        if (!wait_for_finder_to_find_something()) return;

        if (auto) {
            logger.log("\n\n\nAUTO MODE!\n\n\n");
            deduplicate_auto();
        } else {
            logger.log("\n\n\nMANUAL MODE: ask_user_about_each_pair\n\n\n");
            again(false);
        }

    }*/

    //**********************************************************
    private void runnable_deduplication2(Deduplication_engine local_deduplication, boolean auto)
    //**********************************************************
    {
        find_duplicate_pairs(local_deduplication);

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
    private void find_duplicate_pairs(Deduplication_engine local_deduplication)
    //**********************************************************
    {
        List<My_File> files = scan();
        //for(My_File mf : files) logger.log(mf.file.getAbsolutePath());
        logger.log("Deduplication::runnable_deduplication found a total of "+files.size()+ " files");

        console_window.set_status_text("Found " + files.size() + " files ... comparison for identity started...");
        console_window.total_files_to_be_examined.addAndGet(files.size());

        long pairs = (long)files.size()*((long)files.size()-1L);
        pairs /= 2L;
        console_window.total_pairs_to_be_examined.addAndGet(pairs);

        int n = Runtime.getRuntime().availableProcessors()-1;
        if (n < 1) n = 1;
        int inc = files.size()/n;
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
            Actor_engine.execute(duplicate_finder, private_aborter,logger);

            logger.log("Deduplication::runnable_deduplication thread launched on i_min="+i_min+ "i_max="+i_max);
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
                if ( is_finished())
                {
                    console_window.set_end_examined();
                    console_window.set_end_deleted();
                    logger.log("going to actually delete!");
                    break;
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

            Old_and_new_Path oanp = new Old_and_new_Path(to_be_deleted.toPath(), null, Command_old_and_new_Path.command_move_to_trash, Status_old_and_new_Path.before_command,false);
            ll.add(oanp);
            erased++;
            console_window.count_deleted.incrementAndGet();

            if (erased % 10 == 0) console_window.set_status_text("Erased files =" + erased);

        }
        Moving_files.safe_delete_files(browser.my_Stage.the_Stage, ll, private_aborter,logger);

        //Popups.popup_warning("End of automatic de-duplication for :" + target_dir.getAbsolutePath(), erased + " pairs de-duplicated", false, logger);

    }

    private boolean is_finished() {
        if ( remaining_threads.get() == 0) return true;
        return false;
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
            File_pair p = same_file_pairs_input_queue.peek();
            if (p != null) return true;

            if ( is_finished())
            {
                logger.log("wait_for_finder_to_find_something: FINISHED ????? ");

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
    private void ask_user_about_a_duplicate_pair(File_pair file_pair)
    //**********************************************************
    {
        if (!file_pair.f1.my_file.file.exists()) {
            logger.log("giving up:" + file_pair.f1.my_file.file.getAbsolutePath() + " does not exist anymore");
            again(true);
            return;
        }
        if (!file_pair.f2.my_file.file.exists()) {
            logger.log("giving up:" + file_pair.f2.my_file.file.getAbsolutePath() + " does not exist anymore");
            again(true);
            return;
        }
        My_File_and_status files[] = new My_File_and_status[2];
        files[0] = file_pair.f1;
        files[1] = file_pair.f2;


        logger.log("deduplicate:" + file_pair.f1.my_file.file.getAbsolutePath() + "-" + file_pair.f2.my_file.file.getAbsolutePath() + " is_image=" + file_pair.is_image);

        Againor local_againor = this;
        Platform.runLater(() -> {
            if ( stage_with_2_images == null) stage_with_2_images = new Stage_with_2_images(browser, file_pair, local_againor, private_aborter, logger);
            else stage_with_2_images.set_pair(file_pair);
        });
    }
/*

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
*/

    //**********************************************************
    @Override
    public void again(boolean previous_file_deleted)
    //**********************************************************
    {
        if ( private_aborter.should_abort()) return;
        if ( previous_file_deleted) console_window.count_deleted.incrementAndGet();

        logger.log("manual deduplicator: again called !");
        Runnable r = () -> {
            {
                File_pair p = same_file_pairs_input_queue.poll();
                if (p != null) {
                    logger.log("manual deduplicator: ask_user_about_a_duplicate_pair called !");
                    ask_user_about_a_duplicate_pair(p);
                }
                else
                {
                    logger.log("manual deduplicator: nothing to do...");
                    if (is_finished())
                    {
                        logger.log("\nduplicate finder is finished !!");
                        if ( !end_reported)
                        {
                            Popups.popup_warning(browser.my_Stage.the_Stage,"Search for duplicates ENDED", "(no duplicates found)", true, logger);
                            end_reported = true;
                        }
                        console_window.set_end_examined();
                    }
                }
            }
        };
        Actor_engine.execute(r, private_aborter,logger);

    }


    //**********************************************************
    public void count(boolean b)
    //**********************************************************
    {
        logger.log("Deduplication::count()");
        console_window = new Deduplication_console_window(this,"Looking for duplicated files in:" + target_dir.getAbsolutePath(),  800, 800, true, browser, private_aborter, logger);

        Runnable r = () -> just_count();
        Actor_engine.execute(r, private_aborter,logger);
        logger.log("Deduplication::count() runnable_deduplication thread launched");
    }

    //**********************************************************
    private void just_count()
    //**********************************************************
    {
        Deduplication_engine local_deduplication = this;
        /*
        List<My_File> files = scan();
        console_window.get_interface().set_status_text("Found " + files.size() + " files ... comparison for identity started...");
        // launch actor (feeder) in another tread
        finder2 = new Runnable_for_finding_duplicate_file_pairs2(local_deduplication, files, same_file_pairs_input_queue, browser_aborter, logger);
        Actor_engine.execute(finder2,browser_aborter,logger);
        logger.log("Deduplication::look_for_all_files() Duplicate_file_pairs_finder thread launched");
        */
        find_duplicate_pairs(local_deduplication);
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
                if ( is_finished())
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
        console_window.set_status_text("Scanning directories");
        boolean also_hidden_files = Static_application_properties.get_show_hidden_files(logger);

        List<My_File> files = Deduplication_console_window.get_all_files_down(target_dir, console_window, also_hidden_files, logger);
        //Collections.sort(files, by_path_length);



        return files;
    }

}
