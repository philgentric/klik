// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ../../util/execute/System_open_actor.java
//SOURCES ../icons/*.java
//SOURCES ../../util/files_and_paths/Folder_size_stage.java
//SOURCES ../../experimental/metadata/Tag_stage.java
//SOURCES ./My_color.java
/*
//SOURCES ../icons/Icon_destination.java
//SOURCES ../icons/Icon_factory_request.java
//SOURCES ../../audio/Audio_info_frame.java
 */

package klikr.browser_core.items;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.stage.Window;
import klikr.Window_builder;
import klikr.Window_type;
import klikr.browser_core.Image_and_properties;
import klikr.browser_core.virtual_landscape.Scroll_position_cache;
import klikr.javalin.monaco.Javalin_monaco;
import klikr.path_lists.Path_list_provider_for_playlist;
import klikr.settings.boolean_features.Feature;
import klikr.settings.boolean_features.Feature_cache;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.execute.actor.Job;
import klikr.experimental.audio.Audio_info_frame;
import klikr.experimental.audio.Ffmpeg_metadata_editor;
import klikr.path_lists.Path_list_provider_for_file_system;
import klikr.browser_core.icons.Icon_destination;
import klikr.browser_core.icons.Icon_factory_actor;
import klikr.browser_core.icons.Icon_factory_request;
import klikr.path_lists.Path_list_provider;
import klikr.browser_core.virtual_landscape.Selection_handler;
import klikr.images.Exif_stage;
import klikr.look.Font_size;
import klikr.look.Look_and_feel_manager;
import klikr.look.my_i18n.My_I18n;
import klikr.util.execute.System_open_actor;
import klikr.util.files_and_paths.Moving_files;
import klikr.util.image.Full_image_from_disk;
import klikr.util.log.Stack_trace_getter;
import klikr.util.ui.Folder_size_stage;
import klikr.util.files_and_paths.Guess_file_type;
import klikr.util.files_and_paths.Static_files_and_paths_utilities;
import klikr.util.log.Logger;
import klikr.util.ui.Menu_items;
import klikr.util.ui.Popups;
import klikr.util.ui.Text_frame;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

