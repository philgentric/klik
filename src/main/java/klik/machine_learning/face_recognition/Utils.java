// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.machine_learning.face_recognition;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import klik.util.ui.Jfx_batch_injector;
import klik.util.log.Logger;

import java.nio.file.Path;

//**********************************************************
public class Utils
//**********************************************************
{
    //**********************************************************
    public static void display(int size, Image image1, Image image2, Image image3, String title, String label, Logger logger)
    //**********************************************************
    {


        Runnable r = () -> {
            Stage stage = new Stage();
            stage.setTitle(title);
            VBox vBox = new VBox();
            HBox hBox = new HBox();
            vBox.getChildren().add(hBox);

            if ( image1 !=null)
            {
                ImageView iv = new ImageView(image1);
                iv.setPreserveRatio(true);
                iv.setFitWidth(size);
                Pane image_pane = new StackPane(iv);
                hBox.getChildren().add(image_pane);
            }
            if ( image2 !=null)
            {
                ImageView iv = new ImageView(image2);
                iv.setPreserveRatio(true);
                iv.setFitWidth(size);
                Pane image_pane = new StackPane(iv);
                hBox.getChildren().add(image_pane);
            }
            if ( image3 !=null)
            {
                ImageView iv = new ImageView(image3);
                iv.setPreserveRatio(true);
                iv.setFitWidth(size);
                Pane image_pane = new StackPane(iv);
                hBox.getChildren().add(image_pane);
            }

            Label ll = new Label(label);
            vBox.getChildren().add(ll);
            stage.setScene(new Scene(vBox));
            stage.show();
        };

        if ( Platform.isFxApplicationThread())
        {
            r.run();
        }
        else
        {
            Jfx_batch_injector.inject(r, logger);
        }
    }

    public static Image get_image(Path path)
    {
        return new Image(path.toFile().toURI().toString());
    }
}
