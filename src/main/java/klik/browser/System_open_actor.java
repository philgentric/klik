package klik.browser;

import klik.actor.Actor;
import klik.actor.Message;
import klik.util.Popups;
import klik.util.Stack_trace_getter;

import java.awt.*;

public class System_open_actor implements Actor
{

    public static System_open_actor instance;
    public static System_open_actor get()
    {
        if ( instance == null) instance = new System_open_actor();
        return instance;
    }


    private System_open_actor(){}

    @Override
    public String run(Message m) {
        System_open_message som = (System_open_message) m;
        try
        {
            Desktop.getDesktop().open(som.path.toAbsolutePath().toFile());
        }
        catch (Exception e)
        {
            som.logger.log(Stack_trace_getter.get_stack_trace("open failed :" + e));

            if (e.toString().contains("doesn't exist."))
            {
                Popups.popup_warning(som.the_Stage, "Failed?", "Your OS/GUI could not open this file, the error is:\n" + e,false,som.logger);
            }
            else
            {
                Popups.popup_warning(som.the_Stage, "Failed?", "Your OS/GUI could not open this file, the error is:\n" + e + "\nMaybe it is just not properly configured e.g. most often the file extension has to be registered?",false,som.logger);
            }
        }
        return null;
    }


}