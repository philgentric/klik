// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.change.history;
//SOURCES ../../properties/File_storage.java
//SOURCES ../../properties/File_storage_using_Properties.java
//SOURCES ./Properties_for_history.java

import javafx.stage.Window;
import klikr.properties.File_storage;
import klikr.properties.File_storage_using_Properties;
import klikr.util.Shared_services;
import klikr.util.execute.actor.Aborter;
import klikr.util.log.Logger;

import java.util.List;

//**********************************************************
public class History_engine
//**********************************************************
{
    private final Properties_for_history properties_for_history;
    private static volatile History_engine instance;

    //**********************************************************
    public static History_engine get(Window owner)
    //**********************************************************
    {
        if (instance == null)
        {
            synchronized (History_engine.class)
            {
                if (instance == null)
                {
                    instance = new History_engine(owner, Shared_services.aborter(),Shared_services.logger());
                }
            }
        }
        return instance;
    }

    //**********************************************************
    private History_engine(Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        File_storage ip = new File_storage_using_Properties("history","history",false, owner,aborter,logger);
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
