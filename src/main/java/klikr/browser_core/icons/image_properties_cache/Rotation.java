// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.browser_core.icons.image_properties_cache;

//**********************************************************
public enum Rotation
//**********************************************************
{
    normal, // 0
    rot_90_clockwise,
    upsidedown, // 180
    rot_90_anticlockwise;


    //**********************************************************
    public double as_double()
    //**********************************************************
    {
        switch (this)
        {
            case normal -> {
                return 0.0;
            }
            case rot_90_clockwise -> {
                return 90.0;
            }
            case rot_90_anticlockwise -> {
                return 270.0;
            }
            case upsidedown -> {
                return 180.0;
            }
        }
        return 0.0;
    }


    //**********************************************************
    public byte as_byte()
    //**********************************************************
    {
        switch (this)
        {
            case normal -> {
                return 0;
            }
            case rot_90_clockwise -> {
                return 1;
            }
            case rot_90_anticlockwise -> {
                return 2;
            }
            case upsidedown -> {
                return 3;
            }
        }
        return 0;
    }

    //**********************************************************
    public static Rotation from_byte(byte r)
    //**********************************************************
    {
        switch (r)
        {
            case 0:
                return normal;
            case 1:
                return rot_90_clockwise;
            case 2:
                return rot_90_anticlockwise;
            case 3:
                return upsidedown;
        }
        return normal;
    }

}