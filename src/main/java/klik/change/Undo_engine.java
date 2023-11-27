package klik.change;

import javafx.application.Platform;
import javafx.stage.Stage;
import klik.actor.Aborter;
import klik.change.active_list_stage.Datetime_to_signature_source;
import klik.files_and_paths.Moving_files;
import klik.files_and_paths.Old_and_new_Path;
import klik.properties.Static_application_properties;
import klik.util.Logger;
import klik.util.Popups;
import klik.change.active_list_stage.Active_list_stage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

//**********************************************************
public class Undo_engine implements Datetime_to_signature_source
//**********************************************************
{
    private static Undo_engine instance =  null;
    private final Logger logger;
    public static List<Active_list_stage> undo_stages = new ArrayList<>();
    private final Undo_storage_to_disk store;

    public static Undo_engine get_instance(Logger logger)
    {
        if (instance == null) instance = new Undo_engine(logger);
        return instance;
    }
    //**********************************************************
    public static List<Undo_item> get_all_undo_items(Logger logger_)
    //**********************************************************
    {
        if ( instance == null ) instance =  new Undo_engine(logger_);
        return instance.store.read_all_undo_items_from_disk();
    }

    //**********************************************************
    public static void perform_undo(Undo_item item, Stage owner, Logger logger)
    //**********************************************************
    {
        if ( instance == null ) instance =  new Undo_engine(logger);
        instance.undo(item,owner);
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
    public static boolean perform_last_undo(Stage owner, Logger logger)
    //**********************************************************
    {
        if ( instance == null ) instance =  new Undo_engine(logger);
        return instance.undo_last_move(owner);

    }
    //**********************************************************
    public static void remove_all_undo_items()
    //**********************************************************
    {
        instance.remove_all_undo_items_internal();
    }









    //**********************************************************
    private Undo_engine(Logger logger_)
    //**********************************************************
    {
        logger  = logger_;
        store = new Undo_storage_to_disk(logger);

    }

    //**********************************************************
    public static boolean check_validity(Undo_item undo_item, Stage owner, Logger logger)
    //**********************************************************
    {
        return Undo_engine.get_instance(logger).check_validity_internal(undo_item, owner);
    }

    //**********************************************************
    private boolean check_validity_internal(Undo_item undo_item, Stage owner)
    //**********************************************************
    {
        int valid = 0;
        for (Old_and_new_Path e : undo_item.oans)
        {
            Old_and_new_Path r = e.reverse();
            if ( Files.exists(r.old_Path))
            {
                valid++;
            }
            else
            {
                logger.log("\n\n\nIGNORED: this undo item is now invalid, as the source file is not where mentioned in the record... it was probably moved since?\n\n\n");
            }
        }
        if ( valid == 0) return false;
        return true;
    }


    //**********************************************************
    @Override
    public Map<LocalDateTime, String> get()
    //**********************************************************
    {
        List<Undo_item> ll = instance.store.read_all_undo_items_from_disk();
        Map<LocalDateTime, String> returned = new HashMap<>();
        for ( Undo_item ui: ll)
        {
            returned.put(ui.time_stamp,ui.signature());
        }
        return returned;
    }



    //**********************************************************
    public Map<String, Undo_item> get_signature_to_undo_item()
    //**********************************************************
    {
        Map<String, Undo_item> returned = new HashMap<>();
        List<Undo_item> ll = instance.store.read_all_undo_items_from_disk();
        for ( Undo_item ui: ll)
        {
            returned.put(ui.signature(),ui);
        }
        return returned;
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
        if (Undo_storage_to_disk.dbg) logger.log("Undo_engine add:"+ui.to_string());
        store.add(ui);
        refresh_UI();
        return true;
    }

    //**********************************************************
    private boolean undo_last_move(Stage owner)
    //**********************************************************
    {
        Undo_item most_recent_undo_item = store.get_most_recent();
        if (most_recent_undo_item == null) {
            logger.log(" nothing to undo");
            Popups.popup_warning(owner, "Nothing to undo", "The undo list is empty!", true, logger);
            return false;
        }
        return undo(most_recent_undo_item, owner);
    }

    //**********************************************************
    private boolean undo(Undo_item undo_item, Stage owner)
    //**********************************************************
    {
        if (Undo_storage_to_disk.dbg) logger.log("Undo_engine performing: UNDO of "+undo_item.to_string());
        List<Old_and_new_Path> reverse_last_move = new ArrayList<>();
        for (Old_and_new_Path e : undo_item.oans)
        {
            Old_and_new_Path r = e.reverse();
            if ( !Files.exists(r.old_Path))
            {
                logger.log("\n\n\nIGNORED: this undo item is now invalid, as the source file is not where mentioned in the record... it was probably moved since?\n\n\n");
                Popups.popup_warning(owner,"Invalid undo item","The file was probably moved since?",true,logger);
            }
            else {
                reverse_last_move.add(r);
                logger.log("reversed action =" + r.get_string());
            }
        }

        Moving_files.perform_safe_moves_in_a_thread(owner,reverse_last_move, new Aborter(), true, false, logger);

        store.remove_after_undo_done(undo_item);
        refresh_UI();
        return true;
    }



    //**********************************************************
    private void remove_all_undo_items_internal()
    //**********************************************************
    {
        store.remove_all_undo_items_from_property_file();
        refresh_UI();
    }


    //**********************************************************
    private static void refresh_UI()
    //**********************************************************
    {
        for ( Active_list_stage s : undo_stages)
        {
            Platform.runLater(() ->s.define());
        }
    }

}
