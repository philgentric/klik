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

package klik.browser.items;

import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.stage.Window;
import klik.Window_type;
import klik.Instructions;
import klik.util.execute.actor.Aborter;
import klik.util.execute.actor.Actor_engine;
import klik.util.execute.actor.Job;
import klik.audio.Audio_info_frame;
import klik.audio.Ffmpeg_metadata_editor;
import klik.path_lists.Path_list_provider_for_file_system;
import klik.browser.icons.Icon_destination;
import klik.browser.icons.Icon_factory_actor;
import klik.browser.icons.Icon_factory_request;
import klik.browser.virtual_landscape.Path_comparator_source;
import klik.path_lists.Path_list_provider;
import klik.browser.virtual_landscape.Selection_handler;
import klik.experimental.metadata.Tag_stage;
import klik.images.Exif_stage;
import klik.look.Font_size;
import klik.look.Look_and_feel;
import klik.look.Look_and_feel_manager;
import klik.look.my_i18n.My_I18n;
import klik.properties.Non_booleans_properties;
import klik.util.execute.System_open_actor;
import klik.util.files_and_paths.Moving_files;
import klik.util.image.Full_image_from_disk;
import klik.util.ui.Folder_size_stage;
import klik.util.files_and_paths.Guess_file_type;
import klik.util.files_and_paths.Static_files_and_paths_utilities;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import klik.util.ui.Menu_items;
import klik.util.ui.Popups;
import klik.util.ui.Text_frame;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

