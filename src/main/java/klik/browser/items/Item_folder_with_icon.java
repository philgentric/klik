package klik.browser.items;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.actor.Job;
import klik.animated_gifs_from_videos.Animated_gif_from_folder;
import klik.browser.*;
import klik.browser.icons.Icon_destination;
import klik.browser.icons.Icon_factory_actor;
import klik.browser.icons.Icon_factory_request;
import klik.browser.icons.Icon_manager;
import klik.level2.deduplicate.Deduplication_engine;
import klik.files_and_paths.Files_and_Paths;
import klik.files_and_paths.Guess_file_type;
import klik.files_and_paths.Sizes;
import klik.images.decoding.Fast_rotation_from_exif_metadata_extractor;
import klik.look.Font_size;
import klik.look.Look_and_feel;
import klik.look.Look_and_feel_manager;
import klik.look.my_i18n.I18n;
import klik.properties.Static_application_properties;
import klik.util.Logger;
import klik.util.Popups;
import klik.util.Stack_trace_getter;
import klik.util.Threads;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


//**********************************************************
public class Item_folder_with_icon extends Item implements Icon_destination, File_count_receiver, Disk_foot_print_receiver
//**********************************************************
{
    public static final boolean dbg = false;
    public final boolean is_trash;
    public final boolean is_parent;
    public final boolean has_images;
    public final String text;

    double estimated_text_label_height;
    ImageView the_image_view;
    VBox vbox;
    Label label1;
    Label label2;
    Job job;

    private boolean ignore_next_mouse_clicked = false;
    String file_count_text = ".......";
    String disk_footprint_text = ".......";

    //**********************************************************
    public Item_folder_with_icon(
            Browser browser,
            Path path_,
            String text_,
            boolean is_trash_, boolean is_parent_,
            boolean has_images_,
            Logger logger)
    //**********************************************************
    {
        super(browser, path_, logger);
        text = text_;
        is_trash = is_trash_;
        is_parent = is_parent_;
        has_images = has_images_;
        vbox = new VBox();
        Look_and_feel_manager.set_vbox_look(vbox);
        vbox.setAlignment(Pos.TOP_CENTER);

        double font_size = Static_application_properties.get_font_size(logger);
        estimated_text_label_height = klik.look.Look_and_feel.MAGIC_HEIGHT_FACTOR*font_size;
        {
            HBox hbox1 = new HBox();
            vbox.getChildren().add(hbox1);
            double icon_height = Look_and_feel.MAGIC_HEIGHT_FACTOR * font_size;
            Image folder_icon = Look_and_feel_manager.get_folder_icon(icon_height);

            if (folder_icon == null) {
                logger.log("WARNING: cannot get default icon for directory");
            }
            else
            {
                ImageView folder_icon_image_view = new ImageView(folder_icon);
                folder_icon_image_view.setPreserveRatio(true);
                folder_icon_image_view.setFitHeight(font_size);
                hbox1.getChildren().add(folder_icon_image_view);
            }

            if (path == null) {
                logger.log(Stack_trace_getter.get_stack_trace("SHIT HAPPENS"));
                label1 = new Label("----------");
                hbox1.getChildren().add(label1);
                return;
            }

            if (!Files.isDirectory(path))
            {
                logger.log(Stack_trace_getter.get_stack_trace("SHOULD NOT HAPPEN"));
                return;
            }

            if( has_images)
            {
                label1 =  for_a_directory(text,estimated_text_label_height/2, false);
                hbox1.getChildren().add(label1);
                //logger.log("has_images: "+has_images+" path=" +path_+" loading the true image");
                if (dbg) logger.log("setting image tooltip");
                Icon_factory_request ifr = new Icon_factory_request(this, icon_size);
                job = Icon_factory_actor.get_icon_factory(this.browser.my_Stage.the_Stage, logger).make_icon(ifr);
            }
            else
            {
                label1 =  for_a_directory(text,estimated_text_label_height/2, true);
                hbox1.getChildren().add(label1);
                //logger.log("has_images: "+has_images+" path=" +path_+" loading the DUMMY image");
                the_image_view = new ImageView();
                Image image = Look_and_feel_manager.get_dummy_icon(icon_size);
                the_image_view.setImage(image);
                the_image_view.setPreserveRatio(true);
                HBox hbox2 = new HBox();
                hbox2.getChildren().add(the_image_view);
                label2 = new Label("disk foot print");
                hbox2.getChildren().add(label2);
                double local_font_size = font_size;
                double icon_size = Static_application_properties.get_icon_size(logger);
                if ( icon_size <128) local_font_size = 10;
                if ( icon_size <64) local_font_size = 8;
                Font_size.apply_font_size(label2, local_font_size,logger);
                Look_and_feel_manager.set_label_look(label2);
                vbox.getChildren().add(hbox2);
            }


            label1.setTextOverrun(OverrunStyle.ELLIPSIS);
            Font_size.apply_font_size(label1, logger);
            Look_and_feel_manager.set_label_look(label1);
        }


        resize_the_box();

        init_drag_and_drop();
    }

    //**********************************************************
    @Override
    public void cancel_custom()
    //**********************************************************
    {
        Actor_engine.cancel_one(job);
        job = null;
    }

    //**********************************************************
    private void resize_the_box()
    //**********************************************************
    {
        if ( Static_application_properties.get_single_column(logger))
        {
            vbox.setPrefWidth(browser.my_Stage.the_Stage.getWidth()-30);
            vbox.setMinWidth(browser.my_Stage.the_Stage.getWidth()-30);
        }
        else {
            int button_width = Static_application_properties.get_column_width(logger);
            if (button_width < Icon_manager.MIN_COLUMN_WIDTH) button_width = Icon_manager.MIN_COLUMN_WIDTH;
            if (button_width < icon_size) button_width = icon_size;
            vbox.setPrefWidth(button_width);
            vbox.setMinWidth(button_width);
            vbox.setPrefHeight(icon_size + estimated_text_label_height);
            vbox.setMinHeight(icon_size + estimated_text_label_height);
        }
    }


    //**********************************************************
    @Override
    public void set_Image(Image image, boolean real)
    //**********************************************************
    {
        if ( image == null)
        {
            the_image_view = null;
            return;
        }
        if ( the_image_view == null) the_image_view = new ImageView();
        //logger.log(Stack_trace_getter.get_stack_trace("item non image set tooltip icon for "+path+ " image = "+i.getWidth()));
        the_image_view.setImage(image);
        the_image_view.setPreserveRatio(true);
        Path local = get_path_for_display();
        if ( local!=null)
        {
            Double tmp = Fast_rotation_from_exif_metadata_extractor.get_rotation(local,aborter,logger);
            if ( tmp != null) rotation = tmp;
        }

        rotate_and_center(image, the_image_view);

        int button_width = Static_application_properties.get_column_width(logger);
        if ( button_width < Icon_manager.MIN_COLUMN_WIDTH) button_width = Icon_manager.MIN_COLUMN_WIDTH;
        if ( icon_size > button_width)
        {
            double w = icon_size-10;
            //double w = Icon_manager.MIN_OTHER_BUTTON_WIDTH - 10;
            //if ( w < icon_size) w = icon_size-10;
            if ( image.getWidth() > image.getHeight())
            {
                if ( (rotation ==0) || (rotation== 180))
                {
                    // downscale to fit in the VBOX WIDTH
                    the_image_view.setFitWidth(w);
                    the_image_view.setFitHeight(-1); // reset Height!
                }
                else
                {
                    the_image_view.setFitWidth(w);
                }
            }
        }
        resize_the_box();
    }
    //**********************************************************
    @Override
    public void receive_icon(Image icon)
    //**********************************************************
    {
        if ( the_image_view == null) the_image_view = new ImageView();
        Platform.runLater(() -> {
            vbox.getChildren().add(the_image_view);
            set_Image(icon,true);
        });
    }

    // this call is intended only from a working thread typically: in the icon factory 
    //**********************************************************
    @Override
    public Path get_path_for_display()
    //**********************************************************
    {
        //logger.log(Stack_trace_getter.get_stack_trace("get_path_for_display "+get_item_path()));
        if (is_trash) return null;
        if (is_parent) return null;
        // for a file the displayed icon is built from THE FILE ITSELF, if supported:
        if ( !path.toFile().isDirectory()) return path;

        // for a folder we have 3 ways to provide an icon
        // 1) an image is taken from the folder and used as icon
        // 2) multiple images are taken from the folder to form an animated gif icon
        // 3) if there are no documents that we know how to iconify, use defaalt

        // try to find an icon for the folder

        File dir = path.toFile();
        File[] files = dir.listFiles();
        if ( files == null)
        {
            if ( dbg) logger.log("WARNING: dir is access denied: "+path);
            return null;
        }
        if ( files.length == 0)
        {
            if ( dbg) logger.log("dir is empty: "+path);
            return null;
        }
        Arrays.sort(files);
        List<File> images_in_folder = new ArrayList<>();

        for ( File f : files)
        {
            if (f.isDirectory()) continue; // ignore folders
            if (!Guess_file_type.is_file_a_image(f)) continue; // ignore non images
            if (Guess_file_type.is_this_path_a_gif(f.toPath()))
            {
                if (Guess_file_type.is_this_path_a_animated_gif(f.toPath(), aborter, logger))
                {
                    return f.toPath();
                }
                continue; // ignore not animated gifs
            }
            images_in_folder.add(f);
        }
        if ( images_in_folder.isEmpty())
        {
            for (File folder : files)
            {
                if (folder.isDirectory())
                {
                    File[] files2 = folder.listFiles();
                    if ( files2 == null) return null;
                    Arrays.sort(files2);
                    for (File f2 : files2)
                    {
                        if (f2.isDirectory()) continue; // ignore folders
                        if (!Guess_file_type.is_file_a_image(f2)) continue; // ignore non images
                        if (Guess_file_type.is_this_path_a_gif(f2.toPath()))
                        {
                            if (Guess_file_type.is_this_path_a_animated_gif(f2.toPath(),aborter,logger))
                            {
                                return f2.toPath();
                            }
                            continue; // ignore not animated gifs
                        }
                        return f2.toPath();
                    }
                }
            }
            return null;
        }

        Path returned = Animated_gif_from_folder.make_animated_gif_from_all_images_in_folder(browser.my_Stage.the_Stage,path,  images_in_folder,  logger);
        if ( returned != null) return returned;
        if (images_in_folder.isEmpty())
        {
            return null;
        }
        else
        {
            return images_in_folder.get(0).toPath();
        }

    }
    //**********************************************************
    public static boolean would_produce_an_image_down_in_the_tree_files(Path local_path, Logger logger)
    //**********************************************************
    {
       // long start = System.currentTimeMillis();
        File dir = local_path.toFile();
        File[] files = dir.listFiles();
        if (files == null) {
            if (dbg) logger.log("WARNING: dir is access denied: " + local_path);
            return false;
        }
        if (files.length == 0) {
            if (dbg) logger.log("dir is empty: " + local_path);
            return false;
        }

        int count = 0;
        for (File f : files) {
            if (f.isDirectory()) continue; // ignore folders
            if (Guess_file_type.is_file_a_image(f))
            {
                count++;
                break;
            }
        }

        //logger.log("would_produce_an_image_down_in_the_tree_files(2) for " + local_path + " in " + (System.currentTimeMillis() - start) + " milliseconds");
        if (count == 0) return false;
        return true;
    }
    
    //**********************************************************
    @Override // Item
    public void set_is_unselected_internal()
    {
        Look_and_feel_manager.give_button_a_file_style(vbox);
    }


    //**********************************************************
    @Override // Item
    public void set_is_selected_internal()
    //**********************************************************
    {
        Look_and_feel_manager.give_button_a_selected_file_style(vbox);
    }



    //**********************************************************
    private Label for_a_directory(String text, double icon_height, boolean extended)
    //**********************************************************
    {
        Label label;
        if (Static_application_properties.get_show_folder_size(logger))
        {
            String text2 = text+"................"; // temporary string before the file count is determined in a worker thread
            label = new Label(text2);
        }
        else
        {
            label = new Label(text);
        }

        Look_and_feel_manager.set_label_look_for_folder(label,icon_height);
        if (path == null)
        {
            // protect crash when going up: root has no parent
            return label;
        }

        vbox.setOnMouseClicked(mouse_event -> {
            if ( dbg) logger.log("\n\n\nItem_folder_with_icon MouseClicked: "+ mouse_event.toString()+"\n\n\n");

            if ( ignore_next_mouse_clicked)
            {
                if(dbg) logger.log("\n\n\nMOUSE CLICK IGNORED! due to ignore_next_mouse_clicked="+ignore_next_mouse_clicked);
                ignore_next_mouse_clicked = false;
                return;
            }

            if (mouse_event.getButton() == MouseButton.SECONDARY )
            {
                //logger.log("isSecondaryButtonDown");
                // ignore the event, we need to pass it through to get the context menu
                return;
            }
            Path scroll_to = null;
            if (is_parent)
            {
                // special case when going back with the "Parent" button
                scroll_to = browser.get_top_left();
                //logger.log("Item_folder_with_icon is parent, scroll_to=browser.top_left_in_parent = "+scroll_to);
            }
            else
            {
                //logger.log("Item_folder_with_icon is NOT parent");
            }
            Browser_creation_context.replace_different_folder(path,browser, scroll_to, logger);
            mouse_event.consume();
        });
        make_button_drop_receiver_capable();
        give_a_menu_to_the_button();

        if ( extended)
        {
            show_disk_foot_print_folder(this, text, path, aborter, logger);
        }
        else
        {
            if (Static_application_properties.get_show_folder_size(logger))
            {
                show_how_many_files_folder(this, text, path, aborter, logger);
            }
        }
        return label;
    }


    //**********************************************************
    private void make_button_drop_receiver_capable()
    //**********************************************************
    {
        vbox.setOnDragEntered(drag_event -> {
            if (Drag_and_drop.drag_and_drop_dbg) logger.log("OnDragEntered for button" );

            set_background_for_setOnDragEntered();
            //Constants.set_button_style;
            /*if (Files.isDirectory(path))
            {
                //logger.log("OnDragEntered for button, path is a dir" + event);
                Objects.requireNonNull(Look_and_feel_manager.get_instance()).set_hovered_directory_style(vbox);
                logger.log("set_hovered_directory_style");
            }*/
            drag_event.consume();
        });

        vbox.setOnDragExited(drag_event -> {
            if (Drag_and_drop.drag_and_drop_dbg) logger.log("Item_folder_with_icon::OnDragExited");

            set_background_for_setOnDragExited();


            /* mouse moved away, remove the graphical cues */
            /*if (Files.isDirectory(path))
            {
                Look_and_feel_manager.give_button_a_directory_style(vbox);
                logger.log("restoring directory normal style on drag exited");

            }
            else
            {
                Look_and_feel_manager.give_button_a_file_style(vbox);
                logger.log("restoring file normal style on drag exited");
            }

             */
            ignore_next_mouse_clicked = true;
            drag_event.consume();
        });


        vbox.setOnDragOver(drag_event -> {
            if (Drag_and_drop.drag_and_drop_dbg) logger.log("Item_folder_with_icon OnDragOver");
            drag_event.acceptTransferModes(TransferMode.MOVE);
            set_background_for_setOnDragOver();
            drag_event.consume();
        });

        vbox.setOnDragDropped(drag_event -> {

            if (Drag_and_drop.drag_and_drop_dbg) logger.log("OnDragDropped for button");

            Drag_and_drop.accept_drag_dropped_as_a_move_in(
                    browser.my_Stage.the_Stage,
                    drag_event,
                    path,
                    vbox,
                    "button",
                    logger);
            drag_event.consume();
        });
        vbox.setOnDragDetected(drag_event -> {
            if (Drag_and_drop.drag_and_drop_dbg) logger.log("OnDragDetected for button !!" + drag_event);

            Dragboard db = vbox.startDragAndDrop(TransferMode.MOVE);

            ClipboardContent content = new ClipboardContent();
            List<File> l = new ArrayList<>();
            l.add(path.toFile());
            content.putFiles(l);
            db.setContent(content);
            drag_event.consume();
        });

        vbox.setOnDragDone(drag_event -> {
            if (Drag_and_drop.drag_and_drop_dbg)
            {
                if (drag_event.getTransferMode() == TransferMode.MOVE)
                {
                    logger.log("OnDragDone for button !!" + drag_event);
                }
            }

            drag_event.consume();
        });
    }


    //**********************************************************
    public void give_a_menu_to_the_button()
    //**********************************************************
    {
        ContextMenu context_menu = new ContextMenu();

        if (!Files.isDirectory(path))
        {
            // is a "plain" file
            //context_menu.getItems().add(create_rename_dir_menu_item());
            context_menu.getItems().add(Item.create_show_file_size_menu_item(browser, path, dbg,logger));
            context_menu.getItems().add(Item.create_edit_tag_menu_item(path, dbg,logger));
        }
        else
        {
            // is a folder
            context_menu.getItems().add(create_get_folder_size_menu_item());
            if ( is_trash)
            {
                MenuItem menu_item = create_clear_trash_menu_item();
                context_menu.getItems().add(menu_item);
            }

            if(!is_trash && !is_parent)
            {
                context_menu.getItems().add(create_browse_in_new_window_menu_item());
                context_menu.getItems().add(Item.create_edit_tag_menu_item(path, dbg, logger));
                context_menu.getItems().add(create_rename_dir_menu_item());
                context_menu.getItems().add(create_delete_dir_menu_item());
                context_menu.getItems().add(create_copy_dir_menu_item());
                {
                    Menu sub = new Menu("File deduplication tool");
                    context_menu.getItems().add(sub);
                    sub.getItems().add(create_help_on_deduplication_menu_item());
                    sub.getItems().add(create_deduplication_count_menu_item());
                    sub.getItems().add(create_manual_deduplication_menu_item());
                    sub.getItems().add(create_auto_deduplication_menu_item());
                }
            }
        }




        vbox.setOnContextMenuRequested(event -> {
            if( dbg) logger.log("show context menu of button:"+ path.toAbsolutePath());
            context_menu.show(vbox, event.getScreenX(), event.getScreenY());
        });
    }

    //**********************************************************
    private MenuItem create_auto_deduplication_menu_item()
    //**********************************************************
    {
        String text = I18n.get_I18n_string("Deduplicate_auto",logger);
        MenuItem menu_item = new MenuItem(text);
        menu_item.setOnAction(event -> {
            if (dbg) logger.log("Deduplicate auto");

            if ( !Popups.popup_ask_for_confirmation(browser.my_Stage.the_Stage, "EXPERIMENTAL! Are you sure?","Automated deduplication will recurse down this folder and delete (for good = not send them in recycle bin) all duplicate files",logger)) return;
            (new Deduplication_engine(browser, path.toFile(), logger)).do_your_job(true);
        });
        return menu_item;
    }



    //**********************************************************
    private MenuItem create_manual_deduplication_menu_item()
    //**********************************************************
    {
        String text = I18n.get_I18n_string("Deduplicate_manual",logger);

        MenuItem item0 = new MenuItem(text);
        item0.setOnAction(event -> {
            if (dbg) logger.log("Deduplicate manually");
            (new Deduplication_engine(browser, path.toFile(), logger)).do_your_job(false);
        });
        return item0;
    }
    //**********************************************************
    private MenuItem create_deduplication_count_menu_item()
    //**********************************************************
    {
        String text = I18n.get_I18n_string("Deduplicate_count",logger);
        MenuItem item0 = new MenuItem(text);
        item0.setOnAction(event -> {
            if (dbg) logger.log("count duplicates!");
            (new Deduplication_engine(browser, path.toFile(), logger)).count(false);
        });
        return item0;
    }


    //**********************************************************
    private MenuItem create_help_on_deduplication_menu_item()
    //**********************************************************
    {
        String text = I18n.get_I18n_string("Deduplicate_help",logger);
        MenuItem itemhelp = new MenuItem(text);
        itemhelp.setOnAction(event -> Popups.popup_warning(browser.my_Stage.the_Stage,
                "Help on deduplication",
                "The deduplication tool will look recursively down the path starting at:" + path.toAbsolutePath() +
                        "\nLooking for identical files in terms of file content i.e. names/path are different but it IS the same file" +
                        " Then you will be able to either:" +
                        "\n  1. Review each pair of duplicate files one by one" +
                        "\n  2. Or ask for automated deduplication (DANGER!)" +
                        "\n  Beware: automated de-duplication may give unexpected results" +
                        " since you do not choose which file in the pair is deleted." +
                        "\n  However, the files are not actually deleted: they are MOVED to the klik_trash folder," +
                        " which you can visit by clicking on the trash button." +
                        "\n\n WARNING: On folders containing a lot of data, the search can take a long time!",
     false,
                logger));
        return itemhelp;
    }

    //**********************************************************
    private MenuItem create_browse_in_new_window_menu_item()
    //**********************************************************
    {
        MenuItem browse = new MenuItem("Browse in new window");
        browse.setOnAction(event -> {
            if (dbg) logger.log("Browse in new window!");
            Browser_creation_context.additional_different_folder(path,browser,logger);
        });
        return browse;
    }

    //**********************************************************
    private MenuItem create_get_folder_size_menu_item()
    //**********************************************************
    {
        MenuItem size = new MenuItem(I18n.get_I18n_string("Get_folder_size",logger));
        size.setOnAction(event -> Item_button.get_folder_size(path,browser,aborter, logger));
        return size;
    }

/*
    //**********************************************************
    private MenuItem create_slide_show_random_menu_item()
    //**********************************************************
    {
        MenuItem menu_item = new MenuItem("start a random slide show"); //I18n.get_I18n_string("Clear_Trash_Folder",logger));
        menu_item.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                if (dbg) logger.log("random deep slide show");

                // TODO
            }
        });
        return menu_item;
    }

 */
    //**********************************************************
    private MenuItem create_clear_trash_menu_item()
    //**********************************************************
    {
        MenuItem menu_item = new MenuItem(I18n.get_I18n_string("Clear_Trash_Folder",logger));
        menu_item.setOnAction(event -> {
            if (dbg) logger.log("clearing trash!");
            Files_and_Paths.clear_trash_with_warning(browser.my_Stage.the_Stage,aborter, logger);
        });
        return menu_item;
    }

    //**********************************************************
    private MenuItem create_delete_dir_menu_item()
    //**********************************************************
    {
        MenuItem menu_item = new MenuItem(I18n.get_I18n_string("Delete", logger));
        menu_item.setOnAction(event -> {
            if (dbg) logger.log("Deleting!");
            Files_and_Paths.move_to_trash(browser.my_Stage.the_Stage,path, null, new Aborter(), logger);
        });
        return menu_item;
    }

    //**********************************************************
    private MenuItem create_rename_dir_menu_item()
    //**********************************************************
    {
        MenuItem menu_item = new MenuItem(I18n.get_I18n_string("Rename", logger));
        menu_item.setOnAction(event -> {

            if (dbg) logger.log("Item_folder_with_icon: Renaming");
            String original_name = path.getFileName().toString();
            TextField text_edit = new TextField(original_name);
            Node restored = label1.getGraphic();
            label1.setGraphic(text_edit);
            text_edit.setMinWidth(label1.getWidth() * 0.9);
            text_edit.requestFocus();
            text_edit.positionCaret(original_name.length());
            text_edit.setFocusTraversable(true);
            text_edit.setOnAction(actionEvent -> {
                String new_dir_name = text_edit.getText();
                actionEvent.consume();
                Path new_path = Files_and_Paths.change_dir_name(path, logger, new_dir_name);
                if ( new_path == null)

                {
                    if (dbg) logger.log("rename failed");
                    label1.setText(original_name);
                    label1.setGraphic(restored);
                    return;
                }
                path = new_path;
                label1.setText(new_dir_name);
                label1.setGraphic(restored);
                if (dbg) logger.log("rename done");

            });
        });
        return menu_item;
    }

    //**********************************************************
    private MenuItem create_copy_dir_menu_item()
    //**********************************************************
    {
        MenuItem menu_item = new MenuItem(I18n.get_I18n_string("Copy", logger));
        menu_item.setOnAction(event -> {
            if (dbg) logger.log("Copying the directory");
            Path new_path =  Files_and_Paths.ask_user_for_new_dir_name(browser.my_Stage.the_Stage,path,logger);
            if ( new_path == null)
            {
                Popups.popup_warning(browser.my_Stage.the_Stage,"copy of dir failed","names are same ?", false,logger);
                return;
            }
            Files_and_Paths.copy_dir_in_a_thread(browser.my_Stage.the_Stage, path, new_path, logger);
        });
        return menu_item;
    }


    @Override
    public Node get_Node() {
        return vbox;
    }


    @Override
    public double get_Width() {
        return icon_size;
    }

    @Override
    public void set_MinHeight(double h) {
        //vbox.setMinHeight(h);
    }

    //**********************************************************
    @Override
    public double get_Height()
    //**********************************************************
    {
        return icon_size;
    }

    //**********************************************************
    @Override
    public String get_string()
    //**********************************************************
    {
        // only used for debug logging
        return "Item_folder_with_icon for: " + path.toAbsolutePath();
    }


    //**********************************************************
    @Override
    public void set_file_count_text(String text)
    //**********************************************************
    {
        file_count_text = text;
        if ( has_images)
        {
            label1.setText(label1.getText() + "("+text+" files)");
        }
        else
        {
            label2.setText(file_count_text+"\ndisk size: "+disk_footprint_text);
        }
    }

    //**********************************************************
    @Override
    public void set_disk_foot_print_text(Sizes sizes)
    //**********************************************************
    {

        StringBuilder sb = new StringBuilder();
        sb.append(sizes.folders());
        sb.append(" folders\n");
        sb.append(sizes.files());
        sb.append(" files\n");
        sb.append(sizes.images());
        sb.append(" images\n");
        sb.append(sizes.bytes());
        sb.append(" bytes\n");

        file_count_text = sizes.files()+" files";
        disk_footprint_text = sizes.bytes()+" bytes";
        if ( !has_images)
        {
            label2.setText(sb.toString());
        }
    }

    //**********************************************************
    public static void show_how_many_files_folder(
            File_count_receiver size_info_receiver, String text, Path path,Aborter aborter,Logger logger)
    //**********************************************************
    {
        Runnable r = () -> {
            String s = String.valueOf(Files_and_Paths.get_how_many_files_deep(path, aborter,logger));
            Platform.runLater(() -> {
                size_info_receiver.set_file_count_text(s);
            });
        };
        Threads.execute(r,logger);
    }


    //**********************************************************
    public static void show_disk_foot_print_folder(
            Disk_foot_print_receiver disk_foot_print_receiver, String text, Path path,Aborter aborter,Logger logger)
    //**********************************************************
    {
        Runnable r = () -> {
            Sizes sizes = Files_and_Paths.get_sizes_on_disk_deep(path, aborter,logger);
            Platform.runLater(() -> {
                disk_foot_print_receiver.set_disk_foot_print_text(sizes);
            });
        };
        Threads.execute(r,logger);
    }

}
