// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ./Rotation.java
package klikr.browser.icons.image_properties_cache;


import klikr.util.cache.Size_;
import klikr.util.log.Stack_trace_getter;

//**********************************************************
public record Image_properties(double w, double h, Rotation rotation)
//**********************************************************
{

    //**********************************************************
    public static Long size()
    //**********************************************************
    {
        return 2* Size_.of_Double() +Size_.of_enum(); // an enum is 32 or 24 bytes if compressedOops is enabled which depends on the JVM -Xmx (!!)
    }

    //**********************************************************
    public String to_string()
    //**********************************************************
    {
        return w() + " "+ h()+ " " + rotation();
    }


    //**********************************************************
    public static Image_properties from_string(String value) throws NumberFormatException
    //**********************************************************
    {
        int first_space = value.indexOf(" ");
        if ( first_space == -1) return null;
        int second_space = value.indexOf(" ",first_space+1);
        if ( second_space == -1) return null;
        String ws = value.substring(0,first_space);
        double w = Double.parseDouble(ws.trim());
        String hs = value.substring(first_space, second_space);
        double h = Double.parseDouble(hs.trim());
        String rots = value.substring(second_space+1);
        Rotation rotation = Rotation.valueOf(rots.trim());
        return new Image_properties(w,h, rotation);
    }

    //**********************************************************
    public double get_aspect_ratio()
    //**********************************************************
    {
        if ( rotation() == null)
        {
            System.out.println(Stack_trace_getter.get_stack_trace("❌ PANIC: rotation should never be null"));
            return 1.0;
        }
        switch (rotation())
        {
            case normal,upsidedown ->
            {
                return w/h;
            }
            case rot_90_clockwise, rot_90_anticlockwise ->
            {
            return  h/w;
            }
        }
        return 1.0;
    }

    public Double get_image_width() {
        switch (rotation())
        {
            case normal,upsidedown ->
            {
                return w;
            }
            case rot_90_clockwise, rot_90_anticlockwise -> {
                return  h;
            }
        }
        return null;
    }

    public Double get_image_height() {
        switch (rotation())
        {
            case normal,upsidedown ->
            {
                return h;
            }
            case rot_90_clockwise, rot_90_anticlockwise -> {
                return  w;
            }
        }
        return null;
    }


}
