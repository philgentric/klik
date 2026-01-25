// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.util.disk_cache_auto_clean;

import javafx.stage.Window;
import klikr.util.Shared_services;
import klikr.look.my_i18n.My_I18n;
import klikr.properties.boolean_features.Feature;
import klikr.properties.Non_booleans_properties;
import klikr.properties.boolean_features.Booleans;
import klikr.util.cache.Cache_folder;
import klikr.util.files_and_paths.Static_files_and_paths_utilities;
import klikr.util.log.Logger;
import klikr.util.ui.Popups;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

//**********************************************************
public class Disk_usage_monitor
//**********************************************************
{
    private static final boolean dbg = false;
    public static final String TRASH_FOLDER = "Trash folder";
    public final Logger logger;
    public final Window owner;
    private volatile boolean warning_issued = false;

    record Monitored_folder(String name, Path path, boolean auto_delete){}

    List<Monitored_folder> monitored_folders = new ArrayList<>();

    public final int warning_limit_bytes;

    //**********************************************************
    public Disk_usage_monitor(Window owner, Logger logger_)
    //**********************************************************
    {
        logger = logger_;
        this.owner = owner;

        for (Cache_folder cache_folder : Cache_folder.values())
        {
            if(dbg) logger.log("starting Disk_usage_monitor for :" + cache_folder);
            Path ff = Static_files_and_paths_utilities.get_cache_folder(cache_folder, owner, logger);
            Monitored_folder tt = new Monitored_folder(cache_folder.name(), ff, true);
            monitored_folders.add(tt);
        }

        for ( Path t : Static_files_and_paths_utilities.get_existing_trash_dirs(owner,logger))
        {
            monitored_folders.add(new Monitored_folder(TRASH_FOLDER, t, false));
        }

        warning_limit_bytes = Non_booleans_properties.get_folder_warning_size(owner);


    }

    //**********************************************************
    public boolean monitor()
    //**********************************************************
    {
        if ( warning_limit_bytes <= 0)
        {
            logger.log("WARNING: "+     My_I18n.get_I18n_string("Cache_Size_Warning_Limit",owner,logger)+" is zero = no limit, no monitoring!");
            return false;
        }
        //long total = 0;
        for( Monitored_folder monitored_folder : monitored_folders)
        {
            long tmp = Static_files_and_paths_utilities.get_size_on_disk_concurrent(monitored_folder.path, Shared_services.aborter(),logger);

            if ( Shared_services.aborter().should_abort())
            {
                logger.log("Disk_usage_monitor aborted");
                return false;
            }

            tmp = tmp/1_000_000; // mega bytes!


            if ( tmp > warning_limit_bytes)
            {
                if ( !monitored_folder.auto_delete)
                {
                    Popups.popup_warning("❗"+monitored_folder.name+" is getting very large: "+tmp+" Mbytes",
                            "Consider clearing it...\n" +
                                    "or change this limit Using the dedicated item in the preferences menu",
                            false,owner,logger);
                    continue;
                }
                boolean cleared = false;
                if (Booleans.get_boolean_defaults_to_true(Feature.Monitor_folders.name()))
                {
                    for (Cache_folder cache_folder : Cache_folder.values())
                    {
                        if (monitored_folder.name.equals(cache_folder.name()))
                        {
                            Cache_folder.clear_disk_cache(cache_folder, false, owner, Shared_services.aborter(), logger);
                            cleared = true;
                        }
                    }
                }
                if ( cleared) continue;
                if ( !warning_issued)
                {
                    Popups.popup_warning("❗ "+monitored_folder.name+" is getting very large: "+tmp+" Mbytes",
                            "Consider clearing it...\n" +
                                    "or change this limit Using the dedicated item in the preferences menu",
                            false,owner,logger);
                    warning_issued = true;
                }
            }
            //total += tmp;
        }

        /*
        long free = 0;
        for (FileStore fileStore : FileSystems.getDefault().getFileStores())
        {

            try {
                long usable = fileStore.getUsableSpace();
                logger.log(
                        "fileStore:\n"+fileStore.name()+
                                "total:\n"+fileStore.getTotalSpace()+
                           "            usable: "+usable);
                free += usable;
            } catch (IOException e) {
                logger.log(Stack_trace_getter.get_stack_trace(""+e));
            }
            logger.log("\n\n");
        }
        logger.log("Free = "+free+" total used = "+total+ " "+(double)total*100.0/(double)free+" %" );

        if ( total > free/3)
        {
            logger.log("WARNING !!!");
        }*/


        return true;
    }

}
