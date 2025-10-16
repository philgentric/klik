//SOURCES ./House_keeping_actor.java
//SOURCES ./House_keeping_message.java
package klik.change;

import javafx.stage.Window;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.util.files_and_paths.old_and_new.Command;
import klik.util.files_and_paths.Static_files_and_paths_utilities;
import klik.util.files_and_paths.old_and_new.Old_and_new_Path;
import klik.util.files_and_paths.old_and_new.Status;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import klik.util.log.Logger_factory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;


/*
 * this singleton keeps track of guys who want to know
 * about files being changed, that is: deleted, moved or renamed
 * ANY component capable of displaying an image or icon should be listening
 */
//**********************************************************
public class Change_gang
//**********************************************************
{
    public static final boolean dbg = false;
    public Logger dedicated_logger;
    House_keeping_actor house_keeping_actor;
    private final ConcurrentLinkedQueue<Change_receiver> change_gang_receivers;
    public static Change_gang instance = null; // the first guy registering will cause the instance to be created
    //**********************************************************
    private static void create_instance()
    //**********************************************************
    {
        instance = new Change_gang();
    }

    //**********************************************************
    private Change_gang()
    //**********************************************************
    {
        dedicated_logger = Logger_factory.get("change gang");
        change_gang_receivers = new ConcurrentLinkedQueue<>();
        house_keeping_actor = new House_keeping_actor(change_gang_receivers);
    }

    // utility for a registered party to figure out if the changes in the call
    // impact a specific directory

    public enum Possible_outcome
    {
        not_this_folder,
        one_file_gone,
        one_new_file,
        more_changes
    }

    // when you receive a change event, utility to help understand how it is impacting you
    //**********************************************************
    public static Possible_outcome is_my_directory_impacted(Path dir, List<Old_and_new_Path> l, Logger logger)
    //**********************************************************
    {
        for (Old_and_new_Path oan : l)
        {
            if (oan.old_Path == null)
            {
                if ( oan.cmd == Command.command_copy)
                {
                    if ( dbg) logger.log( oan.to_string());
                }
                else
                {
                    logger.log_stack_trace( "should not happen: old path is null and command is not a copy ???"+ oan.to_string());
                }
            }
            else
            {
                if (oan.old_Path.getParent() == null)
                {
                    logger.log(Stack_trace_getter.get_stack_trace("Should not happen?" + oan.to_string()));
                }
                else
                {
                    //if (oan.old_Path.getParent().toAbsolutePath().toString().equals(ref))
                    if (Static_files_and_paths_utilities.is_same_path(oan.old_Path.getParent(),dir,logger))
                    {
                        if (dbg) logger.log("is_my_directory_impacted? YES! "+oan.old_Path.getParent().toAbsolutePath() +" OLD path matches "+ dir.toAbsolutePath());

                        if ( oan.cmd == Command.command_move)
                        {
                            return Possible_outcome.one_file_gone;
                        }
                        return Possible_outcome.more_changes;
                    }
                    else
                    {
                        if (dbg) logger.log("is_my_directory_impacted? No! old_path="+oan.old_Path.getParent().toAbsolutePath() +" does not matches "+ dir.toAbsolutePath());
                    }
                }
            }
            if (oan.new_Path != null)
            {
                if (Static_files_and_paths_utilities.is_same_path(oan.new_Path, dir, logger))
                {
                    if (dbg) logger.log("is_my_directory_impacted? YES! " + oan.new_Path.toAbsolutePath() + " NEW path matches " + dir.toAbsolutePath());
                    if ( oan.cmd == Command.command_move)
                    {
                        return Possible_outcome.one_new_file;
                    }
                    return Possible_outcome.more_changes;
                }
                else
                {
                    if (dbg) logger.log("is_my_directory_impacted? No! new_path="+oan.new_Path.toAbsolutePath() +" does not matches "+ dir.toAbsolutePath());
                }
                if (oan.new_Path.getParent() != null)
                {
                    if (Static_files_and_paths_utilities.is_same_path(oan.new_Path.getParent(), dir, logger))
                    {
                        if (dbg) logger.log("is_my_directory_impacted? YES! " + oan.new_Path.getParent().toAbsolutePath() + " NEW path matches " + dir.toAbsolutePath());
                        if ( oan.cmd == Command.command_move)
                        {
                            return Possible_outcome.one_new_file;
                        }
                        return Possible_outcome.more_changes;
                    }
                    else
                    {
                        if (dbg) logger.log("is_my_directory_impacted? No! new_path="+oan.new_Path.getParent().toAbsolutePath() +" does not matches "+ dir.toAbsolutePath());
                    }
                }
            }
        }
        return Possible_outcome.not_this_folder;
    }


    //**********************************************************
    public static void report_changes(List<Old_and_new_Path> l, Window owner)
    //**********************************************************
    {
        if ( instance != null) instance.event_internal(l,owner);
    }

    // distribute the change event to all registered parties
    //**********************************************************
    private void event_internal(List<Old_and_new_Path> l, Window owner)
    //**********************************************************
    {
        if (l.isEmpty()) return;
        if (dbg) dedicated_logger.log(Stack_trace_getter.get_stack_trace("Change_gang.event_internal()\n   old path ->" + l.get(0).old_Path.toAbsolutePath()+"<-\n   new path ->"+l.get(0).new_Path.toAbsolutePath()+"<-"));
        for (Change_receiver w : change_gang_receivers)
        {
            if ( dbg) dedicated_logger.log("Change_gang.event_internal(), SENDING to gang member:" + w.get_Change_receiver_string());
            w.you_receive_this_because_a_file_event_occurred_somewhere(l, owner, dedicated_logger);
        }
    }
    // ... not really used
    //**********************************************************
    public static void report_anomaly(Path path, Window owner)
    //**********************************************************
    {
        List<Old_and_new_Path> l = new ArrayList<>();
        l.add(new Old_and_new_Path(path,null, Command.command_unknown, Status.before_command,false));
        Change_gang.report_changes(l,owner);
    }




    /*
    house keeping: the Change_gang maintains the list of guys who are interested in changes
     */

    //**********************************************************
    public static void register(Change_receiver change_receiver, Aborter aborter, Logger logger)
    //**********************************************************
    {
        if ( instance ==  null) create_instance();
        instance.register_internal(change_receiver, aborter);
    }
    //**********************************************************
    private void register_internal(Change_receiver change_receiver, Aborter aborter)
    //**********************************************************
    {
        House_keeping_message dr = new House_keeping_message(change_receiver, House_keeping_message_type.register, aborter);
        Actor_engine.run(house_keeping_actor,dr,null, dedicated_logger);
        if ( dbg) dedicated_logger.log("Change_gang: Register_internal " + change_receiver.get_Change_receiver_string());
    }

    //**********************************************************
    public static synchronized void deregister(Change_receiver change_receiver, Aborter aborter)
    //**********************************************************
    {
        if (instance == null) return;
        instance.deregister_internal(change_receiver, aborter);
    }

    //**********************************************************
    private void deregister_internal(Change_receiver change_receiver, Aborter aborter)
    //**********************************************************
    {
        House_keeping_message dr = new House_keeping_message(change_receiver, House_keeping_message_type.deregister, aborter);
        Actor_engine.run(house_keeping_actor,dr,null, dedicated_logger);

        if ( dbg) dedicated_logger.log("Change_gang: De-register_internal " + change_receiver.get_Change_receiver_string());
    }


}
