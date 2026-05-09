// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.path_lists;

import javafx.scene.Node;
import javafx.stage.Window;
import klikr.browser_core.virtual_landscape.Image_found;
import klikr.search.Search_result;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.files_and_paths.Guess_file_type;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;
import klikr.browser_core.virtual_landscape.Redrawer;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

//**********************************************************
public class Path_list_provider_for_search_results implements Path_list_provider
//**********************************************************
{
    public static final boolean dbg = false;
    public static final String KLIKR_IMAGE_PLAYLIST_EXTENSION = "klikr_image_playlist";
    HashMap<String, Search_result> search_results;
    HashMap<String, List<Path>> path_sets;
    HashMap<String, Boolean> search_results_is_max;


    public final Logger logger;
    private final Window owner;
    private final Change_broadcaster change_broadcaster;

    // cached:
    private final String key;
    // we need a list to keep the same order when we rename
    public final ConcurrentLinkedQueue<String> paths = new ConcurrentLinkedQueue<>();
    private Redrawer redrawer;
    //**********************************************************
    public Path_list_provider_for_search_results(
            Window owner,
            Aborter aborter,
            Logger logger)
    //**********************************************************
    {
        this.logger = logger;
        change_broadcaster = new Change_broadcaster(logger);
        this.owner = owner;
        this.key = "search results";
        reload("constructor", aborter);
    }

    //**********************************************************
    public void set_redrawer(Redrawer redrawer)
    //**********************************************************
    {
        this.redrawer = redrawer;
    }


    @Override
    public void set_cache_creation_time(long cache_creation_time) {

    }

    @Override
    public Path get_cache_save_path() {
        return null;
    }

    @Override
    public boolean is_rescan_needed() {
        return true;
    }

    //**********************************************************
    @Override
    public String to_string()
    //**********************************************************
    {
        return "Path_list_provider_for_search_results";
    }

    //**********************************************************
    @Override
    public Path get_folder_path()
    //**********************************************************
    {
        // does not have a meaning for a playlist
        return null;
    }


    //**********************************************************
    @Override
    public String get_key()
    //**********************************************************
    {
        return key;
    }

    //**********************************************************
    @Override
    public int how_many_files_and_folders(boolean force_rescan, boolean consider_also_hidden_files, boolean consider_also_hidden_folders, Aborter aborter)
    //**********************************************************
    {
        int returned = 0;
        for (String s : paths) {
            if (aborter.should_abort()) return 0;
            if ((new File(s)).isDirectory()) {
                if (!consider_also_hidden_folders) {
                    if (Guess_file_type.should_ignore(Path.of(s), logger)) continue;
                    returned++;
                    continue;
                }
            }
            if (!consider_also_hidden_files) {
                if (Guess_file_type.should_ignore(Path.of(s), logger)) continue;
            }
            returned++;
        }
        return returned;

    }


    //**********************************************************
    @Override
    public List<Path> only_file_paths(boolean force_rescan, boolean consider_also_hidden_files, Aborter aborter)
    //**********************************************************
    {
        List<Path> returned = new ArrayList<>();
        for (String s : paths) {
            if ((new File(s)).isDirectory()) continue;
            if (!consider_also_hidden_files) {
                if (Guess_file_type.should_ignore(Path.of(s), logger)) continue;
            }
            returned.add(Path.of(s));
        }
        return returned;
    }

    //**********************************************************
    @Override
    public List<Path> only_song_paths(boolean force_rescan, boolean consider_also_hidden_files, Aborter aborter)
    //**********************************************************
    {
        List<Path> returned = new ArrayList<>();
        for (String s : paths) {
            if ((new File(s)).isDirectory()) continue;
            if (!Guess_file_type.is_this_path_extension_a_music(Path.of(s), logger)) continue;
            if (!consider_also_hidden_files) {
                if (Guess_file_type.should_ignore(Path.of(s), logger)) continue;
            }
            returned.add(Path.of(s));
        }
        return returned;
    }

    //**********************************************************
    @Override
    public List<Path> only_image_paths(boolean force_rescan, boolean consider_also_hidden_files, Aborter aborter)
    //**********************************************************
    {
        List<Path> returned = new ArrayList<>();
        for (String s : paths) {
            if ((new File(s)).isDirectory()) continue;
            if (!Guess_file_type.is_this_path_extension_an_image(Path.of(s), owner, logger)) continue;
            if (!consider_also_hidden_files) {
                if (Guess_file_type.should_ignore(Path.of(s), logger)) continue;
            }
            returned.add(Path.of(s));
        }
        return returned;
    }


    //**********************************************************
    @Override
    public List<Path> only_folder_paths(boolean force_rescan, boolean consider_also_hidden_folders, Aborter aborter)
    //**********************************************************
    {
        List<Path> returned = new ArrayList<>();
        for (String s : paths) {
            if (!(new File(s)).isDirectory()) continue;
            if (!consider_also_hidden_folders) {
                if (Guess_file_type.should_ignore(Path.of(s), logger)) continue;
            }
            returned.add(Path.of(s));
        }
        return returned;
    }

    //**********************************************************
    @Override
    public Path resolve(String string)
    //**********************************************************
    {
        return null;
    }


    //**********************************************************
    public Move_provider get_move_provider_for_file_system()
    //**********************************************************
    {
        // not a file system thing
        return null;
    }

    //**********************************************************
    public Move_provider get_move_in_provider()
    //**********************************************************
    {
        Move_provider move_provider = new Move_provider() {
            @Override
            public void move(Path destination, boolean destination_is_trash, List<File> the_list, Window owner, double x, double y, Aborter aborter, Logger logger) {

                logger.log("Entering move() for Path_list_provider_for_playlist " + the_list.size());
                List<String> the_list2 = new ArrayList<>();
                for (File f : the_list) {
                    the_list2.add(f.getAbsolutePath());
                }
                user_wants_to_add_items(the_list2, aborter);

                report_change(owner);

            }
        };

        return move_provider;
    }

    //**********************************************************
    public void user_wants_to_add_items(
            List<String> the_list_of_new_items,
            Aborter aborter)
    //**********************************************************
    {
        long start = System.currentTimeMillis();
        Runnable r = () ->
        {
            List<String> oks = new ArrayList<>();
            for (String path_s : the_list_of_new_items) {
                logger.log(" looking at " + path_s);
                if (aborter.should_abort()) {
                    logger.log(" ABORTING " + aborter.reason());
                    return;
                }
                File f = new File(path_s);
                if (f.isDirectory()) {
                    logger.log("IGNORED: " + f + " is a directory");
                }
            }
            String last = null;
            List<String> final_dest = new ArrayList<>();
            for (String f : oks) {
                if (!paths.contains(f)) {
                    final_dest.add(f);
                    last = f;
                }
            }
            logger.log(final_dest.size() + " files accepted as possible songs");
            paths.addAll(final_dest);

        };
        Actor_engine.execute(r, "Adding multiple songs to playlist", logger);

    }

    //**********************************************************
    private void report_change(Window owner)
    //**********************************************************
    {
    }

    //**********************************************************
    @Override
    public void delete(Path path, Window owner, double x, double y, Aborter aborter, Logger logger)
    //**********************************************************
    {
        logger.log("Path_list_provider_for_playlist.delete(): " + path.toAbsolutePath().toString());
        //dump("paths before delete");
        paths.remove(path.toAbsolutePath().toString());
        //dump("paths after delete");
        //dump("paths after save");
        report_change(owner);
    }

    //**********************************************************
    private void dump(String msg)
    //**********************************************************
    {
        logger.log("===== Path_list_provider_for_playlist.paths: " + msg + " =====");
        for (String s : paths) {
            logger.log("   " + s);
        }
        logger.log("=========================================");
    }

    //**********************************************************
    @Override
    public void delete_multiple(List<Path> paths, Window owner, double x, double y, Aborter aborter, Logger logger)
    //**********************************************************
    {
        for (Path p : paths) {
            paths.remove(p.toAbsolutePath().toString());
        }
        report_change(owner);
    }

    //**********************************************************
    @Override
    public void reload(String origin, Aborter aborter)
    //**********************************************************
    {

        change_broadcaster.call_all_change_subscribers();
    }

    @Override
    public Change_broadcaster get_change_broadcaster() {
        return change_broadcaster;
    }

    //**********************************************************
    @Override
    public Files_and_folders files_and_folders(boolean force_rescan, Image_found imgfnd, boolean consider_also_hidden_files, boolean consider_also_hidden_folders, Aborter aborter)
    //**********************************************************
    {

        List<Path> files = new ArrayList<>();
        List<Path> folders = new ArrayList<>();
        Files_and_folders returned = new Files_and_folders(files, folders);
        for (String s : paths) {
            if ((new File(s)).isDirectory()) {
                if (!consider_also_hidden_folders) {
                    if (Guess_file_type.should_ignore(Path.of(s), logger)) continue;
                }
                folders.add(Path.of(s));
            } else {
                if (!consider_also_hidden_files) {
                    if (Guess_file_type.should_ignore(Path.of(s), logger)) continue;
                }
                files.add(Path.of(s));
            }
        }
        return returned;
    }


    //**********************************************************
    public void inject_search_results(Search_result sr, String keys, boolean is_max, Window window)
    //**********************************************************
    {
        if (path_sets == null) path_sets = new HashMap<>();
        if (search_results == null) search_results = new HashMap<>();
        search_results.put(keys, sr);
        if (search_results_is_max == null) search_results_is_max = new HashMap<>();
        search_results_is_max.put(keys, is_max);
        List<Path> path_set = path_sets.computeIfAbsent(keys, (s) -> new ArrayList<>());
        path_set.add(sr.path());

        //make_one_button(keys, is_max, sr.path(),window);
        for (Path p : path_set) {
            logger.log("plpfsr adding: " + p.toAbsolutePath().toString());
            paths.add(p.toAbsolutePath().toString());
        }
        redraw("inject_search_results");
    }

    //**********************************************************
    public void erase_all_non_max()
    //**********************************************************
    {
        /*
        List<String> to_be_deleted = new ArrayList<>();
        for (String keys : path_sets.keySet())
        {
            Boolean bool = search_results_is_max.get(keys);
            if (bool == null)
            {
                logger.log(Stack_trace_getter.get_stack_trace("SHOULD NOT HAPPEN"));
            }
            else
            {
                if (!bool) to_be_deleted.add(keys);
            }
        }
        for ( String keys : to_be_deleted)
        {
            List<Path> r = path_sets.remove(keys);
            if ( r != null)
            {
                for (Path p : r) {
                    logger.log("plpfsr removing: " + p.toAbsolutePath().toString());

                    paths.remove(p.toAbsolutePath().toString());
                }
            }
        }
        redraw("erase_all_non_max");
        */
    }

    //**********************************************************
    public void has_ended()
    //**********************************************************
    {
        redraw("has_ended");

    }

    //**********************************************************
    private void redraw(String reason)
    //**********************************************************
    {
        logger.log(("plp for sr "+reason));
        redrawer.redraw("path_list_privider_fpr_search_results "+reason);
    }
}
