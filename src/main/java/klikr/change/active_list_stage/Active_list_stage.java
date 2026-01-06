// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.change.active_list_stage;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.stage.Window;
import klikr.look.Look_and_feel_manager;
import klikr.util.log.Logger;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

//**********************************************************
public class Active_list_stage
//**********************************************************
{

    public static final int WIDTH = 1000;

    public final VBox vbox;
    Datetime_to_signature_source source;
    public final Active_list_stage_action on_action;
    public final Logger logger;
    private final Stage stage;

    //**********************************************************
    public static Active_list_stage show_active_list_stage(String title, Datetime_to_signature_source source_, Active_list_stage_action on_action, Window owner, Logger logger_)
    //**********************************************************
    {
        Active_list_stage returned = new Active_list_stage(title,source_,on_action,owner,logger_);
        return returned;
    }

    //**********************************************************
    private Active_list_stage(String title, Datetime_to_signature_source source_, Active_list_stage_action on_action, Window owner, Logger logger_)
    //**********************************************************
    {
        logger = logger_;
        source = source_;
        this.on_action = on_action;
        // List<Button> list = new ArrayList<>();
        ScrollPane sp = new ScrollPane();
        sp.setPrefSize(WIDTH, 1000);
        vbox = new VBox();
        sp.setContent(vbox);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);

        stage = new Stage();
        stage.initOwner(owner);
        stage.setHeight(600);
        stage.setWidth(1000);

        Scene scene = new Scene(sp, 1000, 600, Color.WHITE);
        stage.setTitle(title);
        stage.setScene(scene);
        stage.show();


        define();

        stage.widthProperty().addListener((observable, oldValue, newValue) -> {
            for ( Node x : vbox.getChildren())
            {
                if ( x instanceof Button) {
                    Button b = (Button) x;
                    b.setPrefWidth(stage.getWidth()-10);
                }
            }
        });
    }

    //**********************************************************
    public void define()
    //**********************************************************
    {
        //logger.log(Stack_trace_getter.get_stack_trace("define!!!"));
        Map<LocalDateTime, String> map = source.get_map_of_date_to_signature();
        List<LocalDateTime> keys = new ArrayList<>(map.keySet());
        Collections.sort(keys);
        Collections.reverse(keys);
        vbox.getChildren().clear();
        for(LocalDateTime local_date_time_as_key : keys)
        {
            String time_stamp_string = local_date_time_as_key.getYear()
                    +" "+ local_date_time_as_key.getMonth()+
                    " " +String.format("%02d", (Integer)local_date_time_as_key.getDayOfMonth())
                    +" "+String.format("%02d", (Integer)local_date_time_as_key.getHour())
                    +"h"+String.format("%02d", (Integer)local_date_time_as_key.getMinute());
            String button_text = time_stamp_string+ "\n"+ map.get(local_date_time_as_key);

            Button b = new Button(button_text);
            b.setAlignment(Pos.BASELINE_LEFT);
            b.setTextAlignment(TextAlignment.LEFT);
            b.setPrefWidth(WIDTH);
            //Font_size.apply_font_size(b,logger);
            Look_and_feel_manager.set_button_look(b, true, stage,logger);

            if (on_action != null)
            {
                b.setOnAction(actionEvent ->
                {
                    String signature = map.get(local_date_time_as_key);
                    logger.log("ACTION for: "+signature);
                    on_action.on_click(signature);
                });
            }
            vbox.getChildren().add(b);
        }
    }


}
