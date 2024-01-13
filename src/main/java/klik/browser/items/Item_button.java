package klik.browser.items;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.text.TextAlignment;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.actor.Job;
import klik.animated_gifs_from_videos.Animated_gif_from_folder;
import klik.browser.*;
import klik.browser.icons.Icon_destination;
import klik.browser.icons.Icon_factory_actor;
import klik.browser.icons.Icon_factory_request;
import klik.browser.icons.Icon_manager;
import klik.files_and_paths.Folder_size;
import klik.level2.deduplicate.Deduplication_engine;
import klik.files_and_paths.Files_and_Paths;
import klik.files_and_paths.Guess_file_type;
import klik.look.Font_size;
import klik.look.Look_and_feel_manager;
import klik.music.Audio_player;
import klik.look.my_i18n.I18n;
import klik.properties.Static_application_properties;
import klik.util.Logger;
import klik.util.Popups;
import klik.util.Threads;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


//**********************************************************
public class Item_button extends Item implements Icon_destination
//**********************************************************
{
    public static final boolean dbg = false;
    public Button button;
    public Label label;
    public final boolean is_dir;
    public final boolean is_trash;
    public final boolean is_parent;
    public final String text;
    private boolean ignore_next_mouse_clicked = false;
    private Job job;

    private static DateTimeFormatter date_time_formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    //**********************************************************
    public Item_button(
            Browser browser,
            Path path_,
            String text_,
            double height,
            boolean is_trash_, boolean is_parent_,
            Logger logger)
    //**********************************************************
    {
        super(browser, path_, logger);
        text = text_;
        is_trash = is_trash_;
        is_parent = is_parent_;
        if (path == null) {
            is_dir = false;
            button = new Button("----------");
            button.setPrefWidth(Control.USE_COMPUTED_SIZE);
            button.setTextOverrun(OverrunStyle.ELLIPSIS);
            button.setGraphicTextGap(20);
            Look_and_feel_manager.set_button_look(button);
            return;
        }

        double button_width = Static_application_properties.get_column_width(logger);
        if ( button_width < Icon_manager.MIN_COLUMN_WIDTH) button_width = Icon_manager.MIN_COLUMN_WIDTH;

        if (Files.isDirectory(path))
        {
            is_dir = true;
            button_for_a_directory(text, button_width, height);
            if ( Static_application_properties.get_show_icons_for_folders(logger))
            {
                if (dbg) logger.log("setting image tooltip");
                //Icon_factory_request ifr = new Icon_factory_request(this, icon_size);
                //Icon_factory.get_icon_factory(logger).make_icon(ifr);
                Icon_factory_request ifr = new Icon_factory_request(this, icon_size);
                job = Icon_factory_actor.get_icon_factory(browser.aborter,browser.icon_manager.paths_manager.aspect_ratio_cache, this.browser.my_Stage.the_Stage, logger).make_icon(ifr);
            }
            //button.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID,new CornerRadii(5),new BorderWidths(1))));
        }
        else
        {
            is_dir = false;
            button_for_a_non_image_file( text,button_width);
            //Look_and_feel.set_thin_border(button);
            //button.setBorder(new Border(new BorderStroke(Color.RED, BorderStrokeStyle.SOLID,new CornerRadii(5),new BorderWidths(2))));
            //button.setPrefHeight(FILE_BUTTON_HEIGHT);
        }
        Look_and_feel_manager.set_button_look(button);

        button.setViewOrder(1);

        button.setManaged(true); // means the parent tells the button its layout
        //button.setManaged(false); // does not work
        button.setMnemonicParsing(false);// avoid suppression of first underscore in names
        button.setTextOverrun(OverrunStyle.ELLIPSIS);
/*
        boolean wide = false;
        if ( Static_application_properties.get_single_column(logger))
        {
            wide = true;
        }
        if ( path.toFile().isFile())
        {
            wide = true;
        }
        if (wide)
        {
            //logger.log("is wide:"+path.toAbsolutePath());
            // leave some room for the scrollbar, does not work?
            button.setPrefWidth(browser.my_Stage.the_Stage.getWidth()-50);
            button.setMinWidth(browser.my_Stage.the_Stage.getWidth()-50);
        }
        else
        {
            button.setPrefWidth(button_width);
            button.setMinWidth(button_width);
        }
*/
        init_drag_and_drop();
    }

    ImageView the_tooltip_image_view;

    @Override
    public void set_Image(Image i, boolean real)
    //**********************************************************
    {

        if ( the_tooltip_image_view == null) the_tooltip_image_view = new ImageView();
        //logger.log(Stack_trace_getter.get_stack_trace("item non image set tooltip icon for "+path+ " image = "+i.getWidth()));
        the_tooltip_image_view.setImage(i);
    }
    //**********************************************************
    @Override
    public void receive_icon(Image_and_rotation image_and_rotation)
    //**********************************************************
    {
        Tooltip tooltip =new Tooltip();
        button.setTooltip(tooltip);
        if ( the_tooltip_image_view == null) the_tooltip_image_view = new ImageView();
        tooltip.setGraphic(the_tooltip_image_view);
        rotation = image_and_rotation.rotation();
        set_Image(image_and_rotation.image(),true);
    }


    //**********************************************************
    public Path get_true_path()
    //**********************************************************
    {
        return path;
    }

    // this call is intended only from a working thread
    // in the icon factory as
    //**********************************************************
    @Override
    public Path get_path_for_display()
    //**********************************************************
    {
        if (is_trash) return null;
        if (is_parent) return null;
        // for a file the displayed icon is built from the file itself, if supported:
        if ( !path.toFile().isDirectory()) return path;

        // for a folder we have 2 ways to provide an icon
        // 1) an image is taken from the folder and used as icon
        // 2) multiple images are taken from the folder to form an animated gif icon

        // try to find an icon for the folder
        return get_an_image_down_in_the_tree_files(path);
        /*
        no recursive madness please!
        if ( returned != null) return returned;
        // ok, so we did not find an image file in the folder
        // let us go down sub directories (if any)
        return get_an_image_down_in_the_tree_folders(path);
        */

    }

    //**********************************************************
    @Override
    public void cancel_custom()
    //**********************************************************
    {
        Actor_engine.cancel_one(job);
        job = null;
    }

    boolean make_animated_gif = true;
    //**********************************************************
    Path get_an_image_down_in_the_tree_files(Path local_path)
    //**********************************************************
    {
        if ( Files.isSymbolicLink(local_path)) return null;
        File dir = local_path.toFile();
        File[] files = dir.listFiles();
        if ( files == null)
        {
            if ( dbg) logger.log("WARNING: dir is access denied: "+local_path);
            return null;
        }
        if ( files.length == 0)
        {
            if ( dbg) logger.log("dir is empty: "+local_path);
            return null;
        }
        Arrays.sort(files);
        List<File> images_in_folder = null;
        if( make_animated_gif)
        {
            images_in_folder = new ArrayList<>();
        }
        for ( File f : files)
        {
            if (f.isDirectory()) continue; // ignore folders
            if (!Guess_file_type.is_file_an_image(f)) continue; // ignore non images
            if( make_animated_gif)
            {
                Objects.requireNonNull(images_in_folder).add(f);
            }
            else
            {
                return f.toPath();
            }
        }
        if( make_animated_gif)
        {
            if ( Objects.requireNonNull(images_in_folder).isEmpty()) return null;

            Path returned = Animated_gif_from_folder.make_animated_gif_from_all_images_in_folder(browser.my_Stage.the_Stage, local_path,  images_in_folder,  logger);
            if ( returned == null)
            {
                if (!images_in_folder.isEmpty()) return images_in_folder.get(0).toPath();
            }
            else
            {
                return returned;
            }
        }

        return null; // no image found
    }

    /*
    //**********************************************************
    Path get_an_image_down_in_the_tree_folders(Path local_path)
    //**********************************************************
    {
        File dir = local_path.toFile();
        File files[] = dir.listFiles();
        Arrays.sort(files);
        if ( files == null)
        {
            if (dbg) logger.log("WARNING dir is access denied: "+local_path);
            return null;
        }
        if ( files.length == 0)
        {
            if ( dbg) logger.log("dir is empty: "+local_path);
            return null;
        }
        for ( File f : files)
        {
            if (f.isDirectory())
            {
                Path p = get_an_image_down_in_the_tree_files(f.toPath());
                if ( p != null) return p;
            }
        }
        return null; // no image found
    }
*/

    //**********************************************************
    @Override // Item
    public void set_is_unselected_internal()
    {
        Look_and_feel_manager.give_button_a_file_style(button);
    }


    //**********************************************************
    @Override // Item
    public void set_is_selected_internal()
    //**********************************************************
    {
        Look_and_feel_manager.give_button_a_selected_file_style(button,label);
    }


    public Button get_button(){ return button;}

    //**********************************************************
    private void button_for_a_non_image_file(String text, double width)
    //**********************************************************
    {


        if ( Static_application_properties.get_single_column(logger))
        {
            /*double space_lenght_d = Look_and_feel_manager.get_look_and_feel_instance(logger).estimate_text_width(" ");// text.length();
            double text_lenght_d = Look_and_feel_manager.get_look_and_feel_instance(logger).estimate_text_width(text);// text.length();
            int text_lenght = (int) (text_lenght_d/space_lenght_d);
            for(int i = 0 ; i < 200-text_lenght; i++)
            {
                sb.append(" ");
            }*/
            StringBuilder sb = new StringBuilder();
            try {
                FileTime x = Files.readAttributes(path, BasicFileAttributes.class).creationTime();
                LocalDateTime ldt = x.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                sb.append(ldt.format(date_time_formatter));
                sb.append("                 ");
                sb.append(Files_and_Paths.get_1_line_string_for_byte_data_size(path.toFile().length()));
                sb.append("                 ");
            } catch (IOException e) {
                logger.log_exception("",e);
            }
            String size_string = sb.toString();
            label = new Label(size_string);
            Font_size.set_preferred_font_size(label,logger);
            //button = new Button(size_string,label);
            // label is the button graphic, so it is displayed ON THE LEFT of the text!
            button = new Button(text,label);
        }
        else
        {
            button = new Button(text);
        }

        button.setMinWidth(width);
        button.setPrefWidth(width);
        Font_size.set_preferred_font_size(button,logger);

        Look_and_feel_manager.give_button_a_file_style(button);
        button.setTextAlignment(TextAlignment.RIGHT);

        button.setOnAction(event -> {



            if ( Guess_file_type.is_this_path_a_playlist(path))
            {
                logger.log("opening audio playlist: " + path.toAbsolutePath());
                Audio_player.play_playlist(path.toFile(),logger);
                return;

            }

            if ( Guess_file_type.is_this_path_a_music(path))
            {
                if ( !Guess_file_type.is_this_a_video_or_audio_file(browser.my_Stage.the_Stage,path,logger))
                {
                    logger.log("opening audio file: " + path.toAbsolutePath());
                    Audio_player.play_song(path.toFile(),logger);
                    return;
                }
            }
            logger.log("asking the system to open: " + path.toAbsolutePath());

            System_open_actor.open_with_system(browser,path,logger);

        });

        give_a_menu_to_the_button();
    }

    //**********************************************************
    public void button_for_a_directory(String text, double width, double height)
    //**********************************************************
    {
        String text2 = text;
        //if ( Static_application_properties.get_show_folder_size(logger)) {    text2 += "..."; // room reservation since the size will be added later}
        if ( path != null)
        {
            if (Files.isSymbolicLink(path)) {
                text2 += " **Symbolic link** ";
            }
        }
        button = new Button(text2);
        button.setMnemonicParsing(false);// avoid suppression of first underscore in names
        Look_and_feel_manager.set_button_look_as_folder(button, height);
        button.setTextAlignment(TextAlignment.RIGHT);
        //double computed_text_width = icons_width + estimate_text_width(text2);

        if (path == null)
        {
            // protect crash when going up: root has no parent
            return;
        }

        button.setOnAction(event -> {

            if ( ignore_next_mouse_clicked)
            {
                //logger.log("button action IGNORED! due to ignore_next_mouse_clicked="+ignore_next_mouse_clicked);
                ignore_next_mouse_clicked = false;
                return;
            }


            Path scroll_to = null;
            if (is_parent)
            {
                // special case when going back with the "Parent" button
                scroll_to = browser.top_left_in_parent;
                //logger.log("\n\n\nItem_non_image is parent, scroll_to=browser.top_left_in_parent = "+scroll_to);
            }
            else
            {
                //logger.log("Item_button is NOT parent");
            }
            // if the button represents a folder, clicking on it "opens" that folder
            // = we create a NEW browser, as a replacement
            Browser_creation_context.replace_different_folder(path,browser,scroll_to,logger);

        });

        make_button_drop_receiver_capable();
        give_a_menu_to_the_button();

        //if ( Static_application_properties.get_show_folder_size(logger)) show_how_many_files_deep_folder(button,text,path,aborter,logger);

    }


    //**********************************************************
    private void make_button_drop_receiver_capable()
    //**********************************************************
    {
        button.setOnDragEntered(drag_event -> {
            if (Drag_and_drop.drag_and_drop_dbg) logger.log("OnDragEntered for button");

            set_background_for_setOnDragEntered();
            /*
            if (Files.isDirectory(path))
            {
                logger.log("OnDragEntered for button, path is a dir" + drag_event);
                set_drag_over_background();
                //Objects.requireNonNull(Look_and_feel_manager.get_instance()).set_dragged_over_directory_style(button);
            }
            */

            /*
            this never happens: file-buttons do not receive drop!

            else
            {
                logger.log("OnDragEntered for button, path is a dir" + event);
                Look_and_feel_manager.get_instance().set_hovered_file_style(button);
            }
            */

            drag_event.consume();
        });

        button.setOnDragExited(drag_event -> {
            if (Drag_and_drop.drag_and_drop_dbg) logger.log("ItemButton OnDragExited for button");

            set_background_for_setOnDragExited();

            /* mouse moved away, remove the graphical cues */

            /*Look_and_feel i = Look_and_feel_manager.get_instance();
            Paint color = i.get_background_color();
            if (Drag_and_drop.dbg_drag_and_drop) logger.log("Item_button setOnDragExited color = "+color);
            button.setBackground(new Background(new BackgroundFill(color, CornerRadii.EMPTY, Insets.EMPTY)));
            */
            /*
            if (Files.isDirectory(path))
            {
                Look_and_feel_manager.give_button_a_directory_style(button);
            }
            else
            {
                Look_and_feel_manager.give_button_a_file_style(button);
            }*/
            ignore_next_mouse_clicked = true;
            drag_event.consume();
        });


        button.setOnDragOver(drag_event -> {
            if (Drag_and_drop.drag_and_drop_dbg) logger.log("OnDragOver for button: "
                    +path.toAbsolutePath());
            drag_event.acceptTransferModes(TransferMode.MOVE);

            set_background_for_setOnDragOver();

            //set_normal_background();

            //button.getStylesheets().add(browser.browser_ui.style_sheet_url_string);
            //button.getStyleClass().add(Browser_UI.LOOK_AND_FEEL_MENU_BUTTONS);
            drag_event.consume();
        });

        button.setOnDragDropped(drag_event -> {
            if (Drag_and_drop.drag_and_drop_dbg) logger.log("OnDragDropped for button !!" + drag_event);
            Drag_and_drop.accept_drag_dropped_as_a_move_in(
                    browser.my_Stage.the_Stage,
                    drag_event,
                    path,
                    button,
                    "button",
                    logger);
            drag_event.consume();
        });
        button.setOnDragDetected(drag_event -> {
            if (Drag_and_drop.drag_and_drop_dbg) logger.log("OnDragDetected for button !!" + drag_event);

            Dragboard db = button.startDragAndDrop(TransferMode.MOVE);

            ClipboardContent content = new ClipboardContent();
            List<File> l = new ArrayList<>();
            l.add(path.toFile());
            content.putFiles(l);
            db.setContent(content);
            drag_event.consume();
        });

        button.setOnDragDone(drag_event -> {
            if (dbg) if (drag_event.getTransferMode() == TransferMode.MOVE)
            {
                logger.log("OnDragDone for button !!" + drag_event);
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
            if ( this.item_type == Iconifiable_item_type.video)
            {
                Item_image.make_menu_items_for_videos(path,browser,context_menu,dbg,aborter,logger);
            }

            // is a "plain" file
            context_menu.getItems().add(create_system_open_menu_item());
            context_menu.getItems().add(create_rename_dir_menu_item());
            context_menu.getItems().add(create_delete_dir_menu_item());

            context_menu.getItems().add(Item.create_show_file_size_menu_item(browser, path, dbg,logger));
            context_menu.getItems().add(Item.create_edit_tag_menu_item(path, dbg,logger));
        }
        else
        {
            // is a folder
            context_menu.getItems().add(create_get_folder_size_menu_item(aborter));
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
                if ( Static_application_properties.get_level2(logger))
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




        button.setOnContextMenuRequested((ContextMenuEvent event) -> {
            if ( dbg) logger.log("show context menu of button:"+ path.toAbsolutePath());
            context_menu.show(button, event.getScreenX(), event.getScreenY());
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
    private MenuItem create_get_folder_size_menu_item(Aborter aborter)
    //**********************************************************
    {
        MenuItem size = new MenuItem(I18n.get_I18n_string("Get_folder_size",logger));
        size.setOnAction(event -> Folder_size.get_folder_size(path,browser,aborter, logger));
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
            Files_and_Paths.clear_trash_with_warning(browser.my_Stage.the_Stage,aborter,logger);
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
            Files_and_Paths.move_to_trash(browser.my_Stage.the_Stage,path, null, aborter,logger);
        });
        return menu_item;
    }


    //**********************************************************
    public static void show_how_many_files_deep_folder(Button button, String text, Path path, Aborter aborter, Logger logger)
    //**********************************************************
    {
        Runnable r = () -> {
            String s = text;
            s += " (" + Files_and_Paths.get_how_many_files_deep(path, aborter, logger) + " files)";

            String finalS = s;
            Platform.runLater(() -> {
                button.setText(finalS);
                //browser.scene_geometry_changed("number of files in button", true);
            });
        };
        Threads.execute(r,logger);
    }


    //**********************************************************
    private MenuItem create_system_open_menu_item()
    //**********************************************************
    {
        MenuItem menu_item = new MenuItem(I18n.get_I18n_string("Open_with_system", logger));
        menu_item.setOnAction(event -> {
            if (dbg) logger.log("Item_button: System Open");
            System_open_actor.open_with_system(browser,path,logger);
        });
        return menu_item;
    }

    //**********************************************************
    private MenuItem create_rename_dir_menu_item()
    //**********************************************************
    {
        MenuItem menu_item = new MenuItem(I18n.get_I18n_string("Rename", logger));
        menu_item.setOnAction(event -> {
            if (dbg) logger.log("Item_button: Renaming");
            String original_name = path.getFileName().toString();
            TextField text_edit = new TextField(original_name);
            Node restored = button.getGraphic();
            button.setGraphic(text_edit);
            text_edit.setMinWidth(button.getWidth() * 0.9);
            text_edit.requestFocus();
            text_edit.positionCaret(original_name.length());
            text_edit.setFocusTraversable(true);
            text_edit.setOnAction(actionEvent -> {
                String new_dir_name = text_edit.getText();
                actionEvent.consume();
                if ( path.toFile().isDirectory() )
                {
                    Path new_path = Files_and_Paths.change_dir_name(path, logger, new_dir_name);
                    if ( new_path == null)
                    {
                        if (dbg) logger.log("rename failed");
                        button.setText(original_name);
                        button.setGraphic(restored);
                        return;
                    }
                    path = new_path;
                    button.setText(new_dir_name);
                    button.setGraphic(restored);
                }
                else
                {
                    Path new_path = Files_and_Paths.change_file_name(path, logger, new_dir_name);
                    if ( new_path == null)
                    {
                        if (dbg) logger.log("rename failed");
                        button.setText(original_name);
                        button.setGraphic(restored);
                        return;
                    }
                    path = new_path;
                    String size = Files_and_Paths.get_1_line_string_for_byte_data_size(path.toFile().length());
                    button.setText(size);
                    label = new Label(new_dir_name);
                    Font_size.set_preferred_font_size(label,logger);
                    button.setGraphic(label);
                }

                if (dbg) logger.log("rename done");
                // button.setOnAction(the_button_event_handler);
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
        return button;
    }

    @Override
    public double get_Width() {
        return button.getWidth();
    }

    @Override
    public void set_MinHeight(double h) {
        button.setMinHeight(h);
    }

    //**********************************************************
    @Override
    public double get_Height()
    //**********************************************************
    {
        if ( button.getHeight() == 0)
        {
            // for some reason until some scroll is applied the height info is 0 ???
            //logger.log("implausible button.getHeight() == 0");
            return 40;
        }
        return button.getHeight();
    }



    //**********************************************************
    @Override
    public String get_string()
    //**********************************************************
    {
        if (is_dir) return "is dir: " + path.toAbsolutePath();
        return "is file: " + path.toAbsolutePath();
    }

}
