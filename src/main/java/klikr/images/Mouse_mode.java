// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.images;

public enum Mouse_mode {
    pix_for_pix, // 1 image pixel = 1 screen pixel, for images larger than the screen, the mouse can be used to change the part of the picture that is visible
    drag_and_drop, // mouse can be used to move the file in another window/button
    click_to_zoom, // mouse can be used to define the zoomed area
}
