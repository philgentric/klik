package klik.actor;


import klik.util.log.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

// Aborter is a thread safe way to signal any piece of background running code
// that it should abort
// in Klik, a typical usage is when the user selects a different folder from browsing:
// all background tasks that are relevant to that folder should be aborted
// this includes icon fabrication, searches, disk scans for sizes etc
// all these will check the aborter of THAT browser window instance,
// and when leaving that browser window (or changing dir) the aborter will be triggered
// Some tasks like backup tasks MUST NOT be aborted when the user changes dir
// or closes a browsing window so they must use a specific aborter

//**********************************************************
public class Aborter
//**********************************************************
{
    private final static boolean dbg = false;
    public final String name;
    private AtomicBoolean abort = new AtomicBoolean(false);
    private final Logger logger;
    public String reason;

    //**********************************************************
    public Aborter(String name, Logger logger)
    //**********************************************************
    {
        this.name = name;
        this.logger = logger;
    }


    //**********************************************************
    public void abort(String reason_)
    //**********************************************************
    {
        reason = reason_;
        //if ( dbg) logger.log(Stack_trace_getter.get_stack_trace("abort "+name));
        if ( dbg) logger.log(("Aborter, aborting this: "+name+" because: "+reason));
        abort.set(true);
    }
    //**********************************************************
    public boolean should_abort()
    //**********************************************************
    {
        //if ( dbg) if( abort.get()) logger.log(Stack_trace_getter.get_stack_trace("should abort "+name));
        if ( dbg) if( abort.get()) logger.log(("should abort "+name+" because: "+reason));
        return abort.get();
    }
}
