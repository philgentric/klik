//SOURCES ../../util/execute/System_open_actor.java
//SOURCES ../icons/*.java
//SOURCES ../../files_and_paths/Folder_size.java
//SOURCES ../../level3/metadata/Tag_stage.java
//SOURCES ./My_color.java
/*
//SOURCES ../icons/Icon_destination.java
//SOURCES ../icons/Icon_factory_request.java
//SOURCES ../../audio/Audio_info_frame.java
 */

package klik.browser.items;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.actor.Job;
import klik.browser.Browser;
import klik.browser.Browser_creation_context;
import klik.files_and_paths.Guess_file_type;
import klik.images.Exif_stage;
import klik.audio.Audio_info_frame;
import klik.util.From_disk;
import klik.util.execute.System_open_actor;
import klik.browser.icons.Icon_destination;
import klik.browser.icons.Icon_factory_request;
import klik.files_and_paths.Files_and_Paths;
import klik.files_and_paths.Folder_size;
import klik.level3.metadata.Tag_stage;
import klik.look.Font_size;
import klik.look.Look_and_feel;
import klik.look.Look_and_feel_manager;
import klik.look.my_i18n.I18n;
import klik.properties.Static_application_properties;
import klik.util.Logger;
import klik.util.Popups;
import klik.util.Stack_trace_getter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

