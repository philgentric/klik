// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.change.history;

import klik.change.undo.Undo_for_moves;
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

    /*
    //**********************************************************
    public boolean monitor()
    //**********************************************************
    {
        Undo_for_moves.erase_if_too_old(1000, 100, logger);
        History_engine.get(logger).erase_if_too_old(100);
        return true;
    }
*/


}
