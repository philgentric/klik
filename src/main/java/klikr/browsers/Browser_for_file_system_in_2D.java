// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ../../actor/Actor_engine.java
//SOURCES ../../actor/Aborter.java
//SOURCES ../../util/ui/Progress_window.java
//SOURCES ../../util/ui/Popups.java
//SOURCES ../../util/log/Stack_trace_getter.java
//SOURCES ../../util/files_and_paths/Old_and_new_Path.java
//SOURCES ../../util/files_and_paths/Filesystem_item_modification_watcher.java
//SOURCES ../../util/files_and_paths/Guess_file_type.java
//SOURCES ../../util/files_and_paths/Ding.java
//SOURCES ../../change/Change_gang.java
//SOURCES ../../change/Change_receiver.java
//SOURCES ../../change/history/History_engine.java
//SOURCES ../../experimental/backup/Backup_singleton.java
//SOURCES ../../experimental/fusk/Fusk_bytes.java
//SOURCES ../../experimental/fusk/Fusk_singleton.java
//SOURCES ../../experimental/fusk/Static_fusk_paths.java
//SOURCES ../../look/Look_and_feel_manager.java
//SOURCES ../../look/my_i18n/My_I18n.java
//SOURCES ../../look/Font_size.java
//SOURCES ../../look/Look_and_feel_manager.java
//SOURCES ../../look/my_i18n/My_I18n.java
//SOURCES ../../look/Jar_utils.java

//SOURCES ./../items/Item_file_with_icon.java
//SOURCES ./../items/Item.java

//SOURCES ../../properties/Non_booleans_properties.java
//SOURCES ../../properties/boolean_features/Feature.java
//SOURCES ../../properties/boolean_features/Feature_cache.java
//SOURCES ../../properties/boolean_features/Feature_change_target.java
//SOURCES ./../icons/image_properties_cache/Image_properties_cache.java
//SOURCES ./../icons/Refresh_target.java
//SOURCES ./../icons/Icon_factory_actor.java
//SOURCES ./../virtual_landscape/Paths_holder.java
//SOURCES ./../locator/Folders_with_large_images_locator.java
//SOURCES ../../images/decoding/Fast_date_from_filesystem.java
//SOURCES ./../virtual_landscape/Virtual_landscape.java
//SOURCES ./../virtual_landscape/Scan_show.java
//SOURCES ./../Escape_keyboard_handler.java
//SOURCES ./../Static_backup_paths.java
//SOURCES ./../Error_receiver.java
//SOURCES ./../virtual_landscape/Scan_show_slave.java
//SOURCES ./../virtual_landscape/Selection_reporter.java
//SOURCES ./../virtual_landscape/Selection_handler.java
//SOURCES ./../Importer.java
//SOURCES ./../virtual_landscape/Browsing_caches.java
//SOURCES ./../virtual_landscape/Path_list_provider.java
//SOURCES ./../Abstract_browser.java

package klikr.browsers;

import javafx.scene.paint.Color;
import javafx.stage.Window;
import klikr.Window_builder;
import klikr.browser_core.virtual_landscape.Scroll_position_cache;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Actor_engine;
import klikr.browser_core.*;
import klikr.path_lists.Path_list_provider;
import klikr.change.Change_gang;
import klikr.settings.boolean_features.Feature;
import klikr.settings.boolean_features.Feature_cache;
import klikr.settings.boolean_features.Feature_change_target;
import klikr.change.file_system_monitoring.Filesystem_item_modification_watcher;
import klikr.change.old_and_new.Old_and_new_Path;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;
import klikr.util.ui.Jfx_batch_injector;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;