//**********************************************************
public abstract class Item implements Icon_destination
//**********************************************************
{
    protected final boolean dbg = false;
    public static final boolean layout_dbg = false;
    public final int icon_size;
    public AtomicBoolean icon_fabrication_requested = new AtomicBoolean(false);
    Job icon_job; // this is needed to cancel the icon request when the item has become invisible

    protected Color color;
    protected Path path;
    protected final Browser browser;
    protected final Logger logger;
    public final Iconifiable_item_type item_type;
    public AtomicBoolean visible_in_scene = new AtomicBoolean(false);
    public final Aborter browser_aborter;

     // javafx_x and javafx_y are going to be used in Translate_X (resp. Y)
    // vertical scroll is managed by substracting the y_offset
    private double javafx_x;
    private double javafx_y;
    // this is the (top-left) position of the image
    // in the possibly hugely tall virtual screen
    // that contains all icons
    private double screen_x_of_image = 0;
    private double screen_y_of_image = 0;

    //**********************************************************
    public Item(Browser browser,
                Path path,
                Color color,
                Logger logger)
    //**********************************************************
    {
        this.browser_aborter = browser.aborter;
        this.browser = browser;
        this.path = path;
        this.logger = logger;
        this.color = color;
        item_type = Iconifiable_item_type.from_extension(path);
        icon_size = Static_application_properties.get_icon_size(logger);
    }

    public final Scene getScene()
    {
        return browser.the_Scene;
    }

    protected final Logger get_logger()
    {
        return logger;
    }


    public void set_translate_X(double dx)
    {
        if (get_Node() != null) get_Node().setTranslateX(dx);
    }

    public void set_translate_Y(double dy)
    {
        if (get_Node() != null) get_Node().setTranslateY(dy);
    }

    public void set_javafx_x(double x_) { javafx_x = x_; }
    public void set_javafx_y(double y_) {javafx_y = y_;}
    public double get_javafx_x() { return javafx_x; }
    public double get_javafx_y()
    {
        return javafx_y;
    }

    public void set_screen_x_of_image(double x_) { screen_x_of_image = x_; }
    public void set_screen_y_of_image(double y_) {screen_y_of_image = y_;}
    public double get_screen_x_of_image() { return screen_x_of_image; }
    public double get_screen_y_of_image() {return screen_y_of_image;}

    public abstract Node get_Node();

    public abstract double get_Width();
    public abstract double get_Height();
    public abstract boolean is_trash();
    public abstract boolean is_parent();


    @Override // Icon_destination
    public boolean get_icon_fabrication_requested() {
        return icon_fabrication_requested.get();
    }

    @Override // Icon_destination
    public void set_icon_fabrication_requested(boolean b) {
        icon_fabrication_requested.set(b);
    }

    @Override
    public Iconifiable_item_type get_item_type() {
        return item_type;
    }

    public abstract String get_string();

    public abstract void set_is_selected_internal();

    public abstract void set_is_unselected_internal();


    //**********************************************************
    public void request_icon_to_factory(int target_icon_size)
    //**********************************************************
    {

        if ( dbg) logger.log("request_icon_to_factory for:"+path);
        Icon_factory_request icon_factory_request = new Icon_factory_request(this, target_icon_size,
                new Aborter("Icon creation for "+path,logger));

        if (dbg) logger.log("icon request : queued! ");

        Icon_destination destination = icon_factory_request.destination;
        if (destination == null) {
            logger.log(Stack_trace_getter.get_stack_trace("SHOULD NOT HAPPEN icon factory : cancel! destination==null"));
            return;
        }

        if (icon_factory_request.destination.get_icon_fabrication_requested())
        {
            logger.log("dont do another icon request, another one is in flight");
            return;
        }
        icon_factory_request.destination.set_icon_fabrication_requested(true);

        icon_job = Actor_engine.run(browser.icon_factory_actor, icon_factory_request, null,logger);
    }


    //**********************************************************
    protected void cancel_icon()
    //**********************************************************
    {
        icon_fabrication_requested.set(false);
        if ( icon_job!= null)
        {
            Actor_engine.cancel_job(icon_job); // will trigger the aborter and if there is an associated thread, will interrupt it
            icon_job = null;
        }
    }


    //**********************************************************
    public void give_a_menu_to_the_button(Button local_button, Label local_label)
    //**********************************************************
    {
        ContextMenu context_menu = new ContextMenu();
        Look_and_feel_manager.set_context_menu_look(context_menu);
        if (Files.isDirectory(path))
        {
            context_menu.getItems().add(create_get_folder_size_menu_item());
            if ( is_trash())
            {
                MenuItem menu_item = create_clear_trash_menu_item();
                context_menu.getItems().add(menu_item);
            }
            if(!is_trash() && !is_parent())
            {
                context_menu.getItems().add(create_browse_in_new_window_menu_item());
                context_menu.getItems().add(create_open_with_system_menu_item(path,logger));
                if ( Static_application_properties.get_level3(logger)) context_menu.getItems().add(Item.create_edit_tag_menu_item(path, dbg, logger));
                context_menu.getItems().add(create_rename_menu_item(local_button,local_label));
                context_menu.getItems().add(create_delete_menu_item());
                context_menu.getItems().add(create_copy_dir_menu_item());
                context_menu.getItems().add(create_edit_color_menu_item(logger));
            }
        }
        else
        {
            if (Guess_file_type.is_this_path_an_image(path))
            {
                context_menu.getItems().add(create_open_exif_frame_menu_item(path,logger));
            }
            if (Guess_file_type.is_this_path_a_music(path))
            {
                context_menu.getItems().add(create_open_mediainfo_frame_menu_item(path,logger));
            }
            if ( this.item_type == Iconifiable_item_type.video)
            {
                Item_image.make_menu_items_for_videos(path,browser,context_menu,dbg, browser_aborter,logger);
            }

            // is a "plain" file
            context_menu.getItems().add(create_open_with_system_menu_item(path,logger));
            context_menu.getItems().add(create_open_with_special_app_item(path,logger));
            context_menu.getItems().add(create_rename_menu_item(local_button,local_label));
            context_menu.getItems().add(create_copy_menu_item());
            context_menu.getItems().add(create_delete_menu_item());

            context_menu.getItems().add(Item.create_show_file_size_menu_item(browser, path, dbg,logger));
            if ( Static_application_properties.get_level3(logger)) context_menu.getItems().add(Item.create_edit_tag_menu_item(path, dbg,logger));
        }


        local_button.setOnContextMenuRequested((ContextMenuEvent event) -> {
            if ( dbg) logger.log("show context menu of button:"+ path.toAbsolutePath());
            context_menu.show(local_button, event.getScreenX(), event.getScreenY());
        });
    }


    //**********************************************************
    public MenuItem create_open_exif_frame_menu_item(Path path, Logger logger)
    //**********************************************************
    {
        String txt = I18n.get_I18n_string("Info_about", logger);
        MenuItem menu_item = new MenuItem(txt);
        menu_item.setOnAction(actionEvent -> {
            if (dbg) logger.log("info");
            Image local_image = From_disk.load_native_resolution_image_from_disk(path, true, browser_aborter,logger);
            Exif_stage.show_exif_stage(local_image, path, browser_aborter, logger);
        });

        return menu_item;
    }

    //**********************************************************
    public MenuItem create_open_mediainfo_frame_menu_item(Path path, Logger logger)
    //**********************************************************
    {
        String txt = I18n.get_I18n_string("Info_about", logger);
        MenuItem menu_item = new MenuItem(txt);
        menu_item.setOnAction(actionEvent -> {
            if (dbg) logger.log("info");
            Audio_info_frame.show(path,logger);
        });

        return menu_item;
    }



    //**********************************************************
    private MenuItem create_browse_in_new_window_menu_item()
    //**********************************************************
    {
        MenuItem browse = new MenuItem("Browse in new window");
        browse.setOnAction(event -> {
            if (dbg) logger.log("Browse in new window!");
            Browser_creation_context.additional_different_folder(path,browser,logger);
        });
        return browse;
    }

    //**********************************************************
    private MenuItem create_get_folder_size_menu_item()
    //**********************************************************
    {
        MenuItem size = new MenuItem(I18n.get_I18n_string("Get_folder_size",logger));
        size.setOnAction(event -> Folder_size.get_folder_size(path,browser, logger));
        return size;
    }


    //**********************************************************
    private MenuItem create_clear_trash_menu_item()
    //**********************************************************
    {
        MenuItem menu_item = new MenuItem(I18n.get_I18n_string("Clear_Trash_Folder",logger));
        menu_item.setOnAction(event -> {
            if (dbg) logger.log("clearing trash!");
            Files_and_Paths.clear_trash_with_warning_fx(browser.my_Stage.the_Stage, browser_aborter,logger);
        });
        return menu_item;
    }


    //**********************************************************
    private MenuItem create_copy_menu_item()
    //**********************************************************
    {

        MenuItem menu_item = new MenuItem(I18n.get_I18n_string("Copy", logger));
        menu_item.setOnAction(event -> {
            if (dbg) logger.log("copying!");

            Path new_path = Files_and_Paths.ask_user_for_new_file_name(browser.my_Stage.the_Stage,path,logger);
            try
            {
                Files.copy(path, new_path);
            } catch (IOException e)
            {
                logger.log("copy failed: could not create new file for: " + path.getFileName() + ", Exception:" + e);
            }
        });
        return menu_item;
    }

    //**********************************************************
    private MenuItem create_delete_menu_item()
    //**********************************************************
    {
        MenuItem menu_item = new MenuItem(I18n.get_I18n_string("Delete", logger));
        menu_item.setOnAction(event -> {
            if (dbg) logger.log("Deleting!");
            Files_and_Paths.move_to_trash(browser.my_Stage.the_Stage,path, null, browser_aborter,logger);
        });
        return menu_item;
    }


    //**********************************************************
    private MenuItem create_rename_menu_item(Button local_button_, Label local_label_)
    //**********************************************************
    {
        final Button local_button = local_button_;
        final Label local_label = local_label_;
        MenuItem menu_item = new MenuItem(I18n.get_I18n_string("Rename", logger));
        menu_item.setOnAction(event -> {
            if (dbg) logger.log("Item_button: Renaming");
            String original_name = path.getFileName().toString();
            TextField text_edit = new TextField(original_name);
            Node restored = local_button.getGraphic();
            local_button.setGraphic(text_edit);
            text_edit.setMinWidth(local_button.getWidth() * 0.9);
            text_edit.requestFocus();
            text_edit.positionCaret(original_name.length());
            text_edit.setFocusTraversable(true);
            text_edit.setOnAction(actionEvent -> {
                String new_dir_name = text_edit.getText();
                actionEvent.consume();
                if ( path.toFile().isDirectory() )
                {
                    Path new_path = Files_and_Paths.change_dir_name(path, new_dir_name, browser_aborter, logger);
                    if ( new_path == null)
                    {
                        if (dbg) logger.log("rename failed");
                        local_button.setText(original_name);
                        local_button.setGraphic(restored);
                        return;
                    }
                    path = new_path;
                    local_button.setText(new_dir_name);
                    local_button.setGraphic(restored);
                }
                else
                {
                    Path new_path = Files_and_Paths.change_file_name(path, new_dir_name, browser_aborter, logger);
                    if ( new_path == null)
                    {
                        if (dbg) logger.log("rename failed");
                        local_button.setText(original_name);
                        local_button.setGraphic(restored);
                        return;
                    }
                    path = new_path;
                    if ( local_label == null)
                    {
                        // the item is a Item_folder_with_icon
                        if (dbg) logger.log("rename done");
                        local_button.setText(new_dir_name);
                        local_button.setGraphic(restored);
                    }
                    else
                    {
                        // the item is a Item_button

                        String size = Files_and_Paths.get_1_line_string_for_byte_data_size(path.toFile().length(),logger);
                        local_button.setText(size);
                        local_label.setText(new_dir_name);
                        //Font_size.set_preferred_font_size(label,logger);
                        Font_size.apply_font_size(local_label, logger);
                        local_button.setGraphic(local_label);
                    }
                }

                if (dbg) logger.log("rename done");
                // button.setOnAction(the_button_event_handler);
            });
        });
        return menu_item;
    }

    //**********************************************************
    private MenuItem create_copy_dir_menu_item()
    //**********************************************************
    {
        MenuItem menu_item = new MenuItem(I18n.get_I18n_string("Copy", logger));
        menu_item.setOnAction(event -> {
            if (dbg) logger.log("Copying the directory");
            Path new_path =  Files_and_Paths.ask_user_for_new_dir_name(browser.my_Stage.the_Stage,path,logger);
            if ( new_path == null)
            {
                Popups.popup_warning(browser.my_Stage.the_Stage,"copy of dir failed","names are same ?", false,logger);
                return;
            }
            Files_and_Paths.copy_dir_in_a_thread(browser.my_Stage.the_Stage, path, new_path, browser_aborter, logger);
        });
        return menu_item;
    }







    //**********************************************************
    public void unset_image_is_selected()
    //**********************************************************
    {
        set_is_unselected_internal();
    }

    //**********************************************************
    public void set_is_selected()
    //**********************************************************
    {
        if (browser.selection_handler.add_to_selected_files(path)) {
            set_is_selected_internal();
            logger.log("item selected:" + path);
        }
    }



    static double xxx = 200;
    static double yyy = 200;
    //**********************************************************
    public static MenuItem create_show_file_size_menu_item(Browser b_, Path path, boolean dbg, Logger logger)
    //**********************************************************
    {
        MenuItem menu_item = new MenuItem(I18n.get_I18n_string("Show_file_size", logger));
        menu_item.setOnAction(event -> {
            if (dbg) logger.log("File size");
            String size_in_bytes = Files_and_Paths.get_1_line_string_with_size(path,logger);
            String message = I18n.get_I18n_string("File_size_for", logger) +"\n"+ path.getFileName().toString();
            //Popups.popup_warning(message, file_size, false,logger);
            Stage local_stage = new Stage();
            local_stage.setHeight(200);
            local_stage.setWidth(600);
            local_stage.setX(xxx);
            local_stage.setY(yyy);
            yyy+= 200;
            if ( yyy > 600)
            {
                yyy = 200;
                xxx += 600;
                if ( xxx > 1000) xxx = 200;
            }
            TextArea textarea1 = new TextArea(message+"\n"+size_in_bytes);
            Font_size.apply_font_size(textarea1,24,logger);
            VBox vbox = new VBox(textarea1);
            Scene scene = new Scene(vbox, Color.WHITE);
            local_stage.setTitle(path.toAbsolutePath().toString());
            local_stage.setScene(scene);
            local_stage.show();

            logger.log("size_in_bytes->"+size_in_bytes+"<-");
            b_.set_status(size_in_bytes);
        });
        return menu_item;
    }

    //**********************************************************
    public static MenuItem create_edit_tag_menu_item(Path path, boolean dbg, Logger logger)
    //**********************************************************
    {
        MenuItem menu_item = new MenuItem(I18n.get_I18n_string("Show_tag", logger));
        menu_item.setOnAction(event -> {
            if (dbg) logger.log("File tag");
            Tag_stage.open_tag_stage(path,true,logger);
        });

        return menu_item;
    }

    //**********************************************************
    public  MenuItem create_edit_color_menu_item(Logger logger)
    //**********************************************************
    {
        String text = I18n.get_I18n_string("Color_Tag",logger);
        Menu menu = new Menu(text);
        List<My_color> possible_colors = new ArrayList<>();
        for (My_color candidate_color : My_colors.get_all_colors(logger))
        {
            possible_colors.add(candidate_color);
        }
        List<CheckMenuItem> all_check_menu_items = new ArrayList<>();
        for ( My_color color : possible_colors)
        {
            create_menu_item_for_one_color( menu, color, all_check_menu_items, logger);
        }
        return menu;
    }

    //**********************************************************
    public void create_menu_item_for_one_color(Menu menu, My_color target_color, List<CheckMenuItem> all_check_menu_items, Logger logger)
    //**********************************************************
    {
        String txt = target_color.localized_name();
        CheckMenuItem item = new CheckMenuItem(txt);
        item.setGraphic(new Circle(10, target_color.color()));
        if ( color == null)
        {
            if ( target_color.java_name() == null)
            {
                item.setSelected(true);
            }
        }
        else
        {
            item.setSelected(color.toString() == target_color.java_name());
        }
        //I18n.get_I18n_string("Font_size",logger) + " = " +target_size);

        item.setOnAction(actionEvent -> {
            CheckMenuItem local = (CheckMenuItem) actionEvent.getSource();
            if (local.isSelected())
            {
                for ( CheckMenuItem cmi : all_check_menu_items)
                {
                    if (cmi != local) cmi.setSelected(false);
                }
                String localized_name = local.getText();

                My_color my_color = My_colors.my_color_from_localized_name(localized_name,logger);
                //logger.log("is selected: ->"+localized_name+"<-");
                color = my_color.color();
                My_colors.save_color(path,my_color.java_name(),logger);
                if ( this instanceof Item_button)
                {
                    Item_button ib = (Item_button) this;
                    double font_size = Static_application_properties.get_font_size(logger);
                    double icon_height = Look_and_feel.MAGIC_HEIGHT_FACTOR * font_size;
                    Look_and_feel_manager.set_button_look_as_folder(ib.button, icon_height, color);
                }
            }
        });
        menu.getItems().add(item);
        all_check_menu_items.add(item);

    }


    //**********************************************************
    public  MenuItem create_open_with_system_menu_item(Path path, Logger logger)
    //**********************************************************
    {
        String text = I18n.get_I18n_string("Open_with_system",logger);
        MenuItem menu_item = new MenuItem(text);
        menu_item.setOnAction(actionEvent -> {
            if (dbg) logger.log("button in item: System Open");
            System_open_actor.open_with_system(browser,path,logger);
        });

        return menu_item;
    }




    //**********************************************************
    public  MenuItem create_open_with_special_app_item(Path path, Logger logger)
    //**********************************************************
    {
        String text = I18n.get_I18n_string("Open_With_Registered_Application",logger);
        MenuItem menu_item = new MenuItem(text);
        menu_item.setOnAction(actionEvent -> {
            if (dbg) logger.log("button in item: Open_With_Registered_Application");
            System_open_actor.open_special(browser.my_Stage.the_Stage,path,browser_aborter,logger);
        });

        return menu_item;
    }





    public Path get_item_path() {
        return path;
    }

    // path for display takes different form depending on the item type
    // it can be null, a PNG icon, or an animated gif
    abstract public Path get_path_for_display(boolean try_deep);




    //**********************************************************
    public void you_are_invisible()
    //**********************************************************
    {
        //logger.log("Invisible: "+path.getFileName());
        //get_Node().setVisible(false); OHOH
        you_are_invisible_specific();
        cancel_icon();

    }

    //**********************************************************
    public void you_are_visible()
    //**********************************************************
    {
        //logger.log("Visible: "+path.getFileName());
        //if( !Platform.isFxApplicationThread())  logger.log(Stack_trace_getter.get_stack_trace("PANIC not on Fx thread"));

        you_are_visible_specific();
        //get_Node().setVisible(true);
        if( has_icon())
            request_icon_to_factory(get_icon_size());
    }


    // this is called SUPER intensively when scrolling
    //**********************************************************
    public synchronized void process_is_visible(double current_vertical_offset)
    //**********************************************************
    {
        //if ( !Platform.isFxApplicationThread()) logger.log(Stack_trace_getter.get_stack_trace("PANIC"));
        //if ( get_javafx_y() == 0) logger.log("process_is_visible item "+get_item_path()+" x="+get_javafx_x()+" y="+get_javafx_y());


        set_translate_X(get_javafx_x());
        set_translate_Y(get_javafx_y() - current_vertical_offset);

        // this is essential: dont call you_are_visible() unless needed
        if (!visible_in_scene.get())
        {
            visible_in_scene.set(true);
            you_are_visible();
        }
    }

    //**********************************************************
    public void process_is_invisible(double current_vertical_offset)
    //**********************************************************
    {
        //if ( !Platform.isFxApplicationThread()) logger.log(Stack_trace_getter.get_stack_trace("PANIC"));

        set_translate_X(get_javafx_x());
        set_translate_Y(get_javafx_y() - current_vertical_offset);

        //if ( !Platform.isFxApplicationThread())logger.log(Stack_trace_getter.get_stack_trace("PANIC process_is_invisible "+Platform.isFxApplicationThread()));
        if (visible_in_scene.get())
        {
            visible_in_scene.set(false);
            you_are_invisible();
            //if (visible_dbg) logger.log("removing from Pane invisible Item: " + get_string());
            //pane.getChildren().remove(get_Node());
        }
    }


    abstract void you_are_visible_specific();
    abstract void you_are_invisible_specific();
    abstract int get_icon_size();
    abstract boolean has_icon();


}
