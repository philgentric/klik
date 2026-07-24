// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ../../util/ui/Text_frame_with_labels.java
package klikr.browser_core.items;

import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.text.TextAlignment;
import klikr.Window_builder;
import klikr.Window_type;
import klikr.browser_core.icons.image_properties_cache.Image_properties;
import klikr.util.cache.Klikr_cache;
import klikr.util.cache.RAM_caches;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Actor_engine;
import klikr.browser_core.*;
import klikr.path_lists.Path_list_provider_for_file_system;
import klikr.browser_core.icons.Icon_destination;
import klikr.browser_core.icons.Icon_factory_actor;
import klikr.util.animated_gifs.Animated_gif_from_folder_content;
import klikr.browser_core.virtual_landscape.*;
import klikr.look.Look_and_feel_manager;
import klikr.settings.Non_booleans_properties;
import klikr.settings.boolean_features.Feature;
import klikr.settings.boolean_features.Feature_cache;
import klikr.util.files_and_paths.Guess_file_type;
import klikr.util.files_and_paths.Sizes;
import klikr.util.files_and_paths.Static_files_and_paths_utilities;
import klikr.util.log.Stack_trace_getter;
import klikr.util.ui.Jfx_batch_injector;
import klikr.util.ui.Popups;

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
    public final Button button;
    //public Label label;
    public String text;
    private static DateTimeFormatter date_time_formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final Klikr_cache<Path, Image_properties> image_properties_cache;
    private final Shutdown_target shutdown_target;
    private final Top_left_provider top_left_provider;


    //**********************************************************
    public Item_folder(
            Item_context item_context,
            Selection_handler selection_handler,
            Icon_factory_actor icon_factory_actor,
            String text_,
            double height,
            Klikr_cache<Path, Image_properties> image_properties_cache,
            Shutdown_target shutdown_target,
            Path_comparator_source path_comparator_source,
            Top_left_provider top_left_provider)
    //**********************************************************
    {
        super(
                item_context,
                selection_handler,
                icon_factory_actor);
        this.image_properties_cache = image_properties_cache;
        this.shutdown_target = shutdown_target;
        this.top_left_provider = top_left_provider;
        text = text_;


        double button_width = Non_booleans_properties.get_column_width(item_context.owner);
        if ( button_width < Virtual_landscape.MIN_COLUMN_WIDTH) button_width = Virtual_landscape.MIN_COLUMN_WIDTH;

        if ( item_context.item_path == null)
        {
            if ( text.isEmpty())
            {
                // this is top level / folder
            }
            else
            {
                item_context.logger.log("❗ Warning PATH is null in item folder for ->"+text+"<-");
            }
            button = null;
            return;
        }
        if (Files.isDirectory(item_context.item_path))
        {
            button = button_for_a_directory(top_left_provider,shutdown_target, item_context,text, button_width, height);
        }
        else
        {
            item_context.logger.log(Stack_trace_getter.get_stack_trace("❌ SHOULD NOT HAPPEN Item_folder path is not a directory ->"+item_context.item_path+"<- text: ->"+text+"<-"));
            button = null;
            return;
        }
        Look_and_feel_manager.set_region_look(button,false,item_context.owner,item_context.logger);
        button.setManaged(true); // means the parent tells the button its layout
        button.setMnemonicParsing(false);// avoid suppression of first underscore in names
        button.setTextOverrun(OverrunStyle.ELLIPSIS);

        if (Feature_cache.get(Feature.Show_file_names_as_tooltips))
        {
            if ( item_context.item_path == null)
            {
                item_context.logger.log(Stack_trace_getter.get_stack_trace("FATAL"));
                return;
            }
            if (item_context.item_path.getFileName() != null)
            {
                Tooltip.install(button, new Tooltip(item_context.item_path.getFileName().toString()));
            }
        }
        Drag_and_drop.init_drag_and_drop_sender_side(get_Node(),selection_handler,item_context.item_path,item_context.logger);

    }



    //**********************************************************
    @Override
    public Iconifiable_item_type get_item_type()
    //**********************************************************
    {
        return Iconifiable_item_type.folder;
    }


    @Override
    public Path get_item_path() {
        Optional<Path> p = item_context.path_list_provider.get_folder_path();
        return p.orElse(null);
    }

    //public ImageView get_image_view(){return null;}
    //public Pane get_pane(){return null;}

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
        item_context.logger.log(Stack_trace_getter.get_stack_trace("❌ SHOULD NOT HAPPEN"));
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
        item_context.logger.log("✅ Item_button get_path_for_display_icon_destination DEEP !???");
        return get_path_for_display(true);
    }

    // this call is intended only from a working thread
    // in the icon factory as
    //**********************************************************
    @Override // Item
    public Path get_path_for_display(boolean try_deep)
    //**********************************************************
    {
        if (item_context.is_trash) return null;
        //if (item_context.is_parent_of!=null) return null;
        Path item_path = get_item_path();
        if ( item_path == null)
        {
            item_context.logger.log(Stack_trace_getter.get_stack_trace(""));
            return null;
        }
        // for a file the displayed icon is built from the file itself, if supported:
        if ( !item_path.toFile().isDirectory())
        {
            return get_item_path();
        }

        if ( !try_deep) return null;

        // for a folder we have 2 ways to provide an icon
        // 1) an image is taken from the folder and used as icon
        // 2) multiple images are taken from the folder to form an animated gif icon

        // try to find an icon for the folder
        return get_an_image_down_in_the_tree_files(item_path);
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
            if ( dbg) item_context.logger.log("❗ WARNING: dir is access denied: "+local_path);
            return null;
        }
        if ( files.length == 0)
        {
            if ( dbg) item_context.logger.log("✅ dir is empty: "+local_path);
            return null;
        }
        Arrays.sort(files);
        List<Path> images_in_folder = null;
        if( make_animated_gif)
        {
            images_in_folder = new ArrayList<>();
        }
        for ( File f : files)
        {
            if (f.isDirectory()) continue; // ignore folders
            if (!Guess_file_type.is_this_file_extension_an_image(f,item_context.owner,item_context.logger)) continue; // ignore non images
            if( make_animated_gif)
            {
                Objects.requireNonNull(images_in_folder).add(f.toPath());
            }
            else
            {
                return f.toPath();
            }
        }
        if( make_animated_gif)
        {
            item_context.logger.log("✅ make_animated_gif");

            if ( Objects.requireNonNull(images_in_folder).isEmpty())
            {
                return null;
            }

            Optional<Path> returned = Animated_gif_from_folder_content.make_animated_gif_from_images_in_folder(
                    item_context.owner,
                    new Path_list_provider_for_file_system(local_path,item_context.owner,item_context.logger),
                    item_context.path_comparator_source,
                    images_in_folder,
                    image_properties_cache,
                    item_context.aborter, item_context.logger);
            if ( returned.isEmpty())
            {
                item_context.logger.log("❌ make_animated_gif_from_all_images_in_folder fails");
                if (!images_in_folder.isEmpty())
                {
                    return images_in_folder.get(0);
                }
            }
            else
            {
                item_context.logger.log("✅ make_animated_gif_from_all_images_in_folder OK");

                return returned.get();
            }
        }

        return null; // no image found
    }


    //**********************************************************
    @Override // Item
    public void set_selected_look_specific(boolean selected)
    //**********************************************************
    {
        if ( selected )
        {
            Look_and_feel_manager.give_button_a_selected_file_style(button, item_context.owner, item_context.logger);
        }
        else
        {
            Look_and_feel_manager.give_button_a_file_style(button,item_context.owner,item_context.logger);
        }
    }


    public Button get_button(){ return button;}


    //**********************************************************
    public static Button button_for_a_directory(Top_left_provider top_left_provider, Shutdown_target shutdown_target, Item_context  item_context, String text, double width, double height)
    //**********************************************************
    {
        String extended_text = text;
        if ( item_context.item_path != null)
        {
            if (Files.isSymbolicLink(item_context.item_path))
            {
                extended_text += " **Symbolic link** ";
            }
        }
        Button returned_button = new Button(extended_text);
        returned_button.setMnemonicParsing(false);// avoid suppression of first underscore in names

        Look_and_feel_manager.set_button_look_as_folder(returned_button, height, item_context.color,item_context.owner,item_context.logger);
        returned_button.setTextAlignment(TextAlignment.RIGHT);
        //double computed_text_width = icons_width + estimate_text_width(text2);

        if (item_context.item_path == null)
        {
            // protect crash when going up: root has no parent
            if ( !text.isEmpty()) item_context.logger.log("✅ WARNING no action for folder ->"+text+"<-");

            if ( item_context.is_trash) {
                returned_button.setOnAction(event -> {
                    Popups.popup_warning("❗ WARNING","NO trash on this media: probably it is read only",true,item_context.owner,item_context.logger);
                });
            }
            return returned_button;
        }

        returned_button.setOnAction(event -> {
            if ( dbg) item_context.logger.log("Button pressed for folder:"+text);
            //Path local_item_path = get_item_path();
            if (item_context.item_path == null)
            {
                // protect crash when going up: root has no parent
                item_context.logger.log("❗ WARNING no action for folder:"+text);
                return;
            }

            // as the button represents a folder, clicking on it "opens" that folder
            // = we create a NEW browser, as a replacement

            if( dbg) item_context.logger.log("Item_folder button setOnAction calling replace_different_folder");

            // this works when going "down", path is the new target path, therefore going back is the parent of that
            Path old_folder_path = item_context.item_path.getParent();
            if ( item_context.item_path.getParent() != null)
            {
                // this works when going up
                //if ( dbg)
                item_context.logger.log("is_up_button");
                old_folder_path = item_context.item_path.getParent();
            }
            item_context.logger.log("old_folder_path="+old_folder_path);
            item_context.logger.log("top_left_provider.get_top_left()="+top_left_provider.get_top_left());

            Window_builder.replace_different_folder(
                    item_context.application,
                    shutdown_target,
                    Window_type.File_system_2D,
                    new Path_list_provider_for_file_system(item_context.item_path,item_context.owner,item_context.logger),
                    old_folder_path,
                    top_left_provider.get_top_left(),
                    item_context.owner,
                    item_context.logger);

        });

        Drag_and_drop.init_drag_and_drop_receiver_side(item_context.path_list_provider.get_move_in_provider(),returned_button,item_context.owner,item_context.item_path,item_context.is_trash,item_context.logger);

        give_a_menu_to_the_button(item_context,returned_button,null);
        return returned_button;
    }


    //**********************************************************
    public void add_how_many_files_deep_folder(
            LongAdder count,
            Button button,
            String text,
            Path path,
            Aborter aborter)
    //**********************************************************
    {
        count.increment();

        Runnable r = () -> {
            Long how_many_files_deep = RAM_caches.folder_file_count_cache.get(path);
            if ( how_many_files_deep == null)
            {
                how_many_files_deep = (Long) Static_files_and_paths_utilities.get_how_many_files_deep(path, aborter, item_context.owner, item_context.logger);
                RAM_caches.folder_file_count_cache.put(path,how_many_files_deep);
            }
            count.decrement();
            String extended_text =  text + " (" + how_many_files_deep + " files)";

            String finalExtended_text = extended_text;
            Jfx_batch_injector.inject(() -> {
                button.setText(finalExtended_text);
                //browser.scene_geometry_changed("number of files in button", true);
            },item_context.logger);
        };
        Actor_engine.execute(r, "Compute how many files deep", item_context.logger);
    }


    //**********************************************************
    public void add_total_size_deep_folder(LongAdder count, Button button, String text, Path path)
    //**********************************************************
    {
        count.increment();
        Runnable r = () -> {

            Long bytes = RAM_caches.folder_total_size_cache.get(path);
            if ( bytes == null)
            {
                //logger.log(path+" length not found in cache");
                Sizes sizes = Static_files_and_paths_utilities.get_sizes_on_disk_deep(path, item_context.aborter, item_context.owner, item_context.logger);
                bytes = (Long) sizes.bytes();
                //logger.log(path+" not found in cache, length is "+bytes+ "bytes");
                RAM_caches.folder_total_size_cache.put(path,bytes);
            }
            else
            {
                item_context.logger.log(path+" length found in cache "+bytes);
            }
            count.decrement();

            StringBuilder sb =  new StringBuilder();
            sb.append(text);
            sb.append("       ");
            sb.append(Static_files_and_paths_utilities.get_1_line_string_for_byte_data_size(bytes,item_context.owner,item_context.logger));

            //sb.append(", ");
            //sb.append(sizes.files());
            //sb.append(" ");
            //sb.append(My_I18n.get_I18n_string("Files",logger));
            String extended_text = sb.toString();
            Jfx_batch_injector.inject(() -> {
                button.setText(extended_text);
                //browser.scene_geometry_changed("number of files in button", true);
            },item_context.logger);
        };
        Actor_engine.execute(r, "Add total length in a folder's button", item_context.logger);
    }



    @Override
    public Node get_Node() {
        return button;
    }


    @Override
    public double get_Width() {
        if ( button != null ) {
            return button.getWidth();
        }
        item_context.logger.log(Stack_trace_getter.get_stack_trace("SHOULD NOT HAPPEN"));
        return 0;
    }


    //**********************************************************
    @Override
    public double get_Height()
    //**********************************************************
    {
        if ( button == null ) return 0;
        if ( button.getHeight() == 0)
        {
            // until it is laid out, the button height is zero
            // so this entity CANNOT be used for "layout"... unless...
            // one cheats
            //logger.log("implausible button.getHeight()");
            return 40;
        }
        return button.getHeight();
    }


    //**********************************************************
    @Override
    public String get_string()
    //**********************************************************
    {
        Path item_path = get_item_path();
        if ( item_path == null)
        {
            item_context.logger.log(Stack_trace_getter.get_stack_trace(""));
            return "Folder has no path ?";
        }
        return "is dir: " + item_path.toAbsolutePath();
    }


}
