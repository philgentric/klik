// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.in3D;

public interface Image_source
{
    Image_and_path get(int i);

    int how_many_items();
}
