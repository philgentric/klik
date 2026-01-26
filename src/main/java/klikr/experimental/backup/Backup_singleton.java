// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.experimental.backup;
//SOURCES ./Backup_engine.java

import javafx.stage.Window;
import klikr.change.bookmarks.Bookmarks;
import klikr.util.Shared_services;
import klikr.util.log.Logger;
import klikr.util.ui.Popups;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

//**********************************************************
public class Backup_singleton
//**********************************************************
{
    private static volatile Backup_singleton instance;
    Logger logger;
    Path source;
    Path destination;
    private final List<Backup_engine> engines = new ArrayList<>();

    //**********************************************************
    public static void set_source(Path source_, Logger logger_)
    //**********************************************************
    {
        Backup_singleton b = get_instance(logger_);
        b.source = source_;
    }
    //**********************************************************
    public static void set_destination(Path destination_, Logger logger_)
    //**********************************************************
    {
        Backup_singleton b = get_instance(logger_);
        b.destination = destination_;
    }
    //**********************************************************
    public static boolean start_the_backup(Window owner)
    //**********************************************************
    {
        if ( instance == null) return false;
        instance.start(owner);
        return true;
    }

    //**********************************************************
    private void start(Window owner)
    //**********************************************************
    {
        Iterator<Backup_engine> it = engines.iterator();
        while ( it.hasNext())
        {
            Backup_engine e = it.next();
            if ( e.is_finished()) it.remove();
            else
            {
                if (e.source.equals(source))
                {
                    if (e.destination.equals(destination)) {
                        Popups.popup_warning("❗ A backup like this is already running", "Sorry: denied",true,owner,logger);
                        return;
                    }
                }
            }
        }

        {
// Get a CONFIRMATION
            String header = "❗ Copy Confirmation Required";
            String content = "This will copy all the files down from directory:\n" + source.toAbsolutePath() + "\n"
                    + "Into the directory:\n" + destination.toAbsolutePath() + "\n"
                    + "(this is safe because files with same names, if different, will be renamed)\n"
                    + "Are you sure you want to do that ?";

            if (!Popups.popup_ask_for_confirmation( header, content, owner,logger)) return;
        }
        boolean deep = false;
        {
        String header = "How deep should the file identity checks be?";
        String content = "Deep means= check every byte matches, cancel means = not deep, check just names and file sizes";

        deep = Popups.popup_ask_for_confirmation(header, content, owner,logger);
        }
        logger.log("backup deep = "+deep);
        Backup_engine b = new Backup_engine(source, destination, logger);
        b.go(deep,owner);
        engines.add(b);
    }

    //**********************************************************
    public static void abort()
    //**********************************************************
    {
        if ( instance==null) return;
        instance.abort_now();
    }

    //**********************************************************
    private void abort_now()
    //**********************************************************
    {
        for ( Backup_engine e : engines)
        {
            logger.log("CANCEL for "+e.to_string());
            e.abort();
        }
    }



    //**********************************************************
    private static Backup_singleton get_instance(Logger logger_)
    //**********************************************************
    {
        if (instance == null)
        {
            synchronized (Backup_singleton.class)
            {
                if (instance == null)
                {
                    instance = new Backup_singleton(logger_);
                }
            }
        }

        return instance;
    }
    //**********************************************************
    private Backup_singleton(Logger logger_)
    //**********************************************************
    {
        logger = logger_;
    }


}
