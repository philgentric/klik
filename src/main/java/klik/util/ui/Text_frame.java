package klik.util.ui;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import klik.actor.Aborter;
import klik.browser.virtual_landscape.Path_comparator_source;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

//**********************************************************
public class Text_frame
//**********************************************************
{
    private static final String TEXT_FRAME = "Text_frame_with_labels";
    private final Path the_path;
    private final WebView web_view = new WebView();
   // private final ScrollPane scroll_pane;
    private final Logger logger;
    private final Stage stage;
    private final AtomicLong font_size_times_1000 = new AtomicLong(2000);

    private final Path_comparator_source path_comparator_source;
    private final Aborter aborter;
    public static void show(Path path, Path_comparator_source path_comparator_source) {
        new Text_frame(path,path_comparator_source);
    }

    List<String> searched = new ArrayList<>();
    //**********************************************************
    private Text_frame(Path path_, Path_comparator_source path_comparator_source)
    //**********************************************************
    {
        the_path = path_;
        this.path_comparator_source = path_comparator_source;
        logger = new File_logger("Text_frame_with_labels");
        aborter = new Aborter("Text_frame_with_labels",logger);
        Filesystem_item_modification_watcher watcher = new Filesystem_item_modification_watcher();
        Aborter aborter = new Aborter("Text_frame_with_labels",logger);
        Filesystem_modification_reporter reporter = () -> {
            //logger.log("Filesystem_item_modification_watcher event ==> RELOADING");
            Platform.runLater(Text_frame.this::reload);
        };
        watcher.init(the_path, reporter, false, 100000, aborter, logger);
        web_view.setFontScale(2.0);
        Scene scene = new Scene(web_view);
        
        stage = new Stage();

        Rectangle2D r = Non_booleans.get_window_bounds(TEXT_FRAME);
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
        stage.setTitle(the_path.toAbsolutePath()+ "   (READ ONLY)   Select text and press 's' or 'k' to highlight all instances");
        stage.setScene(scene);

        stage.addEventHandler(KeyEvent.KEY_PRESSED,
                key_event -> {
                    if (process_key_event(key_event, stage)) return;
                    key_event.consume();
                });

        stage.setOnCloseRequest(e -> {
            aborter.abort("Text_frame_with_labels is closing");
        });
        ChangeListener<Number> change_listener = (observableValue, number, t1) -> {
            logger.log("save_window_bounds for text_frame"+stage.getX()+", "+stage.getY()+", "+stage.getWidth()+", "+stage.getHeight() );
            Non_booleans.save_window_bounds(stage, TEXT_FRAME,logger);
        };
        
        stage.xProperty().addListener(change_listener);
        stage.yProperty().addListener(change_listener);
        stage.show();
        reload();
    }

    //**********************************************************
    private boolean process_key_event(KeyEvent key_event, Stage stage)
    //**********************************************************
    {
        logger.log("process_key_event in Text_frame:"+key_event);

        if (key_event.getCode() == KeyCode.ESCAPE)
        {
            logger.log("process_key_event in Text_frame: ESCAPE");

            stage.close();
            key_event.consume();
            return true;
        }
        if (key_event.isMetaDown())
        {
            double font_scale = font_size_times_1000.get();
            if ( key_event.getCode() == KeyCode.EQUALS)
            {
                if ( font_scale > 4000) font_size_times_1000.set(4000);
                web_view.setFontScale(font_scale/1000.0);
            }
            else if ( key_event.getCode() == KeyCode.MINUS)
            {
                if ( font_scale <0.1 ) font_size_times_1000.set(4000);
                web_view.setFontScale(font_scale/1000.0);
            }
        }
        if((key_event.getText().equals("k"))||(key_event.getText().equals("K"))||(key_event.getText().equals("s"))||(key_event.getText().equals("S")))
        {
            logger.log("process_key_event in Text_frame: k like search_using_keywords_from_the_name");


            String selection = (String) web_view.getEngine().executeScript("window.getSelection().toString()");
            String[] pieces = selection.split(" ");
            searched.clear();
            for (String k : pieces)
            {
                if ( k.trim().isEmpty()) continue;
                searched.add(k.trim());
                logger.log("process_key_event in Text_frame: k like search_using_keywords_from_the_name: k="+k);
            }
            reload();


        }
        logger.log("process_key_event in Text_frame: DONE");

        return false;
    }

    //**********************************************************
    private void reload()
    //**********************************************************
    {
        web_view.getEngine().load("about:blank");

        try {
            List<String> lines = Files.readAllLines(the_path);
            if ( lines.isEmpty())
            {
                web_view.getEngine().loadContent(" ======= EMPTY FILE  =========");
            }
            else {
                StringBuilder t = new StringBuilder();
                t.append("<style type=\"text/css\">\n");
                t.append("p {margin-bottom: 0em;  margin-top: 0em;} \n");
                t.append("</style>");
                for (String line : lines)
                {
                    for ( String w : searched)
                    {
                        if ( line.contains(w))
                        {
                            logger.log("found ->"+w+"<-");
                            line = line.replace(w, "<mark>" + w + "</mark>");
                        }
                    }
                    t.append("<p>").append(line).append("</p>");
                }
                web_view.getEngine().loadContent(t.toString());
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
                        line+="\n";
                    }
                    else
                    {
                        bb[0] = b;
                        line += new String(bb, StandardCharsets.UTF_8);
                    }
                }
                web_view.getEngine().loadContent(line);
            } catch (IOException ex) {
                logger.log(Stack_trace_getter.get_stack_trace(""+e));
                web_view.getEngine().loadContent(" ======= CANNOT READ THIS FILE AT ALL ????  ========="+"\n");
            }

        }
        catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
            web_view.getEngine().loadContent(" ======= CANNOT READ THIS FILE (IS NOT UTF-8 TEXT?)  ========="+"\n");

        }
    }

}