//**********************************************************
public abstract class Item implements Icon_destination
//**********************************************************
{
    protected static final boolean dbg = true;
    public static final boolean layout_dbg = false;
    public AtomicBoolean icon_fabrication_requested = new AtomicBoolean(false);
    Job icon_job; // this is needed to cancel the icon request when the item has become invisible

    protected Color color;
    protected final Window owner;
    protected final Scene scene;
    protected final Logger logger;
    public AtomicBoolean visible_in_scene = new AtomicBoolean(false);
    public final Aborter aborter;

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
    //private Path path; // null for folders
    protected Path_list_provider path_list_provider;
    // never null as it describes the folder for folder
    // and the containing folder for images or files used for going up???
    // not final because renaming a folder requires to change the path_list_provider
    // this is ok as long as there is no other browser open on that folder: the change_gang manages this
    protected final Path_comparator_source path_comparator_source;

    //**********************************************************
    public Item(
            Scene scene,
            Selection_handler selection_handler,
            Icon_factory_actor icon_factory_actor,
            Color color,
            Path_list_provider path_list_provider,
            Path_comparator_source path_comparator_source,
            Window owner,
            Aborter aborter,
            Logger logger)
    //**********************************************************
    {
        this.path_list_provider = path_list_provider;
        this.path_comparator_source = path_comparator_source;
        this.aborter = aborter;
        this.scene = scene;
        this.icon_factory_actor = icon_factory_actor;
        this.selection_handler = selection_handler;
        this.owner = owner;
        this.logger = logger;
        this.color = color;

    }

    // path for display takes different form depending on the item type
    // it can be null, a PNG icon, or an animated gif
    abstract public Path get_path_for_display(boolean try_deep);

    abstract void set_new_path(Path newPath);

    abstract public Path get_item_path();

    
    public final Scene getScene()
    {
        return scene;
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

    public void set_screen_x_of_image(double x_) { virtual_landscape_x = x_; }
    public void set_screen_y_of_image(double y_) {virtual_landscape_y = y_;}
    public double get_screen_x_of_image() { return virtual_landscape_x; }
    public double get_screen_y_of_image() {return virtual_landscape_y;}

    public abstract Node get_Node();
    public abstract double get_Width();
    public abstract double get_Height();
    public abstract boolean is_trash();
    public abstract Path is_parent_of();
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

    public abstract void set_is_selected_internal();

    public abstract void set_is_unselected_internal();


    //**********************************************************
    public void request_icon_to_factory(int target_icon_size)
    //**********************************************************
    {
        if ( dbg) logger.log(("request_icon_to_factory for:"+get_item_path()));
        Icon_factory_request icon_factory_request = new Icon_factory_request(
                this, target_icon_size,owner,// aborter);
                new Aborter("Icon creation for "+get_item_path(),logger));

        if (dbg) logger.log("icon request : queued! ");

        Icon_destination destination = icon_factory_request.destination;
        if (destination == null) {
            logger.log(Stack_trace_getter.get_stack_trace("SHOULD NOT HAPPEN icon factory : cancel! destination==null"));
            return;
        }

        if (icon_factory_request.destination.get_icon_fabrication_requested())
        {
            logger.log("dont do another icon request, another one is in flight");
            return;
        }
        icon_factory_request.destination.set_icon_fabrication_requested(true);

        icon_job = Actor_engine.run(icon_factory_actor, icon_factory_request, null,logger);


    }


    //**********************************************************
    protected void cancel_icon()
    //**********************************************************
    {
        icon_fabrication_requested.set(false);
        if ( icon_job!= null)
        {
            icon_job.cancel(); // will trigger the aborter and if there is an associated thread, will interrupt it
            icon_job = null;
        }
    }

    static double xxx = 200;
    static double yyy = 200;

    //**********************************************************
    public ContextMenu make_context_menu(Button local_button, Label local_label)
    //**********************************************************
    {
        ContextMenu context_menu = new ContextMenu();
        Look_and_feel_manager.set_context_menu_look(context_menu, owner, logger);
        Path local_path = get_item_path();
        if (Files.isDirectory(local_path))
        {
            Menu_items.add_menu_item(
                    "Get_folder_size",
                    event -> Folder_size_stage.get_folder_size(get_item_path(),owner, logger),
                    context_menu,owner,logger);

            if (is_trash())
            {
                Menu_items.add_menu_item(
                    "Clear_Trash_Folder",
                event -> {
                        if (dbg) logger.log("clearing trash!");
                        Static_files_and_paths_utilities.clear_trash(true,owner, aborter,logger);
                    },
                    context_menu,owner,logger);
            }


            if (!is_trash() && (is_parent_of() == null))
            {
                {
                    Path target  = get_item_path().getParent();
                    if ( this instanceof Item_folder)
                    {
                        target  = get_item_path();
                    }
                    Path finalTarget = target;
                    Menu_items.add_menu_item(
                            "Browse_in_new_window",
                            event -> {
                                if (dbg) logger.log("Browse in new window!");
                                Instructions.additional_no_past(Window_type.File_system_2D,new Path_list_provider_for_file_system(finalTarget), owner, logger);
                            }, context_menu, owner, logger);

                    Menu_items.add_menu_item(
                            "Browse_in_new_3D_window",
                            event -> {
                                if (dbg) logger.log("Browse in new window!");
                                Instructions.additional_no_past(Window_type.File_system_3D, new Path_list_provider_for_file_system(finalTarget), owner, logger);
                            }, context_menu, owner, logger);
                }
                create_open_with_system_menu_item(get_item_path(),context_menu);
                /*if (Feature_cache.get(Feature.Enable_tags))
                {
                    create_edit_tag_menu_item(get_item_path(), context_menu, dbg, owner, aborter, logger);
                }*/
                create_rename_menu_item(local_button, local_label,context_menu);
                create_delete_menu_item(context_menu);
                Menu_items.add_menu_item("Copy",
                        event -> {
                        if (dbg) logger.log("Copying the directory");
                        Path new_path =  Static_files_and_paths_utilities.ask_user_for_new_dir_name(owner,get_item_path(),logger);
                        if ( new_path == null)
                        {
                            Popups.popup_warning("❗ copy of dir failed","names are same ?", false,owner,logger);
                            return;
                        }
                        Static_files_and_paths_utilities.copy_dir_in_a_thread(owner, get_item_path(), new_path, aborter, logger);
                    },
                        context_menu,owner,logger);

                create_edit_color_menu_item(context_menu);
            }
        }
        else
        {
            if (Guess_file_type.is_this_path_an_image(get_item_path())) {
                create_open_exif_frame_menu_item(get_item_path(), context_menu);
            }
            if (Guess_file_type.is_this_path_a_music(get_item_path())) {
                create_open_mediainfo_frame_menu_item(get_item_path(), context_menu);
                create_edit_metadata_frame_menu_item(get_item_path(), context_menu);
            }
            if (this.get_item_type() == Iconifiable_item_type.video) {
                Item_file_with_icon.make_menu_items_for_videos(get_item_path(), owner, context_menu, dbg, aborter, logger);
            }

            // is a "plain" file
            create_open_with_system_menu_item(get_item_path(),context_menu);

            Menu_items.add_menu_item(
                    "Open_With_Registered_Application",
                    actionEvent -> {
                    if (dbg) logger.log("button in item: Open_With_Registered_Application");
                    System_open_actor.open_special(get_item_path(), owner,aborter,logger);
                },context_menu,owner,logger);


            Menu_items.add_menu_item("Open_With_Klik_Text_Frame",
                    actionEvent -> {
                    if (dbg) logger.log("button in item: Open_With_Klik_Text_Frame");

                    Text_frame.show(get_item_path(),logger);
                }, context_menu,owner,logger);

            create_rename_menu_item(local_button, local_label,context_menu);

            Menu_items.add_menu_item("Copy",
                    event -> {
                    if (dbg) logger.log("copying!");

                    Path new_path = Static_files_and_paths_utilities.ask_user_for_new_file_name(owner,get_item_path(),logger);
                    if ( new_path == null) return;
                    try
                    {
                        Files.copy(get_item_path(), new_path);
                    } catch (IOException e)
                    {
                        logger.log("copy failed: could not create new file for: " + get_item_path().getFileName() + ", Exception:" + e);
                    }
                },context_menu,owner,logger);

            create_delete_menu_item(context_menu);

            create_show_file_size_menu_item(context_menu);


            /*if (Feature_cache.get(Feature.Enable_tags)) {
                create_edit_tag_menu_item(get_item_path(), context_menu,dbg, owner, aborter, logger);
            }*/
        }
        return context_menu;
    }

    protected void create_show_file_size_menu_item(ContextMenu context_menu) {
        Menu_items.add_menu_item("Show_file_size",
                event -> {
                if (dbg) logger.log("File size");
                String size_in_bytes = Static_files_and_paths_utilities.get_1_line_string_with_size(get_item_path(),owner,logger);
                String message = My_I18n.get_I18n_string("File_size_for", owner,logger) +"\n"+ get_item_path().getFileName().toString();
                //Popups.popup_warning(error_message, file_size, false,logger);
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
                Font_size.apply_this_font_size_to_Node(textarea1,24,logger);
                VBox vbox = new VBox(textarea1);
                Scene scene = new Scene(vbox, Color.WHITE);
                local_stage.setTitle(get_item_path().toAbsolutePath().toString());
                local_stage.setScene(scene);
                local_stage.show();

                logger.log("size_in_bytes->"+size_in_bytes+"<-");
                //b_.set_status(size_in_bytes);
            }, context_menu,owner,logger);
    }

    //**********************************************************
    public void create_open_mediainfo_frame_menu_item(Path path, ContextMenu context_menu)
    //**********************************************************
    {
        Menu_items.add_menu_item("Info_about",
                actionEvent -> {
            if (dbg) logger.log("info");
            Audio_info_frame.show(path,owner,logger);
        },context_menu,owner,logger);
    }
    //**********************************************************
    public void create_edit_metadata_frame_menu_item(Path path, ContextMenu context_menu)
    //**********************************************************
    {
        Menu_items.add_menu_item(
                "Edit_Song_Metadata",
                (ActionEvent e) -> Ffmpeg_metadata_editor.edit_metadata_of_a_file_in_a_thread(path, owner, logger),
                context_menu, owner, logger);

    }


    //**********************************************************
    public void create_open_exif_frame_menu_item(Path path, ContextMenu context_menu)
    //**********************************************************
    {
        Menu_items.add_menu_item("Info_about",
                actionEvent -> {
                    if (dbg) logger.log("info");
                    Actor_engine.execute(()-> {
                        Optional<Image> op = Full_image_from_disk.load_native_resolution_image_from_disk(path, true, owner, aborter, logger);
                        if ( op.isPresent()) Exif_stage.show_exif_stage(op.get(), path, owner, aborter, logger);
                    },"Show EXIF info",logger);
                },
                context_menu,
                owner,
                logger);

    }

    //**********************************************************
    public static void create_edit_tag_menu_item(Path path, ContextMenu context_menu,boolean dbg, Window owner, Aborter aborter,Logger logger)
    //**********************************************************
    {
        Menu_items.add_menu_item("Show_tag",
                event -> {
            if (dbg) logger.log("File tag");
            Tag_stage.open_tag_stage(path,true,owner,aborter,logger);
        },context_menu,owner,logger);
    }

    //**********************************************************
    public void create_open_with_system_menu_item(Path path, ContextMenu context_menu)
    //**********************************************************
    {
        Menu_items.add_menu_item("Open_with_system",
                actionEvent -> {
            if (dbg) logger.log("button in item: System Open");
            System_open_actor.open_with_system(path, owner,aborter,logger);
        },context_menu,owner,logger);
    }

    //**********************************************************
    public void create_edit_color_menu_item(ContextMenu context_menu)
    //**********************************************************
    {

        String text = My_I18n.get_I18n_string("Color_Tag",owner,logger);
        Menu menu = new Menu(text);
        Look_and_feel_manager.set_menu_item_look(menu,owner,logger);
        List<My_color> possible_colors = new ArrayList<>();
        for (My_color candidate_color : My_colors.get_all_colors(owner,logger))
        {
            possible_colors.add(candidate_color);
        }
        List<CheckMenuItem> all_check_menu_items = new ArrayList<>();
        for ( My_color color : possible_colors)
        {
            create_menu_item_for_one_color( menu, color, all_check_menu_items, logger);
        }
        context_menu.getItems().add(menu);
    }

    //**********************************************************
    public void create_menu_item_for_one_color(Menu menu, My_color target_color, List<CheckMenuItem> all_check_menu_items, Logger logger)
    //**********************************************************
    {
        String txt = target_color.localized_name();
        CheckMenuItem item = new CheckMenuItem(txt);
        item.setGraphic(new Circle(10, target_color.color()));
        if ( color == null)
        {
            if ( target_color.java_name() == null)
            {
                item.setSelected(true);
            }
        }
        else
        {
            item.setSelected(color.toString() == target_color.java_name());
        }


        item.setOnAction(actionEvent -> {
            CheckMenuItem local = (CheckMenuItem) actionEvent.getSource();
            if (local.isSelected())
            {
                for ( CheckMenuItem cmi : all_check_menu_items)
                {
                    if (cmi != local) cmi.setSelected(false);
                }
                String localized_name = local.getText();

                My_color my_color = My_colors.my_color_from_localized_name(localized_name,owner,logger);
                //logger.log("is selected: ->"+localized_name+"<-");
                color = my_color.color();
                My_colors.save_color(get_item_path(),my_color.java_name(),logger);
                if ( this instanceof Item_file_no_icon ifni)
                {
                    double font_size = Non_booleans_properties.get_font_size(owner,logger);
                    double icon_height = Look_and_feel.MAGIC_HEIGHT_FACTOR * font_size;
                    Look_and_feel_manager.set_button_look_as_folder(ifni.button, icon_height, color,owner,logger);
                }
                if ( this instanceof Item_folder itf)
                {
                    double font_size = Non_booleans_properties.get_font_size(owner, logger);
                    double icon_height = Look_and_feel.MAGIC_HEIGHT_FACTOR * font_size;
                    Look_and_feel_manager.set_button_look_as_folder(itf.button, icon_height, color,owner,logger);
                }
                if ( this instanceof Item_folder_with_icon itfwi)
                {
                    double font_size = Non_booleans_properties.get_font_size(owner, logger);
                    double icon_height = Look_and_feel.MAGIC_HEIGHT_FACTOR * font_size;
                    Look_and_feel_manager.set_button_look_as_folder(itfwi.button, icon_height, color,owner,logger);
                }
            }
        });
        menu.getItems().add(item);
        all_check_menu_items.add(item);

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
        if (selection_handler.add_to_selected_files(get_item_path())) {
            set_is_selected_internal();
            logger.log("item selected:" + get_item_path());
        }
    }

    //**********************************************************
    public void give_a_menu_to_the_button(Button local_button, Label local_label)
    //**********************************************************
    {
        ContextMenu context_menu = make_context_menu(local_button, local_label);
        local_button.setOnContextMenuRequested((ContextMenuEvent event) -> {
            //if ( dbg)
            logger.log("show context menu of button:"+ get_item_path().toAbsolutePath());
            context_menu.show(local_button, event.getScreenX(), event.getScreenY());
        });
    }



    //**********************************************************
    private void create_delete_menu_item(ContextMenu context_menu)
    //**********************************************************
    {
        Menu_items.add_menu_item("Delete",
                event -> {
            if (dbg) logger.log("Deleting!");
            double x = owner.getX()+100;
            double y = owner.getY()+100;
            path_list_provider.delete(get_item_path(), owner, x, y, aborter, logger);
            //Static_files_and_paths_utilities.move_to_trash(path,owner,x,y, null, browser_aborter,logger);
        },context_menu,owner,logger);
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
        if( has_icon())
            request_icon_to_factory(get_icon_size());
    }


    // this is called SUPER intensively when scrolling
    //**********************************************************
    public synchronized void process_is_visible(double current_vertical_offset)
    //**********************************************************
    {
        //if ( !Platform.isFxApplicationThread()) logger.log(Stack_trace_getter.get_stack_trace("PANIC"));
        //if ( get_javafx_y() == 0) logger.log("process_is_visible item "+get_item_path()+" x="+get_javafx_x()+" y="+get_javafx_y());


        set_translate_X(get_javafx_x());
        set_translate_Y(get_javafx_y() - current_vertical_offset);

        // this is essential: dont call you_are_visible() unless needed
        if (!visible_in_scene.get())
        {
            visible_in_scene.set(true);
            you_are_visible();
        }
    }

    //**********************************************************
    public void process_is_invisible(double current_vertical_offset)
    //**********************************************************
    {
        //if ( !Platform.isFxApplicationThread()) logger.log(Stack_trace_getter.get_stack_trace("PANIC"));

        set_translate_X(get_javafx_x());
        set_translate_Y(get_javafx_y() - current_vertical_offset);

        //if ( !Platform.isFxApplicationThread())logger.log(Stack_trace_getter.get_stack_trace("PANIC process_is_invisible "+Platform.isFxApplicationThread()));
        if (visible_in_scene.get())
        {
            visible_in_scene.set(false);
            you_are_invisible();
        }
    }

    //**********************************************************
    private void create_rename_menu_item(Button local_button, Label local_label, ContextMenu context_menu)
    //**********************************************************
    {
        Menu_items.add_menu_item("Rename",
                event -> {
            if (dbg) logger.log("Item2_button: Renaming");
            String original_name = get_item_path().getFileName().toString();
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
                if ( get_item_path().toFile().isDirectory() )
                {
                    Path new_path = Moving_files.change_dir_name(get_item_path(), new_dir_name, owner, aborter, logger);
                    if ( new_path == null)
                    {
                        if (dbg) logger.log("rename failed");
                        local_button.setText(original_name);
                        local_button.setGraphic(restored);
                        return;
                    }
                    local_button.setText(new_dir_name);
                    local_button.setGraphic(restored);
                    path_list_provider = new Path_list_provider_for_file_system(new_path);
                }
                else
                {
                    double x = owner.getX()+100;
                    double y = owner.getY()+100;
                    Path new_path = Static_files_and_paths_utilities.change_file_name(get_item_path(), new_dir_name, owner,x,y,aborter, logger);
                    if ( new_path == null)
                    {
                        if (dbg) logger.log("rename failed");
                        local_button.setText(original_name);
                        local_button.setGraphic(restored);
                        return;
                    }
                    set_new_path(new_path);
                    if ( local_label == null)
                    {
                        // the item is a Item_folder_with_icon
                        if (dbg) logger.log("rename done");
                        local_button.setText(new_dir_name);
                        local_button.setGraphic(restored);
                    }
                    else
                    {
                        // the item is a Item2_button
                        String size = Static_files_and_paths_utilities.get_1_line_string_for_byte_data_size(get_item_path().toFile().length(),owner,logger);
                        local_button.setText(size);
                        local_label.setText(new_dir_name);
                        //Font_size.set_preferred_font_size(label,logger);
                        Font_size.apply_global_font_size_to_Node(local_label,owner,logger);
                        local_button.setGraphic(local_label);
                    }
                    path_list_provider.reload();
                }

                if (dbg) logger.log("rename done");
                // button.setOnAction(the_button_event_handler);
            });
        },context_menu,owner,logger);
    }
}