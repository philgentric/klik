package klik.browser.locator;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import klik.actor.Actor_engine;
import klik.level2.backup.Backup_singleton;
import klik.util.Logger;
import klik.util.Threads;

import java.nio.file.Path;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

//**********************************************************
public class Monitor
//**********************************************************
{

    private LinkedBlockingQueue<String> input_queue = new LinkedBlockingQueue<>();
    private final Path top;
    private final Locator locator;
    private final Logger logger;
    private TextArea textArea;
    Stage stage;

    //**********************************************************
    public Monitor(Path top, Locator locator, Logger logger)
    //**********************************************************
    {
        this.top = top;
        this.locator = locator;
        this.logger = logger;
        start_monitoring();
    }

    //**********************************************************
    private void start_monitoring()
    //**********************************************************
    {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                for(;;)
                {
                    try {
                        String x = input_queue.poll(10, TimeUnit.MINUTES);
                        Runnable r2 = new Runnable() {
                            @Override
                            public void run() {
                                textArea.setText(textArea.getText()+"\n"+x);
                            }
                        };
                        Platform.runLater(r2);
                    } catch (InterruptedException e) {
                        logger.log_exception("",e);
                        return;
                    }


                }

            }
        };
        Actor_engine.execute(r,logger);
    }

    //**********************************************************
    public void show(String msg)
    //**********************************************************
    {
        input_queue.add(msg);
    }

    //**********************************************************
    public void realize()
    //**********************************************************
    {
        stage = new Stage();
        stage.setTitle("Looking for images in :"+top.toAbsolutePath());
        VBox vbox = new VBox();


        Button cancel = new Button("cancel");
        {
            cancel.setOnAction(actionEvent -> {
                logger.log("Locator CANCEL!");
                locator.cancel();
                stage.close();
                Backup_singleton.abort();
            });
        }
        vbox.getChildren().add(cancel);

        textArea = new TextArea();
        textArea.setWrapText(true);
        textArea.setPrefColumnCount(80);
        textArea.setPrefRowCount(80);
        vbox.getChildren().add(textArea);

        Scene scene = new Scene(vbox);
        stage.setScene(scene);
        stage.show();
    }

    public void close() {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                stage.close();
            }
        };
        Platform.runLater(r);
    }
}
