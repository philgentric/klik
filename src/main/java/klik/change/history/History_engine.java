package klik.change.history;

import klik.actor.Aborter;
import klik.properties.File_based_IProperties;
import klik.properties.IProperties;
import klik.util.log.Logger;

import java.util.List;

//**********************************************************
public class History_engine
//**********************************************************
{
    private final Properties_for_history ph;
    //**********************************************************
    private History_engine(Aborter aborter, Logger logger)
    //**********************************************************
    {
        IProperties ip = new File_based_IProperties("history",aborter,logger);
        ph = new Properties_for_history(ip,  300, logger);
    }

    //**********************************************************
    public static History_engine get(Aborter aborter, Logger logger)
    //**********************************************************
    {
        return new History_engine(aborter,logger);
    }

    //**********************************************************
    public void add(String s)
    //**********************************************************
    {
        ph.add_and_prune(s);
    }

    //**********************************************************
    public List<History_item> get_all_history_items()
    //**********************************************************
    {
        return ph.get_all_history_items();
    }

    //**********************************************************
    public void clear()
    //**********************************************************
    {
        System.out.println("clearing history");
        ph.clear();
    }
}
