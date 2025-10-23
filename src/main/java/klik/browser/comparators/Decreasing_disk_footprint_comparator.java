package klik.browser.comparators;

import klik.util.execute.actor.Aborter;
import klik.browser.Clearable_RAM_cache;
import klik.Shared_services;
import klik.util.files_and_paths.Sizes;
import klik.util.files_and_paths.Static_files_and_paths_utilities;
import klik.util.log.Logger;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

//**********************************************************
public class Decreasing_disk_footprint_comparator implements Comparator<Path>, Clearable_RAM_cache
//**********************************************************
{
    private final Aborter aborter;
    static Map<Path,Long> disk_foot_prints_cache = new HashMap<>();

    public Decreasing_disk_footprint_comparator(Aborter aborter)
    {
        this.aborter = aborter;
    }

    //**********************************************************
    @Override
    public void clear_RAM_cache()
    //**********************************************************
    {
        disk_foot_prints_cache.clear();
    }

    //**********************************************************
    @Override
    public int compare(Path p1, Path p2)
    //**********************************************************
    {
        long s1 = get_disk_footprint_in_bytes(p1, aborter,Shared_services.logger());
        long s2 = get_disk_footprint_in_bytes(p2, aborter,Shared_services.logger());

        int diff = Long.compare(s2,s1);
        if ( diff != 0) return diff;
        return (p1.toString().compareTo(p2.toString()));
    }

    //**********************************************************
    private static long get_disk_footprint_in_bytes(Path p, Aborter local_aborter, Logger logger)
    //**********************************************************
    {
        Long s = disk_foot_prints_cache.get(p);
        if ( s != null)
        {
            return s;
        }
        if ( p.toFile().isDirectory())
        {
            Sizes sizes = Static_files_and_paths_utilities.get_sizes_on_disk_deep(p,local_aborter, logger);
            s = sizes.bytes();
            //logger.log("get_disk_footprint_in_bytes folder = "+p+" "+s);
        }
        else
        {
            s = p.toFile().length();
            //logger.log("get_disk_footprint_in_bytes file = "+p+" "+s);
        }
        disk_foot_prints_cache.put(p,s);
        return s;
    }
}