package klik.actor;


import klik.util.Logger;
import klik.util.Stack_trace_getter;

import java.util.concurrent.atomic.AtomicBoolean;

// a container for aborting background threads
// when using virtual threads, this is not needed as we have a thread to interrupt
// but with workers, we need to be able to interrupt them
// aborter is a thread safe way to signal any piece of background running coode that it should abort
// in klik a typical usage is when the user selects a different folder from browsing:
// all background tasks that are relevant to that folder should be aborted
// this includes icon fabrication, searches, disk scans for sizes etc
// however backup tasks MUST NOT be aborted, so they use a different aborter

//**********************************************************
public class Aborter
//**********************************************************
{
    private final static boolean dbg = false;
    public final String name;
    private AtomicBoolean abort = new AtomicBoolean(false);
    private final Logger logger;

    //**********************************************************
    public Aborter(String name, Logger logger)
    //**********************************************************
    {
        this.name = name;
        this.logger = logger;
    }


    //**********************************************************
    public void abort()
    //**********************************************************
    {
        //if ( dbg) logger.log(Stack_trace_getter.get_stack_trace("abort "+name));
        if ( dbg) logger.log(("Aborter, aborting this: "+name));
        abort.set(true);
    }
    //**********************************************************
    public boolean should_abort()
    //**********************************************************
    {
        if ( dbg) if( abort.get()) logger.log(Stack_trace_getter.get_stack_trace("should abort "+name));
        return abort.get();
    }
}
