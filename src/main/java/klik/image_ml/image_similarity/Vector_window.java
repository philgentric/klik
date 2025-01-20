package klik.image_ml.image_similarity;

import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.stage.Stage;
import klik.actor.Aborter;
import klik.browser.Browser;
import klik.change.Change_gang;
import klik.image_ml.Feature_vector;
import klik.image_ml.Feature_vector_mask;
import klik.images.*;
import klik.level3.fusk.Fusk_static_core;
import klik.level3.fusk.Fusk_strings;
import klik.look.Look_and_feel;
import klik.look.Look_and_feel_manager;
import klik.properties.Static_application_properties;
import klik.util.files_and_paths.Command_old_and_new_Path;
import klik.util.files_and_paths.Moving_files;
import klik.util.files_and_paths.Old_and_new_Path;
import klik.util.files_and_paths.Status_old_and_new_Path;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import klik.util.performance_monitor.Performance_monitor;
import klik.util.ui.Jfx_batch_injector;
import klik.util.ui.Popups;
import org.apache.commons.io.FilenameUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


//**********************************************************
public class Vector_window
//**********************************************************
{
    public static final String VECTOR_WINDOW = "VECTOR_WINDOW";
    private static final double WW = 4;
    private static final double HH = 4;
    private static final int STRIDE = 300/(int)WW;

    static boolean dbg = false;
    public final Scene the_Scene;
    public final Stage the_Stage;
    public final Logger logger;
    public String title_optional_addendum;

    public final Feature_vector fv1;
    public final Feature_vector fv2;


    //**********************************************************
    public Vector_window(
            String title, // this is used to display image similarity
            double x, double y,
            Feature_vector fv1,
            Feature_vector fv2,
            boolean not_same,
            boolean save_window_bounds,
            Logger logger_)
    //**********************************************************
    {
        this.fv1 = fv1;
        this.fv2 = fv2;
        this.title_optional_addendum = title;
        logger = logger_;
        the_Stage = new Stage();
        VBox vbox = new VBox();

        Feature_vector_mask fvm = new Feature_vector_mask(fv1,fv2,not_same,logger);
        int k = 0;
        HBox hbox = null;
        for ( int i  =0 ; i < fv1.features.length; i++)
        {
            if ( k == 0)
            {
                hbox = new HBox();
                vbox.getChildren().add(hbox);
            }
            k++;
            if ( k == STRIDE) k =0;

            //logger.log(title+" "+diff);
            Color color = null;
            if ( fvm.diffs[i]>0.5)
            {
                // red
                color = Color.RED;
            }
            else if ( fvm.diffs[i] == 0.5)
            {
                // green
                color = Color.GREEN;
            }
            else
            {
                // white
                color = Color.WHITE;
            }
            Shape square = new Rectangle(WW,HH, color);
            hbox.getChildren().add(square);
        }
        the_Scene = new Scene(vbox);
        Color background = Look_and_feel_manager.get_instance().get_background_color();
        the_Scene.setFill(background);
        the_Stage.setScene(the_Scene);
        the_Stage.setX(x);
        the_Stage.setY(y);
        the_Stage.setWidth(Image_similarity.W);
        the_Stage.setHeight(100);
        the_Stage.show();
        the_Stage.setTitle(title);


        ChangeListener<Number> change_listener = (observableValue, number, t1) -> {
            if ( dbg) logger.log("ChangeListener: image window position and/or size changed: "+the_Stage.getWidth()+","+ the_Stage.getHeight());
            if ( save_window_bounds) Static_application_properties.save_window_bounds(the_Stage,VECTOR_WINDOW,logger);
        };
        the_Stage.xProperty().addListener(change_listener);
        the_Stage.yProperty().addListener(change_listener);
        the_Stage.widthProperty().addListener(change_listener);
        the_Stage.heightProperty().addListener(change_listener);

    }



}
