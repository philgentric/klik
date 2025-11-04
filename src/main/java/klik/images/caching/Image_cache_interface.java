// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.images.caching;

import javafx.stage.Window;
import klik.images.Image_context;
import klik.images.Image_display_handler;

import java.nio.file.Path;

//**********************************************************
public interface Image_cache_interface
//**********************************************************
{
    Image_context get(String key);
    void put(String key, Image_context value);
    void preload(
            Image_display_handler image_display_handler,
            boolean ultimate,
            boolean forward);
    void evict(Path path, Window owner);
    void clear_all();
    void print();

}
