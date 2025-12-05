// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.images;

import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.Window;
import klik.browser.comparators.Last_access_comparator;
import klik.properties.Sort_files_by;
import klik.util.execute.actor.Aborter;
import klik.images.caching.Image_cache_cafeine;
import klik.images.caching.Image_cache_interface;
import klik.images.caching.Image_cache_linkedhashmap;
import klik.path_lists.Path_list_provider_for_file_system;
import klik.browser.icons.image_properties_cache.Image_properties_RAM_cache;
import klik.browser.virtual_landscape.Browsing_caches;
import klik.browser.virtual_landscape.Path_comparator_source;
import klik.path_lists.Path_list_provider;
import klik.browser.virtual_landscape.Virtual_landscape;
import klik.change.Change_gang;
import klik.machine_learning.feature_vector.Feature_vector_cache;
import klik.look.my_i18n.My_I18n;
import klik.machine_learning.feature_vector.Feature_vector_source;
import klik.machine_learning.image_similarity.Feature_vector_source_for_image_similarity;
import klik.properties.Non_booleans_properties;
import klik.properties.boolean_features.Feature;
import klik.properties.boolean_features.Feature_cache;
import klik.util.Check_remaining_RAM;
import klik.util.files_and_paths.*;
import klik.experimental.fusk.Fusk_static_core;
import klik.experimental.fusk.Fusk_strings;
import klik.look.Look_and_feel;
import klik.look.Look_and_feel_manager;
import klik.util.files_and_paths.old_and_new.Command;
import klik.util.files_and_paths.old_and_new.Old_and_new_Path;
import klik.util.files_and_paths.old_and_new.Status;
import klik.util.image.rescaling.Image_rescaling_filter;
import klik.util.perf.Perf;
import klik.util.ui.Jfx_batch_injector;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import klik.util.ui.Popups;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;


