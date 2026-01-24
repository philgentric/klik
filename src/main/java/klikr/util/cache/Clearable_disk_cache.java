// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.util.cache;

import javafx.stage.Window;
import klikr.util.execute.actor.Aborter;
import klikr.util.log.Logger;

//**********************************************************
public interface Clearable_disk_cache
//**********************************************************
{
    Disk_cleared clear_disk(Window owner, Aborter aborter, Logger logger);

    String name();
}
