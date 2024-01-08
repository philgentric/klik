package klik.util;

import javafx.application.Platform;
import klik.actor.Aborter;
import klik.browser.Browser;
import klik.browser.Browser_creation_context;
import klik.files_and_paths.*;
import klik.properties.Static_application_properties;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

public class Locator {

    public static void locate_photos(Path target, int minimum_image_count_to_show_a_folder, Browser browser, Logger logger)
    {
        //Path home = (new File(System.getProperty(Static_application_properties.USER_HOME))).toPath();


        ConcurrentHashMap<Path,Integer> found = new ConcurrentHashMap<>();

        Runnable r = new Runnable() {
            @Override
            public void run() {
                explore(target.toFile(),minimum_image_count_to_show_a_folder,browser.aborter,found,logger);

                logger.log("going to show ");
                show(target.toFile(),browser.aborter,found,logger);

            }


        };
        Threads.execute(r,logger);
    }
    private static void explore(File dir, int minimum_image_count_to_show_a_folder, Aborter aborter, ConcurrentHashMap<Path, Integer> found, Logger logger) {


        //logger.log("Locator looking at folder: "+dir);
        if (found.get(dir.toPath()) !=null)
        {
            logger.log("Locator looking at folder: "+dir+" already done");
            return;
        }

        File[] all_files = dir.listFiles();
        if ( all_files == null)
        {
            return ;
        }
        int count_images = 0;
        for (File f : all_files)
        {
            if ( aborter.should_abort()) return;

            if ( !f.isFile()) continue;
            if (Guess_file_type.is_file_a_image(f))
            {
                count_images++;
                if (count_images > minimum_image_count_to_show_a_folder)
                {
                    Path parent = dir.toPath().getParent();
                    found.put(parent, minimum_image_count_to_show_a_folder);
                    found.put(dir.toPath(), minimum_image_count_to_show_a_folder);

                    logger.log("images found in: "+dir);
                    return;
                }
            }
        }
        if ( aborter.should_abort()) return;

        for (File f : all_files)
        {
            if ( f.isDirectory()) explore(f,minimum_image_count_to_show_a_folder,aborter, found, logger);
            if ( aborter.should_abort()) return;
        }

    }

    private static void show(File dir, Aborter aborter, ConcurrentHashMap<Path, Integer> found, Logger logger) {


        //logger.log("Locator looking at folder: "+dir);
        if (found.get(dir.toPath()) != null)
        {
            logger.log("Locator looking at folder: "+dir+" ignored");
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    Browser_creation_context.additional_no_past(dir.toPath(), logger);
                }
            };
            Platform.runLater(r);
            return;
        }

        if ( aborter.should_abort()) return;

        File[] all_files = dir.listFiles();
        if ( all_files == null)
        {
            return ;
        }
        for (File f : all_files)
        {
            if ( f.isDirectory()) show(f,aborter, found, logger);
            if ( aborter.should_abort()) return;
        }

    }
}
