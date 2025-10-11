package klik.change.history;

import klik.properties.IProperties;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static klik.properties.Properties_manager.AGE;

//**********************************************************
public class Properties_for_history
//**********************************************************
{
    private final static boolean dbg = false;
    private final Logger logger;
    private final IProperties ip;
    private final int max;

    //**********************************************************
    public Properties_for_history(IProperties ip, int max, Logger logger)
    //**********************************************************
    {
        this.logger = logger;
        this.max = max;
        this.ip = ip;
    }

    /*
    //**********************************************************
    public void erase_if_too_old(int days)
    //**********************************************************
    {
        LocalDateTime now = LocalDateTime.now();
        for (String k : ip.get_all_keys())
        {
            //if (!k.startsWith(HISTORY_OF_DIRS)) continue;
            if (k.endsWith(AGE)) continue;
            String path = ip.get(k);
            if (path == null) {
                logger.log("WEIRD history key failed?");
                continue;
            }
            UUID uuid = extract_uuid_from_key(k);
            if (uuid == null) {
                logger.log("WEIRD cannot get UUID from key?");
                continue;
            }
            String date = ip.get(make_key_for_age(uuid));
            if ( date == null)
            {
                logger.log("WARNING:  cannot get date from uuid="+uuid+" removing item from history");
                ip.remove(k);
                continue;
            }

            LocalDateTime ldt = LocalDateTime.parse(date);
            Duration d = Duration.between(ldt,now);
            if ( d.toDays() > days)
            {
                ip.remove(k);
                ip.remove(make_key_for_age(uuid));
            }
        }
        
    }
*/


    //**********************************************************
    public void add_and_prune(String tag)
    //**********************************************************
    {
        History_item new_item = new History_item(tag, LocalDateTime.now());
        ip.set(tag, tag);

        List<History_item> all_history_items = get_all_history_items();
        all_history_items.add(new_item);
        if (dbg) logger.log("History adding: " + new_item.to_string());
        all_history_items.sort(History_item.comparator_by_date);

        // maintain a short history:
        if (all_history_items.size() > max)
        {
            History_item last = all_history_items.remove(all_history_items.size() - 1);
            for (String k : ip.get_all_keys())
            {
                if (k.endsWith(AGE)) continue;
                if (k.equals(last.value)) {
                    ip.remove(k);
                    break;
                }
            }
        }
    }
    //**********************************************************
    public List<History_item> get_all_history_items()
    //**********************************************************
    {
        List<History_item> returned = new ArrayList<>();
        for (String k : ip.get_all_keys())
        {
            if (k.endsWith(AGE)) continue;
            String v = ip.get(k);
            if (v == null) {
                logger.log("WEIRD history key failed?");
                continue;
            }
            String age_s = ip.get(k + AGE);
            if ( age_s == null)
            {
                logger.log("WEIRD cannot get age from key?"+k);
                continue;
            }
            LocalDateTime ts = LocalDateTime.parse(age_s);
            if (ts == null) {
                logger.log("WEIRD cannot get timestamp from key?");
                continue;
            }
            History_item hi = new History_item(v, ts);
            hi.set_available(Files.exists(Path.of(v)));
            returned.add(hi);
        }
        Collections.sort(returned,History_item.comparator_by_date);
        return returned;
    }

    //**********************************************************
    public void clear()
    //**********************************************************
    {
        System.out.println("clearing history");
        ip.clear();
    }


/*


    //**********************************************************
    public void add_and_prune(String tag)
    //**********************************************************
    {
        remove_if_present(tag);
        History_item new_item = new History_item(tag, LocalDateTime.now());
        ip.set(make_key(new_item.uuid), tag);
        ip.set(make_key_for_age(new_item.uuid), new_item.time_stamp.toString());

        List<History_item> all_history_items = get_all_history_items();
        all_history_items.add(new_item);
        if (dbg) logger.log("History adding: " + new_item.to_string());

        all_history_items.sort(History_item.comparator_by_date);

        // maintain a short history:
        if (all_history_items.size() > max)
        {
            History_item ii = all_history_items.remove(all_history_items.size() - 1);
            for (String k : ip.get_all_keys())
            {
                if (k.endsWith(AGE)) continue;
                UUID uuid = extract_uuid_from_key(k);
                if (uuid == null) continue;
                if (uuid.equals(ii.uuid)) {
                    ip.remove(k);
                    break;
                }
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
    public List<History_item> get_all_history_items()
    //**********************************************************
    {
        List<History_item> returned = new ArrayList<>();
        for (String k : ip.get_all_keys())
        {
            //if (!k.startsWith(HISTORY_OF_DIRS)) continue;
            if (k.endsWith(AGE)) continue;
            String path = ip.get(k);
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
            date = ip.get(make_key_for_age(uuid));
            if ( date == null)
            {
                logger.log("WARNING:  cannot get date from uuid="+uuid+" removing item from history");
                ip.remove(k);
                continue;
            }
            History_item hi = new History_item(path, date, uuid);
            returned.add(hi);
            hi.set_available(Files.exists(Path.of(path)));
        }
        returned.sort(History_item.comparator_by_date);
        return returned;
    }

    //**********************************************************
    private void remove_if_present(String tag)
    //**********************************************************
    {
        for (String k : ip.get_all_keys())
        {
            if (k.endsWith(AGE)) continue;
            String path = ip.get(k);
            if (path.equals(tag) )
            {
                if (dbg) logger.log("History engine: "+tag+" already present in history, removed");
                ip.remove(k);
                UUID uuid = extract_uuid_from_key(k);
                if (uuid != null) {
                    ip.remove(make_key_for_age(uuid));
                }
            }
        }
        
    }





    //**********************************************************
    public List<String> get_all(String key_base)
    //**********************************************************
    {
        List<String> returned = new ArrayList<>();
        for (int i = 0; i < max; i++)
        {
            String path = ip.get(key_base + i);
            logger.log(" item: "+(key_base + i)+"->"+path+"<-");
            if (path != null) returned.add(path);
        }
        return returned;
    }




    //**********************************************************
    private boolean is_already_there(String candidate)
    //**********************************************************
    {
        logger.log(" is_already_there?: ->"+candidate+"<-");

        for (String k : ip.get_all_keys())
        {
            if (!k.startsWith(key_base))
            {
                logger.log("k not a "+key_base+"->"+k+"<-");
                continue;
            }
            logger.log("k is a "+key_base+"->"+k+"<-");
            String v = ip.get(k);
            if (v == null) continue;
            logger.log("k is a "+key_base+"->"+k+"<-"+v);

            if (v.equals(candidate)) return true;
        }
        return false;
    }

    //**********************************************************
    public void clear()
    //**********************************************************
    {
        logger.log("clearing: ->"+key_base+"<-");
        for (int i = 0; i < max; i++)
        {
            String v = ip.get(key_base + i);
            if ( v == null) continue;
            logger.log( "clearing slot: ->"+i+"<-");
            ip.remove(key_base+i);
        }
    }
    */

}
