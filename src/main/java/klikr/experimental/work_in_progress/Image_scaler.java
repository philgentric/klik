// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.experimental.work_in_progress;

import javafx.application.Application;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import klikr.util.log.Stack_trace_getter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Image_scaler extends Application {

    double SCALE_FACTOR = 0.5;

    private Image image;
    private double   scaledImageSize;

    @Override public void init() {
        //image = new Image(IMAGE_LOC);
        File f =  new File("0020.jpg");
        try (FileInputStream input_stream = new FileInputStream(f)) {
             image = new Image(input_stream);
        } catch (FileNotFoundException e) {
            System.out.println(Stack_trace_getter.get_stack_trace(e.toString()));
        } catch (IOException e) {
            System.out.println(Stack_trace_getter.get_stack_trace(e.toString()));
        } catch (OutOfMemoryError e) {
            System.out.println(Stack_trace_getter.get_stack_trace(e.toString()));
        } catch (Exception e) {
            System.out.println(Stack_trace_getter.get_stack_trace(e.toString()));
        }

        scaledImageSize = image.getWidth() * SCALE_FACTOR;
    }

    @Override public void start(Stage stage) {
        GridPane layout = new GridPane();
        layout.setHgap(10);
        layout.setVgap(10);
        ImageView imageView1 = new ImageView(image);
        boolean quality = true;
        if ( quality)
        {
            imageView1.setSmooth(true);
            imageView1.setCache(true);
            imageView1.setCacheHint(CacheHint.QUALITY);
        }
        else
        {
            imageView1.setSmooth(false);
            imageView1.setCache(true);
            imageView1.setCacheHint(CacheHint.SPEED);
        }
        imageView1.setFitWidth(scaledImageSize);
        imageView1.setPreserveRatio(true);

        ImageView imageView2 = new ImageView(image);
        imageView2.setSmooth(false);
        imageView2.setCache(true);
        imageView2.setCacheHint(CacheHint.SPEED);
        imageView2.setFitWidth(scaledImageSize);
        imageView2.setPreserveRatio(true);

        layout.addRow(
                0,
                withTooltip(
                        imageView1, //fittedImageView,
                        "Image fitted using ImageView fitWidth/fitHeight - ImageView smoothing false"
                )
                ,
                withTooltip(
                        imageView2,//resampledImageView,
                        "Image manually recreated as a new WritableImage using a PixelWriter"
                )
        );

        layout.addRow(
                1,
                centeredLabel("imageView1"),
                centeredLabel("imageView2")
                );
        layout.setAlignment(Pos.CENTER);

        layout.setStyle("-fx-background-color: cornsilk; -fx-padding: 10;");
        stage.setScene(
                new Scene(layout)
        );
        stage.show();
    }

    private Node withTooltip(Node node, String text) {
        Tooltip.install(node, new Tooltip(text));
        return node;
    }

    private Label centeredLabel(String text) {
        Label label = new Label(text);
        GridPane.setHalignment(label, HPos.CENTER);
        return label;
    }

    private Image resample(Image input, double scaleFactor)
    {
        final int W = (int) input.getWidth();
        final int H = (int) input.getHeight();
        final double S = scaleFactor;

        WritableImage output = new WritableImage((int)(W * S), (int)(H * S));

        PixelReader reader = input.getPixelReader();
        PixelWriter writer = output.getPixelWriter();

        if ( S > 1)
        {
            for (int y = 0; y < H; y++)
            {
                for (int x = 0; x < W; x++)
                {
                    final int argb = reader.getArgb(x, y);
                    for (int dy = 0; dy < S; dy++)
                    {
                        for (int dx = 0; dx < S; dx++)
                        {
                            int xxx = (int) (x * S + dx);
                            if (xxx >= (int) (W * S)) xxx = (int) (W * S) - 1;
                            int yyy = (int) (y * S + dy);
                            if (yyy >= (int) (H * S)) yyy = (int) (H * S) - 1;
                            writer.setArgb(xxx, yyy, argb);
                        }
                    }
                }
            }
        }
        else
        {
            for (int y = 0; y < H; y++)
            {
                for (int x = 0; x < W; x++)
                {
                    final int argb = reader.getArgb(x, y);
                    for (int dy = 0; dy < 10; dy ++)
                    {
                        for (int dx = 0; dx < 10; dx++)
                        {
                            int xxx = (int) ((double)x * S + (double)dx/10.0);
                            if (xxx >= (int) (W * S)) xxx = (int) (W * S) - 1;
                            int yyy = (int) ((double)y * S + (double)dy/10.0);
                            if (yyy >= (int) (H * S)) yyy = (int) (H * S) - 1;
                            writer.setArgb(xxx, yyy, argb);
                        }
                    }
                }
            }
        }
        return output;
    }

    public static void main(String[] args) { Application.launch(args); }
}