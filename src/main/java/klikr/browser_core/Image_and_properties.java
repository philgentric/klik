// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.browser_core;

import javafx.scene.image.Image;
import javafx.stage.Window;
import klikr.browser_core.icons.image_properties_cache.Image_properties;
import klikr.browser_core.icons.image_properties_cache.Rotation;
import klikr.look.Jar_utils;
import klikr.util.log.Logger;

//**********************************************************
public record Image_and_properties(Image image, Image_properties properties)
//**********************************************************
{
    //**********************************************************
    public static Image_and_properties build(Image image, boolean is_broken_icon)
    //**********************************************************
    {
        return new Image_and_properties(image,new Image_properties(image.getWidth(),image.getHeight(), Rotation.normal,is_broken_icon));
    }

    //**********************************************************
    public static Image_and_properties broken(Window owner, Logger logger)
    //**********************************************************
    {
        Image b = Jar_utils.get_broken_icon(300,owner,logger);
        if ( b == null) return null;
        return Image_and_properties.build(b,true);
    }
}