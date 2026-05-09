package klikr.util.mmap;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import klikr.browser_core.Image_and_properties;
import klikr.browser_core.icons.image_properties_cache.Image_properties;
import klikr.browser_core.icons.image_properties_cache.Rotation;
import klikr.util.log.Logger;
import klikr.util.log.Simple_logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

//**********************************************************
public class Mmap_test extends Application
//**********************************************************
{
    //**********************************************************
    public static void main(String[] args)
    //**********************************************************
    {
        launch(args);
    }

    //**********************************************************
    @Override
    public void start(Stage stage) throws Exception
    //**********************************************************
    {
        VBox vbox = new VBox();
        Scene scene = new Scene(vbox);
        stage.setScene(scene);
        stage.setMinHeight(800);
        stage.setMinWidth(800);
        stage.show();
        Logger logger = new Simple_logger();
        Mmap mmap = Mmap.get_instance(100, null, logger);
        mmap.clear_cache();
        {
            String tag1 = "src/main/resources/icons/Breton.png";
            Image breton = new Image(new File(tag1).toURI().toString());
            vbox.getChildren().add(new ImageView(breton));
        }

        {
            String tag2 = "src/main/resources/icons/French.png";
            Image french = new Image(new File(tag2).toURI().toString());
            Image_and_properties iap = Image_and_properties.build(french,false);
            mmap.write_image_as_pixels(tag2,iap,true,null);
            logger.log("Mmap Writing image to "+tag2);
            Optional<Image_and_properties> iap2 = mmap.read_image_as_pixels(tag2);
            logger.log("Mmap Reading image from "+tag2);
            if (iap2.isEmpty())
            {
                logger.log("Mmap read error for : "+tag2);
                return;
            }
            vbox.getChildren().add(new ImageView(iap2.get().image()));
        }
        {
            String tag3 = "src/main/resources/icons/Korean.png";
            mmap.write_image_as_file(Path.of(tag3), true, null);
            Optional<Image_and_properties> iap3 = mmap.read_image_as_file(Path.of(tag3));
            if (iap3.isEmpty())
            {
                logger.log("Image not found "+tag3);
                return;
            }vbox.getChildren().add(new ImageView(iap3.get().image()));
        }
        boolean qsdfd = true;
        if ( qsdfd)
        {
            // test#1: file
            Path p = Path.of("test.txt");
            byte[] in = new byte[256];
            for (int i = 0; i < 255; i++)
            {
                in[i] = (byte) i;
            }
            try
            {
                Files.write(p, in);
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
            logger.log("File save request for " + p.toAbsolutePath());
            mmap.write_file(p, true);

            byte[] check = mmap.read_file(p);

            boolean done = true;
            for (int i = 0; i < check.length; i++)
            {
                if (in[i] != check[i])
                {
                    logger.log("FATAL");
                    done = false;
                    break;
                }
            }
            if (done)
            {
                logger.log("OK!!!! Retrieved content: " + new String(check));
            }
        }
    }
}
