// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.browser.virtual_landscape;

public enum Selection_state {
    nothing_selected,
    selection_started,
    selection_defined
}

/*
state is a strict loop
nothing_selected
==> mouse starts to select (drawing a box)
selection_started
==> mouse is release
selection_defined
==>  drag can start ... and when drop is executed or cancelled
nothing_selected
 */