package klik.browser.comparators;

import java.nio.file.Path;
import java.util.*;

//**********************************************************
public class Random_comparator implements Comparator<Path>
//**********************************************************
{
    // make sure the randomness
    // is consistent during the life time of the comparator
    Map<Path,Long> memory = new HashMap<>();
    long seed;
    public Random_comparator()
    {
        Random r = new Random();
        seed = r.nextLong();
    }
    @Override
    public int compare(Path p1, Path p2) {
        Long l1 = path_to_long(p1);
        Long l2 = path_to_long(p2);
        return l1.compareTo(l2);
    }

    private Long path_to_long(Path p)
    {
        Long l = memory.get(p);
        if ( l == null) {
            long s1 = UUID.nameUUIDFromBytes(p.getFileName().toString().getBytes()).getMostSignificantBits();
            l = new Random(seed * s1).nextLong();
            memory.put(p,l);
        }
        return l;
    }

};

