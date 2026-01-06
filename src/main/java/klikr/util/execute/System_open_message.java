// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.util.execute;

import javafx.application.Application;
import javafx.stage.Window;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Message;
import klikr.util.log.Logger;

import java.nio.file.Path;

//**********************************************************
public class System_open_message implements Message
//**********************************************************
{

    public final Application application;
    public final Window owner;
    public final Path path;
    public final Logger logger;
    public final Aborter aborter;
    public final boolean with_click_registered_application;
    //**********************************************************
    public System_open_message(boolean special, Application application, Window window, Path path, Aborter aborter, Logger logger)
    //**********************************************************
    {
        this.application = application;
        this.with_click_registered_application = special;
        this.owner = window;
        this.path = path;
        this.logger = logger;
       this.aborter = aborter;
    }

    //**********************************************************
    @Override
    public String to_string()
    //**********************************************************
    {
        return "System.open for " + path;
    }

    //**********************************************************
    @Override
    public Aborter get_aborter() {
        return aborter;
    }
    //**********************************************************

}
