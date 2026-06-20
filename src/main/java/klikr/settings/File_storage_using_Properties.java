// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.settings;

import javafx.stage.Window;
import klikr.util.execute.actor.Aborter;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

//**********************************************************
public class File_storage_using_Properties implements File_storage
//**********************************************************
{
    private final static boolean dbg = false;
    private final static boolean dbg_set= false;
    private final static boolean dbg_get= false;
    public static final String AGE = "_age";
    private final Logger logger;
    private final String tag;
    private final Path path;
    private volatile ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();
    private final boolean with_age;
    //**********************************************************
    public File_storage_using_Properties(String purpose, String filename, boolean with_age, Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        this.with_age = with_age;
        this.tag = purpose;
        this.logger = logger;
        String home = System.getProperty(String_constants.USER_HOME);
        path = Paths.get(home, String_constants.CONF_DIR, filename+".properties");

        //if ( dbg)
            logger.log("File_storage_using_Properties "+path.toAbsolutePath().toString());
        reload_from_disk();
    }
    //**********************************************************
    public File_storage_using_Properties(Path path, String purpose, boolean with_age, Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        this.with_age = with_age;
        this.tag = purpose;
        this.logger = logger;
        this.path = path;

        if ( dbg) logger.log("File_storage_using_Properties "+path.toAbsolutePath().toString());
        reload_from_disk();
    }
    //**********************************************************
    @Override
    public boolean set_and_save(String key, String value)
    //**********************************************************
    {
        merge_by_age();
        set(key, value);
        save_to_disk();
        return true;
    }

    //**********************************************************
    private void merge_by_age()
    //**********************************************************
    {
        Properties fileProps = load();
        if (fileProps == null) return;

        for (String k : fileProps.stringPropertyNames())
        {
            if (k.endsWith(AGE)) continue;  // Skip age keys, handle with their parent
            String fileAgeStr = fileProps.getProperty(k + AGE);
            String memAgeStr = map.get(k + AGE);
            if (memAgeStr == null)
            {
                // Key not in memory → take from file
                map.put(k, fileProps.getProperty(k));
                if (fileAgeStr != null) map.put(k + AGE, fileAgeStr);
            }
            else if (fileAgeStr != null)
            {
                // Both have the key → compare ages
                LocalDateTime fileAge = LocalDateTime.parse(fileAgeStr);
                LocalDateTime memAge = LocalDateTime.parse(memAgeStr);

                if (fileAge.isAfter(memAge))
                {
                    // File is newer → take file's value
                    map.put(k, fileProps.getProperty(k));
                    map.put(k + AGE, fileAgeStr);
                }
                // else: memory is newer → keep memory's value (do nothing)
            }
        }
    }



    //**********************************************************
    @Override
    public boolean set(String key, String value)
    //**********************************************************
    {
        if( dbg_set) logger.log(("File_storage_using_Properties "+ tag +" set() ->"+key+"<- => ->"+value+"<-"));
        map.put(key, value);
        if ( with_age)
        {
            map.put(key+AGE, LocalDateTime.now().toString());
        }
        save_to_disk();
        return true;
    }
    //**********************************************************
    @Override
    public String get(String key)
    //**********************************************************
    {
        String returned =  map.get(key);
        if( dbg_get) logger.log("File_storage_using_Properties "+ tag +" get() ->"+key+"<- => ->"+returned+"<-");
        return returned;
    }
    //**********************************************************
    @Override
    public LocalDateTime get_age(String key)
    //**********************************************************
    {
        String age_s =  map.get(key+AGE);
        if( dbg_get) logger.log("File_storage_using_Properties "+ tag +" get_age() ->"+key+"<- => ->"+age_s+"<-");
        return LocalDateTime.parse(age_s);
    }
    //**********************************************************
    @Override
    public void remove(String key)
    //*********************************************************
    {
        if( dbg_set) logger.log("File_storage_using_Properties "+ tag +" remove() ->"+key+"<-");
        map.remove(key);
        if ( with_age)
        {
            map.remove(key+AGE);
        }
        save_to_disk();
    }

    //**********************************************************
    @Override
    public void clear()
    //**********************************************************
    {
        if( dbg_set) logger.log("File_storage_using_Properties "+ tag +" clear() ");
        map.clear();
        save_to_disk();
    }

    //**********************************************************
    private Properties load()
    //**********************************************************
    {
        Properties returned = new Properties();
        if (dbg) logger.log("load_properties()");
        FileInputStream fis;
        try
        {
            if (Files.exists(path))
            {
                if (!Files.isReadable(path))
                {
                    logger.log("cannot read properties from:" + path.toAbsolutePath());
                    return null;
                }
                fis = new FileInputStream(path.toFile());
                try
                {
                    returned.load(fis);
                }
                catch (IllegalArgumentException ee)
                {
                    logger.log(Stack_trace_getter.get_stack_trace("load_properties Exception: " + ee+ " for path: "+path.toAbsolutePath()));
                    fis.close();
                    return null;
                }
                if (dbg) logger.log("properties loaded from:" + path.toAbsolutePath());
                fis.close();
            }
        }
        catch (Exception e)
        {
            logger.log(Stack_trace_getter.get_stack_trace("load_properties Exception: " + e+ " for path: "+path.toAbsolutePath()));
        }
        return returned;
    }

    //**********************************************************
    @Override
    public void reload_from_disk()
    //**********************************************************
    {
        Properties local = load();
        ConcurrentHashMap<String, String> local_map = new ConcurrentHashMap<>();
        for (String k : local.stringPropertyNames())
        {
            local_map.put(k, local.getProperty(k));
        }
        map = local_map;
    }

    //**********************************************************
    @Override
    public void save_to_disk()
    //**********************************************************
    {
        Properties local = new Properties();
        for (Map.Entry<String, String> e : map.entrySet())
        {
            local.put(e.getKey(), e.getValue());
        }
        if (dbg) logger.log("File_storage_using_Properties: save_to_disk() "+path+ " size:"+local.size());
        try
        {
            FileOutputStream fos = new FileOutputStream(path.toFile());
            local.store(fos,"");
            fos.close();
        }
        catch (IOException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace("save_to_disk Exception: " + e+ " for path: "+path.toAbsolutePath()));
        }
        if (dbg) logger.log("save_to_disk() DONE for: " + path.toAbsolutePath());

    }


    //**********************************************************
    @Override
    public List<String> get_all_keys()
    //**********************************************************
    {
        if( dbg_get) logger.log("File_storage_using_Properties "+ tag +" get_all_keys()");
        Set<String> x = map.keySet();
        List<String> result = new ArrayList<>();
        result.addAll(x);
        return result;
    }

    //**********************************************************
    @Override
    public String get_tag()
    //**********************************************************
    {
        return tag;
    }


}
