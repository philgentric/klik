// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.browsers.browser_core.comparators;

import klikr.util.Kontext;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.log.Stack_trace_getter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;

//**********************************************************
public record Last_access_comparator(Kontext context) implements Comparator<Path>
//**********************************************************
{

    //**********************************************************
    @Override
    public int compare(Path p1, Path p2)
    //**********************************************************
    {
        Integer x = Hidden_files.show_last(p1, p2);
        if (x != null) return x;

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
            context().log_with_stack_trace_from_throwable("",e);
            return p1.getFileName().compareTo(p2.getFileName());
        }

    }

    //**********************************************************
    public static void set_last_access(Path p, Kontext context)
    //**********************************************************
    {
        Actor_engine.execute(()->set_last_access_in_a_thread(p,context),"set_last_access_in_a_thread",context.logger());
    }

    //**********************************************************
    public static void set_last_access_in_a_thread(Path p, Kontext context)
    //**********************************************************
    {
        FileTime ft = FileTime.fromMillis(System.currentTimeMillis());
        BasicFileAttributeView bfav = Files.getFileAttributeView(p, BasicFileAttributeView.class);
        try {
            bfav.setTimes(null, ft, null);
        } catch (IOException e) {
            //context.log_with_stack_trace_from_throwable("",e);
            throw  new UncheckedIOException(e);
        }

    }

}