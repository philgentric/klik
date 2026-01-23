// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.util.disk_cache_auto_clean;

import javafx.stage.Window;
import klikr.util.cache.Cache_folder;
import klikr.util.files_and_paths.Static_files_and_paths_utilities;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

//**********************************************************
public class Disk_cache_auto_clean
//**********************************************************
{
    private static final boolean dbg = false;
    private static final long AGE_LIMIT_IN_DAYS = 2;
    public final Logger logger;
    private volatile boolean warning_issued = false;

    record Monitored_folder(String name, Path path){}

    List<Monitored_folder> monitored_folders = new ArrayList<>();


    //**********************************************************
    public Disk_cache_auto_clean(Window owner, Logger logger_)
    //**********************************************************
    {
        logger = logger_;

        for(Cache_folder cache_folder : Cache_folder.values())
        {
            monitored_folders.add(new Monitored_folder(cache_folder.name(), Static_files_and_paths_utilities.get_cache_folder(cache_folder,owner,logger)));
        }
    }

    //**********************************************************
    public boolean monitor()
    //**********************************************************
    {
        for( Monitored_folder monitored_folder : monitored_folders)
        {
            File[] files = monitored_folder.path.toFile().listFiles();
            if ( files==null)
            {
                if ( !warning_issued)
                {
                    logger.log("WARNING: Disk_cache_auto_clean not able to list files in "+monitored_folder.path);
                    warning_issued = true;
                }
                continue;
            }
            for ( File f : files)
            {
                if ( f.isDirectory())
                {
                    logger.log("WARNING: Disk_cache_auto_clean not erasing folders "+f);
                    continue;
                }
                delete_if_too_old(f);
            }
        }
        return true;
    }

    //**********************************************************
    private void delete_if_too_old(File f)
    //**********************************************************
    {
        long age = Static_files_and_paths_utilities.get_file_age_in_days(f,logger);
        //logger.log(f.toPath().toAbsolutePath()+ " age = "+age+ " days");
        if ( age > AGE_LIMIT_IN_DAYS)
        {
            if ( dbg) logger.log(f.toPath().toAbsolutePath()+ " is too old at "+age+" days, deleting");
            try {
                Files.delete(f.toPath());
            } catch (NoSuchFileException e) {
                logger.log(("delete_if_too_old: "+e.toString()));
                //logger.log(Stack_trace_getter.get_stack_trace("delete_if_too_old: "+e.toString()));
            }
            catch (DirectoryNotEmptyException e) {
                logger.log(Stack_trace_getter.get_stack_trace("delete_if_too_old: "+e.toString()));
            }
            catch (IOException e) {
                logger.log(Stack_trace_getter.get_stack_trace("delete_if_too_old: "+e.toString()));
            }
        }
    }

}
