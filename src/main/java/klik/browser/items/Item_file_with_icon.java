// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ../../images/decoding/Fast_rotation_from_exif_metadata_extractor.java
//SOURCES ../../experimental/work_in_progress/Multiple_image_window.java
//SOURCES ../../image_ml/image_similarity/Similarity_engine.java
//SOURCES ./Item_file.java

package klik.browser.items;

import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Window;
import klik.util.execute.actor.Aborter;
import klik.util.execute.actor.Actor_engine;
import klik.browser.Drag_and_drop;
import klik.browser.Image_and_properties;
import klik.path_lists.Path_list_provider_for_file_system;
import klik.browser.icons.Icon_factory_actor;
import klik.util.animated_gifs.Animated_gifs_from_video;
import klik.util.animated_gifs.Ffmpeg_utils;
import klik.browser.icons.image_properties_cache.Rotation;
import klik.browser.virtual_landscape.Path_comparator_source;
import klik.path_lists.Path_list_provider;
import klik.browser.virtual_landscape.Selection_handler;
import klik.machine_learning.feature_vector.Feature_vector_cache;
import klik.machine_learning.similarity.Similarity_engine;
import klik.images.Image_window;
import klik.util.image.decoding.Fast_rotation_from_exif_metadata_extractor;
import klik.look.Look_and_feel_manager;
import klik.look.my_i18n.My_I18n;
import klik.properties.boolean_features.Feature;
import klik.properties.boolean_features.Feature_cache;
import klik.util.execute.System_open_actor;
import klik.util.files_and_paths.*;
import klik.util.files_and_paths.old_and_new.Command;
import klik.util.files_and_paths.old_and_new.Old_and_new_Path;
import klik.util.files_and_paths.old_and_new.Status;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import klik.util.ui.Jfx_batch_injector;
import klik.util.ui.Menu_items;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;


