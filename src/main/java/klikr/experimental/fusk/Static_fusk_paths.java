// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.experimental.fusk;

import klikr.util.cache.Cache_folder;
import klikr.util.files_and_paths.Static_files_and_paths_utilities;
import klikr.util.mmap.Mmap;

import java.nio.file.Path;


//**********************************************************
public class Static_fusk_paths
//**********************************************************
{
    private Path fusk_destination = null;
    private Path fusk_source = null;
    private static volatile Static_fusk_paths instance;

    //**********************************************************
    public static Path get_fusk_source()
    //**********************************************************
    {
        if ( instance == null) return null;
        return instance.fusk_source;
    }
    //**********************************************************
    public static Path get_fusk_destination()
    //**********************************************************
    {
        if ( instance == null) return null;
        return instance.fusk_destination;
    }

    //**********************************************************
    public static void set_fusk_destination(Path p)
    //**********************************************************
    {
        if ( instance == null) create_instance();
        instance.fusk_destination = p;
    }
    //**********************************************************
    public static void set_fusk_source(Path p)
    //**********************************************************
    {
        if ( instance == null) create_instance();
        instance.fusk_source = p;
    }
    //**********************************************************
    private static void create_instance()
    //**********************************************************
    {
        if (instance == null)
        {
            synchronized (Static_fusk_paths.class)
            {
                if (instance == null)
                {
                    instance = new Static_fusk_paths();
                }
            }
        }

    }
}
