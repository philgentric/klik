package klik.util.ui;

import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PopupControl;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.List;

public class Visually_impaired_menus extends PopupControl
{
    public Visually_impaired_menus(List<MenuItem> items, double maxHeight)
    {
        VBox itemBox = new VBox(4);
        items.forEach(item -> {
            Button b = new Button(item.getText());
            b.setMaxWidth(Double.MAX_VALUE);
            b.setOnAction(e -> { hide(); item.fire(); });
            itemBox.getChildren().add(b);
        });

        ScrollPane scrollPane = new ScrollPane(itemBox);
        scrollPane.setPrefHeight(Math.min(maxHeight, itemBox.getLayoutBounds().getHeight()));
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        BorderPane root = new BorderPane(scrollPane);

//        root.getStyleClass().add("scrollable-menu");
        getScene().setRoot(root);
        setAutoHide(true);
    }

    public void show_at(Window owner, double x, double y) {
        show(owner, x, y);
    }

    public void show_under(Node owner) {
        Bounds b = owner.localToScreen(owner.getBoundsInLocal());
        show(owner, b.getMinX(), b.getMaxY());
    }
}