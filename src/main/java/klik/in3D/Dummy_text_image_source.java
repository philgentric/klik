// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.in3D;

import javafx.geometry.Insets;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class Dummy_text_image_source implements Image_source
{
    private final int size;
    private final int max;
    int i = 0;
    Dummy_text_image_source(int size, int max)
    {
        this.size = size;
        this.max = max;
    }


    @Override
    public Image_and_path get(int i)
    {
        return null;//new Image_and_path(create_image_with_text("-"+i+"-", size, size), null);
    }

    @Override
    public int how_many_items()
    {
        return max;//Integer.MAX_VALUE;
    }

    private Image create_image_with_text(String text, double width, double height) {
        // Create text component
        Text textNode = new Text(text);
        textNode.setFont(Font.font("Arial", FontWeight.BOLD, 56));
        textNode.setFill(Color.BLACK);

        // Create container with white background
        StackPane container = new StackPane(textNode);
        container.setBackground(new Background(new BackgroundFill(Color.WHITE, null, null)));
        container.setPrefSize(width, height);
        container.setPadding(new Insets(20));

        // Ensure the container is properly sized
        container.applyCss();
        container.layout();

        // Take snapshot
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.WHITE);
        WritableImage image = container.snapshot(params, null);

        return image;
    }

}
