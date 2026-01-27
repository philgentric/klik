// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.properties;

import javafx.stage.Window;
import javafx.util.Pair;
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
    private final static boolean dbg = true;
    private final static boolean dbg_set= true;
    private final static boolean dbg_get= true;
    public static final String AGE = "_age";
    private final Logger logger;
    private final String tag;
    private final Path path;
    private final ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();
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

        if ( dbg) logger.log("File_storage_using_Properties "+path.toAbsolutePath().toString());
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
    public boolean set(String key, String value)
    //**********************************************************
    {
        if( dbg_set) logger.log(("File_storage_using_Properties "+ tag +" set() ->"+key+"<- => ->"+value+"<-"));
        map.put(key, value);
        if ( with_age)
        {
            map.put(key+AGE, LocalDateTime.now().toString());
        }

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
    }

    //**********************************************************
    @Override
    public void clear()
    //**********************************************************
    {
        if( dbg_set) logger.log("File_storage_using_Properties "+ tag +" clear() ");
        map.clear();
    }

    //**********************************************************
    @Override
    public void reload_from_disk()
    //**********************************************************
    {
        Properties local = new Properties();
        if (dbg) logger.log("load_properties()");
        FileInputStream fis;
        try
        {

            if (Files.exists(path))
            {
                if (!Files.isReadable(path))
                {
                    logger.log("cannot read properties from:" + path.toAbsolutePath());
                    return;
                }
                fis = new FileInputStream(path.toFile());
                try
                {
                    local.load(fis);
                }
                catch (IllegalArgumentException ee)
                {
                    logger.log(Stack_trace_getter.get_stack_trace("load_properties Exception: " + ee+ " for path: "+path.toAbsolutePath()));
                    fis.close();
                    return;
                }
                if (dbg) logger.log("properties loaded from:" + path.toAbsolutePath());
                fis.close();
            }
        }
        catch (Exception e)
        {
            logger.log(Stack_trace_getter.get_stack_trace("load_properties Exception: " + e+ " for path: "+path.toAbsolutePath()));
        }

        for (String k : local.stringPropertyNames())
        {
            map.put(k, local.getProperty(k));
        }
    }

    @Override
    public void save_to_disk()
    {
        Properties local = new Properties();
        for (String k : map.keySet())
        {
            local.put(k, map.get(k));
        }
        if (dbg) logger.log("save_to_disk()");
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
