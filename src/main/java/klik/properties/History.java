package klik.properties;

import klik.level2.backup.Backup_engine;
import klik.util.Logger;
import klik.util.Stack_trace_getter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static klik.properties.Properties_manager.AGE;

//**********************************************************
public class History
//**********************************************************
{
    public static final boolean dbg = false;
    public static final int max = 30;
    public static final String HISTORY_OF_DIRS = "HISTORY_OF_DIRS";
    private List<History_item> cache;
    private final Properties_manager pm;
    Logger logger;

    //**********************************************************
    public History(Logger logger)
    //**********************************************************
    {
        this.logger = logger;
        //logger.log("History constructor");
        pm = Static_application_properties.get_properties_manager(logger);
    }

    //**********************************************************
    public List<History_item> get_history_from(Properties_manager pm)
    //**********************************************************
    {
        List<History_item> returned = new ArrayList<>();
        for (String k : pm.get_all_keys()) {
            if (!k.startsWith(HISTORY_OF_DIRS)) continue;
            if (k.endsWith(AGE)) continue;
            String path = pm.get(k);
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
            date = pm.get(make_key_for_age(uuid));
            if ( date == null)
            {
                logger.log("WARNING:  cannot get date from uuid="+uuid+" removing item from history");
                pm.remove(k);
                continue;
            }
            if (Files.exists(Path.of(path)))
            {
                returned.add(new History_item(path, date, uuid));
            } else {
                manage_gone_history_item(pm, k, path);
            }
        }
        pm.store_properties();
        returned.sort(History_item.comparator_by_date);
        return returned;
    }

    //**********************************************************
    private void manage_gone_history_item(Properties_manager pm, String k, String path)
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
                pm.remove(k);
                return;
            }
        }

    }

    //**********************************************************
    private String make_key(UUID uuid)
    //**********************************************************
    {
        return HISTORY_OF_DIRS + uuid;
    }

    //**********************************************************
    private String make_key_for_age(UUID uuid)
    //**********************************************************
    {
        return HISTORY_OF_DIRS + uuid + AGE;
    }

    //**********************************************************
    private UUID extract_uuid_from_key(String k)
    //**********************************************************
    {
        if (dbg) logger.log("extract_uuid_from_key : " + k);
        String uuid_s = k.substring(HISTORY_OF_DIRS.length());
        if (dbg) logger.log("uuid string = " + uuid_s);
        try {
            return UUID.fromString(uuid_s);
        } catch (IllegalArgumentException e) {
            logger.log(Stack_trace_getter.get_stack_trace_for_throwable(e));
        }
        return null;
    }

    //**********************************************************
    public static History get_History_instance(Logger logger)
    //**********************************************************
    {
        return new History(logger);
    }


    //**********************************************************
    public List<History_item> get_list()
    //**********************************************************
    {
        if ( cache == null) cache = get_history_from(pm);
        cache.sort(History_item.comparator_by_date);
        return cache;
    }


    //**********************************************************
    public void add(Path p)
    //**********************************************************
    {
        //if (is_already_there(p)) return;


        History_item new_item = new History_item(p.toAbsolutePath().toString(), LocalDateTime.now());
        pm.raw_put(make_key(new_item.uuid), p.toAbsolutePath().toString());
        pm.raw_put(make_key_for_age(new_item.uuid), new_item.time_stamp.toString());
        //pm.raw_put(HISTORY_OF_DIRS+new_item.uuid+_UUID,new_item.uuid.toString());

        if ( cache == null) cache = new ArrayList<>();
        cache.add(new_item);
        if (dbg) logger.log("History adding: " + new_item.to_string());

        cache.sort(History_item.comparator_by_date);

        if (cache.size() > max) {
            History_item ii = cache.remove(cache.size() - 1);

            for (String k : pm.get_all_keys()) {
                if (!k.startsWith(HISTORY_OF_DIRS)) continue;
                if (k.endsWith(AGE)) continue;
                UUID uuid = extract_uuid_from_key(k);
                if (uuid == null) continue;
                if (uuid.equals(ii.uuid)) {
                    pm.remove(k);
                    break;
                }
            }
        }
        pm.store_properties();
    }



    //**********************************************************
    public static void clear(Logger logger)
    //**********************************************************
    {
        get_History_instance(logger).clear_internal();
        Backup_engine.remove_all_properties(logger);
    }

    //**********************************************************
    public void clear_internal()
    //**********************************************************
    {
        for (String k : pm.get_all_keys())
        {
            if (!k.startsWith(HISTORY_OF_DIRS)) continue;
            pm.remove(k);
        }
        pm.store_properties();
        cache = null;
    }


}
