//SOURCES ./Undo_storage_to_disk.java
package klik.change.undo;

import javafx.stage.Stage;
import klik.actor.Aborter;
import klik.change.active_list_stage.Datetime_to_signature_source;
import klik.files_and_paths.Moving_files;
import klik.files_and_paths.Old_and_new_Path;
import klik.util.Fx_batch_injector;
import klik.util.Logger;
import klik.util.Popups;
import klik.change.active_list_stage.Active_list_stage;

import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;
import java.util.List;

//**********************************************************
public class Undo_engine implements Datetime_to_signature_source
//**********************************************************
{
    private static boolean dbg = false;
    private static Undo_engine instance =  null;
    private final Logger logger;
    public static List<Active_list_stage> undo_stages = new ArrayList<>();
    private final Undo_storage_to_disk store;
    private final Aborter aborter;

    //**********************************************************
    public static Undo_engine get_instance(Aborter aborter, Logger logger)
    //**********************************************************
    {
        if (instance == null) instance = new Undo_engine(aborter,logger);
        return instance;
    }
    //**********************************************************
    public static List<Undo_item> get_all_undo_items(Aborter aborter, Logger logger)
    //**********************************************************
    {
        return get_instance(aborter, logger).store.read_all_undo_items_from_disk();
    }

    //**********************************************************
    public static void perform_undo(Undo_item item, Stage owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        get_instance(aborter, logger).undo(item,owner);
    }

    //**********************************************************
    public static boolean add(List<Old_and_new_Path> l, Aborter aborter, Logger logger_)
    //**********************************************************
    {
        if (dbg) logger_.log("Undo_engine::add"+l);
        if (l.isEmpty())
        {
            return false;
            // logger_.log(Stack_trace_getter.get_stack_trace("SHOULD NOT HAPPEN: Undo_engine::add, empty list"));
        }
        return get_instance(aborter, logger_).add_internal(l);
    }
    //**********************************************************
    public static boolean perform_last_undo_fx(Stage owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        return get_instance(aborter, logger).undo_last_move(owner);

    }
    //**********************************************************
    public static void remove_all_undo_items(Stage owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        get_instance(aborter, logger).remove_all_undo_items_internal(owner);
    }

    //**********************************************************
    public static boolean check_validity(Undo_item undo_item, Stage owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        return get_instance(aborter, logger).check_validity_internal(undo_item, owner);
    }

    //**********************************************************
    public static void erase_if_too_old(int max_count, int max_days, Aborter aborter, Logger logger)
    //**********************************************************
    {
        Map<LocalDateTime, String> map = get_instance(aborter, logger).get_map_of_date_to_signature();
        if ( map.keySet().size() < max_count) return;
        LocalDateTime now = LocalDateTime.now();
        List<String> to_be_deleted = new ArrayList<>();
        for( Map.Entry<LocalDateTime, String> e: map.entrySet())
        {
            LocalDateTime date = e.getKey();
            Period p = Period.between(now.toLocalDate(), date.toLocalDate());
            if ( p.getDays() < max_days)
            {
                to_be_deleted.add(e.getValue());
            }
        }
        for ( String signature :to_be_deleted)
        {
            Undo_item ui = get_instance(aborter,logger).get_undo_item_from_signature(signature);
            get_instance(aborter,logger).store.remove_undo_item(ui);
            if ( dbg) logger.log("out of age undo item removed: "+ui.signature());
        }

    }

    //**********************************************************
    Undo_item get_undo_item_from_signature(String signature)
    //**********************************************************
    {
        Map<String, Undo_item> signature_to_undo_item = Undo_engine.get_instance(aborter, logger).get_signature_to_undo_item();
        return signature_to_undo_item.get(signature);
    }





    //**********************************************************
    private Undo_engine(Aborter aborter,Logger logger_)
    //**********************************************************
    {
        this.aborter = aborter;
        logger  = logger_;
        store = new Undo_storage_to_disk(logger);
    }


    //**********************************************************
    private boolean check_validity_internal(Undo_item undo_item, Stage owner)
    //**********************************************************
    {
        int valid = 0;
        for (Old_and_new_Path e : undo_item.oans)
        {
            Old_and_new_Path r = e.reverse_for_restore();
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
    public Map<LocalDateTime, String> get_map_of_date_to_signature()
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
        //Path trash = Static_application_properties.get_trash_dir(logger);
        for(Old_and_new_Path oan : l)
        {
            if ( oan.is_a_restore )
            {
                if ( dbg) logger.log("not recording restore event: "+oan.get_string());
                return false;
            }
            else
            {
                if ( dbg)  logger.log("Adding event: "+oan.get_string());

            }
        }

        Undo_item ui = new Undo_item(l, LocalDateTime.now(), UUID.randomUUID(),logger);
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
            Old_and_new_Path r = e.reverse_for_restore();
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

        Moving_files.perform_safe_moves_in_a_thread(owner,reverse_last_move,  false, aborter, logger);

        store.remove_undo_item(undo_item);
        refresh_UI();
        return true;
    }



    //**********************************************************
    private void remove_all_undo_items_internal(Stage owner)
    //**********************************************************
    {
        store.remove_all_undo_items_from_property_file(owner);
        refresh_UI();
    }


    //**********************************************************
    private static void refresh_UI()
    //**********************************************************
    {
        Fx_batch_injector.inject(()-> {
            for (Active_list_stage s : undo_stages) {
                s.define();
            }
        }, instance.logger);
    }

}
