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

import javafx.application.Application;
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
import klikr.audio.Audio_info_frame;
import klikr.audio.Ffmpeg_metadata_editor;
import klikr.path_lists.Path_list_provider_for_file_system;
import klikr.browser_core.icons.Icon_destination;
import klikr.browser_core.icons.Icon_factory_actor;
import klikr.browser_core.icons.Icon_factory_request;
import klikr.browser_core.virtual_landscape.Path_comparator_source;
import klikr.path_lists.Path_list_provider;
import klikr.browser_core.virtual_landscape.Selection_handler;
import klikr.images.Exif_stage;
import klikr.look.Font_size;
import klikr.look.Look_and_feel;
import klikr.look.Look_and_feel_manager;
import klikr.look.my_i18n.My_I18n;
import klikr.settings.Non_booleans_properties;
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
import org.jspecify.annotations.NonNull;

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
    protected final Application application;
    protected final Window_type window_type;

    //**********************************************************
    public Item(
            Application application,
            Window_type window_type,
            Scene scene,
            Selection_handler selection_handler,
            Icon_factory_actor icon_factory_actor,
            Color color,
            @NonNull  Path_list_provider path_list_provider,
            Path_comparator_source path_comparator_source,
            Window owner,
            Aborter aborter,
            Logger logger)
    //**********************************************************
    {
        this.application = application;
        this.window_type = window_type;
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
        if ( dbg) logger.log(("request_icon_to_factory for:"+get_item_path()));
        Icon_factory_request icon_factory_request = new Icon_factory_request(
                this, target_icon_size,owner,// aborter);
                new Aborter("Icon creation for "+get_item_path(),logger));


        if (icon_fabrication_requested.get())
        {
            logger.log("❗ Icon_factory_actor aborting-0, skipping icon request, as another one is in flight");
            return;
        }
        icon_fabrication_requested.set(true);

        icon_job = Actor_engine.run(icon_factory_actor, icon_factory_request, null,logger);

        if (dbg) logger.log("icon request : queued for: "+get_item_path());

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
        Path item_path = get_item_path();
        if (item_path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace(""));
            return context_menu;
        }
        if (Files.isDirectory(item_path))
        {
            Menu_items.add_menu_item_for_context_menu(
                    "Get_folder_size",true,null,
                    event -> Folder_size_stage.get_folder_size(item_path,owner, logger),
                    context_menu,owner,logger);

            if (is_trash())
            {
                Menu_items.add_menu_item_for_context_menu(
                    "Clear_Trash_Folder",true,null,
                event -> {
                        if (dbg) logger.log("clearing trash!");
                        Static_files_and_paths_utilities.clear_trash(true,owner, aborter,logger);
                    },
                    context_menu,owner,logger);
            }


            if (!is_trash() && (is_parent_of() == null))
            {
                // this is for FOLDERS
                {
                    Path target  = item_path.getParent();
                    if ( this instanceof Item_folder)
                    {
                        target  = item_path;
                    }
                    Path finalTarget = target;

                    Menu_items.add_menu_item_for_context_menu(
                            "Browse_in_new_window",true,
                            null,//(new KeyCodeCombination(KeyCode.N,KeyCombination.SHORTCUT_DOWN)).getDisplayText(),
                            event -> {
                                if (dbg) logger.log("Browse in new window!");
                                Window_builder.additional_no_past(application,Window_type.File_system_2D,new Path_list_provider_for_file_system(finalTarget,owner,logger), owner, logger);
                            }, context_menu, owner, logger);

                    if (Feature_cache.get(Feature.Enable_3D)) {
                        Menu_items.add_menu_item_for_context_menu(
                                "Browse_in_new_3D_window",true,
                                null,
                                event -> {
                                    if (dbg) logger.log("Browse in new window!");
                                    Window_builder.additional_no_past(application,Window_type.File_system_3D, new Path_list_provider_for_file_system(finalTarget, owner, logger), owner, logger);
                                }, context_menu, owner, logger);
                    }

                    Menu_items.add_menu_item_for_context_menu(
                            "Disk_View",true,
                            null,
                            event -> {
                                if (dbg) logger.log("Show disk view");
                                Window_builder.additional_no_past(application,Window_type.File_system_diskview, new Path_list_provider_for_file_system(finalTarget, owner, logger), owner, logger);
                            }, context_menu, owner, logger);
                }
                create_open_with_system_menu_item(item_path,context_menu);
                create_open_with_web_browser_menu_item(item_path,context_menu);
                // does not make sense for a folder create_open_with_registered_application_menu_item(item_path,context_menu);

                /*if (Feature_cache.get(Feature.Enable_tags))
                {
                    create_edit_tag_menu_item(get_item_path(), context_menu, dbg, owner, aborter, logger);
                }*/
                create_rename_menu_item(local_button, local_label, context_menu);

                create_delete_menu_item(context_menu);
                Menu_items.add_menu_item_for_context_menu("Copy",true,
                        null,//(new KeyCodeCombination(KeyCode.C,KeyCodeCombination.SHORTCUT_DOWN)).getDisplayText(),
                        event -> {
                        if (dbg) logger.log("Copying the directory");
                        Path new_path =  Static_files_and_paths_utilities.ask_user_for_new_dir_name(owner,item_path,logger);
                        if ( new_path == null)
                        {
                            Popups.popup_warning("❗ copy of dir failed","names are same ?", false,owner,logger);
                            return;
                        }
                        Static_files_and_paths_utilities.copy_dir_in_a_thread(owner, item_path, new_path, aborter, logger);
                    },
                        context_menu,owner,logger);

                create_edit_color_menu_item(context_menu);
            }
        }
        else
        {
            // this is for a FILE
            if (Guess_file_type.is_this_path_extension_an_image(item_path, owner, logger)) {
                create_open_exif_frame_menu_item(item_path, context_menu);
            }
            if (Guess_file_type.is_this_path_extension_a_music(item_path,logger)) {
                create_open_mediainfo_frame_menu_item(item_path, context_menu);
                create_edit_metadata_frame_menu_item(item_path, context_menu);
            }
            if (this.get_item_type() == Iconifiable_item_type.video) {
                Item_file_with_icon.make_menu_items_for_videos(item_path, owner, context_menu, dbg, aborter, logger);
            }

            // is a "plain" file
            Menu_items.add_menu_item_for_context_menu(
                    "Browse_in_new_window",true,
                    null,//(new KeyCodeCombination(KeyCode.N,KeyCombination.SHORTCUT_DOWN)).getDisplayText(),
                    event -> {
                        if (dbg) logger.log("Browse in new window!");
                        Scroll_position_cache.scroll_position_cache_write(path_list_provider.get_key(),item_path.toAbsolutePath().toString(),"Item Browse_in_new_window",logger);
                        Window_builder.additional_no_past(application,Window_type.File_system_2D,new Path_list_provider_for_file_system(item_path.getParent(),owner,logger), owner, logger);
                    }, context_menu, owner, logger);

            create_open_with_system_menu_item(item_path,context_menu);
            create_open_with_web_browser_menu_item(item_path,context_menu);
            create_open_with_registered_application_menu_item(item_path,context_menu);



            Menu_items.add_menu_item_for_context_menu("View_Text_Read_Only",true,null,
                    actionEvent -> {
                    if (dbg) logger.log("button in item: View_Text_Read_Only");

                    if (Feature_cache.get(Feature.Use_monaco_for_text_edition))
                    {
                        Javalin_monaco.read_only(application, item_path, logger);
                    }
                    else
                    {
                        Text_frame.show(item_path,logger);
                    }
                }, context_menu,owner,logger);

            Menu_items.add_menu_item_for_context_menu("Edit_Text",true,null,
                    actionEvent -> {
                        if (dbg) logger.log("button in item: Edit_Text");

                        if (Feature_cache.get(Feature.Use_monaco_for_text_edition))
                        {
                            Javalin_monaco.edit(application, item_path, logger);
                        }
                        else
                        {
                            Text_frame.show(item_path,logger);
                        }
                    }, context_menu,owner,logger);

            create_rename_menu_item(local_button, local_label,context_menu);

            create_copy_menu_item(context_menu);

            create_delete_menu_item(context_menu);

            Menu_items.create_show_file_size_menu_item(context_menu, item_path,owner,logger);


            /*if (Feature_cache.get(Feature.Enable_tags)) {
                create_edit_tag_menu_item(get_item_path(), context_menu,dbg, owner, aborter, logger);
            }*/
        }
        return context_menu;
    }

    //**********************************************************
    protected void create_delete_menu_item(ContextMenu context_menu)
    //**********************************************************
    {
        String menu_text = null;
        if ( path_list_provider instanceof Path_list_provider_for_file_system path_list_provider_for_file_system)
        {
            logger.log("pathlistprovider detected as FILE SYSTEM"+path_list_provider_for_file_system.get_key());
            menu_text = My_I18n.get_I18n_string("Delete",owner,logger);
        }
        if ( path_list_provider instanceof Path_list_provider_for_playlist path_list_provider_for_playlist)
        {
            menu_text = My_I18n.get_I18n_string("Remove_From_Playlist",owner,logger);
        }
        Menu_items.add_menu_item_for_context_menu(menu_text,false,
                null,//(new KeyCodeCombination(KeyCode.BACK_SPACE)).getDisplayText(),
                event -> {
                    if (dbg) logger.log("Deleting!");
                    double x = owner.getX()+100;
                    double y = owner.getY()+100;
                    Path item_path = get_item_path();
                    if(item_path != null)
                    {
                        path_list_provider.delete(item_path, owner, x, y, aborter, logger);
                    }
                },context_menu,owner,logger);
    }
    //**********************************************************
    public void create_copy_menu_item(ContextMenu context_menu)
    //**********************************************************
    {
        Menu_items.add_menu_item_for_context_menu("Copy",true,
                null,//(new KeyCodeCombination(KeyCode.C,KeyCodeCombination.SHORTCUT_DOWN)).getDisplayText(),
                event -> {
                if (dbg) logger.log("copying!");
                Path item_path = get_item_path();
                if (item_path == null)
                {
                    logger.log(Stack_trace_getter.get_stack_trace(""));
                    return;
                }

                Path new_path = Static_files_and_paths_utilities.ask_user_for_new_file_name(owner,item_path,logger);
                if ( new_path == null) return;
                try
                {
                    Files.copy(item_path, new_path);
                } catch (IOException e)
                {
                    logger.log("copy failed: could not create new file for: " + item_path.getFileName() + ", Exception:" + e);
                }
            }, context_menu,owner,logger);
    }

    //**********************************************************
    protected void create_show_file_size_menu_item(ContextMenu context_menu)
    //**********************************************************
    {
        Menu_items.add_menu_item_for_context_menu("Show_file_size",true,null,
                event -> {
                    Path item_path = get_item_path();
                    if (item_path == null)
                    {
                        logger.log(Stack_trace_getter.get_stack_trace(""));
                        return;
                    }
                    show_file_size(item_path, owner, logger);
                }, context_menu,owner,logger);
    }

    public static void show_file_size(Path path,Window owner, Logger logger)
    {
        if (dbg) logger.log("File length");
        String size_in_bytes = Static_files_and_paths_utilities.get_1_line_string_with_size(path,owner,logger);
        String message = My_I18n.get_I18n_string("File_size_for", owner,logger) +"\n"+ path.getFileName().toString();
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
        local_stage.setTitle(path.toAbsolutePath().toString());
        local_stage.setScene(scene);
        local_stage.show();

        logger.log("size_in_bytes->"+size_in_bytes+"<-");
        //b_.set_status(size_in_bytes);
    }

    //**********************************************************
    public void create_open_mediainfo_frame_menu_item(Path path, ContextMenu context_menu)
    //**********************************************************
    {
        Menu_items.add_menu_item_for_context_menu("Info_about",true,null,
                actionEvent -> {
            if (dbg) logger.log("info");
            Audio_info_frame.show(path,owner,logger);
        },context_menu,owner,logger);
    }
    //**********************************************************
    public void create_edit_metadata_frame_menu_item(Path path, ContextMenu context_menu)
    //**********************************************************
    {
        Menu_items.add_menu_item_for_context_menu(
                "Edit_Song_Metadata",true,null,
                (ActionEvent e) -> Ffmpeg_metadata_editor.edit_metadata_of_a_file_in_a_thread(path, owner, logger),
                context_menu, owner, logger);

    }


    //**********************************************************
    public void create_open_exif_frame_menu_item(Path path, ContextMenu context_menu)
    //**********************************************************
    {
        Menu_items.add_menu_item_for_context_menu("Info_about",true,null,
                actionEvent -> {
                    if (dbg) logger.log("info");
                    Actor_engine.execute(()-> {
                        if ( path != null) {
                            Image_and_properties iap = Full_image_from_disk.load_native_resolution_image_from_disk(path, true, owner, aborter, logger);
                            if (iap != null)
                                Exif_stage.show_exif_stage(iap.image(), path, owner, aborter, logger);
                        }
                    },"Show EXIF info",logger);
                },
                context_menu,
                owner,
                logger);

    }



    //**********************************************************
    public void create_open_with_registered_application_menu_item(Path path, ContextMenu context_menu)
    //**********************************************************
    {
        Menu_items.add_menu_item_for_context_menu(
                "Open_With_Registered_Application",true,null,
                actionEvent -> {
                    if (dbg) logger.log("button in item: Open_With_Registered_Application");
                    System_open_actor.open_with_registered_application(path, owner,aborter,logger);
                },context_menu,owner,logger);
    }

    //**********************************************************
    public void create_open_with_web_browser_menu_item(Path path, ContextMenu context_menu)
    //**********************************************************
    {
        Menu_items.add_menu_item_for_context_menu("Open_With_Web_Browser",true,null,
                actionEvent -> {
            if (dbg) logger.log("button in item: System Open");
            System_open_actor.open_with_web_browser(application,path, owner,aborter,logger);
        },context_menu,owner,logger);
    }

    //**********************************************************
    public void create_open_with_system_menu_item(Path path, ContextMenu context_menu)
    //**********************************************************
    {
        Menu_items.add_menu_item_for_context_menu("Open_With_System",true,null,
                actionEvent -> {
                    if (dbg) logger.log("button in item: System Open");
                    System_open_actor.open_with_system(application,path, owner,aborter,logger);
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
                Path item_path = get_item_path();
                if (item_path == null)
                {
                    logger.log(Stack_trace_getter.get_stack_trace(""));
                    return;
                }
                My_colors.save_color(item_path,my_color.java_name(),logger);
                double font_size = Non_booleans_properties.get_font_size(owner,logger);
                double icon_height = Look_and_feel.MAGIC_HEIGHT_FACTOR * font_size;
                if ( this instanceof Item_file_no_icon ifni)
                {
                    Look_and_feel_manager.set_button_look_as_folder(ifni.button, icon_height, color,owner,logger);
                }
                if ( this instanceof Item_folder itf)
                {
                    Look_and_feel_manager.set_button_look_as_folder(itf.button, icon_height, color,owner,logger);
                }
                if ( this instanceof Item_folder_with_icon itfwi)
                {
                    Look_and_feel_manager.set_button_look_as_folder(itfwi.button, icon_height, color,owner,logger);
                }
            }
        });
        menu.getItems().add(item);
        all_check_menu_items.add(item);

    }


    //**********************************************************
    public void is_in_selection_for_moving(boolean selected)
    //**********************************************************
    {
        set_selected(selected);
        if ( selected )
        {
            Path item_path = get_item_path();
            if (item_path == null) {
                logger.log(Stack_trace_getter.get_stack_trace(""));
                return;
            }
            if (selection_handler.add_to_selected_for_moving(item_path)) {
                logger.log("item selected:" + get_item_path());
            }
        }
    }

    //**********************************************************
    protected void give_a_menu_to_the_button(Button local_button, Label details)
    //**********************************************************
    {
        //logger.log(Stack_trace_getter.get_stack_trace("give_a_menu_to_the_button "));
        local_button.setOnContextMenuRequested((ContextMenuEvent event) -> {
            if ( dbg)
            {
                Path item_path = get_item_path();
                if (item_path == null)
                {
                    logger.log(Stack_trace_getter.get_stack_trace(""));
                    return;
                }
                logger.log("show context menu of button:"+ item_path.toAbsolutePath());
            }
            ContextMenu context_menu = make_context_menu(local_button, details);
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
        if( has_icon())
            request_icon_to_factory(get_icon_size());
    }


    // this is called SUPER intensively when scrolling
    //**********************************************************
    public void process_is_visible(double current_vertical_offset)
    //**********************************************************
    {
        if ( !Platform.isFxApplicationThread())
        {
            logger.log(Stack_trace_getter.get_stack_trace("HAPPENS1 process_is_visible"));
            Platform.runLater(()->process_is_visible(current_vertical_offset));
        }

        set_translate_X(get_javafx_x());
        set_translate_Y(get_javafx_y() - current_vertical_offset);

        // this is essential: dont call you_are_visible() unless needed
        if (!visible_in_scene.get())
        {
            visible_in_scene.set(true);
            you_are_visible();
            set_selected_look_specific(is_selected.get());
        }
    }

    //**********************************************************
    public void process_is_invisible(double current_vertical_offset)
    //**********************************************************
    {
        //if ( !Platform.isFxApplicationThread()) logger.log(Stack_trace_getter.get_stack_trace("PANIC"));

        //WTF set_translate_X(get_javafx_x());
        //WTF set_translate_Y(get_javafx_y() - current_vertical_offset);

        //if ( !Platform.isFxApplicationThread())logger.log(Stack_trace_getter.get_stack_trace("PANIC process_is_invisible "+Platform.isFxApplicationThread()));
        // do the shift only once as you_are_invisible is costly
        if (visible_in_scene.get())
        {
            visible_in_scene.set(false);
            you_are_invisible();
        }
    }

    //**********************************************************
    protected void create_rename_menu_item(Button local_button, Label local_label, ContextMenu context_menu)
    //**********************************************************
    {
        Menu_items.add_menu_item_for_context_menu("Rename",true,
                null,//(new KeyCodeCombination(KeyCode.R)).getDisplayText(),

                event -> {
            if (dbg) logger.log("Item: Renaming");
            Path item_path = get_item_path();
            if (item_path == null)
            {
                logger.log(Stack_trace_getter.get_stack_trace(""));
                return;
            }
            String original_name = item_path.getFileName().toString();
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
                Path item_path_ = get_item_path();
                if (item_path_ == null)
                {
                    logger.log(Stack_trace_getter.get_stack_trace("Should not happen "));
                    return;
                }
                if ( item_path_.toFile().isDirectory() )
                {
                    Path new_path = Moving_files.change_dir_name(item_path_, new_item_name, owner, aborter, logger);
                    if ( new_path == null)
                    {
                        if (dbg) logger.log("rename failed");
                        local_button.setText(original_name);
                        local_button.setGraphic(restored);
                        return;
                    }
                    local_button.setText(new_item_name);
                    local_button.setGraphic(restored);
                    path_list_provider = Path_list_provider.get_appropriate(window_type,new_path,owner,aborter,logger);//new Path_list_provider_for_file_system(new_path,owner,logger);
                }
                else
                {
                    // item is a file
                    if ( path_list_provider instanceof Path_list_provider_for_playlist plpfpl)
                    {
                        Path new_path = Paths.get(item_path_.getParent().toString(), new_item_name);
                        plpfpl.swap(item_path_.toAbsolutePath().toString(),new_path.toAbsolutePath().toString());
                    }
                    double x = owner.getX()+100;
                    double y = owner.getY()+100;
                    Path new_path = Static_files_and_paths_utilities.change_file_name(item_path_, new_item_name, owner,x,y,aborter, logger);
                    if ( new_path == null)
                    {
                        logger.log("❗ rename failed");
                        local_button.setText(original_name);
                        local_button.setGraphic(restored);
                        return;
                    }
                    set_new_path(new_path);
                    if ( local_label == null)
                    {
                        // TODO: verify this ???
                        // the item is a Item_folder_with_icon
                        if (dbg) logger.log("rename done");
                        local_button.setText(new_item_name);
                        local_button.setGraphic(restored);
                    }
                    else
                    {
                        String size = Static_files_and_paths_utilities.get_1_line_string_for_byte_data_size(item_path_.toFile().length(),owner,logger);
                        local_button.setText(size);
                        local_label.setText(new_item_name);
                        //Font_size.set_preferred_font_size(label,logger);
                        Font_size.apply_global_font_size_to_Node(local_label,owner,logger);
                        local_button.setGraphic(local_label);
                    }

                    //no need the disk change will cause  the browser to redraw with a force reload
                    // path_list_provider.reload("file name changed: "+new_item_name, aborter);
                }

                if (dbg) logger.log("rename done");
            });
        },context_menu,owner,logger);
    }
}