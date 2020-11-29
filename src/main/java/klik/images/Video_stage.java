package klik.images;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import klik.browser.Browser;
import klik.change.*;
import klik.look.Look_and_feel_manager;
import klik.util.*;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.*;


//**********************************************************
public class Video_stage
//**********************************************************
{
    static Video_stage instance = null; // we enforce a single instance
    static boolean dbg = false;

    Scene scene;
    Stage the_stage;
    //BorderPane border_pane;

    private double old_mouse_x;
    private double old_mouse_y;
    public double W = 1200;
    public double H = 800;
    Logger logger;
    Media media;
    MediaControl media_control;
    Path path;

    //**********************************************************
    public static Video_stage get_Video_stage(
            Stage from_stage, // for on same screen
            Path path_,
            boolean smaller,
            Logger logger_)
    //**********************************************************
    {
        if (instance != null)
        {
            destroy();
        }
        if (from_stage == null) {
            Video_stage vs = new Video_stage(path_, logger_);
            if ( vs.media == null) return null;
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
        Video_stage returned = new Video_stage(path_, logger_);//, tpe_);

        if ( returned.media == null) return null;

        returned.the_stage.setX(x);
        returned.the_stage.setY(y);
        return returned;
    }

    private static void destroy() {
        instance.media_control.stop();
        instance.the_stage.close();
        instance = null;
    }


    //**********************************************************
    private Video_stage(
            Path path_,
            Logger logger_)
    //**********************************************************
    {
        path = path_;
        logger = logger_;
        init_media();
        if ( media == null)
        {
            return;
        }
        instance = this;

        the_stage = new Stage();
        {
            Image image = Look_and_feel_manager.get_default_icon(300);
            if ( image != null) the_stage.getIcons().add(image);
        }

        MediaPlayer mediaPlayer = new MediaPlayer(media);
        mediaPlayer.setAutoPlay(true);
        media_control = new MediaControl(mediaPlayer);
        scene = new Scene(media_control);
        the_stage.setScene(scene);
        the_stage.show();

        mediaPlayer.setOnReady(()-> the_stage.sizeToScene());


        the_stage.setOnCloseRequest(event -> destroy());

        the_stage.addEventHandler(KeyEvent.KEY_PRESSED,
                new EventHandler<KeyEvent>() {
                    public void handle(final KeyEvent keyEvent) {
                        handle_keyboard(logger, the_stage, keyEvent);
                    }
                });

        
        EventHandler<MouseEvent> mouse_clicked_event_handler = new EventHandler<MouseEvent>() {
            public void handle(final MouseEvent mouseEvent) {
                if (mouseEvent.getButton() == MouseButton.SECONDARY) {
                    handle_mouse_clicked_secondary(mouseEvent,logger);
                }
            }
        };
        the_stage.addEventHandler(MouseEvent.MOUSE_CLICKED, mouse_clicked_event_handler);
        
    }


    //**********************************************************
    private void handle_mouse_clicked_secondary(MouseEvent e, Logger logger)
    //**********************************************************
    {
        logger.log("handle_mouse_clicked_secondary");

        final ContextMenu contextMenu = new ContextMenu();
        contextMenu.setStyle("-fx-foreground-color: white;-fx-background-color: darkgrey;");
        MenuItem info = new MenuItem("Path=" + path.toAbsolutePath() + " (i)");
        contextMenu.getItems().add(info);
        info.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                show_info_stage();
            }
        });
        MenuItem edit = new MenuItem("Edit: open file in system-defined Editor for this file type (e)");
        contextMenu.getItems().add(edit);
        edit.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                edit();
            }
        });
        MenuItem open = new MenuItem("Open: open file in system-defined reader for this file type (o)");
        contextMenu.getItems().add(open);
        open.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                open();
            }
        });


        MenuItem browse = new MenuItem("Browse the dir this image is in, in a new browsing window (b)");
        contextMenu.getItems().add(browse);
        browse.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                logger.log("browse this!");
                Browser.create_browser(null, false, path.getParent(), false, logger);

            }
        });

        MenuItem rename = new MenuItem("Rename (r)");
        contextMenu.getItems().add(rename);
        rename.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                ask_user_for_new_name();
            }
        });
        MenuItem copy = new MenuItem("Copy (c)");
        contextMenu.getItems().add(copy);
        copy.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                copy();
            }
        });


        contextMenu.show(media_control, e.getScreenX(), e.getScreenY());
    }

 
    //**********************************************************
    private void show_wait_cursor()
    //**********************************************************
    {
        the_stage.getScene().getRoot().setCursor(Cursor.WAIT);
        //logger.log("cursor = wait");
    }

    //**********************************************************
    private void restore_cursor()
    //**********************************************************
    {
        the_stage.getScene().getRoot().setCursor(Cursor.DEFAULT);
        //logger.log("cursor = default");
    }

    Path next_to_display;
    Image_file_source image_file_source = null;




    //**********************************************************
    private void edit()
    //**********************************************************
    {
        Desktop d = Desktop.getDesktop();
        logger.log("asking desktop to EDIT: " + path.getFileName());
        try {
            d.edit(path.toFile());
        } catch (IOException e) {
            logger.log("edit error:" + e);
        }
    }

    //**********************************************************
    private void open()
    //**********************************************************
    {
        Desktop d = Desktop.getDesktop();
        logger.log("asking desktop to OPEN: " + path.getFileName());
        try {
            d.open(path.toFile());
        } catch (IOException e) {
            logger.log("open error:" + e);
        }
    }


    //**********************************************************
    private void handle_keyboard(Logger logger, Stage stage, final KeyEvent keyEvent)
    //**********************************************************
    {
        if (keyEvent.getCode() == KeyCode.ESCAPE) {
            destroy();
            logger.log("Image_stage is closing (esc)");
            return;
        }

        logger.log("keyboard :" + keyEvent.toString());
        switch (keyEvent.getText()) {
            default:
                break;

 
            case "b":
                logger.log("b like browse");
                Browser.create_browser(null, false, path.getParent(), false, logger);
                break;

            case "c":
                logger.log("c like copy");
                copy();
                break;

            case "d":
                logger.log("d like delete");
                delete_video_file_on_disk();
                break;

            case "e":
                logger.log("e like edit");
                edit();
                break;

            case "i":
                logger.log("i like information");
                show_info_stage();
                break;
                
            case "o":
                logger.log("o like open ");
                open();
                break;

            case "r":
                logger.log("R like rename");
                ask_user_for_new_name();
                break;


            case "v":
                logger.log("v like up Vote");
                ultim();
                break;

 
        }


    }



    //**********************************************************
    public void show_info_stage()
    //**********************************************************
    {
        TextFlow textFlow = new TextFlow();
        textFlow.setLayoutX(40);
        textFlow.setLayoutY(40);
        {
            Text t = new Text(media.toString());
            textFlow.getChildren().add(t);
            textFlow.getChildren().add(new Text(System.lineSeparator()));
        }
        ScrollPane sp = new ScrollPane();
        sp.setPrefSize(1000, 600);
        sp.setContent(textFlow);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);

        Stage local_stage = new Stage();
        local_stage.setHeight(600);
        local_stage.setWidth(1000);

        Scene scene = new Scene(sp, 1000, 600, Color.WHITE);
        local_stage.setTitle(path.toAbsolutePath().toString());
        local_stage.setScene(scene);
        local_stage.show();
    }


    //**********************************************************
    private void delete_video_file_on_disk()
    //**********************************************************
    {
        Tool_box.safe_delete_one(path, logger);
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
            change_name_of_current_image(new_name);
        }
    }

    //**********************************************************
    private void copy()
    //**********************************************************
    {
        if ( Tool_box.popup_ask_for_confirmation("Warning:","You requested that a COPY of this file should be made. Are you sure?") == false) return;

        Path new_path = null;
        for (int i = 0; i < 2056; i++) {
            new_path = Tool_box.generate_new_candidate_name(path, i, logger);
            if (Files.exists(new_path) == false) break;
        }
        if (new_path == null) {
            logger.log("copy failed: could not create new unused name for" + path.getFileName());
            return;
        }

        try {
            Files.copy(path, new_path);
        } catch (IOException e) {
            logger.log("copy failed: could not create new file for" + path.getFileName() + "Exception:" + e);
            return;
        }
        List<Old_and_new_Path> l = new ArrayList<>();
        l.add(new Old_and_new_Path(
                path,
                new_path,
                Command_old_and_new_Path.command_copy,
                Status_old_and_new_Path.copy_done));
        Change_gang.report_event(l);
    }

    //**********************************************************
    private void ultim()
    //**********************************************************
    {
        String Path_name = path.getFileName().toString();
        if (Path_name.contains(Constants.ULTIM)) {
            logger.log("no vote, name already contains " + Constants.ULTIM);
            return;
        }
        int last_index = Path_name.lastIndexOf('.');
        String extension = Path_name.substring(last_index, Path_name.length());

        String new_name = Path_name.substring(0, last_index);
        new_name += Constants.ULTIM;
        new_name += extension;
        logger.log("old name = " + Path_name);
        logger.log("new name = " + new_name);

        change_name_of_current_image(new_name);
    }

    //**********************************************************
    private void change_name_of_current_image(String new_name)
    //**********************************************************
    {
        logger.log("New name: " + new_name);
        if ( image_file_source == null)
        {
            image_file_source = Image_file_source.get_Image_file_source(path.getParent(), logger);
        }
        path = Tool_box.safe_rename(logger, path, new_name);

        set_stage_title();

    }

    //**********************************************************
    private void set_stage_title()
    //**********************************************************
    {
        String local_title = "";
        if (path != null) {
            local_title = path.getFileName().toString();
        }
        if (path.toFile().length() == 0) {
            local_title += " empty file";
        }

         the_stage.setTitle(local_title);
        logger.log("Video_stage title = " + local_title);
    }

    //**********************************************************
    private void init_media()
    //**********************************************************
    {
        URI uri = path.toUri();
        URL url = null;
        try {
            url = uri.toURL();
        } catch (MalformedURLException e) {
            logger.log(Stack_trace_getter.get_stack_trace("FATAL:" + e));
            return;
        }
        try {
            media = new Media(url.toString());
        } catch (MediaException e) {
            logger.log(Stack_trace_getter.get_stack_trace("" + e));
        }

    }



}
