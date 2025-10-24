//SOURCES ../../image_indexer/Indexer.java
package klik.experimental.work_in_progress;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import klik.util.execute.actor.Aborter;
import klik.path_lists.Path_list_provider;
import klik.images.Image_context;
import klik.look.Look_and_feel_manager;
import klik.util.image.Static_image_utilities;
import klik.util.image.rescaling.Image_rescaling_filter;
import klik.util.ui.Jfx_batch_injector;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


//**********************************************************
public class Multiple_image_window
//**********************************************************
{
    static boolean dbg = false;

    final Scene scene;
    final Stage the_stage;
    final TilePane tile_pane;

    public double W = 1200;
    public double H = 800;
    Logger logger;

    //Indexer image_indexer = null;
    private Image_context ic;
    public final Aborter aborter;
    private final Path_list_provider path_list_provider;
    //**********************************************************
    public static Optional<Multiple_image_window> get_Multiple_image_window(
            String title,
            Window from_stage, // for on same screen
            Path path,
            boolean smaller,
            Path_list_provider path_list_provider,
            Logger logger_)
    //**********************************************************
    {
        Aborter aborter = new Aborter("Multiple_image_window",logger_);
        Optional<Image_context> option = Image_context.get_Image_context(
                path,
                from_stage, aborter, logger_);
        if (option.isEmpty()) {
            logger_.log(Stack_trace_getter.get_stack_trace("❌ Multiple_image_stage PANIC: cannot load image " + path.toAbsolutePath()));
            return Optional.empty();
        }
        logger_.log("Multiple_image_stage OK: image loaded" + path.toAbsolutePath());
        if (from_stage == null) {
            return Optional.of(new Multiple_image_window(title,option.get(), smaller, 800, 600, path_list_provider,aborter, logger_));
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
        Multiple_image_window returned = new Multiple_image_window(title, option.get(), smaller, w, h, path_list_provider,aborter, logger_);//, tpe_);

        returned.the_stage.setX(x);
        returned.the_stage.setY(y);
        return Optional.of(returned);
    }

    //**********************************************************
    private Multiple_image_window(
            String title,
            Image_context local_ic,
            boolean smaller,
            double w, double h,
            Path_list_provider path_list_provider,
            Aborter aborter_, Logger logger_)
    //**********************************************************
    {
        this.path_list_provider = path_list_provider;
        aborter = aborter_;
        ic = local_ic;
        if (ic == null) {
            logger.log(Stack_trace_getter.get_stack_trace("what ??????"));
        }
        logger = logger_;
        logger.log("Multiple_image_stage !!!");
        the_stage = new Stage();
        {
            //Image image = Look_and_feel_manager.get_default_icon(300);
            //if (image != null) the_stage.getIcons().add(image);
            Look_and_feel_manager.set_icon_for_main_window(the_stage,"multi", Look_and_feel_manager.Icon_type.KLIK,the_stage,logger);
        }
        the_stage.setWidth(w);
        the_stage.setHeight(h);
        the_stage.setTitle(title);
        tile_pane = new TilePane();

        set_background();
        VBox vbox = new VBox();
        vbox.getChildren().add(tile_pane);
        {
            Button b = new Button("Swap");
            vbox.getChildren().add(b);
            b.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    swap();
                }
            });
        }
        scene = new Scene(vbox);
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

    //**********************************************************
    record My_ImageView(ImageView iv, String label)
    //**********************************************************
    {
        //**********************************************************
        VBox get()
        //**********************************************************
        {
            VBox vb = new VBox();
            vb.getChildren().add(new TextField(label));
            vb.getChildren().add(iv);
            return vb;
        }
    }

    List<My_ImageView> my_image_views = new ArrayList<>();
    //**********************************************************
    private void swap()
    //**********************************************************
    {
        tile_pane.getChildren().clear();
        tile_pane.getChildren().add(my_image_views.get(1).get());
        tile_pane.getChildren().add(my_image_views.get(0).get());
        My_ImageView tmp0 = my_image_views.get(0);
        My_ImageView tmp1 = my_image_views.get(1);

        my_image_views.clear();
        my_image_views.add(tmp1);
        my_image_views.add(tmp0);

    }

    private static int method = 1;
    //**********************************************************
    private void set_ImageView_compare_1000()
    //**********************************************************
    {
        set_background();

        Jfx_batch_injector.inject(() -> {
                double size = 1000;
                //ic = get_next_GIF();
                if (ic == null) return;
                // if ( ic.image_is_damaged == false)
                tile_pane.getChildren().clear();
                //ic.imageView.fitWidthProperty().unbind();
                if ( method%2 == 0 )
                {
                    System.out.println("method="+method+" left side is method1");
                    method_1();
                    method_2(size,100);
                }
                else
                {
                    System.out.println("method="+method+" left side is method2");
                    method_2(size,100);
                    method_1();
                }
                method++;
            },logger);
    }

    //**********************************************************
    private void method_1()
    //**********************************************************
    {
        add(ic.the_image_view,"javafx");
        logger.log("javafx rescaled used for:" + ic.path.getFileName());
    }


    //**********************************************************
    private void method_2(double w, double h)
    //**********************************************************
    {
        Optional<Image_context> option = get_Image_context_with_alternate_rescaler((int) w, (int) h);
        if (option.isEmpty()) return;
        ImageView local = option.get().the_image_view;
        add(local,"Magic kernel sharp");
        logger.log("bicubic rescaled used for:" + option.get().path.getFileName());
    }

    //**********************************************************
    private void method_3(double w, double h)
    //**********************************************************
    {
        Optional<Image_context> option = get_Image_context_with_alternate_rescaler((int) w, (int) h);
        if (option.isEmpty()) return;
        ImageView local = option.get().the_image_view;
        add(local,"Lanczos");
        logger.log("Lanczos rescaled used for:" + option.get().path.getFileName());
    }

    //**********************************************************
    private void add(ImageView local, String txt)
    //**********************************************************
    {
        local.fitWidthProperty().bind(scene.widthProperty().divide(2));
        local.fitHeightProperty().bind(scene.heightProperty());
        local.setPreserveRatio(true);
        My_ImageView miv = new My_ImageView(local,txt);
        my_image_views.add(miv);
        tile_pane.getChildren().add(miv.get());
    }


    //**********************************************************
    private Optional<Image_context> get_Image_context_with_alternate_rescaler(int width, int height)
    //**********************************************************
    {
        /*if (image_indexer == null)
        {
            image_indexer = Indexer.get_Image_indexer(path_list_provider,new Alphabetical_file_name_comparator(),aborter,logger);
        }*/
        return Static_image_utilities.get_Image_context_with_alternate_rescaler(ic.path, width, height, Image_rescaling_filter.MagicKernelSharp2021, the_stage, aborter, logger);

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
