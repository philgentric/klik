package klik.browser;

import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.stage.Stage;
import klik.actor.Aborter;
import klik.browser.items.Item;
import klik.files_and_paths.Moving_files;
import klik.properties.Static_application_properties;
import klik.util.Logger;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/*
static utilities for drag-and-drop
 */
//**********************************************************
public class Drag_and_drop
//**********************************************************
{
    public static boolean drag_and_drop_dbg = false;

    //**********************************************************
    public static int accept_drag_dropped_as_a_move_in(
            Stage owner,
            DragEvent drag_event,
            Path destination_dir,
            Node excluded,
            String origin,
            Logger logger)
    //**********************************************************
    {

        Object source = drag_event.getGestureSource();
        if (source == null) {
            logger.log("source class is null for" + drag_event.toString());
        } else {
            if (source == excluded) {
                logger.log("source is excluded: cannot drop onto itself");
                drag_event.consume();
                return 0;
            }
            //logger.log(Stack_trace_getter.get_stack_trace("source class is:" + source.getClass().getName()));
            //logger.log("excluded class is:" + excluded.getClass().getName());
            if (source instanceof Item item) {
                Node node_of_source = item.get_big_Node();
                logger.log("excluded:" + excluded);
                // data is dragged over the target
                // accept it only if it is not dragged from the same node
                if (node_of_source == excluded) {
                    logger.log("drag reception for stage: same scene, giving up<<");
                    drag_event.consume();
                    return 0;
                }
            }

        }

        Dragboard dragboard = drag_event.getDragboard();
        List<File> list = new ArrayList<>();
        String s = dragboard.getString();
        if (s == null) {
            logger.log( "dragboard.getString()== null");
        }
        else
        {
            if ( drag_and_drop_dbg) logger.log(origin + " drag ACCEPTED for STRING: " + s);
            for (String ss : s.split("\\r?\\n")) {
                if (ss.isBlank()) continue;
                if ( drag_and_drop_dbg) logger.log(origin + " drag ACCEPTED for additional file: " + ss);
                list.add(new File(ss));
            }
            if (list.isEmpty())
            {
                logger.log(origin + " drag list is empty ? " + s);
            }
        }
        {
            List<File> l = dragboard.getFiles();
            for (File fff : l)
            {
                if ( drag_and_drop_dbg) logger.log(origin + "... drag ACCEPTED for file= " + fff.getAbsolutePath());
                if ( !list.contains(fff) )  list.add(fff);
                // Tool_box.safe_move_a_file_or_dir(destination_dir, logger, fff);
                // logger.log(origin + " 6 drag ACCEPTED for: " + fff.getAbsolutePath());
            }
        }

        // safety check
        int dir_count = 0;
        for ( File f : list)
        {
            if ( f.isDirectory()) dir_count++;
            if (drag_and_drop_dbg) logger.log("going to drag/move:"+f.getAbsolutePath());
        }
        if ( dir_count >= 2)
        {
            logger.log("popup alert to warn user");
            Alert a = new Alert(Alert.AlertType.CONFIRMATION,"Are you sure? It seems you are trying to move "+dir_count+" folders", ButtonType.OK,ButtonType.CANCEL);
            a.showAndWait();
            if ( a.getResult() == ButtonType.CANCEL)
            {
                drag_event.consume();
                return 0;
            }
        }


        boolean destination_is_trash = Static_application_properties.is_this_trash(destination_dir,logger);
        logger.log("\n\naccept_drag_dropped_as_a_move_in " + origin+" destination= "+destination_dir+" is_trash="+ destination_is_trash);

        Moving_files.safe_move_files_or_dirs(owner,
                destination_dir,
                destination_is_trash,
                list,
                new Aborter(),
                logger);
        //Popups.popup_text("Drag and Drop", list.size() + " file(s) moved!", true);

        // tell the source
        drag_event.setDropCompleted(true);
        drag_event.consume();
        return list.size();
    }





}
