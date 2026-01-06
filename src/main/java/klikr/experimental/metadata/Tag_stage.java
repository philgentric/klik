// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ./Tag_type.java
package klikr.experimental.metadata;

import javafx.geometry.HPos;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Pair;
import klikr.util.execute.actor.Aborter;
import klikr.util.log.Logger;

import java.nio.file.Path;
import java.util.Set;

//**********************************************************
public class Tag_stage
//**********************************************************
{

    //**********************************************************
    public static void open_tag_edit_stage(Path path, Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        open_tag_stage( path, true,  owner, aborter,logger);
    }
    //**********************************************************
    public static void open_tag_view_stage(Path path, Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        open_tag_stage( path, false, owner, aborter, logger);
    }

    //**********************************************************
    public static void open_tag_stage(Path path, boolean editable, Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        logger.log("Tag_stage "+path);
        Stage the_stage = new Stage();
        FlowPane the_pane = new FlowPane();
        the_pane.setOrientation(Orientation.VERTICAL);
        the_pane.setColumnHalignment(HPos.LEFT); // align labels on left

        Scene the_scene = new Scene(the_pane);//, W, H);
        the_stage.setScene(the_scene);
        the_stage.show();
        the_stage.setX(200);
        the_stage.setY(200);
        the_stage.setWidth(1200);
        the_stage.setHeight(300);
        the_stage.setTitle("Tags for "+path.toAbsolutePath());
        Metadata_handler local_Metadata_handler = new Metadata_handler(path,owner,aborter,logger);
        //if ( ultra_dbg)
        {
            Set<String> l = local_Metadata_handler.get_all_keys();
            for (String k : l) {
                logger.log("FOUND KEY = " + k + " => " + local_Metadata_handler.get(k));
            }
        }
        for ( Tag_type t: Tag_type.values())
        {
            add_a(local_Metadata_handler, the_pane, t.name(), editable, logger);
        }
    }

    //**********************************************************
    private static void add_a(Metadata_handler local_Metadata_handler, FlowPane the_pane, String what, boolean editable, Logger logger)
    //**********************************************************
    {
        the_pane.getChildren().add(new Text(what));
        HBox box = new HBox();
        the_pane.getChildren().add(box);
        for ( Pair<String, String> p : local_Metadata_handler.get_pairs_for_base(what))
        {
            add_one(local_Metadata_handler, box, p, editable, logger);
        }
        if ( editable)
        {
            TextField text_edit = new TextField("add a <"+what+"> here and type enter to add it");
            text_edit.setOnAction(actionEvent -> {

                // this is the action to ADD a new item in the "category"

                String result = text_edit.getText();
                if ( !result.isEmpty())
                {
                    Pair<String,String> p = local_Metadata_handler.add_for_base(what, result);
                    add_one(local_Metadata_handler,box,p, editable, logger);
                    text_edit.setText("");
                }
                actionEvent.consume();
            });
            the_pane.getChildren().add(new Text("add a " + what));
            the_pane.getChildren().add(text_edit);
        }
        Separator separator = new Separator();
        separator.setMaxWidth(200);
        separator.setHalignment(HPos.LEFT);

        the_pane.getChildren().add(new Separator());

    }

    //**********************************************************
    private static void add_one(Metadata_handler local_Metadata_handler,  HBox box, Pair<String, String> p, boolean editable, Logger logger)
    //**********************************************************
    {
        if (editable) {
            TextField text_edit = new TextField(p.getValue());
            logger.log("add_one " + p);
            box.getChildren().add(text_edit);
            text_edit.setOnAction(actionEvent -> {
                // this is the action to EDIT a new item in the "category"
                String result = text_edit.getText();
                if (result.isEmpty()) {
                    local_Metadata_handler.delete(p.getKey(), true, logger);
                    logger.log("remove " + p);
                    box.getChildren().remove(text_edit);
                } else {
                    local_Metadata_handler.add(p.getKey(), result);
                }
                actionEvent.consume();
            });
        }
        else
        {
            Text text = new Text(p.getValue());
            box.getChildren().add(text);
            box.getChildren().add(new Separator());
        }
    }
}
