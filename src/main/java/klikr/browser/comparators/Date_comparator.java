// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.browser.comparators;

import klikr.util.cache.Clearable_RAM_cache;
import klikr.util.image.decoding.Fast_date_from_filesystem;
import klikr.util.log.Logger;

import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;

//**********************************************************
public record Date_comparator(Logger logger) implements Comparator<Path>
//**********************************************************
{
    //**********************************************************
    @Override
    public int compare(Path p1, Path p2)
    //**********************************************************
    {
        FileTime ldt1 = Fast_date_from_filesystem.get_date(p1, logger);
        FileTime ldt2 = Fast_date_from_filesystem.get_date(p2, logger);
        int diff = ldt2.compareTo(ldt1);
        if (diff != 0) return diff;
        return (p1.toString().compareTo(p2.toString()));
    }
}