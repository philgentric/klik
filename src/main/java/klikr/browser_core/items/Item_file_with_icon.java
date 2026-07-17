// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ../../images/decoding/Fast_rotation_from_exif_metadata_extractor.java
//SOURCES ../../experimental/work_in_progress/Multiple_image_window.java
//SOURCES ../../image_ml/image_similarity/Similarity_engine.java
//SOURCES ./Item_file.java

package klikr.browser_core.items;

import javafx.event.ActionEvent;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Window;
import klikr.Window_builder;
import klikr.Window_type;
import klikr.util.Check_remaining_RAM;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Actor_engine;
import klikr.browser_core.Drag_and_drop;
import klikr.browser_core.Image_and_properties;
import klikr.path_lists.Path_list_provider_for_file_system;
import klikr.browser_core.icons.Icon_factory_actor;
import klikr.util.animated_gifs.Animated_gifs_from_video;
import klikr.util.animated_gifs.Ffmpeg_utils;
import klikr.browser_core.icons.image_properties_cache.Rotation;
import klikr.browser_core.virtual_landscape.Path_comparator_source;
import klikr.path_lists.Path_list_provider;
import klikr.browser_core.virtual_landscape.Selection_handler;
import klikr.machine_learning.feature_vector.Feature_vector_cache;
import klikr.machine_learning.similarity.Similarity_engine;
import klikr.images.Image_window;
import klikr.util.image.decoding.Fast_rotation_from_exif_metadata_extractor;
import klikr.look.Look_and_feel_manager;
import klikr.look.my_i18n.My_I18n;
import klikr.settings.boolean_features.Feature;
import klikr.settings.boolean_features.Feature_cache;
import klikr.util.execute.System_open_actor;
import klikr.util.files_and_paths.*;
import klikr.change.old_and_new.Command;
import klikr.change.old_and_new.Old_and_new_Path;
import klikr.change.old_and_new.Status;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;
import klikr.util.ui.Jfx_batch_injector;
import klikr.util.ui.Menu_items;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;


