//SOURCES ../../experimental/deduplicate/manual/Stage_with_2_images.java
//SOURCES ../../experimental/deduplicate/console/Deduplication_console_window.java
//SOURCES ../../experimental/deduplicate/manual/Againor.java
//SOURCES ./Runnable_for_finding_duplicate_file_pairs_similarity.java
//SOURCES ./Similarity_file_pair.java

package klik.machine_learning.deduplication;

import javafx.stage.Window;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.browser.icons.image_properties_cache.Image_properties_RAM_cache;
import klik.browser.virtual_landscape.Path_comparator_source;
import klik.browser.virtual_landscape.Path_list_provider;
import klik.experimental.deduplicate.Abortable;
import klik.experimental.deduplicate.console.Deduplication_console_window;
import klik.experimental.deduplicate.manual.Againor;
import klik.experimental.deduplicate.manual.Stage_with_2_images;
import klik.machine_learning.feature_vector.Feature_vector_cache;
import klik.machine_learning.similarity.Similarity_file_pair;
import klik.util.files_and_paths.*;
import klik.util.log.Logger;
import klik.util.ui.Jfx_batch_injector;
import klik.util.ui.Popups;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

//**********************************************************
public class Deduplication_by_similarity_engine implements Againor, Abortable
//**********************************************************
{
    public final Window owner;
    private final Supplier<Feature_vector_cache> fv_cache_supplier;
    Logger logger;
    BlockingQueue<Similarity_file_pair> same_file_pairs_input_queue = new LinkedBlockingQueue<>();
    AtomicInteger threads_in_flight = new AtomicInteger(0);
    AtomicInteger duplicates_found = new AtomicInteger(0);
    File target_dir;
    Deduplication_console_window console_window;
    boolean end_reported = false;

    public final Aborter private_aborter = new Aborter("Deduplication_engine",logger);
    Stage_with_2_images stage_with_2_images;
    //boolean same_image_size;
    private final Image_properties_RAM_cache image_properties_RAM_cache; // may be null, if not only image of same size are considered

    private final boolean looking_for_images;
    private final double too_far_away;
    private final Path_list_provider path_list_provider;
    private final Path_comparator_source path_comparator_source;

    //**********************************************************
    public Deduplication_by_similarity_engine(
            boolean looking_for_images, // if false, we look for songs
            Path_list_provider path_list_provider,
            Path_comparator_source path_comparator_source,
            double too_far_away,
            Window owner,
            File target_dir_,
            Image_properties_RAM_cache image_properties_RAM_cache,
            Supplier<Feature_vector_cache> fv_cache_supplier,
            Logger logger_)
    //**********************************************************
    {
        this.looking_for_images = looking_for_images;
        this.path_list_provider = path_list_provider;
        this.path_comparator_source = path_comparator_source;
        this.too_far_away = too_far_away;
        //this.same_image_size = same_image_size;
        this.image_properties_RAM_cache = image_properties_RAM_cache;
        this.fv_cache_supplier = fv_cache_supplier;
        this.owner = owner;
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
                owner, private_aborter, logger);

        Runnable r = this::runnable_deduplication;
        Actor_engine.execute(r,"Deduplicate by similarity",logger);
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
        List<File_with_a_few_bytes> files = null;
        if ( looking_for_images)
        {
            files =  get_all_images();

        }
        else
        {
            files = get_all_songs();
        }
        //for(File_with_a_few_bytes mf : files) logger.log(mf.file.getAbsolutePath());
        logger.log("Deduplication::runnable_deduplication found a total of "+files.size()+ " files");

        console_window.set_status_text("Found " + files.size() + " files ... comparison by similarity started...");
        console_window.total_files_to_be_examined.addAndGet(files.size());

        long pairs = (long)files.size()*((long)files.size()-1L);
        pairs /= 2L;
        console_window.total_pairs_to_be_examined.addAndGet(pairs);

        // launch actor (feeder) in another tread
        Runnable_for_finding_duplicate_file_pairs_similarity duplicate_finder =
                new Runnable_for_finding_duplicate_file_pairs_similarity(
                        File_with_a_few_bytes.convert_to_paths(files),
                        too_far_away,
                        image_properties_RAM_cache, // maybe null
                        fv_cache_supplier,
                        path_comparator_source,
                        this,
                        files,
                        same_file_pairs_input_queue,
                        owner, private_aborter, logger);
        Actor_engine.execute(duplicate_finder,"Deduplicate by similarity (2)",logger);

        logger.log("Deduplication::runnable_deduplication thread launched");
    }

    /*
    //**********************************************************
    private void try_audio()
    //**********************************************************
    {
        List<File_with_a_few_bytes> files = get_all_songs();
        if ( files.isEmpty())
        {
            return;
        }
        //for(File_with_a_few_bytes mf : files) logger.log(mf.file.getAbsolutePath());
        logger.log("Deduplication::runnable_deduplication found a total of "+files.size()+ " files");

        console_window.set_status_text("Found " + files.size() + " files ... comparison by similarity started...");
        console_window.total_files_to_be_examined.addAndGet(files.size());

        long pairs = (long)files.size()*((long)files.size()-1L);
        pairs /= 2L;
        console_window.total_pairs_to_be_examined.addAndGet(pairs);

        // launch actor (feeder) in another tread

        List<Path> paths = File_with_a_few_bytes.convert_to_paths(files);//path_list_provider.only_image_paths(Feature_cache.get(Feature.Show_hidden_files));

        Runnable_for_finding_duplicate_file_pairs_similarity duplicate_finder =
                new Runnable_for_finding_duplicate_file_pairs_similarity(
                        paths,
                        too_far_away_song,//0.04, // MAGIC
                        image_properties_RAM_cache,
                        fv_cache_supplier,
                        path_comparator_source,
                        this,
                        files,
                        same_file_pairs_input_queue,
                        owner, private_aborter, logger);
        Actor_engine.execute(duplicate_finder,logger);

        logger.log("Deduplication::runnable_deduplication thread launched");
    }
*/

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
            if ( stage_with_2_images == null) stage_with_2_images = new Stage_with_2_images(similarity,owner, sim_file_pair.file_pair(), local_againor, console_window.count_deleted, path_list_provider, path_comparator_source,private_aborter, logger);
            else stage_with_2_images.set_pair(similarity,sim_file_pair.file_pair(),path_list_provider, path_comparator_source, private_aborter);
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
                        Popups.popup_warning( "Search for duplicates ENDED", "(no duplicates found)", true, owner,logger);
                        end_reported = true;
                    }
                    console_window.set_end_examined();
                    return;
                }
                logger.log("manual deduplicator: nothing to do at this time but finder threads are still running");
            }
        };
        Actor_engine.execute(r,"Deduplicate by similarity (3)",logger);

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

    //**********************************************************
    private List<File_with_a_few_bytes> get_all_songs()
    //**********************************************************
    {
        List<File_with_a_few_bytes> returned = new ArrayList<>();
        File[] files = target_dir.listFiles();
        if ( files == null) return returned;
        for (File f : files)
        {
            //if ( !Guess_file_type.is_this_a_song(f.toPath(),owner,logger)) continue; too expensive
            if ( !Guess_file_type.is_this_path_a_music(f.toPath())) continue;
            File_with_a_few_bytes mf = new File_with_a_few_bytes(f,logger);
            returned.add(mf);
        }
        return returned;
    }

}
