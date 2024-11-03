package klik.browser.comparators;

import klik.browser.Clearable_cache;

import java.nio.file.Path;
import java.util.Comparator;

//**********************************************************
public class Decreasing_file_size_comparator implements Comparator<Path>, Clearable_cache
//**********************************************************
{

    @Override
    public void clear_RAM_cache() {

    }

    @Override
    public int compare(Path p1, Path p2)
    {
        int diff = Long.compare(p2.toFile().length(), p1.toFile().length());
        if ( diff != 0) return diff;
        return (p1.toString().compareTo(p2.toString()));
    }
};