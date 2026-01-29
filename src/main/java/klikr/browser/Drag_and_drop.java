// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ../util/files_and_paths/Moving_files.java
package klikr.browser;

import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.input.*;
import javafx.stage.Window;
import klikr.util.execute.actor.Aborter;
import klikr.browser.items.Item;
import klikr.browser.virtual_landscape.Selection_handler;
import klikr.look.Look_and_feel_manager;
import klikr.util.log.Logger;

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
    public static boolean drag_and_drop_ultra_dbg = false;

    //**********************************************************
    public static int accept_drag_dropped_as_a_move_in(
            Move_provider move_provider,
            DragEvent drag_event,
            Path destination_dir,
            Node excluded,
            String origin,
            boolean destination_is_trash,
            Window owner,
            Logger logger)
    //**********************************************************
    {

        Object source = drag_event.getGestureSource();
        if (source == null)
        {
            logger.log(("❗ WARNING: accept_drag_dropped_as_a_move_in, cannot check for stupid move because the event's source is null: " + drag_event.getSource()));
        }
        else
        {
            if (source == excluded) {
                logger.log("❗ source is excluded: cannot drop onto itself");
                drag_event.consume();
                return 0;
            }
            //logger.log(Stack_trace_getter.get_stack_trace("source class is:" + source.getClass().getName()));
            //logger.log("excluded class is:" + excluded.getClass().getName());
            if (source instanceof Item item)
            {
                Node node_of_source = item.get_Node();
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
        List<File> the_list = new ArrayList<>();
        String s = dragboard.getString();
        if (s == null) {
            logger.log( "dragboard.getString()== null");
        }
        else
        {
            if ( drag_and_drop_dbg) logger.log(origin + " drag ACCEPTED for STRING:->" + s+ "<-");
            for (String ss : s.split("\\r?\\n"))
            {
                if (ss.isBlank()) continue;
                if ( drag_and_drop_dbg) logger.log(origin + " drag ACCEPTED for additional file: " + ss);
                the_list.add(new File(ss));
            }
            if (the_list.isEmpty())
            {
                logger.log(origin + " drag list is empty ? " + s);
            }
        }
        {
            List<File> l = dragboard.getFiles();
            for (File fff : l)
            {
                if ( drag_and_drop_dbg) logger.log(origin + "... drag ACCEPTED for file= " + fff.getAbsolutePath());
                if ( !the_list.contains(fff) )  the_list.add(fff);
            }
        }

        // safety check
        int dir_count = 0;
        for ( File f : the_list)
        {
            if ( f.isDirectory()) dir_count++;
            if (drag_and_drop_dbg) logger.log("going to drag/move:"+f.getAbsolutePath());
        }
        if ( dir_count >= 2)
        {
            // this code remains as a second line security
            // but it should not happen as:
            // 1. a single folder can be drag-and-dropped, OK
            // 2. the only way to select more than 1 folder is multiple selection, but there is a check in there
            // and folders are just NOT allowed because it is too easy to fool with the mouse and "woosh" ...
            // multiple folders are gone "who knows where"
            logger.log("popup alert to warn user");
            Alert a = new Alert(Alert.AlertType.CONFIRMATION,"Are you sure? It seems you are trying to move "+dir_count+" folders", ButtonType.OK,ButtonType.CANCEL);
            a.showAndWait();
            if ( a.getResult() == ButtonType.CANCEL)
            {
                drag_event.consume();
                return 0;
            }
        }


        //boolean destination_is_trash = Non_booleans_properties.is_this_trash(destination_dir,logger);
        if (drag_and_drop_dbg) logger.log("\n\naccept_drag_dropped_as_a_move_in " + origin+" destination= "+destination_dir+" is_trash="+ destination_is_trash);

        double x = drag_event.getX();
        double y = drag_event.getY();

        move_provider.move(
                destination_dir,
                destination_is_trash,
                the_list,
                owner,x,y,
                new Aborter("dummy",logger),
                logger);


        //Popups.popup_text("Drag and Drop", list.length() + " file(s) moved!", true);

        // tell the source
        drag_event.setDropCompleted(true);
        drag_event.consume();
        return the_list.size();
    }


    //**********************************************************
    public static void init_drag_and_drop_receiver_side(
            Move_provider move_provider,
            Node node,
            Window owner,
            Path path,
            boolean is_trash,
            Logger logger)
    //**********************************************************
    {
        node.setOnDragEntered(drag_event -> {
            if (Drag_and_drop.drag_and_drop_dbg) logger.log("OnDragEntered RECEIVER SIDE" );
            Look_and_feel_manager.set_background_for_setOnDragEntered(node,owner,logger);
            drag_event.consume();
        });
        node.setOnDragExited(drag_event -> {
            if (Drag_and_drop.drag_and_drop_dbg) logger.log("OnDragExited RECEIVER SIDE");
            Look_and_feel_manager.set_background_for_setOnDragExited(node,owner,logger);
            drag_event.consume();
        });
        node.setOnDragOver(drag_event -> {
            if (Drag_and_drop.drag_and_drop_dbg) logger.log("OnDragOver RECEIVER SIDE");

            drag_event.acceptTransferModes(TransferMode.MOVE);
            //GLUON drag_event.acceptTransferModes(TransferMode.COPY);
            Look_and_feel_manager.set_background_for_setOnDragOver(node,owner,logger);
            drag_event.consume();
        });
        node.setOnDragDropped(drag_event -> {
            if (Drag_and_drop.drag_and_drop_dbg) logger.log("OnDragDropped RECEIVER SIDE");
            Drag_and_drop.accept_drag_dropped_as_a_move_in(
                    move_provider,
                    drag_event,
                    path,
                    node,
                    "Browser item",
                    is_trash,
                    owner,
                    logger);
            drag_event.consume();
        });
    }


    // the reason to have the selection handler is that the user is dragging 1 items
    // but if the selection handler has been activated we will also drag all the selected items
    //**********************************************************
    public static void init_drag_and_drop_sender_side(
            Node node,
            Selection_handler selection_handler,
            Path path,
            Logger logger)
    //**********************************************************
    {
        node.setOnDragDetected(drag_event -> {
            if (drag_and_drop_dbg) logger.log("Item.init_drag_and_drop() drag detected SENDER SIDE");
            Dragboard db = node.startDragAndDrop(TransferMode.MOVE);
            //GLUON Dragboard db = node.startDragAndDrop(TransferMode.COPY);
            ClipboardContent content = new ClipboardContent();

            List<File> ll = new ArrayList<>();
            if(selection_handler != null)
            {
                logger.log("Item.init_drag_and_drop_SENDER_SIDE: selection_handler.isPresent()");
                ll.addAll(selection_handler.get_selected_files());
            }
            logger.log("Item.init_drag_and_drop_SENDER_SIDE: " + ll.size() + " files selected");
            // if we are here it is because the user is dragging an item
            if (!ll.contains(path.toFile()))
            {
                ll.add(path.toFile());
                logger.log("Item.init_drag_and_drop_SENDER_SIDE: added the ONE target file: " + path.toAbsolutePath() );
            }
            // this crashes the VM !!?? content.putFiles(ll);
            StringBuilder sb = new StringBuilder();
            for (File f : ll)
            {
                sb.append("\n").append(f.getAbsolutePath());
            }
            if ( Drag_and_drop.drag_and_drop_dbg) logger.log(" selected files: " + sb);
            content.put(DataFormat.PLAIN_TEXT, sb.toString());
            db.setContent(content);
            drag_event.consume();
        });

        node.setOnDragDone(drag_event -> {
            if (drag_event.getTransferMode() == TransferMode.MOVE)
            // GLUON if (drag_event.getTransferMode() == TransferMode.COPY)
            {
                if (drag_and_drop_dbg) logger.log("Item.init_drag_and_drop() SENDER SIDE: setOnDragDone for " + path.toAbsolutePath());
                /*
                DO NOT report it: it will be reported by the receiver Browser scene
                */

                if( selection_handler!= null)
                {
                    //browser.get().set_status(selection_handler.get().get_selected_files_count()+ " files have been dragged out");
                    selection_handler.reset_selection();
                    selection_handler.nothing_selected();
                }
            }
            drag_event.consume();
        });
    }





}
