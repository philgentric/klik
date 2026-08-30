// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.browser_core.comparators;

import klikr.util.P2S;

import java.nio.file.Path;
import java.util.*;

//**********************************************************
public class Random_comparator implements Comparator<Path>
//**********************************************************
{
    // make sure the randomness
    // is consistent during the life time of the comparator
    Map<String,Long> memory = new HashMap<>();
    long seed;
    public Random_comparator()
    {
        Random r = new Random();
        seed = r.nextLong();
    }
    @Override
    public int compare(Path p1, Path p2) {
        Integer x = Hidden_files.show_last(p1, p2);
        if (x != null) return x;

        Long l1 = path_to_long(p1);
        Long l2 = path_to_long(p2);
        return l1.compareTo(l2);
    }

    private Long path_to_long(Path p)
    {
        Long l = memory.get(P2S.p2s(p));
        if ( l == null) {
            long s1 = UUID.nameUUIDFromBytes(p.getFileName().toString().getBytes()).getMostSignificantBits();
            l = new Random(seed * s1).nextLong();
            memory.put(P2S.p2s(p),l);
        }
        return l;
    }

};

