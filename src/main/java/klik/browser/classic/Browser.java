//SOURCES ../actor/Actor_engine.java
//SOURCES ../actor/Aborter.java
//SOURCES ../util/ui/Show_running_film_frame.java
//SOURCES ../util/ui/Popups.java
//SOURCES ../util/log/Stack_trace_getter.java
//SOURCES ../util/files_and_paths/Old_and_new_Path.java
//SOURCES ../util/files_and_paths/Filesystem_item_modification_watcher.java
//SOURCES ../util/files_and_paths/Guess_file_type.java
//SOURCES ../util/files_and_paths/Ding.java
//SOURCES ../change/Change_gang.java
//SOURCES ../change/Change_receiver.java
//SOURCES ../change/history/History_engine.java
//SOURCES ../unstable/backup/Backup_singleton.java
//SOURCES ../unstable/fusk/Fusk_bytes.java
//SOURCES ../unstable/fusk/Fusk_singleton.java
//SOURCES ../unstable/fusk/Static_fusk_paths.java
//SOURCES ../look/Look_and_feel_manager.java
//SOURCES ../look/my_i18n/My_I18n.java
//SOURCES ../look/Font_size.java
//SOURCES ../look/Look_and_feel_manager.java
//SOURCES ../look/my_i18n/My_I18n.java
//SOURCES ../look/Jar_utils.java

//SOURCES ./items/Item_image.java
//SOURCES ./items/Item.java

//SOURCES ../properties/Non_booleans.java
//SOURCES ../properties/Non_booleans.java
//SOURCES ./icons/image_properties_cache/Image_properties_RAM_cache.java
//SOURCES ./icons/Refresh_target.java
//SOURCES ./icons/Icon_factory_actor.java
//SOURCES ./icons/Paths_holder.java
//SOURCES ./locator/Folders_with_large_images_locator.java
//SOURCES ../images/decoding/Fast_date_from_OS.java
//SOURCES ./Virtual_landscape_UI.java
//SOURCES ./Scan_show.java
//SOURCES ./External_close_event_handler.java
//SOURCES ./Static_backup_paths.java
//SOURCES ./Error_receiver.java
//SOURCES ./Scan_show_slave.java
//SOURCES ./Selection_reporter.java
//SOURCES ./Selection_handler.java
//SOURCES ./Importer.java
//SOURCES ./Get_folder_files.java

package klik.browser.classic;

import klik.actor.Actor_engine;
import klik.browser.*;
import klik.browser.virtual_landscape.Browsing_caches;
import klik.browser.virtual_landscape.Path_list_provider;
import klik.browser.virtual_landscape.Virtual_landscape;
import klik.change.Change_gang;
import klik.properties.Booleans;
import klik.properties.Non_booleans;
import klik.util.files_and_paths.Filesystem_item_modification_watcher;
import klik.util.files_and_paths.Old_and_new_Path;
import klik.util.log.Logger;
import klik.util.ui.Jfx_batch_injector;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;


