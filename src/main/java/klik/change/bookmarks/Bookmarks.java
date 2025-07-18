package klik.change.bookmarks;
//SOURCES ../../properties/Properties_with_base.java

import javafx.stage.Window;
import klik.actor.Aborter;
import klik.properties.File_based_IProperties;
import klik.properties.IProperties;
import klik.properties.Properties_with_base;
import klik.util.log.Logger;

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
    private Bookmarks(Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        this.logger = logger;
        ip = new File_based_IProperties("bookmarks",owner, aborter,logger);
        pb = new Properties_with_base(ip,"bookmark_",30,logger);
    }

    //**********************************************************
    public static Bookmarks get(Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        if ( instance == null) instance = new Bookmarks(owner,aborter,logger);
        return instance;
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
