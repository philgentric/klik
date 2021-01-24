package klik.images;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import klik.I18N.I18n;
import klik.browser.Browser;
import klik.change.*;
import klik.find.Finder_in_a_thread;
import klik.look.Look_and_feel_manager;
import klik.properties.Properties;
import klik.util.*;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.*;


//**********************************************************
public class Image_stage implements After_move_handler, Slide_show_slave
//**********************************************************
{

    static boolean dbg = false;

    final Scene scene;
    final Stage the_stage;
    final BorderPane border_pane;

    private double old_mouse_x;
    private double old_mouse_y;
    public double W = 1200;
    public double H = 800;
    Logger logger;
    Image_file_source image_file_source = null;


    /*
    specific to displaying images
     */

    Slide_show slide_show;
    private Image_context image_context;
    boolean ultim_mode = false;
    Image_cache image_cache;

    //**********************************************************
    public static Image_stage get_Image_stage(
            Stage from_stage, // for on same screen
            Path path,
            boolean smaller,
            Logger logger_)
    //**********************************************************
    {
        Image_context local_ic = Image_context.get_Image_context(path, logger_);
        if (local_ic == null) {
            logger_.log(Stack_trace_getter.get_stack_trace("Image_stage PANIC: cannot load image " + path.toAbsolutePath()));
            return null;
        }

        return on_same_screen(from_stage, local_ic, smaller, logger_);
    }

    private static Image_stage on_same_screen(Stage from_stage, Image_context local_ic, boolean smaller, Logger logger_)
    {

        //List<Image_play> l = new ArrayList<>();
        //l.add(new Image_play(local_ic,logger_));
        if (from_stage == null) {
            return new Image_stage(local_ic, smaller, 800, 600, logger_);//, tpe_);
        }
        // make sure the image opens on the same window as the caller
        ObservableList<Screen> intersecting_screens = Screen.getScreensForRectangle(from_stage.getX(), from_stage.getY(), from_stage.getWidth(), from_stage.getHeight());

        if (dbg) {
            ObservableList<Screen> screens = Screen.getScreens();

            for (int i = 0; i < screens.size(); i++) {
                Screen s = screens.get(i);
                logger_.log("screen#" + i);
                logger_.log("    getBounds" + s.getBounds());
                logger_.log("    getVisualBounds" + s.getVisualBounds());
            }


            for (Screen s : intersecting_screens) {
                logger_.log("intersecting screen:" + s);
                logger_.log("    getBounds" + s.getBounds());
                logger_.log("    getVisualBounds" + s.getVisualBounds());
            }
        }
        // often there is only one ...
        Screen current = intersecting_screens.get(0);

        double x = current.getVisualBounds().getMinX();
        double y = current.getVisualBounds().getMinY();
        double w = current.getBounds().getWidth();
        double h = current.getBounds().getHeight();
        if (smaller) {
            w *= 0.5;
            h *= 0.5;
            x += 100;
            y += 100;

        }
        Image_stage returned = new Image_stage(local_ic, smaller, w, h, logger_);//, tpe_);

        returned.the_stage.setX(x);
        returned.the_stage.setY(y);
        return returned;
    }

    EventHandler<MouseEvent> mouse_pressed_click_to_zoom_event_handler;
    EventHandler<MouseEvent> mouse_dragged_click_to_zoom_event_handler;
    EventHandler<MouseEvent> mouse_released_click_to_zoom_event_handler;

    EventHandler<MouseEvent> mouse_pressed_pix_for_pix_event_handler;
    EventHandler<MouseEvent> mouse_dragged_pix_for_pix_event_handler;
    EventHandler<MouseEvent> mouse_released_pix_for_pix_event_handler;

    //**********************************************************
    private Image_stage(
            //List<Image_play> image_plays_,
            Image_context local_ic,
            boolean smaller,
            double w, double h,
            Logger logger_)
    //**********************************************************
    {
        image_context = local_ic;
        //image_plays = image_plays_;
        Change_gang.register(this); // ic must be valid!
        logger = logger_;
        image_cache = new Image_cache(logger);
        the_stage = new Stage();
        {
            Image image = Look_and_feel_manager.get_default_icon(300);
            if ( image != null) the_stage.getIcons().add(image);
        }
        the_stage.setWidth(w);
        the_stage.setHeight(h);
        //stage.initStyle(StageStyle.TRANSPARENT);
        border_pane = new BorderPane(); // makes it trivially easy to center the image!

        set_background(true);
        scene = new Scene(border_pane);
        the_stage.setScene(scene);
        the_stage.show();
        if (smaller == false) set_stage_size_to_fullscreen(the_stage);

        boolean white_background = check_image_size();
        set_ImageView(white_background);

        ChangeListener<Number> change_listener = new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                Rectangle2D b = new Rectangle2D(the_stage.getX(),the_stage.getY(),the_stage.getWidth(),the_stage.getHeight());
                Properties.save_bounds(b);
            }

        };
        the_stage.widthProperty().addListener(change_listener);
        the_stage.heightProperty().addListener(change_listener);


        Image_stage image_stage = this;
        the_stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            public void handle(WindowEvent we) {
                logger.log("Image_stage is closing");
                Change_gang.deregister(image_stage);
            }
        });

        scene.setOnScroll(new EventHandler<ScrollEvent>() {
            @Override
            public void handle(ScrollEvent event) {
                double dy = -event.getDeltaY();
                if (dy == 0) return;
                //logger.log("SCROLL dy=" + dy);
                int yy = (int) (dy / 10.0);
                if (yy == 0) {
                    if (dy < 0) yy = -1;
                    else yy = 1;
                }
                //logger.log("SCROLL after round up=" + yy);

                change_image_relative(yy, false);
            }

        });

        the_stage.addEventHandler(KeyEvent.KEY_PRESSED,
                new EventHandler<KeyEvent>() {
                    public void handle(final KeyEvent keyEvent) {
                        handle_keyboard(logger, the_stage, border_pane, keyEvent);
                    }
                });


        // event handler if window is hidden (or closed, I hope?): stop animation
        the_stage.setOnHiding(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {

                if (slide_show != null) {
                    slide_show.stop_the_show();
                    slide_show = null;
                }
                if (finder_for_k != null) finder_for_k.shutdown();
            }
        });


        EventHandler<MouseEvent> mouse_clicked_event_handler = new EventHandler<MouseEvent>() {
            public void handle(final MouseEvent mouseEvent) {
                if (mouseEvent.getButton() == MouseButton.SECONDARY) {
                    handle_mouse_clicked_secondary(logger, the_stage, border_pane, mouseEvent);
                }
            }
        };
        the_stage.addEventHandler(MouseEvent.MOUSE_CLICKED, mouse_clicked_event_handler);


        mouse_pressed_click_to_zoom_event_handler = new EventHandler<MouseEvent>() {
            public void handle(final MouseEvent mouseEvent) {

                if (mouseEvent.getButton() != MouseButton.SECONDARY) {
                    mouse_pressed_click_to_zoom(mouseEvent);
                }
            }
        };
        mouse_pressed_pix_for_pix_event_handler = new EventHandler<MouseEvent>() {
            public void handle(final MouseEvent mouseEvent) {

                if (mouseEvent.getButton() != MouseButton.SECONDARY) {
                    mouse_pressed_pix_for_pix(mouseEvent);
                }
            }
        };

        mouse_dragged_click_to_zoom_event_handler = new EventHandler<MouseEvent>() {
            public void handle(final MouseEvent mouseEvent) {
                if (mouseEvent.getButton() != MouseButton.SECONDARY) {
                    mouse_dragged_click_to_zoom(mouseEvent);
                }
            }
        };
        mouse_dragged_pix_for_pix_event_handler = new EventHandler<MouseEvent>() {
            public void handle(final MouseEvent mouseEvent) {
                if (mouseEvent.getButton() != MouseButton.SECONDARY) {
                    mouse_dragged_pix_for_pix(mouseEvent);
                }
            }
        };

        mouse_released_click_to_zoom_event_handler = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                if (mouseEvent.getButton() != MouseButton.SECONDARY) {
                    mouse_released_click_to_zoom(mouseEvent);
                }
            }
        };
        mouse_released_pix_for_pix_event_handler = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                if (mouseEvent.getButton() != MouseButton.SECONDARY) {
                    mouse_released_pix_for_pix(mouseEvent);
                }
            }
        };


        enable_drag_and_drop();
    }


    //**********************************************************
    void set_background(boolean white)
    //**********************************************************
    {
        if (white) {
            border_pane.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));

            return;
        }
        if ((image_context.path.getFileName().toString().endsWith(".png")) || (image_context.path.getFileName().toString().endsWith(".PNG"))) {
            border_pane.setBackground(new Background(new BackgroundFill(Color.GREY, CornerRadii.EMPTY, Insets.EMPTY)));
        } else if ((image_context.path.getFileName().toString().endsWith(".gif")) || (image_context.path.getFileName().toString().endsWith(".GIF"))) {
            border_pane.setBackground(new Background(new BackgroundFill(Color.DARKGRAY, CornerRadii.EMPTY, Insets.EMPTY)));
        } else {
            border_pane.setBackground(new Background(new BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY)));
        }

    }

    //**********************************************************
    private void handle_mouse_clicked_secondary(Logger logger, Stage stage, Pane pane, MouseEvent e)
    //**********************************************************
    {
        logger.log("handle_mouse_clicked_secondary");

        final ContextMenu contextMenu = new ContextMenu();
        contextMenu.setStyle("-fx-foreground-color: white;-fx-background-color: darkgrey;");

        ;

        MenuItem info = new MenuItem(I18n.get_I18n_string("Info_about",logger)
                + image_context.path.toAbsolutePath() + I18n.get_I18n_string("Info_about_file_shortcut",logger));
        contextMenu.getItems().add(info);
        info.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                show_exif_stage(logger, image_context);
            }
        });

        MenuItem edit = new MenuItem(I18n.get_I18n_string("Edit",logger));
        contextMenu.getItems().add(edit);
        edit.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                edit();
            }
        });
        MenuItem open = new MenuItem(I18n.get_I18n_string("Open",logger));
        contextMenu.getItems().add(open);
        open.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                open();
            }
        });


        MenuItem browse = new MenuItem(I18n.get_I18n_string("Browse",logger));
        contextMenu.getItems().add(browse);
        browse.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                logger.log("browse this!");
                Browser.create_browser(null, false, image_context.path.getParent(), false, logger);

            }
        });

        MenuItem rename = new MenuItem(I18n.get_I18n_string("Rename_with_shortcut",logger));
        contextMenu.getItems().add(rename);
        rename.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                ask_user_for_new_name();
            }
        });
        MenuItem copy = new MenuItem(I18n.get_I18n_string("Copy",logger));
        contextMenu.getItems().add(copy);
        copy.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                copy();
            }
        });


        MenuItem search_k = new MenuItem(I18n.get_I18n_string("Search_images_by_keywords_from_this_ones_name",logger));
        contextMenu.getItems().add(search_k);
        search_k.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                search_k();
            }
        });

        MenuItem search_y = new MenuItem(I18n.get_I18n_string("Choose_keywords",logger));
        contextMenu.getItems().add(search_y);
        search_k.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                find();
            }
        });

        MenuItem click_to_zoom = new MenuItem(I18n.get_I18n_string("Click_to_zoom",logger));
        contextMenu.getItems().add(click_to_zoom);
        click_to_zoom.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                set_mouse_mode(Mouse_mode.click_to_zoom);
            }
        });

        MenuItem drag_and_drop = new MenuItem(I18n.get_I18n_string("Drag_and_drop",logger));
        contextMenu.getItems().add(drag_and_drop);
        drag_and_drop.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                set_mouse_mode(Mouse_mode.drag_and_drop);
            }
        });

        MenuItem pix_for_pix = new MenuItem(I18n.get_I18n_string("Pix_for_pix",logger));
        contextMenu.getItems().add(pix_for_pix);
        pix_for_pix.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                set_mouse_mode(Mouse_mode.pix_for_pix);
            }
        });

        if ( Guess_file_type_from_extension.is_gif_extension(image_context.path))
        {
            //MenuItem repair1 = new MenuItem("REPAIR step 1: Extract frames in temporary folder");
            MenuItem repair1 = new MenuItem(I18n.get_I18n_string("Repair_animated_gif",logger));
            contextMenu.getItems().add(repair1);
            repair1.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent e) {
                    logger.log("repair1");
                    repair1();
                    Path local_path = repair2();
                    image_context = Image_context.get_Image_context(local_path, logger);
                    set_ImageView(false);
                }
            });
            /*
            MenuItem repair2 = new MenuItem("REPAIR step 2: generate repaired gif from frames in temporary folder");
            contextMenu.getItems().add(repair2);
            repair2.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent e) {
                    logger.log("repair2");
                    repair2();
                }
            });*/
        }
        MenuItem undo_move = new MenuItem(I18n.get_I18n_string("Undo_LAST_move_or_delete",logger));
        contextMenu.getItems().add(undo_move);
        undo_move.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                logger.log("undoing last move");
                Static_change_utilities.undo_last_move(logger);
            }
        });
        contextMenu.show(pane, e.getScreenX(), e.getScreenY());
    }

    //**********************************************************
    private void repair1()
    //**********************************************************
    {
        Path target = image_context.path;
        Path this_dir = target.getParent();
        Path new_dir = Path.of(this_dir.toString(),"tmp_"+target.getFileName().toString());
        try
        {
            Path tmp_dir = Files.createDirectory(new_dir);
            Tool_box.safe_move_a_file_or_dir(tmp_dir, logger, target.toFile());

            List<String> l = new ArrayList<>();
            // convert XXX -scene 1 +adjoin frame_%03d.gif
            l.add("convert");
            l.add(image_context.path.getFileName().toString());
            l.add("-scene");
            l.add("1");
            l.add("+adjoin");
            l.add("frame_%03d.gif");
            Execute_command.execute_command_list(l, tmp_dir.toFile(),2000,logger);
        }
        catch (IOException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
        }

    }

    private static boolean perform_rm = false;

    //**********************************************************
    private Path repair2()
    //**********************************************************
    {
        Path target = image_context.path;
        Path this_dir = target.getParent();
        Path tmp_dir = Path.of(this_dir.toString(),"tmp_"+target.getFileName().toString());

        if (perform_rm)
        {
            List<String> l = new ArrayList<>();
            // rm XXX
            l.add("rm ");
            l.add(image_context.path.getFileName().toString());
            Execute_command.execute_command_list(l, tmp_dir.toFile(),2000,logger);
        }
        {
            List<String> l = new ArrayList<>();
            // convert frame_0??.gif rebuilt.gif
            l.add("convert");
            l.add("frame_0??.gif");
            l.add("../"+image_context.path.getFileName().toString());
            Execute_command.execute_command_list(l, tmp_dir.toFile(),2000,logger);
        }
        if ( perform_rm)
        {
            List<String> l = new ArrayList<>();
            // rm frame_0*
            l.add("rm");
            l.add("frame_0*.gif");
            l.add(image_context.path.getFileName().toString());
            Execute_command.execute_command_list(l, tmp_dir.toFile(),2000,logger);
        }
        return target;
    }

    //**********************************************************
    private void show_wait_cursor()
    //**********************************************************
    {
        the_stage.getScene().getRoot().setCursor(Cursor.WAIT);
        //logger.log("cursor = wait");
    }

    //**********************************************************
    private void restore_cursor()
    //**********************************************************
    {
        the_stage.getScene().getRoot().setCursor(Cursor.DEFAULT);
        //logger.log("cursor = default");
    }


    Mouse_mode mouse_mode = Mouse_mode.drag_and_drop;

    //**********************************************************
    private void set_mouse_mode(Mouse_mode new_mode)
    //**********************************************************
    {
        Mouse_mode old_mode = mouse_mode;
        mouse_mode = new_mode;
        switch (mouse_mode) {
            case drag_and_drop:
                if (old_mode == Mouse_mode.drag_and_drop) return;
                if (old_mode == Mouse_mode.click_to_zoom) disable_click_to_zoom();
                if (old_mode == Mouse_mode.pix_for_pix) disable_pix_for_pix();
                enable_drag_and_drop();
                break;
            case pix_for_pix:
                if (old_mode == Mouse_mode.pix_for_pix) {
                    // we need to re-aplly in case the image was changed
                    pix_for_pix();
                    return;
                }
                if (old_mode == Mouse_mode.click_to_zoom) disable_click_to_zoom();
                if (old_mode == Mouse_mode.drag_and_drop) disable_drag_and_drop();
                enable_pix_for_pix();
                pix_for_pix();
                break;
            case click_to_zoom:
                if (old_mode == Mouse_mode.click_to_zoom) return;
                if (old_mode == Mouse_mode.drag_and_drop) disable_drag_and_drop();
                if (old_mode == Mouse_mode.pix_for_pix) disable_pix_for_pix();
                enable_click_to_zoom();
                break;
        }

        set_stage_title(image_context);
    }

    //**********************************************************
    private void enable_pix_for_pix()
    //**********************************************************
    {

        the_stage.addEventHandler(MouseEvent.MOUSE_PRESSED, mouse_pressed_pix_for_pix_event_handler);
        the_stage.addEventHandler(MouseEvent.MOUSE_RELEASED, mouse_released_pix_for_pix_event_handler);
        the_stage.addEventHandler(MouseEvent.MOUSE_DRAGGED, mouse_dragged_pix_for_pix_event_handler);

    }

    //**********************************************************
    private void disable_pix_for_pix()
    //**********************************************************
    {
        the_stage.removeEventHandler(MouseEvent.MOUSE_PRESSED, mouse_pressed_pix_for_pix_event_handler);
        the_stage.removeEventHandler(MouseEvent.MOUSE_RELEASED, mouse_released_pix_for_pix_event_handler);
        the_stage.removeEventHandler(MouseEvent.MOUSE_DRAGGED, mouse_dragged_pix_for_pix_event_handler);
    }

    //**********************************************************
    private void pix_for_pix()
    //**********************************************************
    {
        /*
        NO! because when there are damaged images
        this blocks the window size to 200x200
        double w = ic.image.getWidth();
        if (w < 200)
        {
            w = 200;
            the_stage.setWidth(w);
        }
        double h = ic.image.getHeight() + the_stage.getScene().getY();
        if (h < 200) {
            h = 200;
            the_stage.setHeight(h);
        }*/
        //logger.log("pix_for_pix image = " + w + " x " + h);
        image_context.imageView.fitWidthProperty().unbind();
        image_context.imageView.fitHeightProperty().unbind();
        image_context.imageView.setFitWidth(image_context.image.getWidth());
        image_context.imageView.setFitHeight(image_context.image.getHeight());
    }


    private boolean old_mouse_valid = false;

    //**********************************************************
    private void mouse_pressed_pix_for_pix(MouseEvent e)
    //**********************************************************
    {
        logger.log("mouse_pressed:");
        old_mouse_valid = true;
        old_mouse_x = e.getX();
        old_mouse_y = e.getY();
    }

    //**********************************************************
    private void mouse_released_pix_for_pix(MouseEvent e)
    //**********************************************************
    {
        logger.log("mouse_released_pix_for_pix:");
        old_mouse_valid = false;
    }

    //**********************************************************
    private void mouse_dragged_pix_for_pix(MouseEvent e)
    //**********************************************************
    {
        if (old_mouse_valid) {
            double dx = e.getX() - old_mouse_x;
            double dy = e.getY() - old_mouse_y;
            logger.log("mouse_dragged_pix_for_pix: dx,dy=" + dx + "," + dy);

            move_image_internal(dx, dy);
        }
        old_mouse_valid = true;
        old_mouse_x = e.getX();
        old_mouse_y = e.getY();
    }

    //**********************************************************
    private void move_image_internal(double dx, double dy)
    //**********************************************************
    {
        if (image_context == null) return;
        if (image_context.imageView == null) return;
        double x = image_context.imageView.getTranslateX();
        double image_pos_x = x + dx;
        image_context.imageView.setTranslateX(image_pos_x);

        double y = image_context.imageView.getTranslateY();
        double image_pos_y = y + dy;
        image_context.imageView.setTranslateY(image_pos_y);

    }

    private void enable_click_to_zoom() {
        // no ! stage.addEventHandler(MouseEvent.MOUSE_CLICKED, mouse_clicked_event_handler);
        the_stage.addEventHandler(MouseEvent.MOUSE_PRESSED, mouse_pressed_click_to_zoom_event_handler);
        the_stage.addEventHandler(MouseEvent.MOUSE_DRAGGED, mouse_dragged_click_to_zoom_event_handler);
        the_stage.addEventHandler(MouseEvent.MOUSE_RELEASED, mouse_released_click_to_zoom_event_handler);

    }

    private void disable_click_to_zoom() {
        //  no! stage.removeEventHandler(MouseEvent.MOUSE_CLICKED, mouse_clicked_event_handler);
        the_stage.removeEventHandler(MouseEvent.MOUSE_PRESSED, mouse_pressed_click_to_zoom_event_handler);
        the_stage.removeEventHandler(MouseEvent.MOUSE_DRAGGED, mouse_dragged_click_to_zoom_event_handler);
        the_stage.removeEventHandler(MouseEvent.MOUSE_RELEASED, mouse_released_click_to_zoom_event_handler);

    }

    private void disable_drag_and_drop() {
        border_pane.setOnDragDetected(null);
        border_pane.setOnDragDone(null);
    }


    Path next_to_display;

    private void enable_drag_and_drop() {
        image_context.imageView.setViewport(null);

        border_pane.setOnDragDetected(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent event) {
                logger.log("Image_stage: onDragDetected");

                Dragboard db = image_context.imageView.startDragAndDrop(TransferMode.MOVE);

                ClipboardContent content = new ClipboardContent();
                List<File> possibly_moved = new ArrayList<>();
                possibly_moved.add(image_context.path.toFile());
                if (image_file_source == null) {
                    image_file_source = Image_file_source.get_Image_file_source(image_context.path.getParent(), logger);
                }
                int current = get_current_image_index();
                Image_and_index xxx = image_file_source.get_Image_and_index(current + 1);
                if (xxx == null) next_to_display = null;
                else next_to_display = xxx.ic.path;

                content.putFiles(possibly_moved);
                db.setContent(content);
                event.consume();


            }
        });

        border_pane.setOnDragDone(new EventHandler<DragEvent>() {
            public void handle(DragEvent event) {
                if (event.getTransferMode() == TransferMode.MOVE) {
                    logger.log("Image_stage: onDragDone");
                    //image is gone, replace it with the next one

                    // very important: RELOAD the file source
                    // otherwise weird things happen
                    // since it was
                    if (image_file_source != null) {
                        image_file_source.scan();
                    } else {
                        // should not happen?
                        // there should always be an onDragDetected before?
                        image_file_source = Image_file_source.get_Image_file_source(image_context.path.getParent(), logger);
                    }

                    int new_index = 0;
                    if (next_to_display != null) {
                        new_index = image_file_source.get_index_of(next_to_display);
                    }
                    change_image_absolute(new_index);
                    the_stage.requestFocus();
                }
                event.consume();
            }
        });


        init_browser_to_image_stage_drag_and_drop();


    }

    // when an image it dropped we display it
    // and the side effect is that the current directory will change

    private void init_browser_to_image_stage_drag_and_drop() {

        border_pane.setOnDragDropped(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event) {
                logger.log("Image_stage/ic.imageView enable_drag_and_drop DragDropped");

                Dragboard db = event.getDragboard();
                List<File> l = db.getFiles();
                for (File fff : l) {
                    logger.log(" 2 drag ACCEPTED for: " + fff.getAbsolutePath());

                    show_wait_cursor();
                    image_context = Image_context.get_Image_context(fff.toPath(), logger);
                    image_file_source = null;
                    set_ImageView(false);
                    break;
                }

                // tell the source
                event.setDropCompleted(true);
                event.consume();
            }
        });

        border_pane.setOnDragOver(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event) {
                logger.log("Image_stage/ic.imageView enable_drag_and_drop DragOver");
                event.acceptTransferModes(TransferMode.MOVE);
                event.consume();
                the_stage.requestFocus();
            }
        });
        border_pane.setOnDragEntered(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event) {
                logger.log("Image_stage/ic.imageView enable_drag_and_drop DragEntered");
                border_pane.setBackground(new Background(new BackgroundFill(Color.PINK, CornerRadii.EMPTY, Insets.EMPTY)));
            }
        });
        border_pane.setOnDragExited(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event) {
                logger.log("Image_stage/ic.imageView enable_drag_and_drop DragExited");
                set_background(false);
            }
        });
    }

    /*

    //**********************************************************
    private void let_the_user_choose_a_move_target_dir(Logger logger, Stage stage, final ContextMenu contextMenu)
    //**********************************************************
    {
        MenuItem choose_folder = new MenuItem("Move to folder: Choose target folder");
        contextMenu.getItems().add(choose_folder);
        After_move_handler image_stage = this;
        choose_folder.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {

                logger.log("choose_folder for " + e.toString());

                List<Path> target_Paths_to_be_moved = new ArrayList<>();
                target_Paths_to_be_moved.add(ic.f);
                DirectoryChooser dc = new DirectoryChooser();
                dc.setInitialDirectory(new File(System.getProperty("user.home")));
                File destination_dir = dc.showDialog(stage);
                if (destination_dir == null) {
                    Alert alert = new Alert(AlertType.ERROR);
                    alert.setHeaderText("Could not open directory");
                    alert.setContentText("The Path is invalid.");

                    alert.showAndWait();
                } else {
                    logger.log("choose_folder: ACTION !" + destination_dir);
                    last_moved.clear();
                    List<Old_and_new_Path> to_be_moved = new ArrayList<Old_and_new_Path>();
                    for (Path target_Path_to_be_moved : target_Paths_to_be_moved) {
                        Path new_Path = (new File(destination_dir, target_Path_to_be_moved.getFileName().toString())).toPath();
                        to_be_moved.add(new Old_and_new_Path(target_Path_to_be_moved, new_Path, Command_old_and_new_Path.command_move, Status_old_and_new_Path.before_command));
                        last_moved.add(new Old_and_new_Path(target_Path_to_be_moved, new_Path, Command_old_and_new_Path.command_move, Status_old_and_new_Path.before_command));
                    }

                    logger.log("perform_the_move_in_a_javafx_Task3, last_move size1:" + to_be_moved.size());
                    Tool_box.perform_the_safe_moves_in_a_thread(to_be_moved, logger);
                    logger.log("perform_the_move_in_a_javafx_Task3, last_move size2:" + last_moved.size());

                    logger.log("trying to save movedir" + destination_dir.getAbsolutePath());
                    {
                        Tool_box.the_properties_manager.save_multiple(Constants.MOVE_DIR, destination_dir.getAbsolutePath());
                    }
                }
            }
        });
    }
    */


    //**********************************************************
    private void edit()
    //**********************************************************
    {
        Desktop d = Desktop.getDesktop();
        logger.log("asking desktop to EDIT: " + image_context.path.getFileName());
        try {
            d.edit(image_context.path.toFile());
        } catch (IOException e) {
            logger.log("edit error:" + e);
        }
    }

    //**********************************************************
    private void open()
    //**********************************************************
    {
        Desktop d = Desktop.getDesktop();
        logger.log("asking desktop to OPEN: " + image_context.path.getFileName());
        try {
            d.open(image_context.path.toFile());
        } catch (IOException e) {
            logger.log("open error:" + e);
        }
    }


    //**********************************************************
    private void set_stage_size_to_fullscreen(Stage stage)
    //**********************************************************
    {
        Screen screen = null;
        if (stage.isShowing()) {
            // we detect on which SCREEN the stage is (the user may have moved it)
            double minX = stage.getX();
            double minY = stage.getY();
            double width = stage.getWidth();
            double height = stage.getHeight();
            Rectangle2D r = new Rectangle2D(minX + 10, minY + 10, width - 100, height - 100);
            //logger.log("application rec"+r);
            ObservableList<Screen> screens = Screen.getScreensForRectangle(r);
            for (Screen s : screens) {
                //Rectangle2D bounds = s.getVisualBounds();
                //logger.log("screen in rec"+bounds);
                screen = s;
            }

        } else {
            // first time: we show the stage on the primary screen
            screen = Screen.getPrimary();
        }

        Rectangle2D bounds = Properties.get_bounds();

        if (bounds == null)
        {
            bounds = screen.getVisualBounds();
            Properties.save_bounds(bounds);
        }
        Scene scene = stage.getScene();
        //logger.log("scene getX" + scene.getX());
        //logger.log("scene getY" + scene.getY());
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());

        W = stage.getWidth();
        H = stage.getHeight() - scene.getY();


    }


    //**********************************************************
    private void handle_keyboard(Logger logger, Stage stage, Pane pane, final KeyEvent keyEvent)
    //**********************************************************
    {
        if (keyEvent.getCode() == KeyCode.ESCAPE) {
            stage.close();
            logger.log("Image_stage is closing (esc)");

            Change_gang.deregister(this);
            return;
        }

        logger.log("keyboard :" + keyEvent.toString());
        switch (keyEvent.getText()) {
            default:
                break;

            case "=":
                logger.log("= like pix-for-pix: use mouse to select visible part of large image");
                set_mouse_mode(Mouse_mode.pix_for_pix);
                break;

            case "b":
                logger.log("b like browse");
                Browser.create_browser(null, false, image_context.path.getParent(), false, logger);
                break;

            case "c":
                logger.log("c like copy");
                copy();
                break;

            case "d":
                logger.log("d like delete");
                delete();
                break;

            case "e":
                logger.log("e like edit");
                edit();
                break;

            case "f":
                logger.log("f like find");
                find();
                break;

            case "i":
                logger.log("i like information");
                show_exif_stage(logger, image_context);
                break;

            case "k":
                logger.log("k like keyword");
                search_k();
                break;

            case "m":
                logger.log("M like Move (enables drag-and-drop mode)");
                set_mouse_mode(Mouse_mode.drag_and_drop);
                break;

            case "o":
                logger.log("o like open ");
                open();
                break;

            case "r":
                logger.log("R like rename");
                ask_user_for_new_name();
                break;

            case "s":
                logger.log("S like slideshow");
                if (slide_show == null) {
                    slide_show = new Slide_show(this, ultim_mode, logger);
                } else {
                    slide_show.stop_the_show();
                    slide_show = null;
                    set_title();
                }
                break;

            case "u":
                logger.log("u like next ultim");
                get_next_u(image_context.path);
                break;

            case "U":
                logger.log("U like ultim MODE");
                if (ultim_mode) ultim_mode = false;
                else ultim_mode = true;
                break;

            case "v":
                logger.log("v like up Vote");
                ultim();
                break;

            case "w":
                logger.log("w => slow down slide show");
                if (slide_show != null) slide_show.slow_down();
                break;

            case "x":
                logger.log("x => speed up slide show");
                if (slide_show != null) slide_show.hurry_up();

                break;

            case "z":
                logger.log("Z like Zoom (enables click-to-zoom mode: use the mouse to select the zoomed area)");
                set_mouse_mode(Mouse_mode.click_to_zoom);
                break;

        }

        switch (keyEvent.getCode()) {
            case UP:
                logger.log("zoom up/in:");
                change_zoom_factor(1.3);
                break;

            case DOWN:
                logger.log("zoom down/out:");
                change_zoom_factor(0.7);
                break;
            case LEFT:
                logger.log("left");
                change_image_relative(-1, ultim_mode);
                break;


            case SPACE:
                logger.log("space");
            case RIGHT:
                logger.log("right");
                change_image_relative(1, ultim_mode);
                break;

            default:
                break;

        }
    }


    Finder_in_a_thread finder_for_k;

    //**********************************************************
    private void search_k()
    //**********************************************************
    {
        if (finder_for_k != null) {
            finder_for_k.update_display_in_FX_thread();
            return;
        }
        finder_for_k = new Finder_in_a_thread(image_context.path, the_stage, border_pane, logger);
        finder_for_k.find_image_files_from_keywords(null, logger);
    }

    //**********************************************************
    private void find()
    //**********************************************************
    {
        logger.log("find()");
        ask_user_and_find(image_context.path, the_stage, border_pane, given_keywords, logger);
    }

    Set<String> given_keywords = new TreeSet<>();

    //**********************************************************
    public static void ask_user_and_find(
            Path target,
            Stage the_stage,
            Pane pane,
            Set<String> given_keywords,
            Logger logger
    )
    //**********************************************************
    {
        logger.log("ask_user_and_find()");

        Runnable r = new Runnable() {
            @Override
            public void run() {
                String ttt = "";
                for (String ss : given_keywords) ttt += ss + " ";
                TextInputDialog dialog = new TextInputDialog(ttt);
                dialog.setTitle("Keywords");
                dialog.setHeaderText("Enter your keywords, separated by space");
                dialog.setContentText("Keywords:");

                logger.log("dialog !");
                Optional<String> result = dialog.showAndWait();
                if (result.isPresent()) {

                    String[] splited = result.get().split("\\s+");

                    given_keywords.clear();
                    for (String s : splited) given_keywords.add(s);

                    Finder_in_a_thread finder;
                    finder = new Finder_in_a_thread(target, the_stage, pane, logger);
                    finder.find_image_files_from_keywords(given_keywords, logger);
                }

            }
        };
        Platform.runLater(r);
    }


    //**********************************************************
    private void get_next_u(Path get_from)
    //**********************************************************
    {
        change_image_relative(1, true);
    }

    //**********************************************************
    public static void show_exif_stage(Logger logger, Image_context ic)
    //**********************************************************
    {
        TextFlow textFlow = new TextFlow();
        textFlow.setLayoutX(40);
        textFlow.setLayoutY(40);
        for (String s : ic.get_exif_metadata()) {
            logger.log("exif tag:" + s);
            Text t = new Text(s);
            textFlow.getChildren().add(t);
            textFlow.getChildren().add(new Text(System.lineSeparator()));
        }
        ScrollPane sp = new ScrollPane();
        sp.setPrefSize(1000, 600);
        sp.setContent(textFlow);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);

        Stage local_stage = new Stage();
        local_stage.setHeight(600);
        local_stage.setWidth(1000);

        Scene scene = new Scene(sp, 1000, 600, Color.WHITE);
        local_stage.setTitle(ic.path.toAbsolutePath().toString());
        local_stage.setScene(scene);
        local_stage.show();
    }


    //**********************************************************
    private void delete()
    //**********************************************************
    {
        int k = get_current_image_index();
        change_image_absolute(k + 1);
        Path f = image_context.path;
        Tool_box.safe_delete_one(f, logger);

        /*
        List<Old_and_new_Path> l = new ArrayList<>();
        l.add(new Old_and_new_Path(f, target, Command_old_and_new_Path.command_delete, Status_old_and_new_Path.deletion_done));
        Change_gang.report_event(l);
        */
    }

    //**********************************************************
    private void ask_user_for_new_name()
    //**********************************************************
    {
        TextInputDialog dialog = new TextInputDialog(image_context.path.getFileName().toString());

        {
            String text = I18n.get_I18n_string("Rename",logger);// to: " + parent.toAbsolutePath().toString();
            dialog.setTitle(text);
        }
        {
            String text = I18n.get_I18n_string("Rename_explained",logger);// to: " + parent.toAbsolutePath().toString();
            dialog.setHeaderText(text);

        }
        {
            String text = I18n.get_I18n_string("New_name",logger);// to: " + parent.toAbsolutePath().toString();
            dialog.setContentText(text);
        }
        // The Java 8 way to get the response value (with lambda expression).
        //result.ifPresent(name -> logger.log("Your name: " + name));
        // Traditional way to get the response value.
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String new_name = result.get();
            change_name_of_current_image(new_name);
        }
    }

    //**********************************************************
    private void copy()
    //**********************************************************
    {
        if ( Tool_box.popup_ask_for_confirmation(I18n.get_I18n_string("Warning",logger),
                I18n.get_I18n_string("Copy_are_you_sure",logger),logger) == false) return;

        Path new_path = null;
        for (int i = 0; i < 2056; i++) {
            new_path = Tool_box.generate_new_candidate_name(image_context.path, i, logger);
            if (Files.exists(new_path) == false) break;
        }
        if (new_path == null) {
            logger.log("copy failed: could not create new unused name for" + image_context.path.getFileName());
            return;
        }

        try {
            Files.copy(image_context.path, new_path);
        } catch (IOException e) {
            logger.log("copy failed: could not create new file for" + image_context.path.getFileName() + "Exception:" + e);
            return;
        }
        List<Old_and_new_Path> l = new ArrayList<>();
        l.add(new Old_and_new_Path(
                image_context.path,
                new_path,
                Command_old_and_new_Path.command_copy,
                Status_old_and_new_Path.copy_done));
        Change_gang.report_event(l);
    }

    //**********************************************************
    private void ultim()
    //**********************************************************
    {
        String Path_name = image_context.path.getFileName().toString();
        if (Path_name.contains(Constants.ULTIM)) {
            logger.log("no vote, name already contains " + Constants.ULTIM);
            return;
        }
        int last_index = Path_name.lastIndexOf('.');
        String extension = Path_name.substring(last_index, Path_name.length());

        String new_name = Path_name.substring(0, last_index);
        new_name += Constants.ULTIM;
        new_name += extension;
        logger.log("old name = " + Path_name);
        logger.log("new name = " + new_name);

        change_name_of_current_image(new_name);
    }

    //**********************************************************
    private void change_name_of_current_image(String new_name)
    //**********************************************************
    {
        logger.log("New name: " + new_name);
        if ( image_file_source == null)
        {
            image_file_source = Image_file_source.get_Image_file_source(image_context.path.getParent(), logger);
        }
        int i = image_context.get_index(image_file_source);
        Path new_path = Tool_box.safe_rename(logger, image_context.path, new_name);
        image_context = new Image_context(new_path, image_context.image, true, i,logger);

        set_stage_title(image_context);

    }

    //**********************************************************
    private void set_stage_title(Image_context ic)
    //**********************************************************
    {
        String local_title = "";
        if (ic.path != null) {
            local_title = ic.path.getFileName().toString();
        }
        if (ic.path.toFile().length() == 0) {
            local_title += " empty file";
        } else if (ic.image_is_damaged) {
            local_title += " damaged or invalid (wrong extension?) file";
        } else {
            local_title += " " + ic.image.getWidth();
            local_title += "x" + ic.image.getHeight();
        }
        if (slide_show != null) {
            local_title += "-- SLIDE-SHOW mode, delay=" + slide_show.inter_frame_ms + "(ms)";
        } else {
            switch (mouse_mode) {
                case drag_and_drop:
                    local_title += "-- drag-and-drop mode (use mouse to drag the image)";
                    break;
                case pix_for_pix:
                    local_title += "-- pix-for-pix mode (use mouse to explore large images)";
                    break;
                case click_to_zoom:
                    local_title += "-- zoom-with-mouse mode (use mouse to select zoom area)";
                    break;
            }
        }
        the_stage.setTitle(local_title);
        //logger.log("Image_stage title = " + local_title);
    }


    //**********************************************************
    @Override
    public void change_image_relative(int i, boolean ultimate)
    //**********************************************************
    {
        logger.log("(0)change_image_relative delta=" + i);
        show_wait_cursor();

        if (image_file_source == null) {
            if (image_context.path == null) {
                set_ImageView_null(null);
                return;
            }
            image_file_source = Image_file_source.get_Image_file_source(image_context.path.getParent(), logger);
        }

        // the safe way is to get the index of the current image from its path
        // however, when renaming a sequence of image this is annoying
        // since when you press next, you are in the new name context...
        int current_index = image_file_source.get_index_of(image_context.path);
        if ( image_context.previous_index >= 0)
        {
            current_index = image_context.previous_index;
        }
        int target = current_index + i;
        target = image_file_source.check_index(target, ultimate);
        logger.log("(1)change_image_relative absolute=" + current_index + "==>" + target);

        boolean forward = true;
        if (i < 0) forward = false;

        {
            String skey = Image_decode_request.get_key(image_file_source, target);
            Image_and_index iai = image_cache.get(skey);
            if (iai != null) {
                image_context = iai.ic;
                logger.log("\n FOUND in CACHE: " + skey);
                set_ImageView(false);

                image_cache.preload(iai.index, ultimate, forward, image_file_source);
                return;

            }
            logger.log("\n NOT found in cache: " + skey);
        }


        Image_and_index iai = image_file_source.get_Image_and_index(target);
        if (iai == null) {
            clear_image_cache("null image (1) in change_image_relative");
            Change_gang.report_anomaly(image_context.path.getParent());
            return;
        }
        if (iai.ic == null) {
            clear_image_cache("null image (2) in change_image_relative");
            return;
        }
        image_context = iai.ic;

        if (check_image_size()) return;
        logger.log("change_image_relative OK! index is:" + target + " for file:" + image_context.path.getFileName());

        set_ImageView(false);

        image_cache.preload(target, ultimate, forward, image_file_source);

    }

    private boolean check_image_size() {
        logger.log("check size:" + image_context.image.getWidth() + "x" + image_context.image.getHeight());
        if ((image_context.image.getHeight() < 1) && (image_context.image.getWidth() < 1)) {
            if (image_context.image_is_damaged == false) {
                clear_image_cache("bad image size");
            }
            //ic.imageView.setImage(Static_image_utilities.get_default_directory_icon(300,logger));
            image_context.imageView.setImage(Look_and_feel_manager.get_broken_icon(300));
            //logger.log("ic.imageView"+ic.imageView.getImage().toString());

            set_ImageView(true);
            return true;
        }
        return false;
    }


    //**********************************************************
    private void clear_image_cache(String error)
    //**********************************************************
    {
        logger.log("oops " + error + ", outOfMemory suspected, clearing image cache");
        image_cache.clear();
    }


    //**********************************************************
    private int get_current_image_index()
    //**********************************************************
    {
        if (image_context.path == null) return -1;
        if (image_file_source == null)
            image_file_source = Image_file_source.get_Image_file_source(image_context.path.getParent(), logger);
        int k = image_file_source.get_index_of(image_context.path);
        return k;
    }

    //**********************************************************
    protected void change_image_absolute(int new_index)//, boolean u)
    //**********************************************************
    {
        logger.log("change_image_absolute ");

        show_wait_cursor();

        Runnable r = new Runnable() {
            @Override
            public void run() {

                if (image_context.path == null) return;
                if (image_file_source == null)
                    image_file_source = Image_file_source.get_Image_file_source(image_context.path.getParent(), logger);

                int target = image_file_source.check_index(new_index, false);
                Image_and_index iai = image_file_source.get_Image_and_index(target);

                if (iai == null) {
                    set_ImageView_null(image_context.path.getParent());
                } else {
                    image_context = iai.ic;
                    logger.log("change_image_absolute index is:" + new_index + " for file:" + image_context.path.getFileName());
                    set_ImageView(false);
                }
            }
        };

        Tool_box.execute(r, logger);
    }

    //**********************************************************
    private void set_ImageView_null(Path dir)
    //**********************************************************
    {
        // no image to display...
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                border_pane.getChildren().clear();
                the_stage.setTitle("No image to display in: " + dir.toAbsolutePath().toString());
                restore_cursor();
            }
        });

    }

    /*
    //**********************************************************
    void check_stage_size()
    //**********************************************************
    {
        ObservableList<Screen> intersecting_screens = Screen.getScreensForRectangle(the_stage.getX(), the_stage.getY(), the_stage.getWidth(), the_stage.getHeight());
        Screen screen = intersecting_screens.get(0);
        Rectangle2D bounds = screen.getVisualBounds();

        if ((the_stage.getWidth() > bounds.getWidth()) || (the_stage.getHeight() > bounds.getHeight())) {
            the_stage.setX(bounds.getMinX());
            the_stage.setY(bounds.getMinY());
            the_stage.setWidth(bounds.getWidth());
            the_stage.setHeight(bounds.getHeight());

        }

    }
    */


    //**********************************************************
    private void set_ImageView(boolean white_background)
    //**********************************************************
    {
        // if pix-for-pix was used on a very large image, the window size is very large too..
        // let us check and correct that

        //check_stage_size();

        //W = the_stage.getWidth();
        //H = the_stage.getHeight() - the_stage.getScene().getY();

        //ic.imageView.setFitWidth(W);
        //ic.imageView.setFitHeight(H);

        image_context.imageView.setPreserveRatio(true);
        //logger.log("smooth?"+image_context.imageView.isSmooth());
        image_context.imageView.setSmooth(true);
        //ic.imageView.setCache(true);
        image_context.imageView.setRotate(image_context.get_rotation());

        // if ( ic.image_is_damaged == false)
        {
            //if ((ic.image.getWidth() > 200) && (ic.image.getHeight() > 200))
            {
                image_context.imageView.fitWidthProperty().bind(scene.widthProperty());
                image_context.imageView.fitHeightProperty().bind(scene.heightProperty());
            }
        }

        set_background(white_background);

        Platform.runLater(new Runnable() {
            public void run() {
                border_pane.getChildren().clear();
                border_pane.setCenter(image_context.imageView);
                //logger.log("ic.imageView"+ image_context.imageView.getImage().toString());
                set_stage_title(image_context);
                if (mouse_mode == Mouse_mode.pix_for_pix) pix_for_pix();
                //restore_cursor();
            }
        });
    }


    //**********************************************************
    private void move_image(double dx, double dy)
    //**********************************************************
    {
        if (image_context.imageView == null) return;
        double x = image_context.imageView.getX();
        double image_pos_x = x + dx;
        image_context.imageView.setX(image_pos_x);

        double y = image_context.imageView.getY();
        double image_pos_y = y + dy;
        image_context.imageView.setY(image_pos_y);

    }

    //**********************************************************
    private void change_zoom_factor(double mul)
    //**********************************************************
    {
        logger.log("old W=" + W + ", H=" + H);
        double h = image_context.imageView.getFitHeight();
        H = h * mul;
        //double dy = (h-H)/2.0;
        double dy = (h - H);
        double w = image_context.imageView.getFitWidth();
        W = w * mul;
        logger.log("new W=" + W + ", H=" + H);
        //double dx = (w-W)/2.0;
        double dx = (w - W);

        set_ImageView(false);
        //move_image(dx, dy);
    }

    Rectangle user_defined_zoom_area = null;

    //**********************************************************
    private void mouse_pressed_click_to_zoom(MouseEvent e)
    //**********************************************************
    {
        logger.log("mouse_pressed_local_zoom:");
        old_mouse_x = e.getX();
        old_mouse_y = e.getY();

        user_defined_zoom_area = new Rectangle(e.getX(), e.getY(), 5, 5);
        user_defined_zoom_area.setFill(Color.TRANSPARENT);
        user_defined_zoom_area.setVisible(true);
        Color c = new javafx.scene.paint.Color(1, 0, 0, 0.2);
        user_defined_zoom_area.setStroke(c);
        user_defined_zoom_area.setStrokeWidth(10.0f);
        border_pane.getChildren().add(user_defined_zoom_area);
        user_defined_zoom_area.toFront();
    }

    //**********************************************************
    private void mouse_released_click_to_zoom(MouseEvent e)
    //**********************************************************
    {
        logger.log("mouse_released_local_zoom:");
        border_pane.getChildren().remove(user_defined_zoom_area);
        if (user_is_selecting_zoom_area == false) return;
        user_is_selecting_zoom_area = false;

        if (user_defined_zoom_area.getWidth() < 5) return;
        if (user_defined_zoom_area.getHeight() < 5) return;

        if (image_context.imageView.getViewport() != null) {
            logger.log("sorry, only one zoom supported at this time");
            image_context.imageView.setViewport(null);
            return;
        }

        // need to correct the rectangle inside the picture

        logger.log("image :" + image_context.image.getWidth() + "x" + image_context.image.getHeight());

        Bounds bounds = image_context.imageView.getLayoutBounds();
        logger.log("image View bounds x/y:" + bounds.getMinX() + "/" + bounds.getMinY() + "w/h :" + bounds.getWidth() + "x" + bounds.getHeight());
        logger.log("rectangle1 :" + user_defined_zoom_area.getX() + "/" + user_defined_zoom_area.getY() + " " + user_defined_zoom_area.getWidth() + "x" + user_defined_zoom_area.getHeight());

        double scale = image_context.image.getWidth() / bounds.getWidth();

        Rectangle2D view_port = new Rectangle2D(
                (user_defined_zoom_area.getX() - image_context.imageView.getLayoutX()) * scale,
                (user_defined_zoom_area.getY() - image_context.imageView.getLayoutY()) * scale,
                user_defined_zoom_area.getWidth() * scale,
                user_defined_zoom_area.getHeight() * scale
        );
        logger.log("rectangle2 :" + view_port.getMinX() + "/" + view_port.getMinY() + " " + view_port.getWidth() + "x" + view_port.getHeight());

        image_context.imageView.setViewport(view_port);
        //Image_stage is = Image_stage.get_Image_stage2(this,ic,rect,the_properties_manager,tpe,logger);
    }

    /*
    private static Image_stage get_Image_stage2(Image_stage from_stage, Image_context ic, Rectangle2D rectangle,  Properties_manager the_properties_manager_, ExecutorService tpe__)
    {
        // generate a new PIXEL image

        double x_offset_in_source = 0;//rectangle.getMinX();
        double y_offset_in_source = 0;//rectangle.getMinY();
        double w = rectangle.getWidth();
        double h = rectangle.getHeight();
        WritableImage destination = new WritableImage( (int)w, (int)h );

        PixelWriter pixel_writer = destination.getPixelWriter();

        PixelReader pixel_reader =  ic.image.getPixelReader();

        for(int x = 0; x <  w; x++ )
        {
            for( int y = 0 ; y < h; y++ )
            {
                int source_x = (int)x_offset_in_source+x;
                int source_y = (int)y_offset_in_source+y;
                logger_.log("x_offset_in_source+x="+source_x+" y_offset_in_source+y:"+source_y);
                Color color = pixel_reader.getColor( source_x,source_y);
                pixel_writer.setColor(x , y, color);
            }
        }

        Image_context local_ic =  Image_context.from_Image(destination,logger_);

        return on_same_screen(from_stage.stage, local_ic, the_properties_manager_,  tpe_, logger_);

    }*/

    private boolean user_is_selecting_zoom_area = false;

    //**********************************************************
    private void mouse_dragged_click_to_zoom(MouseEvent e)
    //**********************************************************
    {
        logger.log("mouse_dragged_local_zoom:");

        if (user_defined_zoom_area != null) {
            user_is_selecting_zoom_area = true;
            double dx = e.getX() - old_mouse_x;
            double dy = e.getY() - old_mouse_y;
            logger.log("MouseDragged: dx,dy=" + dx + "," + dy);
            double w = user_defined_zoom_area.getWidth() + dx;
            double h = user_defined_zoom_area.getHeight() + dy;
            user_defined_zoom_area.setWidth(w);
            user_defined_zoom_area.setHeight(h);

        }
        old_mouse_x = e.getX();
        old_mouse_y = e.getY();
    }


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
        logger.log("mouse_dragged_move_mode: drag detected");


        Dragboard db = image_context.imageView.startDragAndDrop(TransferMode.MOVE);

        ClipboardContent content = new ClipboardContent();
        List<File> l = new ArrayList<>();
        l.add(image_context.path.toFile());
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


    //**********************************************************
    @Override
    public void you_receive_this_because_a_move_occurred_somewhere(List<Old_and_new_Path> l, Logger logger2)
    //**********************************************************
    {

        if (Change_gang.is_my_directory_impacted(image_context.path.getParent(), l, logger2) == false) return;
        image_cache.clear();
        logger2.log("Image_stage::you_receive_this_because_a_move_occurred_somewhere");
        boolean found = false;
        for (Old_and_new_Path oanf : l)
        {
            logger2.log("Image_stage, getting a you_receive_this_because_a_move_occurred_somewhere " + oanf.get_string());
            if (image_context == null) {
                logger2.log("Image_stage, ic == null");
                continue;
            }
            if (image_context.path == null) {
                logger2.log("Image_stage, ic.f == null");
                continue;
            }
            String current_Path_path = image_context.path.toAbsolutePath().toString();
            if (oanf.get_old_Path().toAbsolutePath().toString().equals(current_Path_path)) {
                // the case when the image has been dragged away is handled directly
                // by the setOnDragDone event handler

                // the case we care for HERE is when another type of event occurred
                // for example the image was renamed
                if (image_file_source == null)
                    image_file_source = Image_file_source.get_Image_file_source(image_context.path.getParent(), logger);
                Image_and_index im = image_file_source.get_image_for_path(oanf.new_Path);
                if (im == null) {
                    // the image was moved out of the current directory
                    logger.log("image moved out:" + oanf.get_string());
                }
                else {
                    logger.log("image renamed, same dir:" + oanf.get_string());

                    //image_context = im.ic;
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            set_ImageView(false);
                        }
                    });
                }

            }
        }


    }


    @Override
    public String get_string() {
        if (image_context == null) return Stack_trace_getter.get_stack_trace("Image_stage NO CONTEXT????");
        else return "Image_stage " + image_context.path.toAbsolutePath();
    }


    @Override
    public void set_title() {
        this.set_stage_title(image_context);
    }
}
