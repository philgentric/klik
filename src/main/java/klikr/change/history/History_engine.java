// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.change.history;
//SOURCES ../../properties/IProperties.java
//SOURCES ../../properties/File_based_IProperties.java
//SOURCES ./Properties_for_history.java

import javafx.stage.Window;
import klikr.Shared_services;
import klikr.util.execute.actor.Aborter;
import klikr.properties.File_based_IProperties;
import klikr.properties.IProperties;
import klikr.util.log.Logger;

import java.util.List;

//**********************************************************
public class History_engine
//**********************************************************
{
    private final Properties_for_history properties_for_history;
    private static History_engine instance;

    //**********************************************************
    public static History_engine get(Window owner)
    //**********************************************************
    {
        if ( instance ==null)
        {
            instance = new History_engine(owner, Shared_services.aborter(),Shared_services.logger());
        }
        return instance;
    }

    //**********************************************************
    private History_engine(Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        IProperties ip = new File_based_IProperties("history","history",true, owner,aborter,logger);
        properties_for_history = new Properties_for_history(ip,  300, logger);
    }

    //**********************************************************
    public void add(String s)
    //**********************************************************
    {
        properties_for_history.add_and_prune(s);
    }

    //**********************************************************
    public List<History_item> get_all_history_items()
    //**********************************************************
    {
        return properties_for_history.get_all_history_items();
    }

    //**********************************************************
    public void clear()
    //**********************************************************
    {
        System.out.println("clearing history");
        properties_for_history.clear();
    }

    //**********************************************************
    public String get_back()
    //**********************************************************
    {
        return properties_for_history.get_back();
    }
}
