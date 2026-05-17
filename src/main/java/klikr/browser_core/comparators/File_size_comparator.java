// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.browser_core.comparators;

import javafx.stage.Window;
import klikr.util.Shared_services;
import klikr.util.cache.Clearable_RAM_cache;
import klikr.util.cache.Size_;
import klikr.util.execute.actor.Aborter;
import klikr.util.files_and_paths.Sizes;
import klikr.util.files_and_paths.Static_files_and_paths_utilities;
import klikr.util.log.Logger;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

//**********************************************************
public class File_size_comparator implements Comparator<Path>, Clearable_RAM_cache
//**********************************************************
{
    static Map<Path,Long> file_sizes_cache = new HashMap<>();

    //**********************************************************
    public File_size_comparator()
    //**********************************************************
    {
    }

    //**********************************************************
    @Override
    public double clear_RAM()
    //**********************************************************
    {
        double returned = Size_.of_Map(file_sizes_cache,Size_.of_Path_F(),Size_.of_Long_F());
        file_sizes_cache.clear();
        return returned;
    }

    //**********************************************************
    @Override// Comparator
    public int compare(Path p1, Path p2)
    //**********************************************************
    {
        Integer x = Hidden_files.show_last(p1, p2);
        if (x != null) return x;

        if ( file_sizes_cache.containsKey(p1) ) {

        }

        if ( p1.toFile().isDirectory() ) return -1;
        if ( p2.toFile().isDirectory() ) return -1;

        long s1 = from_cache(p1);
        long s2 = from_cache(p2);

        int diff = Long.compare(s2,s1);
        if ( diff != 0) return diff;
        return (p1.toString().compareTo(p2.toString()));
    }

    //**********************************************************
    private long from_cache(Path p)
    //**********************************************************
    {
        Long r = file_sizes_cache.get(p);
        if (r != null) return r;
        r = p.toFile().length();
        file_sizes_cache.put(p,r);
        return r;
    }


}