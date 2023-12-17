package klik.util;

import klik.actor.Aborter;
import klik.files_and_paths.Disk_scanner;
import klik.files_and_paths.File_payload;
import klik.files_and_paths.Files_and_Paths;
import klik.properties.Static_application_properties;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;
import java.util.ArrayList;
import java.util.List;

public class Disk_usage_monitor {

    public final Logger logger;
    public final Aborter aborter;
    private volatile boolean warning_issued = false;

    record Monitored_folder(String name, Path path){}

    List<Monitored_folder> monitored_folders = new ArrayList<>();

    public final int WARNING_LIMIT_BYTES;

    public Disk_usage_monitor(Aborter aborter_, Logger logger_)
    {
        aborter= aborter_;
        logger = logger_;

        monitored_folders.add(new Monitored_folder("Icon cache folder",Files_and_Paths.get_icon_cache_dir(logger)));
        monitored_folders.add(new Monitored_folder("Folder's icon cache folder",Files_and_Paths.get_folder_icon_cache_dir(logger)));
        monitored_folders.add(new Monitored_folder("Trash folder",Static_application_properties.get_trash_dir(logger)));

        WARNING_LIMIT_BYTES = Static_application_properties.get_size_warning_bytes(logger);


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

    private boolean monitor()
    {
        //long total = 0;
        for( Monitored_folder monitored_folder : monitored_folders)
        {
            long tmp = Files_and_Paths.get_size_on_disk_concurrent(monitored_folder.path,aborter,logger);
            if ( tmp > WARNING_LIMIT_BYTES)
            {
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

    public void start() {
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
