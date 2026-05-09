// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.images;

import klikr.browser_core.icons.image_properties_cache.Rotation;

import java.util.List;

public record Exif_read_result(String title, List<String> exif_items, Rotation rotation, boolean image_is_damaged) {
}