//**********************************************************
public class Item_file_with_icon extends Item_file
//**********************************************************
{
    private final Button button;
    protected ImageView image_view;
    Pane image_pane;
    public Double aspect_ratio;
    public static Image default_icon;
    private final Supplier<Feature_vector_cache> fv_cache_supplier;

    //**********************************************************
    public Item_file_with_icon(
            Item_context item_context,
            Selection_handler selection_handler,
            Icon_factory_actor icon_factory_actor,
            Double aspect_ratio,
            Supplier<Feature_vector_cache> fv_cache_supplier)

    //**********************************************************
    {
        super(
                item_context,
                selection_handler,
                icon_factory_actor);
        this.aspect_ratio = aspect_ratio;
        //this.image_properties_RAM_cache = image_properties_RAM_cache;
        this.fv_cache_supplier = fv_cache_supplier;
        double actual_icon_size = icon_size / 3.0;
        if ( default_icon == null)
        {
            default_icon = Look_and_feel_manager.get_default_icon(actual_icon_size, item_context.owner, item_context.logger);
            if ( default_icon == null)
            {
                item_context.logger.log("FATAL: Default image not found ");
            }
        }

        // first time
        image_view = new ImageView();
        image_view.setPickOnBounds(true); // allow click on transparent areas
        if (Feature_cache.get(Feature.Show_file_names_as_tooltips))
        {
            Tooltip.install(image_view, new Tooltip(item_context.item_path.getFileName().toString()));
        }
        image_pane = new StackPane(image_view);
        button = new Button();
        button.setGraphic(image_pane);
        button.setStyle("-fx-padding: 0; -fx-background-insets: 0; -fx-border-insets: 0;");

        if ( dbg)
            item_context.logger.log("item_file_with_icon: loading default icon in the image view, w=" +default_icon.getWidth()+", h="+default_icon.getHeight()+" FOR:  "+item_context.item_path);
        image_view.setPreserveRatio(true);
        image_view.setSmooth(true);
        image_view.setFitWidth(actual_icon_size);
        image_view.setFitHeight(actual_icon_size);
        image_view.setCache(false);
        //image_view.setCacheHint(CacheHint.SPEED);


        Drag_and_drop.init_drag_and_drop_sender_side(get_Node(),selection_handler,item_context.item_path,item_context.logger);

        button.setOnContextMenuRequested((ContextMenuEvent event) -> {
            Path p = get_item_path();
            if ( p == null)
            {
                item_context.logger.log(Stack_trace_getter.get_stack_trace(""));
                return;
            }
            if ( dbg) item_context.logger.log("show context menu of image_view:"+ p.toAbsolutePath());
            ContextMenu context_menu = make_context_menu();
            context_menu.show(button, event.getScreenX(), event.getScreenY());
        });


        //give_a_menu_to_the_button(button,new Label("toto"));
        button.setOnAction(event -> {
            on_mouse_clicked();
            event.consume();
        });

    }


    @Override // Icon_destination
    public Path get_item_path() {
        return item_context.item_path;
    }

    //**********************************************************
    private void on_mouse_clicked()
    //**********************************************************
    {
        selection_handler.reset_selection(); // will clear all selections
        if ( item_context.item_path == null)
        {
            item_context.logger.log(Stack_trace_getter.get_stack_trace(""));
            return;
        }
        if ( Guess_file_type.is_this_path_extension_an_image(item_context.item_path,item_context.owner,item_context.logger))
        {
            open_an_image(item_context.path_list_provider,item_context.path_comparator_source,item_context.item_path,item_context.owner,item_context.logger);
        }
        else
        {
            System_open_actor.open_with_system(item_context.application,item_context.item_path, item_context.owner,item_context.aborter,item_context.logger);
        }
    }

    //**********************************************************
    @Override // Item
    public void set_selected_look_specific(boolean selected)
    //**********************************************************
    {
        if (selected)
        {
            double DELTA = icon_size / 2.0;
            Rectangle2D r = new Rectangle2D(-DELTA, -DELTA, image_view.getFitWidth() + DELTA, image_view.getFitHeight() + DELTA);
            image_view.setViewport(r);
        }
        else
        {
            //if ( image_view != null)
            image_view.setViewport(null);
        }
    }

    //**********************************************************
    public static void open_an_image(
            Path_list_provider path_list_provider,
            Path_comparator_source path_comparator_source,
            Path path,
            Window owner,
            Logger logger)
    //**********************************************************
    {
        Image_window.get_Image_window(path, path_list_provider,path_comparator_source, owner,new Aborter("Image_viewer",logger),logger);
        if ( dbg) logger.log("\n\n✅ Image_stage opening (same process) for path:" + path.toString());
    }

    @Override // Item
    public Path get_path_for_display(boolean try_deep) {
        return get_item_path();
    }

    @Override // Icon_destination
    public Path get_path_for_display_icon_destination() {
        return get_item_path();
    }




    //**********************************************************
    public ContextMenu make_context_menu()
    //**********************************************************
    {
        //logger.log(Stack_trace_getter.get_stack_trace("Item_file_with_icon make_context_menu"));

        ContextMenu context_menu = new ContextMenu();
        Look_and_feel_manager.set_context_menu_look(context_menu,item_context.owner,item_context.logger);

        if ( item_context.item_path == null)
        {
            item_context.logger.log(Stack_trace_getter.get_stack_trace(""));
            return context_menu;
        }

        double x = item_context.owner.getX()+100;
        double y = item_context.owner.getY()+100;
        create_open_exif_frame_menu_item(context_menu,item_context);

        if ( Feature_cache.get(Feature.Enable_image_similarity))
        {
            if (!Check_remaining_RAM.low_memory.get()) {
                context_menu.getItems().add(create_show_similar_menu_item(
                        item_context.item_path,
                        fv_cache_supplier,
                        item_context.path_comparator_source,
                        item_context.owner,
                        item_context.aborter,
                        item_context.logger));
            }
        }

        {
            Menu menu = get_open_Menu(item_context.item_path,item_context.owner,x, y, item_context.aborter,item_context.logger);
            context_menu.getItems().add(menu);
        }

        {
            MenuItem menu_item = get_rename_MenuItem(item_context.item_path,item_context.owner,x, y, item_context.aborter,item_context.logger);
            context_menu.getItems().add(menu_item);
        }

        create_delete_menu_item(context_menu,item_context);
        create_copy_menu_item(context_menu,item_context);
        create_show_file_size_menu_item(context_menu,item_context);


        if ( this.item_type == Iconifiable_item_type.video)
        {
            make_menu_items_for_videos(item_context.item_path,item_context.owner,context_menu,dbg, item_context.aborter,item_context.logger);
        }
        return context_menu;

    }

    //**********************************************************
    private Menu get_open_Menu(Path path, Window owner, double x, double y, Aborter aborter, Logger logger)
    //**********************************************************
    {
        String s = My_I18n.get_I18n_string("Open", owner, logger);
        Menu returned = new Menu(s);

        {
            MenuItem mi = Menu_items.make_menu_item(
                    "Open_With_Registered_Application",true,
            null,
                    event -> {
                        if (dbg) logger.log("Opening with registered app: "+path);
                        System_open_actor.open_with_registered_application(path, owner,aborter,logger);
                    },
                    owner, logger);
            returned.getItems().add(mi);
        }


        {
            MenuItem mi = Menu_items.make_menu_item(
                    "Open_With_System",true,
                    null,
                    event -> {
                        if (dbg) logger.log("Opening with system: "+path);
                        System_open_actor.open_with_system(item_context.application,path, owner,aborter,logger);
                    },
                    owner, logger);
            returned.getItems().add(mi);
        }
       {
            MenuItem mi = Menu_items.make_menu_item(
                    "Browse_in_new_window",true,
                    null,//(new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN)).getDisplayText(),
                    (ActionEvent e) ->
                            Window_builder.additional_no_past(item_context.application,Window_type.File_system_2D, new Path_list_provider_for_file_system(path.getParent(), owner, logger), owner, logger),
                    owner, logger);
            returned.getItems().add(mi);
        }
        if (Feature_cache.get(Feature.Enable_3D))
        {
            MenuItem mi = Menu_items.make_menu_item(
                            "Browse_in_new_3D_window",true,
                    null,
                            event -> {
                                if (dbg) logger.log("Browse in new window!");
                                Window_builder.additional_no_past(item_context.application,Window_type.File_system_3D, new Path_list_provider_for_file_system(path.getParent(), owner, logger), owner, logger);
                            }, owner, logger);
            returned.getItems().add(mi);
        }
        return returned;
    }

    //**********************************************************
    public static MenuItem create_show_similar_menu_item(
            Path image_path,
            Supplier<Feature_vector_cache> fv_cache_supplier,
            Path_comparator_source path_comparator_source,
            Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        String txt = My_I18n.get_I18n_string("Show_5_similar_images", owner,logger);
        MenuItem menu_item = new MenuItem(txt);
        Look_and_feel_manager.set_menu_item_look(menu_item,owner,logger);
        menu_item.setOnAction(actionEvent -> {
            if (dbg) logger.log("✅ show similar");
            Runnable r = () ->
            {
                double x = owner.getX()+100;
                double y = owner.getY()+100;
                Path_list_provider path_list_provider = new Path_list_provider_for_file_system(image_path.getParent(),owner,logger);
                List<Path> paths =  path_list_provider.only_image_paths(true, Feature_cache.get(Feature.Show_hidden_files),aborter);
                Similarity_engine image_similarity = new Similarity_engine(
                        paths,
                        path_list_provider,
                        path_comparator_source,
                        owner,
                        aborter,logger);
                image_similarity.find_similars_special(
                        true,
                        null,
                        image_path,
                        null,
                        5,
                        true,
                        Double.MAX_VALUE, // MAGIC
                        fv_cache_supplier,
                        owner, x,y,null,aborter);
            };
            Actor_engine.execute(r,"Find and display similar pictures",logger);
        });

        return menu_item;
    }


    //**********************************************************
    public static MenuItem get_rename_MenuItem(Path path, Window owner, double x, double y, Aborter browser_aborter, Logger logger)
    //**********************************************************
    {
        MenuItem menu_item = new MenuItem(My_I18n.get_I18n_string("Rename", owner,logger)+ " "+path.getFileName());
        Look_and_feel_manager.set_menu_item_look(menu_item,owner,logger);
        menu_item.setMnemonicParsing(false);
        menu_item.setOnAction(event -> {
            if (dbg) logger.log("✅ item_file_with_icon: Renaming "+path);

            Path new_path =  Static_files_and_paths_utilities.ask_user_for_new_file_name(owner,path,logger);
            if ( new_path == null) return;

            List<Old_and_new_Path> l = new ArrayList<>();
            Old_and_new_Path oandn = new Old_and_new_Path(path, new_path, Command.command_rename, Status.before_command,false);
            l.add(oandn);
            Moving_files.perform_safe_moves_in_a_thread(l, true, x,y,owner,browser_aborter, logger);
        });
        return menu_item;
    }


    //**********************************************************
    public static void make_menu_items_for_videos(
            Path path, 
            Window owner,
            ContextMenu context_menu, boolean dbg, Aborter aborter, Logger logger)
    //**********************************************************
    {
        Menu_items.add_menu_item_for_context_menu("Convert_To_Mp4",true,null,
    event -> {
            if (dbg) logger.log("✅ convert to mp4");
            AtomicBoolean abort_reported = new AtomicBoolean(false);
            Ffmpeg_utils.video_to_mp4_in_a_thread(path,aborter, abort_reported, owner,logger);
            },
            context_menu,owner,logger);
        Menu_items.add_menu_item_for_context_menu("Generate_many_animated_GIFs",true,null,
                    event -> {
                if (dbg) logger.log("✅ Generating animated gifs !");
                Animated_gifs_from_video.generate_many_gifs(path,5,5,owner,logger);
            }, context_menu,owner,logger);
        Menu_items.add_menu_item_for_context_menu("Generate_Animated_GIF_interactively",true,null,
                event -> {
                if (dbg) logger.log("✅ Generating animated gifs interactively!");
                Animated_gifs_from_video.interactive(path,logger);
            },context_menu,owner,logger);
    }

    @Override
    public double get_Width()
    {
        return icon_size;
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
    public void receive_icon(Image_and_properties iap)
    //**********************************************************
    {
        if ( dbg) item_context.logger.log("ITEM FILE WITH ICON RECEIVING icon");
        // this is NOT on the FX thread
        if ( image_view == null)
        {
            item_context.logger.log(Stack_trace_getter.get_stack_trace("❗ image_view == null"));
            return;
        }

        if (!visible_in_scene.get())
        {
            // this happen if between the time the icon was request and now,
            // the item is not visible anymore typically because the user scrolled away
            if ( dbg)
                item_context.logger.log("❗ visible_in_scene.get() : calling you_are_invisible");
            Jfx_batch_injector.inject(this::you_are_invisible,item_context.logger);
            return;
        }
        if ( iap == null)
        {
            if ( dbg)
                item_context.logger.log("❗ image_and_rotation == null ");
            //Jfx_batch_injector.inject(() -> you_are_invisible(),logger);
            return;
        }
        if ( iap.properties() == null)
        {
            if ( dbg)
                item_context.logger.log("❗ image_and_rotation.properties() == null");
            //Jfx_batch_injector.inject(() -> you_are_invisible(),logger);
            return;
        }
        if ( iap.image() == null)
        {
            if ( dbg)
                item_context.logger.log("❗ image_and_rotation.image() == null");
            //Jfx_batch_injector.inject(() -> you_are_invisible(),logger);
            return;
        }


        if ( (iap.image().getHeight()  < 1) || (iap.image().getWidth() < 1))
        {
            item_context.logger.log(Stack_trace_getter.get_stack_trace("❗ WARNING: empty image, not set "+item_context.item_path.toAbsolutePath()));
            Jfx_batch_injector.inject(this::you_are_invisible,item_context.logger);
            return;
        }
        Jfx_batch_injector.inject(() -> receive_icon_in_fx_thread(iap),item_context.logger);
    }

    //**********************************************************
    @Override
    public boolean has_icon()
    //**********************************************************
    {
        return true;
    }

    //**********************************************************
    public void receive_icon_in_fx_thread(Image_and_properties image_and_properties)
    //**********************************************************
    {
        if ( dbg)
        {
            if ( image_and_properties.image() ==null)
            {
                item_context.logger.log(Stack_trace_getter.get_stack_trace("❌ FATAL receive_icon_in_fx_thread image_and_properties.image() ==null, for: "+get_item_path()));
                return;
            }
            if ( image_and_properties.properties() ==null)
            {
                item_context.logger.log(Stack_trace_getter.get_stack_trace("❌ FATAL receive_icon_in_fx_thread image_and_properties.properties() ==null, for: "+get_item_path()));
                return;
            }
            item_context.logger.log("✅ receive_icon_in_fx_thread," +
                    "\n   w icon=          "+image_and_properties.image().getWidth()+
                    "\n   h icon=          "+image_and_properties.image().getHeight()+
                    "\n   w image=         "+image_and_properties.properties().w()+
                    "\n   h image=         "+image_and_properties.properties().h()+
                    "\n   rot image=       "+image_and_properties.properties().rotation()+
                    "\n   aspect ratio=    "+image_and_properties.properties().get_aspect_ratio()+
                    "\n   for:             "+get_item_path());

        }

        Rotation rotation = null;
        {
            rotation = image_and_properties.properties().rotation();
            if (rotation == null)
            {
                Path p = get_item_path();
                if (p != null)
                {
                    if (Files.exists(p)) {
                        if ((Guess_file_type.is_this_path_extension_a_video(p, item_context.logger))
                        ||
                        (Guess_file_type.is_this_path_extension_a_pdf(p, item_context.logger)))
                        {
                            if (dbg) item_context.logger.log("✅ PDF or video => rot=0");
                            rotation = Rotation.normal;
                        }
                        else
                        {
                            rotation = Fast_rotation_from_exif_metadata_extractor.get_rotation(p, true, item_context.owner, item_context.aborter, item_context.logger);
                            if( rotation == null) rotation = Rotation.normal;
                        }
                    } else {
                        item_context.logger.log(Stack_trace_getter.get_stack_trace("❌ Bad"));
                        you_are_invisible();
                        return;
                    }
                }

            }
        }
        if ( item_type == Iconifiable_item_type.pdf)
        {
            if (aspect_ratio == null)
            {
                item_context.logger.log("❌ SHOULD NOT HAPPEN");
                double local = image_and_properties.image().getWidth()/image_and_properties.image().getHeight();
                if( dbg) item_context.logger.log(Stack_trace_getter.get_stack_trace("setting aspect ratio for PDF from icon: "+ local));
                aspect_ratio = (Double) local;
            }
        }
        // the above operation can take some time...
        // and in the mean time the situation can change
        if (!visible_in_scene.get())
        {
            you_are_invisible();
            return;
        }

        image_view.setSmooth(true);
        if ( dbg) item_context.logger.log("Setting icon !!"+image_and_properties.image().getWidth()+"x"+image_and_properties.image().getHeight());
        image_view.setImage(image_and_properties.image());

        if (( image_and_properties.image().getHeight() >= icon_size) && (image_and_properties.image().getWidth() >= icon_size))
        {
            // this happens when the icon is PDF as we dont scale PDF icons
            if (dbg) item_context.logger.log("✅ icon larger than target HAPPENS1 for: "+get_item_path());
            image_view.setFitWidth(icon_size);
            image_view.setFitHeight(icon_size);
            if ((rotation == Rotation.rot_90_clockwise) || (rotation == Rotation.rot_90_anticlockwise))
            {
                // this actually NEVER HAPPENS now since a PDF icon is never rotated
                //if (dbg)
                item_context.logger.log("❌ HAPPENS2 for: "+get_item_path());
                image_view.setFitWidth(image_and_properties.image().getHeight());
                image_view.setFitHeight(image_and_properties.image().getWidth());
            }
        }
        else
        {
            if ((rotation == Rotation.rot_90_clockwise) || (rotation == Rotation.rot_90_anticlockwise))
            {

                if ( image_and_properties.image().getHeight() < image_and_properties.image().getWidth())
                {
                    if (dbg)
                        item_context.logger.log("✅ HAPPENS3A for: "+get_item_path());
                    image_view.setFitWidth(icon_size);
                    image_view.setFitHeight(-1);
                }
                else
                {
                    // this happens rarely as it is an image that is rotated AND wider than high after rotation
                    //(most of the rotated images are portrait shot by turning the camera
                    if (dbg)
                        item_context.logger.log("✅ HAPPENS3B for: "+get_item_path());
                    image_view.setFitWidth(-1);
                    image_view.setFitHeight(icon_size);
                }
            }
            else
            {
                if (dbg)
                    item_context.logger.log("✅ HAPPENS4 for: "+get_item_path());
                image_view.setFitWidth(image_and_properties.image().getWidth());
                image_view.setFitHeight(image_and_properties.image().getHeight());
            }
        }
        if ( image_and_properties.properties().rotation() != null) {
            image_pane.setRotate(image_and_properties.properties().rotation().as_double());
        }
        else
        {
            if ( dbg) item_context.logger.log("❗ image_and_rotation.rotation() is null");
        }
    }

    //**********************************************************
    @Override // Item
    public int get_icon_size()
    //**********************************************************
    {
        return icon_size;
    }

    //**********************************************************
    @Override // Item
    public void you_are_visible_specific()
    //**********************************************************
    {
        //logger.log("item_file_with_icon::you_are_visible_specific "+get_item_path());
        if ( default_icon == null)
        {
            item_context.logger.log("❌ BAD WARNING: item_file_with_icon: default_icon == null");
            return;
        }

        image_view.setImage(default_icon);

    }
    //**********************************************************
    @Override // Item
    public void you_are_invisible_specific()
    //**********************************************************
    {
        image_view.setImage(null);
    }

    //**********************************************************
    private void log_visibility_state_number(int i)
    //**********************************************************
    {
        get_logger().log(get_item_path()+" visibility state #" + i);
    }


    //**********************************************************
    @Override
    public Node get_Node()
    //**********************************************************
    {
        return button;
    }

    //**********************************************************
    @Override
    public String get_string()
    //**********************************************************
    {

        if ( get_item_path() == null ) return "item_file_with_icon NP PATH ?";
        return "is item_file_with_icon for : " + get_item_path().toAbsolutePath();
    }
}