package klik.machine_learning.similarity;

import javafx.beans.value.ChangeListener;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.stage.Stage;
import javafx.stage.Window;
import klik.machine_learning.feature_vector.Feature_vector;
import klik.machine_learning.feature_vector.Feature_vector_double;
import klik.machine_learning.feature_vector.Feature_vector_mask;
import klik.look.Look_and_feel_manager;
import klik.properties.Non_booleans_properties;
import klik.util.log.Logger;


//**********************************************************
public class Vector_window
//**********************************************************
{
    public static final String VECTOR_WINDOW = "VECTOR_WINDOW";
    private static final double WW = 4;
    private static final double HH = 4;
    private static final int STRIDE = 300/(int)WW;

    static boolean dbg = false;
    public final Scene scene;
    public final Stage stage;
    public final Logger logger;
    public String title_optional_addendum;

    public final Feature_vector fv1;
    public final Feature_vector fv2;


    //**********************************************************
    public Vector_window(
            String title, // this is used to display image similarity
            Window owner,
            double x, double y,
            Feature_vector_double fv1,
            Feature_vector_double fv2,
            boolean not_same,
            boolean save_window_bounds,
            Logger logger_)
    //**********************************************************
    {
        this.fv1 = fv1;
        this.fv2 = fv2;
        this.title_optional_addendum = title;
        logger = logger_;
        stage = new Stage();
        stage.initOwner(owner);
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
        scene = new Scene(vbox);
        Color background = Look_and_feel_manager.get_instance(stage,logger).get_background_color();
        scene.setFill(background);
        stage.setScene(scene);
        stage.setX(x);
        stage.setY(y);
        stage.setWidth(Similarity_engine.W);
        stage.setHeight(100);
        stage.show();
        stage.setTitle(title);


        ChangeListener<Number> change_listener = (observableValue, number, t1) -> {
            if ( dbg) logger.log("ChangeListener: image window position and/or size changed: "+ stage.getWidth()+","+ stage.getHeight());
            if ( save_window_bounds) Non_booleans_properties.save_window_bounds(stage,VECTOR_WINDOW,logger);
        };

        stage.addEventHandler(KeyEvent.KEY_PRESSED,
                key_event -> {
                    if (key_event.getCode() == KeyCode.ESCAPE)
                    {
                        stage.close();
                        key_event.consume();
                    }
                });


        stage.xProperty().addListener(change_listener);
        stage.yProperty().addListener(change_listener);
        stage.widthProperty().addListener(change_listener);
        stage.heightProperty().addListener(change_listener);

    }
}
