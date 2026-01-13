// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.path_lists;

import klikr.browser.virtual_landscape.Path_comparator_source;
import klikr.util.execute.actor.Aborter;
import klikr.properties.boolean_features.Feature;
import klikr.properties.boolean_features.Feature_cache;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;
import klikr.util.perf.Perf;

import java.nio.file.Path;
import java.util.*;

//**********************************************************
class State
//**********************************************************
{
    private final static boolean dbg = false;
    private final Map<Path, Integer> path_to_index;
    private final Map<Integer,Path> index_to_path;
    private final Logger logger;
    private final Aborter aborter;
    private final Path_list_provider path_list_provider;
    private final Path_comparator_source path_comparator_source;
    private final Type type;
    //**********************************************************
    public State(Type type, Path_list_provider path_list_provider, Path_comparator_source path_comparator_source, Aborter aborter,Logger logger)
    //**********************************************************
    {
        this.logger = logger;
        this.aborter = aborter;
        this.path_list_provider = path_list_provider;
        this.type = type;
        this.path_comparator_source = path_comparator_source;
        path_to_index = new HashMap<>();
        index_to_path = new HashMap<>();
        Actor_engine.execute(()->rescan("constructor"),"Indexer rescan",logger);
    }
    //**********************************************************
    public synchronized int how_many_images()
    //**********************************************************
    {
        return index_to_path.size();
    }
    //**********************************************************
    public synchronized void rescan(String reason)
    //**********************************************************
    {
        try ( Perf perf = new Perf("State::rescan "+reason)) {
            //long start = System.currentTimeMillis();
            //logger.log(Stack_trace_getter.get_stack_trace("image file source scan"));

            List<Path> path_list = null;
            switch (type) {
                case images ->
                        path_list = path_list_provider.only_image_paths(Feature_cache.get(Feature.Show_hidden_files));
                case songs ->
                        path_list = path_list_provider.only_song_paths(Feature_cache.get(Feature.Show_hidden_files));
                case all_files ->
                        path_list = path_list_provider.only_file_paths(Feature_cache.get(Feature.Show_hidden_files));
            }
            if (path_list == null) {
                logger.log(Stack_trace_getter.get_stack_trace("rescan failed"));
                return;
            }

            if ( path_comparator_source == null)
            {
                logger.log(Stack_trace_getter.get_stack_trace("path_comparator_source == null"));
            }
            else
            {
                Comparator<Path> comp = path_comparator_source.get_path_comparator();
                if ( comp ==null)
                {
                    logger.log(Stack_trace_getter.get_stack_trace("comp == null"));
                }
                else
                {
                    try
                    {
                        path_list.sort(comp);
                    }
                    catch (IllegalArgumentException e)
                    {
                        logger.log("" + e);
                    }
                }
            }

            index_to_path.clear();
            path_to_index.clear();


            int index = 0;
            for (Path p : path_list) {
                index_to_path.put(index, p);
                path_to_index.put(p, index);
                index++;
            }
        }
    }

    public Integer index_from_path(Path path) {
        return path_to_index.get(path);
    }

    public Path path_from_index(int i) {
        return index_to_path.get(i);
    }
}
