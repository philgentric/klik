// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.util.perf;

import klik.util.execute.actor.Actor_engine;
import klik.util.log.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;

//**********************************************************
public class Perf implements AutoCloseable
//**********************************************************
{
    private static final ConcurrentHashMap<String, LongAdder> durations = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, LongAdder> count = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, LongAccumulator> max = new ConcurrentHashMap<>();
    private String tag;
    private long start;
    private static volatile boolean enabled = false;
    private static volatile boolean new_values = false;

    //**********************************************************
    public Perf(String tag)
    //**********************************************************
    {
        if ( !enabled) return;
        this.tag = tag;
        start =  System.nanoTime();
    }

    //**********************************************************
    @Override
    public void close() //throws Exception
    //**********************************************************
    {
        if ( !enabled) return;
        new_values = true;
        long dur = System.nanoTime()-start;
        durations.computeIfAbsent(tag,k ->new LongAdder()).add(dur);
        count.computeIfAbsent(tag,k ->new LongAdder()).add(1);
        max.computeIfAbsent(tag,k->new LongAccumulator(Long::max,Long.MIN_VALUE)).accumulate(dur);
    }

    //**********************************************************
    public static void print_all(Logger logger)
    //**********************************************************
    {
        logger.log("======= Perfs ===========");
        List<String> tags = new ArrayList<>(durations.keySet());
        Collections.sort(tags);
        for (String tag : tags)
        {
            double ave = durations.get(tag).doubleValue()/count.get(tag).doubleValue();
            double v = max.get(tag).doubleValue();
            if ( v > 1_000_000_000) tag = "❌ "+ tag;
            logger.log(tag+":\n   max= "+format(v)+"\n   ave= "+format(ave));
        }
        logger.log("=========================");
    }

    //**********************************************************
    private static String format(double time)
    //**********************************************************
    {
        if ( time > 1_000_000_000)
        {
            return String.format("%.1f",time/1_000_000_000)+"s  ";
        }
        if ( time > 1_000_000)
        {
            return String.format("%.1f",time/1_000_000)+"ms ";
        }
        if ( time > 1_000)
        {
            return String.format("%.1f",time/1_000)+"us ";
        }
        return String.format("%.1f",time)+"ns ";

    }
    //**********************************************************
    public static void monitor(Logger logger)
    //**********************************************************
    {
        enabled = true;
        Actor_engine.execute(()->{
            for(;;)
            {
                try {
                    if( new_values)
                    {
                        print_all(logger);
                        new_values = false;
                    }
                    Thread.sleep(5_000);
                } catch (Exception e) {
                    logger.log("PERF exception" + e);
                }
            }
        },"Performance monitoring",logger);
    }
}
