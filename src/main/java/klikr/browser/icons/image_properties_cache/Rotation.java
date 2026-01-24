// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.browser.icons.image_properties_cache;

//**********************************************************
public enum Rotation
//**********************************************************
{
    normal, // 0
    rot_90_clockwise,
    upsidedown, // 180
    rot_90_anticlockwise;


    //**********************************************************
    public static double to_angle(Rotation rotation)
    //**********************************************************
    {
        switch (rotation)
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

}