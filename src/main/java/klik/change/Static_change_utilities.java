package klik.change;

import klik.util.Logger;
import klik.util.Tool_box;

import java.util.ArrayList;
import java.util.List;

public class Static_change_utilities {

    public static void undo_last_move(Logger logger) {
        List<Old_and_new_Path> last_moves = Change_gang.instance.last_event;
        if (last_moves == null) {
            logger.log(" nothing to undo");
            return;
        }

        List<Old_and_new_Path> reverse_last_move = new ArrayList<>();
        for (Old_and_new_Path e : last_moves) {
            Old_and_new_Path r = e.reverse();
            reverse_last_move.add(r);
            logger.log("reversed action =" + r.get_string());

        }

        logger.log("perform_the_move_in_a_javafx_Task4");
        Tool_box.perform_the_safe_moves_in_a_thread(reverse_last_move, logger);
    }

}
