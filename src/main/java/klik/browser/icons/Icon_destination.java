// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.browser.icons;

import klik.browser.Image_and_properties;
import klik.browser.items.Iconifiable_item_type;

import java.nio.file.Path;

public interface Icon_destination
{
    void receive_icon(Image_and_properties icon);

    Iconifiable_item_type get_item_type();

    Path get_path_for_display_icon_destination();

    String get_string();

    Path get_item_path();

    boolean get_icon_fabrication_requested(); // this is to prevent more than 1 request per icon

    void set_icon_fabrication_requested(boolean b);
}
