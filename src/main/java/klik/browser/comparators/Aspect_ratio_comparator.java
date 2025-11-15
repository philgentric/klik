// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.browser.comparators;

import klik.browser.Clearable_RAM_cache;
import klik.browser.icons.image_properties_cache.Image_properties;
import klik.browser.icons.image_properties_cache.Image_properties_RAM_cache;
import klik.util.execute.actor.Aborter;

import java.nio.file.Path;
import java.util.Comparator;

//**********************************************************
public record Aspect_ratio_comparator(
        Image_properties_RAM_cache image_properties_ram_cache, Aborter aborter) implements Comparator<Path>, Clearable_RAM_cache
//**********************************************************
{
    //**********************************************************
    public Aspect_ratio_comparator
    //**********************************************************
    {
        System.out.println("Aspect_ratio_comparator");
    }

    //**********************************************************
    @Override
    public void clear_RAM_cache()
    //**********************************************************
    {
        image_properties_ram_cache.clear_RAM_cache();
    }


    //**********************************************************
    @Override
    public int compare(Path p1, Path p2)
    //**********************************************************
    {
        Image_properties ip1 = image_properties_ram_cache.get(p1, aborter, null);
        if (ip1 == null) {
            //logger.log(Stack_trace_getter.get_stack_trace("PANIC image_property not found"));
            return 0;
        }
        Double d1 = ip1.get_aspect_ratio();
        Image_properties ip2 = image_properties_ram_cache.get(p2, aborter,null);
        if (ip2 == null) {
            //logger.log(Stack_trace_getter.get_stack_trace("PANIC image_property not found"));
            return 0;
        }
        Double d2 = ip2.get_aspect_ratio();
        int diff = d1.compareTo(d2);
        if (diff != 0) return diff;
        return (p1.getFileName().toString().compareTo(p2.getFileName().toString()));
    }
}
