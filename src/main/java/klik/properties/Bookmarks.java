package klik.properties;

import klik.actor.Aborter;
import klik.util.log.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//**********************************************************
public class Bookmarks
//**********************************************************
{
    public static final int max = 50;
    public static final String BOOK_MARKS = "BOOK_MARKS";
    private final String key_base; // name of this bookmarks in properties file
    private final List<String> cache;
    private final Properties_manager pm;

    //**********************************************************
    public Bookmarks(String key_base_)
    //**********************************************************
    {
        key_base = key_base_;
        pm = Non_booleans.get_main_properties_manager();
        cache = get_bookmarks_of(pm, key_base);
    }


    //**********************************************************
    public static Bookmarks get_bookmarks()
    //**********************************************************
    {
        return new Bookmarks(BOOK_MARKS);
    }


    //**********************************************************
    public List<String> get_bookmarks_of(Properties_manager pm, String key_base)
    //**********************************************************
    {
        List<String> returned = new ArrayList<>();
        for (int i = 0; i < max; i++) {
            String path = pm.get(key_base + i);
            if (path == null) continue;
            returned.add(path);
        }
        return returned;
    }


    //**********************************************************
    public List<String> get_list()
    //**********************************************************
    {
        Collections.sort(cache);
        return cache;
    }

    //**********************************************************
    public void add(Path p)
    //**********************************************************
    {
        if ( is_already_there(p)) return;
        String s = p.toAbsolutePath().toString();
        cache.add(s);
        pm.save_multiple(key_base, s, false);
    }


    //**********************************************************
    private boolean is_already_there(Path candidate)
    //**********************************************************
    {
        for (String k : pm.get_all_keys()) {
            if (!k.startsWith(BOOK_MARKS)) continue;
            String x = pm.get(k);
            if (x == null) continue;
            Path p = Path.of(x);
            if (p.toAbsolutePath().toString().equals(candidate.toAbsolutePath().toString())) return true;
        }
        return false;
    }

    //**********************************************************
    public void clear()
    //**********************************************************
    {
        pm.clear(key_base);
    }
}
