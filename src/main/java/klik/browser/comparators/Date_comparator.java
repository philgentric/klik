package klik.browser.comparators;

import klik.images.decoding.Fast_date_from_OS;
import klik.util.log.Logger;

import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;

//**********************************************************
public class Date_comparator implements Comparator<Path>
//**********************************************************
{
    private final Logger logger;

    public Date_comparator(Logger logger) {
        this.logger = logger;
    }


    //**********************************************************
    @Override
    public int compare(Path p1, Path p2)
    //**********************************************************
    {
        FileTime ldt1 = Fast_date_from_OS.get_date(p1,logger);
        FileTime ldt2 = Fast_date_from_OS.get_date(p2,logger);
        int diff= ldt2.compareTo(ldt1); // most recent first
        if ( diff != 0) return diff;
        return (p1.toString().compareTo(p2.toString()));
    }
};