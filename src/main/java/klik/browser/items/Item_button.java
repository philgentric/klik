package klik.browser.items;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.actor.Job;
import klik.audio.Audio_player;
import klik.browser.Browser;
import klik.browser.Browser_creation_context;
import klik.browser.Drag_and_drop;
import klik.browser.Image_and_rotation;
import klik.browser.icons.Icon_destination;
import klik.browser.icons.Icon_manager;
import klik.browser.icons.animated_gifs.Animated_gif_from_folder;
import klik.files_and_paths.Files_and_Paths;
import klik.files_and_paths.Guess_file_type;
import klik.files_and_paths.Sizes;
import klik.look.Font_size;
import klik.look.Look_and_feel_manager;
import klik.properties.Static_application_properties;
import klik.util.*;
import klik.util.execute.System_open_actor;

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
public class Item_button extends Item implements Icon_destination
//**********************************************************
{
    public static final boolean dbg = false;
    public Button button;
    public Label label;
    public final boolean is_dir;
    public final boolean is_trash;
    public final boolean is_parent;
    public String text;
    //private Job job;
    private static DateTimeFormatter date_time_formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    //**********************************************************
    public Item_button(
            Browser browser,
            Path path_,
            Color color,
            String text_,
            double height,
            boolean is_trash_, boolean is_parent_,
            Logger logger)
    //**********************************************************
    {
        super(browser, path_, color, logger);
        text = text_;
        is_trash = is_trash_;
        is_parent = is_parent_;
        if (path == null) {
            is_dir = false;
            button = new Button("----------");
            button.setPrefWidth(Control.USE_COMPUTED_SIZE);
            button.setTextOverrun(OverrunStyle.ELLIPSIS);
            button.setGraphicTextGap(20);
            Look_and_feel_manager.set_button_look(button,false);
            return;
        }

        double button_width = Static_application_properties.get_column_width(logger);
        if ( button_width < Icon_manager.MIN_COLUMN_WIDTH) button_width = Icon_manager.MIN_COLUMN_WIDTH;

        if (Files.isDirectory(path))
        {
            is_dir = true;
            button_for_a_directory(text, button_width, height, color);
        }
        else
        {
            is_dir = false;
            button_for_a_non_image_file( text,button_width);
        }
        Look_and_feel_manager.set_button_look(button,false);
        button.setManaged(true); // means the parent tells the button its layout
        button.setMnemonicParsing(false);// avoid suppression of first underscore in names
        button.setTextOverrun(OverrunStyle.ELLIPSIS);
        Drag_and_drop.init_drag_and_drop_sender_side(get_Node(),browser,path,logger);

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
    public void receive_icon(Image_and_rotation image_and_rotation)
    //**********************************************************
    {
        logger.log(Stack_trace_getter.get_stack_trace("SHOULD NOT HAPPEN"));
    }


    //**********************************************************
    public Path get_true_path()
    //**********************************************************
    {
        return path;
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
    @Override // Item
    public Path get_path_for_display(boolean try_deep)
    //**********************************************************
    {
        if (is_trash) return null;
        if (is_parent) return null;
        // for a file the displayed icon is built from the file itself, if supported:
        if ( !path.toFile().isDirectory()) return path;

        if ( !try_deep) return null;

        logger.log("YOPOPOPOOOOO");

        // for a folder we have 2 ways to provide an icon
        // 1) an image is taken from the folder and used as icon
        // 2) multiple images are taken from the folder to form an animated gif icon

        // try to find an icon for the folder
        return get_an_image_down_in_the_tree_files(path);
        /*
        no recursive madness please!
        if ( returned != null) return returned;
        // ok, so we did not find an image file in the folder
        // let us go down sub directories (if any)
        return get_an_image_down_in_the_tree_folders(path);
        */

    }

    //**********************************************************
    @Override
    public void cancel_custom()
    //**********************************************************
    {
        //Actor_engine.cancel_job(job);
        //job = null;
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

            Path returned = Animated_gif_from_folder.make_animated_gif_from_all_images_in_folder(browser.my_Stage.the_Stage, local_path,  images_in_folder,  logger);
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
    @Override // Item
    public void set_is_unselected_internal()
    {
        Look_and_feel_manager.give_button_a_file_style(button);
    }


    //**********************************************************
    @Override // Item
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


        if ( Static_application_properties.get_single_column(logger))
        {
            /*double space_lenght_d = Look_and_feel_manager.get_look_and_feel_instance(logger).estimate_text_width(" ");// text.length();
            double text_lenght_d = Look_and_feel_manager.get_look_and_feel_instance(logger).estimate_text_width(text);// text.length();
            int text_lenght = (int) (text_lenght_d/space_lenght_d);
            for(int i = 0 ; i < 200-text_lenght; i++)
            {
                sb.append(" ");
            }*/
            StringBuilder sb = new StringBuilder();
            try {
                FileTime x = Files.readAttributes(path, BasicFileAttributes.class).creationTime();
                LocalDateTime ldt = x.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                sb.append(ldt.format(date_time_formatter));
                sb.append("                 ");
                sb.append(Files_and_Paths.get_1_line_string_for_byte_data_size(path.toFile().length(),logger));
                sb.append("                 ");
                if (!path.toFile().canWrite())
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
            if ( Guess_file_type.is_this_path_a_text(path))
            {
                logger.log("opening text: " + path.toAbsolutePath());
                Text_frame.show(path,logger);
                return;
            }
            if ( Guess_file_type.is_this_path_a_playlist(path))
            {
                logger.log("opening audio playlist: " + path.toAbsolutePath());
                Audio_player.play_playlist(path.toFile(),logger);
                return;
            }
            if ( Guess_file_type.is_this_path_a_music(path))
            {
                if ( !Guess_file_type.is_this_a_video_or_audio_file(browser.my_Stage.the_Stage,path,logger))
                {
                    logger.log("opening audio file: " + path.toAbsolutePath());
                    Audio_player.play_song(path.toFile(),logger);
                    return;
                }
            }
            logger.log("asking the system to open: " + path.toAbsolutePath());
            System_open_actor.open_with_system(browser,path,logger);
        });

        give_a_menu_to_the_button(button,label);
    }


    //**********************************************************
    public void button_for_a_directory(String text, double width, double height, Color color)
    //**********************************************************
    {
        String extended_text = text;
        //if ( Static_application_properties.get_show_folder_size(logger)) {    text2 += "..."; // room reservation since the size will be added later}
        if ( path != null)
        {
            if (Files.isSymbolicLink(path)) {
                extended_text += " **Symbolic link** ";
            }
        }
        button = new Button(extended_text);
        button.setMnemonicParsing(false);// avoid suppression of first underscore in names

        Look_and_feel_manager.set_button_look_as_folder(button, height, color);
        button.setTextAlignment(TextAlignment.RIGHT);
        //double computed_text_width = icons_width + estimate_text_width(text2);

        if (path == null)
        {
            // protect crash when going up: root has no parent
            logger.log("WARNING no action for folder:"+text);

            if ( text.equals("Trash")) {
                button.setOnAction(event -> {
                    Popups.popup_warning(browser.my_Stage.the_Stage,"WARNING","NO trash on this media: probably it is read only",true,logger);
                });
            }
            return;
        }

        button.setOnAction(event -> {

            if (path == null)
            {
                // protect crash when going up: root has no parent
                logger.log("WARNING no action for folder:"+text);
                return;
            }


            // if the button represents a folder, clicking on it "opens" that folder
            // = we create a NEW browser, as a replacement

            if( dbg) logger.log("Item_button button setOnAction calling replace_different_folder");
            Browser_creation_context.replace_different_folder(path,browser,logger);

        });

        Drag_and_drop.init_drag_and_drop_receiver_side(get_Node(),browser,path,is_trash(),logger);

        give_a_menu_to_the_button(button,label);

        //if ( Static_application_properties.get_show_folder_size(logger)) show_how_many_files_deep_folder(button,text,path,aborter,logger);

    }


    //**********************************************************
    public void add_how_many_files_deep_folder(Button button, String text, Path path, Map<Path, Long> folder_file_count_cache, Aborter aborter, Logger logger)
    //**********************************************************
    {

        Runnable r = () -> {
            Long how_many_files_deep = folder_file_count_cache.get(path);
            if ( how_many_files_deep == null)
            {
                how_many_files_deep = Files_and_Paths.get_how_many_files_deep(path, aborter, logger);
                folder_file_count_cache.put(path,how_many_files_deep);
            }
            String extended_text =  text + " (" + how_many_files_deep + " files)";

            String finalExtended_text = extended_text;
            Fx_batch_injector.inject(() -> {
                button.setText(finalExtended_text);
                //browser.scene_geometry_changed("number of files in button", true);
            },logger);
        };
        Actor_engine.execute(r,aborter, logger);
    }


    //**********************************************************
    public void add_total_size_deep_folder(AtomicInteger count, Button button, String text, Path path, Map<Path, Long> folder_total_sizes, Aborter aborter, Logger logger)
    //**********************************************************
    {
        count.incrementAndGet();
        Runnable r = () -> {

            Long bytes = folder_total_sizes.get(path);
            if ( bytes == null)
            {
                logger.log(path+" size not found in cache");
                Sizes sizes = Files_and_Paths.get_sizes_on_disk_deep(path, aborter, logger);
                bytes = sizes.bytes();
                folder_total_sizes.put(path,bytes);
            }
            else
            {
                logger.log(path+" size  found in cache "+bytes);
            }
            count.decrementAndGet();

            StringBuilder sb =  new StringBuilder();
            sb.append(text);
            sb.append("       ");
            sb.append(Files_and_Paths.get_1_line_string_for_byte_data_size(bytes,logger));

            //sb.append(", ");
            //sb.append(sizes.files());
            //sb.append(" ");
            //sb.append(I18n.get_I18n_string("Files",logger));
            String extended_text = sb.toString();
            Fx_batch_injector.inject(() -> {
                button.setText(extended_text);
                //browser.scene_geometry_changed("number of files in button", true);
            },logger);
        };
        Actor_engine.execute(r,aborter, logger);
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
            // for some reason until some scroll is applied the height info is 0 ???
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
        if (is_dir) return "is dir: " + path.toAbsolutePath();
        return "is file: " + path.toAbsolutePath();
    }


}
