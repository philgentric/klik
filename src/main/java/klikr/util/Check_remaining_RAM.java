// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.util;

import javafx.stage.Window;
import klikr.util.cache.RAM_caches;
import klikr.util.log.Logger;
import klikr.util.ui.Popups;

import java.util.concurrent.atomic.AtomicBoolean;

//**********************************************************
public class Check_remaining_RAM
//**********************************************************
{
    private static final boolean dbg = false;
    public static final long MIN_REMAINING_FREE_MEMORY_MB = 300_000_000;
    private static Long last_time_oom_was_shown = null;

    public static AtomicBoolean low_memory = new AtomicBoolean(false);

    //**********************************************************
    public static boolean RAM_running_low(String message, Window owner, Logger logger)
    //**********************************************************
    {
        long before = get_remaining_memory(logger);
        if ( before > MIN_REMAINING_FREE_MEMORY_MB) return false;
        RAM_caches.clear_all_RAM_caches(owner, logger);
        System.gc();
        long after = get_remaining_memory(logger);
        low_memory.set(true);


        logger.log(message+" Garbage Collector was called, AVAILABLE: "+before/1000_000L + " => "+after/1000_000L);
        if (after > MIN_REMAINING_FREE_MEMORY_MB) return false;

        logger.log("❌ Your java VM machine is running out of RAM!\n"+message+"\nIncrease max in Preferences?");
        boolean show_pop_up = false;
        if  (last_time_oom_was_shown == null)
        {
            show_pop_up = true;
            last_time_oom_was_shown = System.currentTimeMillis();
        }
        else
        {
            long now = System.currentTimeMillis();
            // show popup again (max every 10 minutes)
            if (last_time_oom_was_shown - now > 600_000) show_pop_up = false;
        }
        if ( show_pop_up) Popups.popup_warning("Running out of RAM","Your java VM machine is running out of RAM!\nclose some windows and/or increase max in Preferences", false,owner, logger);

        return true;
    }
    //**********************************************************
    public static long get_remaining_memory(Logger logger)
    //**********************************************************
    {
        //https://stackoverflow.com/questions/3571203/what-are-runtime-getruntime-totalmemory-and-freememory
        Runtime runtime = Runtime.getRuntime();
        if (dbg) logger.log("VM ALLOCATED memory "+(runtime.totalMemory()/1_000_000)+" MB");
        long used = runtime.totalMemory() - runtime.freeMemory();
        if (dbg) logger.log("VM USED memory "+(used/1_000_000)+" MB");
        long max = runtime.maxMemory(); // configured max memory for the JVM i.e. -Xmx
        if (dbg) logger.log("VM configured with max memory "+(max/1_000_000)+" MB");
        long remaining = max - used;
        if (dbg) logger.log("VM remaining memory "+(remaining/1_000_000)+" MB");
        return remaining;
    }

}
