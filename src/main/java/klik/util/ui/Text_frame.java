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
import klik.actor.Actor_engine;
import klik.look.Look_and_feel_manager;
import klik.properties.Non_booleans_properties;
import klik.util.execute.Execute_via_script_in_tmp_file;
import klik.util.files_and_paths.modifications.Filesystem_item_modification_watcher;
import klik.util.files_and_paths.modifications.Filesystem_modification_reporter;
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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;


//**********************************************************
public class Text_frame
//**********************************************************
{
    private static final boolean dbg = false;
    private static final String TEXT_FRAME = "Text_frame";
    private LinkedBlockingQueue<String> the_queue = null;
    private Path the_path = null;
    private List<String> the_lines = null;
    private final WebView web_view = new WebView();
    private final Logger logger;
    private Stage stage;
    private final AtomicLong font_size_times_1000 = new AtomicLong(2000);

    private Aborter aborter;
    private String marked = "";
    private List<Integer> line_numbers_of_marked_items = new ArrayList<>();
    private int scroll = 0;
    private int number_of_items = 0;
    private int marked_item_index = 0;

    // loads lines into the text frame
    //**********************************************************
    public static void show(List<String> the_lines,  Logger logger)
    //**********************************************************
    {
        new Text_frame(the_lines,logger);
    }

    // loads a file and watches it for changes
    //**********************************************************
    public static void show(Path path,  Logger logger)
    //**********************************************************
    {
        new Text_frame(path,logger);
    }

    // is like a console output window
    //**********************************************************
    public static void show(String command, LinkedBlockingQueue<String> queue, Logger logger)
    //**********************************************************
    {
        new Text_frame(command,queue,logger);
    }

    //**********************************************************
    private Text_frame(
            String command,
            LinkedBlockingQueue<String> queue,
            Logger logger)
    //**********************************************************
    {
        this.logger = logger;
        this.the_path = null;
        this.the_lines = null;
        this.the_queue = queue;

        init(command);

        Runnable r = () -> {
            try {
                //boolean active = false;
                for (;;)
                {
                    String line = the_queue.poll(30, TimeUnit.SECONDS);
                    if (line == null)
                    {
                        continue; // timeout
                    }
                    if ( dbg) logger.log("text frame received: " + line);

                    if ( aborter.should_abort()) break;
                    if ( line.equals(Execute_via_script_in_tmp_file.END))
                    {
                        if ( dbg) logger.log("text frame received END");
                        break;
                    }
                    Platform.runLater(() -> {
                        String jsSafe = "<br>"+ line
                                .replace("\\", "\\\\")
                                .replace("\"", "\\\"")
                                .replace("\n", "\\n")
                                .replace("\r", "");

                        //web_view.getEngine().executeScript("document.body.insertAdjacentHTML('beforeend', \"" + jsSafe + "\");");

                        web_view.getEngine().executeScript("window.appendHtml(\"" + jsSafe + "\");");
                        try {
                            web_view.requestLayout();
                        } catch (Exception ignored) {}

                    });
                }
            }
            catch (InterruptedException e)
            {
                logger.log("Installer output thread interrupted: " + e.getMessage());
            }
        };
        Actor_engine.execute(r,"Text frame source monitor",logger);
    }


    //**********************************************************
    private Text_frame(
            List<String> the_lines,
            Logger logger)
    //**********************************************************
    {
        this.logger = logger;
        this.the_path = null;
        this.the_lines = the_lines;

        init("");
    }

    //**********************************************************
    private Text_frame(
            Path the_path,
            Logger logger)
    //**********************************************************
    {
        this.logger = logger;
        this.the_path = the_path;
        this.the_lines =null;

        init(the_path.toAbsolutePath().toString());

        Filesystem_item_modification_watcher watcher = new Filesystem_item_modification_watcher();
        Filesystem_modification_reporter reporter = () -> {
            //logger.log("Filesystem_item_modification_watcher event ==> RELOADING");
            Platform.runLater(() -> Text_frame.this.reload());
        };
        watcher.init(the_path, reporter, false, 100000, aborter, logger);
    }



    //**********************************************************
    private void init(String title_header)
    //**********************************************************
    {
        web_view.setFontScale(2.0);
        aborter = new Aborter("Text_frame", logger);

        String fontUrl = null;
        try {
            if (getClass().getResource("/fonts/AtkinsonHyperlegible-Bold.ttf") != null) {
                fontUrl = getClass().getResource("/fonts/AtkinsonHyperlegible-Bold.ttf").toExternalForm();
            }
        } catch (Exception ignored) {}

        StringBuilder initial = new StringBuilder();
        initial.append("<!doctype html><html><head><meta charset=\"utf-8\">");

        if (fontUrl != null && !fontUrl.isEmpty()) {
            initial.append("<style type=\"text/css\">")
                    .append("@font-face{font-family:'Atkinson'; src: url('").append(fontUrl).append("') format('truetype'); font-weight: bold; font-style: normal;}")
                    .append("body, p { font-family: 'Atkinson', sans-serif; font-size: 14px; }")
                    .append("p { margin-bottom: 0em; margin-top: 0em; }")
                    .append("</style>");
        } else {
            // fallback: system sans-serif
            initial.append("<style type=\"text/css\">")
                    .append("body, p { font-family: sans-serif; font-size: 14px; }")
                    .append("p { margin-bottom: 0em; margin-top: 0em; }")
                    .append("</style>");
        }

        initial.append("<script>\n")
                .append("window.appendHtml = function(s, forceScroll) {\n")
                .append("  try {\n")
                .append("    // are we near the bottom? (50px tolerance)\n")
                .append("    var nearBottom = (window.innerHeight + window.scrollY) >= (document.body.scrollHeight - 50);\n")
                .append("    document.body.insertAdjacentHTML('beforeend', s);\n")
                .append("    // scroll if forced or if we were already near bottom\n")
                .append("    if (forceScroll === true || nearBottom) {\n")
                .append("      window.scrollTo(0, document.body.scrollHeight+100);\n")
                .append("    }\n")
                .append("  } catch(e) { console.error(e); }\n")
                .append("};\n")
                .append("</script></head><body></body></html>");

        web_view.getEngine().loadContent(initial.toString());

        ChangeListener<? super Worker.State> cl = new ChangeListener<Worker.State>() {
            @Override
            public void changed(ObservableValue<? extends Worker.State> observableValue, Worker.State state, Worker.State newState) {
                // is executed once the page is re-loaded
                //logger.log("Text_frame: load state changed to " + newState);
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
            stage.setWidth(800);
            stage.setHeight(600);
        }
        else
        {
            stage.setX(r.getMinX());
            stage.setY(r.getMinY());
            stage.setWidth(r.getWidth());
            stage.setHeight(r.getHeight());
        }
        String title = title_header;//+" / Select text and press s,k or m to highlight all instances, then d or n to jump down and u or p to jump up";
        stage.setTitle(title);
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
            //logger.log("save_window_bounds for text_frame"+stage.getX()+", "+stage.getY()+", "+stage.getWidth()+", "+stage.getHeight() );
            Non_booleans_properties.save_window_bounds(stage, TEXT_FRAME,logger);
        };

        stage.xProperty().addListener(change_listener);
        stage.yProperty().addListener(change_listener);
        stage.show();
        reload();
    }

    //**********************************************************
    private boolean process_key_event(
            KeyEvent key_event,
            Stage stage)
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

        if ( key_event.getCode()==KeyCode.UP || key_event.getText().equals("u") || key_event.getText().equals("p"))
        {
            process_up();
        }
        if ( key_event.getCode()==KeyCode.DOWN || key_event.getText().equals("d")|| key_event.getText().equals("n"))
        {
            process_down();

        }
        logger.log("process_key_event in Text_frame: DONE");

        return false;
    }

    //**********************************************************
    private void process_down()
    //**********************************************************
    {
        logger.log("process_key_event in Text_frame: DOWN");
        if (marked_item_index>= line_numbers_of_marked_items.size()) return;

        int target_id = line_numbers_of_marked_items.get(marked_item_index);
        logger.log("process_key_event in Text_frame: " + marked_item_index + " => " + target_id);

        marked_item_index++;
        if (marked_item_index >= line_numbers_of_marked_items.size()) marked_item_index = 0;
        String target_s = "" + target_id;
        String script =
                "{" +
                        "let element = document.getElementById(" + target_s + ");" +
                        "if ( element) element.scrollIntoView();" +
                        "}";

        web_view.getEngine().executeScript(script);
    }

    //**********************************************************
    private void process_up()
    //**********************************************************
    {
        logger.log("process_key_event in Text_frame: UP");
        if (marked_item_index>= line_numbers_of_marked_items.size()) return;

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
        if ( the_path != null)
        {
            reload_file(the_path);
            return;
        }
        if ( the_lines != null)
        {
            load_lines(the_lines);
        }
    }
    //**********************************************************
    private void reload_file(Path path)
    //**********************************************************
    {
        try
        {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            load_lines(lines);
        }
        catch ( MalformedInputException e)
        {
            try {
                List<String> lines = Files.readAllLines(path, StandardCharsets.ISO_8859_1);
                load_lines(lines);
            }
            catch ( MalformedInputException ee)
            {
                List<String> lines = try_binary(path);
                load_lines(lines);
            }
            catch (IOException ee)
            {
                logger.log(Stack_trace_getter.get_stack_trace("" + ee));
                web_view.getEngine().loadContent(" ======= CANNOT READ THIS FILE AT ALL ????  =========" + "\n");
            }
        }
        catch (IOException eee) {
            logger.log(Stack_trace_getter.get_stack_trace("" + eee));
        }

    }
    //**********************************************************
    private void load_lines(List<String> lines)
    //**********************************************************
    {
        logger.log("Text_frame, got " + lines.size() + " lines");

        scroll = (int) web_view.getEngine().executeScript("window.scrollY");
        logger.log("scroll=" + scroll);

        web_view.getEngine().load("about:blank");
        line_numbers_of_marked_items.clear();
        number_of_items = 0;

        if ( lines.isEmpty())
        {
            web_view.getEngine().loadContent(" ======= EMPTY FILE  =========");
        }
        else
        {
            StringBuilder t = new StringBuilder();
            t.append("<style type=\"text/css\">\n");
            t.append("body, p { font-family: 'Atkinson', sans-serif; font-size: 14px; }\n");
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

    //**********************************************************
    private List<String> try_binary(Path the_path)
    //**********************************************************
    {
        logger.log("file is binary? ");
        List<String> returned = new ArrayList<>();
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
            returned.add(line);
        }
        catch (IOException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
            returned.add(" ======= CANNOT READ THIS FILE AT ALL ????  ========="+"\n");
        }
        return returned;
    }

}
