// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ./Disk_foot_print_receiver.java
package klikr.browser_core.items;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.stage.Window;
import klikr.Window_type;
import klikr.path_lists.Files_and_folders;
import klikr.util.cache.Klikr_cache;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Actor_engine;
import klikr.browser_core.*;
import klikr.path_lists.Path_list_provider_for_file_system;
import klikr.browser_core.icons.Icon_destination;
import klikr.browser_core.icons.Icon_factory_actor;
import klikr.util.animated_gifs.Animated_gif_from_folder_content;
import klikr.browser_core.icons.image_properties_cache.Image_properties;
import klikr.browser_core.icons.image_properties_cache.Rotation;
import klikr.browser_core.virtual_landscape.*;
import klikr.path_lists.Path_list_provider;
import klikr.util.image.decoding.Fast_rotation_from_exif_metadata_extractor;
import klikr.look.Look_and_feel_manager;
import klikr.look.my_i18n.My_I18n;
import klikr.settings.Non_booleans_properties;
import klikr.settings.boolean_features.Feature;
import klikr.settings.boolean_features.Feature_cache;
import klikr.util.files_and_paths.Guess_file_type;
import klikr.util.files_and_paths.Sizes;
import klikr.util.files_and_paths.Static_files_and_paths_utilities;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;
import klikr.util.ui.Jfx_batch_injector;

import java.io.File;
import java.nio.file.Path;
import java.util.*;