//**********************************************************
public class Image_window
//**********************************************************
{
    private static final boolean use_linkedhashmap_for_cache = false;
    public static final String IMAGE_WINDOW = "IMAGE_WINDOW";
    double progress;
    static boolean dbg = false;
    public final Scene the_Scene;
    public final Stage stage;
    public final Pane the_image_Pane;
    public Label the_info_label; // maybe null
    public final Logger logger;
    public final Image_display_handler image_display_handler;
    public final Mouse_handling_for_Image_window mouse_handling_for_image_window;
    public final Aborter aborter;
    public String title_optional_addendum;

    // this is used to manage the closing using ESC
    // of multiple small Image_windows that have been poped up
    // by looking for similar images
    public final static List<Image_window> stage_group = new ArrayList<>();

    private Slide_show slide_show; // not null if a Slide_show is ongoing
    boolean ultim_mode = false;
    boolean is_full_screen = false;
    Path dir;
    private final Image_properties_RAM_cache image_properties_cache;
    private final Supplier<Feature_vector_cache> fv_cache_supplier;
    private Feature_vector_cache fv_cache;
    Path_list_provider path_list_provider;
    public Path_comparator_source path_comparator_source;

    public Image_rescaling_filter rescaler = Image_rescaling_filter.Native;
    public Image_cache_interface image_cache;

    //**********************************************************
    public static Image_window get_Image_window(Path path, Path_list_provider path_list_provider, Optional<Comparator<Path>> image_comparator,Window owner, Aborter aborter, Logger logger_)
    //**********************************************************
    {
        try ( Perf p = new Perf("get_Image_window")) {
            Last_access_comparator.set_last_access(path,logger_);
            Image_window returned = on_same_screen(path, path_list_provider, image_comparator, owner, aborter, logger_);

            return returned;
        }
    }

    //**********************************************************
    private static Image_window on_same_screen(Path path, Path_list_provider path_list_provider,Optional<Comparator<Path>> image_comparator,Window owner,Aborter aborter, Logger logger_)
    //**********************************************************
    {

        Rectangle2D bounds = Non_booleans_properties.get_window_bounds(IMAGE_WINDOW,owner);
        double x = bounds.getMinX();
        double y = bounds.getMinY();
        double w = bounds.getWidth();
        double h = bounds.getHeight();


        Image_window returned = new Image_window(path, null,x, y,w, h, null, true,path_list_provider,image_comparator,aborter,logger_);
        returned.stage.setX(x);
        returned.stage.setY(y);
        return returned;
    }


    //**********************************************************
    public Image_window(
            Path first_image_path,
            Window owner, // is null for 'big girl' windows
            double x, double y,
            double w, double h,
            String title_optional_addendum, // this is used to display image similarity
            boolean save_window_bounds,
            Path_list_provider path_list_provider,
            Optional<Comparator<Path>> image_comparator,
            Aborter aborter,
            Logger logger_)
    //**********************************************************
    {
        try (Perf p = new Perf("Image_window creation"))
        {
            this.aborter = aborter;
            this.path_list_provider = path_list_provider;
            this.title_optional_addendum = title_optional_addendum;
            path_comparator_source = () -> image_comparator.orElse(null);
            logger = logger_;
            dir = first_image_path.getParent();
            stage = new Stage();
            if (owner != null) {
                stage.initOwner(owner);
            }
            the_image_Pane = new StackPane();
            Look_and_feel_manager.set_region_look(the_image_Pane, owner, logger);

            path_list_provider.get_Change().add_change_listener(() -> rescan("Image_window constructor"));

            {
                long remaining_RAM = Check_remaining_RAM.get_remaining_memory(logger);
                int average_estimated_cache_slot_size = 50_000_000; // 50 MB per image, i.e. assume ~3000x~4000 pix on 4 byte
                int cache_slots = (int) (remaining_RAM / average_estimated_cache_slot_size);
                int forward_size = cache_slots / 2;
                if (forward_size > 10) forward_size = 10;
                //logger.log("cache_slots="+cache_slots);

                image_cache = Browsing_caches.image_caches.get(path_list_provider.get_folder_path().toAbsolutePath().toString());

                if ( image_cache == null)
                {
                    if (use_linkedhashmap_for_cache)
                    {
                        image_cache = new Image_cache_linkedhashmap(forward_size, aborter, logger);
                    } else {
                        image_cache = new Image_cache_cafeine(forward_size, aborter, logger);
                    }
                    Browsing_caches.image_caches.put(path_list_provider.get_folder_path().toAbsolutePath().toString(), image_cache);
                }
            }


            if (owner == null) {
                if (!Feature_cache.get(Feature.Hide_beginners_text_on_images)) {
                    String text = My_I18n.get_I18n_string("Image_window_info", owner, logger);
                    text = text.replaceAll(",", "\n");
                    the_info_label = new Label(text);
                    the_info_label.setMaxWidth(400);
                    the_info_label.setWrapText(true);
                    StackPane.setAlignment(the_info_label, Pos.BOTTOM_LEFT);
                }
            }

            Image_properties_RAM_cache tmp = Browsing_caches.image_properties_RAM_cache_of_caches.get(path_list_provider.get_folder_path().toAbsolutePath().toString());
            if (tmp == null) {
                tmp = Image_properties_RAM_cache.build(new Path_list_provider_for_file_system(first_image_path.getParent(),owner,logger), owner, logger);
                Browsing_caches.image_properties_RAM_cache_of_caches.put(path_list_provider.get_folder_path().toAbsolutePath().toString(), tmp);
            }
            image_properties_cache = tmp;


            fv_cache_supplier = () ->
            {
                if (fv_cache == null) {
                    Feature_vector_source fvs = new Feature_vector_source_for_image_similarity(stage,logger);
                    List<Path> paths = path_list_provider.only_image_paths(Feature_cache.get(Feature.Show_hidden_files));
                    Feature_vector_cache.Paths_and_feature_vectors images_and_feature_vectors =
                            Feature_vector_cache.preload_all_feature_vector_in_cache(fvs, paths, path_list_provider, stage, x, y, aborter, logger);
                    fv_cache = images_and_feature_vectors.fv_cache();
                }
                return fv_cache;
            };

            String extension = Extensions.get_extension(first_image_path.getFileName().toString());
            set_background(the_image_Pane, extension, owner);
            the_Scene = new Scene(the_image_Pane);
            //Look_and_feel_manager.set_scene_look(the_Scene,owner,logger);
            Color background = Look_and_feel_manager.get_instance(owner, logger).get_background_color();
            the_Scene.setFill(background);
            stage.setScene(the_Scene);
            stage.setX(x);
            stage.setY(y);
            stage.setWidth(w);
            stage.setHeight(h);
            stage.show();
            {
                Image_window local = this;
                stage.addEventHandler(KeyEvent.KEY_PRESSED,
                        keyEvent -> Keyboard_handling_for_Image_window.handle_keyboard(local, keyEvent, logger));
            }

            Comparator<Path> local_comp = null;
            if (image_comparator.isPresent()) {
                local_comp = image_comparator.get();
            } else {
                // this is going to take possibly a long time !!!
                long start = System.currentTimeMillis();
                local_comp = Sort_files_by.get_image_comparator(new Path_list_provider_for_file_system(first_image_path.getParent(),owner,logger), path_comparator_source, image_properties_cache, stage, x + 100, y + 100, aborter, logger);
                long now = System.currentTimeMillis();
                logger.log("get_image_comparator took " + (now - start) + " ms");
            }
            Optional<Image_display_handler> option = Image_display_handler.get_Image_display_handler_instance(path_list_provider, first_image_path, this, local_comp, aborter, owner, logger);
            if (option.isEmpty()) {
                image_display_handler = null;
                mouse_handling_for_image_window = null;
                set_nothing_to_display(first_image_path);
                return;
            }
            image_display_handler = option.get();
            image_display_handler.set_fv_cache(fv_cache_supplier);
            image_display_handler.set_image_properties_cache(image_properties_cache);


            mouse_handling_for_image_window = new Mouse_handling_for_Image_window(this, logger);

            image_display_handler.change_image_relative(0, false);


            ChangeListener<Number> change_listener = (observableValue, number, t1) -> {
                if (dbg)
                    logger.log("ChangeListener: image window position and/or size changed: " + stage.getWidth() + "," + stage.getHeight());
                if (save_window_bounds) Non_booleans_properties.save_window_bounds(stage, IMAGE_WINDOW, logger);
            };
            stage.xProperty().addListener(change_listener);
            stage.yProperty().addListener(change_listener);
            stage.widthProperty().addListener(change_listener);
            stage.heightProperty().addListener(change_listener);


            // this event handler is NOT called when close() is called
            // from the keyboard handler but only upon an "OS" window close
            stage.setOnCloseRequest(we -> my_close());

            the_Scene.setOnScroll(event -> {
                double dy = -event.getDeltaY();
                if (dy == 0) return;
                //logger.log("SCROLL dy=" + dy);
                int yy = (int) (dy / 10.0);
                if (yy == 0) {
                    if (dy < 0) yy = -1;
                    else yy = 1;
                }
                //logger.log("SCROLL after round up=" + yy);
                image_display_handler.change_image_relative(yy, false);

            });


            // event handler if window is hidden (or closed, I hope?): stop animation
            stage.setOnHiding(event -> {
                if (slide_show != null) {
                    stop_slide_show();
                }
                //image_context_owner.get_image_context().finder_shutdown();

            });

            mouse_handling_for_image_window.create_event_handlers(this, the_image_Pane);
            Virtual_landscape.show_progress_window_on_redraw = false;
        }
    }

    //**********************************************************
    private void rescan(String reason)
    //**********************************************************
    {
        image_display_handler.rescan(reason);
    }


    //**********************************************************
    void set_progress(Path dir, double p)
    //**********************************************************
    {
        progress = p;
    }


    /*
    slide show
     */

    //**********************************************************
    public boolean toggle_slideshow()
    //**********************************************************
    {
        if ( slide_show == null)
        {
            start_slide_show();
            Virtual_landscape.show_progress_window_on_redraw = false;
            return true;
        }
        else
        {
            stop_slide_show();
            return false;
        }
    }

    //**********************************************************
    public boolean is_slide_show_running()
    //**********************************************************
    {
        if ( slide_show == null) return false;
        return true;
    }

    //**********************************************************
    public void start_slide_show()
    //**********************************************************
    {
        slide_show = new Slide_show(image_display_handler, ultim_mode, logger);
    }
    //**********************************************************
    public void stop_slide_show()
    //**********************************************************
    {
        slide_show.stop_the_show();
        slide_show = null;
        image_display_handler.set_title();
    }

    //**********************************************************
    public void hurry_up()
    //**********************************************************
    {
        if ( slide_show!= null) slide_show.hurry_up();
    }

    //**********************************************************
    public void slow_down()
    //**********************************************************
    {
        if ( slide_show!= null) slide_show.slow_down();
    }







    //**********************************************************
    void set_background(Region target, String extension,Window owner)
    //**********************************************************
    {
        BackgroundFill background_fill = get_Background_fill(extension,owner);
        target.setBackground(new Background(background_fill));
    }

    //**********************************************************
    BackgroundFill get_Background_fill(String extension,Window owner)
    //**********************************************************
    {
        BackgroundFill background_fill = null;
        if ( extension.equalsIgnoreCase("png"))
        {
            background_fill = new BackgroundFill(Color.GREY, CornerRadii.EMPTY, Insets.EMPTY);

        }
        else if ( extension.equalsIgnoreCase("gif"))
        {
            background_fill = new BackgroundFill(Color.DARKGRAY, CornerRadii.EMPTY, Insets.EMPTY);
        }
        else
        {
            Look_and_feel laf = Look_and_feel_manager.get_instance(owner,logger);
            background_fill = laf.get_background_fill();
        }
        return background_fill;
    }


    //**********************************************************
    void show_wait_cursor()
    //**********************************************************
    {
        stage.getScene().getRoot().setCursor(Cursor.WAIT);
        if ( dbg) logger.log("cursor = wait");
    }

    //**********************************************************
    void restore_cursor()
    //**********************************************************
    {
        stage.getScene().getRoot().setCursor(Cursor.DEFAULT);
        if ( dbg) logger.log("cursor = default");
    }



/*
    //**********************************************************
    private void set_stage_size_to_fullscreen(Stage stage)
    //**********************************************************
    {
        Screen screen = null;
        if (stage.isShowing())
        {
            // we detect on which SCREEN the stage is (the user may have moved it)
            double minX = stage.getX();
            double minY = stage.getY();
            double width = stage.getWidth();
            double height = stage.getHeight();
            Rectangle2D r = new Rectangle2D(minX + 10, minY + 10, width - 100, height - 100);
            //logger.log("application rec"+r);
            ObservableList<Screen> screens = Screen.getScreensForRectangle(r);
            for (Screen s : screens)
            {
                //Rectangle2D bounds = s.getVisualBounds();
                //logger.log("screen in rec"+bounds);
                screen = s;
            }

        }
        else
        {
            // first time: we show the stage on the primary screen
            screen = Screen.getPrimary();
        }

        Rectangle2D bounds = Non_booleans_properties.get_bounds(logger);

        if (bounds == null)
        {
            bounds = screen.getVisualBounds();
            Non_booleans_properties.save_bounds(bounds,logger);
        }
        Scene scene = stage.getScene();
        //logger.log("scene getX" + scene.getX());
        //logger.log("scene getY" + scene.getY());
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());


    }
*/



    //**********************************************************
    void set_stage_title(Image_context ic)
    //**********************************************************
    {
        if (ic == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("❌ PANIC ic==null"));
            return;
        }

        StringBuilder local_title = new StringBuilder();

        if ( title_optional_addendum !=null) local_title.append(title_optional_addendum).append(" ");
        if (ic.path == null)
        {
            local_title.append(" no image ");
        }
        else
        {


            String extension = Extensions.get_extension(ic.path.getFileName().toString());
            if ( extension.equalsIgnoreCase(Fusk_static_core.FUSK_EXTENSION))
            {
                String base = Extensions.get_base_name(ic.path.toAbsolutePath().toString());
                local_title.append(Fusk_strings.defusk_string(base, logger)).append("*");
            }
            else
            {
                local_title.append(ic.path.getFileName().toString());
            }




            if (ic.path.toFile().length() == 0)
            {
                logger.log("\n\n empty file ???? ic.path = "+ic.path);
                local_title.append(" empty file:->").append(ic.path.toAbsolutePath().toString()).append("<-");
            }
            else if (ic.image_is_damaged)
            {
                local_title.append(" damaged or invalid (wrong extension?) ->").append(ic.path.toAbsolutePath().toString()).append("<- size = ").append(ic.path.toFile().length()).append(" Bytes");
            } else {
                local_title.append(" ").append(ic.image.getWidth()).append("x").append(ic.image.getHeight());
            }

            local_title.append(" ").append(ic.creation_time);
            local_title.append(" ").append(ic.title);
        }
        {
            local_title.append(rescaler.get_String());
        }
        if (slide_show != null)
        {
            local_title.append("-- SLIDE-SHOW mode, delay=").append(slide_show.inter_frame_ms).append("(ms)");
        }
        else
        {
            switch (mouse_handling_for_image_window.mouse_mode) {
                case drag_and_drop -> local_title.append("-- drag-and-drop mode (use mouse to drag the image)");
                case pix_for_pix -> local_title.append("-- pix-for-pix mode (use mouse to explore large images)");
                case click_to_zoom -> local_title.append("-- zoom-with-mouse mode (use mouse to select zoom area)");
            }
        }

        int budjet = 180;
        budjet -= local_title.toString().length();
        budjet -= 4;
        if ( budjet < 10) budjet = 10;
        {
            int max_progress_bar = budjet;
            if ( image_display_handler.image_indexer != null)
            {
                max_progress_bar = image_display_handler.image_indexer.get_max();
                if (max_progress_bar > budjet) max_progress_bar = budjet;
            }
            int filler = budjet - max_progress_bar;
            for (int j = 0; j < filler; j++) local_title.append(" ");
            local_title.append("   ");
            int i = 0;
            for (; i < max_progress_bar * progress; i++) local_title.append("_");
            local_title.append("*");
            for (; i < max_progress_bar; i++) local_title.append("_");
        }
        stage.setTitle(local_title.toString());
    }






    //**********************************************************
    void set_nothing_to_display(Path dir_)
    //**********************************************************
    {
        // no image to display...
        Jfx_batch_injector.inject(() -> {
            //the_BorderPane.getChildren().clear();
            the_image_Pane.getChildren().clear();//setCenter(null);
            if( dir_ != null) stage.setTitle("No image to display in: " + dir_.toAbsolutePath());
            else stage.setTitle("No image to display");
            restore_cursor();
        },logger);

    }


    //**********************************************************
    public void my_close()
    //**********************************************************
    {
        //logger.log("Image_window is closing");
        aborter.abort("Image_window is closing");
        Virtual_landscape.show_progress_window_on_redraw = true;
        Change_gang.deregister(image_display_handler, aborter);
    }

    //**********************************************************
    void set_image_internal(Image_context local_image_context)
    //**********************************************************
    {
        try(Perf p = new Perf("set_image_internal"))
        {
            //logger.log(Stack_trace_getter.get_stack_trace("set_image: "+local_image_context.path));

            if (local_image_context == null) {
                logger.log_stack_trace("❌ FATAL: Image_context is null, should not happen");
                return;
            }
            if (local_image_context.image == null) {
                logger.log_stack_trace("❌ FATAL: Image_context.Image is null, should not happen");
                return;
            }
            // if pix-for-pix was used on a very large image, the window size is very large too..
            // let us check and correct that
            Jfx_batch_injector.inject(() -> {

                local_image_context.the_image_view.setPreserveRatio(true);
                //local_image_context.the_image_view.setSmooth(true);
                double rot = local_image_context.get_rotation(aborter);

                // there is a bug with imageView rotate
                // see: https://stackoverflow.com/questions/53109791/fitting-rotated-imageview-into-application-window-scene
                // but the proposed solution does not work well
                // the trick that works however is to rotate a Pane containing the imageview !!!
                the_image_Pane.setRotate(rot);

                boolean normal = true;
                if (Feature_cache.get(Feature.Dont_zoom_small_images)) {
                    Image image = local_image_context.image;
                    double pane_height = the_image_Pane.getHeight();
                    double pane_width = the_image_Pane.getWidth();
                    if ((image.getHeight() < pane_height) && (image.getWidth() < pane_width)) {
                        if (dbg)
                            logger.log("preventing resize since " + image.getHeight() + " < " + pane_height + " and " + image.getWidth() + " < " + pane_width);

                        local_image_context.the_image_view.fitWidthProperty().unbind();
                        local_image_context.the_image_view.fitHeightProperty().unbind();
                        local_image_context.the_image_view.setFitWidth(local_image_context.image.getWidth());
                        local_image_context.the_image_view.setFitHeight(local_image_context.image.getHeight());
                        normal = false;
                    } else {
                        if (dbg) logger.log("NOT preventing resize");
                    }
                }
                if (normal) {

                    if ((rot == 90) || (rot == 270)) {
                        local_image_context.the_image_view.fitWidthProperty().bind(the_image_Pane.heightProperty());
                        local_image_context.the_image_view.fitHeightProperty().bind(the_image_Pane.widthProperty());
                    } else {
                        local_image_context.the_image_view.fitWidthProperty().bind(the_image_Pane.widthProperty());
                        local_image_context.the_image_view.fitHeightProperty().bind(the_image_Pane.heightProperty());
                    }
                }
                set_background(the_image_Pane, Extensions.get_extension(local_image_context.get_image_name()), stage);

                the_image_Pane.getChildren().clear();
                the_image_Pane.getChildren().add(local_image_context.the_image_view); // <<<< this is what causes the image to be displayed
                if (the_info_label != null) the_image_Pane.getChildren().add(the_info_label);
                set_stage_title(local_image_context);
            }, logger);
        }
    }

    //**********************************************************
    public Optional<Image_context> change_name_of_file(Path new_path)
    //**********************************************************
    {
        if ( image_display_handler.get_image_context().isEmpty()) return Optional.empty();

        // check if there is a ALREADY a file with the new name
        if (new_path.toFile().exists())
        {
            logger.log("name change aborted: there is already a file with that name!");
            Popups.popup_warning("❗ Not done","You cannot use this name:"+new_path.getFileName()+", because there is already a file with that name in the folder",false, stage,logger);
            return Optional.empty();
        }

        // remember the true file name
        Path old_path = image_display_handler.get_image_context().get().path;

        // set the new context: keep the previous path so that multiple renames can be performed
        // and the indexer will find the right "unchanged" index
        Image_context local_new_image_context = new Image_context(new_path, old_path, image_display_handler.get_image_context().get().image, logger);
        logger.log("change_name_of_file local_new_image_context\n      previous="+local_new_image_context.previous_path+"\n      path="+local_new_image_context.previous_path);
        image_display_handler.set_image_context(local_new_image_context);

        // now do the actual renaming
        {
            List<Old_and_new_Path> l = new ArrayList<>();
            Old_and_new_Path oandn = new Old_and_new_Path(old_path, new_path, Command.command_rename, Status.before_command,false);
            oandn.run_after = () -> Jfx_batch_injector.inject(() -> set_stage_title(local_new_image_context),logger);
            l.add(oandn);

            double x = stage.getX()+100;
            double y = stage.getY()+100;
            Moving_files.perform_safe_moves_in_a_thread(l, true, x,y,stage,aborter,logger);
        }
        return Optional.of(local_new_image_context);
    }


    //**********************************************************
    public Path get_folder_path()
    //**********************************************************
    {
        return dir;
    }

    //**********************************************************
    void redisplay(boolean clear_cache)
    //**********************************************************
    {
        if ( clear_cache)
        {
            logger.log("clearing cache for: "+image_display_handler.get_image_context().get().path);
            image_cache.evict(image_display_handler.get_image_context().get().path, stage);
        }
        Optional<Image_context> option = Image_context.build_Image_context(image_display_handler.get_image_context().get().path, this, aborter, logger);
        if (option.isPresent())
        {
            image_display_handler.set_image_context(option.get());
            image_display_handler.change_image_relative(0,false);
            if ( rescaler != Image_rescaling_filter.Native)
            {
                logger.log("image has been re-displayed with alternate rescaler: "+rescaler.name()+" details= "+rescaler.get_String());
            }
            else logger.log("image has been re-displayed with default rescaling");
        }
        else
        {
            logger.log("WARNING: image has NOT been re-displayed???");
        }
    }


    //**********************************************************
    public void preload(boolean ultimate, boolean forward)
    //**********************************************************
    {
        image_cache.preload(image_display_handler,ultimate,forward);
    }


    //**********************************************************
    public void save_in_cache(String skey, Image_context iai)
    //**********************************************************
    {
        image_cache.put(skey,iai);
    }


    //**********************************************************
    public void evict_from_cache(Path path, Window owner)
    //**********************************************************
    {
        image_cache.evict(path, owner);
    }
}
