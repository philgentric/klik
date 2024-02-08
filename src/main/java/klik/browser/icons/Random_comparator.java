package klik.browser.icons;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.Random;
import java.util.UUID;

//**********************************************************
public class Random_comparator implements Comparator<Path>
//**********************************************************
{

    long seed;
    public Random_comparator()
    {
        Random r = new Random();
        seed = r.nextLong();
    }
    @Override
    public int compare(Path p1, Path p2) {

        // same aspect ratio so the order must be pseudo random... but consistent for each comparator instance
        long s1 = UUID.nameUUIDFromBytes(p1.getFileName().toString().getBytes()).getMostSignificantBits();
        Long l1 = new Random(seed*s1).nextLong();
        long s2 = UUID.nameUUIDFromBytes(p2.getFileName().toString().getBytes()).getMostSignificantBits();
        Long l2 = new Random(seed*s2).nextLong();
        return l1.compareTo(l2);

    }

};

