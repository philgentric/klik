package klik.images;

import javafx.application.Platform;
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
import klik.change.After_move_handler;
import klik.change.Change_gang;
import klik.change.Old_and_new_Path;
import klik.find.Finder_in_a_thread;
import klik.browser.Browser;
import klik.look.Look_and_feel_manager;
import klik.util.*;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.*;


//**********************************************************
public class Multiple_image_stage implements After_move_handler, Slide_show_slave
//**********************************************************
{

    static boolean dbg = true;

    final Scene scene;
    final Stage the_stage;
    final TilePane tile_pane;

    private double old_mouse_x;
    private double old_mouse_y;
    public double W = 1200;
    public double H = 800;
    Logger logger;

    //List<Image_play> image_plays = new ArrayList<>();
    Image_file_source image_file_source = null;
    private Image_context ic;


    //List<Old_and_new_Path> last_moves = new ArrayList<>();
    Slide_show slide_show;


    Image_cache image_cache;

    //**********************************************************
    public static Multiple_image_stage get_Multiple_image_stage(
            Stage from_stage, // for on same screen
            Path path,
            boolean smaller,
            Logger logger_)
    //**********************************************************
    {
        Image_context local_ic = Image_context.get_Image_context(path, logger_);
        if (local_ic == null) {
            logger_.log(Stack_trace_getter.get_stack_trace("Multiple_image_stage PANIC: cannot load image " + path.toAbsolutePath()));
            return null;
        }

        logger_.log("Multiple_image_stage OK: image loaded" + path.toAbsolutePath());

        return on_same_screen(from_stage, local_ic, smaller, logger_);
    }

    private static Multiple_image_stage on_same_screen(
            Stage from_stage, Image_context local_ic, boolean smaller, Logger logger_) {

        //List<Image_play> l = new ArrayList<>();
        //l.add(new Image_play(local_ic,logger_));
        if (from_stage == null) {
            return new Multiple_image_stage(local_ic, smaller, 800, 600, logger_);//, tpe_);
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
        Multiple_image_stage returned = new Multiple_image_stage(local_ic, smaller, w, h, logger_);//, tpe_);

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
    private Multiple_image_stage(
            //List<Image_play> image_plays_,
            Image_context local_ic,
            boolean smaller,
            double w, double h,
            Logger logger_)
    //**********************************************************
    {
        ic = local_ic;
        if (ic == null) {
            logger.log(Stack_trace_getter.get_stack_trace("what ??????"));
        }
        //image_plays = image_plays_;
        Change_gang.register(this); // ic must be valid!
        logger = logger_;
        logger.log("Multiple_image_stage !!!");
        image_cache = new Image_cache(logger);
        the_stage = new Stage();
        {
            Image image = Look_and_feel_manager.get_default_icon(300);
            if (image != null) the_stage.getIcons().add(image);
        }
        the_stage.setWidth(w);
        the_stage.setHeight(h);
        //stage.initStyle(StageStyle.TRANSPARENT);


        tile_pane = new TilePane();
        //tile_pane.setPadding(new Insets(5, 0, 5, 0));
        //tile_pane.setVgap(4);
        //tile_pane.setHgap(4);
        //tile_pane.setPrefColumns(2);
        //tile_pane.setPrefRows(2);
        //tile_pane.setStyle("-fx-background-color: DAE6F3;");

        set_background();
        scene = new Scene(tile_pane);
        the_stage.setScene(scene);
        the_stage.show();
        if (smaller == false) set_stage_size_to_fullscreen(the_stage);

        set_ImageView();

        Multiple_image_stage image_stage = this;
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
                        handle_keyboard(logger, the_stage, tile_pane, keyEvent);
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
                if (finder != null) finder.shutdown();
            }
        });


        EventHandler<MouseEvent> mouse_clicked_event_handler = new EventHandler<MouseEvent>() {
            public void handle(final MouseEvent mouseEvent) {
                if (mouseEvent.getButton() == MouseButton.SECONDARY) {
                    handle_mouse_clicked_secondary(logger, the_stage, tile_pane, mouseEvent);
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


    void set_background() {
        if ((ic.path.getFileName().toString().endsWith(".png")) || (ic.path.getFileName().toString().endsWith(".PNG"))) {
            tile_pane.setBackground(new Background(new BackgroundFill(Color.GREY, CornerRadii.EMPTY, Insets.EMPTY)));
        } else if ((ic.path.getFileName().toString().endsWith(".gif")) || (ic.path.getFileName().toString().endsWith(".GIF"))) {
            tile_pane.setBackground(new Background(new BackgroundFill(Color.DARKGRAY, CornerRadii.EMPTY, Insets.EMPTY)));
        } else {
            tile_pane.setBackground(new Background(new BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY)));
        }

    }

    //**********************************************************
    private void handle_mouse_clicked_secondary(Logger logger, Stage stage, Pane pane, MouseEvent e)
    //**********************************************************
    {
        logger.log("handle_mouse_clicked_secondary");

        final ContextMenu contextMenu = new ContextMenu();
        contextMenu.setStyle("-fx-foreground-color: white;-fx-background-color: darkgrey;");
        MenuItem info = new MenuItem("Path=" + ic.path.toAbsolutePath());
        contextMenu.getItems().add(info);
        info.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                show_exif_stage(logger, ic);
            }
        });
        MenuItem edit = new MenuItem("Edit (open file in system-defined Editor for this file type)");
        contextMenu.getItems().add(edit);
        edit.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                edit();
            }
        });
        MenuItem open = new MenuItem("Open (open file in system-defined reader for this file type)");
        contextMenu.getItems().add(open);
        open.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                open();
            }
        });


        MenuItem browse = new MenuItem("Browse the dir this image is in, in a new browsing window");
        contextMenu.getItems().add(browse);
        browse.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                logger.log("browse this!");
                Browser.create_browser(null, false, ic.path.getParent(), false, logger);

            }
        });

        MenuItem rename = new MenuItem("Rename (r)");
        contextMenu.getItems().add(rename);
        rename.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                ask_user_for_new_name2();
            }
        });

        MenuItem search_k = new MenuItem("Search images with same keywords (k)");
        contextMenu.getItems().add(search_k);
        search_k.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                search_k();
            }
        });

        MenuItem search_y = new MenuItem("Choose keywords and search (f)");
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

        /*
        MenuItem undo_move = new MenuItem("UNDO last Move");
        contextMenu.getItems().add(undo_move);
        undo_move.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                logger.log("undoing last move");
                if (last_moves.isEmpty()) {
                    logger.log("undoing last move: FAILED1, no move to undo");
                    return;
                }

                List<Old_and_new_Path> reverse_last_move = new ArrayList<>();
                //for (Old_and_new_Path oanf : last_moved)
                {
                    reverse_last_move.add(last_moves.get(last_moves.size() - 1).reverse());
                }
                if (reverse_last_move.isEmpty()) {
                    logger.log("undoing last move: FAILED2, no move to undo");
                    return;
                }

                logger.log("perform_the_move_in_a_javafx_Task4");
                Tool_box.perform_the_safe_moves_in_a_thread(reverse_last_move, logger);
            }
        });
        */
        //let_the_user_choose_a_move_target_dir(logger, stage, contextMenu);
        //Tool_box.fx_mover(contextMenu, logger, ic.f, last_moved);
        contextMenu.show(pane, e.getScreenX(), e.getScreenY());
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

        set_stage_title(ic);
    }

    private void enable_pix_for_pix() {

        the_stage.addEventHandler(MouseEvent.MOUSE_PRESSED, mouse_pressed_pix_for_pix_event_handler);
        the_stage.addEventHandler(MouseEvent.MOUSE_RELEASED, mouse_released_pix_for_pix_event_handler);
        the_stage.addEventHandler(MouseEvent.MOUSE_DRAGGED, mouse_dragged_pix_for_pix_event_handler);

    }

    private void disable_pix_for_pix() {
        the_stage.removeEventHandler(MouseEvent.MOUSE_PRESSED, mouse_pressed_pix_for_pix_event_handler);
        the_stage.removeEventHandler(MouseEvent.MOUSE_RELEASED, mouse_released_pix_for_pix_event_handler);
        the_stage.removeEventHandler(MouseEvent.MOUSE_DRAGGED, mouse_dragged_pix_for_pix_event_handler);
    }

    private void pix_for_pix() {
        double w = ic.image.getWidth();
        if (w < 200) {
            w = 200;
            the_stage.setWidth(w);
        }
        double h = ic.image.getHeight() + the_stage.getScene().getY();
        if (h < 200) {
            h = 200;
            the_stage.setHeight(h);
        }
        //logger.log("pix_for_pix image = " + w + " x " + h);
        ic.imageView.fitWidthProperty().unbind();
        ic.imageView.fitHeightProperty().unbind();
        ic.imageView.setFitWidth(ic.image.getWidth());
        ic.imageView.setFitHeight(ic.image.getHeight());
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
        if (ic == null) return;
        if (ic.imageView == null) return;
        double x = ic.imageView.getTranslateX();
        double image_pos_x = x + dx;
        ic.imageView.setTranslateX(image_pos_x);

        double y = ic.imageView.getTranslateY();
        double image_pos_y = y + dy;
        ic.imageView.setTranslateY(image_pos_y);

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
        tile_pane.setOnDragDetected(null);
        tile_pane.setOnDragDone(null);
    }

    private void enable_drag_and_drop() {
        ic.imageView.setViewport(null);

        tile_pane.setOnDragDetected(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent event) {
                logger.log("Image_stage enable_drag_and_drop drag detected");

                Dragboard db = ic.imageView.startDragAndDrop(TransferMode.MOVE);

                ClipboardContent content = new ClipboardContent();
                List<File> l = new ArrayList<>();
                l.add(ic.path.toFile());
                //current_image_index_for_drag_and_drop = get_current_image_index();

                content.putFiles(l);
                db.setContent(content);
                event.consume();


            }
        });

        tile_pane.setOnDragDone(new EventHandler<DragEvent>() {
            public void handle(DragEvent event) {
                if (event.getTransferMode() == TransferMode.MOVE) {
                    logger.log("Image_stage enable_drag_and_drop DragDone");
                    //image is gone, replace it with the next one

                    change_image_absolute(get_current_image_index() + 1);
                    the_stage.requestFocus();
                }
                event.consume();
            }
        });


        init_browser_to_image_stage_drag_and_drop();


    }


    private void init_browser_to_image_stage_drag_and_drop() {

        tile_pane.setOnDragDropped(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event) {
                logger.log("Image_stage/ic.imageView enable_drag_and_drop DragDropped");

                Dragboard db = event.getDragboard();
                List<File> l = db.getFiles();
                for (File fff : l) {
                    logger.log(" 3 drag ACCEPTED for: " + fff.getAbsolutePath());

                    show_wait_cursor();
                    ic = Image_context.get_Image_context(fff.toPath(), logger);
                    image_file_source = null;
                    set_ImageView();
                    break;
                }

                // tell the source
                event.setDropCompleted(true);
                event.consume();
            }
        });

        tile_pane.setOnDragOver(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event) {
                logger.log("Image_stage/ic.imageView enable_drag_and_drop DragOver");
                event.acceptTransferModes(TransferMode.MOVE);
                event.consume();
                the_stage.requestFocus();
            }
        });
        tile_pane.setOnDragEntered(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event) {
                logger.log("Image_stage/ic.imageView enable_drag_and_drop DragEntered");
                tile_pane.setBackground(new Background(new BackgroundFill(Color.PINK, CornerRadii.EMPTY, Insets.EMPTY)));
            }
        });
        tile_pane.setOnDragExited(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event) {
                logger.log("Image_stage/ic.imageView enable_drag_and_drop DragExited");
                set_background();
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
        logger.log("asking desktop to EDIT: " + ic.path.getFileName());
        try {
            d.edit(ic.path.toFile());
        } catch (IOException e) {
            logger.log("edit error:" + e);
        }
    }

    //**********************************************************
    private void open()
    //**********************************************************
    {
        Desktop d = Desktop.getDesktop();
        logger.log("asking desktop to OPEN: " + ic.path.getFileName());
        try {
            d.open(ic.path.toFile());
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

        //Screen screen = Screen.getPrimary();
        Rectangle2D bounds = screen.getVisualBounds();

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
            return;
        }

        if ( dbg) logger.log("keyboard :" + keyEvent.toString());
        switch (keyEvent.getText()) {
            default:
                break;

            case "=":
                logger.log("= like pix-for-pix: use mouse to select visible part of large image");
                set_mouse_mode(Mouse_mode.pix_for_pix);
                break;

            case "d":
                logger.log("D like delete");
                delete();
                break;

            case "e":
                logger.log("e like edit");
                edit();
                break;

            case "f":
                logger.log("F like find");
                find();
                break;

            case "i":
                logger.log("I like information");
                show_exif_stage(logger, ic);
                break;

            case "k":
                logger.log("K like keyword");
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
                ask_user_for_new_name2();
                break;

            case "s":
                logger.log("S like slideshow");
                if (slide_show == null) {
                    slide_show = new Slide_show(this, false, logger);
                } else {
                    slide_show.stop_the_show();
                    slide_show = null;
                    set_title();
                }
                break;

            case "u":
                logger.log("U like next ultim");
                get_next_u(ic.path);
                break;

            case "v":
                logger.log("V like up Vote");
                ultim(ic);
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
                change_image_relative(-1, false);
                break;


            case SPACE:
                logger.log("space");
            case RIGHT:
                logger.log("right");
                change_image_relative(1, false);
                break;

            default:
                break;

        }
    }


    Finder_in_a_thread finder;

    //**********************************************************
    private void search_k()
    //**********************************************************
    {
        if (finder != null) {
            finder.update_display_in_FX_thread();
            return;
        }
        finder = new Finder_in_a_thread(ic.path, the_stage, tile_pane, logger);
        finder.find_image_files_from_keywords(null, logger);
    }

    //**********************************************************
    private void find()
    //**********************************************************
    {
        logger.log("find()");
        ask_user_and_find(ic.path, the_stage, tile_pane, logger);
    }

    Set<String> given_keywords = new TreeSet<>();

    //**********************************************************
    public void ask_user_and_find(
            Path target,
            Stage the_stage,
            Pane pane,
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

                    for (String s : splited) given_keywords.add(s);

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
        Path f = ic.path;
        Tool_box.safe_delete_one(f, logger);

        /*
        List<Old_and_new_Path> l = new ArrayList<>();
        l.add(new Old_and_new_Path(f, target, Command_old_and_new_Path.command_delete, Status_old_and_new_Path.deletion_done));
        Change_gang.report_event(l);
        */
    }

    //**********************************************************
    private void ask_user_for_new_name2()
    //**********************************************************
    {
        TextInputDialog dialog = new TextInputDialog(ic.path.getFileName().toString());
        dialog.setTitle("Rename");
        dialog.setHeaderText("To rename this image, enter the new name:");
        dialog.setContentText("New name:");

        // The Java 8 way to get the response value (with lambda expression).
        //result.ifPresent(name -> logger.log("Your name: " + name));
        // Traditional way to get the response value.
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String new_name = result.get();
            change_name(ic, new_name);
        }
    }

    //**********************************************************
    private void ultim(Image_context ic)
    //**********************************************************
    {
        String Path_name = ic.path.getFileName().toString();
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

        change_name(ic, new_name);
    }

    //**********************************************************
    private void change_name(Image_context ic, String new_name)
    //**********************************************************
    {
        logger.log("New name: " + new_name);
        int i = ic.get_index(image_file_source);
        Path new_path = Tool_box.safe_rename(logger, ic.path, new_name);
        ic = new Image_context(new_path, ic.image, true, i,logger);


    }

    //**********************************************************
    private void set_stage_title(Image_context ic)
    //**********************************************************
    {
        String local_title = "";
        if (ic.path != null) {
            local_title = ic.path.getFileName().toString();
        }
        local_title += " " + ic.image.getWidth();
        local_title += "x" + ic.image.getHeight();

        if (slide_show != null) {
            local_title += "-- SLIDE-SHOW mode, delay=" + slide_show.inter_frame_ms + "(ms)";
        } else {
            switch (mouse_mode) {
                case drag_and_drop:
                    local_title += "-- drag-and-drop mode";
                    break;
                case pix_for_pix:
                    local_title += "-- pix-for-pix mode";
                    break;
                case click_to_zoom:
                    local_title += "-- zoom-with-mouse mode";
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
            if (ic.path == null) {
                set_ImageView_null(null);
                return;
            }
            image_file_source = Image_file_source.get_Image_file_source(ic.path.getParent(), logger);
        }

        int current_index = image_file_source.get_index_of(ic.path);
        int target = current_index + i;
        target = image_file_source.check_index(target, ultimate);
        logger.log("(1)change_image_relative absolute=" + current_index + "==>" + target);

        boolean forward = true;
        if (i < 0) forward = false;

        {
            String skey = Image_decode_request.get_key(image_file_source, target);
            Image_and_index iai = image_cache.get(skey);
            if (iai != null) {
                ic = iai.ic;
                logger.log("\n FOUND in CACHE: " + skey);
                set_ImageView();

                image_cache.preload(iai.index, ultimate, forward, image_file_source);
                return;

            }
            logger.log("\n NOT found in cache: " + skey);
        }


        Image_and_index iai = image_file_source.get_Image_and_index(target);
        if (iai == null) {
            clear_image_cache("null image (1) in change_image_relative");
            Change_gang.report_anomaly(ic.path.getParent());
            return;
        }
        if (iai.ic == null) {
            clear_image_cache("null image (2) in change_image_relative");
            return;
        }
        ic = iai.ic;

        logger.log("check size:" + ic.image.getWidth() + "x" + ic.image.getHeight());
        if ((ic.image.getHeight() < 1) && (ic.image.getWidth() < 1)) {
            if (ic.image_is_damaged == false) {
                clear_image_cache("bad image size");
            }
            return;
        }
        logger.log("change_image_relative OK! index is:" + target + " for file:" + ic.path.getFileName());

        set_ImageView();

        image_cache.preload(target, ultimate, forward, image_file_source);

    }


    private void clear_image_cache(String error) {
        logger.log("oops " + error + ", outOfMemory suspected, clearing image cache");
        image_cache.clear();
    }


    private int get_current_image_index() {
        if (ic.path == null) return -1;
        if (image_file_source == null)
            image_file_source = Image_file_source.get_Image_file_source(ic.path.getParent(), logger);
        int k = image_file_source.get_index_of(ic.path);
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

                if (ic.path == null) return;
                if (image_file_source == null)
                    image_file_source = Image_file_source.get_Image_file_source(ic.path.getParent(), logger);

                int target = image_file_source.check_index(new_index, false);
                Image_and_index iai = image_file_source.get_Image_and_index(target);

                if (iai == null) {
                    set_ImageView_null(ic.path.getParent());
                } else {
                    ic = iai.ic;
                    logger.log("change_image_absolute index is:" + new_index + " for file:" + ic.path.getFileName());
                    set_ImageView();
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
                tile_pane.getChildren().clear();
                the_stage.setTitle("No image to display in: " + dir.toAbsolutePath().toString());
                restore_cursor();
            }
        });

    }

    void check_stage_size() {
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

    //**********************************************************
    private void set_ImageView()
    //**********************************************************
    {

        //ic.imageView.setRotate(ic.get_rotation());
        set_background();

        Platform.runLater(new Runnable() {
            public void run() {
                double size = 300;
                ic = get_next_GIF();
                if (ic == null) return;

                tile_pane.getChildren().clear();
                //ic.imageView.fitWidthProperty().unbind();
                ic.imageView.setFitWidth(size);
                //ic.imageView.fitHeightProperty().unbind();
                ic.imageView.setPreserveRatio(true);
                tile_pane.getChildren().add(ic.imageView);
                set_stage_title(ic);
                for (int i = 1; i < 100; ) {
                    ic = get_next_GIF();
                    if (ic == null) break;
                    i++;
                    ic.imageView.setFitWidth(size);
                    ic.imageView.setPreserveRatio(true);
                    tile_pane.getChildren().add(ic.imageView);
                    logger.log("added:" + ic.path.getFileName());
                }


                if (mouse_mode == Mouse_mode.pix_for_pix) pix_for_pix();
                //restore_cursor();
            }
        });
    }


    //**********************************************************
    private void set_ImageView_compare_1000()
    //**********************************************************
    {

        //ic.imageView.setRotate(ic.get_rotation());
        set_background();

        Platform.runLater(new Runnable() {
            public void run() {
                double size = 1000;
                //ic = get_next_GIF();
                if (ic == null) return;

                tile_pane.getChildren().clear();
                //ic.imageView.fitWidthProperty().unbind();
                ic.imageView.setFitWidth(size);
                //ic.imageView.fitHeightProperty().unbind();
                ic.imageView.setPreserveRatio(true);
                tile_pane.getChildren().add(ic.imageView);
                set_stage_title(ic);
                for (int i = 1; i < 2; ) {
                    ic = get_alternate((int) size);
                    //ic = get_next_GIF();
                    if (ic == null) break;
                    i++;
                    ic.imageView.setFitWidth(size);
                    ic.imageView.setPreserveRatio(true);
                    tile_pane.getChildren().add(ic.imageView);
                    logger.log("added:" + ic.path.getFileName());
                }


                if (mouse_mode == Mouse_mode.pix_for_pix) pix_for_pix();
                //restore_cursor();
            }
        });
    }

    private Image_context get_next_GIF() {
        if (image_file_source == null)
            image_file_source = Image_file_source.get_Image_file_source(ic.path.getParent(), logger);
        int index = ic.get_index(image_file_source);
        for (; ; ) {
            index++;
            index = image_file_source.check_index(index, false);
            if (index == 0) return null;

            Image_and_index iai = image_file_source.get_Image_and_index(index);
            if (Guess_file_type_from_extension.is_gif_extension(iai.ic.path) == false) continue;

            return iai.ic;

        }

    }
    private Image_context get_alternate(int size)
    {
        if (image_file_source == null)
        {
            image_file_source = Image_file_source.get_Image_file_source(ic.path.getParent(),logger);
        }
        int index = ic.get_index(image_file_source);
        Image_and_index iai = image_file_source.get_Image_and_index2(index,size);
        return iai.ic;
    }


    //**********************************************************
    private void move_image(double dx, double dy)
    //**********************************************************
    {
        if (ic.imageView == null) return;
        double x = ic.imageView.getX();
        double image_pos_x = x + dx;
        ic.imageView.setX(image_pos_x);

        double y = ic.imageView.getY();
        double image_pos_y = y + dy;
        ic.imageView.setY(image_pos_y);

    }

    //**********************************************************
    private void change_zoom_factor(double mul)
    //**********************************************************
    {
        logger.log("old W=" + W + ", H=" + H);
        double h = ic.imageView.getFitHeight();
        H = h * mul;
        //double dy = (h-H)/2.0;
        double dy = (h - H);
        double w = ic.imageView.getFitWidth();
        W = w * mul;
        logger.log("new W=" + W + ", H=" + H);
        //double dx = (w-W)/2.0;
        double dx = (w - W);

        set_ImageView();
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
        Color c = new Color(1, 0, 0, 0.2);
        user_defined_zoom_area.setStroke(c);
        user_defined_zoom_area.setStrokeWidth(10.0f);
        tile_pane.getChildren().add(user_defined_zoom_area);
        user_defined_zoom_area.toFront();
    }

    //**********************************************************
    private void mouse_released_click_to_zoom(MouseEvent e)
    //**********************************************************
    {
        logger.log("mouse_released_local_zoom:");
        tile_pane.getChildren().remove(user_defined_zoom_area);
        if (user_is_selecting_zoom_area == false) return;
        user_is_selecting_zoom_area = false;

        if (user_defined_zoom_area.getWidth() < 5) return;
        if (user_defined_zoom_area.getHeight() < 5) return;

        if (ic.imageView.getViewport() != null) {
            logger.log("sorry, only one zoom supported at this time");
            ic.imageView.setViewport(null);
            return;
        }

        // need to correct the rectangle inside the picture

        logger.log("image :" + ic.image.getWidth() + "x" + ic.image.getHeight());

        Bounds bounds = ic.imageView.getLayoutBounds();
        logger.log("image View bounds x/y:" + bounds.getMinX() + "/" + bounds.getMinY() + "w/h :" + bounds.getWidth() + "x" + bounds.getHeight());
        logger.log("rectangle1 :" + user_defined_zoom_area.getX() + "/" + user_defined_zoom_area.getY() + " " + user_defined_zoom_area.getWidth() + "x" + user_defined_zoom_area.getHeight());

        double scale = ic.image.getWidth() / bounds.getWidth();

        Rectangle2D view_port = new Rectangle2D(
                (user_defined_zoom_area.getX() - ic.imageView.getLayoutX()) * scale,
                (user_defined_zoom_area.getY() - ic.imageView.getLayoutY()) * scale,
                user_defined_zoom_area.getWidth() * scale,
                user_defined_zoom_area.getHeight() * scale
        );
        logger.log("rectangle2 :" + view_port.getMinX() + "/" + view_port.getMinY() + " " + view_port.getWidth() + "x" + view_port.getHeight());

        ic.imageView.setViewport(view_port);
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


        Dragboard db = ic.imageView.startDragAndDrop(TransferMode.MOVE);

        ClipboardContent content = new ClipboardContent();
        List<File> l = new ArrayList<>();
        l.add(ic.path.toFile());
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

        if (Change_gang.is_my_directory_impacted(ic.path.getParent(), l, logger2) == false) return;
        image_cache.clear();
        logger2.log("Image_stage::you_receive_this_because_a_move_occurred_somewhere");
        boolean found = false;
        for (Old_and_new_Path oanf : l) {
            logger2.log("Image_stage, getting a you_receive_this_because_a_move_occurred_somewhere " + oanf.get_string());
            if (ic == null) {
                logger2.log("Image_stage, ic == null");
                continue;
            }
            if (ic.path == null) {
                logger2.log("Image_stage, ic.f == null");
                continue;
            }
            String current_Path_path = ic.path.toAbsolutePath().toString();
            if (oanf.get_old_Path().toAbsolutePath().toString().equals(current_Path_path)) {
                // the case when the image has been draged away is handled directly
                // by the setOnDragDone event handler

                // the case ee care for is when another type of event occured
                // for example the image was renamed
                if (image_file_source == null)
                    image_file_source = Image_file_source.get_Image_file_source(ic.path.getParent(), logger);
                Image_and_index im = image_file_source.get_image_for_path(oanf.new_Path);
                if (im == null) {
                    // the image was moved out of the current directory
                    logger.log("image moved out:" + oanf.get_string());
                } else {
                    logger.log("image renamed, same dir:" + oanf.get_string());

                    ic = im.ic;
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            set_ImageView();
                        }
                    });
                }

            }
        }


    }


    @Override
    public String get_string() {
        if (ic == null) return Stack_trace_getter.get_stack_trace("Image_stage NO CONTEXT????");
        else return "Image_stage " + ic.path.toAbsolutePath();
    }


    @Override
    public void set_title() {
        this.set_stage_title(ic);
    }
}
