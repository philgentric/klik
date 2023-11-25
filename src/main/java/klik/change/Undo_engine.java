package klik.change;

import javafx.stage.Stage;
import klik.actor.Aborter;
import klik.files_and_paths.Moving_files;
import klik.files_and_paths.Old_and_new_Path;
import klik.properties.Static_application_properties;
import klik.util.Logger;
import klik.util.Popups;
import klik.util.Stack_trace_getter;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

//**********************************************************
public class Undo_engine
//**********************************************************
{
    private static Undo_engine instance =  null;
    private final Logger logger;
    private final Undo_store store;

    //**********************************************************
    public Undo_engine(Logger logger_)
    //**********************************************************
    {
        logger  = logger_;
        store = new Undo_store(logger);

    }

    //**********************************************************
    public static boolean add(List<Old_and_new_Path> l, Logger logger_)
    //**********************************************************
    {
        //logger_.log(Stack_trace_getter.get_stack_trace("Undo_engine::add"));
        if ( instance == null ) instance =  new Undo_engine(logger_);
        return instance.add_internal(l);
    }
    //**********************************************************
    public static boolean undo(Stage owner, Logger logger)
    //**********************************************************
    {
        if ( instance == null ) instance =  new Undo_engine(logger);
        return instance.undo_last_move(owner);

    }

    public static void clear_all() {
    }


    //**********************************************************
    private boolean add_internal(List<Old_and_new_Path> l)
    //**********************************************************
    {
        Path trash = Static_application_properties.get_trash_dir(logger);
        for(Old_and_new_Path oan : l)
        {
            if ( oan.old_Path.toAbsolutePath().getParent().equals(trash.toAbsolutePath()))
            {
                logger.log("not recording restore event: "+oan.get_string());
                return false;
            }
            else
            {
                logger.log("Adding event: "+oan.get_string());

            }
        }

        Undo_item ui = new Undo_item(l, LocalDateTime.now(), UUID.randomUUID());
        if (Undo_store.dbg) logger.log("Undo_engine add:"+ui.to_string());
        store.add(ui);
        return true;
    }

    //**********************************************************
    public boolean undo_last_move(Stage owner)
    //**********************************************************
    {
        Undo_item most_recent_undo_item = store.get_most_recent();
        if ( most_recent_undo_item == null)
        {
            logger.log(" nothing to undo");
            Popups.popup_warning(owner,"Nothing to undo","The undo list is empty!",true,logger);
            return false;
        }
        if (Undo_store.dbg) logger.log("Undo_engine performing: UNDO of "+most_recent_undo_item.to_string());
        List<Old_and_new_Path> reverse_last_move = new ArrayList<>();
        for (Old_and_new_Path e : most_recent_undo_item.oans)
        {
            Old_and_new_Path r = e.reverse();
            reverse_last_move.add(r);
            logger.log("reversed action =" + r.get_string());
        }
        Moving_files.perform_safe_moves_in_a_thread(owner,reverse_last_move, new Aborter(), true, false, logger);

        store.remove_after_undo_done(most_recent_undo_item);
        return true;
    }

}
