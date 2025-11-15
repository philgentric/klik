// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.search;

import javafx.stage.Window;
import klik.util.execute.actor.Aborter;
import klik.util.execute.actor.Message;

//**********************************************************
public class Finder_message implements Message
//**********************************************************
{
    public final Callback_for_file_found_publish callback;
    public final Aborter aborter;
    public final Window owner;
    public final String extension;
    Search_config search_config;

    //**********************************************************
    public Finder_message(Search_config search_config, Callback_for_file_found_publish callback, Aborter aborter, Window owner)
    //**********************************************************
    {
        this.owner = owner;
        this.search_config = search_config;
        if ( search_config.extension() == null)
        {
            this.extension = null;
        }
        else
        {
            if ( search_config.extension().isBlank())
            {
                this.extension = null;
            }
            else
            {
                this.extension = search_config.extension().toLowerCase();
            }
        }
        this.callback = callback;
       // the_browser = the_browser_;
        this.aborter = aborter;
    }

    //**********************************************************
    @Override
    public String to_string()
    //**********************************************************
    {
        return "Finder : "+search_config.path();
    }

    //**********************************************************
    @Override
    public Aborter get_aborter()
    //**********************************************************
    {
        return aborter;
    }
}
