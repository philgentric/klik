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
import javafx.stage.Stage;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.actor.Job;
import klik.animated_gifs_from_videos.Ffmpeg_utils;
import klik.browser.Browser;
import klik.browser.Image_and_rotation;
import klik.browser.System_open_actor;
import klik.browser.icons.Icon_destination;
import klik.browser.icons.Icon_factory_actor;
import klik.browser.icons.Icon_factory_request;
import klik.browser.icons.Icon_status;
import klik.change.Change_gang;
import klik.files_and_paths.*;
import klik.images.Image_window;
import klik.images.decoding.Fast_rotation_from_exif_metadata_extractor;
import klik.level2.experimental.Multiple_image_window;
import klik.look.Look_and_feel_manager;
import klik.look.my_i18n.I18n;
import klik.util.Logger;
import klik.util.Stack_trace_getter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


//**********************************************************
public class Item_image extends Item implements Icon_destination
//**********************************************************
{
    protected ImageView image_view;
    Pane image_pane;
    private static final boolean visibility_dbg = false;
    private boolean rotation_known = false;
    private Job job;
    public double aspect_ratio = -1.0;

    //**********************************************************
    public Item_image(
            Browser b,
            Path p,
            double aspect_ratio, Logger logger)
    //**********************************************************
    {
        super(b,p, logger);
        this.aspect_ratio = aspect_ratio;
    }

    //**********************************************************
    public void load_default_icon()
    //**********************************************************
    {
        double actual_icon_size = icon_size / 3.0;
        Image i = Look_and_feel_manager.get_default_icon(actual_icon_size);
        if ( i == null)
        {
            logger.log("BADBADBAD: item_image: loading default icon failed");
            return;
        }
        if ( dbg) logger.log("item_image: loading default icon in the image view, w=" +i.getWidth()+", h="+i.getHeight()+" FOR:  "+path);
        if ( image_view == null) image_view = new ImageView();
        image_pane = new StackPane(image_view);

        image_view.setImage(i);
        image_view.setPreserveRatio(true);
        image_view.setSmooth(true);
        image_view.setFitWidth(actual_icon_size);
        image_view.setFitHeight(actual_icon_size);
        //the_image_view.setManaged(false);
        image_view.setCache(true);
        image_view.setCacheHint(CacheHint.SPEED);
        init_drag_and_drop();


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
            open_with_system(logger);
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


    @Override
    public Path get_path_for_display() {
        return path;
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
                Old_and_new_Path oandn = new Old_and_new_Path(path, new_path, Command_old_and_new_Path.command_rename, Status_old_and_new_Path.before_command);
                l.add(oandn);
                Moving_files.perform_safe_moves_in_a_thread(browser.my_Stage.the_Stage,l, new Aborter(),true,logger);
            });
            context_menu.getItems().add(menu_item);
        }
        {
            javafx.scene.control.MenuItem menu_item = new javafx.scene.control.MenuItem(I18n.get_I18n_string("Delete", logger));
            menu_item.setOnAction(event -> {
                if (dbg) logger.log("Deleting "+path);
                Files_and_Paths.move_to_trash(browser.my_Stage.the_Stage,path, null, new Aborter(), logger);
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
            context_menu.getItems().add(Item.create_show_file_size_menu_item(browser,path, dbg, logger));
            context_menu.getItems().add(Item.create_edit_tag_menu_item(path, dbg,logger));
        }

        if ( this.item_type == Iconifiable_item_type.video)
        {
            make_menu_items_for_videos(path,browser,context_menu,dbg,aborter,logger);
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

    @Override
    public void receive_icon(Image_and_rotation image_and_rotation)
    {
        rotation_known = true;
        rotation = image_and_rotation.rotation();
        set_Image(image_and_rotation.image(),true);
    }
    //**********************************************************
    @Override
    public void set_Image(Image image, boolean image_is_the_good_one)
    //**********************************************************
    {
        if ( image_view == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("the_image_view == null"));
            return;
        }

        if (!visible_in_scene.get())
        {
            //if ( dbg) g(0);
            image_view.setImage(null);
            icon_status = Icon_status.no_icon;
            return;
        }
        if ( (image.getHeight() == 0) && (image.getWidth() == 0))
        {
            logger.log(Stack_trace_getter.get_stack_trace("WARNING: empty image, not set "+path.toAbsolutePath()));
            return;
        }
        Platform.runLater(() -> do_it_in_fx_thread(image, image_is_the_good_one));
    }

    //**********************************************************
    @Override
    public void cancel_custom()
    //**********************************************************
    {
        Actor_engine.cancel_one(job);
        job = null;
        //logger.log("invisible icon => factory request canceled for "+path);
    }

    //**********************************************************
    public void request_icon_to_factory(Stage owner)
    //**********************************************************
    {

        Icon_factory_request ifr = new Icon_factory_request(this, icon_size);
        job = Icon_factory_actor.get_icon_factory(browser.aborter,browser.icon_manager.paths_manager.aspect_ratio_cache, owner, logger).make_icon(ifr);
        icon_status = Icon_status.true_icon_requested;
    }

    //**********************************************************
    public void do_it_in_fx_thread(Image image, boolean image_is_the_good_one)
    //**********************************************************
    {
        if ( image_view == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("the_image_view == null"));
            return;
        }
        if (( image.getHeight() == 0) && (image.getWidth() ==0))
        {
            logger.log(Stack_trace_getter.get_stack_trace("empty image"));
            return;
        }
        if ( dbg) logger.log("item_image: setting the icon in the image view, w=" +image.getWidth()+", h="+image.getHeight()+ " for: "+path);
        image_view.setImage(image);
        // does not work: the_image_view.setStyle("-fx-background-color: BLACK");
        if (image_is_the_good_one) {
            if (!rotation_known) {
                rotation_known = true;
                if (Files.exists(path)) {
                    if (
                            (Guess_file_type.is_this_path_a_video(path))
                                    || (Guess_file_type.is_this_path_a_pdf(path))
                    ) {
                        rotation = 0;
                    } else {
                        Double rotation_double = Fast_rotation_from_exif_metadata_extractor.get_rotation(path, aborter, logger);
                        if ( rotation_double != null)
                        {
                            rotation = rotation_double;
                            //logger.log(path+" rotation= "+ rotation_double);
                        }
                    }
                } else {
                    image_view.setImage(null);
                    icon_status = Icon_status.no_icon;
                    if (visibility_dbg) log_visibility_state_number(0);
                    return;
                }

            }

            // the above operation can take some time...
            // and in the mean time the situation can change
            if (!visible_in_scene.get()) {
                image_view.setImage(null);
                icon_status = Icon_status.no_icon;
                if (visibility_dbg) log_visibility_state_number(1);
                return;
            }


            image_pane.setPrefWidth(icon_size);
            image_pane.setPrefHeight(icon_size);
            image_pane.setMinWidth(icon_size);
            image_pane.setMinHeight(icon_size);


            image_view.setSmooth(true);

            if (( image.getHeight() >= icon_size) && (image.getWidth() >= icon_size))
            {
                image_view.setFitWidth(icon_size);
                image_view.setFitHeight(icon_size);
                if ((rotation == 90) || (rotation == 270))
                {
                    logger.log("NEVER HAPPENS");
                    image_view.setFitWidth(image.getHeight());
                    image_view.setFitHeight(image.getWidth());
                }
            }
            else
            {
                if ((rotation == 90) || (rotation == 270))
                {
                    if ( image.getHeight() < image.getWidth())
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
                    image_view.setFitWidth(image.getWidth());
                    image_view.setFitHeight(image.getHeight());
                }
            }

            rotate_and_center(image,image_pane);


            icon_status = Icon_status.true_icon;
            if (visibility_dbg) log_visibility_state_number(2);

        }
        else
        {
            icon_status = Icon_status.default_icon;
            if (visibility_dbg) log_visibility_state_number(3);
        }
    }



    private void log_visibility_state_number(int i)
    {
        get_logger().log(path+" visibility state #" + i);
    }


    @Override
    public Node get_Node()
    {
        return image_pane;
    }

    public ImageView get_image_view(){return image_view;}
    public Pane get_pane(){return image_pane;}



    @Override
    public String get_string()
    {
        return "is Item_image for : " + path.toAbsolutePath();
    }
}