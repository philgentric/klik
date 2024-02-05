package klik.images;

import javafx.stage.Stage;
import klik.actor.Aborter;
import klik.files_and_paths.*;
import klik.util.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Same_move_engine {

    static public Path last_destination_folder = null;
    public static void same_move(Path old_path, Stage the_stage, Logger logger)
    {

        Path new_path = Path.of(last_destination_folder.toAbsolutePath().toString(), old_path.getFileName().toString());
        logger.log("Same_move_engine.same_move:\n    old: "+old_path+"\n    new: "+new_path);
        Old_and_new_Path oanp = new Old_and_new_Path(old_path,new_path, Command_old_and_new_Path.command_move, Status_old_and_new_Path.before_command,false);
        List<Old_and_new_Path> ll = new ArrayList<>();
        ll.add(oanp);
        Moving_files.perform_safe_moves_in_a_thread(the_stage,ll, true,new Aborter("same move",logger), logger);
    }
}
