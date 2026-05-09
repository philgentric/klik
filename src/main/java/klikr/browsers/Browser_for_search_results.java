// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.browsers;

import javafx.scene.paint.Color;
import javafx.stage.Window;
import klikr.Window_builder;
import klikr.browser_core.Abstract_browser;
import klikr.browser_core.Window_manager;
import klikr.browser_core.virtual_landscape.Redrawer;
import klikr.path_lists.Path_list_provider;
import klikr.path_lists.Path_list_provider_for_file_system;
import klikr.path_lists.Path_list_provider_for_playlist;
import klikr.path_lists.Path_list_provider_for_search_results;
import klikr.search.Results;
import klikr.search.Search_result;
import klikr.util.execute.actor.Aborter;
import klikr.change.old_and_new.Old_and_new_Path;
import klikr.util.log.Logger;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

//**********************************************************
public class Browser_for_search_results extends Abstract_browser implements Results
//**********************************************************
{
    public Path_list_provider_for_search_results path_list_provider_for_search_results;

    //**********************************************************
    public Browser_for_search_results(Window_builder window_builder, Logger logger)
    //**********************************************************
    {
        super(Color.BEIGE, logger);
        logger.log("Browser_for_search_results\n");
        if (window_builder.path_list_provider instanceof Path_list_provider_for_file_system)
        {
            logger.log("Browser_for_search_results FATAL, need a Path_list_provider_for_search_results\n");
            return;
        }
        if (window_builder.path_list_provider instanceof Path_list_provider_for_playlist)
        {
            logger.log("Browser_for_search_results FATAL, need a Path_list_provider_for_search_results\n");
            return;
        }
        aborter = new Aborter("Abstract_browser for: " + get_name(), logger);

        path_list_provider_for_search_results = (Path_list_provider_for_search_results) window_builder.path_list_provider;
        Redrawer r = origin -> virtual_landscape.redraw_fx(true,"AbstractBrowser for: " + get_name()+" "+origin, true);
        path_list_provider_for_search_results.set_redrawer(r);

        logger.log("Browser_for_search_results created with path_list_provider: " + path_list_provider_for_search_results.get_key());


        init_abstract_browser(window_builder, this, "song_playlist",aborter);

        my_Stage.the_Stage.setOnCloseRequest(event ->
            {
                Window_manager.unregister(ID,logger);
            });
    }


    //**********************************************************    @Override
    protected String get_name()
    //**********************************************************
    {
        return "Browser_for_search_results" ;
    }

    //**********************************************************    @Override
    protected String get_path_for_history()
    //**********************************************************
    {
        return get_Path_list_provider().get_key();
    }


    //*******************************************************
    @Override // File_comparator_provider
    public Comparator<? super Path> get_file_comparator()
    //*******************************************************
    {
        return virtual_landscape.other_file_comparator;
    }

    //**********************************************************
    @Override
    protected Path_list_provider get_Path_list_provider()
    //**********************************************************
    {
        return path_list_provider_for_search_results;
    }

    //**********************************************************
    @Override
    protected String signature()
    //**********************************************************
    {
        return "";
    }

    //**********************************************************
    @Override
    protected void monitor_current_path_list_source()
    //**********************************************************
    {
        logger.log("Browser_for_search_results monitor_current_path_list_source NOT IMPLEMENTED");
    }

    //**********************************************************
    @Override
    public void set_title()
    //**********************************************************
    {
        my_Stage.the_Stage.setTitle("Search results (this is NOT a folder!)");

    }

    //**********************************************************
    @Override // Change_receiver
    public void you_receive_this_because_a_file_event_occurred_somewhere(List<Old_and_new_Path> l, Window owner, Logger logger)
    //**********************************************************
    {
        logger.log("Browser_for_search_results you_receive_this_because_a_file_event_occurred_somewhere "+ l);
        virtual_landscape.redraw_fx(true,"change received",false);
    }

    //**********************************************************
    @Override
    public String get_Change_receiver_string()
    //**********************************************************
    {
        return "";
    }

    //**********************************************************
    @Override // Results
    public void inject_search_results(Search_result sr, String keys, boolean is_max, Window window)
    //**********************************************************
    {
        path_list_provider_for_search_results.inject_search_results(sr,keys,is_max,window);
    }

    //**********************************************************
    @Override // Results
    public void erase_all_non_max()
    //**********************************************************
    {
        path_list_provider_for_search_results.erase_all_non_max();
    }

    //**********************************************************
    @Override  // Results
    public void has_ended()
    //**********************************************************
    {
        path_list_provider_for_search_results.has_ended();

    }
}
