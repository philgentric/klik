package klik.util.ui;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import klik.actor.Aborter;
import klik.properties.Non_booleans;
import klik.util.files_and_paths.Filesystem_item_modification_watcher;
import klik.util.files_and_paths.Filesystem_modification_reporter;
import klik.util.log.File_logger;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;

import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

//**********************************************************
public class Text_frame
//**********************************************************
{
    private static final String TEXT_FRAME = "Text_frame";
    private final Path the_path;
    private final VBox vbox = new VBox();
    private final Logger logger;
    private final AtomicInteger size = new AtomicInteger(20);

    public static void show(Path path, Logger logger) {
        new Text_frame(path,logger);
    }

    //**********************************************************
    private Text_frame(Path path_, Logger logger_)
    //**********************************************************
    {
        the_path = path_;
        logger = new File_logger("Text_frame");//logger_;
        Filesystem_item_modification_watcher watcher = new Filesystem_item_modification_watcher();
        Aborter aborter = new Aborter("Text_frame",logger);
        Filesystem_modification_reporter reporter = new Filesystem_modification_reporter() {
            @Override
            public void report_modified() {
                //logger.log("Filesystem_item_modification_watcher event ==> RELOADING");
                Platform.runLater(Text_frame.this::reload);
            }
        };
        watcher.init(the_path, reporter, false, 100000, aborter, logger);


        ScrollPane sp = new ScrollPane();
        sp.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.ALWAYS);
        sp.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.ALWAYS);

        sp.setContent(vbox);
        reload();
        set_font_size(vbox, size.get());

        Scene scene = new Scene(sp);


        Stage stage = new Stage();

        Rectangle2D r = Non_booleans.get_window_bounds(TEXT_FRAME, logger);
        if ( r == null)
        {
            stage.setX(100);
            stage.setY(1000);
            stage.setWidth(500);
            stage.setHeight(500);
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
            aborter.abort("Text_frame is closing");
        });
        ChangeListener<Number> change_listener = (observableValue, number, t1) -> {
            logger.log("save_window_bounds for text_frame"+stage.getX()+", "+stage.getY()+", "+stage.getWidth()+", "+stage.getHeight() );
            Non_booleans.save_window_bounds(stage, TEXT_FRAME, logger);
        };
        stage.xProperty().addListener(change_listener);
        stage.yProperty().addListener(change_listener);
        stage.show();

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
                set_font_size(vbox, size.get());
            }
            else if ( key_event.getCode() == KeyCode.MINUS)
            {
                size.getAndDecrement();
                if ( size.get() < 4) size.set(4);
                set_font_size(vbox, size.get());
            }
        }
        return false;
    }

    //**********************************************************
    private void reload()
    //**********************************************************
    {
        vbox.getChildren().clear();
        try {
            List<String> lines = Files.readAllLines(the_path);
            if ( lines.isEmpty())
            {
                vbox.getChildren().add(new Label(" ======= EMPTY FILE  ========="));
            }
            else {
                for (String s : lines)
                {
                    Label l = new Label(s);
                    l.setStyle("-fx-font: "+ size+" Helvetica; -fx-font-weight: bold;");
                    l.setWrapText(true);
                    l.setPrefWidth(2000);
                    vbox.getChildren().add(l);
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
                        vbox.getChildren().add(new Label(line));
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
                vbox.getChildren().add(new Label(" ======= CANNOT READ THIS FILE AT ALL ????  ========="));
            }

        }
        catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
            vbox.getChildren().add(new Label(" ======= CANNOT READ THIS FILE (IS NOT UTF-8 TEXT?)  ========="));

        }
    }

    //**********************************************************
    private static void set_font_size(VBox vbox, int size)
    //**********************************************************
    {
        for ( Node n : vbox.getChildren())
        {
            n.setStyle("-fx-font: "+ size+" Helvetica; -fx-font-weight: bold;");
        }
    }
}
