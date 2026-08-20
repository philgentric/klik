// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.browser_core;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.Window;
import klikr.settings.String_constants;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Actor_engine;
import klikr.look.my_i18n.My_I18n;
import klikr.util.files_and_paths.Extensions;
import klikr.util.files_and_paths.disk_scanner.Dir_payload;
import klikr.util.files_and_paths.disk_scanner.Disk_scanner;
import klikr.util.files_and_paths.disk_scanner.File_payload;
import klikr.look.Look_and_feel_manager;
import klikr.util.log.Logger;
import klikr.util.ui.progress.Hourglass;
import klikr.util.ui.Jfx_batch_injector;
import klikr.util.ui.progress.Progress_window;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

//**********************************************************
public class Importer
//**********************************************************
{

    //**********************************************************
    public static void perform_import(Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        Path home = (new File(System.getProperty(String_constants.USER_HOME))).toPath();
        Path target = home.resolve(Path.of("Pictures"));
        //Path target = home.resolve(Path.of("Pictures/Photos Library.photoslibrary"));
        LongAdder counter = new LongAdder();
        Path new_dir = null;

        TextInputDialog dialog = new TextInputDialog(My_I18n.get_I18n_string("New_directory", owner,logger));
        Look_and_feel_manager.set_dialog_look(dialog,owner,logger);
        dialog.initOwner(owner);
        dialog.setWidth(800);
        dialog.setTitle(My_I18n.get_I18n_string("New_directory", owner,logger));
        dialog.setHeaderText(My_I18n.get_I18n_string("Enter_name_of_new_directory", owner,logger));
        dialog.setContentText(My_I18n.get_I18n_string("New_directory_name", owner,logger));

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

        File_payload file_payload = (f) -> {
            logger.log("Importer: looking at file: "+f.getName());
            if (!(Extensions.get_extension(f.getName()).equals("jpeg")))
            {
                logger.log("Importer: skipping at file: "+f.getName()+" wrong extension: "+Extensions.get_extension(f.getName()));
                return;
            }
            try
            {
                Path new_path = Path.of(finalNew_dir.toAbsolutePath().toString(),f.getName());
                Files.copy(f.toPath(), new_path, StandardCopyOption.COPY_ATTRIBUTES);
                logger.log("Importer: copied file: "+f.getName()+" to: "+new_path.toAbsolutePath());
                counter.increment();
            } catch (IOException e)
            {
                logger.log("copy failed: could not create new file for: " + f.getName() + ", Exception:" + e);
                return;
            }
        };

        AtomicBoolean done = new AtomicBoolean(false);
        Runnable r = () -> {
            for(;;)
            {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    logger.log(""+e);
                    return;
                }
                if ( done.get()) return;
                logger.log("Importation: "+counter.doubleValue()+ " images copied");
            }
        };
        Actor_engine.execute(r, "monitor picture importation", logger);

        ConcurrentLinkedQueue<String> warnings = new ConcurrentLinkedQueue<>();
        Disk_scanner.process_folder(
                target,
                "Photo importer",
                file_payload,
                null,
                warnings,
                aborter,
                logger);

        done.set(true);

        logger.log("Importation finished: "+counter.doubleValue()+ " images copied");
        for ( String s : warnings) logger.log(s);

    }

    //**********************************************************
    public static void estimate_size(Window owner,  Logger logger)
    //**********************************************************
    {
        Path home = (new File(System.getProperty(String_constants.USER_HOME))).toPath();
        Path target = home.resolve(Path.of("Pictures"));

        Stage local_stage = new Stage();
        local_stage.initOwner(owner);
        local_stage.setX(owner.getX()+200);
        local_stage.setY(owner.getY()+200);
        local_stage.setHeight(400);
        local_stage.setWidth(800);
        TextArea textarea1 = new TextArea("Please wait, scanning folders...");
        Look_and_feel_manager.set_region_look(textarea1,owner,logger);
        TextArea textarea2 = new TextArea();
        Look_and_feel_manager.set_region_look(textarea2,owner,logger);
        VBox vbox = new VBox(textarea1, textarea2);
        Scene scene = new Scene(vbox, Color.WHITE);
        local_stage.setTitle(target.toAbsolutePath().toString());
        local_stage.setScene(scene);
        local_stage.show();
        //local_stage.setAlwaysOnTop(true);



        LongAdder size = new LongAdder();

        logger.log("Importer: estimation starting");

        File_payload file_payload = new File_payload() {
            @Override
            public void process_file(File f)
            {
                logger.log("Importer: looking at file: "+f.getName());
                if ((Extensions.get_extension(f.getName()).equals("jpeg")))
                {
                    //logger.log("Importer: skipping at file: "+f.getName()+" wrong extension: "+Extensions.get_extension(f.getName()));
                    size.add(f.length());
                }
            }
        };

        AtomicBoolean done = new AtomicBoolean(false);

        ConcurrentLinkedQueue<String> wp = new ConcurrentLinkedQueue<>();

        Aborter local = new Aborter("estimate_size",logger);
        Hourglass hourglass = Progress_window.show_with_abort_button(
                local,
                "Wait, counting Apple Photo images",
                20000,
                owner,
                logger).orElse(null);

        Runnable r = new Runnable() {
            @Override
            public void run() {
                Disk_scanner.process_folder(
                        target,
                        "Photo importer length estimate",
                        file_payload,
                        new Dir_payload() {
                            @Override
                            public void process_dir(File dir) {
                                logger.log("Photo importer processing: " + dir.getAbsolutePath());
                            }
                        },
                        wp,
                        local,
                        logger);
                logger.log("Importer: setting done = true");

                done.set(true);
            }
        };
        Actor_engine.execute(r,"Estimate length for 1 folder",logger);

        Runnable monitor = new Runnable() {
            @Override
            public void run() {
                final String[] progress_string = {"Please wait, scanning folders..."};
                for(;;)
                {
                    if (local.should_abort()) break;
                    if ( done.get())
                    {
                        logger.log("Importer: done");
                        break;
                    }
                    try
                    {
                        Thread.sleep(100);
                    }
                    catch (InterruptedException e)
                    {
                        break;
                    }
                    Platform.runLater(() -> textarea1.setText(progress_string[0]));
                    progress_string[0] += "*";
                    if (progress_string[0].length() > 100) progress_string[0] = "Please wait, scanning folders...";
                }

                if ( hourglass != null) hourglass.close();


                {
                    logger.log("done!");
                    String s = "Importation length estimation: "+size.doubleValue()/1_000_000+" MBytes";
                    for ( String w : wp) s+="\n"+w;
                    String final_s = s;
                    Jfx_batch_injector.inject(() -> textarea1.setText(final_s),logger);
                    logger.log(s);
                }
            }
        };
        Actor_engine.execute(monitor,"Monitor length estimation",logger);



    }
}
