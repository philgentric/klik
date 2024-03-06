package klik.files_and_paths;

import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.browser.Browser;
import klik.browser.items.Item_button;
import klik.look.Font_size;
import klik.look.my_i18n.I18n;
import klik.util.Logger;
import klik.util.execute.Scheduled_thread_pool;

import java.nio.file.Path;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Folder_size {


    private static final double size_stage_x_start = 100;
    private static final double size_stage_y_start = 100;
    private static final double size_stage_height = 600;
    private static final double size_stage_width = 1000;
    private static double size_stage_x = size_stage_x_start;
    private static double size_stage_y = size_stage_y_start;
    public static final String STAR = "*";
    public static final String PLEASE_WAIT_SCANNING_FOLDERS = "Please wait, scanning folders "+ STAR + STAR;


    //**********************************************************
    public static void get_folder_size(Path path, Browser browser, Aborter aborter, Logger logger)
    //**********************************************************
    {
        // open a window to display what is going on and the final result
        Stage local_stage = new Stage();
        local_stage.initOwner(browser.my_Stage.the_Stage);
        local_stage.setX(size_stage_x);
        local_stage.setY(size_stage_y);
        size_stage_y += size_stage_height;
        if ( size_stage_y > 1000)
        {
            size_stage_y = size_stage_y_start;
            size_stage_x += 100;
            if ( size_stage_x > 1000) size_stage_x = size_stage_x_start;
        }

        local_stage.setHeight(size_stage_height);
        local_stage.setWidth(size_stage_width);
        VBox vbox = new VBox();
        TextArea textarea1 = new TextArea("Please wait, scanning folders...");
        vbox.getChildren().add(textarea1);
        TextArea textarea2 = new TextArea();
        vbox.getChildren().add(textarea2);
        textarea2.setMinHeight(400);
        Font_size.apply_font_size(textarea1,24,logger);
        Font_size.apply_font_size(textarea2,20,logger);
        Scene scene = new Scene(vbox, Color.WHITE);
        local_stage.setTitle(path.toAbsolutePath().toString());
        local_stage.setScene(scene);
        local_stage.show();
        local_stage.setAlwaysOnTop(true);
        final boolean[] done = {false};

        Aborter local = new Aborter("get_folder_size",logger);
        local_stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent windowEvent) {
                local.abort();
            }
        });
        Runnable r = () -> {
            Sizes sizes = Files_and_Paths.get_sizes_on_disk_deep(path,local, logger);

            Platform.runLater(() -> {
                String size_on_disk = I18n.get_I18n_string("Size_on_disk",logger);
                String bytes = Files_and_Paths.get_1_line_string_for_byte_data_size(sizes.bytes());
                {
                    String display_text;
                    if (sizes.bytes() < 0)
                    {
                        display_text = path+ "\nAn error occurred, probably Access Denied, check the logs";
                    } else {
                        display_text = path+"\n"+size_on_disk + " " + bytes;
                    }
                    textarea1.setText(display_text);
                    if (Item_button.dbg)  logger.log(display_text);

                    String folders_s = I18n.get_I18n_string("Folders", logger);
                    String files_s = I18n.get_I18n_string("Files", logger);
                    String image_s = I18n.get_I18n_string("Images", logger);
                    String ww ="\n";
                    for ( String w : sizes.warnings()) ww += w+"\n";
                    textarea2.setText(folders_s+": "+ sizes.folders() + "\n"+files_s+": " + sizes.files() + "\n" + image_s+": "+sizes.images()+ww);
                    browser.set_status(path + " :  " + sizes.folders() + " " + folders_s + " , " + sizes.files() + " " + files_s + " , " + sizes.images() + " " + image_s+" "+bytes);
                }
            });
            done[0] =  true;
        };
        Actor_engine.execute(r,aborter, logger);

        // use a scheduled thread to track the process...
        // not sure a sleep would not be just as good?
        ScheduledFuture<?>[] progress_tracking_cancel = {null};
        final String[] progress_string = {PLEASE_WAIT_SCANNING_FOLDERS};
        Runnable progress_tracking = () -> {
            if (done[0])
            {
                logger.log("done!");
                progress_tracking_cancel[0].cancel(true);
                return;
            }
            if (aborter.should_abort()) local.abort();
            Platform.runLater(() -> textarea1.setText(progress_string[0]));
            progress_string[0] += STAR;
            if (progress_string[0].length() > 100) progress_string[0] = PLEASE_WAIT_SCANNING_FOLDERS;
        };

        progress_tracking_cancel[0] = Scheduled_thread_pool.execute(progress_tracking, 300, TimeUnit.MILLISECONDS);

    }

}
