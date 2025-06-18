package klik.images;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import klik.look.Look_and_feel_manager;
import klik.util.ui.Jfx_batch_injector;
import klik.util.log.Logger;

import java.awt.image.BufferedImage;


//**********************************************************
public class Simple_image_window
//**********************************************************
{
    public final Scene the_Scene;
    public final Stage the_Stage;
    final Pane the_image_Pane;
    public final Logger logger;

    //**********************************************************
    public static void open_Simple_image_window(BufferedImage bi, String title, Logger logger)
    //**********************************************************
    {

        if (Platform.isFxApplicationThread())
        {
            new Simple_image_window(bi,title,logger);
        }
        else
        {
            Runnable r = () -> new Simple_image_window(bi,title,logger);
            Jfx_batch_injector.inject(r,logger);
        }
    }

    //**********************************************************
    private Simple_image_window(
            BufferedImage bi,
            String title, Logger logger_)
    //**********************************************************
    {
        logger = logger_;
        the_Stage = new Stage();
        the_Stage.setMinWidth(1000);
        the_Stage.setMinHeight(1000);
        the_image_Pane = new StackPane();
        Look_and_feel_manager.set_region_look(the_image_Pane,the_Stage,logger);

        the_Scene = new Scene(the_image_Pane);

        WritableImage wr = null;
        if (bi != null) {
            wr = new WritableImage(bi.getWidth(), bi.getHeight());
            PixelWriter pw = wr.getPixelWriter();
            for (int x = 0; x < bi.getWidth(); x++) {
                for (int y = 0; y < bi.getHeight(); y++) {
                    pw.setArgb(x, y, bi.getRGB(x, y));
                }
            }
        }
        ImageView iv = new ImageView(wr);
        iv.setPreserveRatio(true);
        iv.fitWidthProperty().bind(the_image_Pane.widthProperty());
        iv.fitHeightProperty().bind(the_image_Pane.heightProperty());

        the_image_Pane.getChildren().add(iv);
        Color background = Look_and_feel_manager.get_instance(the_Stage,logger).get_background_color();
        the_Scene.setFill(background);
        the_Stage.setScene(the_Scene);
        the_Stage.show();
        the_Stage.setTitle(title);


        the_Stage.addEventHandler(KeyEvent.KEY_PRESSED,
                key_event -> {
                    if (key_event.getCode() == KeyCode.ESCAPE)
                    {
                        key_event.consume();
                        the_Stage.close();
                    }
                });

    }




}
