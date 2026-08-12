// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ../../util/ui/Text_frame_with_labels.java
//SOURCES ../../util/ui/Text_frame.java

package klikr.browser_core.items;

import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.text.TextAlignment;
import klikr.Window_builder;
import klikr.Window_type;
import klikr.audio.player.The_audio_player;
import klikr.browser_core.icons.image_properties_cache.Image_properties;
import klikr.javalin.monaco.Javalin_monaco;
import klikr.path_lists.Path_list_provider_for_playlist;
import klikr.settings.boolean_features.Feature_change_target;
import klikr.util.cache.Klikr_cache;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Actor_engine;
import klikr.browser_core.Drag_and_drop;
import klikr.browser_core.Image_and_properties;
import klikr.path_lists.Path_list_provider_for_file_system;
import klikr.browser_core.icons.Icon_destination;
import klikr.browser_core.icons.Icon_factory_actor;
import klikr.util.animated_gifs.Animated_gif_from_folder_content;
import klikr.browser_core.virtual_landscape.*;
import klikr.look.Font_size;
import klikr.look.Look_and_feel_manager;
import klikr.settings.Non_booleans_properties;
import klikr.settings.boolean_features.Feature;
import klikr.settings.boolean_features.Feature_cache;
import klikr.util.execute.System_open_actor;
import klikr.util.files_and_paths.Guess_file_type;
import klikr.util.files_and_paths.Sizes;
import klikr.util.files_and_paths.Static_files_and_paths_utilities;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;
import klikr.util.ui.Jfx_batch_injector;
import klikr.util.ui.Popups;
import klikr.util.ui.Text_frame;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.LongAdder;


