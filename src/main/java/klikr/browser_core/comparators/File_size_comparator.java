// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.browser_core.comparators;

import klikr.util.cache.Clearable_RAM_cache;
import klikr.util.cache.RAM_caches;
import klikr.util.cache.Size_;

import java.nio.file.Path;
import java.util.Comparator;

//**********************************************************
public class File_size_comparator implements Comparator<Path>, Clearable_RAM_cache
//**********************************************************
{

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
        double returned = Size_.of_Map(RAM_caches.file_size_cache,Size_.of_Path_F(),Size_.of_Long_F());
        RAM_caches.file_size_cache.clear();
        return returned;
    }

    //**********************************************************
    @Override// Comparator
    public int compare(Path p1, Path p2)
    //**********************************************************
    {
        Integer x = Hidden_files.show_last(p1, p2);
        if (x != null) return x;

        if ( RAM_caches.file_size_cache.containsKey(p1) ) {

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
        Long r = RAM_caches.file_size_cache.get(p);
        if (r != null) return r;
        r = p.toFile().length();
        RAM_caches.file_size_cache.put(p,r);
        return r;
    }


}