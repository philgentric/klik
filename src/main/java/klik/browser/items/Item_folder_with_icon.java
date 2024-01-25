package klik.browser.items;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.actor.Job;
import klik.browser.icons.animated_gifs.Animated_gif_from_folder;
import klik.browser.*;
import klik.browser.icons.Icon_destination;
import klik.browser.icons.Icon_factory_actor;
import klik.browser.icons.Icon_factory_request;
import klik.browser.icons.Icon_manager;
import klik.files_and_paths.Folder_size;
import klik.level2.deduplicate.Deduplication_engine;
import klik.files_and_paths.Files_and_Paths;
import klik.files_and_paths.Guess_file_type;
import klik.files_and_paths.Sizes;
import klik.images.decoding.Fast_rotation_from_exif_metadata_extractor;
import klik.look.Look_and_feel;
import klik.look.Look_and_feel_manager;
import klik.look.my_i18n.I18n;
import klik.properties.Static_application_properties;
import klik.util.Logger;
import klik.util.Popups;
import klik.util.Stack_trace_getter;

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
    //public final boolean has_images;
    public final String text;

    double estimated_text_label_height;

    // these 2 are for the image representing the folder CONTENT
    // i.e. either nothing (no images in folder)
    // or the first image in the folder
    // or an animated gif with a sample of the images in the folder
    ImageView the_image_view;
    FlowPane the_image_pane;
//    Label label_for_sizes;
    Job job;
    Button the_button;

    private boolean ignore_next_mouse_clicked = false;
    String file_count_text = ".......";
    String disk_footprint_text = ".......";
    boolean show_disk_foot_print = false;//Static_application_properties.
    private final int folder_icon_size;
    private final int column_width; // as set by the icon manager
    //**********************************************************
    public Item_folder_with_icon(
            Browser browser,
            Path path_,
            String text_,
            boolean is_trash_, boolean is_parent_,
            int column_width_,
            Logger logger)
    //**********************************************************
    {
        super(browser, path_, logger);
        column_width = column_width_;
        folder_icon_size = Static_application_properties.get_folder_icon_size(logger);
        // launch content icon fabrication:
        Icon_factory_request ifr = new Icon_factory_request(this, folder_icon_size);
        job = Icon_factory_actor.get_icon_factory(this.browser.aborter, this.browser.icon_manager.paths_manager.aspect_ratio_cache, this.browser.my_Stage.the_Stage, logger).make_icon(ifr);

        text = text_;
        is_trash = is_trash_;
        is_parent = is_parent_;
        //has_images = has_images_;

        double font_size = Static_application_properties.get_font_size(logger);
        estimated_text_label_height = klik.look.Look_and_feel.MAGIC_HEIGHT_FACTOR*font_size;

        double icon_height = Look_and_feel.MAGIC_HEIGHT_FACTOR * font_size;
        Image folder_icon = Look_and_feel_manager.get_folder_icon(icon_height);
        if (folder_icon == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("WARNING: cannot get default icon for directory !?"));
        }

        the_button = new Button(text);
        the_button.setTextOverrun(OverrunStyle.ELLIPSIS);
        the_image_pane = new FlowPane();//)StackPane();
        the_image_pane.setAlignment(Pos.BOTTOM_LEFT);
        the_image_pane.setMinWidth(folder_icon_size);
        the_image_pane.setMaxWidth(folder_icon_size);
        the_image_pane.setMinHeight(folder_icon_size);
        the_image_pane.setMaxHeight(folder_icon_size);
        the_button.setGraphic(the_image_pane);
        the_button.setContentDisplay(ContentDisplay.BOTTOM);

        Look_and_feel_manager.set_button_look(the_button,true);
        the_button.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                Browser_creation_context.replace_different_folder(path,browser, null, logger);
            }
        });

        resize_the_box(the_button);

        init_drag_and_drop_receiver_side();
        init_drag_and_drop_sender_side();
        give_a_menu_to_the_button();
        if ( show_disk_foot_print)
        {
            launch_disk_foot_print_thread(this, text, path, aborter, logger);
        }
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
    private void resize_the_box(Button button)
    //**********************************************************
    {
        if ( Static_application_properties.get_single_column(logger))
        {
            button.setPrefWidth(browser.my_Stage.the_Stage.getWidth()-Icon_manager.RIGHT_SIDE_SINGLE_COLUMN_MARGIN);
            button.setMinWidth(browser.my_Stage.the_Stage.getWidth()-Icon_manager.RIGHT_SIDE_SINGLE_COLUMN_MARGIN);
        }
        else
        {
            button.setPrefWidth(column_width);
            button.setMinWidth(column_width);
            //logger.log("vbox height=folder_icon_size="+folder_icon_size +"+ estimated_text_label_height=" +estimated_text_label_height+"="+(folder_icon_size + estimated_text_label_height));
            double h = folder_icon_size+ estimated_text_label_height;
            button.setPrefHeight(h);
            button.setMinHeight(h);
            button.setMaxHeight(h);
        }
    }


    //**********************************************************
    @Override
    public void set_Image(Image_and_rotation image_and_rotation, boolean real2)
    //**********************************************************
    {
        logger.log("set_Image ");

        if ( image_and_rotation.image() == null)
        {
            the_image_view = null;
            logger.log(Stack_trace_getter.get_stack_trace("image==null for "+path));
            return;
        }
        if ( the_image_view == null)
        {
            the_image_view = new ImageView();
        }
        the_image_pane.getChildren().add(the_image_view);

        //logger.log(Stack_trace_getter.get_stack_trace("item_folder_with_icon setting icon for "+path+ " image width = "+image.getWidth()));
        the_image_view.setImage(image_and_rotation.image());
        the_image_view.setPreserveRatio(true);

        // normally we already have the rotation
        Double local_rot = image_and_rotation.rotation();
        if (local_rot == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("SHOULD NOT HAPPEN"));
            Path local = get_path_for_display(false);
            local_rot = Fast_rotation_from_exif_metadata_extractor.get_rotation(local, dbg, aborter, logger);
            image_and_rotation = new Image_and_rotation(image_and_rotation.image(),local_rot);
        }
