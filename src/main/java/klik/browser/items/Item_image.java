package klik.browser.items;

import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import klik.actor.Aborter;
import klik.browser.icons.animated_gifs.Ffmpeg_utils;
import klik.browser.Browser;
import klik.browser.Image_and_rotation;
import klik.properties.Static_application_properties;
import klik.util.execute.System_open_actor;
import klik.change.Change_gang;
import klik.files_and_paths.*;
import klik.images.Image_window;
import klik.images.decoding.Fast_rotation_from_exif_metadata_extractor;
import klik.level3.experimental.Multiple_image_window;
import klik.look.Look_and_feel_manager;
import klik.look.my_i18n.I18n;
import klik.util.Logger;
import klik.util.Stack_trace_getter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


//**********************************************************
public class Item_image extends Item
//**********************************************************
{
    protected ImageView image_view;
    Pane image_pane;
    private static final boolean visibility_dbg = false;
    public Double aspect_ratio;
    public static Image default_icon;
    double actual_icon_size;

    //**********************************************************
    public Item_image(
            Browser b,
            Path p,
            Double aspect_ratio, Logger logger)
    //**********************************************************
    {
        super(b,p, null, logger);
        this.aspect_ratio = aspect_ratio;
        actual_icon_size = icon_size / 3.0;
        if ( default_icon == null) default_icon = Look_and_feel_manager.get_default_icon(actual_icon_size);
    }

    //**********************************************************
    private void on_mouse_clicked(Logger logger)
    //**********************************************************
    {
        browser.selection_handler.reset_selection(); // will clear all selections

        if ( Guess_file_type.is_this_path_an_image(path))
        {
            open_an_image(logger);
        }
        else
        {
            System_open_actor.open_with_system(browser,path,logger);
            //open_with_system(logger);
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
    public void open_an_image(Logger logger)
    //**********************************************************
    {
        Image_window s = Image_window.get_Image_window(browser, path, logger);
        if ( dbg) logger.log("\n\nImage_stage opening for path:" + path.toString());

        if (s == null) // used to be possible, normally not anymore
        {
            // let us do a bit of checking about why this failed
            Change_gang.report_anomaly(path);
        }
    }


    @Override // Item
    public Path get_path_for_display(boolean try_deep) {
        return path;
    }

    @Override // Icon_destination
    public Path get_path_for_display_icon_destination() {
        return path;
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
    public ContextMenu define_a_menu_to_the_imageview()
    //**********************************************************
    {
        ContextMenu context_menu = new ContextMenu();
        Look_and_feel_manager.set_context_menu_look(context_menu);

        {
            MenuItem menu_item = new MenuItem(I18n.get_I18n_string("Rename", logger)+ " "+path.getFileName());
            menu_item.setOnAction(event -> {
                if (dbg) logger.log("Item_image: Renaming "+path);

                Path new_path =  Files_and_Paths.ask_user_for_new_file_name(browser.my_Stage.the_Stage,path,logger);
                if ( new_path == null) return;

                List<Old_and_new_Path> l = new ArrayList<>();
                Old_and_new_Path oandn = new Old_and_new_Path(path, new_path, Command_old_and_new_Path.command_rename, Status_old_and_new_Path.before_command,false);
                l.add(oandn);
                Moving_files.perform_safe_moves_in_a_thread(browser.my_Stage.the_Stage,l, true, browser_aborter, logger);
            });
            context_menu.getItems().add(menu_item);
        }
        {
            javafx.scene.control.MenuItem menu_item = new javafx.scene.control.MenuItem(I18n.get_I18n_string("Delete", logger));
            menu_item.setOnAction(event -> {
                if (dbg) logger.log("Deleting "+path);
                Files_and_Paths.move_to_trash(browser.my_Stage.the_Stage,path, null, browser_aborter, logger);
            });
            context_menu.getItems().add(menu_item);
        }
        {
            javafx.scene.control.MenuItem menu_item = new javafx.scene.control.MenuItem(I18n.get_I18n_string("Edit", logger));
            menu_item.setOnAction(event -> {
                if (dbg) logger.log("Editing "+path);
                System_open_actor.open_with_system(browser,path,logger);
            });
            context_menu.getItems().add(menu_item);
        }
        {
            javafx.scene.control.MenuItem menu_item = new javafx.scene.control.MenuItem(I18n.get_I18n_string("Open_With_Registered_Application", logger));
            menu_item.setOnAction(event -> {
                if (dbg) logger.log("Opening with registered app: "+path);
                System_open_actor.open_special(browser,path,logger);
            });
            context_menu.getItems().add(menu_item);
        }
        {
            context_menu.getItems().add(Item.create_show_file_size_menu_item(browser,path, dbg, logger));
            if (Static_application_properties.get_level3(logger)) context_menu.getItems().add(Item.create_edit_tag_menu_item(path, dbg,logger));
        }

        if ( this.item_type == Iconifiable_item_type.video)
        {
            make_menu_items_for_videos(path,browser,context_menu,dbg, browser_aborter,logger);
        }
        return context_menu;

    }

    //**********************************************************
    public static void make_menu_items_for_videos(Path path, Browser browser, ContextMenu context_menu, boolean dbg, Aborter aborter, Logger logger)
    //**********************************************************
    {
        {
            javafx.scene.control.MenuItem menu_item = new javafx.scene.control.MenuItem("Convert to mp4");
            menu_item.setOnAction(event -> {
                if (dbg) logger.log("convert to mp4");
                Ffmpeg_utils.video_to_mp4_in_a_thread(browser.my_Stage.the_Stage,path,aborter, logger);
            });
            context_menu.getItems().add(menu_item);
        }
        {
            javafx.scene.control.MenuItem menu_item = new javafx.scene.control.MenuItem("(experimental) generate as many 5s gif animation as 5s in the movie, in a new folder (may take a long time!)");
            menu_item.setOnAction(event -> {
                if (dbg) logger.log("Generating animated gifs !");
                Ffmpeg_utils.generate_many_gifs(browser.my_Stage.the_Stage,path,5,5,aborter,logger);
            });
            context_menu.getItems().add(menu_item);
        }
        {
            javafx.scene.control.MenuItem menu_item = new javafx.scene.control.MenuItem("(experimental) generate gif animations from a video, interactively");
            menu_item.setOnAction(event -> {
                if (dbg) logger.log("Generating animated gifs !");
                Ffmpeg_utils.interactive(path,logger);
            });
            context_menu.getItems().add(menu_item);
        }
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
    public void receive_icon(Image_and_rotation image_and_rotation)
    //**********************************************************
    {
        if ( image_view == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("the_image_view == null"));
            return;
        }

        if (!visible_in_scene.get())
        {
            // this happen if between the time the icon was request and now,
            // the item is not visible anymore typically because the user scrolled away
            if ( dbg) logger.log("!visible_in_scene.get() : setting the image to null in the Image_view");
            you_are_invisible();
            return;
        }
        if ( image_and_rotation == null)
        {
            if ( dbg) logger.log("image_and_rotation == null : setting the image to null in the Image_view");
            you_are_invisible();
            return;
        }
        if ( image_and_rotation.image() == null)
        {
            if ( dbg) logger.log("image_and_rotation == null : setting the image to null in the Image_view");
            you_are_invisible();
            return;
        }

        if ( (image_and_rotation.image().getHeight()  < 1) || (image_and_rotation.image().getWidth() < 1))
        {
            logger.log(Stack_trace_getter.get_stack_trace("WARNING: empty image, not set "+path.toAbsolutePath()));
            you_are_invisible();
            return;
        }
        if ( item_type == Iconifiable_item_type.pdf)
        {
            if (aspect_ratio == null)
            {
                double local = image_and_rotation.image().getWidth()/image_and_rotation.image().getHeight();
                if( dbg) logger.log(Stack_trace_getter.get_stack_trace("setting aspect ratio for PDF from icon: "+ local));
                aspect_ratio = local;
            }
        }

        if ( Browser.use_fx_injector)
        {
            //Item_image the_item_image = this;
            Runnable r = () -> do_it_in_fx_thread(image_and_rotation);
            browser.fx_injector.input.addFirst(r);
        }
        else
        {
            Platform.runLater(() -> do_it_in_fx_thread(image_and_rotation));
        }
    }

    //**********************************************************
    @Override
    public void cancel_custom()
    //**********************************************************
    {
        if (dbg) logger.log("cancel_custom for: " + get_string());
        image_view.setImage(null); // for GC
    }

    //**********************************************************
    @Override
    public boolean has_icon()
    //**********************************************************
    {
        return true;
    }


    //**********************************************************
    public void do_it_in_fx_thread(Image_and_rotation image_and_rotation)
    //**********************************************************
    {
        if ( dbg) logger.log("item_image: setting the icon in the image view, w=" +image_and_rotation.image().getWidth()+", h="+image_and_rotation.image().getHeight()+ " for: "+path);
        image_view.setImage(image_and_rotation.image());
        icon_available.set(true);

        {
            Double local_rot = image_and_rotation.rotation();
            if (local_rot == null)
            {
                if (Files.exists(path))
                {
                    if (
                            (Guess_file_type.is_this_path_a_video(path)) || (Guess_file_type.is_this_path_a_pdf(path))
                    )
                    {
                        if ( dbg) logger.log("PDF => rot=0");
                        local_rot = Double.valueOf(0);
                    }
                    else
                    {
                        local_rot = Fast_rotation_from_exif_metadata_extractor.get_rotation(path, true, browser_aborter, logger);
                    }
                }
                else
                {
                    you_are_invisible();
                     if (visibility_dbg) log_visibility_state_number(0);
                    return;
                }

            }

            // the above operation can take some time...
            // and in the mean time the situation can change
            if (!visible_in_scene.get())
            {
                you_are_invisible();
                if (visibility_dbg) log_visibility_state_number(1);
                return;
            }

            //image_pane.setPrefWidth(icon_size);
            //image_pane.setPrefHeight(icon_size);
            //image_pane.setMinWidth(icon_size);
            //image_pane.setMinHeight(icon_size);
            //image_pane.setBorder(Look_and_feel_manager.get_border());
            //image_pane.setStyle("-fx-border-insets: -1;");

            image_view.setSmooth(true);

            if (( image_and_rotation.image().getHeight() >= icon_size) && (image_and_rotation.image().getWidth() >= icon_size))
            {
                image_view.setFitWidth(icon_size);
                image_view.setFitHeight(icon_size);
                if ((local_rot == 90) || (local_rot == 270))
                {
                    logger.log("HAPPENS for: "+path);
                    image_view.setFitWidth(image_and_rotation.image().getHeight());
                    image_view.setFitHeight(image_and_rotation.image().getWidth());
                }
            }
            else
            {
                if ((local_rot == 90) || (local_rot == 270))
                {
                    if ( image_and_rotation.image().getHeight() < image_and_rotation.image().getWidth())
                    {
                        image_view.setFitWidth(icon_size);
                        image_view.setFitHeight(-1);
                    }
                    else
                    {
                        image_view.setFitWidth(-1);
                        image_view.setFitHeight(icon_size);
                    }
                }
                else
                {
                    image_view.setFitWidth(image_and_rotation.image().getWidth());
                    image_view.setFitHeight(image_and_rotation.image().getHeight());
                }
            }
            if ( image_and_rotation.rotation() != null) {
                image_pane.setRotate(image_and_rotation.rotation());
            }
            icon_available.set(true);
            if (visibility_dbg) log_visibility_state_number(2);
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
        if ( default_icon == null)
        {
            logger.log("BADBADBAD: item_image: loading default icon failed");
            return;
        }
        if ( dbg) logger.log("item_image: loading default icon in the image view, w=" +default_icon.getWidth()+", h="+default_icon.getHeight()+" FOR:  "+path);
        if ( image_view == null) image_view = new ImageView();
        if ( image_pane == null) image_pane = new StackPane(image_view);

        image_view.setImage(default_icon);
        image_view.setPreserveRatio(true);
        image_view.setSmooth(true);
        image_view.setFitWidth(actual_icon_size);
        image_view.setFitHeight(actual_icon_size);
        //the_image_view.setManaged(false);
        image_view.setCache(true);
        image_view.setCacheHint(CacheHint.SPEED);
        init_drag_and_drop_sender_side();

        image_view.setOnMouseClicked(event ->
        {
            if (event.getButton() == MouseButton.SECONDARY)
            {
                //logger.log("\n\nItem_image isSecondaryButtonDown");
                ContextMenu context_menu = define_a_menu_to_the_imageview();
                context_menu.show(image_view, event.getScreenX(), event.getScreenY());
                return;
            }
            if (event.isMetaDown())
            {
                Multiple_image_window s = Multiple_image_window.get_Multiple_image_window(browser.my_Stage.the_Stage, path, false, logger);
                if (s == null)
                {
                    // let us a bit of checking about why this failed
                    Change_gang.report_anomaly(path);
                }
                return;
            }
            if (event.isControlDown())
            {
                if ( dbg) logger.log("\n\nItem_image event=" + event + " CTRL is down");
                set_is_selected();
            }
            else
            {
                if ( dbg) logger.log("\n\nItem_image OnMouseClicked " + path);
                on_mouse_clicked(logger);
            }
        });
    }


    //**********************************************************
    private void log_visibility_state_number(int i)
    //**********************************************************
    {
        get_logger().log(path+" visibility state #" + i);
    }

    //**********************************************************
    @Override
    public Node get_Node()
    //**********************************************************
    {
        return image_pane;
    }

    //**********************************************************
    @Override
    public String get_string()
    //**********************************************************
    {
        return "is Item_image for : " + path.toAbsolutePath();
    }

    @Override
    public boolean get_is_high_priority(){
        switch(item_type) {
            case video, pdf:
                return false;
            default:
                return true;
        }
    }

}