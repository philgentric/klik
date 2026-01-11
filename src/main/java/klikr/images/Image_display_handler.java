// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ./caching/Image_cache_interface.java
//SOURCES ./caching/Image_cache_cafeine.java
//SOURCES ./caching/Image_cache_linkedhashmap.java
//SOURCES ../experimental/work_in_progress/Static_image_utilities.java

package klikr.images;

import javafx.application.Platform;
import javafx.scene.control.ContextMenu;
import javafx.stage.Window;
import klikr.browser.icons.image_properties_cache.Image_properties;
import klikr.browser.virtual_landscape.Path_comparator_source;
import klikr.util.cache.RAM_cache;
import klikr.util.execute.actor.*;
import klikr.path_lists.Path_list_provider;
import klikr.browser.virtual_landscape.Virtual_landscape;
import klikr.change.Change_gang;
import klikr.change.Change_receiver;
import klikr.machine_learning.feature_vector.Feature_vector_cache;
import klikr.path_lists.Type;
import klikr.util.files_and_paths.Static_files_and_paths_utilities;
import klikr.util.files_and_paths.old_and_new.Old_and_new_Path;
import klikr.path_lists.Indexer;
import klikr.util.perf.Perf;
import klikr.util.ui.Jfx_batch_injector;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

//**********************************************************
public class Image_display_handler implements Change_receiver, Slide_show_slave
//**********************************************************
{
    private static final boolean dbg = false;

    public final Image_window image_window; // 'parent'
    public final Logger logger;


    //public final Image_cache_interface image_cache;

    // STATE:
    public Indexer image_indexer; // can be null at the beginning
    private Image_context image_context; // can CHANGE when image is edited or renamed

    public final Aborter aborter;


    //**********************************************************
    public static Optional<Image_display_handler> get_Image_display_handler_instance(
            Path_list_provider path_list_provider,
            Path path,
            Image_window image_window,
            Path_comparator_source path_comparator_source,
            Aborter aborter, Window owner, Logger logger_)
    //**********************************************************
    {
        Optional<Image_context> image_context_ = Image_context.build_Image_context(path,image_window,aborter, logger_);
        if (image_context_.isEmpty())
        {
            logger_.log("WARNING: cannot load image " + path.toAbsolutePath());

            return Optional.empty();
        }

        Optional<Image_display_handler> returned = Optional.of(new Image_display_handler(path_list_provider, image_context_.get(), image_window, path_comparator_source,aborter, logger_));
        return returned;
    }


    //**********************************************************
    private Image_display_handler(
            Path_list_provider path_list_provider,
            Image_context image_context_,
            Image_window v_,
            Path_comparator_source path_comparator_source,
            Aborter aborter, Logger logger_)
    //**********************************************************
    {
        this.aborter = aborter;
        image_context = image_context_;
        logger = logger_;
        image_window = v_;
        if ( dbg) logger.log("image_context.path.getParent()="+image_context_.path.toAbsolutePath().getParent());
        {
            // get the indexer in the background
            image_indexer = null;
            Runnable r = () -> image_indexer = Optional.of(Indexer.get_Image_indexer(Type.images, path_list_provider, path_comparator_source, aborter, logger)).orElse(null);
            Actor_engine.execute(r, "Get image indexer", logger);
        }

        Change_gang.register(this,aborter,logger); // image_context must be valid!




    }

    //**********************************************************
    public Optional<Image_context> get_image_context()
    //**********************************************************
    {
        if ( image_context == null)
        {
            return Optional.empty();
        }
        return Optional.of(image_context);
    }




    //**********************************************************
    void get_next_u(Path get_from)
    //**********************************************************
    {
        change_image_relative(1, true);
    }



    //**********************************************************
    @Override //Change_receiver
    public String get_Change_receiver_string()
    //**********************************************************
    {
        if (image_context == null)
        {
            return Stack_trace_getter.get_stack_trace("should not happen: image_context == null");
        }
        if (image_context.path == null)
        {
            return Stack_trace_getter.get_stack_trace("should not happen: image_context.path == null");
        }

        return "Image_display_handler " + image_context.path.toAbsolutePath();
    }



    //**********************************************************
    @Override //Change_receiver
    public void you_receive_this_because_a_file_event_occurred_somewhere(List<Old_and_new_Path> l, Window owner,Logger logger2)
    //**********************************************************
    {

        if ( dbg) logger2.log("Image_display_handler: you_receive_this_because_a_file_event_occurred_somewhere");

        if ( image_context == null) return;
        //boolean found = false;
        for (Old_and_new_Path oanf : l)
        {
            if ( dbg) logger2.log("Image_display_handler, getting a you_receive_this_because_a_move_occurred_somewhere " + oanf.to_string());

            if (image_context.path == null)
            {
                logger2.log("Image_display_handler, image_context.paath == null");
                continue;
            }
            if ( Static_files_and_paths_utilities.is_same_path(oanf.old_Path,image_context.path,logger))
            {
                if ( dbg) logger.log(oanf.old_Path.toAbsolutePath()+ " OLD path corresponds to currently displayed image "+image_context.path.toAbsolutePath());
                // the case when the image has been dragged away is handled directly
                // by the setOnDragDone event handler

                // the case we care for here is when another type of event occurred
                // for example the image was renamed
                if (image_indexer.is_known(oanf.new_Path))
                {
                    if ( dbg) logger.log("image RENAMED or MODIFIED (change in same dir):" + oanf.to_string());
                    Jfx_batch_injector.inject(() -> {
                        // clear the cache entry in case the file was MODIFIED
                        image_window.evict_from_cache(image_context.path,owner);
                        Static_files_and_paths_utilities.clear_one_icon_from_cache_on_disk(image_context.path,image_window.stage,logger);
                        // reload the image
                        Optional<Image_context> option = Image_context.build_Image_context(image_context.path, image_window,aborter,logger);
                        if ( option.isPresent())
                        {
                            // this CHANGES image_context
                            image_context = option.get();
                            change_image_relative(0,false);
                        }
                        else
                        {
                            logger.log(Stack_trace_getter.get_stack_trace("RE-loading image failed "+image_context.path));
                        }
                    },logger);
                }
                else
                {
                    // the image was moved out of the current directory
                    if ( dbg) logger.log("image moved out:" + oanf.to_string());
                }

            }
            else
            {
                if ( dbg) logger.log(oanf.old_Path.toAbsolutePath()+ "OLD path DOES NOT corresponds to currently displayed image "+image_context.path.toAbsolutePath());
            }
        }


    }



    //**********************************************************
    void delete()
    //**********************************************************
    {
        if ( image_context == null) return;
        Path to_be_deleted = image_context.path;
        change_image_relative(1, image_window.ultim_mode);
        Runnable r = () -> image_indexer.signal_deleted_file(to_be_deleted);
        double x = image_window.stage.getX()+100;
        double y = image_window.stage.getY()+100;

        Static_files_and_paths_utilities.move_to_trash(to_be_deleted,image_window.stage,x,y, r, aborter,logger);
    }


    AtomicBoolean block = new AtomicBoolean(false);

    //**********************************************************
    @Override // Slide_show_slave
    public void change_image_relative(int delta, boolean ultimate)
    //**********************************************************
    {
        try(Perf perf = new Perf("A. change_image_relative"))
        {
            if ( block.get())
            {
                if ( dbg) logger.log("change_image_relative BLOCKED");
                return;
            }
            block.set(true);

            Virtual_landscape.show_progress_window_on_redraw = false;
            if (image_context == null)
            {
                Path p = image_indexer.path_from_index(0);
                if (p == null) return;
                // image_context CHANGES here
                image_context = Image_context.build_Image_context(p, image_window, aborter, logger).orElse(null);
            }
            if (dbg) logger.log("change_image_relative delta=" + delta);

            // first RESET the display mode
            if (image_window.mouse_handling_for_image_window.mouse_mode != Mouse_mode.drag_and_drop) {
                image_window.mouse_handling_for_image_window.set_mouse_mode(image_window, Mouse_mode.drag_and_drop);
            }
            if (delta != 0) image_window.title_optional_addendum = null;

            if (image_context == null) return;

            Image_context[] returned_new_image_context = new Image_context[1];
            Change_image_message change_image_message = new Change_image_message(delta, image_context, image_window, ultimate, returned_new_image_context, aborter, logger);
            // Job_termination_reporter will recover the NEW image_context



            Job_termination_reporter tr = (message, job) ->
            {
                block.set(false);
                try(Perf perf2 = new Perf("B. change_image_relative, Job_termination_reporter")) {

                    image_context = returned_new_image_context[0];
                    if (image_context == null) {
                        // this  happens for example when requesting a "ultimate" image in a folder that does not contain any
                        //logger.log(("warning, image_context == null in termination reporter of change_image_relative"));
                        return;
                    }
                    if (image_context.path == null) {
                        logger.log(Stack_trace_getter.get_stack_trace("❌ Panic"));
                        image_context = null;
                        return;
                    }
                    last_steps_in_a_thread();

                }
            };
            Actor_engine.run(Change_image_actor.get_instance(), change_image_message, tr, logger);
        }
    }

    //**********************************************************
    private void last_steps_in_a_thread()
    //**********************************************************
    {
        Actor_engine.execute(()->last_steps_javafX(),"last_steps_in_a_thread",logger);
    }
    //**********************************************************
    private void last_steps_javafX()
    //**********************************************************
    {
        Platform.runLater(()->last_steps());
    }

    //**********************************************************
    private void last_steps()
    //**********************************************************
    {
        try( Perf perf3 = new Perf("C. set_context_menu"))
        {
            image_context.the_image_view.setOnContextMenuRequested(event ->
                    {
                        //logger.log("context menu for image");
                        ContextMenu contextMenu = Menus_for_image_window.make_context_menu(
                                image_window,
                                fv_cache_supplier,
                                logger);
                        contextMenu.show(image_window.stage, event.getScreenX(), event.getScreenY());

                    }
            );
        }
        try( Perf perf4 = new Perf("D. set_progress"))
        {

            if (image_indexer == null) {
                logger.log("❌ image_indexer not available yet ");
            } else {
                /*
                Index_reporter index_reporter = index -> {
                    image_window.set_progress(image_window.get_folder_path(), index);
                    if (dbg) logger.log("reporting index for: " + image_context.path + " index=" + index);
                };
                index_reporter.report_index((double) image_indexer.get_index(image_context.path) / (double) image_indexer.get_max());
                */
                double index = (double) image_indexer.get_index(image_context.path) / (double) image_indexer.get_max();
                image_window.set_progress(image_window.get_folder_path(), index);

            }
        }
    }

    @Override // Slide_show_slave
    public void set_title()
    {
        if ( image_context == null) return;
        image_window.set_stage_title(image_context);
    }

    public void set_image_context(Image_context image_context_) {
        image_context = image_context_;
    }



    Supplier<Feature_vector_cache> fv_cache_supplier;

    public void set_fv_cache(Supplier<Feature_vector_cache> fvCacheSupplier)
    {
        fv_cache_supplier = fvCacheSupplier;
    }

    RAM_cache<Path, Image_properties> image_properties_cache;
    public void set_image_properties_cache(RAM_cache<Path, Image_properties> imagePropertiesCache) {
        image_properties_cache = imagePropertiesCache;
    }

    public void rescan(String reason) {
        if ( image_indexer != null)
        {
            image_indexer.rescan(reason);
        }
    }


/*

    //**********************************************************
    void handle_mouse_clicked_secondary(
            Image_properties_cache image_properties_cache,
            Supplier<Feature_vector_cache> fv_cache_supplier,
            Window window, MouseEvent e, Logger logger)
    //**********************************************************
    {
        ContextMenu contextMenu = Menus_for_image_window.make_context_menu(image_window, image_properties_cache, fv_cache_supplier,logger);

        contextMenu.show(window, e.getScreenX(), e.getScreenY());
    }
*/



}
