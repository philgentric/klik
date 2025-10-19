//SOURCES ./caching/Image_cache_interface.java
//SOURCES ./caching/Image_cache_cafeine.java
//SOURCES ./caching/Image_cache_linkedhashmap.java
//SOURCES ../experimental/work_in_progress/Static_image_utilities.java

package klik.images;

import javafx.scene.control.ContextMenu;
import javafx.stage.Window;
import klik.actor.*;
import klik.browser.icons.image_properties_cache.Image_properties_RAM_cache;
import klik.browser.virtual_landscape.Path_list_provider;
import klik.browser.virtual_landscape.Virtual_landscape;
import klik.change.Change_gang;
import klik.change.Change_receiver;
import klik.machine_learning.feature_vector.Feature_vector_cache;
import klik.images.caching.Image_cache_linkedhashmap;
import klik.util.Check_remaining_RAM;
import klik.util.files_and_paths.Static_files_and_paths_utilities;
import klik.images.caching.Image_cache_interface;
import klik.images.caching.Image_cache_cafeine;
import klik.util.files_and_paths.old_and_new.Old_and_new_Path;
import klik.image_indexer.Image_indexer;
import klik.util.perf.Perf;
import klik.util.ui.Jfx_batch_injector;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;

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
    private static final boolean use_linkedhashmap = false;

    public final Image_window image_window;
    public final Logger logger;
    public final Image_cache_interface image_cache;

    // STATE: when the image changes a new context is created
    public Optional<Image_indexer> image_indexer;
    private Optional<Image_context> image_context;

    public final Aborter aborter;


    //**********************************************************
    public static Optional<Image_display_handler> get_Image_display_handler_instance(
            Path_list_provider path_list_provider,
            Path path,
            Image_window image_window,
            Comparator<? super Path> file_comparator,
            Aborter aborter, Window owner, Logger logger_)
    //**********************************************************
    {
        Optional<Image_context> image_context_ = Image_context.build_Image_context(path,image_window,aborter, logger_);
        if (image_context_.isEmpty())
        {
            logger_.log("WARNING: cannot load image " + path.toAbsolutePath());

            return Optional.empty();
        }

        Optional<Image_display_handler> returned = Optional.of(new Image_display_handler(path_list_provider, image_context_.get(), image_window, file_comparator,aborter, logger_));
        return returned;
    }


    //**********************************************************
    private Image_display_handler(Path_list_provider path_list_provider, Image_context image_context_, Image_window v_, Comparator<? super Path> file_comparator, Aborter aborter, Logger logger_)
    //**********************************************************
    {
        this.aborter = aborter;
        image_context = Optional.of(image_context_);
        logger = logger_;
        image_window = v_;
        if ( dbg) logger.log("image_context.path.getParent()="+image_context_.path.toAbsolutePath().getParent());
        image_indexer = Optional.empty();
        Runnable r = () -> image_indexer = Optional.of(Image_indexer.get_Image_indexer(path_list_provider,image_context_.path.toAbsolutePath().getParent(), file_comparator, aborter,logger));
        Actor_engine.execute(r,"Get image indexer",logger);

        Change_gang.register(this,aborter,logger); // image_context must be valid!


        long remaining_RAM = Check_remaining_RAM.get_remaining_memory(logger);
        int average_estimated_cache_slot_size = 50_000_000; // 50 MB per image, i.e. assume ~3000x~4000 pix on 4 byte
        int cache_slots = (int) (remaining_RAM/average_estimated_cache_slot_size);
        int forward_size = cache_slots/2;
        if ( forward_size > 10) forward_size = 10;
        //logger.log("cache_slots="+cache_slots);
        if (use_linkedhashmap)
        {
            image_cache = new Image_cache_linkedhashmap(forward_size,aborter,logger);
        }
        else
        {
            image_cache = new Image_cache_cafeine(forward_size,aborter,logger);
        }


    }

    //**********************************************************
    public Optional<Image_context> get_image_context()
    {
        return image_context;
    }
    //**********************************************************




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
        if (image_context.isEmpty())
        {
            return Stack_trace_getter.get_stack_trace("should not happen: image_context == null");
        }
        if (image_context.get().path == null)
        {
            return Stack_trace_getter.get_stack_trace("should not happen: image_context.path == null");
        }

        return "Image_display_handler " + image_context.get().path.toAbsolutePath();
    }



    //**********************************************************
    @Override //Change_receiver
    public void you_receive_this_because_a_file_event_occurred_somewhere(List<Old_and_new_Path> l, Window owner,Logger logger2)
    //**********************************************************
    {

        if ( dbg) logger2.log("Image_display_handler: you_receive_this_because_a_file_event_occurred_somewhere");

        if ( image_context.isEmpty()) return;
        Image_context local = image_context.get();
        //boolean found = false;
        for (Old_and_new_Path oanf : l)
        {
            if ( dbg) logger2.log("Image_display_handler, getting a you_receive_this_because_a_move_occurred_somewhere " + oanf.to_string());

            if (local.path == null)
            {
                logger2.log("Image_display_handler, ic.f == null");
                continue;
            }
            if ( Static_files_and_paths_utilities.is_same_path(oanf.old_Path,local.path,logger))
            {
                if ( dbg) logger.log(oanf.old_Path.toAbsolutePath()+ " OLD path corresponds to currently displayed image "+local.path.toAbsolutePath());
                // the case when the image has been dragged away is handled directly
                // by the setOnDragDone event handler

                // the case we care for here is when another type of event occurred
                // for example the image was renamed
                if (image_indexer.get().is_known(oanf.new_Path))
                {
                    if ( dbg) logger.log("image RENAMED or MODIFIED (change in same dir):" + oanf.to_string());
                    Jfx_batch_injector.inject(() -> {
                        // clear the cache entry in case the file was MODIFIED
                        image_cache.evict(local.path,owner);
                        Static_files_and_paths_utilities.clear_one_icon_from_cache_on_disk(local.path,image_window.stage,logger);
                        // reload the image
                        Optional<Image_context> option = Image_context.build_Image_context(local.path, image_window,aborter,logger);
                        if ( option.isPresent())
                        {
                            image_context = Optional.of(option.get());
                            change_image_relative(0,false);
                        }
                        else
                        {
                            logger.log(Stack_trace_getter.get_stack_trace("RE-loading image failed "+local.path));
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
                if ( dbg) logger.log(oanf.old_Path.toAbsolutePath()+ "OLD path DOES NOT corresponds to currently displayed image "+local.path.toAbsolutePath());
            }
        }


    }



    public void print_image_cache()
    {
        image_cache.print();
    }


    //**********************************************************
    void delete()
    //**********************************************************
    {
        if ( image_context.isEmpty()) return;
        Path to_be_deleted = image_context.get().path;
        change_image_relative(1, image_window.ultim_mode);
        Runnable r = () -> image_indexer.get().signal_deleted_file(to_be_deleted);
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
        if ( block.get())
        {
            if ( dbg) logger.log("change_image_relative BLOCKED");
            return;
        }
        block.set(true);
        try(Perf perf = new Perf("A. change_image_relative")) {
            Virtual_landscape.show_progress_window_on_redraw = false;
            if (image_context.isEmpty()) {
                Path p = image_indexer.get().path_from_index(0);
                if (p == null) return;
                image_context = Image_context.build_Image_context(p, image_window, aborter, logger);
            }
            if (dbg) logger.log("change_image_relative delta=" + delta);

            // first RESET the display mode
            if (image_window.mouse_handling_for_image_window.mouse_mode != Mouse_mode.drag_and_drop) {
                image_window.mouse_handling_for_image_window.set_mouse_mode(image_window, Mouse_mode.drag_and_drop);
            }
            if (delta != 0) image_window.title_optional_addendum = null;

            if (image_context.isEmpty()) return;

            Image_context[] returned_new_image_context = new Image_context[1];
            Change_image_message change_image_message = new Change_image_message(delta, image_context.get(), image_window, ultimate, returned_new_image_context, aborter, logger);
            // Job_termination_reporter will recover the NEW image_context

            Index_reporter index_reporter = index -> {
                image_window.set_progress(image_window.get_folder_path(), index);
                if (dbg) logger.log("reporting index for: " + image_context.get().path + " index=" + index);
            };

            Job_termination_reporter tr = (message, job) ->
            {
                block.set(false);
                try(Perf perf2 = new Perf("B. change_image_relative, Job_termination_reporter")) {

                    Image_context local = returned_new_image_context[0];
                    if (local == null) {
                        // this  happens for example when requesting a "ultimate" image in a folder that does not contain any
                        //logger.log(("warning, image_context == null in termination reporter of change_image_relative"));
                        image_context = Optional.empty();
                        return;
                    }
                    if (local.path == null) {
                        logger.log(Stack_trace_getter.get_stack_trace("Panic"));
                        image_context = Optional.empty();
                        return;
                    }
                    image_context = Optional.of(local);
                    image_context.get().the_image_view.setOnContextMenuRequested(event ->
                            {
                                logger.log("context menu for image");
                                ContextMenu contextMenu = Menus_for_image_window.make_context_menu(
                                        image_window,
                                        image_properties_cache,
                                        fv_cache_supplier,
                                        logger);
                                contextMenu.show(image_window.stage, event.getScreenX(), event.getScreenY());

                            }
                    );
                    image_indexer.ifPresent(imageIndexer -> index_reporter.report_index((double) imageIndexer.get_index(local.path) / (double) imageIndexer.get_max()));
                }
            };
            Actor_engine.run(Change_image_actor.get_instance(), change_image_message, tr, logger);
        }
    }

    @Override // Slide_show_slave
    public void set_title()
    {
        if ( image_context.isEmpty()) return;
        image_window.set_stage_title(image_context.get());
    }

    public void set_image_context(Image_context image_context_) {
        image_context = Optional.of(image_context_);
    }

    public void clear_all_image_cache() {
        image_cache.clear_all();
    }



    public void preload(boolean ultimate, boolean forward)
    {
        image_cache.preload(this,ultimate,forward);
    }


    public void save_in_cache(String skey, Image_context iai) {
        image_cache.put(skey,iai);
    }

    Supplier<Feature_vector_cache> fv_cache_supplier;

    public void set_fv_cache(Supplier<Feature_vector_cache> fvCacheSupplier)
    {
        fv_cache_supplier = fvCacheSupplier;
    }

    Image_properties_RAM_cache image_properties_cache;
    public void set_image_properties_cache(Image_properties_RAM_cache imagePropertiesCache) {
        image_properties_cache = imagePropertiesCache;
    }


/*

    //**********************************************************
    void handle_mouse_clicked_secondary(
            Image_properties_RAM_cache image_properties_cache,
            Supplier<Feature_vector_cache> fv_cache_supplier,
            Window window, MouseEvent e, Logger logger)
    //**********************************************************
    {
        ContextMenu contextMenu = Menus_for_image_window.make_context_menu(image_window, image_properties_cache, fv_cache_supplier,logger);

        contextMenu.show(window, e.getScreenX(), e.getScreenY());
    }
*/



}
