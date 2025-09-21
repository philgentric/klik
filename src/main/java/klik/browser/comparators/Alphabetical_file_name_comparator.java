//SOURCES ../Clearable_RAM_cache.java
package klik.browser.comparators;

import klik.browser.Clearable_RAM_cache;

import java.nio.file.Path;
import java.util.Comparator;

//**********************************************************
public class Alphabetical_file_name_comparator implements Comparator<Path>, Clearable_RAM_cache
//**********************************************************
{
    @Override
    public int compare(Path f1, Path f2)
    {
        int diff = f1.getFileName().toString().compareToIgnoreCase(f2.getFileName().toString());
        if (diff != 0) return diff;
        // in case the file names differ by case
        return f1.getFileName().toString().compareTo(f2.getFileName().toString());
    }

    @Override
    public void clear_RAM_cache() {}
}
