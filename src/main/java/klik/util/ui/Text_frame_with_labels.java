package klik.util.ui;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.*;
import javafx.stage.Stage;
import klik.actor.Aborter;
import klik.properties.Non_booleans_properties;
import klik.util.files_and_paths.Filesystem_item_modification_watcher;
import klik.util.files_and_paths.Filesystem_modification_reporter;
import klik.util.log.Logger;
import klik.util.log.Logger_factory;
import klik.util.log.Stack_trace_getter;

import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

// Text_frame using webview is much better
//**********************************************************
public class Text_frame_with_labels
//**********************************************************
{
    private static final String TEXT_FRAME = "Text_frame_with_labels";
    private final Path the_path;
    private final VBox container = new VBox();
    private final ScrollPane scroll_pane;
    private final Logger logger;
    private final Stage stage;
    private final AtomicInteger size = new AtomicInteger(20);

    public static void show(Path path, Logger logger) {
        new Text_frame_with_labels(path,logger);
    }

    //**********************************************************
    private Text_frame_with_labels(Path path_, Logger logger_)
    //**********************************************************
    {
        the_path = path_;
        logger = Logger_factory.get("Text_frame_with_labels");
        Filesystem_item_modification_watcher watcher = new Filesystem_item_modification_watcher();
        Aborter aborter = new Aborter("Text_frame_with_labels",logger);
        Filesystem_modification_reporter reporter = () -> {
            //logger.log("Filesystem_item_modification_watcher event ==> RELOADING");
            Platform.runLater(Text_frame_with_labels.this::reload);
        };
        watcher.init(the_path, reporter, false, 100000, aborter, logger);


        scroll_pane = new ScrollPane();
        scroll_pane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll_pane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        container.prefWidthProperty().bind(scroll_pane.widthProperty());
        scroll_pane.setContent(container);
        //set_font_size(container, size.get());

        Scene scene = new Scene(scroll_pane);


        stage = new Stage();

        Rectangle2D r = Non_booleans_properties.get_window_bounds(TEXT_FRAME, stage);
        if ( r == null)
        {
            stage.setX(100);
            stage.setY(100);
            stage.setWidth(1000);
            stage.setHeight(1000);
        }
        else
        {
            stage.setX(r.getMinX());
            stage.setY(r.getMinY());
            stage.setWidth(r.getWidth());
            stage.setHeight(r.getHeight());
        }
        stage.setTitle(the_path.toAbsolutePath()+ "           (READ ONLY)");
        stage.setScene(scene);
        //stage.sizeToScene();

        stage.addEventHandler(KeyEvent.KEY_PRESSED,
                key_event -> {
                    if (process_key_event(key_event, stage, size)) return;
                    key_event.consume();
                });

        stage.setOnCloseRequest(e -> {
            aborter.abort("Text_frame_with_labels is closing");
        });
        ChangeListener<Number> change_listener = (observableValue, number, t1) -> {
            logger.log("save_window_bounds for text_frame"+stage.getX()+", "+stage.getY()+", "+stage.getWidth()+", "+stage.getHeight() );
            Non_booleans_properties.save_window_bounds(stage, TEXT_FRAME,logger);
        };
        stage.xProperty().addListener(change_listener);
        stage.yProperty().addListener(change_listener);
        stage.show();
        reload();


    }

    //**********************************************************
    private boolean process_key_event(KeyEvent key_event, Stage stage, AtomicInteger size)
    //**********************************************************
    {
        if (key_event.getCode() == KeyCode.ESCAPE) {
            stage.close();
            key_event.consume();
            return true;
        }
        if (key_event.isMetaDown())
        {
            if ( key_event.getCode() == KeyCode.EQUALS)
            {
                size.getAndIncrement();
                if ( size.get() > 40) size.set(40);
                //set_font_size(container, size.get());
            }
            else if ( key_event.getCode() == KeyCode.MINUS)
            {
                size.getAndDecrement();
                if ( size.get() < 4) size.set(4);
                //set_font_size(container, size.get());
            }
        }
        return false;
    }

    //**********************************************************
    private void reload()
    //**********************************************************
    {
        //vbox.getChildren().clear();
        container.getChildren().clear();

        try {
            List<String> lines = Files.readAllLines(the_path);
            if ( lines.isEmpty())
            {
                container.getChildren().add(new Label(" ======= EMPTY FILE  ========="));
            }
            else {
                for (String s : lines)
                {
                    Text t = new Text(s+" \n");
                    t.setStyle("-fx-border-color: transparent;-fx-focus-color: transparent;-fx-text-box-border: transparent;-fx-font: "+ size+" Helvetica; -fx-font-weight: bold;");
                    container.getChildren().add(t);
                }
            }
        }
        catch ( MalformedInputException e)
        {
            // binary !!!
            try {
                byte[] bytes = Files.readAllBytes(the_path);
                byte bb[] = new byte[1];
                String line = "";
                for ( byte b : bytes)
                {
                    if (( b == 10)||(b==13))
                    {
                        container.getChildren().add(new TextField(line+"\n"));
                        line = "";
                    }
                    else
                    {
                        bb[0] = b;
                        line += new String(bb, StandardCharsets.UTF_8);
                    }
                }
            } catch (IOException ex) {
                logger.log(Stack_trace_getter.get_stack_trace(""+e));
                container.getChildren().add(new TextField(" ======= CANNOT READ THIS FILE AT ALL ????  ========="+"\n"));
            }

        }
        catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
            container.getChildren().add(new TextField(" ======= CANNOT READ THIS FILE (IS NOT UTF-8 TEXT?)  ========="+"\n"));

        }
    }

    //**********************************************************
    private static void set_font_size(Pane vbox, int size)
    //**********************************************************
    {
        for ( Node n : vbox.getChildren())
        {
            n.setStyle("-fx-font: "+ size+" Helvetica; -fx-font-weight: bold;");
        }
    }
}
