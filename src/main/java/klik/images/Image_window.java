package klik.images;

import javafx.application.Platform;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import klik.actor.Aborter;
import klik.browser.Browser;
import klik.change.Change_gang;
import klik.files_and_paths.*;
import klik.fusk.Fusk_static_core;
import klik.fusk.Fusk_strings;
import klik.properties.Static_application_properties;
import klik.util.Logger;
import klik.util.Stack_trace_getter;
import klik.util.Threads;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


//**********************************************************
public class Image_window
//**********************************************************
{

    static boolean dbg = false;

    public final Scene the_Scene;
    public final Stage the_Stage;
    public final BorderPane the_BorderPane;
    public final Logger logger;
    public final Image_display_handler image_display_handler;
    public final Mouse_handling_for_Image_stage mouse_handling_for_image_stage;
    public final Aborter aborter;
    MenuBar the_menu_bar;

    Slide_show slide_show; // not null if a Slide_show is ongoing
    boolean ultim_mode = false;
    boolean exit_on_escape = true;
    ProgressBar the_progress_bar;
    //Slider the_progress_bar;
    Path dir;

    //**********************************************************
    public static Image_window get_Image_stage(
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
            return new Image_window(b, path,
                    //smaller,
                    800, 600, logger_);//, tpe_);
        }
        // make sure the image opens on the same window as the caller
        ObservableList<Screen> intersecting_screens = Screen.getScreensForRectangle(from_stage.getX(), from_stage.getY(), from_stage.getWidth(), from_stage.getHeight());

        if (dbg)
        {
            ObservableList<Screen> screens = Screen.getScreens();

            for (int i = 0; i < screens.size(); i++)
            {
                Screen s = screens.get(i);
                logger_.log("screen#" + i);
                logger_.log("    getBounds" + s.getBounds());
                logger_.log("    getVisualBounds" + s.getVisualBounds());
            }


            for (Screen s : intersecting_screens)
            {
                logger_.log("intersecting screen:" + s);
                logger_.log("    getBounds" + s.getBounds());
                logger_.log("    getVisualBounds" + s.getVisualBounds());
            }
        }
        // often there is only one ...
        Screen current = intersecting_screens.get(0);

        double x = current.getVisualBounds().getMinX();
        double y = current.getVisualBounds().getMinY();
        double w = current.getBounds().getWidth();
        double h = current.getBounds().getHeight();
        Image_window returned = new Image_window(b, path, w, h, logger_);
        returned.the_Stage.setX(x);
        returned.the_Stage.setY(y);
        return returned;
    }


    //**********************************************************
    private Image_window(
            Browser the_browser,
            Path first_image_path,
            double w, double h,
            Logger logger_)
    //**********************************************************
    {
        dir = first_image_path.getParent();
        aborter = new Aborter();
        logger = logger_;
        the_Stage = new Stage();
        the_Stage.setWidth(w);
        the_Stage.setHeight(h);
        the_BorderPane = new BorderPane(); // makes it trivially easy to center the image!
        set_background(first_image_path.getFileName().toString(),true);
        the_Scene = new Scene(the_BorderPane);
        the_Stage.setScene(the_Scene);
        the_Stage.show();

        boolean high_quality = false;
        image_display_handler = Image_display_handler.get_Image_display_handler_instance(high_quality, first_image_path,this, the_browser.aborter, logger);
        if ( image_display_handler == null)
        {
            mouse_handling_for_image_stage = null;
            set_nothing_to_display(first_image_path);
            return;
        }



        the_menu_bar = Menu_for_image_stage.make_menu_bar(the_browser,this, image_display_handler);
        BorderPane top_border_pane = new  BorderPane();
        top_border_pane.setLeft(the_menu_bar);
        the_progress_bar = new ProgressBar();// new Slider(0,1,0); //
        //the_progress_bar.setOrientation(Orientation.HORIZONTAL);

        the_progress_bar.setPrefWidth(1000);
        top_border_pane.setCenter(the_progress_bar);

        the_BorderPane.setTop(top_border_pane);
        mouse_handling_for_image_stage = new Mouse_handling_for_Image_stage(this, logger);

        //boolean white_background = mouse_handling_for_image_stage.something_is_wrong_with_image_size();

        image_display_handler.change_image_relative(0,false);
        //set_image(image_context_owner.get_image_context(), white_background);

        ChangeListener<Number> change_listener = (observableValue, number, t1) -> {
            Rectangle2D b = new Rectangle2D(the_Stage.getX(), the_Stage.getY(), the_Stage.getWidth(), the_Stage.getHeight());
            Static_application_properties.save_bounds(b,logger);
        };
        the_Stage.widthProperty().addListener(change_listener);
        the_Stage.heightProperty().addListener(change_listener);


        //Image_stage image_stage = this;
        the_Stage.setOnCloseRequest(we -> {
            logger.log("Image_stage is closing");
            aborter.abort();
            Change_gang.deregister(image_display_handler);
        });

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

        {
            Image_window local = this;
            the_Stage.addEventHandler(KeyEvent.KEY_PRESSED,
                    keyEvent -> Keyboard_handling_for_Image_stage.handle_keyboard(the_browser,local, keyEvent, logger));
        }

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
                image_display_handler.handle_mouse_clicked_secondary(the_browser, the_Stage, the_BorderPane, mouseEvent,logger);
            }
        };
        the_Stage.addEventHandler(MouseEvent.MOUSE_CLICKED, mouse_clicked_event_handler);

        mouse_handling_for_image_stage.create_event_handlers(this);

    }

    //**********************************************************
    void set_progress(Path dir, double index)
    //**********************************************************
    {
        Path[] dir_static = new Path[1];
        dir_static[0] = dir;
        Runnable rr = () -> {
            if ( dir_static[0] == null)
            {
                dir_static[0] = image_display_handler.image_context.path.getParent();
            }
            long how_many_files = 0;

            File[] files = dir_static[0].toFile().listFiles();
            if ( files == null)
            {
                return;
            }
            Arrays.sort(files);

            for(File f : files)
            {
                if (Guess_file_type.is_this_path_an_image(f.toPath())) how_many_files++;
            }
            double progress = (index+1.0) /(double)how_many_files;
            if ( dbg) logger.log("progress = "+progress);

            Platform.runLater(() -> the_progress_bar.setProgress(progress));
        };
        Threads.execute(rr,logger);
    }


    //**********************************************************
    public void toggle_slideshow()
    //**********************************************************
    {
        if ( slide_show == null)
        {
            start_slide_show();
        }
        else
        {
            stop_slide_show();
        }
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
    void set_background(String image_name, boolean white)
    //**********************************************************
    {
        if (white)
        {
            the_BorderPane.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
            return;
        }
        if ((image_name.endsWith(".png")) || (image_name.endsWith(".PNG")))
        {
            the_BorderPane.setBackground(new Background(new BackgroundFill(Color.GREY, CornerRadii.EMPTY, Insets.EMPTY)));
        }
        else if ((image_name.endsWith(".gif")) || (image_name.endsWith(".GIF")))
        {
            the_BorderPane.setBackground(new Background(new BackgroundFill(Color.DARKGRAY, CornerRadii.EMPTY, Insets.EMPTY)));
        }
        else
        {
            the_BorderPane.setBackground(new Background(new BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY)));
        }

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

        String local_title;
        if (ic.path == null)
        {
            local_title = " no image ";
        }
        else
        {


            String extension = FilenameUtils.getExtension(ic.path.getFileName().toString());
            if ( extension.equalsIgnoreCase(Fusk_static_core.FUSK_EXTENSION))
            {
                String base = FilenameUtils.getBaseName(ic.path.toAbsolutePath().toString());
                local_title = Fusk_strings.defusk_string(base, logger)+ "*";
            }
            else
            {
                local_title = ic.path.getFileName().toString();
            }




            if (ic.path.toFile().length() == 0)
            {
                logger.log("\n\n empty file ???? ic.path = "+ic.path);
                local_title += " empty file";
            } else if (ic.image_is_damaged) {
                local_title += " damaged or invalid (wrong extension?) file, size = "+Files_and_Paths.get_size_on_disk(ic.path,null, logger)+" Bytes";
            } else {
                local_title += " " + ic.image.getWidth();
                local_title += "x" + ic.image.getHeight();
            }

            local_title += ic.title;
        }
        if (slide_show != null)
        {
            local_title += "-- SLIDE-SHOW mode, delay=" + slide_show.inter_frame_ms + "(ms)";
        }
        else
        {
            switch (mouse_handling_for_image_stage.mouse_mode) {
                case drag_and_drop -> local_title += "-- drag-and-drop mode (use mouse to drag the image)";
                case pix_for_pix -> local_title += "-- pix-for-pix mode (use mouse to explore large images)";
                case click_to_zoom -> local_title += "-- zoom-with-mouse mode (use mouse to select zoom area)";
            }
        }
        the_Stage.setTitle(local_title);
        //logger.log("Image_stage title = " + local_title);
    }






    //**********************************************************
    void set_nothing_to_display(Path dir_)
    //**********************************************************
    {
        // no image to display...
        Platform.runLater(() -> {
            //the_BorderPane.getChildren().clear();
            the_BorderPane.setCenter(null);
            if( dir_ != null) the_Stage.setTitle("No image to display in: " + dir_.toAbsolutePath());
            else the_Stage.setTitle("No image to display in");
            restore_cursor();
        });

    }



    //**********************************************************
    void set_image(Image_context local_image_context, boolean white_background)
    //**********************************************************
    {
        //logger.log("set_image: "+local_image_context.path);

        if ( local_image_context == null)
        {
            logger.log_stack_trace("WTF Image_context is null, should not happen");
            return;
        }
        // if pix-for-pix was used on a very large image, the window size is very large too..
        // let us check and correct that

        local_image_context.the_image_view.setPreserveRatio(true);
        //logger.log("smooth?"+local_image_context.imageView.isSmooth());
        local_image_context.the_image_view.setSmooth(true);
        //ic.imageView.setCache(true);
        double rot = local_image_context.get_rotation(aborter);
        local_image_context.the_image_view.setRotate(rot);
        //local_image_context.imageView.setRotate(0);

        ReadOnlyDoubleProperty h1 = the_Scene.heightProperty();
        ReadOnlyDoubleProperty h2 = the_menu_bar.heightProperty();
        DoubleBinding real_height_property = h1.subtract(h2);
        if (( rot == 90) || ( rot == 270))
        {
            // when the image is rotated imageview "width property" becomes ... the display height !!!
            // (and vice-versa)
            local_image_context.the_image_view.fitWidthProperty().bind(real_height_property);
            local_image_context.the_image_view.fitHeightProperty().bind(the_Scene.widthProperty());
        }
        else
        {
            // this will work properly only for rot = 0 and rot = 180
            // for exotic values, image corners will be truncated ...
            local_image_context.the_image_view.fitWidthProperty().bind(the_Scene.widthProperty());
            local_image_context.the_image_view.fitHeightProperty().bind(real_height_property);
        }

        set_background(local_image_context.get_image_name(),  white_background);

        boolean local_pix_for_pix =  false;
        /*
        if (Iconifiable_item_type.from_extension(local_image_context.path) == Iconifiable_item_type.image_gif)
        {
            // lots of gifs are small so blowing them up is bad fo quality
            // but SOME are large
            if ( the_Scene.getHeight() > local_image_context.imageView.getImage().getHeight())
            {
                if (the_Scene.getWidth() > local_image_context.imageView.getImage().getWidth())
                {
                    logger.log("image is gif setting mode pix for pix");
                    local_pix_for_pix = true;
                }
            }
        }*/
        if ( the_Scene.getHeight() > local_image_context.the_image_view.getImage().getHeight())
        {
            if (the_Scene.getWidth() > local_image_context.the_image_view.getImage().getWidth())
            {
                local_pix_for_pix = true;
            }
        }
        final boolean local_pix_for_pix2 =  local_pix_for_pix;

        Platform.runLater(() -> {
            //the_BorderPane.getChildren().clear();
            the_BorderPane.setCenter(local_image_context.the_image_view); // <<<< this is what causes the image to be displayed
            //logger.log("ic.imageView"+ local_image_context.imageView.getImage().toString());
            set_stage_title(local_image_context);
            if (mouse_handling_for_image_stage.mouse_mode == Mouse_mode.pix_for_pix || local_pix_for_pix2 ) mouse_handling_for_image_stage.pix_for_pix();
        });
    }

    //**********************************************************
    public Image_context change_name_of_file(Path new_path)
    //**********************************************************
    {
        // remember the true file name
        Path old_path = image_display_handler.get_image_context().path;

        // set the new context: keep the previous path so that multiple renames can be performed
        // and the indexer will find the right "unchanged" index
        Image_context local_new_image_context = new Image_context(new_path, image_display_handler.get_image_context().previous_path, image_display_handler.get_image_context().image, logger);
        logger.log("change_name_of_file local_new_image_context\n      previous="+local_new_image_context.previous_path+"\n      path="+local_new_image_context.previous_path);
        image_display_handler.set_image_context(local_new_image_context);

        // now do the actual renaming
        {
            List<Old_and_new_Path> l = new ArrayList<>();
            Old_and_new_Path oandn = new Old_and_new_Path(old_path, new_path, Command_old_and_new_Path.command_rename, Status_old_and_new_Path.before_command);
            oandn.run_after = () -> Platform.runLater(() -> set_stage_title(local_new_image_context));
            l.add(oandn);
            Moving_files.perform_safe_moves_in_a_thread(the_Stage,l, aborter,true, logger);
        }

        /*
        no need to report perform_safe_moves_in_a_thread will do it
        {
            List<Old_and_new_Path> l = new ArrayList<>();
            l.add(new Old_and_new_Path(old_path, new_path, Command_old_and_new_Path.command_rename, Status_old_and_new_Path.rename_done));
            Change_gang.report_changes(l);
        }
        */
        return local_new_image_context;
    }


    public Path get_dir() {
        return dir;
    }
}