//**********************************************************
public class Item_file_with_icon extends Item_file
//**********************************************************
{
    private Button button;
    protected ImageView image_view;
    Pane image_pane;
    public Double aspect_ratio;
    public static Image default_icon;
    //private final Image_properties_RAM_cache image_properties_RAM_cache;
    private final Supplier<Feature_vector_cache> fv_cache_supplier;

    //**********************************************************
    public Item_file_with_icon(
            Scene scene,
            Selection_handler selection_handler,
            Icon_factory_actor icon_factory_actor,
            Color color,
            Double aspect_ratio,
            //Image_properties_RAM_cache image_properties_RAM_cache,
            Supplier<Feature_vector_cache> fv_cache_supplier,
            Path path_,
            Path_list_provider path_list_provider,
            Path_comparator_source path_comparator_source,
            Window owner,
            Aborter aborter,
            Logger logger)

    //**********************************************************
    {
        super(
                scene,
                selection_handler,
                icon_factory_actor,
                color,
                path_,
                path_list_provider,
                path_comparator_source,
                owner,
                aborter,
                logger);
        this.aspect_ratio = aspect_ratio;
        //this.image_properties_RAM_cache = image_properties_RAM_cache;
        this.fv_cache_supplier = fv_cache_supplier;
        double actual_icon_size = icon_size / 3.0;
        if ( default_icon == null) default_icon = Look_and_feel_manager.get_default_icon(actual_icon_size,owner,logger);

        // first time
        image_view = new ImageView();
        image_view.setPickOnBounds(true); // allow click on transparent areas
        if (Feature_cache.get(Feature.Show_file_names_as_tooltips))
        {
            Tooltip.install(image_view, new Tooltip(path.getFileName().toString()));
        }
        image_pane = new StackPane(image_view);
        button = new Button();
        button.setGraphic(image_pane);
        button.setStyle("-fx-padding: 0; -fx-background-insets: 0; -fx-border-insets: 0;");

        if ( dbg)
            logger.log("item_file_with_icon: loading default icon in the image view, w=" +default_icon.getWidth()+", h="+default_icon.getHeight()+" FOR:  "+path);
        image_view.setPreserveRatio(true);
        image_view.setSmooth(true);
        image_view.setFitWidth(actual_icon_size);
        image_view.setFitHeight(actual_icon_size);
        image_view.setCache(false);
        //image_view.setCacheHint(CacheHint.SPEED);


        Drag_and_drop.init_drag_and_drop_sender_side(get_Node(),selection_handler,path,logger);

        ContextMenu context_menu = make_context_menu();
        button.setOnContextMenuRequested((ContextMenuEvent event) -> {
            //if ( dbg)
            logger.log("show context menu of image_view:"+ get_item_path().toAbsolutePath());
            context_menu.show(button, event.getScreenX(), event.getScreenY());
        });


        //give_a_menu_to_the_button(button,new Label("toto"));
        button.setOnAction(event -> {
            on_mouse_clicked(logger);
            event.consume();
        });

    }


    @Override
    void set_new_path(Path newPath) {
        path = newPath;
    }

    @Override
    public Path get_item_path() {
        return path;
    }

    //**********************************************************
    private void on_mouse_clicked(Logger logger)
    //**********************************************************
    {
        selection_handler.reset_selection(); // will clear all selections

        if ( Guess_file_type.is_this_path_an_image(get_item_path(),owner,logger))
        {
            open_an_image(path_list_provider,path_comparator_source,get_item_path(),owner,logger);
        }
        else
        {
            System_open_actor.open_with_system(get_item_path(), owner,aborter,logger);
        }
    }

    //**********************************************************
    @Override // Item
    public void set_is_unselected_internal()
    {
        if ( image_view != null) image_view.setViewport(null);
    }
    //**********************************************************
    @Override // Item
    public void set_is_selected_internal()
    //**********************************************************
    {
       double DELTA = icon_size/2.0;
       Rectangle2D r = new Rectangle2D(-DELTA, -DELTA, image_view.getFitWidth() + DELTA, image_view.getFitHeight() + DELTA);
       image_view.setViewport(r);
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
        Comparator<Path> x = path_comparator_source.get_path_comparator();
        Optional<Comparator<Path>> image_comparator = Optional.empty();
        if ( x == null)
        {
            logger.log("❗ WARNING: Comparator is null");
        }
        else
        {
            image_comparator = Optional.of(x);
        }
        Image_window.get_Image_window(path, path_list_provider,image_comparator, owner,new Aborter("Image_viewer",logger),logger);
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
    @Override
    public boolean is_trash()
    //**********************************************************
    {
        return false;
    }

    //**********************************************************
    @Override
    public Path is_parent_of()
    //**********************************************************
    {
        return null;
    }

    //**********************************************************
    public ContextMenu make_context_menu()
    //**********************************************************
    {
        ContextMenu context_menu = new ContextMenu();
        Look_and_feel_manager.set_context_menu_look(context_menu,owner,logger);

        double x = owner.getX()+100;
        double y = owner.getY()+100;
        create_open_exif_frame_menu_item(get_item_path(),context_menu);

        context_menu.getItems().add(create_show_similar_menu_item(
                get_item_path(),
                //image_properties_RAM_cache,
                fv_cache_supplier,
                path_comparator_source,
                owner,
                aborter,
                logger));


        {
            MenuItem menu_item = get_rename_MenuItem(get_item_path(),owner,x, y, aborter,logger);
            context_menu.getItems().add(menu_item);
        }

        create_delete_menu_item(context_menu);
        create_copy_menu_item(context_menu);
        create_show_file_size_menu_item(context_menu);
        /*
        Menu_items.add_menu_item("Delete",
                    event -> {
                if (dbg) logger.log("Deleting "+get_item_path());
                path_list_provider.delete(get_item_path(),owner,x,y, aborter,logger);
            },context_menu,owner,logger);
        */
        Menu_items.add_menu_item_for_context_menu("Edit",
                    event -> {
                if (dbg) logger.log("Editing "+get_item_path());
                System_open_actor.open_with_system(get_item_path(), owner,aborter,logger);
            },context_menu,owner,logger);

        Menu_items.add_menu_item_for_context_menu("Open_With_Registered_Application",
                    event -> {
                if (dbg) logger.log("Opening with registered app: "+get_item_path());
                System_open_actor.open_special(get_item_path(), owner,aborter,logger);
            },context_menu,owner,logger);

        {
            create_show_file_size_menu_item(context_menu);
            /*if (Feature_cache.get(Feature.Enable_tags))
            {
                create_edit_tag_menu_item(get_item_path(), context_menu,dbg, owner,aborter,logger);
            }*/
        }

        if ( this.item_type == Iconifiable_item_type.video)
        {
            make_menu_items_for_videos(get_item_path(),owner,context_menu,dbg, aborter,logger);
        }
        return context_menu;

    }

    //**********************************************************
    public static MenuItem create_show_similar_menu_item(Path image_path,
                                                         //Image_properties_RAM_cache image_properties_cache,
                                                         Supplier<Feature_vector_cache> fv_cache_supplier,
                                                         Path_comparator_source path_comparator_source,
                                                         Window owner,
                                                         Aborter browser_aborter,
                                                         Logger logger)
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
                List<Path> paths =  path_list_provider.only_image_paths(Feature_cache.get(Feature.Show_hidden_files));
                Similarity_engine image_similarity = new Similarity_engine(
                        paths,
                        path_list_provider,
                        path_comparator_source,
                        owner,
                        browser_aborter,logger);
                image_similarity.find_similars_special(
                        true,
                        null,
                        image_path,
                        null,
                        5,
                        true,
                        Double.MAX_VALUE, // MAGIC
                        fv_cache_supplier,
                        owner, x,y,null,browser_aborter);
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
        Menu_items.add_menu_item_for_context_menu("Convert_To_Mp4",
    event -> {
            if (dbg) logger.log("✅ convert to mp4");
            AtomicBoolean abort_reported = new AtomicBoolean(false);
            Ffmpeg_utils.video_to_mp4_in_a_thread(path,aborter, abort_reported, owner,logger);
            },
            context_menu,owner,logger);
        Menu_items.add_menu_item_for_context_menu("Generate_many_animated_GIFs",
                    event -> {
                if (dbg) logger.log("✅ Generating animated gifs !");
                Animated_gifs_from_video.generate_many_gifs(path,5,5,owner,logger);
            }, context_menu,owner,logger);
        Menu_items.add_menu_item_for_context_menu("Generate_Animated_GIF_interactively",
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
    public void receive_icon(Image_and_properties image_and_rotation)
    //**********************************************************
    {
        if ( dbg) logger.log("ITEM FILE WITH ICON RECEIVING icon");
        // this is NOT on the FX thread
        if ( image_view == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("❗ the_image_view == null"));
            return;
        }

        if (!visible_in_scene.get())
        {
            // this happen if between the time the icon was request and now,
            // the item is not visible anymore typically because the user scrolled away
            if ( dbg)
                logger.log("❗ visible_in_scene.get() : calling you_are_invisible");
            Jfx_batch_injector.inject(this::you_are_invisible,logger);
            return;
        }
        if ( image_and_rotation == null)
        {
            if ( dbg)
                logger.log("❗ image_and_rotation == null ");
            //Jfx_batch_injector.inject(() -> you_are_invisible(),logger);
            return;
        }
        if ( image_and_rotation.image() == null)
        {
            if ( dbg)
                logger.log("❗ image_and_rotation.image() == null : setting the image to null in the Image_view");
            //Jfx_batch_injector.inject(() -> you_are_invisible(),logger);
            return;
        }

        if ( (image_and_rotation.image().getHeight()  < 1) || (image_and_rotation.image().getWidth() < 1))
        {
            logger.log(Stack_trace_getter.get_stack_trace("❗ WARNING: empty image, not set "+get_item_path().toAbsolutePath()));
            Jfx_batch_injector.inject(this::you_are_invisible,logger);
            return;
        }
        Jfx_batch_injector.inject(() -> receive_icon_in_fx_thread(image_and_rotation),logger);
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
            if ( image_and_properties.properties() ==null)
            {
                logger.log(Stack_trace_getter.get_stack_trace("❌ FATAL receive_icon_in_fx_thread image_and_properties.properties() ==null, for: "+get_item_path()));
                return;
            }
            logger.log("✅ receive_icon_in_fx_thread," +
                    "\n   w icon=          "+image_and_properties.image().getWidth()+
                    "\n   h icon=          "+image_and_properties.image().getHeight()+
                    "\n   w image=         "+image_and_properties.properties().w()+
                    "\n   h image=         "+image_and_properties.properties().h()+
                    "\n   rot image=       "+image_and_properties.properties().rotation()+
                    "\n   aspect ratio=    "+image_and_properties.properties().get_aspect_ratio()+
                    "\n   for:             "+get_item_path());

        }

        double local_rot = 0;
        {
            Rotation rotation = image_and_properties.properties().rotation();
            if (rotation == null)
            {
                if (Files.exists(get_item_path()))
                {
                    if (
                            (Guess_file_type.is_this_path_a_video(get_item_path(),owner,logger)) || (Guess_file_type.is_this_path_a_pdf(get_item_path(),owner,logger))
                    ) {
                        if (dbg) logger.log("✅ PDF or video => rot=0");
                        local_rot = 0;
                    } else {
                        local_rot = Fast_rotation_from_exif_metadata_extractor.get_rotation(get_item_path(), true, aborter, logger).orElse(0.0);
                    }
                }
                else
                {
                    logger.log(Stack_trace_getter.get_stack_trace("❌ Bad"));
                    you_are_invisible();
                    return;
                }

            }
        }
        if ( item_type == Iconifiable_item_type.pdf)
        {
            if (aspect_ratio == null)
            {
                logger.log("❌ SHOULD NOT HAPPEN");
                double local = image_and_properties.image().getWidth()/image_and_properties.image().getHeight();
                if( dbg) logger.log(Stack_trace_getter.get_stack_trace("setting aspect ratio for PDF from icon: "+ local));
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
        image_view.setImage(image_and_properties.image());

        if (( image_and_properties.image().getHeight() >= icon_size) && (image_and_properties.image().getWidth() >= icon_size))
        {
            // this happens when the icon is PDF as we dont scale PDF icons
            if (dbg) logger.log("✅ icon larger than target HAPPENS1 for: "+get_item_path());
            image_view.setFitWidth(icon_size);
            image_view.setFitHeight(icon_size);
            if ((local_rot == 90) || (local_rot == 270))
            {
                // this actually NEVER HAPPENS now since a PDF icon is never rotated
                //if (dbg)
                    logger.log("❌ HAPPENS2 for: "+get_item_path());
                image_view.setFitWidth(image_and_properties.image().getHeight());
                image_view.setFitHeight(image_and_properties.image().getWidth());
            }
        }
        else
        {
            if ((local_rot == 90) || (local_rot == 270))
            {

                if ( image_and_properties.image().getHeight() < image_and_properties.image().getWidth())
                {
                    if (dbg)
                        logger.log("✅ HAPPENS3A for: "+get_item_path());
                    image_view.setFitWidth(icon_size);
                    image_view.setFitHeight(-1);
                }
                else
                {
                    // this happens rarely as it is an image that is rotated AND wider than high after rotation
                    //(most of the rotated images are portrait shot by turning the camera
                    if (dbg)
                        logger.log("✅ HAPPENS3B for: "+get_item_path());
                    image_view.setFitWidth(-1);
                    image_view.setFitHeight(icon_size);
                }
            }
            else
            {
                if (dbg)
                    logger.log("✅ HAPPENS4 for: "+get_item_path());
                image_view.setFitWidth(image_and_properties.image().getWidth());
                image_view.setFitHeight(image_and_properties.image().getHeight());
            }
        }
        if ( image_and_properties.properties().rotation() != null) {
            image_pane.setRotate(Rotation.to_angle(image_and_properties.properties().rotation()));
        }
        else
        {
            if ( dbg) logger.log("❗ image_and_rotation.rotation() is null");
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
            logger.log("❌ BAD WARNING: item_file_with_icon: default_icon == null");
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
        //return image_pane;
        return button;
    }

    //**********************************************************
    @Override
    public String get_string()
    //**********************************************************
    {
        return "is item_file_with_icon for : " + get_item_path().toAbsolutePath();
    }
}