package klik.deduplicate;

import javafx.application.Platform;
import klik.change.Command_old_and_new_Path;
import klik.change.Old_and_new_Path;
import klik.change.Status_old_and_new_Path;
import klik.util.Constants;
import klik.util.Logger;
import klik.util.Tool_box;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class Deduplication implements Againor {
    Logger logger;
    ConcurrentLinkedQueue<File_pair> same_in_pairs = new ConcurrentLinkedQueue<File_pair>();
    int n_threads;
    AtomicInteger remaining_threads = new AtomicInteger(0);
    AtomicInteger grand_total = new AtomicInteger(0);
    File target_dir;
    boolean files_are_sorted_by_size = false;

    //**********************************************************
    public Deduplication(File target_dir_, Logger logger_)
    //**********************************************************
    {
        target_dir = target_dir_;
        logger = logger_;
        n_threads = Runtime.getRuntime().availableProcessors();
    }

    //**********************************************************
    public void look_for_all_files(boolean auto)
    //**********************************************************
    {
        FX_popup popup = new FX_popup(
                "Please wait... (do not change dir), looking for duplicates in:",
                target_dir,
                500,
                500, logger);

        Deduplication local_deduplication = this;
        boolean multi_t = true;
        if (multi_t) {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    int n = n_threads - 1;
                    List<My_File> files = get_all_files_down(target_dir, popup, logger);

                    popup.pong("Found " + files.size() + " files ... comparison for identity starts now!");

                    int part = files.size() / n;
                    int start = 0;
                    int end = part;

                    int i = 0;
                    for (; ; ) {
                        Duplicate_file_pairs_finder d = new Duplicate_file_pairs_finder(local_deduplication, i, start, end, files, same_in_pairs, popup, logger);
                        Tool_box.execute(d, logger);
                        start = end;
                        end += part;
                        if (end >= files.size()) end = files.size();
                        i++;
                        if (i >= n - 1) break;
                    }
                    Duplicate_file_pairs_finder d = new Duplicate_file_pairs_finder(local_deduplication, i, end, files.size(), files, same_in_pairs, popup, logger);
                    Tool_box.execute(d, logger);

                    if (auto) {
                        deduplicate_all();

                    } else {
                        wait_until_some_duplicate_pairs_have_been_found();

                    }
                }
            };

            Thread t = new Thread(r);
            t.start();

        } else {
            List<My_File> files = get_all_files_down(target_dir, popup, logger);
            Duplicate_file_pairs_finder d = new Duplicate_file_pairs_finder(local_deduplication, 0, 0, files.size(), files, same_in_pairs, popup, logger);
            Tool_box.execute(d, logger);
        }
    }


    //**********************************************************
    private static List<My_File> get_all_files_down(File cwd, FX_popup popup, Logger logger)
    //**********************************************************
    {
        List<My_File> returned = new ArrayList<>();
        File[] files = cwd.listFiles();
        if (files == null) return returned;
        for (File f : files) {
            if (f.isDirectory()) {
                popup.ping();
                returned.addAll(get_all_files_down(f, popup, logger));
            } else {
                if (f.getName().equals(".DS_Store") == false) {
                    if (f.length() == 0) {
                        logger.log("WARNING: empty file found:" + f.getAbsolutePath());
                        continue;
                    }
                    My_File mf = new My_File(f);
                    returned.add(mf);
                }
            }
        }
        return returned;
    }


    //**********************************************************
    private void deduplicate_all()
    //**********************************************************
    {

        logger.log("deduplicate ALL: starting");

        // wait max 60 seconds for threads to find something
        for (int i =0; i<60; i++ )
        {
            File_pair p = same_in_pairs.peek();
            if (p == null)
            {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    logger.log("deduplicate ALL: sleep interrupted");
                }
            }
        }
        int done = 0;
        File files[] = new File[2];
        for (;;)
        {
            File_pair p = same_in_pairs.poll();
            if (p == null)
            {
                if (remaining_threads.get() == 0) break;
                continue;
            }
            files[0] = p.f1;
            files[1] = p.f2;
            File ddd = favor(p);
            // if there are more than 2 copies, strange things happen
            if (ddd == null) {
                logger.log("deduplicating:\n\t"
                        + p.f1.getAbsolutePath() + "\n\t"
                        + p.f2.getAbsolutePath() + "\n\t"
                        + "GIVING UP!");
                continue;
            }
            logger.log("deduplicating:\n\t"
                    + p.f1.getAbsolutePath() + "\n\t"
                    + p.f2.getAbsolutePath() + "\n\t"
                    + "going to delete:\n\t" + ddd.getAbsolutePath());

            List<Old_and_new_Path> l = new ArrayList<Old_and_new_Path>();
            l.add(new Old_and_new_Path(ddd.toPath(), null, Command_old_and_new_Path.command_delete, Status_old_and_new_Path.before_command));
            Tool_box.safe_delete_all(l, logger);
            done++;

        }

        Tool_box.popup_text("End of automatic de-duplication", done + " pairs de-duplicated");

    }

    //**********************************************************
    private File favor(File_pair p)
    //**********************************************************
    {
        // favor files with ultim in the name
        if (p.f1.getName().endsWith(Constants.ULTIM)) {
            if (p.f2.getName().endsWith(Constants.ULTIM) == false) {
                if (p.f2.exists() == false) {
                    // if there are more than 2 copies, strange things happen
                    return null;
                }
                return p.f1;
            }
        }
        if (p.f2.getName().endsWith(Constants.ULTIM)) {
            if (p.f1.getName().endsWith(Constants.ULTIM) == false) {
                if (p.f1.exists() == false) {
                    // if there are more than 2 copies, strange things happen
                    return null;
                }
                return p.f2;
            }
        }
        // if the names are identical (thus, they are in a different dir)
        // favor long path names
        if (p.f1.getName().equals(p.f2.getName())) {
            if (p.f1.getAbsolutePath().length() < p.f2.getAbsolutePath().length()) {
                if (p.f2.exists() == false) return null;
                return p.f1;
            } else {
                if (p.f1.exists() == false) return null;
                return p.f2;
            }
        }


        // otherwise favor files with long names
        if (p.f1.getName().length() < p.f2.getName().length()) {
            if (p.f2.exists() == false) return null;
            return p.f1;
        } else {
            if (p.f1.exists() == false) return null;
            return p.f2;
        }
    }


    //**********************************************************
    private void wait_until_some_duplicate_pairs_have_been_found()
    //**********************************************************
    {
        boolean cantry = false;
        for (; ; ) {
            File_pair p = same_in_pairs.poll();
            if (p == null) {
                logger.log("no more or not yet ?");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (cantry) {
                    int local = remaining_threads.get();
                    logger.log("remaining threads = " + local);
                    if (local == 0) break;
                } else {
                    cantry = true;
                }
            } else {
                ask_user_for_pair(p);
                return;
            }
        }
        Tool_box.popup_text("No duplicate found", "");

    }

    private void ask_user_for_pair(File_pair p) {
        if (p.f1.exists() == false) {
            logger.log("giving up:" + p.f1.getAbsolutePath() + " does not exist anymore");
            again();
            return;
        }
        if (p.f2.exists() == false) {
            logger.log("giving up:" + p.f2.getAbsolutePath() + " does not exist anymore");
            again();
            return;
        }
        File files[] = new File[2];
        files[0] = p.f1;
        files[1] = p.f2;


        logger.log("deduplicate:" + p.f1.getAbsolutePath() + "-" + p.f2.getAbsolutePath());

        if (p.is_images) {
            N_image_stage is = new N_image_stage(files, this,
                    logger);
        } else {
            List<String> given_keywords_list = new ArrayList<String>();
            given_keywords_list.add(p.f1.getAbsolutePath());
            given_keywords_list.add(p.f2.getAbsolutePath());
            int size = (int) (p.f1.length() / 1000L);

            Againor local = this;
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    Deduplicate_popup pop = new Deduplicate_popup("these files are identical !" + size + "(kB)", given_keywords_list, 800, 200, local, logger);

                }
            });
        }
    }


    void sort_pairs_by_file_size() {
        Comparator<File_pair> comp = new Comparator<File_pair>() {

            @Override
            public int compare(File_pair o1, File_pair o2) {
                return Long.valueOf(o1.f1.length()).compareTo(Long.valueOf(o2.f1.length()));
            }
        };
        List<File_pair> same_in_pairs2 = new ArrayList<File_pair>(same_in_pairs);
        Collections.sort(same_in_pairs2, comp);
        Collections.reverse(same_in_pairs2);
        same_in_pairs.clear();
        same_in_pairs.addAll(same_in_pairs2);
        files_are_sorted_by_size = true;
    }


    @Override
    public void again() {

        File_pair p = same_in_pairs.poll();
        if (p == null) {
            logger.log("no more or not yet ?");
            Tool_box.popup_text("End of duplicates (or no duplicate found)", "Maybe you want to launch search again to check?");
            return;
        }
        ask_user_for_pair(p);
    }
}
