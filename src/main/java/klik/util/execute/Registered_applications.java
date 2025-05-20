package klik.util.execute;

import javafx.stage.Window;
import klik.actor.Aborter;
import klik.properties.Non_zooleans;
import klik.properties.Properties_manager;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;

import javax.swing.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

//**********************************************************
public class Registered_applications
//**********************************************************
{
    public static final String USER_CANCELLED = "USER_CANCELLED";
    private static final Map<String,String> map = new HashMap<String,String>();

    public static final String REGISTERED_APPLICATIONS_FILENAME = "registered_applications.properties";
    private static Properties_manager properties_manager;

    //**********************************************************
    public static String get_registered_application(String extension, Window owner, Aborter aborter,Logger logger)
    //**********************************************************
    {
        extension = extension.toLowerCase();
        load_map(aborter,logger);

        String returned =  map.get(extension);
        if ( returned != null)
        {
            logger.log("Registered_applications.get_registered_application: found "+extension+" ==> "+returned);
            return returned;
        }
        // ask the user
        LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();
        // JFileChooser will work "only once" if it is not called on the Swing thread
        String finalExtension = extension;
        SwingUtilities.invokeLater(() -> {
                    JFileChooser app_chooser = new JFileChooser();
                    app_chooser.setDialogTitle("Please select the application to open files with the extension " + finalExtension);
                    app_chooser.setFileHidingEnabled(false);
                    app_chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                    Path home = Paths.get(System.getProperty(Non_zooleans.USER_HOME));
                    app_chooser.setCurrentDirectory(home.toFile());
                    int status = app_chooser.showOpenDialog(null);
                    if (status == JFileChooser.APPROVE_OPTION) {
                        map.put(finalExtension, app_chooser.getSelectedFile().getAbsolutePath());
                        save_map(logger);
                        queue.add(app_chooser.getSelectedFile().getAbsolutePath());
                    }
                    queue.add(USER_CANCELLED);
        });

        // wait max 10 minutes
        try {
            String res = queue.poll(10, TimeUnit.MINUTES);
            if (res.equals(USER_CANCELLED)) return null;
            return res;
        } catch (InterruptedException e) {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
        }
        //Popups.popup_warning(owner,"Do not know how to open files with the extension "+extension,"To REGISTER what application to use, browse with klik and use the right-click menu with register_application",false,logger);
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
            String value = map.get(key);
            properties_manager.add(key,value);
            logger.log("Registered_applications.save_map: "+key+" "+value);
        }
    }

    //**********************************************************
    private static void load_map(Aborter aborter, Logger logger)
    //**********************************************************
    {
        if ( properties_manager == null)
        {
            String home = System.getProperty(Non_zooleans.USER_HOME);
            Path p = Paths.get(home, Non_zooleans.CONF_DIR, REGISTERED_APPLICATIONS_FILENAME);
            properties_manager = new Properties_manager(p,"Registered applications DB",aborter,logger);
        }
        for (String key : properties_manager.get_all_keys())
        {
            String value = properties_manager.get(key);
            map.put(key,value);
        }
    }


}
