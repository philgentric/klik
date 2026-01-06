// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.images.caching;

import javafx.stage.Window;
import klikr.browser.Clearable_RAM_cache;
import klikr.images.Image_context;
import klikr.images.Image_display_handler;

import java.nio.file.Path;

//**********************************************************
public interface Image_cache_interface extends Clearable_RAM_cache
//**********************************************************
{
    Image_context get(String key);
    void put(String key, Image_context value);
    void preload(
            Image_display_handler image_display_handler,
            boolean ultimate,
            boolean forward);
    void evict(Path path, Window owner);
    //void clear_R();
    void print();

}
