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
    public final boolean with_web_browser;

    //**********************************************************
    public System_open_message(
            boolean with_click_registered_application,
            boolean with_web_browser,
            Application application, Window window, Path path, Aborter aborter, Logger logger)
    //**********************************************************
    {
        this.application = application;
        this.with_web_browser = with_web_browser;
        this.with_click_registered_application = with_click_registered_application;
        this.owner = window;
        this.path = path;
        this.logger = logger;
       this.aborter = aborter;
    }

    //**********************************************************
    @Override
    public String thread_name()
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
