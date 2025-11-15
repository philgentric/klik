// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.properties;

import javafx.stage.Window;
import javafx.util.Pair;
import klik.util.execute.actor.Aborter;
import klik.util.execute.actor.Actor_engine;
import klik.util.log.Logger;
import klik.util.ui.Popups;
import klik.util.log.Stack_trace_getter;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

//**********************************************************
public class Properties_manager
//**********************************************************
{

    private static final boolean dbg = false;
    private static final boolean ultra_dbg = false;
    public static final String AGE = "_age";
    public static final int max = 30;

    private final Properties the_Properties;
    private final Path the_properties_path;
    private final Logger logger;
    //private final Aborter aborter;
    private final String purpose;

    // saving to file is done in a separate thread:
    private final BlockingQueue<Boolean> disk_store_request_queue = new LinkedBlockingQueue<>();

    //**********************************************************
    public Properties_manager(Path f_, String purpose, Window owner,Aborter aborter,Logger logger)
    //**********************************************************
    {
        Objects.requireNonNull(aborter);
        this.purpose = purpose;
        //this.aborter = aborter;
        this.logger = logger;
        the_properties_path = f_;
        the_Properties = new Properties();
        load_properties(the_Properties, the_properties_path);
        start_store_engine(purpose,  owner,aborter, logger);

        //for ( String k : get_all_keys()) logger.log("property: " + k + " = " + get(k));
    }

    //**********************************************************
    public Set<String> get_all_keys()
    //**********************************************************
    {
        return the_Properties.stringPropertyNames();
    }

    //**********************************************************
    public void store_properties(boolean reload_before_save)
    //**********************************************************
    {
        disk_store_request_queue.add(reload_before_save);
    }

    // trying to limit disk writes for source that can be super active
    // like the image properties cache or image feature vectors etc
    // but keep as safe as possible, especially always saved on clean exit (with aborter)
    //**********************************************************
    private void start_store_engine(String purpose, Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        Runnable r = () -> {
            for(;;)
            {
                if (aborter.should_abort())
                {
                    if ( dbg) logger.log("Properties store engine aborting: " + purpose + " " + the_properties_path);
                }
                try {
                    Boolean reload_before_save = disk_store_request_queue.poll(20, TimeUnit.SECONDS);
                    if ( reload_before_save == null)
                    {
                        // this is a time out (20 seconds), nothing to save
                        if (aborter.should_abort())
                        {
                            //logger.log("exiting Properties store engine due to abort: " + purpose + " " + the_properties_path);
                            return;
                        }
                        continue;
                    }
                    if ( disk_store_request_queue.peek() != null)
                    {
                        //logger.log("ignoring as there are more requests for saving Properties store engine : " + tag + " " + the_properties_path);
                        // if another request is already in flight, we will have an opportunity to save very soon
                        continue;
                    }
                    save(reload_before_save, owner);
                    if (aborter.should_abort())
                    {
                        logger.log("aborting (after saving) Properties store engine : " + purpose + " " + the_properties_path);
                        return;
                    }
                }
                catch (InterruptedException e)
                {
                    logger.log("INTERRUPTED Properties store engine : " + purpose + " " + the_properties_path);
                    return;
                }
            }
        };
        Actor_engine.execute(r, "Properties_manager store engine for :"+purpose,logger);
    }

    //**********************************************************
    private void save(boolean reload_before_save, Window owner)
    //**********************************************************
    {
        if ( reload_before_save)
        {
            // before saving to disk we will reload the properties
            // because another instance may have made changes and saved then to disk
            // the reconciliation will consist in:
            // 1. adding all entries that are on disk but not in this RAM hashmap
            // 2. resolve conflict on a given value:keep the most recent value

            Properties on_disk = new Properties();
            load_properties(on_disk, the_properties_path);
            for (String k : on_disk.stringPropertyNames())
            {
                if ( k.endsWith(AGE))
                {
                    //logger.log("ignoring age key:"+k);
                    continue;
                }
                String v = the_Properties.getProperty(k);
                if (v == null)
                {
                    // absent in THIS RAM hashtable =  must be added
                    the_Properties.setProperty(k, on_disk.getProperty(k));
                    String age = on_disk.getProperty(k + AGE);
                    if (age == null)
                    {
                        age = LocalDateTime.now().toString();
                    }
                    //logger.log("storing age for key:"+k);
                    the_Properties.setProperty(k + AGE, age);
                }
                else
                {
                    String age_here_s = the_Properties.getProperty(k + AGE);
                    if (age_here_s == null) continue;
                    // conflict : take the most recent VALUE
                    String age_on_disk_s = on_disk.getProperty(k + AGE);
                    if (age_on_disk_s == null) continue;
                    LocalDateTime age_here;
                    try {
                        age_here = LocalDateTime.parse(age_here_s);
                    }
                    catch (DateTimeParseException e)
                    {
                        logger.log("WARNING cannot parse this date string? ->"+age_here_s+"<-");
                        return;
                    }
                    LocalDateTime age_on_disk;
                    try {
                        age_on_disk = LocalDateTime.parse(age_on_disk_s);
                    }
                    catch (DateTimeParseException e)
                    {
                        logger.log("WARNING cannot parse this date string? ->"+age_on_disk_s+"<-");
                        return;
                    }
                    if (age_on_disk.isAfter(age_here)) {
                        // the value in this hashtable is outdated, let us update it
                        the_Properties.setProperty(k, on_disk.getProperty(k));
                        the_Properties.setProperty(k + AGE, age_on_disk_s);
                    }
                }
            }
        }
        save_to_disk(owner);
    }

    //**********************************************************
    private void save_to_disk(Window owner)
    //**********************************************************
    {
       if (dbg) logger.log("Properties_manager: save to disk "+the_properties_path.toAbsolutePath());

        if (!Files.exists(the_properties_path))
        {
            try {
                Files.createDirectories(the_properties_path.getParent());
                Files.createFile(the_properties_path);
            }
            catch (FileAlreadyExistsException e)
            {
                if (dbg) logger.log("Warning: " + Stack_trace_getter.get_stack_trace_for_throwable(e));
            }
            catch (IOException e) {
                logger.log("❌ FATAL: " + Stack_trace_getter.get_stack_trace_for_throwable(e));
                return;
            }
            if (dbg) logger.log("created file:"+ the_properties_path);
        }
        else
        {
            if (ultra_dbg) logger.log(" file exists:"+ the_properties_path);
        }

        if (!Files.isWritable(the_properties_path))
        {
            Popups.popup_Exception(new AccessDeniedException(the_properties_path.toAbsolutePath().toString()), 200, "Cannot store properties ", owner, logger);
            logger.log("X FATAL: cannot write properties in:" + the_properties_path.toAbsolutePath());
            return;
        }
        else
        {
            if (ultra_dbg) logger.log(" file is writable:"+ the_properties_path);
        }

        try
        {
            FileOutputStream fos = new FileOutputStream(the_properties_path.toFile());
            the_Properties.store(fos, "no comment");
            fos.close();
            if (dbg) logger.log(("ALL properties stored in:" + the_properties_path.toAbsolutePath()));
        }
        catch (Exception e)
        {
            //logger.log("store_properties Exception: " + Stack_trace_getter.get_stack_trace_for_throwable(e));
            Popups.popup_Exception(new AccessDeniedException(the_properties_path.toAbsolutePath().toString()), 200, "Cannot store properties due to: "+e, owner,logger);

        }
    }

    //**********************************************************
    public void load_properties(Properties target, Path path)
    //**********************************************************
    {
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
                target.load(fis);
                if (dbg) logger.log("properties loaded from:" + path.toAbsolutePath());
                fis.close();
            }

        } catch (Exception e)
        {
            logger.log("load_properties Exception: " + e);
        }
    }


    //**********************************************************
    public String get(String key)
    //**********************************************************
    {
        return the_Properties.getProperty(key);
    }

    //**********************************************************
    public String get(String key, String replace_with_this_if_not_found)
    //**********************************************************
    {
        return the_Properties.getProperty(key, replace_with_this_if_not_found);
    }


    //**********************************************************
    void clear()
    //**********************************************************
    {
        logger.log("clearing properties");
        the_Properties.clear();
        disk_store_request_queue.add(false);
    }



    //**********************************************************
    public boolean add(String key, String value)
    //**********************************************************
    {
        the_Properties.setProperty(key, value);
        the_Properties.setProperty(key+AGE, LocalDateTime.now().toString());
        disk_store_request_queue.add(true);
        return true;
    }


    //**********************************************************
    public Object remove(String key)
    //**********************************************************
    {
        the_Properties.remove(key+AGE);
        Object returned = the_Properties.remove(key);
        disk_store_request_queue.add(true);
        return returned;
    }

    //**********************************************************
    public void clear(String key_base)
    //**********************************************************
    {
        for (int i = 0; i < max; i++)
        {
            String key = key_base + i;
            String value = get(key);
            if (value != null) remove(key);

            // maybe we also stored the age?
            {
                String key2 = key_base + i + AGE;
                String value2 = get(key2);
                if (value2 != null) remove(key2);
            }
        }
        store_properties(false);
    }



    /*
     * "with base"" API: for a given keyword (base-key), can store up to "max" items
     * When max is reached, the oldest element is overwritten
     */



    Comparator<? super Pair<String, String>> comp = Comparator.comparing((Pair<String, String> o) -> o.getKey()).thenComparing(Pair::getValue);

    //**********************************************************
    public List<Pair<String, String>> get_pairs_for_base(String key_base)
    //**********************************************************
    {
        List<Pair<String, String>> returned = new ArrayList<>();
        for (int i = 0; i < max; i++)
        {
            if (get(key_base + i) != null) returned.add(new Pair<>(key_base + i, get(key_base + i)));
        }
        returned.sort(comp);
        return returned;
    }
    //**********************************************************
    public Pair<String,String> add_for_base(String base, String value)
    //**********************************************************
    {
        String key = save_multiple(base,value);
        return new Pair<>(key,value);
    }



    // saves a value for a base-key, handling oldest-replacement silently
    //**********************************************************
    public String save_multiple(String key_base, String value)
    //**********************************************************
    {
        // avoid saving several times the same value
        for (Entry<?, ?> e : the_Properties.entrySet())
        {
            String val = (String) e.getValue();
            if (val.equals(value))
            {
                String local_key = (String) e.getKey();
                if (local_key.startsWith(key_base)) return null;
            }
        }
        String key = get_one_empty_key_for_base(key_base);

        add(key, value);
        return key;
    }




    // if there are no more available slots,
    // this will ERASE the OLDEST value
    //**********************************************************
    private String get_one_empty_key_for_base(String key_base)
    //**********************************************************
    {
        for (int i = 0; i < max; i++)
        {
            if (get(key_base + i) == null) return key_base + i;
        }

        String key = null;
        {
            // erase the oldest one
            LocalDateTime oldest = null;
            for (int i = 0; i < max; i++)
            {
                String date = get(key_base + i + AGE);
                if (date == null)
                {
                    key = key_base + i;
                    break;
                }
                else
                {
                    LocalDateTime ld = LocalDateTime.parse(date);
                    if (oldest == null)
                    {
                        oldest = ld;
                        key = key_base + i;
                    }
                    else
                    {
                        if (oldest.isAfter(ld))
                        {
                            oldest = ld;
                            key = key_base + i;
                        }
                    }
                }
            }
        }

        return key;
    }


    //**********************************************************
    public void delete(String key, boolean and_save)
    //**********************************************************
    {
        remove(key);
        if ( and_save) store_properties(false);
    }

    //**********************************************************
    public void force_reload_from_disk()
    //**********************************************************
    {
        load_properties(the_Properties,the_properties_path);
    }



    /*

    // list all stored values for a base-key
    //**********************************************************
    public List<String> get_values_for_base(String key_base)
    //**********************************************************
    {
        List<String> returned = new ArrayList<>();
        for (int i = 0; i < max; i++)
        {
            if (get(key_base + i) != null) returned.add(get(key_base + i));
        }
        Collections.sort(returned);
        return returned;
    }



    //**********************************************************
    public boolean remove_invalid_dir(Path dir)
    //**********************************************************
    {
        String to_be_removed_key;
        String to_be_removed_value;
        for (Entry<?, ?> e : the_Properties.entrySet())
        {
            String val = (String) e.getValue();
            if (val.equals(dir.toAbsolutePath().toString()))
            {
                to_be_removed_key = (String) e.getKey();
                to_be_removed_value = val;
                boolean status = the_Properties.remove(to_be_removed_key, to_be_removed_value);
                store_properties();
                return status;
            }
        }
        return false;
    }

    /// unit test

    //**********************************************************
    public static void main(String[] deb)
    //**********************************************************
    {
        String TOTO = "toto";
        File f_ = new File("debil.txt");
        Logger logger = Logger_factory.get_system_logger("Properties test");
        Properties_manager pm = Properties_manager.get(f_.toPath(), "unit test",new Aborter("dummy",logger),logger);

        for (int i = 0; i < 15; i++)
        {
            String s = pm.get_one_empty_key_for_base(TOTO);
            String value = "value for " + s;
            pm.add(s, value);
        }

        for (String s : pm.get_values_for_base(TOTO))
        {
            logger.log(s);
        }

        String s = pm.get_one_empty_key_for_base(TOTO);
        if (s == null)
        {
            logger.log("FATL 35343");
            return;
        }
        String value = "value for REPLACED " + s;
        pm.add(s, value);
        logger.log(s + " is now key for: " + value);
    }


*/
}