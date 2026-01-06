// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.path_lists;

import java.util.ArrayList;
import java.util.List;

public class Change
{
    List<Runnable> change_listeners = new ArrayList<>();

    public void add_change_listener(Runnable listener)
    {
        change_listeners.add(listener);
    }
    public  void call_change_listeners()
    {
        for (Runnable r : change_listeners)
        {
            r.run();
        }
    }
}
