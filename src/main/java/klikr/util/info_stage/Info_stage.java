// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.util.info_stage;

import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.Stage;

import java.util.List;

/*
pops up a new frame with informative text structured in "Lines"
 */
//**********************************************************
public class Info_stage
//**********************************************************
{

    //**********************************************************
    public static void show_info_stage(String title,
                                       List<Line_for_info_stage> lines,
                                       Image icon, Runnable on_window_close)
    //**********************************************************
    {
        TextFlow textFlow = new TextFlow();
        textFlow.setLayoutX(40);
        textFlow.setLayoutY(40);

        for(Line_for_info_stage l : lines)
        {
            if (l.is_bold) one_line_bold(l.text,textFlow);
            else one_line(l.text,textFlow);
        }

        VBox vb = new VBox();
        if ( icon != null) vb.getChildren().add(new ImageView(icon));
        ScrollPane sp = new ScrollPane();
        sp.setPrefSize(1000, 1000);
        sp.setContent(textFlow);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        vb.getChildren().add(sp);
        Stage local_stage = new Stage();
        local_stage.setHeight(600);
        local_stage.setWidth(1000);

        Scene scene = new Scene(vb, 1000, 600, Color.WHITE);
        local_stage.setTitle(title);
        local_stage.setScene(scene);
        local_stage.show();

        if (on_window_close != null)
        {
            local_stage.setOnCloseRequest(windowEvent -> on_window_close.run());
        }
    }

    //**********************************************************
    private static void one_line_bold(String s, TextFlow textFlow)
    //**********************************************************
    {
        Text t = new Text(s);
        t.setFont(Font.font("verdana", FontWeight.BOLD, FontPosture.REGULAR,24));
        textFlow.getChildren().add(t);
        textFlow.getChildren().add(new Text(System.lineSeparator()));
    }

    //**********************************************************
    private static void one_line(String s, TextFlow textFlow)
    //**********************************************************
    {
        Text t = new Text(s);
        t.setFont(Font.font("verdana", FontWeight.NORMAL, FontPosture.REGULAR,16));
        textFlow.getChildren().add(t);
        textFlow.getChildren().add(new Text(System.lineSeparator()));
    }


}
