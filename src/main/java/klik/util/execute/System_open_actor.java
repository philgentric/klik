//SOURCES ./System_open_message.java
//SOURCES ./Registered_applications.java
package klik.util.execute;

import javafx.stage.Window;
import klik.actor.Aborter;
import klik.actor.Actor;
import klik.actor.Actor_engine;
import klik.actor.Message;
import klik.util.files_and_paths.Static_files_and_paths_utilities;
import klik.util.ui.Jfx_batch_injector;
import klik.util.log.Logger;
import klik.util.ui.Popups;
import klik.util.log.Stack_trace_getter;

import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

//**********************************************************
public class System_open_actor implements Actor
//**********************************************************
{
    //**********************************************************
    public static void open_with_system(
            Window window,
            Path path,
            Aborter aborter,
            Logger logger)
    //**********************************************************
    {
    Actor_engine.run(
            System_open_actor.get(),
            new System_open_message(false,window, path, aborter,logger),null,logger);
    }

    private static System_open_actor instance;
    //**********************************************************
    private static System_open_actor get()
    //**********************************************************
    {
        if ( instance == null) instance = new System_open_actor();
        return instance;
    }


    //**********************************************************
    public static void open_special(Window owner, Path path, Aborter aborter, Logger logger)
    //**********************************************************
    {
        logger.log("open_special " + path);
        Actor_engine.run(
                System_open_actor.get(),
                new System_open_message(true,owner, path, aborter,logger),null,logger);
    }


    //**********************************************************
    @Override
    public String run(Message m)
    //**********************************************************
    {
        System_open_message som = (System_open_message) m;
        if (som.special) return special(som);
        try
        {
            Desktop.getDesktop().open(som.path.toAbsolutePath().toFile());
        }
        catch (Exception e)
        {
            som.logger.log(Stack_trace_getter.get_stack_trace("open failed :" + e));

            if (e.toString().contains("doesn't exist."))
            {
                Jfx_batch_injector.inject(() -> Popups.popup_warning( "Failed?", "Your OS/GUI could not open this file, the error is:\n" + e,false,som.owner,som.logger), som.logger);
            }
            else
            {
                Jfx_batch_injector.inject(() -> Popups.popup_warning( "Failed?", "Your OS/GUI could not open this file, the error is:\n" + e + "\nMaybe it is just not properly configured e.g. most often the file extension has to be registered?",false,som.owner,som.logger), som.logger);
            }
        }
        return null;
    }


    //**********************************************************
    private String special(System_open_message som)
    //**********************************************************
    {
        String extension = Static_files_and_paths_utilities.get_extension(som.path.toFile().getName());

        String app = Registered_applications.get_registered_application(extension, som.owner, som.aborter,som.logger);

        if ( app == null)
        {
            som.logger.log("open special aborted");
            return null;
        }
        som.logger.log("special open for " + som.path + " with " + app);
        Jfx_batch_injector.inject(() -> Popups.popup_warning( "Calling MacOS open for: ", "Please wait " + som.path,true,som.owner,som.logger), som.logger);

        call_MACOS_open(som, app);
        //call_exec(som, app));

        return null;
    }

    //**********************************************************
    private boolean call_MACOS_open(System_open_message som, String app)
    //**********************************************************
    {
        List<String> list = new ArrayList<>();
        list.add("open");
        list.add(som.path.toFile().getAbsolutePath());
        list.add("-a");
        list.add(app);

        if (som.aborter.should_abort())
        {
            som.logger.log("open special aborted");
            return true;
        }
        // Output file is empty
        StringBuilder sb = new StringBuilder();
        File wd = som.path.toFile().getParentFile();
        if (Execute_command.execute_command_list(list, wd, 2000, sb, som.logger) == null)
        {
            som.logger.log("open special failed:\n"+ sb +"\n\n\n");
            return true;
        }
        som.logger.log("\n\n\n open special  output :\n"+ sb +"\n\n\n");
        return false;
    }

    //**********************************************************
    private static boolean call_exec(System_open_message som, String app)
    //**********************************************************
    {
        List<String> list = new ArrayList<>();
        list.add(app);
        list.add(som.path.toFile().getAbsolutePath());

        if (som.aborter.should_abort())
        {
            som.logger.log("open special aborted");
            return true;
        }
        // Output file is empty
        StringBuilder sb = new StringBuilder();
        File wd = som.path.toFile().getParentFile();
        if (Execute_command.execute_command_list(list, wd, 2000, sb, som.logger)==null)
        {
            som.logger.log("open special failed:\n"+ sb +"\n\n\n");
            return true;
        }
        som.logger.log("\n\n\n open special  output :\n"+ sb +"\n\n\n");
        return false;
    }


}
