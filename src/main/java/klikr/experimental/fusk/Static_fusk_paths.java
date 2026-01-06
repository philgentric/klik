// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.experimental.fusk;

import java.nio.file.Path;


//**********************************************************
public class Static_fusk_paths
//**********************************************************
{
    private Path fusk_destination = null;
    private Path fusk_source = null;
    private static Static_fusk_paths instance;

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
        instance = new Static_fusk_paths();
    }
}
