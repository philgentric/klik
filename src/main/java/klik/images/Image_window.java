package klik.images;

import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import klik.actor.Aborter;
import klik.browser.Browser;
import klik.change.Change_gang;
import klik.util.files_and_paths.*;
import klik.level3.fusk.Fusk_static_core;
import klik.level3.fusk.Fusk_strings;
import klik.look.Look_and_feel;
import klik.look.Look_and_feel_manager;
import klik.properties.Static_application_properties;
import klik.util.ui.Jfx_batch_injector;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import org.apache.commons.io.FilenameUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


//**********************************************************
public class Image_window
//**********************************************************
{
    public static final String IMAGE_WINDOW = "IMAGE_WINDOW";
    double progress;
    static boolean dbg = false;
    public final Scene the_Scene;
    public final Stage the_Stage;
    final Pane the_image_Pane;
    public final Logger logger;
    public final Image_display_handler image_display_handler;
    public final Mouse_handling_for_Image_window mouse_handling_for_image_window;
    public final Aborter aborter;

    private Slide_show slide_show; // not null if a Slide_show is ongoing
    boolean ultim_mode = false;
    boolean is_full_screen = false;
    Path dir;

    //**********************************************************
    public static Image_window get_Image_window(
            Browser b,
            Path path,
            Logger logger_)
    //**********************************************************
    {
        Stage from_stage = null;
        if ( b != null) from_stage = b.my_Stage.the_Stage; // for on same screen
        return on_same_screen(b, from_stage, path, logger_);
    }

    //**********************************************************
    private static Image_window on_same_screen(Browser b, Stage from_stage, Path path, Logger logger_)
    //**********************************************************
    {
        if (from_stage == null)
        {
            double x = 0;
            double y = 0;
            double w = 800;
            double h = 600;
            Rectangle2D bounds = Static_application_properties.get_window_bounds(IMAGE_WINDOW,logger_);
            logger_.log("got bounds from properties="+bounds);
            if (bounds != null)
            {
                x = bounds.getMinX();
                y = bounds.getMinY();
                w = bounds.getWidth();
                h = bounds.getHeight();
            }

            return new Image_window(b, path, x,y, w,h, logger_);
        }

        Rectangle2D bounds = Static_application_properties.get_window_bounds(IMAGE_WINDOW,logger_);
        double x = bounds.getMinX();
        double y = bounds.getMinY();
        double w = bounds.getWidth();
        double h = bounds.getHeight();

        Image_window returned = new Image_window(b, path, x, y,w, h, logger_);
        returned.the_Stage.setX(x);
        returned.the_Stage.setY(y);
        return returned;
    }


    //**********************************************************
    private Image_window(
            Browser the_browser,
            Path first_image_path,
            double x, double y,
            double w, double h,
            Logger logger_)
    //**********************************************************
    {
        logger = logger_;
        dir = first_image_path.getParent();
        aborter = new Aborter("Image_window",logger);
        the_Stage = new Stage();
        the_image_Pane = new StackPane();
        Look_and_feel_manager.set_region_look(the_image_Pane);

        String extension = FilenameUtils.getExtension(first_image_path.getFileName().toString());
        set_background(the_image_Pane,extension);
        the_Scene = new Scene(the_image_Pane);
        Color background = Look_and_feel_manager.get_instance().get_background_color();
        the_Scene.setFill(background);
        the_Stage.setScene(the_Scene);
        the_Stage.setX(x);
        the_Stage.setY(y);
        the_Stage.setWidth(w);
        the_Stage.setHeight(h);
        the_Stage.show();
        {
            Image_window local = this;
            the_Stage.addEventHandler(KeyEvent.KEY_PRESSED,
                    keyEvent -> Keyboard_handling_for_Image_window.handle_keyboard(the_browser,local, keyEvent, logger));
        }

        boolean high_quality = false;
        Optional<Image_display_handler> option = Image_display_handler.get_Image_display_handler_instance(high_quality, first_image_path, this, the_browser.get_file_comparator(), aborter, logger);
        if ( option.isEmpty())
        {
            image_display_handler = null;
            mouse_handling_for_image_window = null;
            set_nothing_to_display(first_image_path);
            return;
        }
        image_display_handler = option.get();
        mouse_handling_for_image_window = new Mouse_handling_for_Image_window(this, logger);

        //boolean white_background = mouse_handling_for_image_stage.something_is_wrong_with_image_size();

        image_display_handler.change_image_relative(0,false);


            //set_image(image_context_owner.get_image_context(), white_background);

        ChangeListener<Number> change_listener = (observableValue, number, t1) -> {
            if ( dbg) logger.log("ChangeListener: image window position and/or size changed: "+the_Stage.getWidth()+","+ the_Stage.getHeight());
            Static_application_properties.save_window_bounds(the_Stage,IMAGE_WINDOW,logger);
        };
        the_Stage.xProperty().addListener(change_listener);
        the_Stage.yProperty().addListener(change_listener);
        the_Stage.widthProperty().addListener(change_listener);
        the_Stage.heightProperty().addListener(change_listener);



        // this event handler is NOT called when close() is called
        // from the keyboard handler but only upon an "OS" window close
        the_Stage.setOnCloseRequest(we -> my_close());

        the_Scene.setOnScroll(event -> {
            double dy = -event.getDeltaY();
            if (dy == 0) return;
            //logger.log("SCROLL dy=" + dy);
            int yy = (int) (dy / 10.0);
            if (yy == 0)
            {
                if (dy < 0) yy = -1;
                else yy = 1;
            }
            //logger.log("SCROLL after round up=" + yy);
            image_display_handler.change_image_relative(yy, false);
        });



        // event handler if window is hidden (or closed, I hope?): stop animation
        the_Stage.setOnHiding(event -> {
            if (slide_show != null)
            {
                stop_slide_show();
            }
            //image_context_owner.get_image_context().finder_shutdown();

        });


        EventHandler<MouseEvent> mouse_clicked_event_handler = mouseEvent -> {
            if (mouseEvent.getButton() == MouseButton.SECONDARY)
            {
                image_display_handler.handle_mouse_clicked_secondary(the_browser, the_Stage, mouseEvent,logger);
            }
        };
        the_Stage.addEventHandler(MouseEvent.MOUSE_CLICKED, mouse_clicked_event_handler);



        mouse_handling_for_image_window.create_event_handlers(this, the_image_Pane);
        Browser.show_running_man = false;

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
    void set_background(Region target, String extension)
    //**********************************************************
    {
        BackgroundFill background_fill = get_Background_fill(extension);
        target.setBackground(new Background(background_fill));
    }

    //**********************************************************
    BackgroundFill get_Background_fill(String extension)
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
            Look_and_feel current_style = Look_and_feel.read_look_and_feel_from_properties_file(logger);
            background_fill = current_style.get_background_fill();
        }
        return background_fill;
    }

    //**********************************************************
    void show_wait_cursor()
    //**********************************************************
    {
        the_Stage.getScene().getRoot().setCursor(Cursor.WAIT);
        if ( dbg) logger.log("cursor = wait");
    }

    //**********************************************************
    void restore_cursor()
    //**********************************************************
    {
        the_Stage.getScene().getRoot().setCursor(Cursor.DEFAULT);
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

        Rectangle2D bounds = Static_application_properties.get_bounds(logger);

        if (bounds == null)
        {
            bounds = screen.getVisualBounds();
            Static_application_properties.save_bounds(bounds,logger);
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
            logger.log(Stack_trace_getter.get_stack_trace("PANIC ic==null"));
            return;
        }

        StringBuilder local_title = new StringBuilder();


        if (ic.path == null)
        {
            local_title.append(" no image ");
        }
        else
        {


            String extension = FilenameUtils.getExtension(ic.path.getFileName().toString());
            if ( extension.equalsIgnoreCase(Fusk_static_core.FUSK_EXTENSION))
            {
                String base = FilenameUtils.getBaseName(ic.path.toAbsolutePath().toString());
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
            int max_progress_bar = image_display_handler.image_indexer.get_max();
            if (max_progress_bar > budjet) max_progress_bar = budjet;
            int filler = budjet - max_progress_bar;
            for (int j = 0; j < filler; j++) local_title.append(" ");
            local_title.append("   ");
            int i = 0;
            for (; i < max_progress_bar * progress; i++) local_title.append("_");
            local_title.append("*");
            for (; i < max_progress_bar; i++) local_title.append("_");
        }
        the_Stage.setTitle(local_title.toString());
    }






    //**********************************************************
    void set_nothing_to_display(Path dir_)
    //**********************************************************
    {
        // no image to display...
        Jfx_batch_injector.inject(() -> {
            //the_BorderPane.getChildren().clear();
            the_image_Pane.getChildren().clear();//setCenter(null);
            if( dir_ != null) the_Stage.setTitle("No image to display in: " + dir_.toAbsolutePath());
            else the_Stage.setTitle("No image to display");
            restore_cursor();
        },logger);

    }


    //**********************************************************
    public void my_close()
    //**********************************************************
    {
        logger.log("Image_window is closing");
        aborter.abort("Image_window is closing");
        Browser.show_running_man = true;
        Change_gang.deregister(image_display_handler, aborter);
    }

    //**********************************************************
    void set_image(Image_context local_image_context)
    //**********************************************************
    {
        //logger.log(Stack_trace_getter.get_stack_trace("set_image: "+local_image_context.path));

        if ( local_image_context == null)
        {
            logger.log_stack_trace("WTF Image_context is null, should not happen");
            return;
        }
        // if pix-for-pix was used on a very large image, the window size is very large too..
        // let us check and correct that
        Jfx_batch_injector.inject(() -> {

            local_image_context.the_image_view.setPreserveRatio(true);
            local_image_context.the_image_view.setSmooth(true);
            double rot = local_image_context.get_rotation(aborter);

            // there is a bug with imageView rotate
            // see: https://stackoverflow.com/questions/53109791/fitting-rotated-imageview-into-application-window-scene
            // but the proposed solution does not work well
            // the trick that works however is to rotate a Pane containing the imageview !!!
            the_image_Pane.setRotate(rot);
            if (( rot == 90) || ( rot == 270))
            {
                local_image_context.the_image_view.fitWidthProperty().bind(the_image_Pane.heightProperty());
                local_image_context.the_image_view.fitHeightProperty().bind(the_image_Pane.widthProperty());
            }
            else {
                local_image_context.the_image_view.fitWidthProperty().bind(the_image_Pane.widthProperty());
                local_image_context.the_image_view.fitHeightProperty().bind(the_image_Pane.heightProperty());
            }
            set_background(the_image_Pane,FilenameUtils.getExtension(local_image_context.get_image_name()));

            the_image_Pane.getChildren().clear();
            the_image_Pane.getChildren().add(local_image_context.the_image_view); // <<<< this is what causes the image to be displayed
            set_stage_title(local_image_context);
        },logger);
    }

    //**********************************************************
    public Optional<Image_context> change_name_of_file(Path new_path)
    //**********************************************************
    {
        if ( image_display_handler.get_image_context().isEmpty()) return Optional.empty();
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
            Old_and_new_Path oandn = new Old_and_new_Path(old_path, new_path, Command_old_and_new_Path.command_rename, Status_old_and_new_Path.before_command,false);
            oandn.run_after = () -> Jfx_batch_injector.inject(() -> set_stage_title(local_new_image_context),logger);
            l.add(oandn);
            Moving_files.perform_safe_moves_in_a_thread(the_Stage,l, true, aborter,logger);
        }
        return Optional.of(local_new_image_context);
    }


    //**********************************************************
    public Path get_folder_path()
    //**********************************************************
    {
        return dir;
    }



}
