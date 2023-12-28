package klik.change.history;

import klik.actor.Aborter;
import klik.change.undo.Undo_engine;
import klik.util.Logger;

//**********************************************************
public class History_auto_clean
//**********************************************************
{
    private static final boolean dbg = false;
    private static final long AGE_LIMIT_IN_DAYS = 2;
    public final Logger logger;
    public final Aborter aborter;
    private volatile boolean warning_issued = false;


    //**********************************************************
    public History_auto_clean(Aborter aborter_, Logger logger_)
    //**********************************************************
    {
        aborter= aborter_;
        logger = logger_;
    }

    //**********************************************************
    public boolean monitor()
    //**********************************************************
    {
        Undo_engine.erase_if_too_old(1000,100,logger);
        History_engine.erase_if_too_old(100,logger);
        return true;
    }



}
