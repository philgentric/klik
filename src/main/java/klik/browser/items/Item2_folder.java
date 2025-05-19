//SOURCES ../../util/ui/Text_frame.java
package klik.browser.items;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.stage.Window;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.audio.Audio_player;
import klik.browser.*;
import klik.browser.classic.Folder_path_list_provider;
import klik.browser.icons.Icon_destination;
import klik.browser.icons.Icon_factory_actor;
import klik.browser.icons.animated_gifs.Animated_gif_from_folder;
import klik.browser.icons.image_properties_cache.Image_properties_RAM_cache;
import klik.browser.virtual_landscape.*;
import klik.look.Font_size;
import klik.look.Look_and_feel_manager;
import klik.properties.Booleans;
import klik.properties.Experimental_features;
import klik.properties.Non_booleans;
import klik.util.execute.System_open_actor;
import klik.util.files_and_paths.Guess_file_type;
import klik.util.files_and_paths.Sizes;
import klik.util.files_and_paths.Static_files_and_paths_utilities;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import klik.util.ui.Jfx_batch_injector;
import klik.util.ui.Popups;
import klik.util.ui.Text_frame;

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
import java.util.concurrent.atomic.AtomicInteger;


//**********************************************************
public class Item2_folder extends Item2 implements Icon_destination
//**********************************************************
{
    public static final boolean dbg = true;
    public Button button;
    public Label label;
    public final boolean is_trash;
    public final boolean is_parent;
    public String text;
    private static DateTimeFormatter date_time_formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final Image_properties_RAM_cache image_properties_RAM_cache;
    private final Shutdown_target shutdown_target;
    private final Top_left_provider top_left_provider;
    protected final Path_comparator_source path_comparator_source;


    //**********************************************************
    public Item2_folder(
            Window owner,
            Scene scene,
            Selection_handler selection_handler,
            Icon_factory_actor icon_factory_actor,
            Color color,
            String text_,
            double height,
            boolean is_trash_,
            boolean is_parent_,
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
                owner,
                scene,
                selection_handler,
                icon_factory_actor,
                color,
                path_list_provider,
                aborter,
                logger);
        this.image_properties_RAM_cache = image_properties_RAM_cache;
        this.shutdown_target = shutdown_target;
        this.top_left_provider = top_left_provider;
        this.path_comparator_source = path_comparator_source;
        text = text_;
        is_trash = is_trash_;
        is_parent = is_parent_;
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

        double button_width = Non_booleans.get_column_width();
        if ( button_width < Virtual_landscape.MIN_COLUMN_WIDTH) button_width = Virtual_landscape.MIN_COLUMN_WIDTH;

        Path local = get_item_path();
        if ( local == null)
        {
            logger.log("PANIC PATH is null");
            return;
        }
        if (Files.isDirectory(local))
        {
            button_for_a_directory(text, button_width, height, color);
        }
        else
        {
            logger.log("SHOULD NOT HAPPEN Item2_folder path is not a dorectory!");
        }
        Look_and_feel_manager.set_button_look(button,false);
        button.setManaged(true); // means the parent tells the button its layout
        button.setMnemonicParsing(false);// avoid suppression of first underscore in names
        button.setTextOverrun(OverrunStyle.ELLIPSIS);
        Tooltip.install(button,new Tooltip(get_item_path().toString()));

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
    @Override // Item2
    public void you_are_visible_specific()
    //**********************************************************
    {

    }

    //**********************************************************
    @Override // Item2
    public void you_are_invisible_specific()
    //**********************************************************
    {

    }


    //**********************************************************
    @Override // Item2
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
        logger.log(Stack_trace_getter.get_stack_trace("SHOULD NOT HAPPEN"));
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
        logger.log("Item_button get_path_for_display_icon_destination DEEP !???");
        return get_path_for_display(true);
    }

    // this call is intended only from a working thread
    // in the icon factory as
    //**********************************************************
    @Override // Item2
    public Path get_path_for_display(boolean try_deep)
    //**********************************************************
    {
        if (is_trash) return null;
        if (is_parent) return null;
        // for a file the displayed icon is built from the file itself, if supported:
        if ( !get_item_path().toFile().isDirectory()) return get_item_path();

        if ( !try_deep) return null;

        logger.log("YOPOPOPOOOOO");

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
            if ( dbg) logger.log("WARNING: dir is access denied: "+local_path);
            return null;
        }
        if ( files.length == 0)
        {
            if ( dbg) logger.log("dir is empty: "+local_path);
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
            if (!Guess_file_type.is_file_an_image(f)) continue; // ignore non images
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
            logger.log("make_animated_gif!?");

            if ( Objects.requireNonNull(images_in_folder).isEmpty())
            {
                return null;
            }

            Path returned = Animated_gif_from_folder.make_animated_gif_from_images_in_folder(
                    owner,
                    new Folder_path_list_provider(local_path),
                    path_comparator_source,
                    images_in_folder,
                    image_properties_RAM_cache,
                    aborter, logger);
            if ( returned == null)
            {
                logger.log("make_animated_gif_from_all_images_in_folder fails");
                if (!images_in_folder.isEmpty()) return images_in_folder.get(0).toPath();
            }
            else
            {
                logger.log("make_animated_gif_from_all_images_in_folder OK");

                return returned;
            }
        }

        return null; // no image found
    }


    //**********************************************************
    @Override // Item2
    public void set_is_unselected_internal()
    {
        Look_and_feel_manager.give_button_a_file_style(button);
    }


    //**********************************************************
    @Override // Item2
    public void set_is_selected_internal()
    //**********************************************************
    {
        Look_and_feel_manager.give_button_a_selected_file_style(button);
    }


    public Button get_button(){ return button;}

    //**********************************************************
    private void button_for_a_non_image_file(String text, double width)
    //**********************************************************
    {

        if ( Booleans.get_boolean(Booleans.SINGLE_COLUMN))
        {
            StringBuilder sb = new StringBuilder();
            try {
                FileTime x = Files.readAttributes(get_item_path(), BasicFileAttributes.class).creationTime();
                LocalDateTime ldt = x.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                sb.append(ldt.format(date_time_formatter));
                sb.append("                 ");
                sb.append(Static_files_and_paths_utilities.get_1_line_string_for_byte_data_size(get_item_path().toFile().length(),logger));
                sb.append("                 ");
                if (!get_item_path().toFile().canWrite())
                {
                    sb.append("Not Writable!                 ");
                }
            } catch (IOException e) {
                logger.log_exception("",e);
            }
            String size_string = sb.toString();
            label = new Label(size_string);
            //Font_size.set_preferred_font_size(label,logger);
            Font_size.apply_font_size(label,logger);
            button = new Button(text,label);
        }
        else
        {
            button = new Button(text);
        }

        button.setMinWidth(width);
        button.setPrefWidth(width);
        //Font_size.set_preferred_font_size(button,logger);
        Font_size.apply_font_size(button,logger);

        Look_and_feel_manager.give_button_a_file_style(button);
        button.setTextAlignment(TextAlignment.RIGHT);

        button.setOnAction(event -> {

            logger.log("ON ACTION " + get_item_path().toAbsolutePath());

            if ( Guess_file_type.is_this_path_a_text(get_item_path()))
            {
                logger.log("opening text: " + get_item_path().toAbsolutePath());
                Text_frame.show(get_item_path(),logger);
                return;
            }
            if ( Guess_file_type.is_this_path_an_audio_playlist(get_item_path()))
            {
                logger.log("opening audio playlist: " + get_item_path().toAbsolutePath());
                Audio_player.play_playlist(get_item_path().toFile(),logger);
                return;
            }
            if (Booleans.get_boolean(Experimental_features.enable_image_playlists.name()) )
            {
                if (Guess_file_type.is_this_path_an_image_playlist(get_item_path())) {
                    logger.log("NOT IMPLEMENTED opening image playlist: " + get_item_path().toAbsolutePath());
                    //New_window_context2.open_new_image_playlist(get_item_path(), owner, get_item_path().getParent(),top_left_provider.get_top_left(),logger);
                    return;
                }
            }
            if ( Guess_file_type.is_this_path_a_music(get_item_path()))
            {
                if ( !Guess_file_type.is_this_a_video_or_audio_file(owner,get_item_path(),logger))
                {
                    logger.log("Item_button, opening audio file: " + get_item_path().toAbsolutePath());
                    Audio_player.play_song_in_separate_process(get_item_path().toFile(),logger);
                    return;
                }
            }
            logger.log("asking the system to open: " + get_item_path().toAbsolutePath());
            System_open_actor.open_with_system(owner,get_item_path(), aborter,logger);
        });

        give_a_menu_to_the_button(button,label);
    }


    //**********************************************************
    public void button_for_a_directory(String text, double width, double height, Color color)
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

        Look_and_feel_manager.set_button_look_as_folder(button, height, color);
        button.setTextAlignment(TextAlignment.RIGHT);
        //double computed_text_width = icons_width + estimate_text_width(text2);

        if (get_item_path() == null)
        {
            // protect crash when going up: root has no parent
            logger.log("WARNING no action for folder:"+text);

            if ( text.equals("Trash")) {
                button.setOnAction(event -> {
                    Popups.popup_warning(owner,"WARNING","NO trash on this media: probably it is read only",true,logger);
                });
            }
            return;
        }

        button.setOnAction(event -> {
            logger.log("BUTTON PRESSED for folder:"+text);

            if (get_item_path() == null)
            {
                // protect crash when going up: root has no parent
                logger.log("WARNING no action for folder:"+text);
                return;
            }

            // as the button represents a folder, clicking on it "opens" that folder
            // = we create a NEW browser, as a replacement

            if( dbg) logger.log("Item2_folder button setOnAction calling replace_different_folder");

            logger.log("\n\nget_item_path "+get_item_path()+"\npath_list_provider.get_folder_path()="+path_list_provider.get_folder_path());
            Path old_folder_path;
            Path top_left;
            if ( is_parent())
            {
                top_left = top_left_provider.get_top_left();
                if ( top_left != null) top_left = top_left.getParent();
                old_folder_path = path_list_provider.get_folder_path(); // when the button is the parent aka up button, the old path is the current path
                logger.log("\n\nreplace_different_folder IS PARENT old_folder_path "+old_folder_path+" \ntop_left "+top_left);
            }
            else
            {
                top_left = top_left_provider.get_top_left();
                old_folder_path = get_item_path().getParent(); // this works when going "down", path is the new target path, therefore going back is the parent of that
                logger.log("\n\nreplace_different_folder old_folder_path "+old_folder_path+" \ntop_left_provider.get_top_left() "+top_left_provider.get_top_left());
            }
            New_window_context2.replace_different_folder(
                    shutdown_target,
                    get_item_path(),
                    owner,
                    old_folder_path,
                    top_left,
                    logger);

        });

        Drag_and_drop.init_drag_and_drop_receiver_side(path_list_provider.get_move_provider(),get_Node(),owner,get_item_path(),is_trash(),logger);

        give_a_menu_to_the_button(button,label);

        //if ( Non_booleans.get_show_folder_size(logger)) show_how_many_files_deep_folder(button,text,path,aborter,logger);

    }


    //**********************************************************
    public void add_how_many_files_deep_folder(
            AtomicInteger count,
            Button button,
            String text,
            Path path,
            Map<Path, Long> folder_file_count_cache,
            Aborter aborter,
            Logger logger)
    //**********************************************************
    {
        count.incrementAndGet();

        Runnable r = () -> {
            Long how_many_files_deep = folder_file_count_cache.get(path);
            if ( how_many_files_deep == null)
            {
                how_many_files_deep = Static_files_and_paths_utilities.get_how_many_files_deep(path, aborter, logger);
                folder_file_count_cache.put(path,how_many_files_deep);
            }
            count.decrementAndGet();
            String extended_text =  text + " (" + how_many_files_deep + " files)";

            String finalExtended_text = extended_text;
            Jfx_batch_injector.inject(() -> {
                button.setText(finalExtended_text);
                //browser.scene_geometry_changed("number of files in button", true);
            },logger);
        };
        Actor_engine.execute(r, logger);
    }


    //**********************************************************
    public void add_total_size_deep_folder(AtomicInteger count, Button button, String text, Path path,
                                           Map<Path, Long> folder_total_sizes,
                                           Aborter aborter, Logger logger)
    //**********************************************************
    {
        count.incrementAndGet();
        Runnable r = () -> {

            Long bytes = folder_total_sizes.get(path);
            if ( bytes == null)
            {
                //logger.log(path+" size not found in cache");
                Sizes sizes = Static_files_and_paths_utilities.get_sizes_on_disk_deep(path, aborter, logger);
                bytes = sizes.bytes();
                //logger.log(path+" not found in cache, size is "+bytes+ "bytes");
                folder_total_sizes.put(path,bytes);
            }
            else
            {
                logger.log(path+" size found in cache "+bytes);
            }
            count.decrementAndGet();

            StringBuilder sb =  new StringBuilder();
            sb.append(text);
            sb.append("       ");
            sb.append(Static_files_and_paths_utilities.get_1_line_string_for_byte_data_size(bytes,logger));

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
        Actor_engine.execute(r, logger);
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
    public boolean is_parent()
    //**********************************************************
    {
        return is_parent;
    }

    //**********************************************************
    @Override
    public String get_string()
    //**********************************************************
    {
        return "is dir: " + get_item_path().toAbsolutePath();
    }


}
