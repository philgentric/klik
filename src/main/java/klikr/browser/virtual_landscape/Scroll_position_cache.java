package klikr.browser.virtual_landscape;

import klikr.util.cache.Clearable_RAM_cache;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class Scroll_position_cache implements Clearable_RAM_cache
{
    public final static Map<String, Path> scroll_position_cache = new HashMap<>();

    //**********************************************************
    @Override
    public void clear_RAM()
    //**********************************************************
    {
        scroll_position_cache.clear();
    }


    //**********************************************************
    public static void scroll_position_cache_write(Path folder_path, Path top_left_item_path)
    //**********************************************************
    {
        if ( top_left_item_path != null) scroll_position_cache.put(folder_path.toAbsolutePath().toString(), top_left_item_path);
    }

    //**********************************************************
    public static void scroll_position_cache_clear()
    //**********************************************************
    {
        scroll_position_cache.clear();
    }

    //**********************************************************
    public static Path scroll_position_cache_read(Path folder_path)
    //**********************************************************
    {
        return scroll_position_cache.get(folder_path.toAbsolutePath().toString());
    }
}