//**********************************************************
public class Item_folder_with_icon extends Item_folder implements Icon_destination, Disk_foot_print_receiver
//**********************************************************
{
    public static final boolean dbg = false;
    public final String text;
    double estimated_text_label_height;

    // these 2 are for the image representing the folder CONTENT
    // i.e. either nothing (no images in folder)
    // or the first image in the folder
    // or an animated gif with a sample of the images in the folder
    ImageView the_image_view;
    FlowPane the_image_pane;
    Label label_for_sizes;
    private final int folder_icon_size;
    private final int column_width; // as set by the icon manager
    private  final Klikr_cache<Path, Image_properties> image_properties_cache;

    //**********************************************************
    public Item_folder_with_icon(
            Application application,
            Window_type window_type,
            Window owner,
            Scene scene,
            Selection_handler selection_handler,
            Icon_factory_actor icon_factory_actor,
            Color color,
            String text_,
            int column_width_,
            double height,
            Klikr_cache<Path, Image_properties> image_properties_cache,
            Shutdown_target shutdown_target,
            Path_list_provider path_list_provider,
            Path_comparator_source path_comparator_source,
            Top_left_provider top_left_provider,
            Aborter aborter,
            Logger logger)
    //**********************************************************
    {
        super(
                application,
                window_type,
                scene,
                selection_handler,
                icon_factory_actor,
                color,
                text_,
                height,
                false,
                null,
                image_properties_cache,
                shutdown_target,
                path_list_provider,
                path_comparator_source,
                top_left_provider,
                owner,
                aborter,
                logger);
        column_width = column_width_;
        this.image_properties_cache = image_properties_cache;
        folder_icon_size = Non_booleans_properties.get_folder_icon_size(owner);
        // launch content icon fabrication:
        text = text_;
        double font_size = Non_booleans_properties.get_font_size(owner,logger);
        estimated_text_label_height = klikr.look.Look_and_feel.MAGIC_HEIGHT_FACTOR*font_size;

        //button = new Button(text);
        //button.setMnemonicParsing(false);
        //button.setTextOverrun(OverrunStyle.ELLIPSIS);
        the_image_pane = new FlowPane();
        the_image_pane.setAlignment(Pos.BOTTOM_LEFT);
        the_image_pane.setMinWidth(folder_icon_size);
        the_image_pane.setMaxWidth(folder_icon_size);
        the_image_pane.setMinHeight(folder_icon_size);
        the_image_pane.setMaxHeight(folder_icon_size);
        button.setGraphic(the_image_pane);
        button.setContentDisplay(ContentDisplay.BOTTOM);

        Look_and_feel_manager.set_button_look(button,true,owner,logger);


        resize_the_box(button);
    }


    @Override
    public Iconifiable_item_type get_item_type() {
        return Iconifiable_item_type.folder;
    }

    //**********************************************************
    @Override
    void set_new_path(Path newPath)
    //**********************************************************
    {
        logger.log(Stack_trace_getter.get_stack_trace("set_new_path???"));
    }

    //**********************************************************
    @Override
    public Path get_item_path()
    //**********************************************************
    {
        Optional<Path> p= path_list_provider.get_folder_path();
        return p.orElse(null);
    }

    //**********************************************************
    @Override // Item
    public int get_icon_size()
    //**********************************************************
    {
        return folder_icon_size;
    }

    //**********************************************************
    @Override // Item
    public void you_are_visible_specific()
    //**********************************************************
    {
    }

    //**********************************************************
    @Override
    public boolean has_icon()
    //**********************************************************
    {
        return true;
    }
    //**********************************************************
    @Override
    public void you_are_invisible_specific()
    //**********************************************************
    {
    }

    //**********************************************************
    private void resize_the_box(Button button)
    //**********************************************************
    {
        if ( Feature_cache.get(Feature.Show_single_column_with_details))
        {
            button.setPrefWidth(owner.getWidth()- Virtual_landscape.RIGHT_SIDE_SINGLE_COLUMN_MARGIN);
            button.setMinWidth(owner.getWidth()- Virtual_landscape.RIGHT_SIDE_SINGLE_COLUMN_MARGIN);
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
    public void receive_icon(Image_and_properties image_and_rotation)
    //**********************************************************
    {
        Jfx_batch_injector.inject(() -> set_icon(image_and_rotation),logger);
    }

    //**********************************************************
    private void set_icon(Image_and_properties image_and_properties)
    //**********************************************************
    {
        if ( image_and_properties.image() == null)
        {
            the_image_view = null;
            logger.log(Stack_trace_getter.get_stack_trace("image==null for "+get_item_path()));
            return;
        }
        if ( the_image_view == null)
        {
            the_image_view = new ImageView();
            the_image_view.setPickOnBounds(true); // allow click on transparent areas
            the_image_pane.getChildren().add(the_image_view);
        }

        //logger.log(Stack_trace_getter.get_stack_trace("item_folder_with_icon setting icon for "+path+ " image width = "+image.getWidth()));
        the_image_view.setImage(image_and_properties.image());
        the_image_view.setPreserveRatio(true);

        // normally we already have the rotation
        Image_properties properties = image_and_properties.properties();
        if (properties == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("SHOULD NOT HAPPEN"));
        }
        else
        {
            Rotation rotation = properties.rotation();
            if (rotation == null)
            {
                logger.log(Stack_trace_getter.get_stack_trace("SHOULD NOT HAPPEN"));
                Path pfd = get_path_for_display(false);
                if ( pfd == null)
                {
                    logger.log(Stack_trace_getter.get_stack_trace(""));
                    return;
                }
                rotation = Fast_rotation_from_exif_metadata_extractor.get_rotation(pfd, dbg, owner, aborter, logger);
                if ( rotation == null) rotation = Rotation.normal;
                the_image_pane.setRotate(rotation.as_double());
            }
            else
            {
                the_image_pane.setRotate(rotation.as_double());
            }
        }
        resize_the_box(button);
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
        if ( !try_deep)
        {
            logger.log(Stack_trace_getter.get_stack_trace("❌ SHOULD NOT HAPPEN"));
            return null;
        }

        // try to find an icon for the folder
        Path item_path = get_item_path();
        if ( item_path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace(""));
            return null;
        }
        Files_and_folders faf = path_list_provider.files_and_folders(
                true,
                null,
                Feature_cache.get(Feature.Show_hidden_files),
                Feature_cache.get(Feature.Show_hidden_folders),
                aborter);


        if ( (faf.folders().isEmpty()) &&(faf.files().isEmpty()))
        {
            if ( dbg) logger.log("✅ dir is empty: "+get_item_path());
            create_label_for_sizes("Empty folder");
            return null;
        }
        Arrays.sort(faf.files().toArray(new Path[0]));
        List<Path> images_in_folder = new ArrayList<>();
        for ( Path file : faf.files())
        {
            if (!Guess_file_type.is_this_path_extension_an_image(file,owner,logger)) continue; // ignore non images
            if (Guess_file_type.is_this_path_extension_a_gif(file,logger))
            {
                if (Guess_file_type.is_this_path_a_animated_gif(file, owner, aborter, logger))
                {
                    return file;
                }
                continue; // ignore not animated gifs
            }
            images_in_folder.add(file);
        }
        if ( images_in_folder.isEmpty())
        {
            for (Path folder : faf.folders())
            {
                File[] files2 = folder.toFile().listFiles();
                if (files2 == null) return null;
                Arrays.sort(files2);
                for (File f2 : files2) {
                    if (f2.isDirectory()) continue; // ignore folders
                    if (!Guess_file_type.is_this_file_extension_an_image(f2,owner,logger)) continue; // ignore non images
                    if (Guess_file_type.is_this_path_extension_a_gif(f2.toPath(),logger)) {
                        if (Guess_file_type.is_this_path_a_animated_gif(f2.toPath(), owner,aborter, logger)) {
                            return f2.toPath();
                        }
                        continue; // ignore not animated gifs
                    }
                    return f2.toPath();
                }

            }
            create_label_for_sizes("...computing sizes...");
            launch_disk_foot_print_thread(this, item_path, aborter, owner, logger);
            return null;
        }

        Optional<Path> returned = Animated_gif_from_folder_content.make_animated_gif_from_images_in_folder(
                owner,
                new Path_list_provider_for_file_system(item_path,owner,logger),
                path_comparator_source,
                images_in_folder, image_properties_cache, aborter,logger);
        if ( returned.isPresent())
        {
            if (dbg) logger.log("✅ animated gif made");
            return returned.get();
        }
        if (images_in_folder.isEmpty())
        {
            if (dbg) logger.log("✅ no images");
            return null;
        }
        else
        {
            if (dbg) logger.log("✅ picking first image");
            return images_in_folder.get(0);
        }
    }

    //**********************************************************
    private void create_label_for_sizes(String s)
    //**********************************************************
    {
        label_for_sizes = new Label(s);
        Look_and_feel_manager.set_label_look(label_for_sizes,owner,logger);
        Jfx_batch_injector.inject(() -> {
            the_image_pane.getChildren().clear();
            the_image_pane.getChildren().add(label_for_sizes);
        },logger);
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
        if ( get_item_path() != null) {
            return "Item_folder_with_icon for: " + get_item_path().toAbsolutePath();
        }
        return "Item_folder_with_icon: NO PATH for icon";
    }


    //**********************************************************
    @Override
    public void set_disk_foot_print_text(Sizes sizes)
    //**********************************************************
    {
        if (label_for_sizes == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("SHOULD NOT HAPPEN"));
            return;
        }
        boolean on_one_line;
        if ( folder_icon_size >= 128)
        {
            // large icon = enough room for 3 lines of text
            on_one_line = false;
        }
        else
        {
            on_one_line = true;
            label_for_sizes.setWrapText(true);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(Static_files_and_paths_utilities.get_1_line_string_for_byte_data_size(sizes.bytes(),owner,logger));

        intercalaire(on_one_line, sb);

        sb.append(sizes.folders());
        String folders = My_I18n.get_I18n_string("Folders",owner,logger);
        sb.append(" ");
        sb.append(folders);

        intercalaire(on_one_line, sb);

        sb.append(sizes.files());
        String files = My_I18n.get_I18n_string("Files",owner,logger);
        sb.append(" ");
        sb.append(files);

        intercalaire(on_one_line, sb);

        sb.append(sizes.images());
        String images = My_I18n.get_I18n_string("Images",owner,logger);
        sb.append(" ");
        sb.append(images);
        label_for_sizes.setText(sb.toString());

    }

    //**********************************************************
    private static void intercalaire(boolean on_one_line, StringBuilder sb)
    //**********************************************************
    {
        if (on_one_line) {
            sb.append(", ");
        }
        else {
            sb.append("\n");
        }
    }


    static Random random = new Random();
    //**********************************************************
    public static void launch_disk_foot_print_thread(Disk_foot_print_receiver disk_foot_print_receiver, Path path, Aborter aborter, Window owner, Logger logger)
    //**********************************************************
    {
        Runnable r = () -> {
            try {
                Thread.sleep(50+random.nextInt(300));
            } catch (InterruptedException e) {
                logger.log(""+e);
            }
            if ( aborter.should_abort()) return;
            Sizes sizes = Static_files_and_paths_utilities.get_sizes_on_disk_deep(path, aborter, owner, logger);
            Jfx_batch_injector.inject(() -> disk_foot_print_receiver.set_disk_foot_print_text(sizes),logger);
        };
        Actor_engine.execute(r,"Compute length deep",logger);
    }


}
