// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.util.info_stage;

import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.Stage;
import klikr.util.Kontext;

import java.util.List;

/*
pops up a new frame with informative text structured in "Lines"
 */
//**********************************************************
public class Info_stage
//**********************************************************
{
    private final String title;
    private final List<Line_for_info_stage> lines;
    private final Image icon;
    private double large_font_size = 24;
    private double small_font_size = 16;
    private final TextFlow textFlow = new TextFlow();
    private final Kontext context;

    //**********************************************************
    public void larger_font_size()
    //**********************************************************
    {
        large_font_size = large_font_size * 1.2;
        small_font_size = small_font_size * 1.2;
        clear();
        add_all_lines();
        context.log("larger font size");
    }

    //**********************************************************
    public void smaller_font_size()
    //**********************************************************
    {
        large_font_size = large_font_size * 0.8;
        small_font_size = small_font_size * 0.8;
        clear();
        add_all_lines();
        context.log("smaller font size");
    }



    //**********************************************************
    public static void show_info_stage(String title,
                                       List<Line_for_info_stage> lines,
                                       Image icon, Kontext context)
    //**********************************************************
    {
        Info_stage local = new Info_stage(title,lines,icon,context);
    }

    //**********************************************************
    public Info_stage(String title,
                      List<Line_for_info_stage> lines,
                      Image icon,
                      Kontext context)
    //**********************************************************
    {
        this.title = title;
        this.lines = lines;
        this.icon = icon;
        this.context = context;
        textFlow.setLayoutX(40);
        textFlow.setLayoutY(40);

        add_all_lines();

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

        Shortcuts.larger(scene,this::larger_font_size,context);
        Shortcuts.smaller(scene,this::smaller_font_size,context);
        Shortcuts.debug(scene,context);


        local_stage.setTitle(title);
        local_stage.setScene(scene);
        local_stage.show();
        local_stage.requestFocus();
        local_stage.toFront();
    }

    //**********************************************************
    private void add_all_lines()
    //**********************************************************
    {
        for(Line_for_info_stage l : lines)
        {
            if (l.is_bold) one_line_bold(l.text);
            else one_line(l.text);
        }
    }

    //**********************************************************
    private void one_line_bold(String s)
    //**********************************************************
    {
        Text t = new Text(s);
        t.setFont(Font.font("AtkinsonHyperlegible-Bold.ttf", FontWeight.BOLD, FontPosture.REGULAR,large_font_size));
        textFlow.getChildren().add(t);
        textFlow.getChildren().add(new Text(System.lineSeparator()));
    }

    //**********************************************************
    private void one_line(String s)
    //**********************************************************
    {
        Text t = new Text(s);
        t.setFont(Font.font("AtkinsonHyperlegible-Bold.ttf", FontWeight.NORMAL, FontPosture.REGULAR,small_font_size));
        textFlow.getChildren().add(t);
        textFlow.getChildren().add(new Text(System.lineSeparator()));
    }

    //**********************************************************
    private  void clear()
    //**********************************************************
    {
        textFlow.getChildren().clear();
    }


}