//**********************************************************
public class Item_file_no_icon extends Item_file implements Icon_destination
//**********************************************************
{
    public static final boolean dbg = false;

    private static final boolean make_animated_gif = true;

    public Button button;
    public Label details; // may be null, as this is the text displayed when 'Show_single_column_with_details' == true
    public String text;
    private static final DateTimeFormatter date_time_formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final Klikr_cache<Path, Image_properties> image_properties_cache;
    private final Feature_change_target feature_change_target;

    //**********************************************************
    public Item_file_no_icon(
            Item_context item_context,
            Selection_handler selection_handler,
            Feature_change_target feature_change_target,
            Icon_factory_actor icon_factory_actor,
            String text_,
            Klikr_cache<Path, Image_properties> image_properties_cache)
    //**********************************************************
    {
        super(item_context,selection_handler,icon_factory_actor);
        this.feature_change_target = feature_change_target;
        this.image_properties_cache = image_properties_cache;
        text = text_;
        if (item_context.item_path == null) {
            item_context.logger.log(Stack_trace_getter.get_stack_trace("❌ FATAL: path is null"+item_context.path_list_provider.get_key()));
            return;
        }

        double button_width = Non_booleans_properties.get_column_width(item_context.owner);
        if ( button_width < Virtual_landscape.MIN_COLUMN_WIDTH) button_width = Virtual_landscape.MIN_COLUMN_WIDTH;

        button_for_a_non_image_file( text,button_width);

        Look_and_feel_manager.set_region_look(button,false,item_context.owner,item_context.logger);
        button.setManaged(true); // means the parent tells the button its layout
        button.setMnemonicParsing(false);// avoid suppression of first underscore in names
        button.setTextOverrun(OverrunStyle.ELLIPSIS);
        if (Feature_cache.get(Feature.Show_file_names_as_tooltips))
        {
            Tooltip.install(button, new Tooltip(  item_context.item_path.toString()));
        }
        Drag_and_drop.init_drag_and_drop_sender_side(get_Node(),selection_handler,item_context.item_path,item_context.logger);
    }



    //**********************************************************
    @Override // Icon_destination
    public Path get_item_path()
    //**********************************************************
    {
        return item_context.item_path;
    }

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
        item_context.logger.log(Stack_trace_getter.get_stack_trace("SHOULD NOT HAPPEN"));
    }


    //**********************************************************
    public Path get_true_path()
    //**********************************************************
    {
        return get_item_path();
    }

    //**********************************************************
    @Override // Icon_destination
    public Path get_path_for_display_icon_destination()
    //**********************************************************
    {
        item_context.logger.log("Item_file_no_icon get_path_for_display_icon_destination DEEP !???");
        return get_path_for_display(true);
    }

    // this call is intended only from a working thread
    // in the icon factory as
    //**********************************************************
    @Override // Item
    public Path get_path_for_display(boolean try_deep)
    //**********************************************************
    {
        Path item_path = get_item_path();
        if ( item_path == null)
        {
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
            if ( dbg) item_context.logger.log("❗ dir is empty: "+local_path);
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
                images_in_folder.add(f.toPath());
            }
            else
            {
                // use the first image as icon
                return f.toPath();
            }
        }
        if( make_animated_gif)
        {
            item_context.logger.log("✅ make_animated_gif");

            if ( images_in_folder.isEmpty())
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
                if (dbg) item_context.logger.log("❗ make_animated_gif_from_all_images_in_folder fails");
                // use the first image as icon, if any
                if (!images_in_folder.isEmpty())
                {
                    return images_in_folder.get(0);
                }
            }
            else
            {
                if (dbg) item_context.logger.log("✅ make_animated_gif_from_all_images_in_folder OK");
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
      //  return;

        if (button == null) // always true?
        {
            item_context.logger.log("Item_file_no_icon button=null"+item_context.item_path);
            return;
        }
        /*if (selected)
        {
            if( dbg) item_context.logger.log("Item_file_no_icon set_selected_look for " + item_context.item_path);
            Look_and_feel_manager.give_button_a_selected_file_style(button, item_context.owner, item_context.logger);
        }
        else
        {
            if( dbg) item_context.logger.log("Item_file_no_icon unset_selected_look for "+item_context.item_path);
            Look_and_feel_manager.give_button_a_file_style(button,item_context.owner,item_context.logger);
        }
    */
    }


    public Button get_button(){ return button;}

    //**********************************************************
    private void button_for_a_non_image_file(String text, double width)
    //**********************************************************
    {

        if (item_context.item_path == null) {
            item_context.logger.log(Stack_trace_getter.get_stack_trace("item_path == nul for "+text));
            return;
        }

        if ( Feature_cache.get(Feature.Show_single_column_with_details))
        {
            StringBuilder sb = new StringBuilder();
            try {

                FileTime x = Files.readAttributes(item_context.item_path, BasicFileAttributes.class).creationTime();
                LocalDateTime ldt = x.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                sb.append(ldt.format(date_time_formatter));
                sb.append("                 ");
                sb.append(Static_files_and_paths_utilities.get_1_line_string_for_byte_data_size(item_context.item_path.toFile().length(),item_context.owner,item_context.logger));
                sb.append("                 ");
                if (!item_context.item_path.toFile().canWrite())
                {
                    sb.append("❗ Not Writable!                 ");
                }
            } catch (IOException e) {
                item_context.logger.log_exception("",e);
            }
            details = new Label(sb.toString());
            //Font_size.set_preferred_font_size(label,logger);
            Font_size.apply_global_font_size_to_Node(details,item_context.owner,item_context.logger);
            button = new Button(text, details);
        }
        else
        {
            button = new Button(text);
        }

        button.setMinWidth(width);
        button.setPrefWidth(width);
        //Font_size.set_preferred_font_size(button,logger);
        Font_size.apply_global_font_size_to_Node(button,item_context.owner,item_context.logger);

        Look_and_feel_manager.give_button_a_file_style(button,item_context.owner,item_context.logger);
        button.setTextAlignment(TextAlignment.RIGHT);

        button.setOnAction(event -> {

            if ( !item_context.item_path.toFile().exists())
            {
                Jfx_batch_injector.inject(() -> Popups.popup_warning( "❌  impossible, this path does not exist: " + item_context.item_path.toAbsolutePath(), "Sorry", false,item_context.owner,item_context.logger), item_context.logger);

                item_context.logger.log("❌  impossible, this path does not exist: " + item_context.item_path.toAbsolutePath());
                return;
            }
            item_context.logger.log("✅ ON ACTION " + item_context.item_path.toAbsolutePath());

            if ( Guess_file_type.is_this_path_extension_a_text(item_context.item_path,item_context.owner,item_context.logger))
            {
                item_context.logger.log("✅ opening text: " + item_context.item_path.toAbsolutePath());
                if ( Feature_cache.get(Feature.Use_monaco_for_text_edition))
                {
                    Javalin_monaco.read_only(item_context.application,item_context.item_path,item_context.logger);
                }
                else
                {
                    Text_frame.show(item_context.item_path, item_context.logger);
                }
                return;
            }
            if ( Guess_file_type.is_this_path_extension_an_audio_playlist(item_context.item_path,item_context.logger))
            {
                item_context.logger.log("✅ opening audio playlist: " + item_context.item_path.toAbsolutePath());
                Window_builder.additional_no_past(item_context.application,Window_type.Song_playlist,new Path_list_provider_for_playlist(item_context.item_path,  item_context.owner, item_context.aborter, item_context.logger),item_context.owner,item_context.logger);
                return;
            }

            if ( Guess_file_type.is_this_path_extension_a_music(item_context.item_path,item_context.logger))
            {
                if ( Guess_file_type.does_this_file_contain_an_audio_track(item_context.item_path,item_context.owner,item_context.logger))
                {
                    item_context.logger.log("✅ Item_file_no_icn, opening audio file: " + item_context.item_path.toAbsolutePath());
                    item_context.logger.log("path_list_provider="+item_context.path_list_provider.to_string());

                    The_audio_player.play_song_in_folder(item_context.application,item_context.item_path,item_context.owner,item_context.logger);
                    return;
                }
            }
            item_context.logger.log("✅ asking the system to open: " + item_context.item_path.toAbsolutePath());
            System_open_actor.open_with_system(item_context.application, item_context.item_path, item_context.owner,item_context.aborter,item_context.logger);
        });

        give_a_menu_to_the_button(item_context,button, details);
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
                how_many_files_deep = (Long) Static_files_and_paths_utilities.get_how_many_files_deep(path, aborter,  item_context.owner, logger);
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
        Actor_engine.execute(r, "Compute and display how many files deep",logger);
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
                //logger.log(path+" length not found in cache");
                Sizes sizes = Static_files_and_paths_utilities.get_sizes_on_disk_deep(path, aborter, item_context.owner, logger);
                bytes = (Long) sizes.bytes();
                //logger.log(path+" not found in cache, length is "+bytes+ "bytes");
                folder_total_sizes.put(path,bytes);
            }
            else
            {
                logger.log("✅ "+path+" length found in cache "+bytes);
            }
            count.decrement();

            StringBuilder sb =  new StringBuilder();
            sb.append(text);
            sb.append("       ");
            sb.append(Static_files_and_paths_utilities.get_1_line_string_for_byte_data_size(bytes,item_context.owner,logger));

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
        Actor_engine.execute(r, "Compute and display length deep",logger);
    }

    @Override
    public Node get_Node() {
        return button;
    }


    @Override
    public double get_Width() {
        if(button == null) return 0;
        return button.getWidth();
    }


    //**********************************************************
    @Override
    public double get_Height()
    //**********************************************************
    {
        if ( button == null) return 0;
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
    public String get_string()
    //**********************************************************
    {

        Path p = get_item_path();
        if ( p == null)
        {
            item_context.logger.log(Stack_trace_getter.get_stack_trace(""));
            return "Item_file_no_icon no path ?" ;
        }
        return "Item_file_no_icon, file: " + p.toAbsolutePath();
    }


}
