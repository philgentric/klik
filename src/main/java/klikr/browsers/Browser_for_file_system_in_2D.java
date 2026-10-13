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
//SOURCES ../../experimental/backup/Backup_service.java
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
import klikr.Window_builder;
import klikr.browsers.browser_core.virtual_landscape.Scroll_position_cache;
import klikr.util.Kontext;
import klikr.util.execute.Guess_OS;
import klikr.util.execute.Operating_system;
import klikr.util.execute.actor.Actor_engine;
import klikr.browsers.browser_core.*;
import klikr.path_lists.Path_list_provider;
import klikr.change.Change_gang;
import klikr.settings.boolean_features.Feature;
import klikr.settings.boolean_features.Feature_cache;
import klikr.settings.boolean_features.Feature_change_target;
import klikr.change.file_system_monitoring.Filesystem_item_modification_watcher;
import klikr.change.old_and_new.Old_and_new_Path;
import klikr.util.execute.actor.Executor;
import klikr.util.execute.actor.My_Runnable;
import klikr.util.files_and_paths.Static_files_and_paths_utilities;
import klikr.util.log.Logger;
import klikr.util.ui.Jfx_batch_injector;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;


//**********************************************************
public class Browser_for_file_system_in_2D extends Abstract_browser implements Feature_change_target
//**********************************************************
{
    public final Path_list_provider path_list_provider;
    //**********************************************************
    public Browser_for_file_system_in_2D(Window_builder window_builder, Kontext k)
    //**********************************************************
    {
        super(window_builder, "klikr","Browser_for_file_system_in_2D",
                Color.WHITE, k);
        k.log("window_builder"+window_builder.to_string());
        path_list_provider = window_builder.path_list_provider;

        if( Feature_cache.get(Feature.Monitor_folders))
        {
            monitor_current_path_list_source();
        }
        Optional<Path> op = path_list_provider.get_folder_path();
        if ( op.isEmpty()) context.log_with_stack_trace("\n\n\n"+Logger.error+" FATAL)");
        if ( dbg)
        {
            op.ifPresent(path -> context.log("\n\n\n\n\n\nNEW BROWSER " + path));
        }
        init_base(this);
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
        context.log("replace_current_item not implemented for Browser_for_file_system_in_2D");
    }

    //**********************************************************
    @Override // Feature_change_target
    public void update_feature(Feature feature, boolean new_val)
    //**********************************************************
    {
        context.log("feature update received:"+feature+" new val:"+new_val);

        monitor_current_path_list_source();
    }

    //**********************************************************
    private static class Volume
    //**********************************************************
    {
        public String id;
        public String name;
        public String node;
        public String total;
        public String free;
        public Path path;

        public Volume(String line) {
            id = line;
        }

        public String to_string() {
            return "\n*************************" +
                    "\n   path:"+path+
                    "\n   id:"+ id +
                    "\n   node:"+ node +
                    "\n   name:"+name +
                    "\n   total:"+total+
                    "\n   free:"+free;
        }
    }

    //**********************************************************
    private void call_disk_util()
    //**********************************************************
    {
        // call diskutil
        ProcessBuilder pb = new ProcessBuilder("diskutil", "info", "-all");
        pb.redirectErrorStream(true);
        try {
            List<Volume> volumes = new ArrayList<>();
            Process p = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            //StringBuilder output = new StringBuilder();
            String line;
            String DEVICE_IDENTIFIER = "Device Identifier:";
            String VOLUME_NAME = "Volume Name:";
            String DEVICE_NODE = "Device Node:";
            String OS_ONLY = "Media OS Use Only:";
            String FREE_SPACE = "Container Free Space:";
            String TOTAL_SPACE = "Container Total Space:";
            String PATH = "Mount Point:";
            Volume vol = null;
            while ((line = reader.readLine()) != null) {
                if (line.contains(DEVICE_IDENTIFIER)) {
                    String local = line.substring(line.indexOf(DEVICE_IDENTIFIER) + DEVICE_IDENTIFIER.length());
                    vol = new Volume(local.strip());
                }
                if (line.contains(OS_ONLY)) {
                    vol = null;
                }
                if (vol == null) continue;
                if (line.contains(DEVICE_NODE)) {
                    vol.node = line.substring(line.indexOf(DEVICE_NODE) + DEVICE_NODE.length()).strip();
                }

                if (line.contains(FREE_SPACE)) {
                    vol.free = line.substring(line.indexOf(FREE_SPACE) + FREE_SPACE.length()).strip();
                }

                if (line.contains(TOTAL_SPACE)) {
                    vol.total = line.substring(line.indexOf(TOTAL_SPACE) + TOTAL_SPACE.length()).strip();
                }

                if (line.contains(PATH)) {
                    String local = line.substring(line.indexOf(PATH) + PATH.length()).strip();
                    vol.path = Path.of(local);
                }


                if (line.contains(VOLUME_NAME)) {
                    String local = line.substring(line.indexOf(VOLUME_NAME) + VOLUME_NAME.length()).strip();
                    if (!local.contains("Not applicable (no file system)")) {
                        //output.append(local).append("\n");
                        vol.name = local;
                        volumes.add(vol);
                    }
                }
            }
            p.waitFor();
            context.log("diskutil result: ");
            for (Volume v : volumes) {
                context.log(v.to_string());
            }
        } catch (IOException | InterruptedException e) {
            context.log_with_stack_trace_from_throwable(Logger.error, e);
        }
    }
    //**********************************************************
    @Override // Abstract_browser
    public void monitor_current_path_list_source()
    //**********************************************************
    {
        if (path_list_provider == null)
        {
            context.log_with_stack_trace(Logger.error+ "path_list_provider == null");
            return;
        }

        context.log("VVVVVVV   path_list_provider"+path_list_provider.get_key());
        Feature_cache.register_for(Feature.Monitor_folders,this);

        // ALWAYS monitor external drives
        Optional<Path> op = path_list_provider.get_folder_path();
        if (op.isEmpty())
        {
            context.log_with_stack_trace("no folders ?");
            return;
        }
        boolean monitor_this_folder = false;
        boolean is_volumes = Filesystem_item_modification_watcher.is_this_folder_showing_external_drives(op.get(), context.logger());
        if ( is_volumes) {
            context.log("folder contains volumes");
            monitor_this_folder = true;
            Iterable<FileStore> stores = FileSystems.getDefault().getFileStores();
            for (FileStore d : stores) {
                try {
                    long size = d.getUsableSpace();
                    String s = Static_files_and_paths_utilities.get_1_line_string_for_byte_data_size(size, context);

                    context.log(d.name() + " UsableSpace: " + s);
                } catch (IOException e) {
                    context.log_with_stack_trace_from_throwable(Logger.error, e);
                }
            }
        }


        Operating_system os = Guess_OS.guess(context.logger());
        switch (os) {
            case Linux -> {
                context.log("Linux");
            }
            case MacOS -> {
                context.log("MacOS");
                My_Runnable r = new My_Runnable() {
                    @Override
                    public void run() throws Exception {
                        call_disk_util();
                    }
                };
                Executor.execute(r,"disk util", context.logger());
            }
            case Windows -> {
                context.log("Windows");
            }
        }


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
                filesystem_item_modification_watcher = Filesystem_item_modification_watcher.monitor_folder(op.get(), FOLDER_MONITORING_TIMEOUT_IN_MINUTES, context);
                if (filesystem_item_modification_watcher == null)
                {
                    context.log(Logger.warning+" WARNING: cannot monitor folder " + op.get());
                }
                else
                {
                    context.log(Logger.ok+" Started monitoring folder " + op.get());

                }
            };
            Actor_engine.execute(r, "Monitor file system changes", context.logger());
        }
        else
        {
            if ( filesystem_item_modification_watcher != null)
            {
                context.log(Logger.ok+" Stopped monitoring folder " + op.get());
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

    /*
    //**********************************************************
    @Override // Abstract_browser
    public String get_path_for_history()
    //**********************************************************
    {
        if ( path_list_provider == null)
        {
            context.log_with_stack_trace(Logger.error+ "path_list_provider == null in get_path_for_history");
            return null;
        }
        if (path_list_provider.get_folder_path().isEmpty())
        {
            context.log_with_stack_trace(Logger.error+ "path_list_provider.get_folder_path().isEmpty() in get_path_for_history");
            return null;
        }
        return path_list_provider.get_folder_path().get().toString();
    }
*/
    //**********************************************************
    @Override // Abstract_browser
    public String get_name()
    //**********************************************************
    {
        if ( path_list_provider == null) return "should not happen";
        return "Browser_for_file_system_in_2D for "+ path_list_provider.get_key();
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
       context.setTitle(name);// fast temporary
        Runnable r = () -> {
            // can be super slow on network drives or slow drives
            // (e.g. USB)  ==> run in a thread
            int how_many_files = path_list_provider.how_many_files_and_folders(true,Feature_cache.get(Feature.Show_hidden_files), Feature_cache.get(Feature.Show_hidden_folders),context.aborter());
            String s = name + " :     " + (long) how_many_files + " files & folders";
            //virtual_landscape.set_status(s);
            if(virtual_landscape != null)
            {
                virtual_landscape.set_status(s);
            }
            else
            {
                if (dbg)        context.setTitle(name);// fast temporary
                context.log_with_stack_trace("set_status not done as virtual_landscape not ready yet");
            }
            Jfx_batch_injector.inject(() ->
            {
                context.setTitle(s);
            }, context);

        };
        Actor_engine.execute(r, "Compute and display how many files", context.logger());


    }


    //**********************************************************
    @Override // Change_receiver
    public void you_receive_this_because_a_file_event_occurred_somewhere(List<Old_and_new_Path> l, Kontext context)
    //**********************************************************
    {

        if ( virtual_landscape.change_events_off) return;
        //if (!my_Stage.the_Stage.isShowing())
        //{
        //    context.log("you_receive_this_because_a_file_event_occurred_somewhere event ignored");
        //    return;
        //}

        Optional<Path> op = path_list_provider.get_folder_path();
        if( op.isEmpty() )
        {
            context.log_with_stack_trace("");
            return;
        }

        context.log("Browser_for_file_system_in_2D for: "+op.get()+ ", CHANGE GANG CALL received");

        switch (Change_gang.is_my_directory_impacted(op.get(), l, context))
        {
            case more_changes: {
                //if (dbg)
                    context.log("1 Browser_for_file_system_in_2D of: " + op.get() + " RECOGNIZED change gang notification: " + l);

                for ( Old_and_new_Path oan : l)
                {
                    // the events of interest are ONLY the ones
                    // when a file is dropped in.
                    // if a file was moved away or deleted
                    // recording its new path would be a bad bug
                    if ( oan.new_Path != null)
                    {
                        if (oan.new_Path.startsWith(op.get()))
                        {
                            // make sure the window will scroll to the landing point of the displaced file
                            Scroll_position_cache.scroll_position_cache_write(path_list_provider.get_key(),oan.new_Path.toAbsolutePath().normalize().toString(),"Change_broadcaster Gang event received = new item in folder", context.logger());
                        }
                    }
                }
                context.log("redraw_fx due to change gang");
                virtual_landscape.redraw_fx(true,"change gang for dir: " + op.get(),true);
            }
            break;
            case one_new_file, one_file_gone: {
                if (dbg) context.log("CHANGE GANG received: Browser_for_file_system_in_2D of: " + op.get() + " RECOGNIZED change gang notification: " + l);
                context.log("redraw_fx due to change gang");
                virtual_landscape.redraw_fx(true,"change gang for dir: " + op.get(), true);
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
