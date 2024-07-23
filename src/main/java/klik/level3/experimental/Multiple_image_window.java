//SOURCES ../../image_indexer/Image_indexer.java
package klik.level3.experimental;

import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.TilePane;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import klik.actor.Aborter;
import klik.browser.icons.Paths_manager;
import klik.image_indexer.Image_indexer;
import klik.images.Image_context;
import klik.look.Look_and_feel_manager;
import klik.util.Fx_batch_injector;
import klik.util.Logger;
import klik.util.Stack_trace_getter;

import java.nio.file.Path;
import java.util.Optional;


//**********************************************************
public class Multiple_image_window
//**********************************************************
{
    static boolean dbg = false;

    final Scene scene;
    final Stage the_stage;
    final TilePane tile_pane;

    private double old_mouse_x;
    private double old_mouse_y;
    public double W = 1200;
    public double H = 800;
    Logger logger;

    Image_indexer image_indexer = null;
    private Image_context ic;
    public final Aborter aborter;
    //**********************************************************
    public static Optional<Multiple_image_window> get_Multiple_image_window(
            Stage from_stage, // for on same screen
            Path path,
            boolean smaller,
            Logger logger_)
    //**********************************************************
    {
        Aborter aborter = new Aborter("Multiple_image_window",logger_);
        Optional<Image_context> option = Image_context.get_Image_context(path, aborter, logger_);
        if (option.isEmpty()) {
            logger_.log(Stack_trace_getter.get_stack_trace("Multiple_image_stage PANIC: cannot load image " + path.toAbsolutePath()));
            return Optional.empty();
        }
        logger_.log("Multiple_image_stage OK: image loaded" + path.toAbsolutePath());
        if (from_stage == null) {
            return Optional.of(new Multiple_image_window(option.get(), smaller, 800, 600, aborter, logger_));
        }
        // make sure the image opens on the same window as the caller
        ObservableList<Screen> intersecting_screens = Screen.getScreensForRectangle(from_stage.getX(), from_stage.getY(), from_stage.getWidth(), from_stage.getHeight());

        if (dbg) {
            ObservableList<Screen> screens = Screen.getScreens();

            for (int i = 0; i < screens.size(); i++) {
                Screen s = screens.get(i);
                logger_.log("screen#" + i);
                logger_.log("    getBounds" + s.getBounds());
                logger_.log("    getVisualBounds" + s.getVisualBounds());
            }


            for (Screen s : intersecting_screens) {
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
        if (smaller) {
            w *= 0.5;
            h *= 0.5;
            x += 100;
            y += 100;

        }
        Multiple_image_window returned = new Multiple_image_window(option.get(), smaller, w, h, aborter, logger_);//, tpe_);

        returned.the_stage.setX(x);
        returned.the_stage.setY(y);
        return Optional.of(returned);
    }

    //**********************************************************
    private Multiple_image_window(
            //List<Image_play> image_plays_,
            Image_context local_ic,
            boolean smaller,
            double w, double h,
            Aborter aborter_, Logger logger_)
    //**********************************************************
    {
        aborter = aborter_;
        ic = local_ic;
        if (ic == null) {
            logger.log(Stack_trace_getter.get_stack_trace("what ??????"));
        }
        logger = logger_;
        logger.log("Multiple_image_stage !!!");
        the_stage = new Stage();
        {
            Image image = Look_and_feel_manager.get_default_icon(300);
            if (image != null) the_stage.getIcons().add(image);
        }
        the_stage.setWidth(w);
        the_stage.setHeight(h);
        tile_pane = new TilePane();

        set_background();
        scene = new Scene(tile_pane);
        the_stage.setScene(scene);
        the_stage.show();
        if (!smaller) set_stage_size_to_fullscreen(the_stage);

        set_ImageView_compare_1000();


        the_stage.addEventHandler(KeyEvent.KEY_PRESSED,
                new EventHandler<KeyEvent>() {
                    public void handle(final KeyEvent key_event) {

                        if (key_event.getCode() == KeyCode.ESCAPE) {
                            the_stage.close();
                            key_event.consume();
                        }
                    }
                });

    }

    private static int method = 0;
    //**********************************************************
    private void set_ImageView_compare_1000()
    //**********************************************************
    {
        set_background();

        Fx_batch_injector.inject(() -> {
                double size = 1000;
                //ic = get_next_GIF();
                if (ic == null) return;
                // if ( ic.image_is_damaged == false)
                tile_pane.getChildren().clear();
                //ic.imageView.fitWidthProperty().unbind();
                if ( method%2 == 0 )
                {
                    System.out.println("method="+method+" left side is method1");
                    method_1(size);
                    method_2(size);
                }
                else
                {
                    System.out.println("method="+method+" left side is method2");
                    method_2(size);
                    method_1(size);
                }
                method++;
            },logger);
    }

    private void method_1(double size)
    {
        {
            //if ((ic.image.getWidth() > 200) && (ic.image.getHeight() > 200))
            {
                ic.the_image_view.fitWidthProperty().bind(scene.widthProperty().divide(2));
                ic.the_image_view.fitHeightProperty().bind(scene.heightProperty());
            }
        }
        //ic.imageView.setFitWidth(size);
        //ic.imageView.fitHeightProperty().unbind();
        ic.the_image_view.setPreserveRatio(true);
        tile_pane.getChildren().add(ic.the_image_view);
    }

    private void method_2(double size)
    {
        Optional<Image_context> option = get_Image_context_with_alternate_rescaler((int) size);
        if (option.isEmpty()) return;
        option.get().the_image_view.fitWidthProperty().bind(scene.widthProperty().divide(2));
        option.get().the_image_view.fitHeightProperty().bind(scene.heightProperty());
        option.get().the_image_view.setPreserveRatio(true);
        tile_pane.getChildren().add(option.get().the_image_view);
        logger.log("added:" + option.get().path.getFileName());
    }


    //**********************************************************
    private Optional<Image_context> get_Image_context_with_alternate_rescaler(int width)
    //**********************************************************
    {
        if (image_indexer == null)
        {
            image_indexer = Image_indexer.get_Image_indexer(ic.path.getParent(),Paths_manager.alphabetical_file_name_comparator,logger);
        }
        return Static_image_utilities.get_Image_context_with_alternate_rescaler(ic.path, width, aborter, logger);

    }


    //**********************************************************
    void set_background()
    //**********************************************************
    {
        if ((ic.path.getFileName().toString().endsWith(".png")) || (ic.path.getFileName().toString().endsWith(".PNG"))) {
            tile_pane.setBackground(new Background(new BackgroundFill(Color.GREY, CornerRadii.EMPTY, Insets.EMPTY)));
        } else if ((ic.path.getFileName().toString().endsWith(".gif")) || (ic.path.getFileName().toString().endsWith(".GIF"))) {
            tile_pane.setBackground(new Background(new BackgroundFill(Color.DARKGRAY, CornerRadii.EMPTY, Insets.EMPTY)));
        } else {
            tile_pane.setBackground(new Background(new BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY)));
        }
    }

    //**********************************************************
    private void set_stage_size_to_fullscreen(Stage stage)
    //**********************************************************
    {
        Screen screen = null;
        if (stage.isShowing()) {
            // we detect on which SCREEN the stage is (the user may have moved it)
            double minX = stage.getX();
            double minY = stage.getY();
            double width = stage.getWidth();
            double height = stage.getHeight();
            Rectangle2D r = new Rectangle2D(minX + 10, minY + 10, width - 100, height - 100);
            //logger.log("application rec"+r);
            ObservableList<Screen> screens = Screen.getScreensForRectangle(r);
            for (Screen s : screens) {
                //Rectangle2D bounds = s.getVisualBounds();
                //logger.log("screen in rec"+bounds);
                screen = s;
            }

        } else {
            // first time: we show the stage on the primary screen
            screen = Screen.getPrimary();
        }

        //Screen screen = Screen.getPrimary();
        Rectangle2D bounds = screen.getVisualBounds();

        Scene scene = stage.getScene();
        //logger.log("scene getX" + scene.getX());
        //logger.log("scene getY" + scene.getY());
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());

        W = stage.getWidth();
        H = stage.getHeight() - scene.getY();
    }
}
