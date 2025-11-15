// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ../../../util/files_and_paths/File_with_a_few_bytes.java
package klik.experimental.deduplicate.console;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import klik.util.execute.actor.Aborter;
import klik.experimental.deduplicate.Abortable;
import klik.util.files_and_paths.File_with_a_few_bytes;
import klik.util.files_and_paths.Guess_file_type;
import klik.look.Look_and_feel_manager;
import klik.util.ui.Jfx_batch_injector;
import klik.util.log.Logger;
import klik.util.execute.Scheduled_thread_pool;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;


//**********************************************************
public class Deduplication_console_window
//**********************************************************
{
    private static final boolean dbg = false;
    public static final String STATUS = "Status: ";
    Aborter private_aborter;
    //Browser browser;
    Window owner;
    public LongAdder count_directory_examined = new LongAdder();
    Label label_directory_examined;
    Label label_total_files_to_be_examined;
    Label label_total_pairs_to_be_examined;
    public LongAdder total_files_to_be_examined = new LongAdder();
    public LongAdder total_pairs_to_be_examined = new LongAdder();
    public LongAdder count_pairs_examined = new LongAdder();
    Label label_examined;
    public LongAdder count_duplicates = new LongAdder();
    Label label_count_to_be_deleted;
    public LongAdder  count_deleted = new LongAdder();
    Label label_count_deleted;
    ProgressBar progress_bar_examined;
    ProgressBar progress_bar_deleted;
    Label label_status;
    private final boolean just_count;

    Logger logger;

    Abortable abortable;

    //**********************************************************
    public Deduplication_console_window(
            Abortable abortable_,
            String title,
            double w, double h,
            boolean just_count_,
            Window owner,
            Aborter aborter_,
            Logger logger)
    //**********************************************************
    {
        this.owner = owner;
        abortable = abortable_;
        private_aborter = aborter_;
        just_count = just_count_;
        this.logger = logger;
        //the_console = new Deduplication_console_interface(this, logger);
        Stage stage = new Stage();
        stage.setHeight(h);
        stage.setWidth(w);

        stage.setTitle(title);
        VBox vbox = new VBox();
        Look_and_feel_manager.set_region_look(vbox,owner,logger);
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
        Look_and_feel_manager.set_button_look(cancel,true,owner,logger);
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
            Look_and_feel_manager.set_region_look(label_status,owner,logger);
            vbox.getChildren().add(label_status);
        }
        {
            label_directory_examined = new Label();
            Look_and_feel_manager.set_region_look(label_directory_examined,owner,logger);
            vbox.getChildren().add(label_directory_examined);
            label_total_files_to_be_examined= new Label();
            Look_and_feel_manager.set_region_look(label_total_files_to_be_examined,owner,logger);
            vbox.getChildren().add(label_total_files_to_be_examined);
            label_total_pairs_to_be_examined= new Label();
            Look_and_feel_manager.set_region_look(label_total_pairs_to_be_examined,owner,logger);
            vbox.getChildren().add(label_total_pairs_to_be_examined);
        }
        {
            label_examined = new Label();
            Look_and_feel_manager.set_region_look(label_examined,owner,logger);

            //label_examined.setWrapText(true);
            vbox.getChildren().add(label_examined);
            progress_bar_examined = new ProgressBar();
            progress_bar_examined.setPrefWidth(600);
            vbox.getChildren().add(progress_bar_examined);
        }
        if ( !just_count) {
            label_count_to_be_deleted = new Label();
            Look_and_feel_manager.set_region_look(label_count_to_be_deleted,owner,logger);

            //label_count_to_be_deleted.setWrapText(true);
            vbox.getChildren().add(label_count_to_be_deleted);
        }

        if ( !just_count)
        {
            label_count_deleted = new Label();
            Look_and_feel_manager.set_region_look(label_count_deleted,owner,logger);

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
        abortable.abort();
        private_aborter.abort("Deduplication_console_window::abort()");
    }

    //**********************************************************
    private void start_display_updating_event_pump()
    //**********************************************************
    {
        logger.log("starting thing to do thread");
        Runnable r = () -> {
            {
                if (private_aborter.should_abort()) return;
                refresh_UI();
            }
        };
        Scheduled_thread_pool.execute(r,200, TimeUnit.MILLISECONDS);
    }

    //**********************************************************
    private void refresh_UI()
    //**********************************************************
    {

        Runnable r = (() -> {
            label_directory_examined.setText("Directories: " + count_directory_examined.doubleValue());
            label_directory_examined.setText("Directories: " + count_directory_examined.doubleValue());
            label_total_files_to_be_examined.setText("Files to examine: " + total_files_to_be_examined.doubleValue());
            label_total_pairs_to_be_examined.setText("Pairs to examine: " + total_pairs_to_be_examined.doubleValue());
            label_examined.setText("Examined file pairs: " + count_pairs_examined.doubleValue());//+" "+new_text_examined);
            if (dbg) logger.log("count_pairs_examined=" + count_pairs_examined.doubleValue());
            if (dbg) logger.log("total_pairs_to_be_examined=" + total_pairs_to_be_examined.doubleValue());
            progress_bar_examined.setProgress((double) count_pairs_examined.doubleValue() / (double) total_pairs_to_be_examined.doubleValue());
            if (!just_count) {
                label_count_to_be_deleted.setText("Duplicates found: " + count_duplicates.doubleValue());// + " " + new_text_to_be_deleted);
                label_count_deleted.setText("Duplicates deleted:" + count_deleted.doubleValue());// + " " + new_text_deleted);
                progress_bar_deleted.setProgress((double) count_deleted.doubleValue() / (double) count_duplicates.doubleValue());
            }
        });

        Jfx_batch_injector.inject(r,logger);
    }




    //**********************************************************
    public void set_end_examined()
    //**********************************************************
    {
        Jfx_batch_injector.inject(() -> progress_bar_examined.setProgress(1.0),logger);
    }
    //**********************************************************
    public void set_end_deleted()
    //**********************************************************
    {
        Jfx_batch_injector.inject(() -> { if ( !just_count) progress_bar_deleted.setProgress(1.0);},logger);
    }

    //**********************************************************
    public void set_status_text(String status)
    //**********************************************************
    {
        Jfx_batch_injector.inject(() ->{

            if (private_aborter.should_abort()) return;
            label_status.setText(STATUS+ status);
        },logger);
    }

    //**********************************************************
    public static List<File_with_a_few_bytes> get_all_files_down(File cwd, Deduplication_console_window popup, boolean consider_also_hidden_files, Logger logger)
    //**********************************************************
    {
        List<File_with_a_few_bytes> returned = new ArrayList<>();
        File[] files = cwd.listFiles();
        if (files == null) return returned;
        for (File f : files) {
            if (f.isDirectory())
            {
                if (popup != null) popup.count_directory_examined.increment();
                returned.addAll(get_all_files_down(f, popup, consider_also_hidden_files, logger));
            }
            else
            {
                if (!consider_also_hidden_files) if (Guess_file_type.should_ignore(f.toPath(),popup.owner,logger)) continue;
                if (f.length() == 0) {
                    logger.log("WARNING: empty file found:" + f.getAbsolutePath());
                    continue;
                }
                File_with_a_few_bytes mf = new File_with_a_few_bytes(f, logger);
                returned.add(mf);
            }
        }
        return returned;
    }

}
