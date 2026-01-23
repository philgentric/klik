// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.util.execute;

import javafx.application.Platform;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import klikr.properties.String_constants;
import klikr.util.execute.actor.Aborter;
import klikr.properties.Non_booleans_properties;
import klikr.properties.Properties_manager;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static klikr.properties.Properties_manager.AGE;

//**********************************************************
public class Registered_applications
//**********************************************************
{
    public static final String USER_CANCELLED = "USER_CANCELLED";
    private static final Map<String,String> map = new HashMap<>();

    public static final String REGISTERED_APPLICATIONS_FILENAME = "registered_applications.properties";
    private static Properties_manager properties_manager;

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
        if ( properties_manager == null)
        {
            logger.log("Registered_applications.save_map: properties_manager is null");
            return;
        }
        for (String key : map.keySet())
        {
            if ( key.endsWith(AGE)) continue; // skip age entries
            String value = map.get(key);
            properties_manager.add(key,value, true);
            logger.log("Registered_applications.save_map: "+key+" "+value);
        }
    }

    //**********************************************************
    private static void load_map(Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        if ( properties_manager == null)
        {
            String home = System.getProperty(String_constants.USER_HOME);
            Path p = Paths.get(home, String_constants.CONF_DIR, REGISTERED_APPLICATIONS_FILENAME);
            properties_manager = new Properties_manager(p,"Registered applications DB",owner,aborter,logger);
        }
        for (String key : properties_manager.get_all_keys())
        {
            String value = properties_manager.get(key);
            map.put(key,value);
        }
    }


}