//**********************************************************
public class Browser extends Abstract_browser
//**********************************************************
{
    public final Path_list_provider path_list_provider;
    //**********************************************************
    public Browser(New_window_context context, Logger logger_)
    //**********************************************************
    {
        super(logger_);
        Path path;
        if ( context.target_path == null)
        {
            path = Paths.get(System.getProperty(Non_booleans.USER_HOME));
        }
        else
        {
            path = context.target_path;
        }
        path_list_provider = new Folder_path_list_provider(path);
        init(context,this,"klik");

        //if ( dbg)
            logger.log("\n\n\n\n\n\n\n\n\n\n\nNEW BROWSER "+path_list_provider.get_folder_path());

    }
    //**********************************************************
    @Override // Abstract_browser
    public void monitor()
    //**********************************************************
    {
        monitor_folder();
    }

    //**********************************************************
    @Override
    public Path_list_provider get_Path_list_provider()
    //**********************************************************
    {
        return path_list_provider;
    }

    //**********************************************************
    @Override // Abstract_browser
    public String get_name()
    //**********************************************************
    {
        if ( path_list_provider == null) return "should not happen";
        return path_list_provider.get_name();
    }


    //**********************************************************
    private void monitor_folder()
    //**********************************************************
    {
        boolean monitor_this_folder = false;

        // ALWAYS monitor external drives
        monitor_this_folder = Filesystem_item_modification_watcher.is_this_folder_showing_external_drives(path_list_provider.get_folder_path(), logger);

        if (!monitor_this_folder) {
            if (Booleans.get_boolean_defaults_to_true(Booleans.MONITOR_BROWSED_FOLDERS)) {
                monitor_this_folder = true;
            }
        }

        if (monitor_this_folder) {
            Runnable r = () -> {
                filesystem_item_modification_watcher = Filesystem_item_modification_watcher.monitor_folder(path_list_provider.get_folder_path(), FOLDER_MONITORING_TIMEOUT_IN_MINUTES, Shared_services.shared_services_aborter, logger);
                if (filesystem_item_modification_watcher == null) {
                    logger.log("WARNING: cannot monitor folder " + path_list_provider.get_folder_path());
                }
            };
            Actor_engine.execute(r, logger);
        }
    }



    //**********************************************************
    @Override // Abstract_browser
    public String signature()
    //**********************************************************
    {
        return "  Browser ID= " + abstract_browser_ID + " total window count: " + number_of_windows.get() + " esc=" + my_Stage.escape;
    }

    //**********************************************************
    @Override // Title_target
    public void set_title()
    //**********************************************************
    {
        if (path_list_provider == null) return;
        String name = path_list_provider.get_folder_path().toAbsolutePath().toString();
        my_Stage.the_Stage.setTitle(name);// fast temporary
        Runnable r = () -> {
            // can be super slow on network drives or slow drives
            // (e.g. USB)  ==> run in a thread
            int how_many_files = path_list_provider.how_many_files_and_folders(Virtual_landscape.show_hidden_files, Virtual_landscape.show_hidden_folders);

            Jfx_batch_injector.inject(() -> my_Stage.the_Stage.setTitle(name + " :     " + (long) how_many_files + " files & folders"), logger);

        };
        Actor_engine.execute(r, logger);


    }


    //**********************************************************
    @Override // Change_receiver
    public void you_receive_this_because_a_file_event_occurred_somewhere(List<Old_and_new_Path> l, Logger logger)
    //**********************************************************
    {

        if (!my_Stage.the_Stage.isShowing()) {
            logger.log("you_receive_this_because_a_file_event_occurred_somewhere event ignored");
            return;
        }

        logger.log("Browser for: "+path_list_provider.get_folder_path()+ ", CHANGE GANG CALL received");

        switch (Change_gang.is_my_directory_impacted(path_list_provider.get_folder_path(), l, logger))
        {
            case more_changes: {
                //if (dbg)
                    logger.log("1 Browser of: " + path_list_provider.get_folder_path() + " RECOGNIZED change gang notification: " + l);

                for ( Old_and_new_Path oan : l)
                {
                    // the events of interest are ONLY the ones
                    // when a file is dropped in.
                    // if a file was moved away or deleted
                    // recording its new path would be a bad bug
                    if ( oan.new_Path != null) {
                        if (oan.new_Path.startsWith(path_list_provider.get_folder_path())) {
                            // make sure the window will scroll to the landing point of the displaced file
                            Browsing_caches.scroll_position_cache.put(path_list_provider.get_folder_path().toAbsolutePath().toString(), oan.new_Path);
                        }
                    }
                }
                virtual_landscape.redraw_fx("change gang for dir: " + path_list_provider.get_folder_path());
            }
            ;
            break;
            case one_new_file, one_file_gone: {
                //if (dbg)
                    logger.log("2 Browser of: " + path_list_provider.get_folder_path() + " RECOGNIZED change gang notification: " + l);
                virtual_landscape.redraw_fx("change gang for dir: " + path_list_provider.get_folder_path());
            }
            break;
            default:
                break;
        }
    }


    //**********************************************************
    @Override // Change_receiver
    public String get_Change_receiver_string()
    //**********************************************************
    {
        return "Browser:" + path_list_provider.get_folder_path().toAbsolutePath() + " " + abstract_browser_ID;
    }

}
