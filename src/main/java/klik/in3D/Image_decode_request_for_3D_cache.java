// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.in3D;

import javafx.stage.Window;
import klik.util.execute.actor.Aborter;
import klik.util.execute.actor.Message;

import java.nio.file.Path;

//**********************************************************
public class Image_decode_request_for_3D_cache implements Message
//**********************************************************
{
    public final Path path;
    public final Image_cache_cafeine_for_3D cache;
    public final Aborter aborter;
    public final Window owner;

    //**********************************************************
    public Image_decode_request_for_3D_cache(Path path,
                                             Image_cache_cafeine_for_3D cache,
                                             Aborter aborter,
                                             Window owner)
    //**********************************************************
    {
        this.path = path;
        this.cache = cache;
        this.aborter = aborter;
        this.owner = owner;
    }

    //**********************************************************
    public static String get_key(Path path)
    //**********************************************************
    {
        return path.toAbsolutePath().toString();
    }




    //**********************************************************
    public String get_string()
    //**********************************************************
    {
        if ( path == null)         return "path:null";

        return " path:" + path.toAbsolutePath();
    }

    //**********************************************************
    @Override
    public String to_string()
    //**********************************************************
    {
        return "image decoding request for: "+path;
    }

    @Override
    public Aborter get_aborter() {
        return aborter;
    }
}
