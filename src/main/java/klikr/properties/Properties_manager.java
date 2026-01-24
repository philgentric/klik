// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.properties;

import javafx.stage.Window;
import javafx.util.Pair;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.Map.Entry;

//**********************************************************
public class Properties_manager
//**********************************************************
{

    private static final boolean dbg = false;
    private static final boolean ultra_dbg = false;
    public static final String AGE = "_age";
    public static final int max = 30;

    private final Properties the_Properties;
    final Path the_properties_path;
    final Logger logger;
    final Aborter aborter;
    final String purpose;
    final boolean with_age;

    // saving to file is done in a separate thread:

    //**********************************************************
    public Properties_manager(Path f_, String purpose, boolean with_age, Window owner,Aborter aborter,Logger logger)
    //**********************************************************
    {
        Objects.requireNonNull(aborter);
        this.purpose = purpose;
        this.aborter = aborter;
        this.logger = logger;
        this.with_age = with_age;
        the_properties_path = f_;
        the_Properties = new Properties();
        load_properties(the_Properties, the_properties_path);

    }

    //**********************************************************
    public Properties_manager(Path f_, String purpose, Window owner,Aborter aborter,Logger logger)
    //**********************************************************
    {
        Objects.requireNonNull(aborter);
        this.purpose = purpose;
        this.aborter = aborter;
        this.logger = logger;
        this.with_age = false;
        the_properties_path = f_;
        the_Properties = new Properties();
        load_properties(the_Properties, the_properties_path);

    }

    //**********************************************************
    public Set<String> get_all_keys()
    //**********************************************************
    {
        return the_Properties.stringPropertyNames();
    }

    //**********************************************************
    private void store_properties(boolean reload_before_save)
    //**********************************************************
    {
        Store_engine.get_queue(logger).add(new Save_job(reload_before_save,this));
    }

    // trying to limit disk writes for source that can be super active
    // like the image properties cache or image feature vectors etc
    // but keep as safe as possible, especially always saved on clean exit (with aborter)

    //**********************************************************
    public void save_everything_to_disk(boolean reload_before_save)
    //**********************************************************
    {
        Actor_engine.execute(()->save_everything_to_disk_internal(reload_before_save),"save_everything_to_disk for "+purpose,logger);
    }

    //**********************************************************
    void save_everything_to_disk_internal(boolean reload_before_save)
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
        save_to_disk();
    }

    //**********************************************************
    private void save_to_disk()
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
            //Popups.popup_Exception(new AccessDeniedException(the_properties_path.toAbsolutePath().toString()), 200, "Cannot store properties ", owner, logger);
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
            if (dbg) logger.log((the_Properties.size()+"  properties stored in:" + the_properties_path.toAbsolutePath()));
        }
        catch (Exception e)
        {
            logger.log("store_properties Exception: " + Stack_trace_getter.get_stack_trace_for_throwable(e));
            //Popups.popup_Exception(new AccessDeniedException(the_properties_path.toAbsolutePath().toString()), 200, "Cannot store properties due to: "+e, owner,logger);

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
            logger.log(Stack_trace_getter.get_stack_trace("load_properties Exception: " + e));
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
        Store_engine.get_queue(logger).add(new Save_job(false,this));
    }



    //**********************************************************
    public boolean add(String key, String value, boolean reload_before_save)
    //**********************************************************
    {
        the_Properties.setProperty(key, value);
        if ( with_age) the_Properties.setProperty(key+AGE, LocalDateTime.now().toString());
        Store_engine.get_queue(logger).add(new Save_job(reload_before_save,this));
        return true;
    }


    //**********************************************************
    public Object remove(String key)
    //**********************************************************
    {
        the_Properties.remove(key+AGE);
        Object returned = the_Properties.remove(key);
        Store_engine.get_queue(logger).add(new Save_job(true,this));
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

        add(key, value, true);
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