package klik.browser.comparators;

import klik.machine_learning.similarity.Path_pair;
import klik.machine_learning.similarity.Similarity_cache;

import java.nio.file.Path;
import java.util.List;

//**********************************************************
public record Closest_neighbor(Path p1, Path closest, double dist)
//**********************************************************
{

    //**********************************************************
    public static Closest_neighbor find_closest_of(Path p1, List<Path> images, Similarity_cache similarity_cache)
    //**********************************************************
    {
        double min = Double.MAX_VALUE;
        Path closest = null;
        for (Path p2 : images)
        {
            if (p1.equals(p2)) continue;
            Double d = similarity_cache.get(Path_pair.get(p1, p2));
            if (d == null) {
                // typically means the similarity is above the THRESHOLD
                //logger.log(" no similarity for "+p1+" and "+p2);
                continue;
            }
            if (d < min)
            {
                min = d;
                closest = p2;
            }
        }
        if (closest == null) return null;
        Closest_neighbor cn = new Closest_neighbor(p1, closest, min);
        return cn;
    }
}

