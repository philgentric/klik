// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.util.execute.actor;

import klikr.util.log.Logger;

/*
sometimes something should stop based on 2 (or more) conditions
for example a request may be cancelable (via a button in the UI)
and would stop to make sense when the visited folder is closed
 */

//**********************************************************
public class Or_aborter extends Aborter
//**********************************************************
{
    private final Aborter a;
    private final Aborter b;

    //**********************************************************
    public Or_aborter(Aborter a, Aborter b, Logger logger)
    //**********************************************************
    {
        super("Or_aborter", logger);
        this.a = a;
        this.b = b;
    }

    //**********************************************************
    @Override
    public boolean should_abort()
    //**********************************************************
    {
        if ( a == null )
        {
            if ( b == null ) return false;
            return b.should_abort();
        }
        if ( b == null ) return a.should_abort();
        return a.should_abort() || b.should_abort();
    }

    //**********************************************************
    @Override
    public String reason()
    //**********************************************************
    {
        if (a.should_abort()) return a.reason();
        if (b.should_abort()) return b.reason();
        return a.reason()+ " or " +b.reason();
    }

}