// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ./Finder_frame.java
package klik.search;

import javafx.stage.Window;
import klik.util.execute.actor.Aborter;
import klik.browser.virtual_landscape.Path_comparator_source;
import klik.path_lists.Path_list_provider;
import klik.util.log.Logger;

import java.util.List;

//**********************************************************
public class Finder
//**********************************************************
{
    //**********************************************************
    public static void find(
            Path_list_provider path_list_provider,
            Path_comparator_source path_comparator_source,
            List<String> keywords,
            boolean search_only_images,
            Aborter aborter,
            Window owner,
            Logger logger)
    //**********************************************************
    {
        Finder_frame popup = new Finder_frame(
                    keywords,
                    search_only_images,
                    path_list_provider,
                    path_comparator_source,
                    aborter,
                    owner,
                    logger);
            popup.start_search();
    }
}
