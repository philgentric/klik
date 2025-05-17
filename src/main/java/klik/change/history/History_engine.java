//SOURCES ../../unstable/backup/Backup_engine.java
package klik.change.history;

import klik.actor.Aborter;
import klik.browser.Shared_services;
import klik.properties.Non_booleans;
import klik.experimental.backup.Backup_engine;
import klik.properties.Properties_manager;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static klik.properties.Properties_manager.AGE;

//**********************************************************
public class History_engine
//**********************************************************
{
    public static final boolean dbg = false;
    public static final String HISTORY_FILENAME = "history.properties";
    private static Properties_manager global_history_properties_manager;
    Logger logger;
    private final static int max = 100;
    private static History_engine instance;

    //**********************************************************
    public static History_engine get_instance( Logger logger)
    //**********************************************************
    {
        if ( instance == null) instance = new History_engine(logger);
        return instance;
    }

    //**********************************************************
    private History_engine( Logger logger)
    //**********************************************************
    {
        this.logger = logger;
        if ( global_history_properties_manager == null)
        {
            String home = System.getProperty(Non_booleans.USER_HOME);
            Path p = Paths.get(home, Non_booleans.CONF_DIR, HISTORY_FILENAME);
            global_history_properties_manager = new Properties_manager(p, "History DB", Shared_services.shared_services_aborter, logger);
        }
    }

    //**********************************************************
    public static void clear(Aborter aborter, Logger logger)
    //**********************************************************
    {
        get_instance(logger).clear_all_internal();
        Backup_engine.remove_all_properties(aborter,logger);
    }
    //**********************************************************
    public static void erase_if_too_old(int days, Logger logger)
    //**********************************************************
    {
        get_instance(logger).erase_if_too_old(days);
    }

    //**********************************************************
    public void erase_if_too_old(int days)
    //**********************************************************
    {
        LocalDateTime now = LocalDateTime.now();
        for (String k : global_history_properties_manager.get_all_keys())
        {
            //if (!k.startsWith(HISTORY_OF_DIRS)) continue;
            if (k.endsWith(AGE)) continue;
            String path = global_history_properties_manager.get(k);
            if (path == null) {
                logger.log("WEIRD history key failed?");
                continue;
            }
            UUID uuid = extract_uuid_from_key(k);
            if (uuid == null) {
                logger.log("WEIRD cannot get UUID from key?");
                continue;
            }
            String date = global_history_properties_manager.get(make_key_for_age(uuid));
            if ( date == null)
            {
                logger.log("WARNING:  cannot get date from uuid="+uuid+" removing item from history");
                global_history_properties_manager.remove(k);
                continue;
            }

            LocalDateTime ldt = LocalDateTime.parse(date);
            Duration d = Duration.between(ldt,now);
            if ( d.toDays() > days)
            {
                global_history_properties_manager.remove(k);
                global_history_properties_manager.remove(make_key_for_age(uuid));
            }
        }
        global_history_properties_manager.store_properties();
    }




    //**********************************************************
    public List<History_item> get_all_history_items()
    //**********************************************************
    {
        List<History_item> returned = new ArrayList<>();
        for (String k : global_history_properties_manager.get_all_keys())
        {
            //if (!k.startsWith(HISTORY_OF_DIRS)) continue;
            if (k.endsWith(AGE)) continue;
            String path = global_history_properties_manager.get(k);
            if (path == null) {
                logger.log("WEIRD history key failed?");
                continue;
            }

            UUID uuid = extract_uuid_from_key(k);
            if (uuid == null) {
                logger.log("WEIRD cannot get UUID from key?");
                continue;
            }
            String date;
            date = global_history_properties_manager.get(make_key_for_age(uuid));
            if ( date == null)
            {
                logger.log("WARNING:  cannot get date from uuid="+uuid+" removing item from history");
                global_history_properties_manager.remove(k);
                continue;
            }
            History_item hi = new History_item(path, date, uuid);
            returned.add(hi);
            hi.set_available(Files.exists(Path.of(path)));
        }
        global_history_properties_manager.store_properties();
        returned.sort(History_item.comparator_by_date);
        return returned;
    }

    //**********************************************************
    private void manage_gone_history_item(String key, String path)
    //**********************************************************
    {
        Path p = Path.of(path);
        for (;;)
        {
            p = p.getParent();
            if (p == null) {
                logger.log("history: path " + path + " not found, not shown  (could be a removed external drive?)");
                return;
            }
            if (p.toFile().exists())
            {
                // this is OS dependent i.e. /Volumes is typical of macOS
                if (p.getFileName().toString().equals("/Volumes"))
                {
                    logger.log("history: path " + path + " not found, not shown  (probably removed external drive?)");
                    return;
                }
                if (dbg) logger.log("history: path " + path + "  REMOVED from history since it does not exist but the parent does");
                global_history_properties_manager.remove(key);
                return;
            }
        }

    }

    //**********************************************************
    private String make_key(UUID uuid)
    //**********************************************************
    {
        return  uuid.toString();
    }

    //**********************************************************
    private String make_key_for_age(UUID uuid)
    //**********************************************************
    {
        return  uuid.toString() + AGE;
    }

    //**********************************************************
    private UUID extract_uuid_from_key(String k)
    //**********************************************************
    {
        if (dbg) logger.log("extract_uuid_from_key : " + k);
        String uuid_s = k;//.substring(HISTORY_OF_DIRS.length());
        if (dbg) logger.log("uuid string = " + uuid_s);
        try {
            return UUID.fromString(uuid_s);
        } catch (IllegalArgumentException e) {
            logger.log(Stack_trace_getter.get_stack_trace_for_throwable(e));
        }
        return null;
    }



    //**********************************************************
    public void add(String tag)
    //**********************************************************
    {
        remove_if_present(tag);
        History_item new_item = new History_item(tag, LocalDateTime.now());
        global_history_properties_manager.add(make_key(new_item.uuid), tag);
        global_history_properties_manager.add(make_key_for_age(new_item.uuid), new_item.time_stamp.toString());

        List<History_item> all_history_items = get_all_history_items();
        all_history_items.add(new_item);
        if (dbg) logger.log("History adding: " + new_item.to_string());

        all_history_items.sort(History_item.comparator_by_date);



        // maintain a short history:
        if (all_history_items.size() > max)
        {
            History_item ii = all_history_items.remove(all_history_items.size() - 1);
            for (String k : global_history_properties_manager.get_all_keys())
            {
                if (k.endsWith(AGE)) continue;
                UUID uuid = extract_uuid_from_key(k);
                if (uuid == null) continue;
                if (uuid.equals(ii.uuid)) {
                    global_history_properties_manager.remove(k);
                    break;
                }
            }
        }
        global_history_properties_manager.store_properties();
    }

    //**********************************************************
    private void remove_if_present(String tag)
    //**********************************************************
    {
        for (String k : global_history_properties_manager.get_all_keys())
        {
            if (k.endsWith(AGE)) continue;
            String path = global_history_properties_manager.get(k);
            if (path.equals(tag) )
            {
                if (dbg) logger.log("History engine: "+tag+" already present in history, removed");
                global_history_properties_manager.remove(k);
                UUID uuid = extract_uuid_from_key(k);
                if (uuid != null) {
                    global_history_properties_manager.remove(make_key_for_age(uuid));
                }
            }
        }
        global_history_properties_manager.store_properties();
    }


    //**********************************************************
    public void clear_all_internal()
    //**********************************************************
    {
        for (String k : global_history_properties_manager.get_all_keys())
        {
            //if (!k.startsWith(HISTORY_OF_DIRS)) continue;
            global_history_properties_manager.remove(k);
        }
        global_history_properties_manager.store_properties();
    }


}
