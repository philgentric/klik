package klik.util;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Text_frame {

    public static void show(Path path, Logger logger) {
        new Text_frame(path,logger);
    }

    private Text_frame(Path path, Logger logger)
    {
        ScrollPane sp = new ScrollPane();
        sp.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.ALWAYS);
        sp.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.ALWAYS);

        VBox vbox = new VBox();
        sp.setContent(vbox);
        try {
            List<String> lines = Files.readAllLines(path);
            if ( lines.isEmpty())
            {
                vbox.getChildren().add(new Label(" ======= EMPTY FILE  ========="));
            }
            else {
                for (String s : lines)
                {
                    Label l = new Label(s);
                    l.setWrapText(true);
                    l.setPrefWidth(800);
                    vbox.getChildren().add(l);
                }
            }
        }
        catch ( MalformedInputException e)
        {
            try {
                byte[] bytes = Files.readAllBytes(path);
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
        AtomicInteger size = new AtomicInteger(20);
        set_font_size(vbox, size.get());

        vbox.getChildren().add(new Label("                                                                   "));
        vbox.getChildren().add(new Label("-------------------------------------------------------------------"));
        Scene scene = new Scene(sp);
        Stage stage = new Stage();
        stage.setTitle(path.toAbsolutePath()+ "           (READ ONLY)");
        stage.setScene(scene);
        stage.sizeToScene();


        stage.addEventHandler(KeyEvent.KEY_PRESSED,
                key_event -> {
                    if (key_event.getCode() == KeyCode.ESCAPE) {
                        stage.close();
                        key_event.consume();
                        return;
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
                    key_event.consume();
                });
        stage.show();
    }

    private static void set_font_size(VBox vbox, int size) {
        for ( Node n : vbox.getChildren())
        {
            n.setStyle("-fx-font: "+ size+" Helvetica; -fx-font-weight: bold;");
        }
    }
}
