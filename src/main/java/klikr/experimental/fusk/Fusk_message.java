// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.experimental.fusk;

import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Message;

import java.io.File;

//**********************************************************
public class Fusk_message implements Message
//**********************************************************
{
    public final File destination_folder;
    public final File target_dir;
    public final Aborter aborter;
    //**********************************************************
    public Fusk_message(File target_dir_, File destination_folder, Aborter aborter)
    //**********************************************************
    {
        this.destination_folder = destination_folder;
        target_dir = target_dir_;
        this.aborter = aborter;
    }
    //**********************************************************
    @Override
    public String to_string()
    //**********************************************************
    {
        return "Fusk_message: "+target_dir;
    }

    @Override
    public Aborter get_aborter() {
        return aborter;
    }
}
