package klik.change;

import klik.files_and_paths.Old_and_new_Path;
import klik.util.Logger;

import java.util.List;


/*
 * classes which display images (full images or icons) implement this
 * 
 * when a move can be ordered (eventually it can be
 * a multiple move from the icons' panel)
 * the mover-code executes in a separate thread
 * At the end of the operation the mover-code can call
 * this interface: it allows image display-ers
 * to perform update properly after a move
 * e.g. mark the moved icon as moved, or display a new icon etc
 *
 * in case of a move operated while two windows are open, one being the "origin" and the other "the destination"
 * example with drag-and-drop, both windows must be updated, for this reason
 * all windows are registered to the Change_gang
 * 
 */


//**********************************************************
public interface Change_receiver
//**********************************************************
{
	void you_receive_this_because_a_file_event_occurred_somewhere(List<Old_and_new_Path> l, Logger logger2);

	String get_string();

}
