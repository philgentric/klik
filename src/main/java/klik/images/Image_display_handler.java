package klik.images;

import javafx.application.Platform;
import javafx.scene.control.ContextMenu;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.actor.Job_termination_reporter;
import klik.browser.Browser;
import klik.change.Change_gang;
import klik.change.Change_receiver;
import klik.experimental.Static_image_utilities;
import klik.files_and_paths.Files_and_Paths;
import klik.files_and_paths.Old_and_new_Path;
import klik.image_indexer.Image_indexer;
import klik.look.Look_and_feel_manager;
import klik.util.Logger;
import klik.util.Stack_trace_getter;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

//**********************************************************
public class Image_display_handler implements Change_receiver, Slide_show_slave
//**********************************************************
{
    private static final boolean dbg = false;

    public final Image_window image_stage;
    public final Image_indexer image_indexer;
    final Logger logger;
    public final Cache_interface image_cache;

    // STATE: when the image changes a new context is created
    Image_context image_context;

    // alternate rescaler:
    boolean alternate_rescaler = false;

    //**********************************************************
    public static Image_display_handler get_Image_display_handler_instance(boolean use_alternate_rescaler, Path path, Image_window v_, Aborter aborter, Logger logger_)
    //**********************************************************
    {
        Image_context image_context_ = build_Image_context(use_alternate_rescaler,path, aborter, logger_);
        if (image_context_ == null)
        {
            logger_.log(Stack_trace_getter.get_stack_trace("Image_stage PANIC: cannot load image " + path.toAbsolutePath()));
            return null;
        }
        return new Image_display_handler(image_context_,v_,logger_);
    }

    //**********************************************************
    static Image_context build_Image_context(boolean use_alternate_rescaler, Path path, Aborter aborter, Logger logger_)
    //**********************************************************
    {
        Image_context image_context_;
        if (use_alternate_rescaler)
        {
            System.out.println("high quality is ON");
            image_context_ = Static_image_utilities.get_Image_context_with_alternate_rescaler(path, 800, aborter, logger_);
        }
        else
        {
            image_context_ = Image_context.get_Image_context(path, aborter, logger_);
        }
        return image_context_;
    }

    //**********************************************************
    private Image_display_handler(Image_context image_context_, Image_window v_, Logger logger_)
    //**********************************************************
    {
        image_context = image_context_;
        logger = logger_;
        image_stage = v_;
        if ( dbg) logger.log("image_context.path.getParent()="+image_context.path.toAbsolutePath().getParent());
        image_indexer = Image_indexer.get_Image_indexer(image_context.path.toAbsolutePath().getParent(),logger);

        Change_gang.register(this,logger); // ic must be valid!

        image_cache = new Image_cache_cafeine(logger);
        {
            Image image = Look_and_feel_manager.get_default_icon(300);
            if (image != null) image_stage.the_Stage.getIcons().add(image);
        }
    }

    //**********************************************************
    public Image_context get_image_context()
    {
        return image_context;
    }
    //**********************************************************

    //**********************************************************
    Image_context local_getImage_context(Path path, Aborter aborter)
    //**********************************************************
    {
        Image_context iai;
        if (alternate_rescaler)
        {
            iai = Static_image_utilities.get_Image_context_with_alternate_rescaler(path, (int) image_stage.the_Stage.getWidth(),image_stage.aborter,logger);
        }
        else
        {
            iai = Image_context.get_Image_context(path,aborter, logger);
        }
        return iai;
    }



    //**********************************************************
    void get_next_u(Path get_from)
    //**********************************************************
    {
        change_image_relative(1, true);
    }



    //**********************************************************
    @Override //Change_receiver
    public String get_string()
    //**********************************************************
    {
        if (image_context == null)
        {
            return Stack_trace_getter.get_stack_trace("Image_stage NO CONTEXT????");
        }
        if (image_context.path == null)
        {
            return Stack_trace_getter.get_stack_trace("Image_stage NO PATH IN CONTEXT????");
        }

        return "Image_stage " + image_context.path.toAbsolutePath();
    }



    //**********************************************************
    void handle_mouse_clicked_secondary(Browser the_browser, Stage stage, BorderPane border_pane, MouseEvent e, Logger logger)
    //**********************************************************
    {
        logger.log("handle_mouse_clicked_secondary");

        ContextMenu contextMenu = Menu_for_image_stage.make_context_menu(the_browser, image_stage, this);
        contextMenu.show(border_pane, e.getScreenX(), e.getScreenY());
    }





    //**********************************************************
    @Override //Change_receiver
    public void you_receive_this_because_a_file_event_occurred_somewhere(List<Old_and_new_Path> l, Logger logger2)
    //**********************************************************
    {

        if ( dbg) logger2.log("Image_stage::you_receive_this_because_a_file_event_occurred_somewhere");
/*
        if (Change_gang.is_my_directory_impacted(image_context.path.getParent(), l, logger2) == false)
        {
            logger2.log("Image_stage::you_receive_this_because_a_move_occurred_somewhere NOT INTERESTED");
            return;
        }
        logger2.log("Image_stage::you_receive_this_because_a_move_occurred_somewhere YES");
*/
        //boolean found = false;
        for (Old_and_new_Path oanf : l)
        {
            if ( dbg) logger2.log("Image_stage, getting a you_receive_this_because_a_move_occurred_somewhere " + oanf.get_string());
            if (image_context == null)
            {
                logger2.log("Image_stage, ic == null");
                continue;
            }
            if (image_context.path == null)
            {
                logger2.log("Image_stage, ic.f == null");
                continue;
            }
            if ( Files_and_Paths.is_same_path(oanf.get_old_Path(),image_context.path,logger))
            {
                if ( dbg) logger.log(oanf.get_old_Path().toAbsolutePath()+ " OLD path corresponds to currently displayed image "+image_context.path.toAbsolutePath());
                // the case when the image has been dragged away is handled directly
                // by the setOnDragDone event handler

                // the case we care for HERE is when another type of event occurred
                // for example the image was renamed
                if (image_indexer.exists(oanf.new_Path))
                {
                    if ( dbg) logger.log("image RENAMED or MODIFIED (change in same dir):" + oanf.get_string());
                    Platform.runLater(() -> {
                        // clear the cache entry in case the file was MODIFIED
                        image_cache.evict(image_context.path);
                        Files_and_Paths.clear_one_icon_from_cache_on_disk(image_context.path,logger);
                        // reload the image
                        image_context =   local_getImage_context(image_context.path,  new Aborter());
                        image_stage.set_image(image_context,false);
                    });
                }
                else
                {
                    // the image was moved out of the current directory
                    if ( dbg) logger.log("image moved out:" + oanf.get_string());
                }

            }
            else
            {
                if ( dbg) logger.log(oanf.get_old_Path().toAbsolutePath()+ "OLD path DOES NOT corresponds to currently displayed image "+image_context.path.toAbsolutePath());
            }
        }


    }






    //**********************************************************
    void delete()
    //**********************************************************
    {
        Path to_be_deleted = image_context.path;


        change_image_relative(1,image_stage.ultim_mode);

        Runnable r = () -> image_indexer.signal_deleted_file(to_be_deleted);

        Files_and_Paths.move_to_trash(image_stage.the_Stage,to_be_deleted, r, new Aborter(),logger);
    }


    AtomicBoolean block = new AtomicBoolean(false);

    //**********************************************************
    @Override // Slide_show_slave
    public void change_image_relative(int delta, boolean ultimate)
    //**********************************************************
    {
        if ( block.get())
        {
            if ( dbg) logger.log("change_image_relative BLOCKED");
            return;
        }
        block.set(true);
        if ( dbg) logger.log("change_image_relative delta=" + delta);

        // first RESET the display mode
        if ( image_stage.mouse_handling_for_image_stage.mouse_mode != Mouse_mode.drag_and_drop)
        {
            image_stage.mouse_handling_for_image_stage.set_mouse_mode(image_stage, Mouse_mode.drag_and_drop);
        }

        Image_context[] returned_new_image_context = new Image_context[1];
        Change_image_message m = new Change_image_message(delta,image_context,image_stage,ultimate,returned_new_image_context,logger);
        // Job_termination_reporter will recover the NEW image_context

        Index_reporter index_reporter = index -> {
            image_stage.set_progress(image_stage.get_dir(), index);
            if ( dbg) logger.log("reporting index for: "+ image_context.path+" index="+index);
        };

        Job_termination_reporter tr =  (message, job) -> {
            block.set(false);
            image_context=returned_new_image_context[0];
            if ( image_context == null)
            {
                logger.log(Stack_trace_getter.get_stack_trace("Panic"));
                return;
            }
            if ( image_context.path == null)
            {
                logger.log(Stack_trace_getter.get_stack_trace("Panic"));
                return;
            }
            index_reporter.report_index(image_indexer.get_index(image_context.path));
        };
        Actor_engine.run(Change_image_actor.get_instance(), m, tr,logger);
        /*
        if ( dbg) logger.log("change_image_relative delta=" + delta);

        if (image_indexer == null)
        {
            if ( dbg) logger.log("change_image_relative image_file_source == null");
            if (image_context.path == null)
            {
                if ( dbg) logger.log("change_image_relative image_context.path == null");
                image_stage.set_nothing_to_display(null);
                return;
            }
        }

        // the safe way is to get the index of the current image from its path
        // however, when renaming a sequence of image this is annoying
        // since when you press next, you are in the new name context...

        image_stage.show_wait_cursor();
        Path target = image_indexer.get_new_path_relative(image_context.path,delta,ultimate);
        if ( target == null)
        {
            if ( dbg) logger.log("change_image_relative something really bad happened, like the whole dir was deleted behind the scene");
            image_stage.set_nothing_to_display(null);
            image_stage.restore_cursor();
            return;
        }
        if ( dbg) logger.log("change_image_relative target = "+target);
        String skey = Image_context.get_key(target);
        Image_context iai = image_cache.get(skey);
        boolean forward = true;
        if ( delta < 0) forward = false;
        if (iai != null)
        {
            image_context = iai;
            if ( dbg) logger.log("\n FOUND in CACHE: " + skey);
            image_stage.set_image(image_context,false);
            image_stage.restore_cursor();
            image_cache.preload(this, ultimate, forward, alternate_rescaler,(int) the_Stage.getWidth());
            return;
        }
        if ( dbg) logger.log("\n NOT found in cache: " + skey);
        iai = local_getImage_context(target);
        if (iai == null)
        {
            if ( dbg) logger.log("null image (1) in change_image_relative");
            Change_gang.report_anomaly(image_context.path.getParent());
            image_stage.restore_cursor();
            return;
        }

        image_context = iai;

        if (image_stage.mouse_handling_for_image_stage.something_is_wrong_with_image_size())
        {
            if ( dbg) logger.log("something_is_wrong_with_image_size in change_image_relative");
            //image_stage.restore_cursor();
            //return;
        }
        else
        {
            if ( dbg) logger.log("change_image_relative OK! index is:" + target + " for file:" + image_context.path.getFileName());
        }

        image_stage.set_image(image_context,false);
        image_stage.restore_cursor();

        image_cache.preload(this, ultimate, forward, alternate_rescaler,(int) the_Stage.getWidth());
        */
    }

    @Override // Slide_show_slave
    public void set_title()
    {
        image_stage.set_stage_title(image_context);
    }


    public void set_image_context(Image_context image_context_) {
        image_context = image_context_;
    }
}
