package klik.change;

import klik.util.Logger;
import klik.util.Stack_trace_getter;
import klik.util.System_out_logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;


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

    public static final Change_gang instance = new Change_gang();

    private ConcurrentLinkedQueue<After_move_handler> current_change_gang;
    //private ConcurrentLinkedQueue<After_move_handler_message> future_change_gang;
    private LinkedBlockingQueue<After_move_handler_message> future_change_gang;
    public List<Old_and_new_Path> last_event;

    //**********************************************************
    private Change_gang()
    //**********************************************************
    {
        //dedicated_logger = new Disruptor_logger("Change_gang.txt");
        dedicated_logger = new System_out_logger();
        current_change_gang = new ConcurrentLinkedQueue<>();
        //future_change_gang = new ConcurrentLinkedQueue<>();
        future_change_gang = new LinkedBlockingQueue<>();
        (new Thread(new Runnable() {
            @Override
            public void run() {
                house_keeping();
            }
        })).start();
    }

    //**********************************************************
    public static void register(After_move_handler wtdam)
    //**********************************************************
    {
        instance.register_internal(wtdam);
    }

    //**********************************************************
    public static boolean is_my_directory_impacted(Path dir, List<Old_and_new_Path> l, Logger logger)
    //**********************************************************
    {
        String ref = dir.toAbsolutePath().toString();
        for (Old_and_new_Path oan : l) {
            if (oan.get_old_Path() == null) {
                logger.log(Stack_trace_getter.get_stack_trace("Should not happen?" + oan.get_string()));
            } else {
                if (oan.get_old_Path().getParent() == null) {
                    logger.log(Stack_trace_getter.get_stack_trace("Should not happen?" + oan.get_string()));
                } else {
                    if (oan.get_old_Path().getParent().toAbsolutePath().toString().equals(ref)) return true;
                }
            }
            if (oan.get_new_Path() != null) {
                if (oan.get_new_Path().getParent().toAbsolutePath().toString().equals(ref)) return true;
            }
        }
        return false;
    }

    //**********************************************************
	public static void report_anomaly(Path path)
    //**********************************************************
    {
        List<Old_and_new_Path> l = new ArrayList<Old_and_new_Path>();
        l.add(new Old_and_new_Path(path,null,Command_old_and_new_Path.command_unknown, Status_old_and_new_Path.before_command));
        Change_gang.report_event(l);
	}

	//**********************************************************
    private void register_internal(After_move_handler wtdam)
    //**********************************************************
    {
        After_move_handler_message dr = new After_move_handler_message(wtdam, After_move_handler_message_type.register);
        future_change_gang.add(dr);
        list_future("after register");

    }

    //**********************************************************
    public static synchronized void deregister(After_move_handler wtdam)
    //**********************************************************
    {
        if (instance == null) return;
        instance.deregister_internal(wtdam);
    }

    //**********************************************************
    private void deregister_internal(After_move_handler wtdam)
    //**********************************************************
    {
        After_move_handler_message dr = new After_move_handler_message(wtdam, After_move_handler_message_type.deregister);
        future_change_gang.add(dr);

        //dedicated_logger.log(Stack_trace_getter.get_stack_trace("Change_gang: DEregister_internal "+wtdam.get_string()));
        if ( dbg)
        {
            dedicated_logger.log("Change_gang: De-register_internal " + wtdam.get_string());
            list_future("after deregister");
        }
    }

    void list_future(String reason)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("*********FUTURE: " + reason + "****\n");
        for (After_move_handler_message cc : future_change_gang) {
            sb.append("        ");
            sb.append(cc.originator.get_string());
            sb.append(" ");
            sb.append(cc.type);
            sb.append("\n");
        }
        sb.append("*********CURRENT: *************\n");
        for (After_move_handler cc : current_change_gang) {
            sb.append("       ");
            sb.append(cc.get_string());
            sb.append("\n");
        }
        sb.append("*******************************\n");

        dedicated_logger.log(sb.toString());
    }


    //**********************************************************
    public static void report_event(List<Old_and_new_Path> l)
    //**********************************************************
    {
        instance.event_internal(l);
    }

    //**********************************************************
    private void event_internal(List<Old_and_new_Path> l)
    //**********************************************************
    {
        if (l.isEmpty()) return;

        last_event = l;

        //Stack_printer.print(logger2, "Change_gang.event_internal()");
        //dedicated_logger.log(Stack_trace_getter.get_stack_trace("Change_gang.event_internal()"+l.get(0).get_old_Path().getFileName()));
        dedicated_logger.log("Change_gang.event_internal()" + l.get(0).get_old_Path().getFileName());


        for (After_move_handler w : current_change_gang) {
            dedicated_logger.log("Change_gang.event_internal(), SENDING to gang member:" + w.get_string());
            w.you_receive_this_because_a_move_occurred_somewhere(l, dedicated_logger);
        }
    }

    private void house_keeping() {
        for (; ; ) {
            After_move_handler_message amhm = null;
            try {
                amhm = future_change_gang.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //if ( amhm == null) break;
            switch (amhm.type) {
                case register:
                    current_change_gang.add(amhm.originator);
                    break;
                case deregister:
                    current_change_gang.remove(amhm.originator);
                    break;
            }
        }

    }

    public static List<Old_and_new_Path> is_my_directory_impacted_by_a_gone_file(Path dir, List<Old_and_new_Path> l) {
        List<Old_and_new_Path> returned = new ArrayList<>();
        for (Old_and_new_Path oan : l) {
            // are you the origin directory?
            if (dir.toAbsolutePath().toString().equals(oan.get_old_Path().getParent().toString())) {
                Command_old_and_new_Path c = oan.get_cmd();
                switch (c) {
                    case command_delete:
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

    public static List<Old_and_new_Path> is_my_directory_impacted_by_a_renamed_file(Path dir, List<Old_and_new_Path> l) {
        List<Old_and_new_Path> returned = new ArrayList<>();
        for (Old_and_new_Path oan : l) {
            // are you the origin directory?
            if (dir.toAbsolutePath().toString().equals(oan.get_old_Path().getParent().toString())) {
                Command_old_and_new_Path c = oan.get_cmd();
                switch (c) {
                    case command_delete:
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

    public static List<Old_and_new_Path> is_my_directory_impacted_by_a_new_file(Path dir, List<Old_and_new_Path> l, Logger logger) {
        List<Old_and_new_Path> returned = new ArrayList<>();
        for (Old_and_new_Path oan : l) {
            if (dir == null) {
                logger.log("dir == null");
            }
            if (oan.get_new_Path() == null) {
                logger.log("oan.get_new_Path() == null");
            }
            // are you the destination directory?
            if (dir.toAbsolutePath().toString().equals(oan.get_new_Path().getParent().toString())) {
                Command_old_and_new_Path c = oan.get_cmd();
                switch (c) {
                    case command_delete:
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

}
