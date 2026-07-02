// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ./System_open_message.java
//SOURCES ./Registered_applications.java
package klikr.util.execute;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.stage.Window;
import klikr.Klikr_application;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Actor;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.execute.actor.Message;
import klikr.util.files_and_paths.Extensions;
import klikr.util.ui.Jfx_batch_injector;
import klikr.util.log.Logger;
import klikr.util.ui.Popups;
import klikr.util.log.Stack_trace_getter;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

//**********************************************************
public class System_open_actor implements Actor
//**********************************************************
{
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(System_open_actor.class);
    private static volatile System_open_actor instance;

    //**********************************************************
    public static void open_with_system(
            Application application,
            Path path,
            Window window,
            Aborter aborter,
            Logger logger)
    //**********************************************************
    {
    Actor_engine.run(
            System_open_actor.get(),
            new System_open_message(false,false, application,window, path, aborter,logger),null,logger);
    }

    //**********************************************************
    public static void open_with_web_browser(
            Application application,
            Path path,
            Window window,
            Aborter aborter,
            Logger logger)
    //**********************************************************
    {
        Actor_engine.run(
                System_open_actor.get(),
                new System_open_message(false,true, application,window, path, aborter,logger),null,logger);
    }

    //**********************************************************
    public static void open_with_registered_application(
            Path path, Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        logger.log("open_with_click_registered_application " + path);
        Actor_engine.run(
                System_open_actor.get(),
                new System_open_message(true, false, Klikr_application.application, owner, path, aborter,logger),null,logger);
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
    @Override
    public String run(Message m)
    //**********************************************************
    {
        System_open_message som = (System_open_message) m;
        if (som.with_click_registered_application)
        {
            return with_registered_application(som);
        }

        if ( som.with_web_browser)
        {
            return open_with_web_browser(som);
        }
        // default:
        call_os_specific_open(som,null);

        return null;
    }

    //**********************************************************
    private static String open_with_web_browser(System_open_message som)
    //**********************************************************
    {
        try
        {
            som.logger.log("going to call showDocument for "+ som.path);
            som.application.getHostServices().showDocument(som.path.toUri().toString());
            // dont use this, it is AWT
            //Desktop.getDesktop().open(som.path.toAbsolutePath().toFile());
        }
        catch (Exception e)
        {
            som.logger.log(Stack_trace_getter.get_stack_trace("❗ open failed :" + e));

            if (e.toString().contains("doesn't exist."))
            {
                Jfx_batch_injector.inject(() -> Popups.popup_warning( "❗ Failed", "Your OS/GUI could not open this file, the error is:\n" + e,false, som.owner, som.logger), som.logger);
            }
            else
            {
                Jfx_batch_injector.inject(() -> Popups.popup_warning( "❗ Failed", "Your OS/GUI could not open this file, the error is:\n" + e + "\nMaybe it is just not properly configured e.g. most often the file extension has to be registered?",false, som.owner, som.logger), som.logger);
            }
            return "failed";
        }
        return "ok";
    }


    //**********************************************************
    private String with_registered_application(System_open_message som)
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

        call_os_specific_open(som,app);

        return "ok";
    }

    //**********************************************************
    private boolean call_os_specific_open(System_open_message som,
                                          String app // can be null
    )
    //**********************************************************
    {
        if ( som.path == null)
        {
            som.logger.log("call_os_specific_open failed, path is null");
            return false;
        }

        if ( !som.path.toFile().exists() )
        {
            som.logger.log("call_os_specific_open failed, path does not exist: "+som.path);
            return false;
        }
        if ( som.path.getParent() == null )
        {
            som.logger.log("call_os_specific_open failed, no parent for path: "+som.path);
            return false;
        }
        Operating_system os = Guess_OS.guess(som.logger);

        Jfx_batch_injector.inject(() -> Popups.popup_warning( "❗ Calling "+os.name()+" to open: "+som.path, "Please wait ",true,som.owner,som.logger), som.logger);

        List<String> list = new ArrayList<>();
        switch ( os)
        {
            case Linux:
                if ( app == null) {
                    list.add("xdg-open");
                }
                else
                {
                    list.add("gtk-launch");
                    list.add(app);
                }
                list.add(som.path.toFile().getAbsolutePath());
                break;
            case MacOS :
                list.add("open");
                list.add(som.path.toFile().getAbsolutePath());
                if ( app != null)
                {
                    list.add("-a");
                    list.add(app);
                }
                break;
            case Windows :
                if ( app == null)
                {
                    list.add("cmd");
                    list.add("/c");
                    list.add("start");
                }
                else
                {
                    // app must be the full path i.e. c:\Program Files\AppName\app.exe
                    list.add(app);
                }
                list.add(som.path.toFile().getAbsolutePath());
                break;
            default:
                return false;
        }



        if (som.aborter.should_abort())
        {
            som.logger.log("open with os-specific aborted");
            return false;
        }
        StringBuilder sb = new StringBuilder();
        File wd = som.path.toFile().getParentFile();

        // on macOS the app 'does not show' because the JVM puts kikr back on top TOO FAST
        //  trick: we hide the app for a while
        Platform.runLater(()->{
            ((Stage)som.owner).hide();
        });

        Execute_result res = Execute_command.execute_command_list_no_wait(list, wd, som.logger);
        if ( !res.status())
        {
            som.logger.log("open with "+os.name()+" failed:\n"+ sb +"\n\n\n");
            return false;
        }
        som.logger.log("\n\n\n open with "+os.name()+" output :\n"+ sb +"\n\n\n");


        // then we un-hide, with transparency
        Actor_engine.execute(()->{
            try
            {
                Thread.sleep(500);
            }
            catch (InterruptedException e)
            {
                som.logger.log(""+e);
                return;
            }Platform.runLater(()->{
                ((Stage)som.owner).show();
                ((Stage)som.owner).setOpacity(0.3);
            });
            try
            {
                Thread.sleep(3000);
            }
            catch (InterruptedException e)
            {
                som.logger.log(""+e);
                return;
            }
            // then we restore klikr ..
            // Case A: (with a bit of luck) klikr is now UNDER the app
            // case B: klikr is on top, but the user could SEE that the other app HAS OPENED
            Platform.runLater(()->{
                ((Stage)som.owner).show();
                ((Stage)som.owner).setOpacity(1);
            });

        },"macos trick",som.logger);

        return true;
    }

}
