//SOURCES ./Disk_foot_print_receiver.java
package klik.browser.items;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.stage.Window;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.browser.*;
import klik.path_lists.Path_list_provider_for_file_system;
import klik.browser.icons.Icon_destination;
import klik.browser.icons.Icon_factory_actor;
import klik.browser.icons.animated_gifs.Animated_gif_from_folder_content;
import klik.browser.icons.image_properties_cache.Image_properties;
import klik.browser.icons.image_properties_cache.Image_properties_RAM_cache;
import klik.browser.icons.image_properties_cache.Rotation;
import klik.browser.virtual_landscape.*;
import klik.path_lists.Path_list_provider;
import klik.util.image.decoding.Fast_rotation_from_exif_metadata_extractor;
import klik.look.Look_and_feel_manager;
import klik.look.my_i18n.My_I18n;
import klik.properties.Non_booleans_properties;
import klik.properties.boolean_features.Feature;
import klik.properties.boolean_features.Feature_cache;
import klik.util.files_and_paths.Guess_file_type;
import klik.util.files_and_paths.Sizes;
import klik.util.files_and_paths.Static_files_and_paths_utilities;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import klik.util.ui.Jfx_batch_injector;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;


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
    private  final Image_properties_RAM_cache image_properties_RAM_cache;

    //**********************************************************
    public Item_folder_with_icon(
            Window owner,
            Scene scene,
            Selection_handler selection_handler,
            Icon_factory_actor icon_factory_actor,
            Color color,
            String text_,
            int column_width_,
            double height,
            boolean is_trash_,
            Path is_parent_of,
            Image_properties_RAM_cache image_properties_RAM_cache,
            Shutdown_target shutdown_target,
            Path_list_provider path_list_provider,
            Path_comparator_source path_comparator_source,
            Top_left_provider top_left_provider,
            Aborter aborter,
            Logger logger)
    //**********************************************************
    {
        super(
                scene,
                selection_handler,
                icon_factory_actor,
                color,
                text_,
                height,
                is_trash_,
                is_parent_of,
                image_properties_RAM_cache,
                shutdown_target,
                path_list_provider,
                path_comparator_source,
                top_left_provider,
                owner,
                aborter,
                logger);
        column_width = column_width_;
        this.image_properties_RAM_cache = image_properties_RAM_cache;
        folder_icon_size = Non_booleans_properties.get_folder_icon_size(owner);
        // launch content icon fabrication:
        text = text_;
        double font_size = Non_booleans_properties.get_font_size(owner,logger);
        estimated_text_label_height = klik.look.Look_and_feel.MAGIC_HEIGHT_FACTOR*font_size;

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
    }

    //**********************************************************
    @Override
    public Path get_item_path()
    //**********************************************************
    {
        return path_list_provider.get_folder_path();
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
        if ( Feature_cache.get(Feature.Show_single_column))
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
        double local_rot = 0;
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
                Path local = get_path_for_display(false);
                local_rot = Fast_rotation_from_exif_metadata_extractor.get_rotation(local, dbg, aborter, logger).orElse(0.0);
                the_image_pane.setRotate(local_rot);
            }
            else
            {
                the_image_pane.setRotate(Rotation.to_angle(rotation));
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
            logger.log(Stack_trace_getter.get_stack_trace("SHOULD NOT HAPPEN"));
            return null;
        }

        // try to find an icon for the folder
        File dir = get_item_path().toFile();
        File[] files = dir.listFiles();
        if ( files == null)
        {
            if ( dbg) logger.log("WARNING: dir is access denied: "+get_item_path());
            create_label_for_sizes("Access Denied?");
            return null;
        }
        if ( files.length == 0)
        {
            if ( dbg) logger.log("dir is empty: "+get_item_path());
            create_label_for_sizes("Empty folder");
            return null;
        }
        Arrays.sort(files);
        List<File> images_in_folder = new ArrayList<>();

        for ( File f : files)
        {
            if (f.isDirectory()) continue; // ignore folders
            if (!Guess_file_type.is_this_file_an_image(f)) continue; // ignore non images
            if (Guess_file_type.is_this_path_a_gif(f.toPath()))
            {
                if (Guess_file_type.is_this_path_a_animated_gif(f.toPath(), owner, aborter, logger))
                {
                    return f.toPath();
                }
                continue; // ignore not animated gifs
            }
            images_in_folder.add(f);
        }
        if ( images_in_folder.isEmpty())
        {
            for (File folder : files) {
                if (folder.isDirectory()) {
                    File[] files2 = folder.listFiles();
                    if (files2 == null) return null;
                    Arrays.sort(files2);
                    for (File f2 : files2) {
                        if (f2.isDirectory()) continue; // ignore folders
                        if (!Guess_file_type.is_this_file_an_image(f2)) continue; // ignore non images
                        if (Guess_file_type.is_this_path_a_gif(f2.toPath())) {
                            if (Guess_file_type.is_this_path_a_animated_gif(f2.toPath(), owner,aborter, logger)) {
                                return f2.toPath();
                            }
                            continue; // ignore not animated gifs
                        }
                        return f2.toPath();
                    }
                }
            }
            create_label_for_sizes("...computing sizes...");
            launch_disk_foot_print_thread(this, get_item_path(), aborter, logger);
            return null;
        }

        Path returned = Animated_gif_from_folder_content.make_animated_gif_from_images_in_folder(
                owner,
                new Path_list_provider_for_file_system(get_item_path()),
                path_comparator_source,
                images_in_folder,  image_properties_RAM_cache, aborter,logger);
        if ( returned != null)
        {
            if (dbg) logger.log("animated gif made");
            return returned;
        }
        if (images_in_folder.isEmpty())
        {
            if (dbg) logger.log("no images");
            return null;
        }
        else
        {
            if (dbg) logger.log("picking first image");
            return images_in_folder.get(0).toPath();
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
    @Override // Item
    public void set_is_unselected_internal()
    {
        Look_and_feel_manager.give_button_a_file_style(button,owner,logger);
    }

    //**********************************************************
    @Override // Item
    public void set_is_selected_internal()
    //**********************************************************
    {
        Look_and_feel_manager.give_button_a_selected_file_style(button,owner,logger);
    }



    @Override
    public Node get_Node() {
        return button;
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
        return is_parent_of;
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
        return "Item_folder_with_icon for: " + get_item_path().toAbsolutePath();
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
        sb.append(folders);
        intercalaire(on_one_line, sb);
        sb.append(sizes.files());
        String files = My_I18n.get_I18n_string("Files",owner,logger);
        sb.append(files);
        intercalaire(on_one_line, sb);
        sb.append(sizes.images());
        String images = My_I18n.get_I18n_string("Images",owner,logger);

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
    public static void launch_disk_foot_print_thread(Disk_foot_print_receiver disk_foot_print_receiver, Path path, Aborter aborter, Logger logger)
    //**********************************************************
    {
        Runnable r = () -> {
            try {
                Thread.sleep(50+random.nextInt(300));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if ( aborter.should_abort()) return;
            Sizes sizes = Static_files_and_paths_utilities.get_sizes_on_disk_deep(path, aborter,logger);
            Jfx_batch_injector.inject(() -> disk_foot_print_receiver.set_disk_foot_print_text(sizes),logger);
        };
        Actor_engine.execute(r,"Compute size deep",logger);
    }


}
