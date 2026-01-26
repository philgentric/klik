// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ./System_open_message.java
//SOURCES ./Registered_applications.java
package klikr.util.execute;

import javafx.stage.Window;
import klikr.Klikr_application;
import klikr.properties.Store_engine;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Actor;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.execute.actor.Message;
import klikr.util.files_and_paths.Extensions;
import klikr.util.ui.Jfx_batch_injector;
import klikr.util.log.Logger;
import klikr.util.ui.Popups;
import klikr.util.log.Stack_trace_getter;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

//**********************************************************
public class System_open_actor implements Actor
//**********************************************************
{
    private static volatile System_open_actor instance;

    //**********************************************************
    public static void open_with_system(
            Path path,
            Window window,
            Aborter aborter,
            Logger logger)
    //**********************************************************
    {
    Actor_engine.run(
            System_open_actor.get(),
            new System_open_message(false, Klikr_application.application,window, path, aborter,logger),null,logger);
    }


    //**********************************************************
    @Override
    public String name()
    //**********************************************************
    {
        return "System_open_actor";
    }


    //**********************************************************
    private static System_open_actor get()
    //**********************************************************
    {
        if (instance == null)
        {
            synchronized (System_open_actor.class)
            {
                if (instance == null)
                {
                    instance = new System_open_actor();
                }
            }
        }
        return instance;
    }


    //**********************************************************
    public static void open_with_click_registered_application(
            Path path, Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        logger.log("open_with_click_registered_application " + path);
        Actor_engine.run(
                System_open_actor.get(),
                new System_open_message(true, Klikr_application.application, owner, path, aborter,logger),null,logger);
    }


    //**********************************************************
    @Override
    public String run(Message m)
    //**********************************************************
    {
        System_open_message som = (System_open_message) m;
        if (som.with_click_registered_application) return with_click_registered_application(som);
        try
        {
            ((System_open_message) m).logger.log("going to call showDocument for "+som.path);
            som.application.getHostServices().showDocument(som.path.toUri().toString());
            //Desktop.getDesktop().open(som.path.toAbsolutePath().toFile());
        }
        catch (Exception e)
        {
            som.logger.log(Stack_trace_getter.get_stack_trace("❗ open failed :" + e));

            if (e.toString().contains("doesn't exist."))
            {
                Jfx_batch_injector.inject(() -> Popups.popup_warning( "❗ Failed", "Your OS/GUI could not open this file, the error is:\n" + e,false,som.owner,som.logger), som.logger);
            }
            else
            {
                Jfx_batch_injector.inject(() -> Popups.popup_warning( "❗ Failed", "Your OS/GUI could not open this file, the error is:\n" + e + "\nMaybe it is just not properly configured e.g. most often the file extension has to be registered?",false,som.owner,som.logger), som.logger);
            }
        }
        return null;
    }


    //**********************************************************
    private String with_click_registered_application(System_open_message som)
    //**********************************************************
    {
        String extension = Extensions.get_extension(som.path.toFile().getName());
        String app = Registered_applications.get_registered_application(extension, som.owner, som.aborter,som.logger);

        if ( app == null)
        {
            som.logger.log("❌ open with_click_registered_application aborted, no registered operation for this extension ");
            return "error";
        }
        som.logger.log("❗ open with click registered application for " + som.path + " with " + app);

        call_macOS_open(som, app);
        //call_exec(som, app));

        return "ok";
    }

    //**********************************************************
    private boolean call_macOS_open(System_open_message som, String app)
    //**********************************************************
    {
        Jfx_batch_injector.inject(() -> Popups.popup_warning( "❗ Calling macOS to open: "+som.path, "Please wait ",true,som.owner,som.logger), som.logger);

        List<String> list = new ArrayList<>();
        list.add("open");
        list.add(som.path.toFile().getAbsolutePath());
        list.add("-a");
        list.add(app);

        if (som.aborter.should_abort())
        {
            som.logger.log("open with macOS aborted");
            return false;
        }
        // Output file is empty
        StringBuilder sb = new StringBuilder();
        File wd = som.path.toFile().getParentFile();
        Execute_result res = Execute_command.execute_command_list(list, wd, 2000, sb, som.logger);
        if ( !res.status())
        {
            som.logger.log("open with macOS failed:\n"+ sb +"\n\n\n");
            return false;
        }
        som.logger.log("\n\n\n open with macOS output :\n"+ sb +"\n\n\n");
        return true;
    }

}
