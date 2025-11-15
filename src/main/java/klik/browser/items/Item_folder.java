// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ../../util/ui/Text_frame_with_labels.java
package klik.browser.items;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.stage.Window;
import klik.Window_type;
import klik.Instructions;
import klik.util.execute.actor.Aborter;
import klik.util.execute.actor.Actor_engine;
import klik.browser.*;
import klik.path_lists.Path_list_provider_for_file_system;
import klik.browser.icons.Icon_destination;
import klik.browser.icons.Icon_factory_actor;
import klik.util.animated_gifs.Animated_gif_from_folder_content;
import klik.browser.icons.image_properties_cache.Image_properties_RAM_cache;
import klik.browser.virtual_landscape.*;
import klik.look.Look_and_feel_manager;
import klik.path_lists.Path_list_provider;
import klik.properties.Non_booleans_properties;
import klik.properties.boolean_features.Feature;
import klik.properties.boolean_features.Feature_cache;
import klik.util.files_and_paths.Guess_file_type;
import klik.util.files_and_paths.Sizes;
import klik.util.files_and_paths.Static_files_and_paths_utilities;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import klik.util.ui.Jfx_batch_injector;
import klik.util.ui.Popups;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.LongAdder;


//**********************************************************
public class Item_folder extends Item implements Icon_destination
//**********************************************************
{
    public static final boolean dbg = false;
    public Button button;
    public Label label;
    public final boolean is_trash;
    public final Path is_parent_of;
    public String text;
    private static DateTimeFormatter date_time_formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final Image_properties_RAM_cache image_properties_RAM_cache;
    private final Shutdown_target shutdown_target;
    private final Top_left_provider top_left_provider;


    //**********************************************************
    public Item_folder(
            Scene scene,
            Selection_handler selection_handler,
            Icon_factory_actor icon_factory_actor,
            Color color,
            String text_,
            double height,
            boolean is_trash_,
            Path is_parent_of,
            Image_properties_RAM_cache image_properties_RAM_cache,
            Shutdown_target shutdown_target,
            Path_list_provider path_list_provider,
            Path_comparator_source path_comparator_source,
            Top_left_provider top_left_provider,
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
                path_list_provider,
                path_comparator_source,
                owner,
                aborter,
                logger);
        this.image_properties_RAM_cache = image_properties_RAM_cache;
        this.shutdown_target = shutdown_target;
        this.top_left_provider = top_left_provider;
        text = text_;
        is_trash = is_trash_;
        this.is_parent_of = is_parent_of;
        /*
        if (path == null) {
            is_dir = false;
            button = new Button("----------");
            button.setPrefWidth(Control.USE_COMPUTED_SIZE);
            button.setTextOverrun(OverrunStyle.ELLIPSIS);
            button.setGraphicTextGap(20);
            Look_and_feel_manager.set_button_look(button,false);
            return;
        }*/

        double button_width = Non_booleans_properties.get_column_width(owner);
        if ( button_width < Virtual_landscape.MIN_COLUMN_WIDTH) button_width = Virtual_landscape.MIN_COLUMN_WIDTH;

        Path local = get_item_path();
        if ( local == null)
        {
            if ( text.isEmpty())
            {
                // this is top level / folder
            }
            else
            {
                logger.log("❗ Warning PATH is null in item folder for ->"+text+"<-");
            }
            return;
        }
        if (Files.isDirectory(local))
        {
            button_for_a_directory(text, is_trash, button_width, height, color);
        }
        else
        {
            logger.log("❌ SHOULD NOT HAPPEN Item_folder path is not a directory text=->"+text+"<-+path ="+local);
            return;
        }
        Look_and_feel_manager.set_button_look(button,false,owner,logger);
        button.setManaged(true); // means the parent tells the button its layout
        button.setMnemonicParsing(false);// avoid suppression of first underscore in names
        button.setTextOverrun(OverrunStyle.ELLIPSIS);
        if (Feature_cache.get(Feature.Show_file_names_as_tooltips))
        {
            Tooltip.install(button, new Tooltip(get_item_path().toString()));
        }
        Drag_and_drop.init_drag_and_drop_sender_side(get_Node(),selection_handler,get_item_path(),logger);

    }



    @Override
    public Iconifiable_item_type get_item_type() {
        return null;
    }


    @Override
    void set_new_path(Path newPath) {

    }

    @Override
    public Path get_item_path() {
        return path_list_provider.get_folder_path();
    }

    public ImageView get_image_view(){return null;}
    public Pane get_pane(){return null;}

    //**********************************************************
    @Override // Item
    public void you_are_visible_specific()
    //**********************************************************
    {

    }

    //**********************************************************
    @Override // Item
    public void you_are_invisible_specific()
    //**********************************************************
    {

    }


    //**********************************************************
    @Override // Item
    public int get_icon_size()
    //**********************************************************
    {
        return 0;
    }


    //**********************************************************
    @Override
    public boolean has_icon()
    //**********************************************************
    {
        return false;
    }
    //**********************************************************
    @Override
    public void receive_icon(Image_and_properties image_and_rotation)
    //**********************************************************
    {
        logger.log(Stack_trace_getter.get_stack_trace("❌ SHOULD NOT HAPPEN"));
    }


    //**********************************************************
    public Path get_true_path()
    //**********************************************************
    {
        return get_item_path();
    }

    @Override // Icon_destination
    public Path get_path_for_display_icon_destination()
    {
        logger.log("✅ Item_button get_path_for_display_icon_destination DEEP !???");
        return get_path_for_display(true);
    }

    // this call is intended only from a working thread
    // in the icon factory as
    //**********************************************************
    @Override // Item
    public Path get_path_for_display(boolean try_deep)
    //**********************************************************
    {
        if (is_trash) return null;
        if (is_parent_of!=null) return null;
        // for a file the displayed icon is built from the file itself, if supported:
        if ( !get_item_path().toFile().isDirectory()) return get_item_path();

        if ( !try_deep) return null;

        // for a folder we have 2 ways to provide an icon
        // 1) an image is taken from the folder and used as icon
        // 2) multiple images are taken from the folder to form an animated gif icon

        // try to find an icon for the folder
        return get_an_image_down_in_the_tree_files(get_item_path());
        /*
        no recursive madness please!
        if ( returned != null) return returned;
        // ok, so we did not find an image file in the folder
        // let us go down sub directories (if any)
        return get_an_image_down_in_the_tree_folders(path);
        */

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
            if ( dbg) logger.log("❗ WARNING: dir is access denied: "+local_path);
            return null;
        }
        if ( files.length == 0)
        {
            if ( dbg) logger.log("✅ dir is empty: "+local_path);
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
            if (!Guess_file_type.is_this_file_an_image(f,owner,logger)) continue; // ignore non images
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
            logger.log("✅ make_animated_gif");

            if ( Objects.requireNonNull(images_in_folder).isEmpty())
            {
                return null;
            }

            Path returned = Animated_gif_from_folder_content.make_animated_gif_from_images_in_folder(
                    owner,
                    new Path_list_provider_for_file_system(local_path,owner,logger),
                    path_comparator_source,
                    images_in_folder,
                    image_properties_RAM_cache,
                    aborter, logger);
            if ( returned == null)
            {
                logger.log("❌ make_animated_gif_from_all_images_in_folder fails");
                if (!images_in_folder.isEmpty()) return images_in_folder.get(0).toPath();
            }
            else
            {
                logger.log("✅ make_animated_gif_from_all_images_in_folder OK");

                return returned;
            }
        }

        return null; // no image found
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


    public Button get_button(){ return button;}


    //**********************************************************
    public void button_for_a_directory(String text, boolean is_trash, double width, double height, Color color)
    //**********************************************************
    {
        String extended_text = text;
        if ( get_item_path() != null)
        {
            if (Files.isSymbolicLink(get_item_path())) {
                extended_text += " **Symbolic link** ";
            }
        }
        button = new Button(extended_text);
        button.setMnemonicParsing(false);// avoid suppression of first underscore in names

        Look_and_feel_manager.set_button_look_as_folder(button, height, color,owner,logger);
        button.setTextAlignment(TextAlignment.RIGHT);
        //double computed_text_width = icons_width + estimate_text_width(text2);

        if (get_item_path() == null)
        {
            // protect crash when going up: root has no parent
            if ( !text.isEmpty()) logger.log("✅ WARNING no action for folder ->"+text+"<-");

            if ( is_trash) {
                button.setOnAction(event -> {
                    Popups.popup_warning("❗ WARNING","NO trash on this media: probably it is read only",true,owner,logger);
                });
            }
            return;
        }

        button.setOnAction(event -> {
            if ( dbg) logger.log("Button pressed for folder:"+text);

            if (get_item_path() == null)
            {
                // protect crash when going up: root has no parent
                logger.log("❗ WARNING no action for folder:"+text);
                return;
            }

            // as the button represents a folder, clicking on it "opens" that folder
            // = we create a NEW browser, as a replacement

            if( dbg) logger.log("Item_folder button setOnAction calling replace_different_folder");

            // this works when going "down", path is the new target path, therefore going back is the parent of that
            Path old_folder_path = get_item_path().getParent();
            if ( is_parent_of()!=null)
            {
                if ( dbg) logger.log("is_up_button");
                old_folder_path = is_parent_of();
            }
            Browsing_caches.scroll_position_cache_write(old_folder_path,top_left_provider.get_top_left());

            Instructions.replace_different_folder(
                    shutdown_target,
                    Window_type.File_system_2D,
                    new Path_list_provider_for_file_system(get_item_path(),owner,logger),
                    owner,
                    logger);

        });

        Drag_and_drop.init_drag_and_drop_receiver_side(path_list_provider.get_move_provider(),get_Node(),owner,get_item_path(),is_trash(),logger);

        give_a_menu_to_the_button(button,label);
    }


    //**********************************************************
    public void add_how_many_files_deep_folder(
            LongAdder count,
            Button button,
            String text,
            Path path,
            Map<Path, Long> folder_file_count_cache,
            Aborter aborter,
            Logger logger)
    //**********************************************************
    {
        count.increment();

        Runnable r = () -> {
            Long how_many_files_deep = folder_file_count_cache.get(path);
            if ( how_many_files_deep == null)
            {
                how_many_files_deep = (Long) Static_files_and_paths_utilities.get_how_many_files_deep(path, aborter, owner, logger);
                folder_file_count_cache.put(path,how_many_files_deep);
            }
            count.decrement();
            String extended_text =  text + " (" + how_many_files_deep + " files)";

            String finalExtended_text = extended_text;
            Jfx_batch_injector.inject(() -> {
                button.setText(finalExtended_text);
                //browser.scene_geometry_changed("number of files in button", true);
            },logger);
        };
        Actor_engine.execute(r, "Compute how many files deep", logger);
    }


    //**********************************************************
    public void add_total_size_deep_folder(LongAdder count, Button button, String text, Path path,
                                           Map<Path, Long> folder_total_sizes,
                                           Aborter aborter, Logger logger)
    //**********************************************************
    {
        count.increment();
        Runnable r = () -> {

            Long bytes = folder_total_sizes.get(path);
            if ( bytes == null)
            {
                //logger.log(path+" size not found in cache");
                Sizes sizes = Static_files_and_paths_utilities.get_sizes_on_disk_deep(path, aborter, owner, logger);
                bytes = (Long) sizes.bytes();
                //logger.log(path+" not found in cache, size is "+bytes+ "bytes");
                folder_total_sizes.put(path,bytes);
            }
            else
            {
                logger.log(path+" size found in cache "+bytes);
            }
            count.decrement();

            StringBuilder sb =  new StringBuilder();
            sb.append(text);
            sb.append("       ");
            sb.append(Static_files_and_paths_utilities.get_1_line_string_for_byte_data_size(bytes,owner,logger));

            //sb.append(", ");
            //sb.append(sizes.files());
            //sb.append(" ");
            //sb.append(My_I18n.get_I18n_string("Files",logger));
            String extended_text = sb.toString();
            Jfx_batch_injector.inject(() -> {
                button.setText(extended_text);
                //browser.scene_geometry_changed("number of files in button", true);
            },logger);
        };
        Actor_engine.execute(r, "Add total size in a folder's button", logger);
    }



    @Override
    public Node get_Node() {
        return button;
    }


    @Override
    public double get_Width() {
        return button.getWidth();
    }


    //**********************************************************
    @Override
    public double get_Height()
    //**********************************************************
    {
        if ( button.getHeight() == 0)
        {
            // until it is laid out, the button height is zero
            // so this entity CANNOT be used for "layout"... unless...
            // one cheats
            //logger.log("implausible button.getHeight() == 0");
            return 40;
        }
        return button.getHeight();
    }

    //**********************************************************
    @Override
    public boolean is_trash()
    //**********************************************************
    {
        return is_trash;
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
    public String get_string()
    //**********************************************************
    {
        return "is dir: " + get_item_path().toAbsolutePath();
    }


}
