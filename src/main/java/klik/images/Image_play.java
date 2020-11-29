package klik.images;

import javafx.geometry.Insets;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import klik.util.Logger;

public class Image_play {
    private Image_file_source image_file_source;
    private Image_context current_image;
    Logger logger;
    BorderPane border_pane;
    public Image_play(Image_context local_ic, Logger logger_)
    {
        logger = logger_;
        current_image = local_ic;
        image_file_source = Image_file_source.get_Image_file_source(current_image.path.getParent(),logger);
    }

    void set_background()
    {
        if ((current_image.path.getFileName().toString().endsWith(".png")) || (current_image.path.getFileName().toString().endsWith(".PNG"))) {
            border_pane.setBackground(new Background(new BackgroundFill(Color.GREY, CornerRadii.EMPTY, Insets.EMPTY)));
        } else if ((current_image.path.getFileName().toString().endsWith(".gif")) || (current_image.path.getFileName().toString().endsWith(".GIF"))) {
            border_pane.setBackground(new Background(new BackgroundFill(Color.DARKGRAY, CornerRadii.EMPTY, Insets.EMPTY)));
        } else {
            border_pane.setBackground(new Background(new BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY)));
        }

    }
/*
    //**********************************************************
    private void handle_mouse_clicked_secondary(Logger logger, Stage stage, Pane pane, MouseEvent e)
    //**********************************************************
    {
        logger.log("handle_mouse_clicked_secondary");

        final ContextMenu contextMenu = new ContextMenu();
        contextMenu.setStyle("-fx-foreground-color: white;-fx-background-color: darkgrey;");
        MenuItem info = new MenuItem("Path=" + current_image.f.toAbsolutePath());
        contextMenu.getItems().add(info);
        info.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                show_exif_stage(logger, current_image);
            }
        });
        MenuItem edit = new MenuItem("Edit");
        contextMenu.getItems().add(edit);
        edit.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                edit(logger);
            }
        });


        MenuItem browse = new MenuItem("Browse the dir this image is in, in a new browsing window");
        contextMenu.getItems().add(browse);
        browse.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                logger.log("browse this!");
                Browser.create_browser(null, 100, 100, ic.f.getParent(), false, logger);

            }
        });

        MenuItem rename = new MenuItem("Rename (r)");
        contextMenu.getItems().add(rename);
        rename.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                ask_user_for_new_name2(current_image);
            }
        });

        MenuItem search_k = new MenuItem("Search images with names containing similar keywords (k)");
        contextMenu.getItems().add(search_k);
        search_k.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                search_k();
            }
        });

        MenuItem search_y = new MenuItem("Search images with your keywords in their names (y)");
        contextMenu.getItems().add(search_y);
        search_k.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                find();
            }
        });

        MenuItem click_to_zoom = new MenuItem("Set click-to-zoom mode (z): use the mouse to select a zoom area");
        contextMenu.getItems().add(click_to_zoom);
        click_to_zoom.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                set_mouse_mode(Mouse_mode.click_to_zoom);
            }
        });

        MenuItem drag_and_drop = new MenuItem("Set drag-and-drop mode (m): drag-and-drop images to another directory");
        contextMenu.getItems().add(drag_and_drop);
        drag_and_drop.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                set_mouse_mode(Mouse_mode.drag_and_drop);
            }
        });

        MenuItem pix_for_pix = new MenuItem("Set pix-for-pix mode (=): use mouse to change the visible part of a large image");
        contextMenu.getItems().add(pix_for_pix);
        pix_for_pix.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                set_mouse_mode(Mouse_mode.pix_for_pix);
            }
        });


        //let_the_user_choose_a_move_target_dir(logger, stage, contextMenu);
        //Tool_box.fx_mover(contextMenu, logger, ic.f, last_moved);
        contextMenu.show(pane, e.getScreenX(), e.getScreenY());
    }
*/
}
