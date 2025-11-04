// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.util.execute.actor;


import klik.util.log.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

// Aborter is a thread safe way to signal any piece of
// background running code that it should stop
// in Klik, a typical usage is when the user selects a different folder from browsing:
// all background tasks that are relevant to that folder should be aborted
// this includes icon fabrication, searches, disk scans for sizes etc
// all these will check the aborter of THAT browser window instance,
// and when leaving that browser window (or changing dir) the aborter will be triggered
// Some tasks like backup tasks SHOULD NOT be aborted when the user changes dir
// or closes a browsing window so they must use a specific aborter

//**********************************************************
public class Aborter
//**********************************************************
{
    private final static boolean dbg = false;
    public final String name;
    private AtomicBoolean abort = new AtomicBoolean(false);
    private final Logger logger;
    private String reason;
    private Runnable on_abort;

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

    //**********************************************************
    public String reason()
    //**********************************************************
    {
        return reason;
    }

    /*
    optionally, one can specify a Runnable that will be called
    by calling  aborter.on_abort()

    this is a trick to "carry" a runnable in the Aborter
    the alternative being to add the runnable as an additional parameter...

    if (aborter.should_abort()) {
                    logger.log("path manager aborting (1) scan_list "+ path_list_provider.get_folder_path().toAbsolutePath());
                    aborter.on_abort();
                    return;
                }
     */
    //**********************************************************
    public void add_on_abort(Runnable r)
    //**********************************************************
    {
        on_abort = r;
    }

    //**********************************************************
    public void on_abort()
    //**********************************************************
    {
        if ( on_abort != null) on_abort.run();
    }

}
