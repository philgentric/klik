// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.change.history;

import klikr.properties.File_storage;
import klikr.util.log.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

import static klikr.properties.File_storage_using_Properties.AGE;

//**********************************************************
public class Properties_for_history
//**********************************************************
{
    private final static boolean dbg = true;
    private final Logger logger;
    private final File_storage storage;
    private final int max;

    // back button management
    String current;
    Deque<String> stack = new ArrayDeque<>();
    List<String> back_trace = new ArrayList<>();

    //**********************************************************
    public Properties_for_history(File_storage storage, int max, Logger logger)
    //**********************************************************
    {
        this.logger = logger;
        this.max = max;
        this.storage = storage;
    }


    //**********************************************************
    public void add_and_prune(String tag)
    //**********************************************************
    {
        if ( current != null)
        {
            if ( !current.equals(tag))
            {
                //logger.log("pushing :"+current);
                if ( !back_trace.contains(tag))
                {
                    stack.push(current);
                    back_trace.clear();
                }
                else
                {
                    // this is a back move, dont record
                }
            }
        }
        current = tag;
        LocalDateTime now = LocalDateTime.now();
        History_item new_item = new History_item(tag, now);
        storage.set(tag, now.toString());
        storage.save_to_disk();

        List<History_item> all_history_items = get_all_history_items();
        all_history_items.add(new_item);
        if (dbg) logger.log("History adding: " + new_item.to_string());
        all_history_items.sort(History_item.comparator_by_date);

        // maintain a short history:
        if (all_history_items.size() > max)
        {
            History_item last = all_history_items.remove(all_history_items.size() - 1);
            for (String k : storage.get_all_keys())
            {
                if (k.endsWith(AGE)) continue;
                if (k.equals(last.value)) {
                    storage.remove(k);
                    break;
                }
            }
            storage.save_to_disk();
        }
    }
    //**********************************************************
    public List<History_item> get_all_history_items()
    //**********************************************************
    {
        List<History_item> returned = new ArrayList<>();
        for (String k : storage.get_all_keys())
        {
            if (k.endsWith(AGE)) continue;
            String age_s = storage.get(k);
            if ( age_s == null)
            {
                logger.log("WEIRD cannot get age from key?"+k);
                continue;
            }
            LocalDateTime ts = LocalDateTime.parse(age_s);
            if (ts == null) {
                logger.log("WEIRD cannot get timestamp from ->"+age_s+"<-");
                continue;
            }
            History_item hi = new History_item(k, ts);
            hi.set_available(Files.exists(Path.of(k)));
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
        storage.clear();
    }

    //**********************************************************
    public String get_back()
    //**********************************************************
    {
        try {
            String returned = stack.pop();
            logger.log("popping :" + returned);
            back_trace.add(returned);
            return returned;
        }
        catch( NoSuchElementException e)
        {
            return null;
        }
    }
}
