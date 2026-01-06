// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.util;

import klikr.util.log.Logger;

//**********************************************************
public class Check_remaining_RAM
//**********************************************************
{
    private static final boolean dbg = false;
    public static final long MIN_REMAINING_FREE_MEMORY_10MB = 10_000_000;


    //**********************************************************
    public static boolean RAM_running_low(Logger logger)
    //**********************************************************
    {
        if (get_remaining_memory(logger) < MIN_REMAINING_FREE_MEMORY_10MB) {
            logger.log("\n\nWARNING: running low on memory ! ");
            return true;
        }
        return false;
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
