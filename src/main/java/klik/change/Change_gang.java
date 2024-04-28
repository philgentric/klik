package klik.change;

import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.files_and_paths.Command_old_and_new_Path;
import klik.files_and_paths.Files_and_Paths;
import klik.files_and_paths.Old_and_new_Path;
import klik.files_and_paths.Status_old_and_new_Path;
import klik.util.Logger;
import klik.util.Stack_trace_getter;
import klik.util.System_out_logger;

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
    private static void create_instance(Logger logger)
    //**********************************************************
    {
        instance = new Change_gang(logger);
    }

    //**********************************************************
    private Change_gang(Logger logger)
    //**********************************************************
    {
        //dedicated_logger = new Disruptor_logger("Change_gang.txt");
        dedicated_logger = new System_out_logger();
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
            if (oan.get_old_Path() == null)
            {
                if ( oan.cmd == Command_old_and_new_Path.command_copy)
                {
                    if ( dbg) logger.log( oan.get_string());
                }
                else
                {
                    logger.log_stack_trace( "should not happen: old path is null and command is not a copy ???"+ oan.get_string());
                }
            }
            else
            {
                if (oan.get_old_Path().getParent() == null)
                {
                    logger.log(Stack_trace_getter.get_stack_trace("Should not happen?" + oan.get_string()));
                }
                else
                {
                    //if (oan.get_old_Path().getParent().toAbsolutePath().toString().equals(ref))
                    if (Files_and_Paths.is_same_path(oan.get_old_Path().getParent(),dir,logger))
                    {
                        if (dbg) logger.log("is_my_directory_impacted? YES! "+oan.get_old_Path().getParent().toAbsolutePath() +" OLD path matches "+ dir.toAbsolutePath());

                        if ( oan.cmd == Command_old_and_new_Path.command_move)
                        {
                            return Possible_outcome.one_file_gone;
                        }
                        return Possible_outcome.more_changes;
                    }
                    else
                    {
                        if (dbg) logger.log("is_my_directory_impacted? No! old_path="+oan.get_old_Path().getParent().toAbsolutePath() +" does not matches "+ dir.toAbsolutePath());
                    }
                }
            }
            if (oan.get_new_Path() != null)
            {
                if (Files_and_Paths.is_same_path(oan.get_new_Path(), dir, logger))
                {
                    if (dbg) logger.log("is_my_directory_impacted? YES! " + oan.get_new_Path().toAbsolutePath() + " NEW path matches " + dir.toAbsolutePath());
                    if ( oan.cmd == Command_old_and_new_Path.command_move)
                    {
                        return Possible_outcome.one_new_file;
                    }
                    return Possible_outcome.more_changes;
                }
                else
                {
                    if (dbg) logger.log("is_my_directory_impacted? No! new_path="+oan.get_new_Path().toAbsolutePath() +" does not matches "+ dir.toAbsolutePath());
                }
                if (oan.get_new_Path().getParent() != null)
                {
                    if (Files_and_Paths.is_same_path(oan.get_new_Path().getParent(), dir, logger))
                    {
                        if (dbg) logger.log("is_my_directory_impacted? YES! " + oan.get_new_Path().getParent().toAbsolutePath() + " NEW path matches " + dir.toAbsolutePath());
                        if ( oan.cmd == Command_old_and_new_Path.command_move)
                        {
                            return Possible_outcome.one_new_file;
                        }
                        return Possible_outcome.more_changes;
                    }
                    else
                    {
                        if (dbg) logger.log("is_my_directory_impacted? No! new_path="+oan.get_new_Path().getParent().toAbsolutePath() +" does not matches "+ dir.toAbsolutePath());
                    }
                }
            }
        }
        return Possible_outcome.not_this_folder;
    }


    //**********************************************************
    public static void report_changes(List<Old_and_new_Path> l)
    //**********************************************************
    {
        if ( instance != null) instance.event_internal(l);
    }

    // distribute the change event to all registered parties
    //**********************************************************
    private void event_internal(List<Old_and_new_Path> l)
    //**********************************************************
    {
        if (l.isEmpty()) return;
        if (dbg) dedicated_logger.log(Stack_trace_getter.get_stack_trace("Change_gang.event_internal()\n   old path ->" + l.get(0).get_old_Path().toAbsolutePath()+"<-\n   new path ->"+l.get(0).get_new_Path().toAbsolutePath()+"<-"));
        for (Change_receiver w : change_gang_receivers)
        {
            if ( dbg) dedicated_logger.log("Change_gang.event_internal(), SENDING to gang member:" + w.get_string());
            w.you_receive_this_because_a_file_event_occurred_somewhere(l, dedicated_logger);
        }
    }
    // ... not really used
    //**********************************************************
    public static void report_anomaly(Path path)
    //**********************************************************
    {
        List<Old_and_new_Path> l = new ArrayList<>();
        l.add(new Old_and_new_Path(path,null, Command_old_and_new_Path.command_unknown, Status_old_and_new_Path.before_command,false));
        Change_gang.report_changes(l);
    }




    /*
    house keeping: the Change_gang maintains the list of guys who are interested in changes
     */

    //**********************************************************
    public static void register(Change_receiver wtdam, Aborter aborter, Logger logger)
    //**********************************************************
    {
        if ( instance ==  null) create_instance(logger);
        instance.register_internal(wtdam, aborter);
    }
    //**********************************************************
    private void register_internal(Change_receiver change_receiver, Aborter aborter)
    //**********************************************************
    {
        House_keeping_message dr = new House_keeping_message(change_receiver, House_keeping_message_type.register, aborter);
        Actor_engine.run(house_keeping_actor,dr,null, dedicated_logger);
        if ( dbg) dedicated_logger.log("Change_gang: Register_internal " + change_receiver.get_string());
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

        if ( dbg) dedicated_logger.log("Change_gang: De-register_internal " + change_receiver.get_string());
    }





    /*
    old code



    //**********************************************************
    public static List<Old_and_new_Path> is_my_directory_impacted_by_a_gone_file(Path dir, List<Old_and_new_Path> l,Logger logger)
    //**********************************************************
    {
        List<Old_and_new_Path> returned = new ArrayList<>();
        for (Old_and_new_Path oan : l) {
            // are you the origin directory?
            if ( Files_and_Paths.is_same_path(dir,oan.get_old_Path().getParent(),logger)) {
                Command_old_and_new_Path c = oan.get_cmd();
                switch (c) {
                    case command_move_to_trash:
                        instance.dedicated_logger.log("delete origin dir=" + dir);
                        returned.add(oan);
                        break;
                    case command_move:
                        instance.dedicated_logger.log("move origin dir=" + dir);
                        returned.add(oan);
                        break;
                    case command_rename:
                        instance.dedicated_logger.log("renaming origin dir=" + dir);
                        break;
                    case command_edit:
                        instance.dedicated_logger.log("edit origin dir=" + dir);
                        break;
                    case command_unknown:
                        instance.dedicated_logger.log("UNKNOWN? origin dir=" + dir);
                        break;
                }
            }
        }
        return returned;
    }

    //**********************************************************
    public static List<Old_and_new_Path> is_my_directory_impacted_by_a_renamed_file(Path dir, List<Old_and_new_Path> l, Logger logger)
    //**********************************************************
    {
        List<Old_and_new_Path> returned = new ArrayList<>();
        for (Old_and_new_Path oan : l) {
            // are you the origin directory?
            if (Files_and_Paths.is_same_path( dir,oan.get_old_Path().getParent(),logger)) {
                Command_old_and_new_Path c = oan.get_cmd();
                switch (c) {
                    case command_move_to_trash:
                        instance.dedicated_logger.log("delete origin dir=" + dir);
                        break;
                    case command_move:
                        instance.dedicated_logger.log("move origin dir=" + dir);
                        break;
                    case command_rename:
                        instance.dedicated_logger.log("renaming origin dir=" + dir);
                        returned.add(oan);
                        break;
                    case command_edit:
                        instance.dedicated_logger.log("edit origin dir=" + dir);
                        break;
                    case command_unknown:
                        instance.dedicated_logger.log("UNKNOWN? origin dir=" + dir);
                        break;
                }
                returned.add(oan);
            }

        }
        return returned;
    }

    //**********************************************************
    public static List<Old_and_new_Path> is_my_directory_impacted_by_a_new_file(Path dir, List<Old_and_new_Path> l, Logger logger)
    //**********************************************************
    {
        List<Old_and_new_Path> returned = new ArrayList<>();
        for (Old_and_new_Path oan : l) {
            if (dir == null) {
                logger.log("dir == null");
            }
            if (oan.get_new_Path() == null) {
                logger.log("oan.get_new_Path() == null");
            }
            // are you the destination directory?
            if (Files_and_Paths.is_same_path(dir,oan.get_new_Path().getParent(),logger)) {
                Command_old_and_new_Path c = oan.get_cmd();
                switch (c) {
                    case command_move_to_trash:
                        // the user is browsing the trash dir?
                        instance.dedicated_logger.log("delete destination dir=" + dir);
                        break;
                    case command_move:
                        instance.dedicated_logger.log("move destination dir=" + dir);
                        returned.add(oan);
                        break;
                    case command_rename:
                        instance.dedicated_logger.log("renaming destination dir=" + dir);
                        break;
                    case command_edit:
                        instance.dedicated_logger.log("edit destination dir=" + dir);
                        break;
                    case command_unknown:
                        instance.dedicated_logger.log("UNKNOWN? destination dir=" + dir);
                        break;
                }
            }
        }
        return returned;
    }
*/
}
