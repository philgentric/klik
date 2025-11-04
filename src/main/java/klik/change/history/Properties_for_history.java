// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

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

}
