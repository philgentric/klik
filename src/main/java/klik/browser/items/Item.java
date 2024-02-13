package klik.browser.items;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.actor.Job;
import klik.browser.Browser;
import klik.browser.Browser_creation_context;
import klik.browser.Drag_and_drop;
import klik.browser.System_open_actor;
import klik.browser.icons.Icon_destination;
import klik.browser.icons.Icon_factory_request;
import klik.files_and_paths.Files_and_Paths;
import klik.files_and_paths.Folder_size;
import klik.level2.deduplicate.Deduplication_engine;
import klik.look.Font_size;
import klik.look.Look_and_feel;
import klik.look.Look_and_feel_manager;
import klik.level2.metadata.Tag_stage;
import klik.look.my_i18n.I18n;
import klik.properties.Static_application_properties;
import klik.util.Logger;
import klik.util.Popups;
import klik.util.Stack_trace_getter;

import java.awt.Desktop;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

//**********************************************************
public abstract class Item implements Icon_destination
//**********************************************************
{
    protected final boolean dbg = false;
    public static final boolean layout_dbg = false;

    public final int icon_size;
    /* state machine for icons is:
    before anything: icon_fabrication_requested = false & icon_available

    when item becomes visible

    if ( !icon_available) we launch request icon factory <<< this can occur a LOT of times

    = actors are started and soon as one really starts icon_fabrication_requested == true
    and other actors will fail fast

    as soon as set_image occurs icon_available = true

    as soon as the item becomes invisible icon_available = false

     */
    public AtomicBoolean icon_fabrication_requested = new AtomicBoolean(false);
    public AtomicBoolean icon_available = new AtomicBoolean(false);
    Job icon_job; // this is needed to cancel the icon request when the item has become invisible




    protected Path path;
    protected final Browser browser;
    protected final Logger logger;
    public final Iconifiable_item_type item_type;
    public AtomicBoolean visible_in_scene = new AtomicBoolean(false);
    public final Aborter browser_aborter;

    // virtual coordinates: will change whenever the window geometry changes
    // this is the (top-left) position if the square box containing the image
    // for a landscape image we can shift up so javafx_y < screen_y_of_image
    // for a portrait image we can shift left so javafx_x < screen_x_of_image
    private double javafx_x;
    private double javafx_y;
    // this is the (top-left) position of the image
    private double screen_x_of_image = 0;
    private double screen_y_of_image = 0;

    //**********************************************************
    public Item(Browser browser,
                Path path,
                Logger logger)
    //**********************************************************
    {
        this.browser_aborter = browser.aborter;
        this.browser = browser;
        this.path = path;
        this.logger = logger;
        item_type = Iconifiable_item_type.from_extension(path);
        icon_size = Static_application_properties.get_icon_size(logger);
    }

    public final Scene getScene()
    {
        return browser.the_Scene;
    }

    protected final Logger get_logger()
    {
        return logger;
    }


    public void set_translate_X(double dx)
    {
        if (get_Node() != null) get_Node().setTranslateX(dx);
    }

    public void set_translate_Y(double dy)
    {
        if (get_Node() != null) get_Node().setTranslateY(dy);
    }

    public void set_javafx_x(double x_) { javafx_x = x_; }
    public void set_javafx_y(double y_) {javafx_y = y_;}
    public double get_javafx_x() { return javafx_x; }
    public double get_javafx_y()
    {
        return javafx_y;
    }

    public void set_screen_x_of_image(double x_) { screen_x_of_image = x_; }
    public void set_screen_y_of_image(double y_) {screen_y_of_image = y_;}
    public double get_screen_x_of_image() { return screen_x_of_image; }
    public double get_screen_y_of_image()
    {
        return screen_y_of_image;
    }

    public abstract Node get_Node();

    public abstract double get_Width();
    public abstract double get_Height();
    public abstract boolean is_trash();
    public abstract boolean is_parent();


    @Override // Icon_destination
    public boolean get_icon_fabrication_requested() {
        return icon_fabrication_requested.get();
    }

    @Override // Icon_destination
    public boolean get_icon_available() {
        return icon_available.get();
    }

    @Override // Icon_destination
    public void set_icon_fabrication_requested(boolean b) {
        icon_fabrication_requested.set(true);
    }

    @Override
    public Iconifiable_item_type get_item_type() {
        return item_type;
    }

    public abstract String get_string();

    public abstract void set_is_selected_internal();

    public abstract void set_is_unselected_internal();


    //**********************************************************
    public void request_icon_to_factory(int target_icon_size, boolean is_high_priority)
    //**********************************************************
    {

        if ( dbg) logger.log("request_icon_to_factory for:"+path);
        Icon_factory_request icon_factory_request = new Icon_factory_request(this, target_icon_size,is_high_priority, new Aborter("Icon creation for "+path,logger));

        if (dbg) logger.log("icon request : queued! ");

        Icon_destination destination = icon_factory_request.destination;
        if (destination == null) {
            logger.log(Stack_trace_getter.get_stack_trace("SHOULD NOT HAPPEN icon factory : cancel! destination==null"));
            return;
        }

        if (icon_factory_request.destination.get_icon_fabrication_requested())
        {
            logger.log("cancel icon request, another one is in flight");
            return;
        }
        icon_factory_request.destination.set_icon_fabrication_requested(true);

        icon_job = Actor_engine.run(browser.icon_factory_actor, icon_factory_request, null,logger);
    }


    //**********************************************************
    protected void cancel_icon()
    //**********************************************************
    {
        icon_available.set(false);
        icon_fabrication_requested.set(false);
        if ( icon_job!= null)
        {
            Actor_engine.cancel_one(icon_job); // will trigger the aborter and if there is an associated thread, will interrupt it
            icon_job = null;
        }
    }




    //**********************************************************
    public void give_a_menu_to_the_button(Button local_button, Label local_label)
    //**********************************************************
    {
        ContextMenu context_menu = new ContextMenu();
        Look_and_feel_manager.set_context_menu_look(context_menu);
        if (!Files.isDirectory(path))
        {
            if ( this.item_type == Iconifiable_item_type.video)
            {
                Item_image.make_menu_items_for_videos(path,browser,context_menu,dbg, browser_aborter,logger);
            }
            // is a "plain" file
            context_menu.getItems().add(create_system_open_menu_item());
            context_menu.getItems().add(create_rename_menu_item(local_button,local_label));
            context_menu.getItems().add(create_delete_menu_item());
            context_menu.getItems().add(Item.create_show_file_size_menu_item(browser, path, dbg,logger));
            context_menu.getItems().add(Item.create_edit_tag_menu_item(path, dbg,logger));
        }
        else
        {
            // is a folder
            context_menu.getItems().add(create_get_folder_size_menu_item(browser_aborter));
            if ( is_trash())
            {
                MenuItem menu_item = create_clear_trash_menu_item();
                context_menu.getItems().add(menu_item);
            }

            if(!is_trash() && !is_parent())
            {
                context_menu.getItems().add(create_browse_in_new_window_menu_item());
                context_menu.getItems().add(Item.create_edit_tag_menu_item(path, dbg, logger));
                context_menu.getItems().add(create_rename_menu_item(local_button,local_label));
                context_menu.getItems().add(create_delete_menu_item());
                context_menu.getItems().add(create_copy_dir_menu_item());

            }
        }

        local_button.setOnContextMenuRequested((ContextMenuEvent event) -> {
            if ( dbg) logger.log("show context menu of button:"+ path.toAbsolutePath());
            context_menu.show(local_button, event.getScreenX(), event.getScreenY());
        });
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


    //**********************************************************
    private MenuItem create_clear_trash_menu_item()
    //**********************************************************
    {
        MenuItem menu_item = new MenuItem(I18n.get_I18n_string("Clear_Trash_Folder",logger));
        menu_item.setOnAction(event -> {
            if (dbg) logger.log("clearing trash!");
            Files_and_Paths.clear_trash_with_warning(browser.my_Stage.the_Stage, browser_aborter,logger);
        });
        return menu_item;
    }

    //**********************************************************
    private MenuItem create_delete_menu_item()
    //**********************************************************
    {
        MenuItem menu_item = new MenuItem(I18n.get_I18n_string("Delete", logger));
        menu_item.setOnAction(event -> {
            if (dbg) logger.log("Deleting!");
            Files_and_Paths.move_to_trash(browser.my_Stage.the_Stage,path, null, browser_aborter,logger);
        });
        return menu_item;
    }



    //**********************************************************
    private MenuItem create_system_open_menu_item()
    //**********************************************************
    {
        MenuItem menu_item = new MenuItem(I18n.get_I18n_string("Open_with_system", logger));
        menu_item.setOnAction(event -> {
            if (dbg) logger.log("button in item: System Open");
            System_open_actor.open_with_system(browser,path,logger);
        });
        return menu_item;
    }


    //**********************************************************
    private MenuItem create_rename_menu_item(Button local_button_, Label local_label_)
    //**********************************************************
    {
        final Button local_button = local_button_;
        final Label local_label = local_label_;
        MenuItem menu_item = new MenuItem(I18n.get_I18n_string("Rename", logger));
        menu_item.setOnAction(event -> {
            if (dbg) logger.log("Item_button: Renaming");
            String original_name = path.getFileName().toString();
            TextField text_edit = new TextField(original_name);
            Node restored = local_button.getGraphic();
            local_button.setGraphic(text_edit);
            text_edit.setMinWidth(local_button.getWidth() * 0.9);
            text_edit.requestFocus();
            text_edit.positionCaret(original_name.length());
            text_edit.setFocusTraversable(true);
            text_edit.setOnAction(actionEvent -> {
                String new_dir_name = text_edit.getText();
                actionEvent.consume();
                if ( path.toFile().isDirectory() )
                {
                    Path new_path = Files_and_Paths.change_dir_name(path, new_dir_name, browser_aborter, logger);
                    if ( new_path == null)
                    {
                        if (dbg) logger.log("rename failed");
                        local_button.setText(original_name);
                        local_button.setGraphic(restored);
                        return;
                    }
                    path = new_path;
                    local_button.setText(new_dir_name);
                    local_button.setGraphic(restored);
                }
                else
                {
                    Path new_path = Files_and_Paths.change_file_name(path, new_dir_name, browser_aborter, logger);
                    if ( new_path == null)
                    {
                        if (dbg) logger.log("rename failed");
                        local_button.setText(original_name);
                        local_button.setGraphic(restored);
                        return;
                    }
                    path = new_path;
                    if ( local_label == null)
                    {
                        // the item is a Item_folder_with_icon
                        if (dbg) logger.log("rename done");
                        local_button.setText(new_dir_name);
                        local_button.setGraphic(restored);
                    }
                    else
                    {
                        // the item is a Item_button

                        String size = Files_and_Paths.get_1_line_string_for_byte_data_size(path.toFile().length());
                        local_button.setText(size);
                        local_label.setText(new_dir_name);
                        //Font_size.set_preferred_font_size(label,logger);
                        Font_size.apply_font_size(local_label, logger);
                        local_button.setGraphic(local_label);
                    }
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
            Files_and_Paths.copy_dir_in_a_thread(browser.my_Stage.the_Stage, path, new_path, browser_aborter, logger);
        });
        return menu_item;
    }







    //**********************************************************
    public void unset_image_is_selected()
    //**********************************************************
    {
        set_is_unselected_internal();
    }

    //**********************************************************
    public void set_is_selected()
    //**********************************************************
    {
        if (browser.selection_handler.add_to_selected_files(path)) {
            set_is_selected_internal();
            logger.log("item selected:" + path);
        }
    }

    //**********************************************************
    private void set_background(BackgroundFill background_fill)
    //**********************************************************
    {
        Node n = get_Node();
        if ( n instanceof Button)
        {
            Button button = (Button)n;
            button.setBackground(new Background(background_fill));
            Node node = button.getGraphic();
            if (node instanceof Label)
            {
                Look_and_feel_manager.set_label_look((Label) node);
            }
        }
        else if ( n instanceof FlowPane)
        {
            ((FlowPane)n).setBackground(new Background(background_fill));
        }
    }


    //**********************************************************
    void set_background_for_setOnDragEntered()
    //**********************************************************
    {
        BackgroundFill background_fill = Look_and_feel_manager.get_drag_fill();
        if (Drag_and_drop.drag_and_drop_dbg) logger.log("Item_folder_with_icon OnDragOver color = "+background_fill);
        set_background(background_fill);
    }

    //**********************************************************
    void set_background_for_setOnDragOver()
    //**********************************************************
    {
        set_background_for_setOnDragEntered();
    }

    //**********************************************************
    void set_background_for_setOnDragExited()
    //**********************************************************
    {
        Look_and_feel i = Look_and_feel_manager.get_instance();
        BackgroundFill color = i.get_background_fill();
        if (Drag_and_drop.drag_and_drop_dbg) logger.log("Item_folder_with_icon setOnDragExited color = "+color);
        set_background(color);

    }
    //**********************************************************
    public void init_drag_and_drop_sender_side()
    //**********************************************************
    {
        get_Node().setOnDragDetected(drag_event -> {
            if (dbg) logger.log("Item.init_drag_and_drop() drag detected SENDER SIDE");
            Dragboard db = get_Node().startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
/*
            if (browser.selection_handler.get_select_all_folders())
            {
                logger.log("Item.init_drag_and_drop() drag detected, adding ALL folders");
                // the browser is in select all mode so this means we dont take just this 1 folder
                List<File> tmp = browser.get_folder_list();
                browser.selection_handler.set_select_all_folders(false);
                browser.selection_handler.reset_selection();
                browser.selection_handler.add_into_selected_files(tmp);
            }
            if (browser.selection_handler.get_select_all_files())
            {
                logger.log("Item.init_drag_and_drop() drag detected, adding ALL files");
                // the browser is in select all mode so this means we dont take just this 1 file
                List<File> tmp = browser.get_file_list();
                browser.selection_handler.set_select_all_files(false);
                browser.selection_handler.reset_selection();
                browser.selection_handler.add_into_selected_files(tmp);
            }
*/
            List<File> ll = browser.selection_handler.get_selected_files();
            // if we are here it is because the user is dragging an item
            if (!ll.contains(path.toFile())) {
                ll.add(path.toFile());
            }
            // this crashes the VM !!?? content.putFiles(ll);
            StringBuilder sb = new StringBuilder();
            for (File f : ll)
            {
                sb.append("\n").append(f.getAbsolutePath());
            }
            if ( Drag_and_drop.drag_and_drop_dbg) logger.log(" selected files: " + sb);
            content.put(DataFormat.PLAIN_TEXT, sb.toString());
            db.setContent(content);
            drag_event.consume();
        });

        get_Node().setOnDragDone(drag_event -> {
            if (drag_event.getTransferMode() == TransferMode.MOVE)
            {
                if (dbg) logger.log("Item.init_drag_and_drop() SENDER SIDE: setOnDragDone for " + path.toAbsolutePath());
                /*
                DO NOT report it: it will be reported by the receiver Browser scene
                List<Old_and_new_Path> l = new ArrayList<>();
                Command_old_and_new_Path k = Command_old_and_new_Path.command_move;
                Old_and_new_Path oan = new Old_and_new_Path(null,f,k);
                oan.set_status(Status_old_and_new_Path.status_moved);
                l.add(oan);
                Change_gang.report_event(l);*/

                browser.set_status(browser.selection_handler.get_selected_files_count()+ " files have been dragged out");
                browser.selection_handler.reset_selection();
                browser.selection_handler.nothing_selected();
            }
            drag_event.consume();
        });
    }


    //**********************************************************
    protected void init_drag_and_drop_receiver_side()
    //**********************************************************
    {
        get_Node().setOnDragEntered(drag_event -> {
            if (Drag_and_drop.drag_and_drop_dbg) logger.log("OnDragEntered RECEIVER SIDE" );
            set_background_for_setOnDragEntered();
            drag_event.consume();
        });
        get_Node().setOnDragExited(drag_event -> {
            if (Drag_and_drop.drag_and_drop_dbg) logger.log("OnDragExited RECEIVER SIDE");
            set_background_for_setOnDragExited();
            drag_event.consume();
        });
        get_Node().setOnDragOver(drag_event -> {
            if (Drag_and_drop.drag_and_drop_dbg) logger.log("OnDragOver RECEIVER SIDE");
            drag_event.acceptTransferModes(TransferMode.MOVE);
            set_background_for_setOnDragOver();
            drag_event.consume();
        });
        get_Node().setOnDragDropped(drag_event -> {
            if (Drag_and_drop.drag_and_drop_dbg) logger.log("OnDragDropped RECEIVER SIDE");
            Drag_and_drop.accept_drag_dropped_as_a_move_in(
                    browser.my_Stage.the_Stage,
                    drag_event,
                    path,
                    get_Node(),
                    "Browser item",
                    is_trash(),
                    logger);
            drag_event.consume();
        });
    }


    //**********************************************************
    public void open_with_system(Logger logger)
    //**********************************************************
    {
        logger.log("open_with_system for path:" + path.toString());
        // try to open it with the system
        try
        {
            Desktop.getDesktop().open(path.toAbsolutePath().toFile());
        }
        catch (Exception e)
        {
            logger.log(Stack_trace_getter.get_stack_trace("open failed :" + e));

            if (e.toString().contains("doesn't exist."))
            {
                String error = "Your OS/GUI could not open this file";
                Popups.popup_warning(browser.my_Stage.the_Stage, "Open failed", error+", the error is:\n" + e, true,logger);
                browser.set_status(error);
            }
            else
            {
                Popups.popup_warning(browser.my_Stage.the_Stage,"Open failed", "Your OS/GUI could not open this file, the error is:\n" + e + "\nMaybe it is just not properly configured e.g. most often the file extension has to be registered?", false,logger);
            }
        }
    }

    static double xxx = 200;
    static double yyy = 200;
    //**********************************************************
    public static MenuItem create_show_file_size_menu_item(Browser b_, Path path, boolean dbg, Logger logger)
    //**********************************************************
    {
        MenuItem menu_item = new MenuItem(I18n.get_I18n_string("Show_file_size", logger));
        menu_item.setOnAction(event -> {
            if (dbg) logger.log("File size");
            String size_in_bytes = Files_and_Paths.get_1_line_string_with_size(path,logger);
            String message = I18n.get_I18n_string("File_size_for", logger) +"\n"+ path.getFileName().toString();
            //Popups.popup_warning(message, file_size, false,logger);
            Stage local_stage = new Stage();
            local_stage.setHeight(200);
            local_stage.setWidth(600);
            local_stage.setX(xxx);
            local_stage.setY(yyy);
            yyy+= 200;
            if ( yyy > 600)
            {
                yyy = 200;
                xxx += 600;
                if ( xxx > 1000) xxx = 200;
            }
            TextArea textarea1 = new TextArea(message+"\n"+size_in_bytes);
            Font_size.apply_font_size(textarea1,24,logger);
            VBox vbox = new VBox(textarea1);
            Scene scene = new Scene(vbox, Color.WHITE);
            local_stage.setTitle(path.toAbsolutePath().toString());
            local_stage.setScene(scene);
            local_stage.show();

            logger.log("size_in_bytes->"+size_in_bytes+"<-");
            b_.set_status(size_in_bytes);
        });
        return menu_item;
    }

    //**********************************************************
    public static MenuItem create_edit_tag_menu_item(Path path, boolean dbg, Logger logger)
    //**********************************************************
    {
        MenuItem menu_item = new MenuItem(I18n.get_I18n_string("Show_tag", logger));
        menu_item.setOnAction(event -> {
            if (dbg) logger.log("File tag");
            Tag_stage.open_tag_stage(path,true,logger);
        });

        return menu_item;
    }


    public Path get_item_path() {
        return path;
    }

    // path for display takes different form depending on the item type
    // it can be null, a PNG icon, or an animated gif
    abstract public Path get_path_for_display(boolean try_deep);




    //**********************************************************
    public void you_are_invisible()
    //**********************************************************
    {
        if (get_Node() == null) return;
        get_Node().setVisible(false);
        cancel_icon();
        cancel_custom();
    }
    public abstract void cancel_custom();

    //**********************************************************
    public void you_are_visible()
    //**********************************************************
    {
        you_are_visible_specific();
        get_Node().setVisible(true);
        if( has_icon()) request_icon_to_factory(get_icon_size(),get_is_high_priority());
    }


    abstract void you_are_visible_specific();
    abstract int get_icon_size();
    abstract boolean has_icon();


    public boolean get_is_high_priority(){return true;}; // ticket

}
