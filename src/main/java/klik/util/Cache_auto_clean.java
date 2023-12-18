package klik.util;

import klik.actor.Aborter;
import klik.files_and_paths.Files_and_Paths;
import klik.properties.Static_application_properties;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

//**********************************************************
public class Cache_auto_clean
//**********************************************************
{
    private static final boolean dbg = false;
    private static final long AGE_LIMIT_IN_DAYS = 2;
    public final Logger logger;
    public final Aborter aborter;
    private volatile boolean warning_issued = false;

    record Monitored_folder(String name, Path path){}

    List<Monitored_folder> monitored_folders = new ArrayList<>();


    //**********************************************************
    public Cache_auto_clean(Aborter aborter_, Logger logger_)
    //**********************************************************
    {
        aborter= aborter_;
        logger = logger_;

        monitored_folders.add(new Monitored_folder("Icon cache folder",Files_and_Paths.get_icon_cache_dir(logger)));
        monitored_folders.add(new Monitored_folder("Folder's icon cache folder",Files_and_Paths.get_folder_icon_cache_dir(logger)));
        monitored_folders.add(new Monitored_folder("Trash folder",Static_application_properties.get_trash_dir(logger)));

    }

    //**********************************************************
    private boolean monitor()
    //**********************************************************
    {
        //long total = 0;
        for( Monitored_folder monitored_folder : monitored_folders)
        {
            File[] files = monitored_folder.path.toFile().listFiles();
            for ( File f : files)
            {
                long age = Files_and_Paths.get_file_age_in_days(f,logger);
                //logger.log(f.toPath().toAbsolutePath()+ " age = "+age+ " days");
                if ( age > AGE_LIMIT_IN_DAYS)
                {
                    if ( dbg) logger.log(f.toPath().toAbsolutePath()+ " is too old at "+age+" days, deleting");
                    try {
                        Files.delete(f.toPath());
                    } catch (IOException e) {
                        logger.log(""+e);
                    }
                }
            }
        }

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
                        Thread.sleep(10*60*1000);
                    } catch (InterruptedException e) {
                        logger.log(""+e);
                    }
                }
            }
        };
        Threads.execute(r,logger);

    }
}
