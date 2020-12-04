package klik.browser;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;
import klik.change.Change_gang;
import klik.images.Multiple_image_stage;
//implement_video
//import klik.video.Video_stage;
import klik.images.Exif_metadata_extractor;
import klik.images.Image_stage;
import klik.look.Look_and_feel_manager;
import klik.util.Guess_file_type_from_extension;
import klik.util.Logger;
import klik.util.Stack_trace_getter;
import klik.util.Tool_box;

import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

//**********************************************************
public class Item_image extends Item
//**********************************************************
{
    protected final ImageView imageview;
    private double icon_size;
    private int rotation = -1; // cache
    public Icon_status icon_status = Icon_status.no_icon;
    //**********************************************************
    public Item_image(
            Browser b,
            Path f_,
            Scene scene,
            double icon_size_,
            Logger logger)
    //**********************************************************
    {
        super(b,f_, scene, logger);
        icon_size = icon_size_;
        imageview = new ImageView();
    }

    //**********************************************************
    public void load_default_icon(
            Stage from_stage,
            Logger logger
            )
    //**********************************************************
    {
        double actual_icon_size = icon_size/3;
        Image i = Look_and_feel_manager.get_default_icon(actual_icon_size);
        imageview.setImage(i);
        imageview.setPreserveRatio(true);
        imageview.setSmooth(true);
        //imageview.setCache(true);
        imageview.setFitWidth(actual_icon_size);
        imageview.setFitHeight(actual_icon_size);

        imageview.setOnMouseClicked(event ->
        {
            logger.log("\n\nItem_image event="+event.toString());

            if ( event.getButton() == MouseButton.SECONDARY)
            {
                logger.log("\n\nItem_image isSecondaryButtonDown");
                ContextMenu context_menu = define_a_menu_to_the_imageview();
                context_menu.show(imageview, event.getScreenX(), event.getScreenY());
                return;
            }
            if ( event.isMetaDown() ) {
                Multiple_image_stage s = Multiple_image_stage.get_Multiple_image_stage(from_stage, get_Path(), false, logger);
                logger.log("\n\nMULTI");
                if (s == null) {
                    // let us a bit of checking about why this failed

                    Change_gang.report_anomaly(get_Path());
                }
                return;
            }
            {
                logger.log("\n\nItem_image OnMouseClicked " + get_Path());
                //implement_video
               // if (Guess_file_type_from_extension.is_this_path_a_video(get_Path())) {
               //     open_a_video(from_stage, logger);
               // } else
                    {
                    open_an_image(from_stage, logger);
                }
            }

        });
        imageview.setManaged(false);

        init_drag_and_drop();

    }

    //**********************************************************
    private void open_an_image(Stage from_stage, Logger logger)
    //**********************************************************
    {
        Image_stage s = Image_stage.get_Image_stage(from_stage, get_Path(), false, logger);
        logger.log("\n\nImage_stage opening for path:"+get_Path().toString());

        if (s == null) {
            // let us a bit of checking about why this failed

            Change_gang.report_anomaly(get_Path());
        }
    }

    /*

    //**********************************************************
    private void open_a_video(Stage from_stage, Logger logger)
    //**********************************************************
    {
        // this is a video! need to open it with the system !

        Video_stage s = Video_stage.get_Video_stage(from_stage, get_Path(), false, logger);
        logger.log("\n\nVideo_stage opening for path:"+get_Path().toString());

        if (s != null) return;

        // try to open it with the system

        try
        {
            Desktop.getDesktop().open(path.toAbsolutePath().toFile());
        }
        catch (Exception e)
        {
            logger.log(Stack_trace_getter.get_stack_trace("open failed :" + e));

            if (e.toString().contains("doesn't exist."))
            {
                Tool_box.popup_text("Failed?", "Your OS/GUI could not open this file, the error is:\n" + e);
            }
            else
            {
                Tool_box.popup_text("Failed?", "Your OS/GUI could not open this file, the error is:\n" + e + "\nMaybe it is just not properly configured e.g. most often the file extension has to be registered?");
            }
        }
    }
*/
    //**********************************************************
    public ContextMenu define_a_menu_to_the_imageview()
    //**********************************************************
    {
        ContextMenu context_menu = new ContextMenu();

        {
            javafx.scene.control.MenuItem item2 = new javafx.scene.control.MenuItem("Rename");
            item2.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event)
                {
                    if (dbg) logger.log("Renaming!");
                    ask_user_for_new_name();
                }
            });
            context_menu.getItems().add(item2);
        }
        {
            javafx.scene.control.MenuItem item2 = new javafx.scene.control.MenuItem("Delete");
            item2.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    if (dbg) logger.log("Deleting!");
                    Tool_box.safe_delete_one(path, logger);
                }
            });
            context_menu.getItems().add(item2);

        }
        {
            javafx.scene.control.MenuItem item2 = new MenuItem("Show file size");
            item2.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    if (dbg) logger.log("File size");

                    String file_size = Tool_box.get_2_line_string_with_size(path);


                    Tool_box.popup_text("File size for:"+path.getFileName().toString(), file_size);
                }
            });
            context_menu.getItems().add(item2);

        }
        return context_menu;
        /*
        imageview.setOnContextMenuRequested(new EventHandler<ContextMenuEvent>()
        {
            @Override
            public void handle(ContextMenuEvent event)
            {
                context_menu.show(imageview, event.getScreenX(), event.getScreenY());
            }
        });*/
    }


    //**********************************************************
    private void ask_user_for_new_name()
    //**********************************************************
    {
        TextInputDialog dialog = new TextInputDialog(path.getFileName().toString());
        dialog.setTitle("Rename");
        dialog.setHeaderText("To rename this image, enter the new name:");
        dialog.setContentText("New name:");

        // The Java 8 way to get the response value (with lambda expression).
        //result.ifPresent(name -> logger.log("Your name: " + name));
        // Traditional way to get the response value.
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String new_name = result.get();
            path = Tool_box.safe_rename(logger, path, new_name);
        }
    }

    @Override
    public void set_MinWidth(double w)
    {
        icon_size = w;
    }

    @Override
    public double get_Width()
    {
        return icon_size;//imageview.getFitWidth();
    }

    @Override
    public void set_MinHeight(double h)
    {
    } // not used, the height is defined by the IMAGE(icon!) in the imageview

    protected Double height = null; // cache !

    //**********************************************************
    @Override
    public double get_Height()
    //**********************************************************
    {
        if ( height == null)
        {
            Image image = imageview.getImage();
            if ( image == null) return icon_size;

            if (( imageview.getRotate() == 90) || ( imageview.getRotate() == 270))
            {
                height = image.getWidth();
            }
            else
            {
                height = image.getHeight();
            }
        }
        return height;

    }

    //**********************************************************
     @Override
    public void set_Image(Image i, boolean image_is_the_good_one)
     //**********************************************************
    {


       if ( visible_in_scene == false)
        {
            if ( dbg) g(0);
            imageview.setImage(null);
            icon_status = Icon_status.no_icon;
            return;
        }

            Platform.runLater(new Runnable() {
            @Override
            public void run() {
                do_it_in_fx_thread(i, image_is_the_good_one);
            }
        });
    }

    //public void set_imageview_null(){imageview=null;};

    //**********************************************************
    public void do_it_in_fx_thread(Image i, boolean image_is_the_good_one)
    //**********************************************************
    {
        imageview.setImage(i);
        imageview.setVisible(true);
        if ( image_is_the_good_one)
        {
            if ( rotation < 0)
            {
                if (Files.exists(get_Path()))
                {
                    if (Guess_file_type_from_extension.is_this_path_a_video(get_Path()))
                    {
                        rotation = 0;
                    }
                    else {
                        Exif_metadata_extractor extractor = new Exif_metadata_extractor(get_Path(), get_logger());
                        rotation = extractor.get_rotation();
                    }
                }
                else
                {
                    imageview.setImage(null);
                    icon_status = Icon_status.no_icon;
                    if ( dbg) g(3);
                    return;
                }

            }
            //get_logger().log(Platform.isFxApplicationThread()+ " actual icon w="+i.getWidth()+" h="+i.getHeight());

            // the above operation can take some time...
            // and in the mean time the situation can change
            if ( visible_in_scene == false)
            {
                imageview.setImage(null);
                icon_status = Icon_status.no_icon;
                if ( dbg) g(4);
                return;
            }
            imageview.setRotate(rotation);
            imageview.setSmooth(true);
            imageview.setFitWidth(icon_size);
            imageview.setFitHeight(icon_size);
            icon_status = Icon_status.true_icon;
        }
        else
        {
            icon_status = Icon_status.default_icon;
        }

        height = null;

    }

    private void g(int i)
    {
        get_logger().log("finally invisible "+i);
    }


    @Override
    public Node get_Node()
    {
        return imageview;
    }


    @Override
    public String get_string()
    {
        return "image "+get_Path().toAbsolutePath();
    };

}