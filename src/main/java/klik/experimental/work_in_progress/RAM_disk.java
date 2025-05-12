package klik.experimental.work_in_progress;

import javafx.stage.Window;
import klik.properties.Non_booleans;
import klik.util.log.Logger;
import klik.util.ui.Popups;
import klik.util.log.Stack_trace_getter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

// this is a RAM disk for caching
// Note that on my recent Mac I could not
// "sea" a difference between the RAM disk and the SSD disk


//**********************************************************
public class RAM_disk
//**********************************************************
{
    public  static final String RAM_disk_name = "klik_RAM_disk_please_do_not_eject";
    private static String ram_disk_path = null;
    private static boolean init_done = false;
    private static final boolean dbg = false;

    //**********************************************************
    public static boolean init_RAM_disk_path(Window owner, Logger logger)
    //**********************************************************
    {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac"))
        {
            ram_disk_path = "/Volumes/"+RAM_disk_name;
            return true;
        }
        else if (os.contains("nix") || os.contains("nux") )
        {
            ram_disk_path = "/tmp/"+RAM_disk_name;
            return true;
        }
        else
        {
            ram_disk_path = null;
            Popups.popup_warning(owner,"Sorry, RAM disk is not supported under windows","Sorry, RAM disk is not supported under windows",false,logger);
        }
        return false;
    }

    //**********************************************************
    public static boolean init_RAM_disk(long RAM_disk_size_MB, Window owner, Logger logger)
    //**********************************************************
    {
        String os = System.getProperty("os.name").toLowerCase();
        String[] command;

        if (os.contains("mac"))
        {
            // macOS command to create RAM disk
            int sectorSize = 512; // assuming a standard 512-byte sector size
            long ramDiskSizeInSectors = (RAM_disk_size_MB * 1024 * 1024) / sectorSize;

            command = new String[]{"/bin/sh", "-c", "diskutil erasevolume HFS+ '"+RAM_disk_name+"' `hdiutil attach -nomount ram://"+ramDiskSizeInSectors+"`"};
        }
        else if (os.contains("nix") || os.contains("nux") )
        {
            // Linux command to create RAM disk
            command = new String[]{"/bin/sh", "-c", "mkdir -p /tmp/ramdisk; mount -t tmpfs -o size="+RAM_disk_size_MB +"m tmpfs /tmp/ramdisk"};
        } else {
            logger.log("Sorry, RAM disk is not supported under windows");
            Popups.popup_warning(owner,"Sorry, RAM disk is not supported under windows","Sorry, RAM disk is not supported under windows",false,logger);
            return false;
        }

        try {
            Process proc = new ProcessBuilder(command).start();
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
            return false;
        }

        return true;
    }


    //**********************************************************
    public static String get_top_folder_name(Window owner, Logger logger)
    //**********************************************************
    {
        if ( ram_disk_path == null)
        {
            if( !init_RAM_disk_path(owner, logger))
            {
                return null;
            }
        }

        if ( !init_done)
        {
            if( !ram_disk_mounted() )
            {
                init_RAM_disk(1000,owner,logger);
            }
            init_done = true;
        }
        return ram_disk_path;
    }

    //**********************************************************
    private static boolean ram_disk_mounted()
    //**********************************************************
    {
        File ram_disk = Path.of(ram_disk_path).toFile();
        if ( ram_disk.exists())
        {
            if ( ram_disk.isDirectory()) return true;
        }
        return false;
    }



    // returns a directory using that relative name, on the user home
    // creates it if needed
    //**********************************************************
    public static Path get_absolute_dir_on_RAM_disk(String relative_dir_name, Window owner, Logger logger)
    //**********************************************************
    {
        if (dbg) logger.log("dir_name=" + relative_dir_name);

        return Non_booleans.from_top_folder(RAM_disk.get_top_folder_name(owner,logger), relative_dir_name,false, logger);

    }


}

