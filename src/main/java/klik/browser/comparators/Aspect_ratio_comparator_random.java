package klik.browser.comparators;

import klik.browser.Clearable_RAM_cache;
import klik.browser.icons.image_properties_cache.Image_properties;
import klik.browser.icons.image_properties_cache.Image_properties_RAM_cache;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

//**********************************************************
public class Aspect_ratio_comparator_random implements Comparator<Path>, Clearable_RAM_cache
//**********************************************************
{
    private final long seed;
    // make sure the comparator is consistent
    private final HashMap<Path,Long> cache_local = new HashMap<>();
    private final Image_properties_RAM_cache image_properties_ram_cache;

    //**********************************************************
    public Aspect_ratio_comparator_random(Image_properties_RAM_cache image_properties_ram_cache)
    //**********************************************************
    {
        this.image_properties_ram_cache = image_properties_ram_cache;
        Random r = new Random();
        seed = r.nextLong();
    }

    //**********************************************************
    @Override
    public void clear_RAM_cache()
    //**********************************************************
    {
        cache_local.clear();
        image_properties_ram_cache.clear_cache();
    }
    //**********************************************************
    @Override
    public int compare(Path p1, Path p2)
    //**********************************************************
    {
        Image_properties ip1 = image_properties_ram_cache.get_from_cache(p1,null);
        if ( ip1 == null)
        {
            //logger.log(Stack_trace_getter.get_stack_trace("PANIC image_property not found"));
            return 0;
        }
        Double d1 = ip1.get_aspect_ratio();
        Image_properties ip2 = image_properties_ram_cache.get_from_cache(p2,null);
        if ( ip2 == null)
        {
            //logger.log(Stack_trace_getter.get_stack_trace("PANIC image_property not found"));
            return 0;
        }
        Double d2 = ip2.get_aspect_ratio();

        int diff = d1.compareTo(d2);
        if (diff != 0) return diff;

        Long l1 = cache_local.get(p1);
        if ( l1 == null) {
            // same aspect ratio so the order must be pseudo random... but consistent for each comparator instance
            long s1 = UUID.nameUUIDFromBytes(p1.getFileName().toString().getBytes()).getMostSignificantBits();
            l1 = new Random(seed * s1).nextLong();
            cache_local.put(p1,l1);
        }

        Long l2 = cache_local.get(p2);
        if ( l2 == null) {
            // same aspect ratio so the order must be pseudo random... but consistent for each comparator instance
            long s2 = UUID.nameUUIDFromBytes(p2.getFileName().toString().getBytes()).getMostSignificantBits();
            l2 = new Random(seed * s2).nextLong();
            cache_local.put(p2, l2);
        }

        return l1.compareTo(l2);
    }
}
