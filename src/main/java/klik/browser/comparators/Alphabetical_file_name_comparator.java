package klik.browser.comparators;

import java.nio.file.Path;
import java.util.Comparator;

//**********************************************************
public class Alphabetical_file_name_comparator implements Comparator<Path>
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
};
