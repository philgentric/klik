package klik.util.cache_auto_clean;

import javafx.stage.Stage;
import klik.actor.Aborter;
import klik.browser.icons.image_properties_cache.Image_properties_RAM_cache;
import klik.look.my_i18n.My_I18n;
import klik.util.files_and_paths.Static_files_and_paths_utilities;
import klik.properties.Static_application_properties;
import klik.util.log.Logger;
import klik.util.ui.Popups;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

//**********************************************************
public class Disk_usage_monitor
//**********************************************************
{

    public static final String ICON_CACHE_FOLDER = "Icon cache folder";
    public static final String IMAGE_PROPERTIES_CACHE_FOLDER = "Aspect ratio cache folder";
    public static final String TRASH_FOLDER = "Trash folder";
    public final Logger logger;
    public final Aborter aborter;
    private volatile boolean warning_issued = false;

    record Monitored_folder(String name, Path path, boolean auto_delete){}

    List<Monitored_folder> monitored_folders = new ArrayList<>();

    public final int warning_limit_bytes;

    //**********************************************************
    public Disk_usage_monitor(Stage owner, Aborter aborter_, Logger logger_)
    //**********************************************************
    {
        aborter= aborter_;
        logger = logger_;

        monitored_folders.add(new Monitored_folder(ICON_CACHE_FOLDER, Static_files_and_paths_utilities.get_icons_cache_dir(owner, logger),true));
        monitored_folders.add(new Monitored_folder(IMAGE_PROPERTIES_CACHE_FOLDER, Image_properties_RAM_cache.get_image_properties_cache_dir(owner, logger),true));
        monitored_folders.add(new Monitored_folder("Folder's icon cache folder", Static_files_and_paths_utilities.get_folders_icons_cache_dir(logger),true));

        for ( Path t : Static_application_properties.get_existing_trash_dirs(logger))
        {
            monitored_folders.add(new Monitored_folder(TRASH_FOLDER, t, false));
        }

        warning_limit_bytes = Static_application_properties.get_folder_warning_size(logger);


    }

    //**********************************************************
    public boolean monitor()
    //**********************************************************
    {
        if ( warning_limit_bytes <= 0)
        {
            logger.log("WARNING: "+     My_I18n.get_I18n_string("Cache_Size_Warning_Limit",logger)+" is zero = no limit, no monitoring!");
            return false;
        }
        //long total = 0;
        for( Monitored_folder monitored_folder : monitored_folders)
        {
            long tmp = Static_files_and_paths_utilities.get_size_on_disk_concurrent(monitored_folder.path,aborter,logger);

            if ( aborter.should_abort())
            {
                logger.log("Disk_usage_monitor aborted");
                return false;
            }

            tmp = tmp/1_000_000; // mega bytes!


            if ( tmp > warning_limit_bytes)
            {
                if ( !monitored_folder.auto_delete)
                {
                    Popups.popup_warning(null,monitored_folder.name+" is getting very large: "+tmp+" Mbytes",
                            "Consider clearing it...(using Files/Clean menu item)\n" +
                                    "or change this limit Using the dedicated item in the preferences menu",
                            false,logger);
                    continue;
                }
                if( monitored_folder.name.equals(ICON_CACHE_FOLDER))
                {
                    if (Static_application_properties.get_auto_purge_disk_caches(logger))
                    {
                        Static_files_and_paths_utilities.clear_icon_cache_on_disk_no_warning(null,logger);
                        Static_files_and_paths_utilities.clear_folder_icon_DISK_cache_no_warning_fx(logger);
                        continue;
                    }
                }
                if( monitored_folder.name.equals(IMAGE_PROPERTIES_CACHE_FOLDER))
                {
                    if (Static_application_properties.get_auto_purge_disk_caches(logger))
                    {
                        Static_files_and_paths_utilities.clear_image_properties_DISK_cache_no_warning_fx(null,logger);
                        continue;
                    }
                }
                if ( !warning_issued)
                {
                    Popups.popup_warning(null,monitored_folder.name+" is getting very large: "+tmp+" Mbytes",
                            "Consider clearing it...(using Files/Clean menu item)\n" +
                                    "or change this limit Using the dedicated item in the preferences menu",
                            false,logger);
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
