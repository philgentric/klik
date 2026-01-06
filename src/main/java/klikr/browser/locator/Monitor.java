// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.browser.locator;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Actor_engine;
import klikr.experimental.backup.Backup_singleton;
import klikr.util.ui.Jfx_batch_injector;
import klikr.util.log.Logger;

import java.nio.file.Path;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

//**********************************************************
public class Monitor
//**********************************************************
{

    private LinkedBlockingQueue<String> input_queue = new LinkedBlockingQueue<>();
    private final Path top;
    private final Folders_with_large_images_locator locator;
    private final Logger logger;
    private TextArea textArea;
    Stage stage;

    //**********************************************************
    public Monitor(Path top, Folders_with_large_images_locator locator, Aborter aborter, Logger logger)
    //**********************************************************
    {
        this.top = top;
        this.locator = locator;
        this.logger = logger;
        start_monitoring(aborter);
    }

    //**********************************************************
    private void start_monitoring(Aborter aborter)
    //**********************************************************
    {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                for(;;)
                {
                    if ( aborter.should_abort())
                    {
                        logger.log("Folders_with_large_images_locator aborted");
                        return;
                    }
                    try {
                        String x = input_queue.poll(10, TimeUnit.MINUTES);
                        Jfx_batch_injector.inject(()->textArea.setText(textArea.getText()+"\n"+x),logger);
                    } catch (InterruptedException e) {
                        logger.log_exception("",e);
                        return;
                    }


                }

            }
        };
        Actor_engine.execute(r,"Folder with large images: monitor finder",logger);
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
                logger.log("Folders_with_large_images_locator CANCEL!");
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
        Jfx_batch_injector.inject(()->stage.close(),logger);
    }
}
