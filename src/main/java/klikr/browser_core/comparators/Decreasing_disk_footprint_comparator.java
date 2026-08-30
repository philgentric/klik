// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.browser_core.comparators;

import javafx.stage.Window;
import klikr.util.P2S;
import klikr.util.cache.RAM_caches;
import klikr.util.cache.Size_;
import klikr.util.execute.actor.Aborter;
import klikr.util.cache.Clearable_RAM_cache;
import klikr.util.Shared_services;
import klikr.util.files_and_paths.Sizes;
import klikr.util.files_and_paths.Static_files_and_paths_utilities;
import klikr.util.log.Logger;

import java.nio.file.Path;
import java.util.Comparator;

//**********************************************************
public class Decreasing_disk_footprint_comparator implements Comparator<Path>, Clearable_RAM_cache
//**********************************************************
{
    private final Aborter aborter;
    private final Window owner;

    //**********************************************************
    public Decreasing_disk_footprint_comparator(Aborter aborter, Window owner)
    //**********************************************************
    {
        this.aborter = aborter;
        this.owner = owner;
    }

    //**********************************************************
    @Override
    public double clear_RAM()
    //**********************************************************
    {
        double returned = Size_.of_Map(RAM_caches.folder_total_size_cache,Size_.of_String_F(),Size_.of_Long_F());
        RAM_caches.folder_total_size_cache.clear();
        return returned;
    }

    //**********************************************************
    @Override
    public int compare(Path p1, Path p2)
    //**********************************************************
    {
        Integer x = Hidden_files.show_last(p1, p2);
        if (x != null) return x;

        long s1 = get_disk_footprint_in_bytes(p1, aborter,owner,Shared_services.logger());
        long s2 = get_disk_footprint_in_bytes(p2, aborter,owner,Shared_services.logger());

        int diff = Long.compare(s2,s1);
        if ( diff != 0) return diff;
        return (p1.toString().compareTo(p2.toString()));
    }

    //**********************************************************
    private static long get_disk_footprint_in_bytes(Path p, Aborter local_aborter, Window owner, Logger logger)
    //**********************************************************
    {
        Long s = RAM_caches.folder_total_size_cache.get(P2S.p2s(p));
        if ( s != null)
        {
            return s;
        }
        if ( p.toFile().isDirectory())
        {
            Sizes sizes = Static_files_and_paths_utilities.get_sizes_on_disk_deep_concurrent(p,local_aborter, owner,logger);
            s = sizes.bytes();
            //logger.log("get_disk_footprint_in_bytes folder = "+p+" "+s);
        }
        else
        {
            s = p.toFile().length();
            //logger.log("get_disk_footprint_in_bytes file = "+p+" "+s);
        }
        RAM_caches.folder_total_size_cache.put(P2S.p2s(p),s);
        return s;
    }
}