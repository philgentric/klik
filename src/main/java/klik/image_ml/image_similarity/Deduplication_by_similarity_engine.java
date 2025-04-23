//SOURCES ../../level2/deduplicate/manual/Stage_with_2_images.java
//SOURCES ../../level2/deduplicate/console/Deduplication_console_window.java
//SOURCES ../../level2/deduplicate/manual/Againor.java
//SOURCES ./Runnable_for_finding_duplicate_file_pairs_similarity.java
//SOURCES ./Similarity_file_pair.java

package klik.image_ml.image_similarity;

import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.browser.Browser;
import klik.unstable.deduplicate.Abortable;
import klik.unstable.deduplicate.console.Deduplication_console_window;
import klik.unstable.deduplicate.manual.Againor;
import klik.unstable.deduplicate.manual.Stage_with_2_images;
import klik.util.files_and_paths.*;
import klik.util.log.Logger;
import klik.util.ui.Jfx_batch_injector;
import klik.util.ui.Popups;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

//**********************************************************
public class Deduplication_by_similarity_engine implements Againor, Abortable
//**********************************************************
{
    Browser browser;
    Logger logger;
    BlockingQueue<Similarity_file_pair> same_file_pairs_input_queue = new LinkedBlockingQueue<>();
    AtomicInteger threads_in_flight = new AtomicInteger(0);
    AtomicInteger duplicates_found = new AtomicInteger(0);
    File target_dir;
     Deduplication_console_window console_window;
    boolean end_reported = false;

    private final Aborter private_aborter = new Aborter("Deduplication_engine",logger);
    Stage_with_2_images stage_with_2_images;
    boolean quasi_same;

    //**********************************************************
    public Deduplication_by_similarity_engine(boolean quasi_same, Browser b_, File target_dir_, Logger logger_)
    //**********************************************************
    {
        this.quasi_same = quasi_same;
        browser = b_;
        target_dir = target_dir_;
        logger = logger_;
        //browser_aborter = b_.aborter;
    }


    //**********************************************************
    public void do_your_job()
    //**********************************************************
    {
        logger.log("Deduplication::look_for_all_files()");


        console_window = new Deduplication_console_window(
                this,
                "Looking for similar pictures in:" + target_dir.getAbsolutePath(),
                800,
                800,
                false,
                browser.my_Stage.the_Stage, private_aborter, logger);

        Runnable r = this::runnable_deduplication;
        Actor_engine.execute(r,logger);
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
    private void runnable_deduplication()
    //**********************************************************
    {
        find_duplicate_pairs();

        if (!wait_for_finder_to_find_something())
        {
            logger.log("wait_for_finder_to_find_something returns false");
            return;
        }
        again();
    }

    //**********************************************************
    private void find_duplicate_pairs()
    //**********************************************************
    {
        List<File_with_a_few_bytes> files = get_all_images();
        //for(File_with_a_few_bytes mf : files) logger.log(mf.file.getAbsolutePath());
        logger.log("Deduplication::runnable_deduplication found a total of "+files.size()+ " files");

        console_window.set_status_text("Found " + files.size() + " files ... comparison by similarity started...");
        console_window.total_files_to_be_examined.addAndGet(files.size());

        long pairs = (long)files.size()*((long)files.size()-1L);
        pairs /= 2L;
        console_window.total_pairs_to_be_examined.addAndGet(pairs);

        // launch actor (feeder) in another tread
        Runnable_for_finding_duplicate_file_pairs_similarity duplicate_finder = new Runnable_for_finding_duplicate_file_pairs_similarity(browser.virtual_landscape.image_properties_RAM_cache, quasi_same,this, files,  same_file_pairs_input_queue, private_aborter, logger);
        Actor_engine.execute(duplicate_finder,logger);

        logger.log("Deduplication::runnable_deduplication thread launched");
    }




    //**********************************************************
    private boolean are_threaded_finders_finished()
    //**********************************************************
    {
        if ( threads_in_flight.get() == 0) return true;
        return false;
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
            Similarity_file_pair p = same_file_pairs_input_queue.peek();
            if (p != null) return true;

            if ( are_threaded_finders_finished())
            {
                logger.log("wait_for_finder_to_find_something: FINISHED, there was nothing to find ");

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
    private void ask_user_about_a_duplicate_pair(Similarity_file_pair sim_file_pair)
    //**********************************************************
    {
        logger.log("Similar:" + sim_file_pair.file_pair().f1().getAbsolutePath() + "-" + sim_file_pair.file_pair().f2().getAbsolutePath());

        Againor local_againor = this;
        Jfx_batch_injector.inject(() -> {
            String similarity = ""+sim_file_pair.similarity();
            if ( stage_with_2_images == null) stage_with_2_images = new Stage_with_2_images(similarity,browser, sim_file_pair.file_pair(), local_againor, console_window.count_deleted, private_aborter, logger);
            else stage_with_2_images.set_pair(similarity,sim_file_pair.file_pair());
        },logger);
    }


    //**********************************************************
    @Override
    public void again()
    //**********************************************************
    {
        if ( private_aborter.should_abort()) return;

        logger.log("manual deduplicator: again called !");
        Runnable r = () -> {
            // this loop is only to manage the 3 second timeout on the queue
            for(;;)
            {
                if ( private_aborter.should_abort()) return;
                Similarity_file_pair p;
                try
                {
                    p = same_file_pairs_input_queue.poll(3, TimeUnit.SECONDS);
                }
                catch (InterruptedException e)
                {
                    logger.log("" + e);
                    return;
                }
                if (p != null)
                {
                    logger.log("manual deduplicator: ask_user_about_a_duplicate_pair called !");
                    if (!p.file_pair().f1().exists())
                    {
                        logger.log("skipping search result because " + p.file_pair().f1().getAbsolutePath() + " does not exist anymore");
                        continue;
                    }
                    if (!p.file_pair().f2().exists()) {
                        logger.log("skipping search result because " + p.file_pair().f2().getAbsolutePath() + " does not exist anymore");
                        continue;
                    }

                    ask_user_about_a_duplicate_pair(p);
                    return;
                }
                // p == null means timeout
                if (are_threaded_finders_finished())
                {
                    logger.log("\nduplicate finder is finished !!");
                    if (!end_reported) {
                        Popups.popup_warning(browser.my_Stage.the_Stage, "Search for duplicates ENDED", "(no duplicates found)", true, logger);
                        end_reported = true;
                    }
                    console_window.set_end_examined();
                    return;
                }
                logger.log("manual deduplicator: nothing to do at this time but finder threads are still running");
            }
        };
        Actor_engine.execute(r,logger);

    }

    //**********************************************************
    private List<File_with_a_few_bytes> get_all_images()
    //**********************************************************
    {
        List<File_with_a_few_bytes> returned = new ArrayList<>();
        File[] files = target_dir.listFiles();
        if ( files == null) return returned;
        for (File f : files)
        {
            if ( !Guess_file_type.is_file_an_image(f)) continue;
            File_with_a_few_bytes mf = new File_with_a_few_bytes(f,logger);
            returned.add(mf);
        }
        return returned;
    }

}
