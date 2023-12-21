package klik.util;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import klik.actor.Aborter;
import klik.files_and_paths.*;
import klik.look.Font_size;
import klik.look.my_i18n.I18n;
import klik.properties.Static_application_properties;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

//**********************************************************
public class Importer
//**********************************************************
{

    //**********************************************************
    public static void perform_import(Stage owner, Logger logger)
    //**********************************************************
    {
        Path home = (new File(System.getProperty(Static_application_properties.USER_HOME))).toPath();


        Path target = home.resolve(Path.of("Pictures"));
        //Path target = home.resolve(Path.of("Pictures/Photos Library.photoslibrary"));


        AtomicInteger counter = new AtomicInteger(0);
        Path new_dir = null;

        TextInputDialog dialog = new TextInputDialog(I18n.get_I18n_string("New_directory", logger));
        dialog.initOwner(owner);
        dialog.setWidth(800);
        dialog.setTitle(I18n.get_I18n_string("New_directory", logger));
        dialog.setHeaderText(I18n.get_I18n_string("Enter_name_of_new_directory", logger));
        dialog.setContentText(I18n.get_I18n_string("New_directory_name", logger));

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String new_name = result.get();
            for (int i = 0; i < 10; i++)
            {
                try {
                    new_dir = home.resolve(new_name);
                    Files.createDirectory(new_dir);
                    break;
                } catch (IOException e) {
                    logger.log(new_name + "new directory creation FAILED: " + e);
                    new_name += "_";
                }
            }
        }
        if ( new_dir == null)
        {
            logger.log("could not create new folder? ");
            return;
        }
        Path finalNew_dir = new_dir;

        logger.log("Importer: copy starting");

        File_payload file_payload = new File_payload() {
            @Override
            public void process_file(File f) {
                logger.log("Importer: looking at file: "+f.getName());
                if (!(FilenameUtils.getExtension(f.getName()).equals("jpeg")))
                {
                    logger.log("Importer: skipping at file: "+f.getName()+" wrong extension: "+FilenameUtils.getExtension(f.getName()));
                    return;
                }
                try
                {
                    Path new_path = Path.of(finalNew_dir.toAbsolutePath().toString(),f.getName());
                    Files.copy(f.toPath(), new_path, StandardCopyOption.COPY_ATTRIBUTES);
                    logger.log("Importer: copied file: "+f.getName()+" to: "+new_path.toAbsolutePath());
                    counter.incrementAndGet();
                } catch (IOException e)
                {
                    logger.log("copy failed: could not create new file for: " + f.getName() + ", Exception:" + e);
                    return;
                }
            }
        };

        AtomicBoolean done = new AtomicBoolean(false);
        Runnable r = new Runnable() {
            @Override
            public void run() {
                for(;;)
                {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    if ( done.get()) return;
                    logger.log("Importation: "+counter.get()+ " images copied");
                }
            }
        };
        Threads.execute(r,logger);

        Disk_scanner.process_folder(
                target,
                file_payload,
                null,
                new Aborter (),
                logger);

        done.set(true);

        logger.log("Importation finished: "+counter.get()+ " images copied");

    }

    //**********************************************************
    public static void estimate_size(Stage owner, Logger logger)
    //**********************************************************
    {
        Path home = (new File(System.getProperty(Static_application_properties.USER_HOME))).toPath();

        Path target = home.resolve(Path.of("Pictures"));

        Stage local_stage = new Stage();
        local_stage.initOwner(owner);
        local_stage.setX(200);
        local_stage.setY(200);
        local_stage.setHeight(400);
        local_stage.setWidth(800);
        TextArea textarea1 = new TextArea("Please wait, scanning folders...");
        TextArea textarea2 = new TextArea();
        Font_size.set_font_size(textarea1,24,logger);
        Font_size.set_font_size(textarea2,20,logger);
        VBox vbox = new VBox(textarea1, textarea2);
        Scene scene = new Scene(vbox, Color.WHITE);
        local_stage.setTitle(target.toAbsolutePath().toString());
        local_stage.setScene(scene);
        local_stage.show();
        local_stage.setAlwaysOnTop(true);



        AtomicLong size = new AtomicLong(0);

        logger.log("Importer: estimation starting");

        File_payload file_payload = new File_payload() {
            @Override
            public void process_file(File f) {
                //logger.log("Importer: looking at file: "+f.getName());
                if (!(FilenameUtils.getExtension(f.getName()).equals("jpeg")))
                {
                    //logger.log("Importer: skipping at file: "+f.getName()+" wrong extension: "+FilenameUtils.getExtension(f.getName()));
                    return;
                }
                size.addAndGet(f.length());
            }
        };

        AtomicBoolean done = new AtomicBoolean(false);
        /*
        Runnable r = new Runnable() {
            @Override
            public void run() {
                for(;;)
                {
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    if ( done.get())
                    {
                        return;
                    }
                    long val = size.get();
                    if ( last_print.get() +100_000_000 < val)
                    {
                        last_print.set(val);
                        logger.log("Apple Photos' importation Size ..."+val/1_000_000+" MBytes");
                    }
                }
            }
        };
        Threads.execute(r,logger);
*/
        Disk_scanner.process_folder(
                target,
                file_payload,
                null,
                new Aborter (),
                logger);

        done.set(true);



        // use a scheduled thread to track the process...
        // not sure a sleep would not be just as good?
        ScheduledFuture<?>[] progress_tracking_cancel = {null};
        final String[] progress_string = {"Please wait, scanning folders..."};
        Runnable progress_tracking = () -> {
            if (done.get())
            {
                logger.log("done!");
                progress_tracking_cancel[0].cancel(true);
                String s = "Importation size estimation: "+size.get()/1_000_000+" MBytes";
                Platform.runLater(() -> textarea1.setText(s));
                logger.log(s);
                return;
            }
            Platform.runLater(() -> textarea1.setText(progress_string[0]));
            progress_string[0] += "*";
            if (progress_string[0].length() > 100) progress_string[0] = "Please wait, scanning folders...";
        };

        progress_tracking_cancel[0] = Scheduled_thread_pool.execute(progress_tracking, 300, TimeUnit.MILLISECONDS);

    }
}
