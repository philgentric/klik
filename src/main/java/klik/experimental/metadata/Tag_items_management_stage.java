// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.experimental.metadata;

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
import klik.util.execute.actor.Aborter;
import klik.properties.Properties_manager;
import klik.util.log.Logger;

//**********************************************************
public class Tag_items_management_stage
//**********************************************************
{
    private static final boolean ultra_dbg = false;
    private static final String STORED_TAG = "StoredTag";

    //**********************************************************
    public static void open_tag_management_stage(Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {

        logger.log("Tag_management_stage ");
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
        the_stage.setTitle("Manage stored tags");

        //IProperties pm = Shared_services.main_properties();

        the_stage.show();
    }

    //**********************************************************
    private static void add_a(FlowPane the_pane, String k, Properties_manager pm, Logger logger)
    //**********************************************************
    {
        String what = pm.get(k);
        the_pane.getChildren().add(new Text(what));
        HBox box = new HBox();
        the_pane.getChildren().add(box);
        for ( Pair<String, String> p : pm.get_pairs_for_base(what))
        {
            add_one(pm,
                    //the_pane, what,
                    box, p, logger);
        }
        {
            TextField text_edit = new TextField("add a <"+what+"> here and type enter to add it");
            text_edit.setOnAction(actionEvent -> {

                // this is the action to ADD a new item in the "category"

                String result = text_edit.getText();
                if ( !result.isEmpty())
                {
                    Pair<String,String> p = pm.add_for_base(what, result);
                    add_one(pm,
                            //the_pane,what,
                            box,p, logger);
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
    private static void add_one(Properties_manager pm,
                                //FlowPane the_pane, String what,
                                HBox box, Pair<String, String> p, Logger logger)
    //**********************************************************
    {
            TextField text_edit = new TextField(p.getValue());
            logger.log("add_one " + p);
            box.getChildren().add(text_edit);
            text_edit.setOnAction(actionEvent -> {

                // this is the action to EDIT a new item in the "category"

                String result = text_edit.getText();
                if (result.isEmpty()) {
                    pm.delete(p.getKey(), true);
                    logger.log("remove " + p);
                    box.getChildren().remove(text_edit);
                } else {
                    pm.add(p.getKey(), result);
                }
                actionEvent.consume();
            });

    }
}
