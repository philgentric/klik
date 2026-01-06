// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.properties;

import klikr.util.log.Logger;

import java.util.ArrayList;
import java.util.List;

//**********************************************************
public class Properties_with_base
//**********************************************************
{
    private final String key_base; // base name in properties file
    private final IProperties ip;
    private final int max;

    //**********************************************************
    public Properties_with_base(IProperties ip, String key_base, int max, Logger logger)
    //**********************************************************
    {
        this.max = max;
        this.key_base = key_base;
        this.ip = ip;
    }

    //**********************************************************
    public List<String> get_all()
    //**********************************************************
    {
        List<String> returned = new ArrayList<>();
        for (int i = 0; i < max; i++)
        {
            String path = ip.get(key_base + i);
            //System.out.println((key_base + i)+"->"+path+"<-");
            if (path != null) returned.add(path);
        }
        return returned;
    }


    //**********************************************************
    public void add(String s)
    //**********************************************************
    {
        if ( is_already_there(s)) return;
        System.out.println(" ADDING: ->"+key_base+"<-"+s);
        for (int i = 0; i < max; i++)
        {
            String path = ip.get(key_base + i);
            if ( path == null)
            {
                // free slot
                System.out.println("bookmark found free slot: ->"+i+"<-");
                ip.set(key_base+i, s);
                return;
            }
        }
        // no more free slots, let us remove the last one
        ip.set(key_base+(max-1), s);
        System.out.println("bookmark no free slot for: ->"+s+"<-");

    }


    //**********************************************************
    private boolean is_already_there(String candidate)
    //**********************************************************
    {
        System.out.println("bookmark is_already_there?: ->"+candidate+"<-");

        for (String k : ip.get_all_keys())
        {
            if (!k.startsWith(key_base))
            {
                System.out.println("k not a "+key_base+"->"+k+"<-");
                continue;
            }
            System.out.println("k is a "+key_base+"->"+k+"<-");
            String v = ip.get(k);
            if (v == null) continue;
            System.out.println("k is a "+key_base+"->"+k+"<-"+v);

            if (v.equals(candidate)) return true;
        }
        return false;
    }

    //**********************************************************
    public void clear()
    //**********************************************************
    {
        System.out.println("bookmark clearing: ->"+key_base+"<-");
        ip.clear();
    }
}