//**********************************************************
public class Browser_for_file_system_in_2D extends Abstract_browser implements Feature_change_target
//**********************************************************
{
    public final Path_list_provider path_list_provider;
    //**********************************************************
    public Browser_for_file_system_in_2D(Window_builder window_builder, Logger logger_)
    //**********************************************************
    {
        super(Color.WHITE, logger_);
        path_list_provider = window_builder.path_list_provider;
        if ( dbg) logger.log("\n\n\n\n\n\nNEW BROWSER "+path_list_provider.get_folder_path());

        aborter = new Aborter("Browser_for_file_system_in_2D_"+window_builder.path_list_provider.get_key(),logger_);
        init_abstract_browser(
                window_builder,
                this, "klikr", aborter);


    }

    //*******************************************************
    @Override
    public Comparator<? super Path> get_file_comparator()
    //*******************************************************
    {
        return virtual_landscape.other_file_comparator;
    }

    //*******************************************************
    @Override // Owner_provider
    public void set_unique_selected_item(Path path)
    //*******************************************************
    {
        // todo
        logger.log("replace_current_item not implemented for Browser_for_file_system_in_2D");
    }

    //**********************************************************
    @Override // Feature_change_target
    public void update_feature(Feature feature, boolean new_val)
    //**********************************************************
    {
        logger.log("feature update received:"+feature+" new val:"+new_val);

        monitor_current_path_list_source();
    }

    //**********************************************************
    @Override // Abstract_browser
    public void monitor_current_path_list_source()
    //**********************************************************
    {
        Feature_cache.register_for(Feature.Monitor_folders,this);
        boolean monitor_this_folder = false;

        // ALWAYS monitor external drives
        Optional<Path> folder_path = path_list_provider.get_folder_path();
        if (folder_path.isEmpty())
        {
            logger.log(Stack_trace_getter.get_stack_trace(""));
            return;
        }
        monitor_this_folder = Filesystem_item_modification_watcher.is_this_folder_showing_external_drives(folder_path.get(), logger);


        if (!monitor_this_folder)
        {
            if (Feature_cache.get(Feature.Monitor_folders))
            {
                monitor_this_folder = true;
            }
        }

        if (monitor_this_folder)
        {
            Runnable r = () -> {
                filesystem_item_modification_watcher = Filesystem_item_modification_watcher.monitor_folder(folder_path.get(), FOLDER_MONITORING_TIMEOUT_IN_MINUTES, my_Stage.the_Stage, aborter, logger);
                if (filesystem_item_modification_watcher == null)
                {
                    logger.log("❗ WARNING: cannot monitor folder " + folder_path);
                }
                else
                {
                    logger.log("✅ Started monitoring folder " + folder_path);

                }
            };
            Actor_engine.execute(r, "Monitor file system changes", logger);
        }
        else
        {
            if ( filesystem_item_modification_watcher != null)
            {
                logger.log("✅ Stopped monitoring folder " + folder_path);
                filesystem_item_modification_watcher.cancel();
            }
        }
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
    public String get_path_for_history()
    //**********************************************************
    {
        if ( path_list_provider == null) return null;
        if ( path_list_provider.get_folder_path() == null) return null;
        return path_list_provider.get_folder_path().toString();
    }

    //**********************************************************
    @Override // Abstract_browser
    public String get_name()
    //**********************************************************
    {
        if ( path_list_provider == null) return "should not happen";
        return path_list_provider.get_key();
    }




    //**********************************************************
    @Override // Abstract_browser
    public String signature()
    //**********************************************************
    {
        return "  Browser_for_file_system_in_2D ID= " + ID + " total window count: " + Window_manager.how_many_windows() + " esc=" + my_Stage.escape;
    }

    //**********************************************************
    @Override // Title_target
    public void set_title()
    //**********************************************************
    {
        if (path_list_provider == null) return;
        String name = path_list_provider.get_key();
        my_Stage.the_Stage.setTitle(name);// fast temporary
        Runnable r = () -> {
            // can be super slow on network drives or slow drives
            // (e.g. USB)  ==> run in a thread
            int how_many_files = path_list_provider.how_many_files_and_folders(false,Feature_cache.get(Feature.Show_hidden_files), Feature_cache.get(Feature.Show_hidden_folders),aborter);

            Jfx_batch_injector.inject(() -> my_Stage.the_Stage.setTitle(name + " :     " + (long) how_many_files + " files & folders"), logger);

        };
        Actor_engine.execute(r, "Compute and display how many files", logger);


    }


    //**********************************************************
    @Override // Change_receiver
    public void you_receive_this_because_a_file_event_occurred_somewhere(List<Old_and_new_Path> l, Window owner, Logger logger)
    //**********************************************************
    {

        if ( virtual_landscape.change_events_off) return;
        //if (!my_Stage.the_Stage.isShowing())
        //{
        //    logger.log("you_receive_this_because_a_file_event_occurred_somewhere event ignored");
        //    return;
        //}

        Optional<Path> folder_path = path_list_provider.get_folder_path();
        if( folder_path.isEmpty() )
        {
            logger.log(Stack_trace_getter.get_stack_trace(""));
            return;
        }

        logger.log("Browser_for_file_system_in_2D for: "+folder_path+ ", CHANGE GANG CALL received");

        switch (Change_gang.is_my_directory_impacted(folder_path.get(), l, logger))
        {
            case more_changes: {
                //if (dbg)
                    logger.log("1 Browser_for_file_system_in_2D of: " + folder_path + " RECOGNIZED change gang notification: " + l);

                for ( Old_and_new_Path oan : l)
                {
                    // the events of interest are ONLY the ones
                    // when a file is dropped in.
                    // if a file was moved away or deleted
                    // recording its new path would be a bad bug
                    if ( oan.new_Path != null)
                    {
                        if (oan.new_Path.startsWith(folder_path.get()))
                        {
                            // make sure the window will scroll to the landing point of the displaced file
                            Scroll_position_cache.scroll_position_cache_write(path_list_provider.get_key(),oan.new_Path.toAbsolutePath().normalize().toString(),"Change_broadcaster Gang event received = new item in folder",logger);
                        }
                    }
                }
                logger.log("redraw_fx due to change gang");
                virtual_landscape.redraw_fx(true,"change gang for dir: " + folder_path,true);
            }
            break;
            case one_new_file, one_file_gone: {
                if (dbg) logger.log("CHANGE GANG received: Browser_for_file_system_in_2D of: " + folder_path + " RECOGNIZED change gang notification: " + l);
                logger.log("redraw_fx due to change gang");
                virtual_landscape.redraw_fx(true,"change gang for dir: " + folder_path, true);
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
        Optional<Path> folder_path = path_list_provider.get_folder_path();
        if ( folder_path.isEmpty()) return "Browser_for_file_system_in_2D NO PATH ?";
        return "Browser_for_file_system_in_2D:" + folder_path.get().toAbsolutePath() + " " + ID;
    }


}
