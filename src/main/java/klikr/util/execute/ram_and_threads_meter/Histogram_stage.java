// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ./Value_getter.java
//SOURCES ./Real_to_pixel.java
//SOURCES ./Graph_for_meters.java
package klikr.util.execute.ram_and_threads_meter;

import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import klikr.look.Look_and_feel_manager;
import klikr.util.log.Logger;

import java.util.List;

//**********************************************************
public class Histogram_stage
//**********************************************************
{

    public static final double DISPLAY_PIXEL_HEIGHT = 600;
    private static Stage instance;

    //**********************************************************
    public static void show_stage(List<Double> values, Logger logger)
    //**********************************************************
    {
        instance = create_stage(values,logger);
    }

    //**********************************************************
    private static Stage create_stage(List<Double> values, Logger logger)
    //**********************************************************
    {
        Stage stage = new Stage();
        HBox hbox = new HBox();
        int x_offset = 5;
        {
            Graph_for_histograms graph = new Graph_for_histograms("Histogram",values, x_offset,Color.RED, stage,logger);
            hbox.getChildren().add(graph.vbox);
        }

        Scene scene = new Scene(hbox, Look_and_feel_manager.get_instance(stage,logger).get_background_color());
        stage.setScene(scene);
        stage.setTitle("title");
        stage.setMinWidth(1200);
        stage.setMinHeight(DISPLAY_PIXEL_HEIGHT+100);
        stage.show();
        return stage;
    }


}