//**********************************************************
public abstract class Item implements Icon_destination
//**********************************************************
{
    protected static final boolean dbg = false;
    public static final boolean layout_dbg = false;
    public AtomicBoolean icon_fabrication_requested = new AtomicBoolean(false);
    Job icon_job; // this is needed to cancel the icon request when the item has become invisible

    protected Item_context item_context;

    public AtomicBoolean visible_in_scene = new AtomicBoolean(false);

    // javafx_x and javafx_y are going to be used in Translate_X (resp. Y)
    // vertical scroll is managed by substracting the y_offset
    private double javafx_x;
    private double javafx_y;
    // this is the (top-left) position of the image
    // in the possibly hugely tall virtual landscape
    // that contains all icons
    private double virtual_landscape_x = 0;
    private double virtual_landscape_y = 0;
    protected final Icon_factory_actor icon_factory_actor;
    protected final Selection_handler selection_handler;
    // never null as it describes the folder for folder
    // and the containing folder for images or files used for going up???
    // not final because renaming a folder requires to change the path_list_provider
    // this is ok as long as there is no other browser open on that folder: the change_gang manages this

    //**********************************************************
    public Item(
            Item_context item_context,
            Selection_handler selection_handler,
            Icon_factory_actor icon_factory_actor)
    //**********************************************************
    {
        this.item_context = item_context;
        this.icon_factory_actor = icon_factory_actor;
        this.selection_handler = selection_handler;

    }

    // path for display takes different form depending on the item type
    // it can be null, a PNG icon, or an animated gif
    abstract public Path get_path_for_display(boolean try_deep);


    public final Scene getScene() {
        return item_context.scene;
    }

    protected final Logger get_logger() {
        return item_context.logger;
    }

    public void set_translate_X(double dx) {
        if (get_Node() != null) get_Node().setTranslateX(dx);
    }

    public void set_translate_Y(double dy) {
        if (get_Node() != null) get_Node().setTranslateY(dy);
    }

    public void set_javafx_x(double x_) {
        javafx_x = x_;
    }

    public void set_javafx_y(double y_) {
        javafx_y = y_;
    }

    public double get_javafx_x() {
        return javafx_x;
    }

    public double get_javafx_y() {
        return javafx_y;
    }

    public void set_screen_x_of_image(double x_) {
        virtual_landscape_x = x_;
    }

    public void set_screen_y_of_image(double y_) {
        virtual_landscape_y = y_;
    }

    public double get_screen_x_of_image() {
        return virtual_landscape_x;
    }

    public double get_screen_y_of_image() {
        return virtual_landscape_y;
    }

    public abstract Node get_Node();

    public abstract double get_Width();

    public abstract double get_Height();


    abstract void you_are_visible_specific();

    abstract void you_are_invisible_specific();

    abstract int get_icon_size();

    abstract boolean has_icon();


    @Override // Icon_destination
    public boolean get_icon_fabrication_requested() {
        return icon_fabrication_requested.get();
    }

    @Override // Icon_destination
    public void set_icon_fabrication_requested(boolean b) {
        icon_fabrication_requested.set(b);
    }


    public abstract String get_string();

    public abstract void set_selected_look_specific(boolean b);

    private final AtomicBoolean is_selected = new AtomicBoolean(false);

    //**********************************************************
    public void set_selected(boolean b)
    //**********************************************************
    {
        is_selected.set(b);
        set_selected_look_specific(b);
    }

    //**********************************************************
    public void request_icon_to_factory(int target_icon_size)
    //**********************************************************
    {
        if (dbg) item_context.logger.log(("request_icon_to_factory for:" + get_item_path()));
        Icon_factory_request icon_factory_request = new Icon_factory_request(
                this, target_icon_size, item_context.owner,// aborter);
                new Aborter("Icon creation for " + get_item_path(), item_context.logger));


        if (icon_fabrication_requested.get()) {
            item_context.logger.log(Logger.warning+" Icon_factory_actor aborting-0, skipping icon request, as another one is in flight");
            return;
        }
        icon_fabrication_requested.set(true);

        icon_job = Actor_engine.run(icon_factory_actor, icon_factory_request, null, item_context.logger);

        if (dbg) item_context.logger.log("icon request : queued for: " + get_item_path());

    }


    //**********************************************************
    protected void cancel_icon()
    //**********************************************************
    {
        icon_fabrication_requested.set(false);
        if (icon_job != null) {
            icon_job.cancel(); // will trigger the aborter and if there is an associated thread, will interrupt it
            icon_job = null;
        }
    }

    static double xxx = 200;
    static double yyy = 200;

    //**********************************************************
    public static ContextMenu make_context_menu(Item_context item_context, Button local_button, Label local_label)
    //**********************************************************
    {
        ContextMenu context_menu = new ContextMenu();
        Look_and_feel_manager.set_context_menu_look(context_menu, item_context.owner, item_context.logger);
        if (item_context.item_path == null) {
            item_context.logger.log(Stack_trace_getter.get_stack_trace(""));
            return context_menu;
        }
        if (Files.isDirectory(item_context.item_path))
        {
            create_folder_size_menu_item(context_menu, item_context);


            if (item_context.is_trash)
            {
                Menu_items.add_menu_item_for_context_menu(
                        "Clear_Trash_Folder", true, null,
                        event -> {
                            if (dbg) item_context.logger.log("clearing trash!");
                            Static_files_and_paths_utilities.clear_trash(true, item_context.owner, item_context.aborter, item_context.logger);
                        },
                        context_menu, item_context.owner, item_context.logger);
            }


            if (!item_context.is_trash)
                {
                {
                    Path folder_path = item_context.item_path.getParent();

                    Menu_items.add_menu_item_for_context_menu(
                            "Browse_in_new_window", true,
                            null,//(new KeyCodeCombination(KeyCode.N,KeyCombination.SHORTCUT_DOWN)).getDisplayText(),
                            event -> {
                                if (dbg) item_context.logger.log("Browse in new window!");
                                Window_builder.additional_no_past(item_context.application, Window_type.File_system_2D, new Path_list_provider_for_file_system(folder_path, item_context.owner, item_context.logger), item_context.owner, item_context.logger);
                            }, context_menu, item_context.owner, item_context.logger);

                    if (Feature_cache.get(Feature.Enable_3D)) {
                        Menu_items.add_menu_item_for_context_menu(
                                "Browse_in_new_3D_window", true,
                                null,
                                event -> {
                                    if (dbg) item_context.logger.log("Browse in new window!");
                                    Window_builder.additional_no_past(item_context.application, Window_type.File_system_3D, new Path_list_provider_for_file_system(folder_path, item_context.owner, item_context.logger), item_context.owner, item_context.logger);
                                }, context_menu, item_context.owner, item_context.logger);
                    }

                    Menu_items.add_menu_item_for_context_menu(
                            "Disk_View", true,
                            null,
                            event -> {
                                if (dbg) item_context.logger.log("Show disk view");
                                Window_builder.additional_no_past(item_context.application, Window_type.File_system_diskview, new Path_list_provider_for_file_system(folder_path, item_context.owner, item_context.logger), item_context.owner, item_context.logger);
                            }, context_menu, item_context.owner, item_context.logger);
                }
                create_open_with_system_menu_item(context_menu, item_context);
                create_open_with_web_browser_menu_item(context_menu, item_context);
                create_rename_menu_item(item_context, local_button, local_label, context_menu);

                create_delete_menu_item(context_menu, item_context);
                Menu_items.add_menu_item_for_context_menu("Copy", true,
                        null,//(new KeyCodeCombination(KeyCode.C,KeyCodeCombination.SHORTCUT_DOWN)).getDisplayText(),
                        event -> {
                            if (dbg) item_context.logger.log("Copying the directory");
                            Path new_path = Static_files_and_paths_utilities.ask_user_for_new_dir_name(item_context.owner, item_context.item_path, item_context.logger);
                            if (new_path == null) {
                                Popups.popup_warning(Logger.warning+" copy of dir failed", "names are same ?", false, item_context.owner, item_context.logger);
                                return;
                            }
                            Static_files_and_paths_utilities.copy_dir_in_a_thread(item_context.owner, item_context.item_path, new_path, item_context.aborter, item_context.logger);
                        },
                        context_menu, item_context.owner, item_context.logger);

                create_edit_color_menu_item(context_menu, item_context);
            }
        } else {

            // this is for a FILE
            if (Guess_file_type.is_this_path_extension_an_image(item_context.item_path, item_context.owner, item_context.logger)) {
                create_open_exif_frame_menu_item(context_menu, item_context);
            }
            if (Guess_file_type.is_this_path_extension_a_music(item_context.item_path, item_context.logger)) {
                create_open_mediainfo_frame_menu_item(context_menu, item_context);
                create_edit_metadata_frame_menu_item(context_menu, item_context);
            }
            if (item_context.item_type == Iconifiable_item_type.video) {
                Item_file_with_icon.make_menu_items_for_videos(item_context.item_path, item_context.owner, context_menu, dbg, item_context.aborter, item_context.logger);
            }

            // is a "plain" file
            Menu_items.add_menu_item_for_context_menu(
                    "Browse_in_new_window", true,
                    null,//(new KeyCodeCombination(KeyCode.N,KeyCombination.SHORTCUT_DOWN)).getDisplayText(),
                    event -> {
                        if (dbg) item_context.logger.log("Browse in new window!");
                        Scroll_position_cache.scroll_position_cache_write(item_context.path_list_provider.get_key(), item_context.item_path.toAbsolutePath().toString(), "Item Browse_in_new_window", item_context.logger);
                        Window_builder.additional_no_past(item_context.application, Window_type.File_system_2D, new Path_list_provider_for_file_system(item_context.item_path.getParent(), item_context.owner, item_context.logger), item_context.owner, item_context.logger);
                    }, context_menu, item_context.owner, item_context.logger);

            create_open_with_system_menu_item(context_menu, item_context);
            create_open_with_web_browser_menu_item(context_menu, item_context);
            create_open_with_registered_application_menu_item(context_menu, item_context);


            Menu_items.add_menu_item_for_context_menu("View_Text_Read_Only", true, null,
                    actionEvent -> {
                        if (dbg) item_context.logger.log("button in item: View_Text_Read_Only");

                        if (Feature_cache.get(Feature.Use_monaco_for_text_edition)) {
                            Javalin_monaco.read_only(item_context.application, item_context.item_path, item_context.logger);
                        } else {
                            Text_frame.show(item_context.item_path, item_context.logger);
                        }
                    }, context_menu, item_context.owner, item_context.logger);

            Menu_items.add_menu_item_for_context_menu("Edit_Text", true, null,
                    actionEvent -> {
                        if (dbg) item_context.logger.log("button in item: Edit_Text");

                        if (Feature_cache.get(Feature.Use_monaco_for_text_edition)) {
                            Javalin_monaco.edit(item_context.application, item_context.item_path, item_context.logger);
                        } else {
                            Text_frame.show(item_context.item_path, item_context.logger);
                        }
                    }, context_menu, item_context.owner, item_context.logger);

            create_rename_menu_item(item_context, local_button, local_label, context_menu);

            create_copy_menu_item(context_menu, item_context);

            create_delete_menu_item(context_menu, item_context);

            Menu_items.create_show_file_size_menu_item(context_menu, item_context);


            /*if (Feature_cache.get(Feature.Enable_tags)) {
                create_edit_tag_menu_item(get_item_path(), context_menu,dbg, owner, aborter, logger);
            }*/
        }
        return context_menu;
    }

    //**********************************************************
    protected static void create_delete_menu_item(ContextMenu context_menu, Item_context item_context)
    //**********************************************************
    {
        String menu_text = null;
        if (item_context.path_list_provider instanceof Path_list_provider_for_file_system path_list_provider_for_file_system) {
            item_context.logger.log("pathlistprovider detected as FILE SYSTEM" + path_list_provider_for_file_system.get_key());
            menu_text = My_I18n.get_I18n_string("Delete", item_context.owner, item_context.logger);
        }
        if (item_context.path_list_provider instanceof Path_list_provider_for_playlist path_list_provider_for_playlist) {
            menu_text = My_I18n.get_I18n_string("Remove_From_Playlist", item_context.owner, item_context.logger);
        }
        Menu_items.add_menu_item_for_context_menu(menu_text, false,
                null,//(new KeyCodeCombination(KeyCode.BACK_SPACE)).getDisplayText(),
                event -> {
                    if (dbg) item_context.logger.log("Deleting!");
                   if (item_context.item_path != null) {
                        item_context.path_list_provider.delete(item_context.item_path, item_context.owner, item_context.aborter, item_context.logger);
                    }
                }, context_menu, item_context.owner, item_context.logger);
    }

    //**********************************************************
    public static void create_copy_menu_item(ContextMenu context_menu, Item_context item_context)
    //**********************************************************
    {
        Menu_items.add_menu_item_for_context_menu("Copy", true,
                null,//(new KeyCodeCombination(KeyCode.C,KeyCodeCombination.SHORTCUT_DOWN)).getDisplayText(),
                event -> {
                    if (dbg) item_context.logger.log("copying!");
                    if (item_context.item_path == null) {
                        item_context.logger.log(Stack_trace_getter.get_stack_trace(""));
                        return;
                    }

                    Path new_path = Static_files_and_paths_utilities.ask_user_for_new_file_name(item_context.owner, item_context.item_path, item_context.logger);
                    if (new_path == null) return;
                    try {
                        Files.copy(item_context.item_path, new_path);
                    } catch (IOException e) {
                        String text = "copy failed for: " + new_path.getFileName() ;
                        item_context.logger.log(text+" Exception:" + e);
                        Popups.popup_Exception(e,200,text , item_context.owner, item_context.logger);
                    }
                }, context_menu, item_context.owner, item_context.logger);
    }

    //**********************************************************
    protected void create_show_file_size_menu_item(ContextMenu context_menu, Item_context item_context)
    //**********************************************************
    {
        Menu_items.add_menu_item_for_context_menu("Show_file_size", true, null,
                event -> {
                    if (item_context.item_path == null) {
                        this.item_context.logger.log(Stack_trace_getter.get_stack_trace(""));
                        return;
                    }
                    show_file_size(item_context.item_path, this.item_context.owner, this.item_context.logger);
                }, context_menu, this.item_context.owner, this.item_context.logger);
    }

    public static void show_file_size(Path path, Window owner, Logger logger) {
        if (dbg) logger.log("File length");
        String size_in_bytes = Static_files_and_paths_utilities.get_1_line_string_with_size(path, owner, logger);
        String message = My_I18n.get_I18n_string("File_size_for", owner, logger) + "\n" + path.getFileName().toString();
        //Popups.popup_warning(error_message, file_size, false,logger);
        Stage local_stage = new Stage();
        local_stage.setHeight(200);
        local_stage.setWidth(600);
        local_stage.setX(owner.getX()+xxx);
        local_stage.setY(owner.getY()+yyy);
        yyy += 200;
        if (yyy > 600) {
            yyy = 200;
            xxx += 600;
            if (xxx > 1000) xxx = 200;
        }
        TextArea textarea1 = new TextArea(message + "\n" + size_in_bytes);
        Font_size.apply_this_font_size_to_Node(textarea1, 24, logger);
        VBox vbox = new VBox(textarea1);
        Scene scene = new Scene(vbox, Color.WHITE);
        local_stage.setTitle(path.toAbsolutePath().toString());
        local_stage.setScene(scene);
        local_stage.show();

        logger.log("size_in_bytes->" + size_in_bytes + "<-");
        //b_.set_status(size_in_bytes);
    }


    //**********************************************************
    public void is_in_selection_for_moving(boolean selected)
    //**********************************************************
    {
        set_selected(selected);
        if (selected) {
            Path item_path = get_item_path();
            if (item_path == null) {
                item_context.logger.log(Stack_trace_getter.get_stack_trace(""));
                return;
            }
            if (selection_handler.add_to_selected_for_moving(item_path)) {
                item_context.logger.log("item selected:" + get_item_path());
            }
        }
    }

    //**********************************************************
    public static void give_a_menu_to_the_button(Item_context item_context, Button local_button, Label details)
    //**********************************************************
    {
        //logger.log(Stack_trace_getter.get_stack_trace("give_a_menu_to_the_button "));
        local_button.setOnContextMenuRequested((ContextMenuEvent event) -> {
            if (dbg) {
                if (item_context.item_path == null) {
                    item_context.logger.log(Stack_trace_getter.get_stack_trace(""));
                    return;
                }
                item_context.logger.log("show context menu of button:" + item_context.item_path.toAbsolutePath());
            }
            ContextMenu context_menu = make_context_menu(item_context, local_button, details);
            context_menu.show(local_button, event.getScreenX(), event.getScreenY());
        });
    }


    //**********************************************************
    public void you_are_invisible()
    //**********************************************************
    {
        you_are_invisible_specific();
        cancel_icon();

    }

    //**********************************************************
    public void you_are_visible()
    //**********************************************************
    {
        //logger.log("Visible: "+path.getFileName());
        //if( !Platform.isFxApplicationThread())  logger.log(Stack_trace_getter.get_stack_trace("PANIC not on Fx thread"));

        you_are_visible_specific();
        //get_Node().setVisible(true);
        if (has_icon())
            request_icon_to_factory(get_icon_size());
    }


    // this is called SUPER intensively when scrolling
    //**********************************************************
    public void process_is_visible(double current_vertical_offset)
    //**********************************************************
    {
        if (!Platform.isFxApplicationThread()) {
            item_context.logger.log(Stack_trace_getter.get_stack_trace("HAPPENS1 process_is_visible"));
            Platform.runLater(() -> process_is_visible(current_vertical_offset));
        }

        set_translate_X(get_javafx_x());
        set_translate_Y(get_javafx_y() - current_vertical_offset);

        // this is essential: dont call you_are_visible() unless needed
        if (!visible_in_scene.get()) {
            visible_in_scene.set(true);
            you_are_visible();
            set_selected_look_specific(is_selected.get());
        }
    }

    //**********************************************************
    public void process_is_invisible(double local_current_vertical_offset)
    //**********************************************************
    {
        //if ( !Platform.isFxApplicationThread()) logger.log(Stack_trace_getter.get_stack_trace("PANIC"));

        //WTF set_translate_X(get_javafx_x());
        //WTF set_translate_Y(get_javafx_y() - current_vertical_offset);

        //if ( !Platform.isFxApplicationThread())logger.log(Stack_trace_getter.get_stack_trace("PANIC process_is_invisible "+Platform.isFxApplicationThread()));
        // do the shift only once as you_are_invisible is costly
        if (visible_in_scene.get()) {
            visible_in_scene.set(false);
            you_are_invisible();
        }
    }

    //**********************************************************
    public static void create_rename_menu_item(Item_context item_context, Button local_button, Label local_label, ContextMenu context_menu)
    //**********************************************************
    {
        Menu_items.add_menu_item_for_context_menu("Rename", true,
                null,//(new KeyCodeCombination(KeyCode.R)).getDisplayText(),

                event -> {
                    if (dbg) item_context.logger.log("Item: Renaming");
                    if (item_context.item_path == null) {
                        item_context.logger.log(Stack_trace_getter.get_stack_trace(""));
                        return;
                    }
                    String original_name = item_context.item_path.getFileName().toString();
                    TextField text_edit = new TextField(original_name);
                    Node restored = local_button.getGraphic();
                    local_button.setGraphic(text_edit);
                    text_edit.setMinWidth(local_button.getWidth() * 0.9);
                    text_edit.requestFocus();
                    text_edit.positionCaret(original_name.length());
                    text_edit.setFocusTraversable(true);
                    text_edit.setOnAction(actionEvent -> {

                        String new_item_name = text_edit.getText();
                        actionEvent.consume();
                        if (item_context.item_path == null) {
                            item_context.logger.log(Stack_trace_getter.get_stack_trace("Should not happen "));
                            return;
                        }
                        if (item_context.item_path.toFile().isDirectory()) {
                            Path new_path = Moving_files.change_dir_name(item_context.item_path, new_item_name, item_context.owner, item_context.aborter, item_context.logger);
                            if (new_path == null) {
                                if (dbg) item_context.logger.log("rename failed");
                                local_button.setText(original_name);
                                local_button.setGraphic(restored);
                                return;
                            }
                            local_button.setText(new_item_name);
                            local_button.setGraphic(restored);
                            item_context.path_list_provider = Path_list_provider.get_appropriate(item_context.window_type, new_path, item_context.owner, item_context.aborter, item_context.logger);//new Path_list_provider_for_file_system(new_path,owner,logger);
                        } else {
                            // item is a file
                            Path old = item_context.item_path.toAbsolutePath();
                            if (item_context.path_list_provider instanceof Path_list_provider_for_playlist plpfpl) {
                                Path new_path = Paths.get(item_context.item_path.getParent().toString(), new_item_name);
                                plpfpl.swap(old.toString(), new_path.toAbsolutePath().toString());
                            }
                            double x = item_context.owner.getX() + 100;
                            double y = item_context.owner.getY() + 100;
                            Path new_path = Static_files_and_paths_utilities.change_file_name(old, new_item_name, item_context.owner, item_context.aborter, item_context.logger);
                            if (new_path == null) {
                                item_context.logger.log(Logger.warning+" rename failed");
                                local_button.setText(original_name);
                                local_button.setGraphic(restored);
                                return;
                            }
                            item_context.item_path = new_path;

                            if (local_label == null) {
                                // TODO: verify this ???
                                // the item is a Item_folder_with_icon
                                if (dbg) item_context.logger.log("rename done");
                                local_button.setText(new_item_name);
                                local_button.setGraphic(restored);
                            } else {
                                String size = Static_files_and_paths_utilities.get_1_line_string_for_byte_data_size(item_context.item_path.toFile().length(), item_context.owner, item_context.logger);
                                local_button.setText(size);
                                local_label.setText(new_item_name);
                                //Font_size.set_preferred_font_size(label,logger);
                                Font_size.apply_global_font_size_to_Node(local_label, item_context.owner, item_context.logger);
                                local_button.setGraphic(local_label);
                            }

                            //no need the disk change will cause  the browser to redraw with a force reload
                            // path_list_provider.reload("file name changed: "+new_item_name, aborter);
                        }

                        if (dbg) item_context.logger.log("rename done");
                    });
                }, context_menu, item_context.owner, item_context.logger);
    }


    //**********************************************************
    public static void create_open_mediainfo_frame_menu_item(ContextMenu context_menu, Item_context item_context)
    //**********************************************************
    {
        Menu_items.add_menu_item_for_context_menu("Info_about", true, null,
                actionEvent -> {
                    if (dbg) item_context.logger.log("info");
                    Audio_info_frame.show(item_context.item_path, item_context.owner, item_context.logger);
                }, context_menu, item_context.owner, item_context.logger);
    }

    //**********************************************************
    public static void create_edit_metadata_frame_menu_item(ContextMenu context_menu, Item_context item_context)
    //**********************************************************
    {
        Menu_items.add_menu_item_for_context_menu(
                "Edit_Song_Metadata", true, null,
                (ActionEvent e) -> Ffmpeg_metadata_editor.edit_metadata_of_a_file_in_a_thread(item_context.item_path, item_context.owner, item_context.logger),
                context_menu, item_context.owner, item_context.logger);

    }


    //**********************************************************
    public static void create_open_exif_frame_menu_item(ContextMenu context_menu, Item_context item_context)
    //**********************************************************
    {
        Menu_items.add_menu_item_for_context_menu("Info_about", true, null,
                actionEvent -> {
                    if (dbg) item_context.logger.log("info");
                    Actor_engine.execute(() -> {
                        if (item_context.item_path != null) {
                            Image_and_properties iap = Full_image_from_disk.load_native_resolution_image_from_disk(item_context.item_path, true, item_context.owner, item_context.aborter, item_context.logger);
                            if (iap != null)
                                Exif_stage.show_exif_stage(iap.image(), item_context.item_path, item_context.owner, item_context.aborter, item_context.logger);
                        }
                    }, "Show EXIF info", item_context.logger);
                },
                context_menu,
                item_context.owner,
                item_context.logger);

    }


    //**********************************************************
    public static void create_open_with_registered_application_menu_item(ContextMenu context_menu, Item_context item_context)
    //**********************************************************
    {
        Menu_items.add_menu_item_for_context_menu(
                "Open_With_Registered_Application", true, null,
                actionEvent -> {
                    if (dbg) item_context.logger.log("button in item: Open_With_Registered_Application");
                    System_open_actor.open_with_registered_application(item_context.item_path, item_context.owner, item_context.aborter, item_context.logger);
                }, context_menu, item_context.owner, item_context.logger);
    }

    //**********************************************************
    public static void create_open_with_web_browser_menu_item(ContextMenu context_menu, Item_context item_context)
    //**********************************************************
    {
        Menu_items.add_menu_item_for_context_menu("Open_With_Web_Browser", true, null,
                actionEvent -> {
                    if (dbg) item_context.logger.log("button in item: System Open");
                    System_open_actor.open_with_web_browser(item_context.application, item_context.item_path, item_context.owner, item_context.aborter, item_context.logger);
                }, context_menu, item_context.owner, item_context.logger);
    }

    //**********************************************************
    public static void create_open_with_system_menu_item(ContextMenu context_menu, Item_context item_context)
    //**********************************************************
    {
        Menu_items.add_menu_item_for_context_menu("Open_With_System", true, null,
                actionEvent -> {
                    if (dbg) item_context.logger.log("button in item: System Open");
                    System_open_actor.open_with_system(item_context.application, item_context.item_path, item_context.owner, item_context.aborter, item_context.logger);
                }, context_menu, item_context.owner, item_context.logger);
    }


    //**********************************************************
    public static void create_folder_size_menu_item(ContextMenu context_menu, Item_context item_context)
    //**********************************************************
    {
        Menu_items.add_menu_item_for_context_menu(
                "Folder_size_total", true, null,
                event -> Folder_size_stage.get_folder_size(item_context.item_path, item_context.owner, item_context.logger),
                context_menu, item_context.owner, item_context.logger);
    }

    //**********************************************************
    public static void create_edit_color_menu_item(ContextMenu context_menu, Item_context item_context)
    //**********************************************************
    {

        String text = My_I18n.get_I18n_string("Color_Tag", item_context.owner, item_context.logger);
        Menu color_tag_menu = new Menu(text);
        Look_and_feel_manager.set_menu_item_look(color_tag_menu, item_context.owner, item_context.logger);
        List<My_color> possible_colors = new ArrayList<>();
        for (My_color candidate_color : My_colors.get_all_colors(item_context.owner, item_context.logger))
        {
            possible_colors.add(candidate_color);
        }
        List<CheckMenuItem> all_check_menu_items = new ArrayList<>();
        for (My_color color : possible_colors) {
            create_menu_item_for_one_color(
                    color_tag_menu, color, all_check_menu_items, item_context);
        }
        context_menu.getItems().add(color_tag_menu);
    }

    //**********************************************************
    public static void create_menu_item_for_one_color(
            Menu menu, My_color target_color, List<CheckMenuItem> all_check_menu_items, Item_context item_context)
    //**********************************************************
    {
        if (dbg) {
            if (target_color.color() == null) {
                item_context.logger.log("color menu item for: ->NO COLOR<-");
            } else {
                item_context.logger.log("color menu item for: ->" + target_color.color().toString() + "<-");
            }
        }

        String txt = target_color.localized_name();
        CheckMenuItem check_menu_item = new CheckMenuItem(txt);
        check_menu_item.setGraphic(new Circle(10, target_color.color()));
        if ((item_context.tag_color == null) && (target_color.color() == null))
        {
            check_menu_item.setSelected(true);
        }
        if ((item_context.tag_color != null) && (target_color.color() != null))
        {
            check_menu_item.setSelected(item_context.tag_color.toString().equals(target_color.color().toString()));
        }

        menu.getItems().add(check_menu_item);
        all_check_menu_items.add(check_menu_item);

        check_menu_item.setOnAction(actionEvent ->
        {
            CheckMenuItem local = (CheckMenuItem) actionEvent.getSource();
            if (local.isSelected())
            {
                on_color_selected(all_check_menu_items, item_context, local);


            }


        });
    }

    //**********************************************************
    private static void on_color_selected(List<CheckMenuItem> all_check_menu_items, Item_context item_context, CheckMenuItem local)
    //**********************************************************
    {
        for (CheckMenuItem cmi : all_check_menu_items) {
            if (cmi != local) cmi.setSelected(false);
        }
        String localized_name = local.getText();

        My_color my_color = My_colors.my_color_from_localized_name(localized_name, item_context.owner, item_context.logger);
        item_context.logger.log("is selected: ->"+localized_name+"<-");
        if( my_color == null)
        {
            item_context.tag_color = null;

        }
        else
        {
            item_context.tag_color = my_color.color();
        }
        Window_builder.replace_same_folder(
                item_context.application,
                item_context.shutdown_target,
                item_context.window_type,
                item_context.path_list_provider,
                item_context.path_list_provider.get_key(),
                item_context.top_left,
                item_context.owner,
                item_context.logger);        if (item_context.item_path == null) {
            item_context.logger.log(Stack_trace_getter.get_stack_trace("should not happen: no path ?"));
            return;
        }
        My_colors.save_color(item_context.item_path, my_color.color().toString(), item_context.logger);
        //double font_size = Non_booleans_properties.get_font_size(item_context.owner, item_context.logger);
        //double icon_height = Look_and_feel.MAGIC_HEIGHT_FACTOR * font_size;
    }
}