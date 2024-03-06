package klik.util.execute;

import javafx.application.Platform;
import klik.actor.Actor;
import klik.actor.Actor_engine;
import klik.actor.Message;
import klik.browser.Browser;
import klik.util.Logger;
import klik.util.Popups;
import klik.util.Stack_trace_getter;
import org.apache.commons.io.FilenameUtils;

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
    public static void open_with_system(Browser browser, Path path, Logger logger)
    //**********************************************************
    {
    Actor_engine.run(
            System_open_actor.get(),
            new System_open_message(false,browser.my_Stage.the_Stage, path, browser.aborter,logger),null,logger);
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
    public static void open_special(Browser browser, Path path, Logger logger)
    //**********************************************************
    {
        logger.log("open_special " + path);
        Actor_engine.run(
                System_open_actor.get(),
                new System_open_message(true,browser.my_Stage.the_Stage, path, browser.aborter,logger),null,logger);
    }


    //**********************************************************
    @Override
    public String run(Message m)
    //**********************************************************
    {
        System_open_message som = (System_open_message) m;

        Platform.runLater(() -> Popups.popup_warning(som.the_Stage, "Opening....", "Please wait " + som.path,true,som.logger));

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
                Platform.runLater(() -> Popups.popup_warning(som.the_Stage, "Failed?", "Your OS/GUI could not open this file, the error is:\n" + e,false,som.logger));
            }
            else
            {
                Platform.runLater(() -> Popups.popup_warning(som.the_Stage, "Failed?", "Your OS/GUI could not open this file, the error is:\n" + e + "\nMaybe it is just not properly configured e.g. most often the file extension has to be registered?",false,som.logger));
            }
        }
        return null;
    }


    //**********************************************************
    private String special(System_open_message som)
    //**********************************************************
    {
        String extension = FilenameUtils.getExtension(som.path.toFile().getName());

        String app = Registered_applications.get_registered_application(extension, som.the_Stage, som.logger);

        if ( app == null)
        {
            som.logger.log("open special aborted");
            return null;
        }
        som.logger.log("special open for " + som.path + " with " + app);
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
        if (!Execute_command.execute_command_list(list, wd, 2000, sb, som.logger))
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
        if (!Execute_command.execute_command_list(list, wd, 2000, sb, som.logger))
        {
            som.logger.log("open special failed:\n"+ sb +"\n\n\n");
            return true;
        }
        som.logger.log("\n\n\n open special  output :\n"+ sb +"\n\n\n");
        return false;
    }


}
