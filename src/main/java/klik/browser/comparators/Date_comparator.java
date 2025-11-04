// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.browser.comparators;

import klik.browser.Clearable_RAM_cache;
import klik.util.image.decoding.Fast_date_from_filesystem;
import klik.util.log.Logger;

import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;

//**********************************************************
public record Date_comparator(Logger logger) implements Comparator<Path>, Clearable_RAM_cache
//**********************************************************
{
    @Override
    public void clear_RAM_cache() {}

    //**********************************************************
    @Override
    public int compare(Path p1, Path p2)
    //**********************************************************
    {
        FileTime ldt1 = Fast_date_from_filesystem.get_date(p1, logger);
        FileTime ldt2 = Fast_date_from_filesystem.get_date(p2, logger);
        int diff = ldt1.compareTo(ldt2); // oldest first
        if (diff != 0) return diff;
        return (p1.toString().compareTo(p2.toString()));
    }
}