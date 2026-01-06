// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.machine_learning.similarity;

import javafx.stage.Window;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Message;

import java.nio.file.Path;

//**********************************************************
public class Similarity_cache_warmer_message implements Message
//**********************************************************
{

    private final Aborter browser_aborter;
    final Path p1;
    private final Window owner;

    //**********************************************************
    public Similarity_cache_warmer_message(Window owner, Aborter browser_aborter, Path p1)
    //**********************************************************
    {
        this.owner = owner;
        this.browser_aborter = browser_aborter;
        this.p1 = p1;
    }
    @Override
    public String to_string() {
        return "Similarity_cache_warmer_message "+p1;
    }

    @Override
    public Aborter get_aborter() {
        return browser_aborter;
    }

    public Window get_owner() { return owner; }

}
