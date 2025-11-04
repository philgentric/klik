// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ./State.java
package klik.path_lists;

import klik.util.execute.actor.Aborter;
import klik.properties.Non_booleans_properties;
import klik.util.log.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/*
The role of the indexer is to always display files in a given order (e.g. alphabetic, or file size etc)
for this we maintain a list of paths and the reverse map

use cases are:
- "next" with 2 sub-cases: (+1) or (jump forward to next file that has <this characteristic>)
- "previous with 2 sub-cases: (-1) or (jump backward to next file that has <this characteristic>)

difficulties arise because
- a file may be renamed (move same folder, different file name)
- a file may be moved OUT (to a different folder, which includes disappearing from the trash folder)
- a file may be moved IN (from a different folder, which includes moving to the trash folder)
- a file may be deleted ( like a real "not trash folder" deletion)

Any of this can be from 2 different origins:
- internally: from actions generated in THIS instance of klik (but a different browser instance typically?)
- "behind our back" the file system may be changing, for example the user has another file manager open
 (or another instance of klik) and is renaming/moving/deleting etc

In both cases, browser windows and image windows CAN get warned thanks to the change gang
- internal action : the change gang tells us... if the action included sending an event!
- external actions : a FFilesystem_item_modification_watcher can be used ... OR ... we discover the problem because of an error!

typical scenario is:
- we display image #23
- behind the scene image #24 is deleted
- user selects "next"
- we increment 23 into 24
- we get the path from the list
- ERROR when reading: file does not exist
- we increment to 25 and retry ....
(eventually if the full directory has been erased/moved, we will end up with no image...)


 */

//**********************************************************
public class Indexer
//**********************************************************
{
    public final static boolean dbg = false;
    private final Logger logger;

    private final State state;

    //**********************************************************
    public static Indexer get_Image_indexer(Type type, Path_list_provider path_list_provider, Comparator<? super Path> file_comparator, Aborter aborter, Logger logger_)
    //**********************************************************
    {
        return new Indexer(type, path_list_provider, file_comparator, aborter, logger_);
    }

    //**********************************************************
    private Indexer(Type type, Path_list_provider path_list_provider, Comparator<? super Path> fileComparator, Aborter aborter, Logger l)
    //**********************************************************
    {
        logger = l;
        //current_dir = dir_;
        state = new State(type, path_list_provider,fileComparator, aborter,logger);
    }

    //**********************************************************
    public Path get_new_path_relative(Path previous_path, int delta, boolean ultimate)
    //**********************************************************
    {
        int target = 0;
        Integer current_index = state.index_from_path(previous_path);//.toAbsolutePath());
        if ( dbg) logger.log("path_to_index("+previous_path+")="+current_index);
        if ( current_index == null)
        {
            if ( dbg) logger.log("unknown path:" + previous_path);
            state.rescan("get_new_path_relative failed");
            current_index = state.index_from_path(previous_path);
            if ( current_index == null)
            {
                logger.log("OHO: Indexer does not know the path:" + previous_path);
            }
        }
        else
        {
            target = current_index + delta;
            if ( dbg) logger.log( "new index :"+target+"="+current_index+"+"+delta);
            if (target < 0) {
                if ( dbg) logger.log("This is before start: i < 0");
                target = state.how_many_images() - 1;
            }
            if (target >= state.how_many_images())
            {
                if (state.how_many_images() == 0) {
                    logger.log("❌ FATAL: path_list.size()=" + state.how_many_images());
                    return null;
                }
                target = 0;
            }
        }

        /*
        ok so now we have a target INDEX...
        but it does not mean there is (still) a corresponding file,
        especially since we may also be looking for an ULTIM one
        */
        for (int max = 0; max < 2*state.how_many_images();max++)
        {
            Path returned = state.path_from_index(target);
            if ( dbg) logger.log("index_to_path("+target+")="+returned);
            if (Files.exists(returned))
            {
                if (ultimate)
                {
                    if (!returned.getFileName().toString().toLowerCase().contains(Non_booleans_properties.ULTIM) )
                    {
                        target = increment(target);
                        continue;
                    }
                }
                // OK!
                if ( dbg) logger.log("returning path="+returned);
                return returned;
            }
            else
            {
                if ( dbg) logger.log("file does not exist anymore :"+returned+" ... rescan !");
                state.rescan("A file does not exists anymore");
            }
            target = increment(target);
        }
        logger.log("FAILED for "+previous_path+" delta="+delta);
        // FAILED!
        return null;
    }

    //**********************************************************
    private int increment(int index)
    //**********************************************************
    {
        index++;
        if ( dbg) logger.log("checking new index="+index);
        if (index >= state.how_many_images())
        {
            if ( dbg) logger.log("This is the beyond the end: i=" + index + " >= path_list.size()=" + state.how_many_images());
            index = 0;
        }
        return index;
    }

    //**********************************************************
    public List<Path> get_paths(Path start, int how_many_preload_to_request, boolean forward, boolean ultimate)
    //**********************************************************
    {
        List<Path> returned = new ArrayList<>();
        Integer start_index = state.index_from_path(start);
        if ( start_index == null) return returned;
        if ( forward)
        {
            for (int i = start_index; i < start_index + how_many_preload_to_request; i++)
            {
                Path p = state.path_from_index(i);
                add_on_conditions(ultimate, returned, p);
            }
        }
        else
        {
            for (int i = start_index; i > start_index - how_many_preload_to_request; i--)
            {
                Path p = state.path_from_index(i);
                add_on_conditions(ultimate, returned, p);
            }
        }
        return returned;
    }

    //**********************************************************
    private static void add_on_conditions(boolean ultimate, List<Path> returned, Path p)
    //**********************************************************
    {
        if (p == null) return;
        if (ultimate)
        {
            if (!p.getFileName().toString().toLowerCase().contains(Non_booleans_properties.ULTIM)) return;
        }
        returned.add(p);
    }

    //**********************************************************
    public boolean distance_larger_than(int max_distance, Path ref, Path other)
    //**********************************************************
    {
        Integer i1 = state.index_from_path(ref);
        if ( i1 == null)
        {
            state.rescan("distance_larger_than error 1");
            return true;
        }
        Integer i2 = state.index_from_path(other);
        if ( i2 == null)
        {
            state.rescan("distance_larger_than error 2");
            return true;
        }

        int diff = i2-i1;
        if ( diff < 0 ) diff = -diff;
        if ( diff > max_distance) return true;
        return false;
    }

    //**********************************************************
    public boolean is_known(Path path)
    //**********************************************************
    {
        if ( state.index_from_path(path) == null) return false;
        return true;
    }

    //**********************************************************
    public int how_many_images()
    //**********************************************************
    {
        return state.how_many_images();
    }

    //**********************************************************
    public void signal_deleted_file(Path to_be_deleted)
    //**********************************************************
    {
        state.rescan("signal_deleted_file");
    }

    //**********************************************************
    public void signal_file_copied()
    //**********************************************************
    {
        state.rescan("signal_file_copied");
    }

    //**********************************************************
    public int get_index(Path path)
    //**********************************************************
    {
        Integer i =  state.index_from_path(path);
        if ( i == null) return -1;
        return i;
    }

    public void rescan(String reason) {
        state.rescan(reason);
    }

    public int get_max() {
        return state.how_many_images();
    }

    public Path path_from_index(int i) {
        return state.path_from_index(i);
    }


}
