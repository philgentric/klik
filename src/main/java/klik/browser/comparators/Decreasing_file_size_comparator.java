package klik.browser.comparators;

import java.nio.file.Path;
import java.util.Comparator;

//**********************************************************
public class Decreasing_file_size_comparator implements Comparator<Path>
//**********************************************************
{
    @Override
    public int compare(Path p1, Path p2)
    {
        int diff = Long.compare(p2.toFile().length(), p1.toFile().length());
        if ( diff != 0) return diff;
        return (p1.toString().compareTo(p2.toString()));
    }
};