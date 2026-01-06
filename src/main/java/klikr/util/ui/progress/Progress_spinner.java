// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.util.ui.progress;


import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeType;
import javafx.util.Duration;



//**********************************************************
public class Progress_spinner
//**********************************************************
{
    RotateTransition rotate_transition;
    StackPane stack_pane;

    //**********************************************************
    public Pane start()
    //**********************************************************
    {
        double size = 80;
        double stroke = 8;

        Arc arc = new Arc();
        arc.setType(ArcType.OPEN);
        arc.setFill(Color.TRANSPARENT);
        arc.setStroke(Color.web("#4285F4")); // Google blue
        arc.setStrokeWidth(stroke);
        arc.setStrokeType(StrokeType.CENTERED);
        arc.setStrokeLineCap(StrokeLineCap.ROUND);
        arc.setStartAngle(0);     // will rotate, start angle is arbitrary
        arc.setLength(270);       // 3/4 circle

        stack_pane = new StackPane(arc);
        stack_pane.setPadding(new Insets(20));
        stack_pane.setPrefSize(size, size);

        arc.centerXProperty().unbind();
        arc.centerYProperty().unbind();
        arc.radiusXProperty().unbind();
        arc.radiusYProperty().unbind();

        Platform.runLater(() -> {
                    double w = stack_pane.getWidth();
                    double h = stack_pane.getHeight();
                    double r = 20;//Math.max(10, Math.min(w, h) / 2.0 - arc.getStrokeWidth() / 2.0);
                    arc.setCenterX(w / 2.0);
                    arc.setCenterY(h / 2.0);
                    arc.setRadiusX(r);
                    arc.setRadiusY(r);
                });
        rotate_transition = new RotateTransition(Duration.millis(900), arc);
        rotate_transition.setFromAngle(0);
        rotate_transition.setToAngle(360);
        rotate_transition.setInterpolator(Interpolator.LINEAR);
        rotate_transition.setCycleCount(Animation.INDEFINITE);
        rotate_transition.play();
        //System.out.println("play transition");

        return stack_pane;
    }



    //**********************************************************
    public void stop()
    //**********************************************************
    {
        rotate_transition.pause();
        //System.out.println("pause transition");
    }

    public StackPane stack_pane()
    {
        return stack_pane;
    }
}
