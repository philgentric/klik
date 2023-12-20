package klik.util;

import klik.actor.Aborter;
import klik.files_and_paths.Files_and_Paths;
import klik.properties.Static_application_properties;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

//**********************************************************
public class Disk_usage_monitor
//**********************************************************
{

    public static final String ICON_CACHE_FOLDER = "Icon cache folder";
    public static final String ASPECT_RATIO_CACHE_FOLDER = "Aspect ratio cache folder";
    public static final String TRASH_FOLDER = "Trash folder";
    public final Logger logger;
    public final Aborter aborter;
    private volatile boolean warning_issued = false;

    record Monitored_folder(String name, Path path){}

    List<Monitored_folder> monitored_folders = new ArrayList<>();

    public final int warning_limit_bytes;

    //**********************************************************
    public Disk_usage_monitor(Aborter aborter_, Logger logger_)
    //**********************************************************
    {
        aborter= aborter_;
        logger = logger_;

        monitored_folders.add(new Monitored_folder(ICON_CACHE_FOLDER,Files_and_Paths.get_icon_cache_dir(logger)));
        monitored_folders.add(new Monitored_folder(ASPECT_RATIO_CACHE_FOLDER,Files_and_Paths.get_aspect_ratio_cache_dir(logger)));
        monitored_folders.add(new Monitored_folder("Folder's icon cache folder",Files_and_Paths.get_folder_icon_cache_dir(logger)));
        monitored_folders.add(new Monitored_folder(TRASH_FOLDER,Static_application_properties.get_trash_dir(logger)));

        warning_limit_bytes = Static_application_properties.get_size_warning_bytes(logger);


        /*
        for (FileStore fileStore : FileSystems.getDefault().getFileStores())
        {
            try {
                logger.log("fileStore: "+fileStore.name()+" "+fileStore.getUsableSpace());
            } catch (IOException e) {
                logger.log(Stack_trace_getter.get_stack_trace(""+e));
            }
        }*/


    }

    //**********************************************************
    private boolean monitor()
    //**********************************************************
    {
        //long total = 0;
        for( Monitored_folder monitored_folder : monitored_folders)
        {
            long tmp = Files_and_Paths.get_size_on_disk_concurrent(monitored_folder.path,aborter,logger);
            if ( tmp > warning_limit_bytes)
            {
                if( monitored_folder.name.equals(ICON_CACHE_FOLDER))
                {
                    if (Static_application_properties.get_auto_purge_disk_caches(logger))
                    {
                        Files_and_Paths.clear_icon_cache_on_disk_no_warning(logger);
                        Files_and_Paths.clear_folder_icon_cache_no_warning(logger);
                        continue;
                    }
                }
                if( monitored_folder.name.equals(ASPECT_RATIO_CACHE_FOLDER))
                {
                    if (Static_application_properties.get_auto_purge_disk_caches(logger))
                    {
                        Files_and_Paths.clear_aspect_ratio_cache_on_disk_no_warning(logger);
                        continue;
                    }
                }
                if ( !warning_issued)
                {
                    Popups.popup_warning(null,monitored_folder.name+" is getting very large: "+tmp/1000_000+" Mbytes",
                            "Consider clearing it...(using Files/Clean menu item)\n" +
                                    "or change this limit by editing "+Static_application_properties.DISK_CACHE_SIZE_WARNING_BYTES+" in "+Static_application_properties.PROPERTIES_FILENAME,
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

    //**********************************************************
    public void start()
    //**********************************************************
    {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                for(;;)
                {
                    if ( aborter.should_abort()) return;
                    if ( !monitor()) break;

                    try {
                        Thread.sleep(3*1000);
                    } catch (InterruptedException e) {
                        logger.log(""+e);
                    }
                }
            }
        };
        Threads.execute(r,logger);

    }
}
