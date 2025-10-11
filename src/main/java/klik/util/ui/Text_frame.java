package klik.util.ui;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import klik.actor.Aborter;
import klik.browser.virtual_landscape.Path_comparator_source;
import klik.look.Look_and_feel_manager;
import klik.properties.Non_booleans_properties;
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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

//**********************************************************
public class Text_frame
//**********************************************************
{
    private static final String TEXT_FRAME = "Text_frame";
    private final Path the_path;
    private final WebView web_view = new WebView();
    private final Logger logger;
    private final Stage stage;
    private final AtomicLong font_size_times_1000 = new AtomicLong(2000);

    private final Aborter aborter;
    private String marked = "";
    private List<Integer> line_numbers_of_marked_items = new ArrayList<>();
    private int scroll = 0;
    private int number_of_items = 0;
    private int marked_item_index = 0;

    //**********************************************************
    public static void show(Path path,  Logger logger)
    //**********************************************************
    {
        new Text_frame(path,logger);
    }


    //**********************************************************
    private Text_frame(Path path, Logger logger)
    //**********************************************************
    {
        this.logger = logger;
        the_path = path;
        aborter = new Aborter("Text_frame",logger);
        Filesystem_item_modification_watcher watcher = new Filesystem_item_modification_watcher();
        Filesystem_modification_reporter reporter = () -> {
            //logger.log("Filesystem_item_modification_watcher event ==> RELOADING");
            Platform.runLater(Text_frame.this::reload);
        };
        watcher.init(the_path, reporter, false, 100000, aborter, logger);
        web_view.setFontScale(2.0);




        ChangeListener<? super Worker.State> cl = new ChangeListener<Worker.State>() {
            @Override
            public void changed(ObservableValue<? extends Worker.State> observableValue, Worker.State state, Worker.State newState) {
                // is executed once the page is re-loaded
                if (newState == Worker.State.SUCCEEDED)
                {
                    web_view.getEngine().executeScript("window.scroll(0," + scroll + ");");
                }
            }
        };

        web_view.getEngine().getLoadWorker().stateProperty().addListener(cl);


        Scene scene = new Scene(web_view);
        
        stage = new Stage();

        Rectangle2D r = Non_booleans_properties.get_window_bounds(TEXT_FRAME,stage);
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
        stage.setTitle(the_path.toAbsolutePath()+ "   (READ ONLY)   Select text and press s,k or m to highlight all instances, then d or n to jump down and u or p to jump up");
        stage.setScene(scene);

        stage.addEventHandler(KeyEvent.KEY_PRESSED,
                key_event -> {
                    if (process_key_event(key_event, stage)) return;
                    key_event.consume();
                });

        stage.setOnCloseRequest(e -> {
            aborter.abort("Text_frame is closing");
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
    private boolean process_key_event(KeyEvent key_event, Stage stage)
    //**********************************************************
    {
        logger.log("process_key_event in Text_frame:"+key_event);

        if (key_event.getCode().equals(KeyCode.ESCAPE))
        {
            logger.log("process_key_event in Text_frame: ESCAPE");

            stage.close();
            key_event.consume();
            return true;
        }
        if (key_event.isMetaDown())
        {
            double font_scale = font_size_times_1000.get();
            if ( key_event.getCode().equals(KeyCode.EQUALS))
            {
                if ( font_scale > 4000) font_size_times_1000.set(4000);
                web_view.setFontScale(font_scale/1000.0);
            }
            else if ( key_event.getCode().equals(KeyCode.MINUS))
            {
                if ( font_scale <0.1 ) font_size_times_1000.set(4000);
                web_view.setFontScale(font_scale/1000.0);
            }
        }
        String accelerators_for_marking[] = {"m","k","s"};

        for ( String accel : accelerators_for_marking)
        {
            if ( key_event.getText().equals(accel))
            {
                if (search_and_mark()) return true;
            }
        }

        if( key_event.isControlDown() && key_event.getCode().equals(KeyCode.F))
        {
            if (search_and_mark()) return true;
        }

        if( key_event.isMetaDown() && key_event.getCode().equals(KeyCode.F))
        {
            if (search_and_mark()) return true;
        }

        if ( key_event.getText().equals("u") || key_event.getText().equals("p"))
        {
            logger.log("process_key_event in Text_frame: UP");
            int target_id = line_numbers_of_marked_items.get(marked_item_index);
            logger.log("process_key_event in Text_frame: "+ marked_item_index +" => "+target_id);

            marked_item_index--;
            if ( marked_item_index < 0) marked_item_index = line_numbers_of_marked_items.size()-1;
            String target_s = ""+target_id;
            String script =
                    "{" +
                            "let element = document.getElementById("+target_s+");" +
                            "if ( element) element.scrollIntoView();"+
                            "}";

            web_view.getEngine().executeScript(script);
        }
        if ( key_event.getText().equals("d")|| key_event.getText().equals("n"))
        {
            logger.log("process_key_event in Text_frame: DOWN");
            int target_id = line_numbers_of_marked_items.get(marked_item_index);
            logger.log("process_key_event in Text_frame: "+ marked_item_index +" => "+target_id);

            marked_item_index++;
            if ( marked_item_index >= line_numbers_of_marked_items.size()) marked_item_index = 0;
            String target_s = ""+target_id;
            String script =
            "{" +
                    "let element = document.getElementById("+target_s+");" +
                    "if ( element) element.scrollIntoView();"+
            "}";

            web_view.getEngine().executeScript(script);

        }
        logger.log("process_key_event in Text_frame: DONE");

        return false;
    }

    //**********************************************************
    private boolean search_and_mark()
    //**********************************************************
    {
        logger.log("process_key_event in Text_frame: ");
        marked = (String) web_view.getEngine().executeScript("window.getSelection().toString()");
        if( marked.isEmpty())
        {
            logger.log("process_key_event in Text_frame: marked is empty");
            TextInputDialog dialog = new TextInputDialog("Enter text");
            Look_and_feel_manager.set_dialog_look(dialog,stage,logger);
            dialog.initOwner(stage);
            dialog.setTitle("Enter text to search");
            dialog.setHeaderText("Enter text to search, then use 'd' or 'n' to jump down and 'u' or 'p' to jump up");
            dialog.setContentText("Text:");

            logger.log("dialog !");
            Optional<String> result = dialog.showAndWait();
            if (result.isPresent())
            {
                marked = result.get();
                reload();
            }
            return true;
        }
        reload();
        return true;
    }

    //**********************************************************
    private void reload()
    //**********************************************************
    {
        scroll = (int) web_view.getEngine().executeScript("window.scrollY");
        logger.log("scroll=" + scroll);

        web_view.getEngine().load("about:blank");
        line_numbers_of_marked_items.clear();
        number_of_items = 0;
        List<String> lines = null;
        try
        {
            lines = Files.readAllLines(the_path, StandardCharsets.UTF_8);
        }
        catch ( MalformedInputException e)
        {
            try
            {
                lines = Files.readAllLines(the_path, StandardCharsets.ISO_8859_1);
            }
            catch ( MalformedInputException ee)
            {
                try_binary(the_path);
                return;
            }
            catch (IOException ee)
            {
                logger.log(Stack_trace_getter.get_stack_trace("" + ee));
                web_view.getEngine().loadContent(" ======= CANNOT READ THIS FILE AT ALL ????  =========" + "\n");
                return;
            }
        }
        catch (IOException eee) {
            logger.log(Stack_trace_getter.get_stack_trace("" + eee));
        }

        logger.log("Text_frame, read " + lines.size() + " lines from " + the_path);
        if ( lines.isEmpty())
        {
            web_view.getEngine().loadContent(" ======= EMPTY FILE  =========");
        }
        else
        {
            StringBuilder t = new StringBuilder();
            t.append("<style type=\"text/css\">\n");
            t.append("p {margin-bottom: 0em;  margin-top: 0em;} \n");
            t.append("</style>");
            for (String line : lines)
            {
                if ( line.contains(marked))
                {
                    line = line.replace(marked, "<mark>" + marked + "</mark>");
                    line_numbers_of_marked_items.add(number_of_items);
                }
                String ID_s = ""+number_of_items;
                t.append("<p id=\""+ ID_s +"\">").append(line).append("</p>");
                number_of_items++;
            }
            web_view.getEngine().loadContent(t.toString());
        }


    }

    private void try_binary(Path the_path)
    {
        logger.log("file is binary? ");
        // binary !!!
        try
        {
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
        }
        catch (IOException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
            web_view.getEngine().loadContent(" ======= CANNOT READ THIS FILE AT ALL ????  ========="+"\n");
        }
    }

}
