//SOURCES ./Disk_foot_print_receiver.java
package klik.browser.items;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;

import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.browser.Browser;
import klik.browser.Browser_creation_context;
import klik.browser.Drag_and_drop;
import klik.browser.Image_and_properties;
import klik.browser.icons.Icon_destination;
import klik.browser.icons.Virtual_landscape;
import klik.browser.icons.animated_gifs.Animated_gif_from_folder;
import klik.browser.icons.image_properties_cache.Rotation;
import klik.look.my_i18n.My_I18n;
import klik.util.files_and_paths.Static_files_and_paths_utilities;
import klik.util.files_and_paths.Guess_file_type;
import klik.util.files_and_paths.Sizes;
import klik.images.decoding.Fast_rotation_from_exif_metadata_extractor;
import klik.look.Look_and_feel_manager;
import klik.properties.Static_application_properties;
import klik.util.ui.Jfx_batch_injector;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;

import java.io.File;
import java.nio.file.Path;
import java.util.*;


//**********************************************************
public class Item_folder_with_icon extends Item implements Icon_destination, Disk_foot_print_receiver
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
    Button the_button;
    private final int folder_icon_size;
    private final int column_width; // as set by the icon manager
    //**********************************************************
    public Item_folder_with_icon(
            Browser browser,
            Path path_,
            Color color,
            String text_,
            int column_width_,
            Logger logger)
    //**********************************************************
    {
        super(browser, path_, color, logger);
        column_width = column_width_;
        folder_icon_size = Static_application_properties.get_folder_icon_size(logger);
        // launch content icon fabrication:
        text = text_;
        double font_size = Static_application_properties.get_font_size(logger);
        estimated_text_label_height = klik.look.Look_and_feel.MAGIC_HEIGHT_FACTOR*font_size;

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
        the_button.setOnAction(actionEvent -> Browser_creation_context.replace_different_folder(path,browser, logger));
        Tooltip.install(the_button,new Tooltip(path.getFileName().toString()));

        resize_the_box(the_button);

        Drag_and_drop.init_drag_and_drop_receiver_side(get_Node(),browser,path,is_trash(),logger);
        Drag_and_drop.init_drag_and_drop_sender_side(get_Node(),browser,path,logger);
        give_a_menu_to_the_button(the_button,null);

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
        if ( Static_application_properties.get_single_column(logger))
        {
            button.setPrefWidth(browser.my_Stage.the_Stage.getWidth()- Virtual_landscape.RIGHT_SIDE_SINGLE_COLUMN_MARGIN);
            button.setMinWidth(browser.my_Stage.the_Stage.getWidth()- Virtual_landscape.RIGHT_SIDE_SINGLE_COLUMN_MARGIN);
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
            logger.log(Stack_trace_getter.get_stack_trace("image==null for "+path));
            return;
        }
        if ( the_image_view == null)
        {
            the_image_view = new ImageView();
            the_image_pane.getChildren().add(the_image_view);
        }

        //logger.log(Stack_trace_getter.get_stack_trace("item_folder_with_icon setting icon for "+path+ " image width = "+image.getWidth()));
        the_image_view.setImage(image_and_properties.image());
        the_image_view.setPreserveRatio(true);

        // normally we already have the rotation
        double local_rot = 0;
        Rotation rotation = image_and_properties.properties().rotation();
        if (rotation == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("SHOULD NOT HAPPEN"));
            Path local = get_path_for_display(false);
            local_rot = Fast_rotation_from_exif_metadata_extractor.get_rotation(local, dbg, browser_aborter, logger);
            the_image_pane.setRotate(local_rot);
        }
        else
        {
            the_image_pane.setRotate(Rotation.to_angle(rotation));
        }
        resize_the_box(the_button);
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
        File dir = path.toFile();
        File[] files = dir.listFiles();
        if ( files == null)
        {
            if ( dbg) logger.log("WARNING: dir is access denied: "+path);
            create_label_for_sizes("Access Denied?");
            return null;
        }
        if ( files.length == 0)
        {
            if ( dbg) logger.log("dir is empty: "+path);
            create_label_for_sizes("Empty folder");
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
                if (Guess_file_type.is_this_path_a_animated_gif(f.toPath(), browser_aborter, logger))
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
                        if (!Guess_file_type.is_file_an_image(f2)) continue; // ignore non images
                        if (Guess_file_type.is_this_path_a_gif(f2.toPath())) {
                            if (Guess_file_type.is_this_path_a_animated_gif(f2.toPath(), browser_aborter, logger)) {
                                return f2.toPath();
                            }
                            continue; // ignore not animated gifs
                        }
                        return f2.toPath();
                    }
                }
            }
            create_label_for_sizes("...computing sizes...");
            launch_disk_foot_print_thread(this, path, browser_aborter, logger);
            return null;
        }

        Path returned = Animated_gif_from_folder.make_animated_gif_from_all_images_in_folder(browser.my_Stage.the_Stage,path,  images_in_folder,  logger);
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
            return images_in_folder.getFirst().toPath();
        }
    }

    //**********************************************************
    private void create_label_for_sizes(String s)
    //**********************************************************
    {
        label_for_sizes = new Label(s);
        Look_and_feel_manager.set_label_look(label_for_sizes);
        Jfx_batch_injector.inject(() -> {
            the_image_pane.getChildren().clear();
            the_image_pane.getChildren().add(label_for_sizes);
        },logger);
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


    @Override
    public Node get_Node() {
        return the_button;
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
    public boolean is_parent()
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
        return "Item_folder_with_icon for: " + path.toAbsolutePath();
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
        sb.append(Static_files_and_paths_utilities.get_1_line_string_for_byte_data_size(sizes.bytes(),logger));
        intercalaire(on_one_line, sb);
        sb.append(sizes.folders());
        String folders = My_I18n.get_I18n_string("Folders",logger);
        sb.append(folders);
        intercalaire(on_one_line, sb);
        sb.append(sizes.files());
        String files = My_I18n.get_I18n_string("Files",logger);
        sb.append(files);
        intercalaire(on_one_line, sb);
        sb.append(sizes.images());
        String images = My_I18n.get_I18n_string("Images",logger);

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
        Actor_engine.execute(r,logger);
    }


}
