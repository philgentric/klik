// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.change.bookmarks;
//SOURCES ../../properties/Properties_with_base.java

import javafx.stage.Window;
import klikr.Shared_services;
import klikr.util.execute.actor.Aborter;
import klikr.properties.File_based_IProperties;
import klikr.properties.IProperties;
import klikr.properties.Properties_with_base;
import klikr.util.log.Logger;

import java.util.List;

//**********************************************************
public class Bookmarks
//**********************************************************
{
    private static Bookmarks instance = null;
    private final Logger logger;
    private final IProperties ip;
    private final Properties_with_base pb;

    //**********************************************************
    public static Bookmarks get(Window owner)
    //**********************************************************
    {
        if ( instance == null) instance = new Bookmarks(owner, Shared_services.aborter(),Shared_services.logger());
        return instance;
    }

    //**********************************************************
    private Bookmarks(Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        this.logger = logger;
        ip = new File_based_IProperties("bookmarks","bookmarks",true, owner, aborter,logger);
        pb = new Properties_with_base(ip,"bookmark_",30,logger);
    }


    //**********************************************************
    public void add(String s)
    //**********************************************************
    {
        pb.add(s);
    }
    //**********************************************************
    public void clear()
    //**********************************************************
    {
        pb.clear();
    }
    //**********************************************************
    public List<String> get_list()
    //**********************************************************
    {
        return pb.get_all();
    }
}
