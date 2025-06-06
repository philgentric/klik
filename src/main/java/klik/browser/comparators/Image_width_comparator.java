package klik.browser.comparators;

import klik.browser.Clearable_RAM_cache;
import klik.browser.icons.image_properties_cache.Image_properties;
import klik.browser.icons.image_properties_cache.Image_properties_RAM_cache;

import java.nio.file.Path;
import java.util.Comparator;

//**********************************************************
public class Image_width_comparator implements Comparator<Path>, Clearable_RAM_cache
{
    private final Image_properties_RAM_cache image_properties_ram_cache;

    public Image_width_comparator(Image_properties_RAM_cache image_properties_ram_cache) {
        this.image_properties_ram_cache = image_properties_ram_cache;
    }


    @Override
    public void clear_RAM_cache() {

    }

    @Override
    public int compare(Path p1, Path p2) {
        Image_properties ip1 = image_properties_ram_cache.get_from_cache(p1,null);
        if ( ip1 == null)
        {
            //logger.log(Stack_trace_getter.get_stack_trace("PANIC image_property not found"));
            return 0;
        }
        Double d1 = ip1.get_image_width();
        if ( d1 == null) return 0;
        Image_properties ip2 = image_properties_ram_cache.get_from_cache(p2,null);
        if ( ip2 == null)
        {
            //logger.log(Stack_trace_getter.get_stack_trace("PANIC image_property not found"));
            return 0;
        }
        Double d2 = ip2.get_image_width();
        if ( d2 == null) return 0;

        int diff =  d1.compareTo(d2);
        if ( diff != 0) return diff;
        return (p1.getFileName().toString().compareTo(p2.getFileName().toString()));
    }
}
