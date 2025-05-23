package klik.change;

import javafx.stage.Stage;
import klik.actor.Aborter;
import klik.util.files_and_paths.*;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

//**********************************************************
public class Redo_same_move_engine
//**********************************************************
{

    static public Path last_destination_folder = null;
    //**********************************************************
    public static void same_move(Path old_path, Stage the_stage, Logger logger)
//**********************************************************
    {
        if ( last_destination_folder == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("PANIC "));
            return;
        }
        Path new_path = Path.of(last_destination_folder.toAbsolutePath().toString(), old_path.getFileName().toString());
        logger.log("Redo_same_move_engine.same_move:\n    old: "+old_path+"\n    new: "+new_path);
        Old_and_new_Path oanp = new Old_and_new_Path(old_path,new_path, Command_old_and_new_Path.command_move, Status_old_and_new_Path.before_command,false);
        List<Old_and_new_Path> ll = new ArrayList<>();
        ll.add(oanp);
        double x = the_stage.getX()+100;
        double y = the_stage.getY()+100;
        Moving_files.perform_safe_moves_in_a_thread(the_stage,x,y,ll, true,new Aborter("same move",logger), logger);
    }
}
