// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.util.execute;

import javafx.application.Platform;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import klikr.properties.File_storage_using_Properties;
import klikr.properties.String_constants;
import klikr.util.execute.actor.Aborter;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static klikr.properties.File_storage_using_Properties.AGE;

//**********************************************************
public class Registered_applications
//**********************************************************
{
    public static final String USER_CANCELLED = "USER_CANCELLED";
    private static final Map<String,String> map = new HashMap<>();

    public static final String REGISTERED_APPLICATIONS_FILENAME = "registered_applications.properties";
    private static File_storage_using_Properties storage;

    //**********************************************************
    public static String get_registered_application(String extension, Window owner, Aborter aborter,Logger logger)
    //**********************************************************
    {
        extension = extension.toLowerCase();
        load_map(owner,aborter,logger);

        String returned =  map.get(extension);
        if ( returned != null)
        {
            logger.log("Registered_applications.get_registered_application: found "+extension+" ==> "+returned);
            return returned;
        }
        logger.log("NO registered application found for extension ->"+extension+"<-");

        // ask the user
        LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();
        String finalExtension = extension;
        Platform.runLater(() -> {
            FileChooser file_chooser = new FileChooser();
            file_chooser.setTitle("Please select the application to open files with the extension " + finalExtension);
            Path home = Paths.get(System.getProperty(String_constants.USER_HOME));
            file_chooser.setInitialDirectory(home.toFile());
            File selected = file_chooser.showOpenDialog(owner);
            if (selected == null) {
                queue.add(USER_CANCELLED);
                return;
            }
            map.put(finalExtension, selected.getAbsolutePath());
            save_map(logger);
            queue.add(selected.getAbsolutePath());

        });

        // wait max 10 minutes
        try {
            String res = queue.poll(10, TimeUnit.MINUTES);
            if (res.equals(USER_CANCELLED)) return null;
            return res;
        } catch (InterruptedException e) {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
        }
        //Popups.popup_warning(owner,"Do not know how to open files with the extension "+extension,"To REGISTER what application to use, browse with klikr and use the right-click menu with register_application",false,logger);
        return null;
    }

    //**********************************************************
    private static void save_map(Logger logger)
    //**********************************************************
    {
        if ( storage == null)
        {
            logger.log("Registered_applications.save_map: properties_manager is null");
            return;
        }
        for (String key : map.keySet())
        {
            if ( key.endsWith(AGE)) continue; // skip age entries
            String value = map.get(key);
            storage.set(key,value);
            storage.save_to_disk();
            logger.log("Registered_applications.save_map: "+key+" "+value);
        }
    }

    //**********************************************************
    private static void load_map(Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        if ( storage == null)
        {
            storage = new File_storage_using_Properties("Registered applications DB",REGISTERED_APPLICATIONS_FILENAME,false,owner,aborter,logger);
        }
        for (String key : storage.get_all_keys())
        {
            String value = storage.get(key);
            map.put(key,value);
        }
    }


}
