// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.images;

import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.*;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import klik.browser.Drag_and_drop;
import klik.look.Jar_utils;
import klik.look.Look_and_feel_manager;
import klik.properties.Cache_folder;
import klik.properties.Non_booleans_properties;
import klik.util.files_and_paths.Static_files_and_paths_utilities;
import klik.util.image.Static_image_utilities;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

//**********************************************************
public class Mouse_handling_for_Image_window
//**********************************************************
{
    private final static boolean dbg = false;
    private final Image_window image_window;
    private final Logger logger;
    private boolean previous_mouse_valid = false;
    private double previous_mouse_x;
    private double previous_mouse_y;
    private double initial_mouse_x;
    private double initial_mouse_y;
    private boolean user_is_selecting_zoom_area = false;
    private Rectangle user_defined_zoom_area = null;

    Mouse_mode mouse_mode = Mouse_mode.drag_and_drop;

    EventHandler<MouseEvent> mouse_pressed_click_to_zoom_event_handler;
    EventHandler<MouseEvent> mouse_dragged_click_to_zoom_event_handler;
    EventHandler<MouseEvent> mouse_released_click_to_zoom_event_handler;

    EventHandler<MouseEvent> mouse_pressed_pix_for_pix_event_handler;
    EventHandler<MouseEvent> mouse_dragged_pix_for_pix_event_handler;
    EventHandler<MouseEvent> mouse_released_pix_for_pix_event_handler;

    public static Path cropped_image_path = null;


    //**********************************************************
    public Mouse_handling_for_Image_window(Image_window image_window_, Logger logger)
    //**********************************************************
    {
        image_window = image_window_;
        this.logger = logger;
    }


    //**********************************************************
    void mouse_pressed_pix_for_pix(MouseEvent mouse_event,Pane pane)
    //**********************************************************
    {
        Point2D in_parent = translate_mouse_event_to_in_pane(mouse_event, pane);
        logger.log("mouse_pressed:");
        previous_mouse_valid = true;
        previous_mouse_x = in_parent.getX();
        previous_mouse_y = in_parent.getY();
    }

    //**********************************************************
    void mouse_released_pix_for_pix(MouseEvent e)
    //**********************************************************
    {
        logger.log("mouse_released_pix_for_pix:");
        previous_mouse_valid = false;
    }

    //**********************************************************
    void mouse_dragged_pix_for_pix(MouseEvent mouse_event, Pane pane)
    //**********************************************************
    {
        Point2D in_parent = translate_mouse_event_to_in_pane(mouse_event, pane);

        if (previous_mouse_valid)
        {
            double dx = in_parent.getX() - previous_mouse_x;
            double dy = in_parent.getY() - previous_mouse_y;
            //logger.log("mouse_dragged_pix_for_pix: dx,dy=" + dx + "," + dy);

            move_image_internal(dx, dy);
        }
        previous_mouse_valid = true;
        previous_mouse_x = in_parent.getX();
        previous_mouse_y = in_parent.getY();
    }

    //**********************************************************
    private void move_image_internal(double dx, double dy)
    //**********************************************************
    {
        Optional<Image_context> local = image_window.image_display_handler.get_image_context();
        if (local.isEmpty()) return;
        //if (local.the_image_view == null) return;
        /*
        this is for moving the imageview in the scene
        local.imageView.setTranslateX(local.imageView.getTranslateX()+dx);
        local.imageView.setTranslateY(local.imageView.getTranslateY()+dy);
         what we want is to move the image INSIDE the viewport
         */
        local.get().move_viewport(dx,dy);

    }

    //**********************************************************
    private static Point2D translate_mouse_event_to_in_pane(MouseEvent mouse_event, Pane pane)
    //**********************************************************
    {
        Point2D in_parent = pane.sceneToLocal(mouse_event.getSceneX(), mouse_event.getSceneY());
        return in_parent;
    }

    //**********************************************************
    void mouse_pressed_click_to_zoom(MouseEvent mouse_event, Pane pane)
    //**********************************************************
    {
        logger.log("mouse_pressed_click_to_zoom: event="+mouse_event );

        Point2D in_parent = translate_mouse_event_to_in_pane(mouse_event, pane);
        initial_mouse_x = in_parent.getX();
        initial_mouse_y = in_parent.getY();

        logger.log("mouse_pressed_click_to_zoom: x="+initial_mouse_x+" y="+initial_mouse_y );

        user_defined_zoom_area = new Rectangle(initial_mouse_x, initial_mouse_y, 5, 5);
        user_defined_zoom_area.setFill(Color.TRANSPARENT);
        user_defined_zoom_area.setVisible(true);
        Color c = new javafx.scene.paint.Color(1/*red*/, 0/*green*/, 0/*blue*/, /*opacity*/0.2);
        user_defined_zoom_area.setStroke(c);
        user_defined_zoom_area.setStrokeWidth(10.0f);
        user_defined_zoom_area.setManaged(false);// otherwise the rectangle is always centered i.e. x,y is ignored
        pane.getChildren().add(user_defined_zoom_area);
        user_defined_zoom_area.toFront();
    }
    //**********************************************************
    void mouse_released_click_to_zoom(Pane pane)
    //**********************************************************
    {
        if ( image_window.image_display_handler.get_image_context().isEmpty()) return;

        Image_context local = image_window.image_display_handler.get_image_context().get();

        logger.log("mouse_released_local_zoom:");
        pane.getChildren().remove(user_defined_zoom_area);
        if (!user_is_selecting_zoom_area) return;
        user_is_selecting_zoom_area = false;

        if (user_defined_zoom_area.getWidth() < 5) return;
        if (user_defined_zoom_area.getHeight() < 5) return;

        if (local.the_image_view.getViewport() != null)
        {
            logger.log("sorry, only one zoom supported at this time");
            local.the_image_view.setViewport(null);
            return;
        }

        // need to correct the rectangle inside the picture

        logger.log("image :" + local.image.getWidth() + "x" + local.image.getHeight());

        Bounds bounds = local.the_image_view.getLayoutBounds();
        logger.log("image View bounds x/y:" + bounds.getMinX() + "/" + bounds.getMinY() + "w/h :" + bounds.getWidth() + "x" + bounds.getHeight());
        logger.log("rectangle1 :" + user_defined_zoom_area.getX() + "/" + user_defined_zoom_area.getY() + " " + user_defined_zoom_area.getWidth() + "x" + user_defined_zoom_area.getHeight());

        double scale = local.image.getWidth() / bounds.getWidth();

        Rectangle2D view_port = new Rectangle2D(
                (user_defined_zoom_area.getX() - local.the_image_view.getLayoutX()) * scale,
                (user_defined_zoom_area.getY() - local.the_image_view.getLayoutY()) * scale,
                user_defined_zoom_area.getWidth() * scale,
                user_defined_zoom_area.getHeight() * scale
        );
        logger.log("rectangle2 :" + view_port.getMinX() + "/" + view_port.getMinY() + " " + view_port.getWidth() + "x" + view_port.getHeight());

        local.the_image_view.setViewport(view_port);

        Image cropped_image = new
                WritableImage(
                local.the_image_view.getImage().getPixelReader(),
                (int)view_port.getMinX(),
                (int)view_port.getMinY(),
                (int)view_port.getWidth(),
                (int)view_port.getHeight());
        Path icon_cache_dir = Static_files_and_paths_utilities.get_cache_dir(Cache_folder.klik_icon_cache, image_window.stage, logger);

        cropped_image_path = icon_cache_dir.resolve("cropped_image.png");
        Static_image_utilities.write_png_to_disk(cropped_image,cropped_image_path,logger);

    }


    //**********************************************************
    void mouse_dragged_click_to_zoom(MouseEvent mouse_event, Pane pane)
    //**********************************************************
    {
        if ( dbg) logger.log("mouse_dragged_local_zoom:");
        Point2D in_parent = translate_mouse_event_to_in_pane(mouse_event, pane);

        if (user_defined_zoom_area != null)
        {
            user_is_selecting_zoom_area = true;
            double new_x = in_parent.getX();
            double new_y = in_parent.getY();
            user_defined_zoom_area.setX(Math.min(initial_mouse_x, new_x));
            user_defined_zoom_area.setY(Math.min(initial_mouse_y, new_y));
            user_defined_zoom_area.setWidth(Math.abs(new_x - initial_mouse_x));
            user_defined_zoom_area.setHeight(Math.abs(new_y - initial_mouse_y));
        }

    }

/*
    //**********************************************************
    private void mouse_pressed_move_mode(MouseEvent e)
    //**********************************************************
    {
        logger.log("mouse_pressed_move_mode:");
        old_mouse_x = e.getX();
        old_mouse_y = e.getY();

    }

    //**********************************************************
    private void mouse_dragged_move_mode(MouseEvent e)
    //**********************************************************
    {
        logger.log("Image_stage mouse_dragged_move_mode: drag detected");


        Dragboard db = image_stage.image_display_handler.get_image_context().the_image_view.startDragAndDrop(TransferMode.MOVE);

        ClipboardContent content = new ClipboardContent();
        List<File> l = new ArrayList<>();
        l.add(image_stage.image_display_handler.get_image_context().path.toFile());
        content.putFiles(l);
        db.setContent(content);
        // event.consume();
    }

    //**********************************************************
    private void mouse_released_move_mode(MouseEvent e)
    //**********************************************************
    {
        logger.log("mouse_released_move_mode:");

    }
*/

    //**********************************************************
    public void create_event_handlers(Image_window image_stage, Pane target_pane)
    //**********************************************************
    {
        mouse_pressed_click_to_zoom_event_handler = mouseEvent -> {
            if (mouseEvent.getButton() != MouseButton.SECONDARY) mouse_pressed_click_to_zoom(mouseEvent,target_pane);
        };
        mouse_pressed_pix_for_pix_event_handler = mouseEvent -> {
            if (mouseEvent.getButton() != MouseButton.SECONDARY) mouse_pressed_pix_for_pix(mouseEvent,target_pane);
        };

        mouse_dragged_click_to_zoom_event_handler = mouseEvent -> {
            if (mouseEvent.getButton() != MouseButton.SECONDARY) mouse_dragged_click_to_zoom(mouseEvent, target_pane);
        };
        mouse_dragged_pix_for_pix_event_handler = mouseEvent -> {
            if (mouseEvent.getButton() != MouseButton.SECONDARY) mouse_dragged_pix_for_pix(mouseEvent,target_pane);
        };

        mouse_released_click_to_zoom_event_handler = mouseEvent -> {
            if (mouseEvent.getButton() != MouseButton.SECONDARY) mouse_released_click_to_zoom(target_pane);
        };
        mouse_released_pix_for_pix_event_handler = mouseEvent -> {
            if (mouseEvent.getButton() != MouseButton.SECONDARY) mouse_released_pix_for_pix(mouseEvent);
        };

        drag_and_drop(image_stage);

    }


    //**********************************************************
    private void pix_for_pix(Stage the_stage)
    //**********************************************************
    {

        the_stage.addEventHandler(MouseEvent.MOUSE_PRESSED, mouse_pressed_pix_for_pix_event_handler);
        the_stage.addEventHandler(MouseEvent.MOUSE_RELEASED, mouse_released_pix_for_pix_event_handler);
        the_stage.addEventHandler(MouseEvent.MOUSE_DRAGGED, mouse_dragged_pix_for_pix_event_handler);

    }

    //**********************************************************
    private void disable_pix_for_pix(Stage the_stage)
    //**********************************************************
    {
        the_stage.removeEventHandler(MouseEvent.MOUSE_PRESSED, mouse_pressed_pix_for_pix_event_handler);
        the_stage.removeEventHandler(MouseEvent.MOUSE_RELEASED, mouse_released_pix_for_pix_event_handler);
        the_stage.removeEventHandler(MouseEvent.MOUSE_DRAGGED, mouse_dragged_pix_for_pix_event_handler);
    }

    //**********************************************************
    void pix_for_pix()
    //**********************************************************
    {
        Image_display_handler tmp = image_window.image_display_handler;
        if ( tmp == null)
        {
            logger.log("pix_for_pix weird = Image_display_handler is null?");
            return;
        }
        if ( tmp.get_image_context().isEmpty()) return;
        Image_context local_image_context = tmp.get_image_context().get();
        local_image_context.the_image_view.fitWidthProperty().unbind();
        local_image_context.the_image_view.fitHeightProperty().unbind();
        local_image_context.the_image_view.setFitWidth(local_image_context.image.getWidth());
        local_image_context.the_image_view.setFitHeight(local_image_context.image.getHeight());
    }




    //**********************************************************
    private void click_to_zoom(Pane pane)
    //**********************************************************
    {
        pane.addEventHandler(MouseEvent.MOUSE_PRESSED, mouse_pressed_click_to_zoom_event_handler);
        pane.addEventHandler(MouseEvent.MOUSE_DRAGGED, mouse_dragged_click_to_zoom_event_handler);
        pane.addEventHandler(MouseEvent.MOUSE_RELEASED, mouse_released_click_to_zoom_event_handler);
    }

    //**********************************************************
    private void disable_click_to_zoom(Pane pane)
    //**********************************************************
    {
        Mouse_handling_for_Image_window.cropped_image_path = null;
        pane.removeEventHandler(MouseEvent.MOUSE_PRESSED, mouse_pressed_click_to_zoom_event_handler);
        pane.removeEventHandler(MouseEvent.MOUSE_DRAGGED, mouse_dragged_click_to_zoom_event_handler);
        pane.removeEventHandler(MouseEvent.MOUSE_RELEASED, mouse_released_click_to_zoom_event_handler);
    }

    //**********************************************************
    void set_mouse_mode(Image_window image_window, Mouse_mode new_mode)
    //**********************************************************
    {
        if ( image_window.image_display_handler.get_image_context().isEmpty()) return;
        Mouse_mode old_mode = mouse_mode;
        mouse_mode = new_mode;
        switch (mouse_mode) {
            case drag_and_drop -> {
                if (old_mode == Mouse_mode.drag_and_drop) return;
                if (old_mode == Mouse_mode.click_to_zoom) disable_click_to_zoom(image_window.the_image_Pane);
                if (old_mode == Mouse_mode.pix_for_pix) disable_pix_for_pix(image_window.stage);
                drag_and_drop(image_window);
                // REFRESH i.e. especially when going out of pix-for-pix
                if(!something_is_wrong_with_image_size())
                {
                    image_window.redisplay(false);//set_image(image_window.image_display_handler.get_image_context().get());
                }
            }
            case pix_for_pix -> {
                if (old_mode == Mouse_mode.pix_for_pix) {
                    // we need to re-aplly in case the image was changed
                    pix_for_pix();
                    return;
                }
                if (old_mode == Mouse_mode.click_to_zoom) disable_click_to_zoom(image_window.the_image_Pane);
                if (old_mode == Mouse_mode.drag_and_drop) disable_drag_and_drop(image_window.the_image_Pane);
                pix_for_pix(image_window.stage);
                pix_for_pix();
            }
            case click_to_zoom -> {
                if (old_mode == Mouse_mode.click_to_zoom) return;
                if (old_mode == Mouse_mode.drag_and_drop) disable_drag_and_drop(image_window.the_image_Pane);
                if (old_mode == Mouse_mode.pix_for_pix) disable_pix_for_pix(image_window.stage);
                click_to_zoom(image_window.the_image_Pane);
            }
        }

        image_window.set_stage_title(image_window.image_display_handler.get_image_context().get());
    }

    //**********************************************************
    boolean something_is_wrong_with_image_size()
    //****************************************y******************
    {
        if ( image_window.image_display_handler.get_image_context().isEmpty()) return true;
        Image_context local = image_window.image_display_handler.get_image_context().get();
        if (local == null)
        {
            logger.log("image_context is null");
            return true;
        }
        //logger.log("check size:" + local.image.getWidth() + "x" + local.image.getHeight());
        if ((local.image.getHeight() < 1) && (local.image.getWidth() < 1))
        {
            if (!local.image_is_damaged)
            {
                logger.log("bad image size");
            }
            logger.log("setting image view as broken icon");
            local.the_image_view.setImage(Jar_utils.get_broken_icon(300, image_window.stage, logger));
            return true;
        }
        return false;
    }

    //**********************************************************
    private void disable_drag_and_drop(Node border_pane)
    //**********************************************************
    {
        border_pane.setOnDragDetected(null);
        border_pane.setOnDragDone(null);
    }



    //**********************************************************
    private void drag_and_drop(Image_window image_window)
    //**********************************************************
    {

        if(image_window.image_display_handler.get_image_context().isEmpty())
        {
            logger.log(Stack_trace_getter.get_stack_trace("BADBABDBAD drag_and_drop: get_image_context is empty"));
            return;
        }
        {
            Image_context ic = image_window.image_display_handler.get_image_context().get();
            if ( ic != null) ic.the_image_view.setViewport(null);
        }
        image_window.the_image_Pane.setOnDragDetected(event -> {
            if (Drag_and_drop.drag_and_drop_dbg) logger.log("Image_stage: onDragDetected");

            Dragboard db = image_window.the_image_Pane.startDragAndDrop(TransferMode.MOVE);
            //GLUON Dragboard db = image_window.the_image_Pane.startDragAndDrop(TransferMode.COPY);

            ClipboardContent content = new ClipboardContent();
            List<File> possibly_moved = new ArrayList<>();

            if (Drag_and_drop.drag_and_drop_dbg) logger.log("going to drag and drop: "+ Objects.requireNonNull(image_window.image_display_handler.get_image_context().get().path).toFile().getAbsolutePath());
            Path p = image_window.image_display_handler.get_image_context().get().path;
            if ( p !=null)
            {
                possibly_moved.add(p.toFile());
                content.putFiles(possibly_moved);
                db.setContent(content);
            }
            event.consume();
        });

        image_window.the_image_Pane.setOnDragDone(drag_event -> {
            if (drag_event.getTransferMode() == TransferMode.MOVE)
            {
                if (Drag_and_drop.drag_and_drop_dbg) logger.log("Image_stage: onDragDone");
                //image is gone, replace it with the next one

                image_window.image_display_handler.change_image_relative(1,false);
                image_window.stage.requestFocus();
            }
            drag_event.consume();
        });


     // when an image it dropped we display it
    // and the side effect is that the current directory will change

        image_window.the_image_Pane.setOnDragDropped(drag_event -> {
            if (Drag_and_drop.drag_and_drop_dbg) logger.log("Image_stage/ic.imageView drag_and_drop DragDropped");

            Dragboard db = drag_event.getDragboard();
            List<File> l = db.getFiles();
            // take one
            for (File file : l)
            {
                if (Drag_and_drop.drag_and_drop_dbg) logger.log("  drag2 ACCEPTED for: " + file.getAbsolutePath());

                image_window.show_wait_cursor();
                Optional<Image_context> option = Image_context.build_Image_context(file.toPath(), image_window, image_window.aborter, logger);
                if ( option.isPresent())
                {
                    image_window.image_display_handler.set_image_context(option.get());
                    image_window.redisplay(true);
                }
                else {
                    logger.log(Stack_trace_getter.get_stack_trace("loading image failed for: "+file));
                }
                image_window.restore_cursor();
                break;
            }

            // tell the source
            drag_event.setDropCompleted(true);
            drag_event.consume();
        });

        image_window.the_image_Pane.setOnDragOver(drag_event -> {
            if (Drag_and_drop.drag_and_drop_dbg) logger.log("Image_stage/ic.imageView drag_and_drop DragOver");
            drag_event.acceptTransferModes(TransferMode.MOVE);
            drag_event.consume();
            image_window.stage.requestFocus();
        });
        image_window.the_image_Pane.setOnDragEntered(drag_event -> {
            if (Drag_and_drop.drag_and_drop_dbg) logger.log("Image_stage/ic.imageView drag_and_drop DragEntered");
            Look_and_feel_manager.set_drag_look_for_pane(image_window.the_image_Pane, image_window.stage, logger);
            drag_event.consume();
        });
        image_window.the_image_Pane.setOnDragExited(drag_event -> {
            if (Drag_and_drop.drag_and_drop_dbg) logger.log("Image_stage/ic.imageView drag_and_drop DragExited");
            if (image_window.image_display_handler.get_image_context().isPresent())
            {
                image_window.set_background(image_window.the_image_Pane,image_window.image_display_handler.get_image_context().get().path.getFileName().toString(), image_window.stage);
            }
            drag_event.consume();
        });
    }

}
