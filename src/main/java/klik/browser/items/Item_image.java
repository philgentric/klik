package klik.browser.items;

import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.actor.Job;
import klik.animated_gifs_from_videos.Animated_gif_generator;
import klik.browser.Browser;
import klik.browser.System_open_actor;
import klik.browser.System_open_message;
import klik.browser.icons.Icon_destination;
import klik.browser.icons.Icon_factory_actor;
import klik.browser.icons.Icon_factory_request;
import klik.browser.icons.Icon_status;
import klik.change.Change_gang;
import klik.level2.experimental.Multiple_image_stage;
import klik.files_and_paths.*;
import klik.images.Image_window;
import klik.images.decoding.Fast_rotation_from_exif_metadata_extractor;
import klik.look.Look_and_feel_manager;
import klik.look.my_i18n.I18n;
import klik.util.Logger;
import klik.util.Popups;
import klik.util.Stack_trace_getter;

import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


//**********************************************************
public class Item_image extends Item implements Icon_destination
//**********************************************************
{
    protected ImageView the_image_view;
    private static final boolean dbg_visibility = false;
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
        if ( the_image_view == null) the_image_view = new ImageView();
        the_image_view.setImage(i);
        the_image_view.setPreserveRatio(true);
        the_image_view.setSmooth(true);
        //imageview.setCache(true);
        the_image_view.setFitWidth(actual_icon_size);
        the_image_view.setFitHeight(actual_icon_size);
        //the_image_view.setManaged(false);
        the_image_view.setCache(true);
        the_image_view.setCacheHint(CacheHint.SPEED);
        init_drag_and_drop();


        the_image_view.setOnMouseClicked(event ->
        {
            if (event.getButton() == MouseButton.SECONDARY)
            {
                //logger.log("\n\nItem_image isSecondaryButtonDown");
                ContextMenu context_menu = define_a_menu_to_the_imageview();
                context_menu.show(the_image_view, event.getScreenX(), event.getScreenY());
                return;
            }
            if (event.isMetaDown())
            {
                Multiple_image_stage s = Multiple_image_stage.get_Multiple_image_stage(browser.my_Stage.the_Stage, path, false, logger);
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
            open_with_system(logger);
        }
    }

    //**********************************************************
    @Override // Item
    public void set_is_unselected_internal()
    {
        if ( the_image_view != null) the_image_view.setViewport(null);
    }
    //**********************************************************
    @Override // Item
    public void set_is_selected_internal()
    //**********************************************************
    {
       double DELTA = icon_size/2.0;
       Rectangle2D r = new Rectangle2D(-DELTA, -DELTA, the_image_view.getFitWidth() + DELTA, the_image_view.getFitHeight() + DELTA);
       the_image_view.setViewport(r);
    }

    //**********************************************************
    public void open_an_image(Logger logger)
    //**********************************************************
    {
        Image_window s = Image_window.get_Image_stage(browser, path, logger);
        if ( dbg) logger.log("\n\nImage_stage opening for path:" + path.toString());

        if (s == null) // used to be possible, normally not anymore
        {
            // let us do a bit of checking about why this failed
            Change_gang.report_anomaly(path);
        }
    }

    //**********************************************************
    public void open_with_system(Logger logger)
    //**********************************************************
    {
        logger.log("opening with system (using java Desktop class) for path:" + path.toString());
        // try to open it with the system
        try
        {
            Desktop.getDesktop().open(path.toAbsolutePath().toFile());
        }
        catch (Exception e)
        {
            logger.log(Stack_trace_getter.get_stack_trace("open failed :" + e));

            if (e.toString().contains("doesn't exist."))
            {
                Popups.popup_warning(browser.my_Stage.the_Stage,"Failed?", "Your OS/GUI could not open this file, the error is:\n" + e, false, logger);
            }
            else
            {
                Popups.popup_warning(browser.my_Stage.the_Stage,"Failed?", "Your OS/GUI could not open this file, the error is:\n" + e + "\nMaybe it is just not properly configured e.g. most often the file extension has to be registered?", false, logger);
            }
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

        {
            javafx.scene.control.MenuItem menu_item = new javafx.scene.control.MenuItem(I18n.get_I18n_string("Rename", logger));
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
                Actor_engine.run(
                        System_open_actor.get(),
                        new System_open_message(browser.my_Stage.the_Stage,path,logger),null,logger);
            });
            context_menu.getItems().add(menu_item);
        }
        {
            context_menu.getItems().add(Item.create_show_file_size_menu_item(browser,path, dbg, logger));
            context_menu.getItems().add(Item.create_edit_tag_menu_item(path, dbg,logger));
        }

        if ( this.item_type == Iconifiable_item_type.video)
        {
            {
                javafx.scene.control.MenuItem menu_item = new javafx.scene.control.MenuItem("(experimental) generate an awfull lot of 5s gif animations!");
                menu_item.setOnAction(event -> {
                    if (dbg) logger.log("Generating animated gifs !");
                    Animated_gif_generator.generate_many_gifs(browser.my_Stage.the_Stage,path,5,5,logger);
                });
                context_menu.getItems().add(menu_item);
            }
            {
                javafx.scene.control.MenuItem menu_item = new javafx.scene.control.MenuItem("(experimental) generate an 1 gif animation interactively");
                menu_item.setOnAction(event -> {
                    if (dbg) logger.log("Generating animated gifs !");
                    Animated_gif_generator.interactive(path,logger);
                });
                context_menu.getItems().add(menu_item);
            }
        }
        return context_menu;

    }



    @Override
    public double get_Width()
    {
        return icon_size;
    }

    @Override
    public void set_MinHeight(double h)
    {
    } // not used, the height is defined by the IMAGE(icon!) in the imageview


    //**********************************************************
    @Override
    public double get_Height()
    //**********************************************************
    {
        return icon_size;
    }

    @Override
    public void receive_icon(Image icon) {
        set_Image(icon,true);
    }
    //**********************************************************
    @Override
    public void set_Image(Image i, boolean image_is_the_good_one)
    //**********************************************************
    {
        if ( the_image_view == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("the_image_view == null"));
            return;
        }

        if (!visible_in_scene)
        {
            //if ( dbg) g(0);
            the_image_view.setImage(null);
            icon_status = Icon_status.no_icon;
            return;
        }
        if ( (i.getHeight() == 0) && (i.getWidth() == 0))
        {
            logger.log(Stack_trace_getter.get_stack_trace("WARNING: empty image, not set "+path.toAbsolutePath()));
            return;
        }
        //aspect_ratio = i.getWidth()/i.getHeight();
        //logger.log("aspect ratio: "+aspect_ratio);
        Platform.runLater(() -> do_it_in_fx_thread(i, image_is_the_good_one));
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
        job = Icon_factory_actor.get_icon_factory(owner, logger).make_icon(ifr);
        icon_status = Icon_status.true_icon_requested;
    }

    //**********************************************************
    public void do_it_in_fx_thread(Image i, boolean image_is_the_good_one)
    //**********************************************************
    {
        if ( the_image_view == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("the_image_view == null"));
            return;
        }
        if (( i.getHeight() == 0) && (i.getWidth() ==0))
        {
            logger.log(Stack_trace_getter.get_stack_trace("empty image"));
            return;
        }
        if ( dbg) logger.log("item_image: setting the icon in the image view, w=" +i.getWidth()+", h="+i.getHeight()+ " for: "+path);
        the_image_view.setImage(i);
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
                        if ( rotation_double != null) rotation = rotation_double;
                    }
                } else {
                    the_image_view.setImage(null);
                    icon_status = Icon_status.no_icon;
                    if (dbg_visibility) log_visibility_state_number(0);
                    return;
                }

            }

            // the above operation can take some time...
            // and in the mean time the situation can change
            if (!visible_in_scene) {
                the_image_view.setImage(null);
                icon_status = Icon_status.no_icon;
                if (dbg_visibility) log_visibility_state_number(1);
                return;
            }

            rotate_and_center(i, the_image_view);

            the_image_view.setSmooth(true);

            if (( i.getHeight() >= icon_size) && (i.getWidth() >= icon_size))
            {
                //the_image_view.setManaged(false);
                the_image_view.setFitWidth(icon_size);
                the_image_view.setFitHeight(icon_size);
                if ((rotation == 90) || (rotation == 270)) {
                    the_image_view.setFitWidth(icon_size);
                    the_image_view.setFitHeight(-1);
                }
            }
            else {
                //the_image_view.setManaged(false);
                // does not work !
                //logger.log("very small image displayed :"+i.getWidth()+"x"+i.getHeight());
                if ((rotation == 90) || (rotation == 270)) {
                    the_image_view.setFitWidth(i.getHeight());
                    the_image_view.setFitHeight(i.getWidth());
                }
                else {
                    the_image_view.setFitWidth(i.getWidth());
                    the_image_view.setFitHeight(i.getHeight());
                }
            }

            icon_status = Icon_status.true_icon;
            if (dbg_visibility) log_visibility_state_number(2);

        }
        else
        {
            icon_status = Icon_status.default_icon;
            if (dbg_visibility) log_visibility_state_number(3);
        }
    }



    private void log_visibility_state_number(int i)
    {
        get_logger().log(path+" visibility state #" + i);
    }


    @Override
    public Node get_Node()
    {
        return the_image_view;
    }


    @Override
    public String get_string()
    {
        return "is Item_image for : " + path.toAbsolutePath();
    }
}