package klik.images.caching;

import javafx.stage.Window;
import klik.images.Image_context;
import klik.images.Image_display_handler;

import java.nio.file.Path;

public interface Cache_interface
{
    Image_context get(String key);
    void put(String key, Image_context value);
    void preload(Image_display_handler image_display_handler, boolean ultimate, boolean forward, Window owner);
    void evict(Path path, Window owner);
    void clear_all();
    void print();
}
