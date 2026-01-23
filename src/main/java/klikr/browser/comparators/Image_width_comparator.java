// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.browser.comparators;

import javafx.stage.Window;
import klikr.util.cache.Clearable_RAM_cache;
import klikr.browser.icons.image_properties_cache.Image_properties;
import klikr.util.cache.Klikr_cache;
import klikr.util.execute.actor.Aborter;

import java.nio.file.Path;
import java.util.Comparator;

//**********************************************************
public record Image_width_comparator(
        Klikr_cache<Path, Image_properties> image_properties_cache,
        Aborter aborter, Window owner)
        implements Comparator<Path>
//**********************************************************
{

    //**********************************************************
    @Override
    public int compare(Path p1, Path p2)
    //**********************************************************
    {
        Image_properties ip1 = image_properties_cache.get(p1, aborter,null, owner);
        if (ip1 == null) {
            //logger.log(Stack_trace_getter.get_stack_trace("PANIC image_property not found"));
            return 0;
        }
        Double d1 = ip1.get_image_width();
        if (d1 == null) return 0;
        Image_properties ip2 = image_properties_cache.get(p2, aborter,null, owner);
        if (ip2 == null) {
            //logger.log(Stack_trace_getter.get_stack_trace("PANIC image_property not found"));
            return 0;
        }
        Double d2 = ip2.get_image_width();
        if (d2 == null) return 0;

        int diff = d1.compareTo(d2);
        if (diff != 0) return diff;
        return (p1.getFileName().toString().compareTo(p2.getFileName().toString()));
    }
}
