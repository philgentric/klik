package klik.search;

import klik.util.execute.actor.Aborter;
import klik.util.execute.actor.Message;

//**********************************************************
public class Finder_message implements Message
//**********************************************************
{
    public final Callback_for_file_found_publish callback;
    //public final Browser the_browser;
    public final Aborter aborter;
    public final String extension;
    Search_config search_config;

    //**********************************************************
    public Finder_message(Search_config search_config, Callback_for_file_found_publish callback_, Aborter aborter_)//, Browser the_browser_)
    //**********************************************************
    {
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
        callback = callback_;
       // the_browser = the_browser_;
        aborter = aborter_;
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
