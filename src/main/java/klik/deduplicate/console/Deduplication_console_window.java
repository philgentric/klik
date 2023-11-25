package klik.deduplicate.console;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import klik.actor.Aborter;
import klik.util.Logger;
import klik.util.Stack_trace_getter;
import klik.util.Threads;

import java.util.concurrent.LinkedBlockingDeque;


//**********************************************************
public class Deduplication_console_window
//**********************************************************
{
    private static final boolean dbg = false;
    public static final String STATUS = "Status: ";
    Aborter aborter;
    int count_directory_examined;
    Label label_directory_examined;
    Label label_total_to_be_examined;
    int count_examined;
    Label label_examined;
    int count_to_be_deleted;
    Label label_count_to_be_deleted;
    private int count_deleted;
    Label label_count_deleted;
    ProgressBar progress_bar_examined;
    ProgressBar progress_bar_deleted;
    int total_to_be_examined;
    Label label_status;
    private final boolean just_count;

    Logger logger;

    Deduplication_console_interface the_console;

    //**********************************************************
    public Deduplication_console_window(
            String title,
            double w, double h,
            boolean just_count_,
            Aborter aborter_,
            Logger logger)
    //**********************************************************
    {
        aborter = aborter_;
        just_count = just_count_;
        this.logger = logger;
        the_console = new Deduplication_console_interface(this, logger);
        Stage stage = new Stage();
        stage.setHeight(h);
        stage.setWidth(w);

        stage.setTitle(title);
        VBox vbox = new VBox();
        Scene scene = new Scene(vbox);//, w, h, Color.WHITE);
        stage.setScene(scene);
        stage.show();

        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent windowEvent) {

                logger.log("Deduplication_console_window: closing the window");
                abort();
            }
        });
        Button cancel = new Button("cancel");
        {
            cancel.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent actionEvent) {
                    logger.log("Deduplication_console_window: cancel button");
                    abort();
                    stage.close();

                }
            });
        }
        vbox.getChildren().add(cancel);
        {
            label_status = new Label(STATUS);
            vbox.getChildren().add(label_status);
        }
        {
            label_directory_examined = new Label();
            vbox.getChildren().add(label_directory_examined);
            label_total_to_be_examined= new Label();
            vbox.getChildren().add(label_total_to_be_examined);
        }
        {
            label_examined = new Label();
            //label_examined.setWrapText(true);
            vbox.getChildren().add(label_examined);
            progress_bar_examined = new ProgressBar();
            progress_bar_examined.setPrefWidth(600);
            vbox.getChildren().add(progress_bar_examined);
        }
        if ( !just_count) {
            label_count_to_be_deleted = new Label();
            //label_count_to_be_deleted.setWrapText(true);
            vbox.getChildren().add(label_count_to_be_deleted);
        }

        if ( !just_count)
        {
            label_count_deleted = new Label();
            //label_examined.setWrapText(true);
            vbox.getChildren().add(label_count_deleted);
            progress_bar_deleted = new ProgressBar();
            progress_bar_deleted.setPrefWidth(600);
            vbox.getChildren().add(progress_bar_deleted);
        }

        start_display_updating_event_pump();
    }


    //**********************************************************
    public void abort()
    //**********************************************************
    {
        aborter.abort();
        Thing_to_do thing_to_do = Thing_to_do.get_die_thing_to_do();
        the_console.add(thing_to_do);
    }

    //**********************************************************
    private void start_display_updating_event_pump()
    //**********************************************************
    {
        logger.log("starting thing to do thread");
        LinkedBlockingDeque<Thing_to_do> queue = the_console.get_queue();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                for (;;)
                {
                    if (aborter.should_abort()) return;
                    process_event(queue);
                }
            }
        };
        Threads.execute(r, logger);
    }

    // returns true if the thread should stop
    //**********************************************************
    private void process_event(LinkedBlockingDeque<Thing_to_do> queue)
    //**********************************************************
    {

        try {
            Thing_to_do thing_to_do = queue.take();
            logger.log("getting one thing to do : "+thing_to_do.type);

            switch (thing_to_do.type) {
                case die:
                    aborter.abort();
                    update_display();
                    return;
                case add_to_status:
                    add_to_status(thing_to_do.text);
                    break;
                case set_total_to_be_examined:
                    total_to_be_examined = Integer.valueOf(thing_to_do.text);
                    break;
                case increment_directory_examined:
                    count_directory_examined++;
                    break;
                case increment_examined:
                    count_examined++;
                    break;
                case increment_to_be_deleted:
                    count_to_be_deleted++;
                    break;
                case increment_deleted:
                    count_deleted++;
                    break;
            }
        }
        catch (InterruptedException e) {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
        }
        update_display();
    }


    //**********************************************************
    private void update_display()
    //**********************************************************
    {
        Platform.runLater(() -> {
            label_directory_examined.setText("Directories: " + count_directory_examined);
            label_total_to_be_examined.setText("Files to examine: " + total_to_be_examined);
            label_examined.setText("Examined file pairs: " + count_examined);//+" "+new_text_examined);
            if (dbg) logger.log("count_examined=" + count_examined);
            if (dbg) logger.log("total_to_be_examined=" + total_to_be_examined);
            progress_bar_examined.setProgress((double) count_examined / (double) total_to_be_examined);
            if (!just_count) {
                label_count_to_be_deleted.setText("Duplicates found: " + count_to_be_deleted);// + " " + new_text_to_be_deleted);
                label_count_deleted.setText("Duplicates deleted:" + count_deleted);// + " " + new_text_deleted);
                progress_bar_deleted.setProgress((double) count_deleted / (double) count_to_be_deleted);
            }
        });
    }


    //**********************************************************
    private void add_to_status(String text)
    //**********************************************************
    {
        Platform.runLater(() ->{
                if (aborter.should_abort()) return;
                label_status.setText(STATUS+ text);
            });
    }




    //**********************************************************
    public Deduplication_console_interface get_interface()
    //**********************************************************
    {
        return the_console;
    }

    //**********************************************************
    public void set_end_examined()
    //**********************************************************
    {
        Platform.runLater(() -> progress_bar_examined.setProgress(1.0));
    }
    //**********************************************************
    public void set_end_deleted()
    //**********************************************************
    {
        Platform.runLater(() -> { if ( !just_count) progress_bar_deleted.setProgress(1.0);});
    }
}