/*
        if ((local_rot == 90) || (local_rot == 270))
        {
            if ( image_and_rotation.image().getHeight() < image_and_rotation.image().getWidth())
            {
                logger.log("option1 "+path);
                the_image_view.setFitWidth(folder_icon_size-10);
                the_image_view.setFitHeight(-1);
            }
            else
            {
                logger.log("option2 "+path);
                the_image_view.setFitWidth(-1);
                the_image_view.setFitHeight(folder_icon_size-10);
            }
        }
        else
        {
            logger.log("option3 "+path);
            the_image_view.setFitWidth(image_and_rotation.image().getWidth());
            the_image_view.setFitHeight(image_and_rotation.image().getHeight());
        }

 */
        the_image_pane.setRotate(image_and_rotation.rotation());


/*
        if ( folder_icon_size > column_width)
        {
            double w = folder_icon_size-10;
            if ( image_and_rotation.image().getWidth() > image_and_rotation.image().getHeight())
            {
                if ( (image_and_rotation.rotation() ==0) || (image_and_rotation.rotation()== 180))
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

 */
        resize_the_box(the_button);
    }
    //**********************************************************
    @Override
    public void receive_icon(Image_and_rotation image_and_rotation)
    //**********************************************************
    {
        Platform.runLater(() -> {
            set_Image(image_and_rotation,true);
        });
    }
    //**********************************************************
    @Override // Icon_destination
    public Path get_path_for_display_icon_destination()
    //**********************************************************
    {
        return get_path_for_display(true);
    }

    // this call is intended only from a working thread typically: in the icon factory 
    //**********************************************************
    @Override // Item
    public Path get_path_for_display(boolean try_deep)
    //**********************************************************
    {
        //if ( try_deep) logger.log(Stack_trace_getter.get_stack_trace("get_path_for_display with try deep "));
        if (is_trash) return null;
        if (is_parent) return null;
        // for a file the displayed icon is built from THE FILE ITSELF, if supported:
        if ( !path.toFile().isDirectory())
        {
            logger.log(Stack_trace_getter.get_stack_trace("SHOULD NOT HAPPEN"));
            return path;
        }

        if ( !try_deep)
        {
            logger.log(Stack_trace_getter.get_stack_trace("SHOULD NOT HAPPEN"));
            return null;
        }

        // for a folder we have 3 ways to provide an icon
        // 1) an image is taken from the folder and used as icon
        // 2) multiple images are taken from the folder to form an animated gif icon
        // 3) if there are no documents that we know how to iconify, use defaalt
        // this is "TRY DEEP", it takes time so it should be called ONLY from the icon factory
        // i.e. in a separste thread

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
            if (!Guess_file_type.is_file_an_image(f)) continue; // ignore non images
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
        if ( images_in_folder.isEmpty()) {
            for (File folder : files) {
                if (folder.isDirectory()) {
                    File[] files2 = folder.listFiles();
                    if (files2 == null) return null;
                    Arrays.sort(files2);
                    for (File f2 : files2) {
                        if (f2.isDirectory()) continue; // ignore folders
                        if (!Guess_file_type.is_file_an_image(f2)) continue; // ignore non images
                        if (Guess_file_type.is_this_path_a_gif(f2.toPath())) {
                            if (Guess_file_type.is_this_path_a_animated_gif(f2.toPath(), aborter, logger)) {
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
        if ( returned != null)
        {
            logger.log("animated gif made");
            return returned;
        }
        if (images_in_folder.isEmpty())
        {
            logger.log("no images");
            return null;
        }
        else
        {
            logger.log("picking first image");
            return images_in_folder.get(0).toPath();
        }

    }
    //**********************************************************
    @Override // Item
    public void set_is_unselected_internal()
    {
        Look_and_feel_manager.give_button_a_file_style(the_button);
    }

    //**********************************************************
    @Override // Item
    public void set_is_selected_internal()
    //**********************************************************
    {
        Look_and_feel_manager.give_button_a_selected_file_style(the_button);
    }

    //**********************************************************
    private void init_drag_and_drop_receiver_side()
    //**********************************************************
    {
        the_button.setOnDragEntered(drag_event -> {
            if (Drag_and_drop.drag_and_drop_dbg) logger.log("OnDragEntered for button RECEIVER SIDE" );
            set_background_for_setOnDragEntered();
            drag_event.consume();
        });
        the_button.setOnDragExited(drag_event -> {
            if (Drag_and_drop.drag_and_drop_dbg) logger.log("Item_folder_with_icon::OnDragExited  RECEIVER SIDE");
            set_background_for_setOnDragExited();
            ignore_next_mouse_clicked = true;
            drag_event.consume();
        });
        the_button.setOnDragOver(drag_event -> {
            if (Drag_and_drop.drag_and_drop_dbg) logger.log("Item_folder_with_icon OnDragOver  RECEIVER SIDE");
            drag_event.acceptTransferModes(TransferMode.MOVE);
            set_background_for_setOnDragOver();
            drag_event.consume();
        });
        the_button.setOnDragDropped(drag_event -> {
            if (Drag_and_drop.drag_and_drop_dbg) logger.log("OnDragDropped for button  RECEIVER SIDE");
            Drag_and_drop.accept_drag_dropped_as_a_move_in(
                    browser.my_Stage.the_Stage,
                    drag_event,
                    path,
                    the_button,
                    "button",
                    logger);
            drag_event.consume();
        });
    }


    //**********************************************************
    public void give_a_menu_to_the_button()
    //**********************************************************
    {
        ContextMenu context_menu = new ContextMenu();
        Look_and_feel_manager.set_context_menu_look(context_menu);

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




        the_button.setOnContextMenuRequested(event -> {
            if( dbg) logger.log("show context menu of button:"+ path.toAbsolutePath());
            context_menu.show(the_button, event.getScreenX(), event.getScreenY());
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
        size.setOnAction(event -> Folder_size.get_folder_size(path,browser,aborter, logger));
        return size;
    }

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
        MenuItem menu_item = new MenuItem(I18n.get_I18n_string("Rename", logger)+" "+path.getFileName());
        menu_item.setOnAction(event -> {

            if (dbg) logger.log("Item_folder_with_icon: Renaming");
            String original_name = path.getFileName().toString();
            TextField text_edit = new TextField(original_name);
            Node restored = the_button.getGraphic();
            the_button.setGraphic(text_edit);
            text_edit.setMinWidth(the_button.getWidth() * 0.9);
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
                    the_button.setText(original_name);
                    the_button.setGraphic(restored);
                    return;
                }
                path = new_path;
                the_button.setText(new_dir_name);
                the_button.setGraphic(restored);
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
        return the_button;
    }



    //**********************************************************
    @Override
    public double get_Width()
    //**********************************************************
    {
        double returned = folder_icon_size;
        if ( returned < column_width) returned= column_width;
        return returned;
    }

    //**********************************************************
    @Override
    public double get_Height()
    //**********************************************************
    {
        return folder_icon_size + estimated_text_label_height;
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
        /*if ( has_images)
        {
            label1.setText(label1.getText() + "("+text+" files)");
        }
        else*/
        {
            //label_for_sizes.setText(file_count_text+"\ndisk size: "+disk_footprint_text);
        }
    }

    //**********************************************************
    @Override
    public void set_disk_foot_print_text(Sizes sizes)
    //**********************************************************
    {
        file_count_text = sizes.files()+" files";
        disk_footprint_text = sizes.bytes()+" bytes";
        //if ( !has_images)
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
            //label_for_sizes.setText(sb.toString());
        }
    }



    //**********************************************************
    public static void launch_disk_foot_print_thread(
            Disk_foot_print_receiver disk_foot_print_receiver, String text, Path path,Aborter aborter,Logger logger)
    //**********************************************************
    {
        Runnable r = () -> {
            Sizes sizes = Files_and_Paths.get_sizes_on_disk_deep(path, aborter,logger);
            Platform.runLater(() -> {
                disk_foot_print_receiver.set_disk_foot_print_text(sizes);
            });
        };
        Actor_engine.execute(r,logger);
    }



}
