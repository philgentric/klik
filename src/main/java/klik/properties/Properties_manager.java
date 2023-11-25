package klik.properties;

import javafx.util.Pair;
import klik.util.Logger;
import klik.util.Popups;
import klik.util.Stack_trace_getter;
import klik.util.System_out_logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Map.Entry;

//**********************************************************
public class Properties_manager
//**********************************************************
{
    private static final boolean dbg = false;
    public static final String AGE = "_age";
    public static final int max = 30;

    private final Properties the_Properties;
    private final Path f;
    Logger logger;

    //**********************************************************
    public Properties_manager(Path f_, Logger logger_)
    //**********************************************************
    {
        logger = logger_;
        f = f_;
        the_Properties = new Properties();
        load_properties();
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
        if (dbg) logger.log("store_properties()");

        if (!Files.exists(f))
        {
            try {
                Files.createFile(f);
            } catch (IOException e) {
                logger.log("store_properties Exception: " + Stack_trace_getter.get_stack_trace_for_throwable(e));
                return;
            }
            if (dbg) logger.log("created file:"+f);
        }
        else
        {
            if (dbg) logger.log(" file exists:"+f);
        }

        if (!Files.isWritable(f))
        {
            Popups.popup_Exception(new AccessDeniedException(f.toAbsolutePath().toString()), 200, "Cannot store properties ", logger);
            logger.log("ALERT: cannot write properties in:" + f.toAbsolutePath());
            return;
        }
        else
        {
            if (dbg) logger.log(" file is writable:"+f);
        }

        try
        {
            FileOutputStream fos = new FileOutputStream(f.toFile());
            the_Properties.store(fos, "no comment");
            fos.close();
            if (dbg) logger.log(("ALL properties stored in:" + f.toAbsolutePath()));
        } catch (Exception e)
        {
            logger.log("store_properties Exception: " + Stack_trace_getter.get_stack_trace_for_throwable(e));
        }
    }

    //**********************************************************
    public void load_properties()
    //**********************************************************
    {
        if (dbg) logger.log("load_properties()");
        FileInputStream fis;
        try
        {

            if (Files.exists(f))
            {
                if (!Files.isReadable(f))
                {
                    logger.log("cannot read properties from:" + f.toAbsolutePath());
                    return;
                }
                fis = new FileInputStream(f.toFile());
                the_Properties.load(fis);
                if (dbg) logger.log("properties loaded from:" + f.toAbsolutePath());
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
        return (String) the_Properties.get(key);
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

    /*
     * imperative store: if a previous event with the same key had been saved,
     * it will be erased
     */
    //**********************************************************
    public void imperative_store(String key, String value, boolean with_age, boolean and_save)
    //**********************************************************
    {
        if (dbg) logger.log("Static_application_properties: imperative_store " + key + "=" + value);

        the_Properties.put(key, value);
        LocalDateTime now = LocalDateTime.now();
        if (with_age) the_Properties.put(key + AGE, now.toString());

        if (and_save) store_properties();
    }

    //**********************************************************
    public void raw_put(String k, String v)
    //**********************************************************
    {
        if ( dbg) logger.log("raw_put " + k + " " + v);
        the_Properties.put(k, v);
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
     * SMART API: for a given keyword (base-key), can store up to "max" items
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
    public void add(String key, String value)
    //**********************************************************
    {
        imperative_store(key,value,false,true);
    }
    //**********************************************************
    public void delete(String key, boolean and_save, Logger logger)
    //**********************************************************
    {
        remove(key);
        if ( and_save) store_properties();
    }

    // list all stored values for a base-key
    //**********************************************************
    public List<Key_value> get_key_values_for_base(String key_base)
    //**********************************************************
    {
        List<Key_value> returned = new ArrayList<>();
        for (int i = 0; i < max; i++)
        {
            if (get(key_base + i) != null)
            {
                String b = get(key_base + i);
                returned.add(new Key_value(key_base + i, b));
            }
        }
        Collections.sort(returned);
        return returned;
    }

    //**********************************************************
    public String get_most_recent_value_for_base(String key_base)
    //**********************************************************
    {
        String returned = null;
        LocalDateTime most_recent = null;
        for (int i = 0; i < max; i++)
        {
            String candidate = get(key_base + i);
            String date = get(key_base + i + AGE);
            if (date == null)
            {
                returned = candidate;
                break;
            }
            LocalDateTime ld = LocalDateTime.parse(date);
            if (most_recent == null)
            {
                most_recent = ld;
                returned = candidate;
            }
            else
            {
                if (ld.isAfter(most_recent))
                {
                    most_recent = ld;
                    returned = candidate;
                }

            }
        }
        return returned;
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

        imperative_store(key, value, with_age, true);
        return key;
    }

    // saves a value for a base-key, handling oldest-replacement silently
    // does NOT look for a free seat: the key is the full key
    //**********************************************************
    public boolean save_unico(String full_key, String value, boolean with_age)
    //**********************************************************
    {
        imperative_store(full_key, value, with_age, true);
        return true;
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
        Logger logger = new System_out_logger();
        Properties_manager pm = new Properties_manager(f_.toPath(), logger);

        for (int i = 0; i < 15; i++)
        {
            String s = pm.get_one_empty_key_for_base(TOTO);
            String value = "value for " + s;
            pm.imperative_store(s, value, true, true);
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
        pm.imperative_store(s, value, true, true);
        logger.log(s + " is now key for: " + value);
    }


}