// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.browser.comparators;

import klikr.util.cache.Clearable_RAM_cache;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;

//**********************************************************
public record Last_access_comparator(Logger logger) implements Comparator<Path>
//**********************************************************
{

    //**********************************************************
    @Override
    public int compare(Path p1, Path p2)
    //**********************************************************
    {
        BasicFileAttributeView bfav1 = Files.getFileAttributeView(p1, BasicFileAttributeView.class);
        BasicFileAttributeView bfav2 = Files.getFileAttributeView(p2, BasicFileAttributeView.class);
        try
        {
            FileTime ft1 = bfav1.readAttributes().lastAccessTime();
            FileTime ft2 = bfav2.readAttributes().lastAccessTime();
            int diff = ft2.compareTo(ft1); // most recent first
            if (diff != 0)
            {
                return diff;
            }
            return (p1.toString().compareTo(p2.toString()));
        }
        catch (IOException e)
        {
            logger().log(Stack_trace_getter.get_stack_trace(""+e));
            return p1.getFileName().compareTo(p2.getFileName());
        }

    }

    //**********************************************************
    public static void set_last_access(Path p, Logger logger)
    //**********************************************************
    {
        Actor_engine.execute(()->set_last_access_in_a_thread(p,logger),"set_last_access_in_a_thread",logger);
    }

    //**********************************************************
    public static void set_last_access_in_a_thread(Path p, Logger logger)
    //**********************************************************
    {
        FileTime ft = FileTime.fromMillis(System.currentTimeMillis());
        BasicFileAttributeView bfav = Files.getFileAttributeView(p, BasicFileAttributeView.class);
        try {
            bfav.setTimes(null, ft, null);
            //logger.log("set_last_access: "+p+" "+ft);
        } catch (IOException e) {
            logger.log(""+e);
        }

    }

}