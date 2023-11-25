package klik.images;

import java.nio.file.Path;

public interface Cache_interface
{
    Image_context get(String key);
    Object put(String key, Image_context value);
    void preload(Image_display_handler image_display_handler, boolean ultimate, boolean forward, boolean high_quality);

    void evict(Path path);

    void clear_all();

    void print();
}
