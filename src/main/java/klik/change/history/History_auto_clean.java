package klik.change.history;

import klik.actor.Aborter;
import klik.change.undo.Undo_engine;
import klik.util.log.Logger;

//**********************************************************
public class History_auto_clean
//**********************************************************
{
    public final Logger logger;
    private volatile boolean warning_issued = false;


    //**********************************************************
    public History_auto_clean(Logger logger_)
    //**********************************************************
    {
        logger = logger_;
    }

    //**********************************************************
    public boolean monitor()
    //**********************************************************
    {
        Undo_engine.erase_if_too_old(1000,100, logger);
        History_engine.erase_if_too_old(100,logger);
        return true;
    }



}
