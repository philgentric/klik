package klik.util.ui;

import javafx.scene.control.Button;
import javafx.stage.Window;
import klik.look.Look_and_feel_manager;
import klik.util.log.Logger;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.TextField;
import javafx.scene.layout.Border;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import javafx.util.Duration;

//**********************************************************
public class Scrollable_text_field extends Region
//**********************************************************
{
    private static final boolean dbg = false;
    private final TextField text_field = new TextField();
    private Timeline scroll_timeline; // keeps the animation running
    private double delta_width; // most‑left translateX
    private boolean scroll_direction_to_left = true; // Direction flag for the running timeline
    private final Logger logger;

    //**********************************************************
    public Scrollable_text_field(String text, Button button, Window owner, Logger logger)
    //**********************************************************
    {
        this.logger = logger;
        text_field.setText(text);
        text_field.setStyle("-fx-caret-width: 8px;-fx-caret-color: blue;");
        getChildren().add(text_field);
        setMaxWidth(Double.MAX_VALUE); // Allow growing to fill parent
        setMaxHeight(Region.USE_PREF_SIZE); // Prevent vertical stretching

        text_field.setBorder(Border.EMPTY);
        //textField.setMouseTransparent(true);
        text_field.setFocusTraversable(true); // we don’t need focus
        text_field.setEditable(false); // change to false if you only want a label
        text_field.setOnMouseMoved(e -> onHover(e.getX()));
        text_field.setOnMouseExited(e -> stopScroll());
        text_field.setOnMouseClicked(e->{
            if ( e.getClickCount() == 2)
            {
                if ( button != null) button.fire();
            }
        });

        Look_and_feel_manager.set_TextField_look(text_field,false,owner,logger);
    }

    //**********************************************************
    @Override
    protected void layoutChildren()
    //**********************************************************
    {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0)
            return;

        /* 1️⃣ Position the TextField */
        double tfHeight = h;// - BAR_HEIGHT;
        if (tfHeight < 0) tfHeight = 0;
        text_field.resizeRelocate(0, 0, w, tfHeight);

        double text_width = computeTextWidth(text_field.getText());
        delta_width = text_width - w;
        //logger.log("delta_width="+ delta_width);
    }

    //**********************************************************
    @Override
    protected double computePrefWidth(double height)
    //**********************************************************
    {
        // Return a reasonable default, or delegate to textField
        return 200;
    }

    //**********************************************************
    @Override
    protected double computePrefHeight(double width)
    //**********************************************************
    {
        // We need enough space for the text field + the bar
        // If width is -1 (unknown), we can just ask for the text field's pref height
        double tfH = text_field.prefHeight(width);
        return tfH;
    }

    //**********************************************************
    @Override
    protected double computeMinHeight(double width)
    //**********************************************************
    {
        return computePrefHeight(width);
    }

    //**********************************************************
    private double computeTextWidth(String txt)
    //**********************************************************
    {
        Text t = new Text(txt);
        t.setFont(text_field.getFont());
        return t.getLayoutBounds().getWidth();
    }

    //**********************************************************
    private void onHover(double mouseX)
    //**********************************************************
    {
        //logger.log("onHover mouseX: " + mouseX);
        boolean newDirectionIsLeft = mouseX < getWidth() / 2;

        if (scroll_timeline == null || scroll_timeline.getStatus() != Animation.Status.RUNNING)
        {

            scroll_direction_to_left = newDirectionIsLeft;
            startScroll();
        }
        else
        {
            if (scroll_direction_to_left != newDirectionIsLeft) {
                if ( dbg) logger.log("Hover direction changed to " + (newDirectionIsLeft ? "LEFT" : "RIGHT"));
                scroll_direction_to_left = newDirectionIsLeft;
            }
        }
    }

    //**********************************************************
    private void startScroll()
    //**********************************************************
    {
        if (delta_width <= 0)
        {
            if ( dbg) logger.log("NOP startScroll: maxScrollX=" + delta_width);
            return; // No need to scroll if text fits
        }
        if ( dbg) logger.log("startScroll: maxScrollX=" + delta_width);

        if (scroll_timeline != null && scroll_timeline.getStatus() == Animation.Status.RUNNING)
        {
            if ( dbg) logger.log("already scrolling ");
            return; // already scrolling
        }

        if ( dbg) logger.log("Starting scroll. Direction: " + (scroll_direction_to_left ? "LEFT" : "RIGHT"));

        scroll_timeline = new Timeline(
                new KeyFrame(Duration.millis(20), e -> {
                    //double delta = scrollSpeed * 0.02; // 20ms per frame

                    //int target;
                    if (scroll_direction_to_left)
                    {
                        //logger.log("Animation to left");
                        text_field.selectBackward();
                    }
                    else
                    {
                        //logger.log("Animation to right");
                        text_field.selectForward();
                    }

                    text_field.deselect();
                    //current = target;
                }));
        scroll_timeline.setCycleCount(Animation.INDEFINITE);
        scroll_timeline.play();
    }

    //**********************************************************
    private void stopScroll()
    //**********************************************************
    {
        if (scroll_timeline != null) {
            if ( dbg) logger.log("Stopping scroll");
            scroll_timeline.stop();
        }
    }
}