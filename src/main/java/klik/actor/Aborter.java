package klik.actor;


import klik.util.Logger;
import klik.util.Stack_trace_getter;

import java.util.concurrent.atomic.AtomicBoolean;

// a container for aborting background tasks
//**********************************************************
public class Aborter
//**********************************************************
{
    private final static boolean dbg = true;
    private final String name;
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
        if ( dbg) logger.log(("abort "+name));
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
