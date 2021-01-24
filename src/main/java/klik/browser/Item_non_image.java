package klik.browser;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.*;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import klik.I18N.I18n;
import klik.deduplicate.Deduplication;
import klik.look.Look_and_feel_manager;
import klik.util.Logger;
import klik.util.Stack_trace_getter;
import klik.util.Tool_box;

import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

//**********************************************************
public class Item_non_image extends Item
//**********************************************************
{
    public static final boolean dbg = false;
    Button button;
    EventHandler<ActionEvent> the_button_event_handler;
    public final boolean is_dir;

    //**********************************************************
    public Item_non_image(
            Browser this_Browser_scene,
            Stage the_stage,
            Pane the_pane,
            Path path_, String text, Scene scene,
            double DIR_BUTTON_HEIGHT,
            double FILE_BUTTON_HEIGHT,
            Logger logger)
    //**********************************************************
    {
        super(this_Browser_scene, path_, scene, logger);
        if (path == null) {
            is_dir = false;
            button = new Button("----------");

            //button.setOnMouseEntered(e -> button.toFront());

            //button.setManaged(true);
            return;
        }

        if (Files.isDirectory(path))
        {
            is_dir = true;
            button_for_a_directory(the_stage, the_pane, text, DIR_BUTTON_HEIGHT, false);
            button.setPrefHeight(DIR_BUTTON_HEIGHT);

        }
        else
        {
            is_dir = false;
            button_for_a_non_image_file(the_stage, the_pane, text, FILE_BUTTON_HEIGHT);
            button.setPrefHeight(FILE_BUTTON_HEIGHT);
        }

        button.setManaged(true);
        //button.setManaged(false);
        button.setMnemonicParsing(false);// avoid suppression of first underscore in names

        init_drag_and_drop();
    }

    //**********************************************************
    private void button_for_a_non_image_file(
            Stage the_stage,
            Pane the_pane,
            String text,
            double FILE_BUTTON_HEIGHT)
    //**********************************************************
    {
        button = new Button(text);
        button.setMinHeight(FILE_BUTTON_HEIGHT);
        Look_and_feel_manager.get_instance().set_file_style(button);

        the_button_event_handler = new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent actionEvent)
            {
                logger.log("asking the system to open: " + path.toAbsolutePath());

                Runnable r = new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            Desktop.getDesktop().open(path.toAbsolutePath().toFile());
                        }
                        catch (Exception e)
                        {
                            logger.log(Stack_trace_getter.get_stack_trace("open failed :" + e));

                            if (e.toString().contains("doesn't exist."))
                            {
                                Tool_box.popup_text("Failed?", "Your OS/GUI could not open this file, the error is:\n" + e);
                            }
                            else
                            {
                                Tool_box.popup_text("Failed?", "Your OS/GUI could not open this file, the error is:\n" + e + "\nMaybe it is just not properly configured e.g. most often the file extension has to be registered?");
                            }
                        }

                    }
                };
                Tool_box.execute(r, logger);
            }
        };
        button.setOnAction(the_button_event_handler);

        give_a_menu_to_the_button(the_stage, the_pane, false);
    }

    //**********************************************************
    void button_for_a_directory(Stage the_stage, Pane the_pane, String text, double DIR_BUTTON_HEIGHT, boolean is_trash)
    //**********************************************************
    {
        button = new Button(text);
        button.setMinHeight(DIR_BUTTON_HEIGHT);
        button.setMnemonicParsing(false);// avoid suppression of first underscore in names

        //Look_and_feel.set_button_style(button, true);
        Look_and_feel_manager.set_button_look_as_folder(button,DIR_BUTTON_HEIGHT);

        if (path == null)
        {
            // protect crash when going up: root has no parent
            return;
        }

        the_button_event_handler = new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent actionEvent)
            {
                Browser.create_browser(the_browser, true, path, false, logger);
            }
        };

        button.setOnAction(the_button_event_handler);

        make_button_drop_receiver_capable();
        give_a_menu_to_the_button(the_stage, the_pane, is_trash);
    }

    //**********************************************************
    private void make_button_drop_receiver_capable()
    //**********************************************************
    {
        button.setOnDragEntered(new EventHandler<DragEvent>() {
            public void handle(DragEvent event) {
                if (dbg) logger.log("OnDragEntered for button !!" + event);

                //Constants.set_button_style;
                if (Files.isDirectory(path))
                {
                    Look_and_feel_manager.get_instance().set_hovered_directory_style(button);
                }
                else
                {
                    Look_and_feel_manager.get_instance().set_hovered_file_style(button);
                }
                event.consume();
            }
        });

        button.setOnDragExited(new EventHandler<DragEvent>()
        {
            public void handle(DragEvent event) {
                if (dbg) logger.log("OnDragExited for button !!" + event);

                /* mouse moved away, remove the graphical cues */
                if (Files.isDirectory(path))
                {
                    Look_and_feel_manager.get_instance().set_directory_style(button);
                }
                else
                {
                    Look_and_feel_manager.get_instance().set_file_style(button);
                }
                event.consume();
            }
        });


        button.setOnDragOver(new EventHandler<DragEvent>()
        {
            public void handle(DragEvent event)
            {
                if (dbg) logger.log("OnDragOver for button !!" + event);
                event.acceptTransferModes(TransferMode.MOVE);
                event.consume();
            }
        });

        button.setOnDragDropped(new EventHandler<DragEvent>()
        {
            public void handle(DragEvent event)
            {

                if (dbg) logger.log("OnDragDropped for button !!" + event);

                Tool_box.accept_drag_dropped_as_a_move_in(
                        event,
                        path,
                        button,
                        "button",
                        logger);
                /*


                Dragboard db = event.getDragboard();
                List<File> l = db.getFiles();
                List<Old_and_new_Path> loan = new ArrayList<Old_and_new_Path>();
                for (File fff : l) {
                    if ( dbg) logger.log(" 1 drag ACCEPTED for: " + fff.getAbsolutePath());

                    Path new_fff = Paths.get(path.toAbsolutePath().toString(), fff.getName());

                    Path old_Path_ = fff.toPath();
                    Path new_Path_ = new_fff;
                    Command_old_and_new_Path cmd_ = Command_old_and_new_Path.command_move;
                    Old_and_new_Path oan = new Old_and_new_Path(old_Path_, new_Path_, cmd_, Status_old_and_new_Path.before_command);
                    loan.add(oan);
                }

                Tool_box.perform_the_safe_moves_in_a_thread(loan,logger);
                // tell the source
                event.setDropCompleted(true);
                event.consume();

                 */
            }
        });
        button.setOnDragDetected(new EventHandler<MouseEvent>()
        {
            public void handle(MouseEvent event)
            {
                if (dbg) logger.log("OnDragDetected for button !!" + event);

                Dragboard db = button.startDragAndDrop(TransferMode.MOVE);

                ClipboardContent content = new ClipboardContent();
                List<File> l = new ArrayList<>();
                l.add(path.toFile());
                content.putFiles(l);
                db.setContent(content);
                event.consume();
            }
        });

        button.setOnDragDone(new EventHandler<DragEvent>()
        {
            public void handle(DragEvent event)
            {
                if (dbg) if (event.getTransferMode() == TransferMode.MOVE)
                {
                    logger.log("OnDragDone for button !!" + event);
                }
                event.consume();
            }
        });
    }

    //**********************************************************
    public void give_a_menu_to_the_button(Stage the_stage, Pane the_pane, boolean is_trash)
    //**********************************************************
    {
        ContextMenu context_menu = new ContextMenu();

        if ( is_trash == false)
        {
            javafx.scene.control.MenuItem menu_item = new javafx.scene.control.MenuItem(I18n.get_I18n_string("Rename",logger));
            menu_item.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event)
                {
                    if (dbg) logger.log("Renaming!");

                    //button.setDisable(true); no!!!
                    //button.removeEventHandler(ActionEvent.ACTION,the_button_event_handler);
                    //if (dbg) logger.log("button action null!");

                    //rename_file_or_dir(this_Browser_scene, path, logger);
                    String original = path.getFileName().toString();
                    javafx.scene.control.TextField text_edit = new javafx.scene.control.TextField(original);
                    Node restored = button.getGraphic();
                    button.setGraphic(text_edit);
                    text_edit.setMinWidth(button.getWidth() * 0.9);
                    text_edit.requestFocus();
                    text_edit.positionCaret(original.length());
                    text_edit.setFocusTraversable(true);
                    text_edit.setOnAction(new EventHandler<ActionEvent>()
                    {
                        @Override
                        public void handle(ActionEvent actionEvent) {
                            String result = text_edit.getText();
                            //button.setGraphic(new Text(result));
                            button.setText(result);
                            button.setGraphic(restored);
                            actionEvent.consume();
                            path = Tool_box.change_dir_name(path, logger, result);
                            if (dbg) logger.log("rename done");
                           // button.setOnAction(the_button_event_handler);
                        }
                    });
                }
            });
            context_menu.getItems().add(menu_item);

        }
        if ( is_trash)
        {
            javafx.scene.control.MenuItem menu_item = new javafx.scene.control.MenuItem(I18n.get_I18n_string("Clear_Trash_Folder",logger));
            menu_item.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    if (dbg) logger.log("clearing trash!");
                    Tool_box.clear_trash(logger);
                }
            });
            context_menu.getItems().add(menu_item);
        }
        else
        {
            javafx.scene.control.MenuItem menu_item = new javafx.scene.control.MenuItem(I18n.get_I18n_string("Delete",logger));
            menu_item.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    if (dbg) logger.log("Deleting!");
                    Tool_box.safe_delete_one(path, logger);
                }
            });
            context_menu.getItems().add(menu_item);

        }
        if (Files.isDirectory(path))
        {
            MenuItem size = new javafx.scene.control.MenuItem(I18n.get_I18n_string("Get_folder_size",logger));
            context_menu.getItems().add(size);
            size.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle(path.toAbsolutePath().toString());
                    //alert.setResizable(true);
                   // alert.onShownProperty().addListener(e->{Platform.runLater(()->alert.setResizable(false));});
                    alert.getDialogPane().setMinWidth(1000);
                    alert.setHeaderText("Please wait...");
                    alert.setContentText("...can take a while");
                    alert.getDialogPane().setStyle("-fx-font-size:20px;");
                    alert.show();

                    Runnable r = new Runnable() {
                        @Override
                        public void run() {

                            long size = Tool_box.get_size_on_disk(path,logger);
                            long d = Tool_box.get_how_many_folders(path,logger);
                            long f = Tool_box.get_how_many_files(path,logger);
                            long i = Tool_box.get_how_many_images(path,logger);

                            Platform.runLater(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    String text;
                                    if ( size < 0)
                                    {
                                        text = "An error occurred, probably Access Denied, check the logs";
                                    }
                                    else {
                                        //text = path.toAbsolutePath()+"\n size on disk: "+Tool_box.get_1_line_string_with_size(size);
                                        text = "size on disk: "+Tool_box.get_1_line_string_with_size(size);
                                    }
                                    String s = I18n.get_I18n_string("Number_of_folders_files_images",logger);
                                    alert.setHeaderText(s+d+"/"+f+"/"+i);

                                    Label label = new Label(text);
                                    label.setWrapText(true);
                                    alert.getDialogPane().setContent(label);

                                    //alert.setContentText(text);
                                    if (dbg) logger.log(text);
                                }
                            });
                        }
                    };
                    (new Thread(r)).start();
                }
            });
            MenuItem browse = new javafx.scene.control.MenuItem("Browse in new window");
            context_menu.getItems().add(browse);
            browse.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    if (dbg) logger.log("Browse in new window!");
                    Browser.create_browser(null, false, path, false, logger);
                }
            });

            if ( is_trash == false)
            {
                class Trick {
                    Button but = null;
                    Deduplication deduplication = null;
                }
                ;
                Trick trick = new Trick();
                trick.but = button;
                javafx.scene.control.Menu sub = new javafx.scene.control.Menu("File deduplication tool");
                context_menu.getItems().add(sub);

                javafx.scene.control.MenuItem itemhelp = new javafx.scene.control.MenuItem("Help on file deduplication");
                itemhelp.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        Tool_box.popup_text("Help on deduplication",
                                "The deduplication tool will look recursively down the path starting at:" + path.toAbsolutePath().toString() +
                                        "\nLooking for identical files in terms of file content i.e. names/path are different but it IS the same file" +
                                        " Then you will be able to either:" +
                                        "\n  1. Review each pair of duplicate files one by one" +
                                        "\n  2. Or ask for automated deduplication (DANGER!)" +
                                        "\n  Beware: automated de-duplication may give unexpected results" +
                                        " since you do not choose which file in the pair is deleted." +
                                        "\n  However, the files are not actually deleted: they are MOVED to the klik_trash folder," +
                                        " which you can visit by clicking on the trash button." +
                                        "\n\n WARNING: On folders containing a lot of data, the search can take a long time!"
                        );
                    }
                });
                sub.getItems().add(itemhelp);
                javafx.scene.control.MenuItem item0 = new javafx.scene.control.MenuItem("Start MANUAL (one by one confirmation) search for duplicate files");
                item0.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        if (dbg) logger.log("Deduplicate!");
                        trick.deduplication = new Deduplication(path.toFile(), logger);
                        trick.deduplication.look_for_all_files(false);
                    }
                });
                sub.getItems().add(item0);
            /*
            MenuItem item1 = new MenuItem("Look at next duplicate file pair");
            item1.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {

                    if ( trick.deduplication == null)
                    {
                        Tool_box.popup_text("Ohoh!?","You must launch search first");
                        return;
                    }
                    if ( dbg) logger.log("Look at next duplicate pair");
                    trick.deduplication.deduplicate_one();
                }
            });
            sub.getItems().add(item1);
            //item1.setDisable(true);
            */
                javafx.scene.control.MenuItem menu_item = new javafx.scene.control.MenuItem("De-duplicate ALL (see help!)");
                menu_item.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        trick.deduplication = new Deduplication(path.toFile(), logger);
                        trick.deduplication.look_for_all_files(true);
                    }
                });
                sub.getItems().add(menu_item);
                //menu_item.setDisable(true);

            }
        }
        else
        {
            // is a file
            context_menu.getItems().add(Item.create_show_file_size_menu_item(path, dbg,logger));
        }
        button.setOnContextMenuRequested(new EventHandler<ContextMenuEvent>()
        {
            @Override
            public void handle(ContextMenuEvent event)
            {
                context_menu.show(the_pane, event.getScreenX(), event.getScreenY());
            }
        });
    }


    @Override
    public Node get_Node() {
        return button;
    }

    @Override
    public void set_MinWidth(double w) {
        button.setMinWidth(w);
    }

    @Override
    public double get_Width() {
        return button.getWidth();
    }

    @Override
    public void set_MinHeight(double h) {
        button.setMinHeight(h);
    }

    @Override
    public double get_Height() {
        return button.getHeight();
    }

    @Override
    public void set_Image(Image i, boolean real) {
    }

    @Override
    public String get_string() {
        if (is_dir) return "dir " + get_Path().toAbsolutePath();
        return "file " + get_Path().toAbsolutePath();
    }
}
