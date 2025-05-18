package klik.properties;

import javafx.util.Pair;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.util.log.Logger;
import klik.util.ui.Popups;
import klik.util.log.Stack_trace_getter;
import klik.util.log.System_logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
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
    public static final String AGE = "_age";
    public static final int max = 30;

    private final Properties the_Properties;
    private final Path the_properties_path;
    private final Logger logger;
    private final Aborter aborter;
    private final String tag;

    // saving to file is done in a separate thread:
    public final BlockingQueue<Boolean> disk_store_request_queue = new LinkedBlockingQueue<>();

    //**********************************************************
    public Properties_manager(Path f_, String tag, Aborter aborter,Logger logger)
    //**********************************************************
    {
        this.tag = tag;
        this.aborter = aborter;
        this.logger = logger;
        the_properties_path = f_;
        the_Properties = new Properties();
        load_properties();
        start_store_engine( aborter,  logger);

        //for ( String k : get_all_keys()) logger.log("property: " + k + " = " + get(k));
    }

    //**********************************************************
    public Set<String> get_all_keys()
    //**********************************************************
    {
        return the_Properties.stringPropertyNames();
    }

    //**********************************************************
    public void store_properties()
    //**********************************************************
    {
        disk_store_request_queue.add(true);
    }

    // trying to limit disk writes for source that can be super active
    // like the image properties cache or image feature vectors etc
    // but keep as safe as possible, especially always saved on clean exit (with aborter)
    //**********************************************************
    private void start_store_engine(Aborter aborter, Logger logger)
    //**********************************************************
    {
        Runnable r = () -> {
            for(;;)
            {
                try {
                    Boolean b = disk_store_request_queue.poll(20, TimeUnit.SECONDS);
                    if (aborter.should_abort())
                    {
                        // always save on clean exit
                        save();
                        logger.log("aborting (after saving) Properties store engine : " + tag + " " + the_properties_path);
                        return;
                    }
                    if ( b == null)
                    {
                        // this is a time out (20 seconds),
                        // the tile out is here to make sure we read the aborter
                        // no need save
                        continue;
                    }
                    if ( disk_store_request_queue.peek() != null)
                    {
                        logger.log("ignoring as there are more requests for saving Properties store engine : " + tag + " " + the_properties_path);
                        // if another request is already in flight, we will have an opportunity to save very soon
                        continue;
                    }

                    save();
                }
                catch (InterruptedException e)
                {
                    save();
                    logger.log("saving INTERRUPTED Properties store engine : " + tag + " " + the_properties_path);
                    return;
                }
            }
        };
        Actor_engine.execute(r, logger);
    }

    //**********************************************************
    private void save()
    //**********************************************************
    {
       //if (dbg)
            logger.log("Properties: save "+the_properties_path.toAbsolutePath());

        if (!Files.exists(the_properties_path))
        {
            try {
                Files.createFile(the_properties_path);
            }
            catch (FileAlreadyExistsException e)
            {
                if (dbg) logger.log("Warning: " + Stack_trace_getter.get_stack_trace_for_throwable(e));
            }
            catch (IOException e) {
                logger.log("FATAL: " + Stack_trace_getter.get_stack_trace_for_throwable(e));
                return;
            }
            if (dbg) logger.log("created file:"+ the_properties_path);
        }
        else
        {
            if (dbg) logger.log(" file exists:"+ the_properties_path);
        }

        if (!Files.isWritable(the_properties_path))
        {
            Popups.popup_Exception(new AccessDeniedException(the_properties_path.toAbsolutePath().toString()), 200, "Cannot store properties ", logger);
            logger.log("ALERT: cannot write properties in:" + the_properties_path.toAbsolutePath());
            return;
        }
        else
        {
            if (dbg) logger.log(" file is writable:"+ the_properties_path);
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
            Popups.popup_Exception(new AccessDeniedException(the_properties_path.toAbsolutePath().toString()), 200, "Cannot store properties due to: "+e, logger);

        }
    }

    //**********************************************************
    private void load_properties()
    //**********************************************************
    {
        if (dbg) logger.log("load_properties()");
        FileInputStream fis;
        try
        {

            if (Files.exists(the_properties_path))
            {
                if (!Files.isReadable(the_properties_path))
                {
                    logger.log("cannot read properties from:" + the_properties_path.toAbsolutePath());
                    return;
                }
                fis = new FileInputStream(the_properties_path.toFile());
                the_Properties.load(fis);
                if (dbg) logger.log("properties loaded from:" + the_properties_path.toAbsolutePath());
                fis.close();
            }

        } catch (Exception e)
        {
            logger.log("load_properties Exception: " + e);
        }
    }


    /*
     * low level API: use only for single ponctual items
     */

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
    private void clear()
    //**********************************************************
    {
        the_Properties.clear();
    }



    //**********************************************************
    public boolean add(String key, String value)
    //**********************************************************
    {
        the_Properties.setProperty(key, value);
        disk_store_request_queue.add(true);
        return true;
    }


    //**********************************************************
    public Object remove(String key)
    //**********************************************************
    {
        return the_Properties.remove(key);
    }

    //**********************************************************
    public void remove(String key, String value)
    //**********************************************************
    {
        the_Properties.remove(key, value);
    }

    //**********************************************************
    public void clear(String key_base)
    //**********************************************************
    {
        for (int i = 0; i < max; i++)
        {
            String key = key_base + i;
            String value = get(key);
            if (value != null) remove(key, value);

            // maybe we also stored the age?
            {
                String key2 = key_base + i + AGE;
                String value2 = get(key2);
                if (value2 != null) remove(key2, value2);
            }
        }
        store_properties();
    }



    /*
     * "with base"" API: for a given keyword (base-key), can store up to "max" items
     * When max is reached, the oldest element is overwritten
     */

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
        String key = save_multiple(base,value,false);
        return new Pair<>(key,value);
    }
    //**********************************************************
    public void delete(String key, boolean and_save, Logger logger)
    //**********************************************************
    {
        remove(key);
        if ( and_save) store_properties();
    }



    // saves a value for a base-key, handling oldest-replacement silently
    //**********************************************************
    public String save_multiple(String key_base, String value, boolean with_age)
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

        add_with_age(key, value, with_age);
        return key;
    }

    //**********************************************************
    public void add_with_age(String key, String value, boolean with_age)
    //**********************************************************
    {
        if (dbg) logger.log("Non_booleans: imperative_store " + key + "=" + value);

        LocalDateTime now = LocalDateTime.now();
        add(key + AGE, now.toString());
        if (with_age)
        {
            the_Properties.setProperty(key + AGE, now.toString());
        }
        else
        {
            the_Properties.setProperty(key, value);
        }
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

    /*
     * unit test
     */

    //**********************************************************
    public static void main(String[] deb)
    //**********************************************************
    {
        String TOTO = "toto";
        File f_ = new File("debil.txt");
        Logger logger = System_logger.get_system_logger("Properties test");
        Properties_manager pm = new Properties_manager(f_.toPath(), "unit test",new Aborter("dummy",logger),logger);

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



}