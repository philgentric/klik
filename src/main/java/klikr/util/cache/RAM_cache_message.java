package klikr.util.cache;

import javafx.stage.Window;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Message;

//**********************************************************
public class RAM_cache_message<K,V> implements Message
//**********************************************************
{
    public final K key;
    public final Aborter aborter;
    public final Window owner;
    public final Klikr_cache<K,V> cache;
    public final boolean check_if_present;
    //**********************************************************
    public RAM_cache_message(K key, boolean check_if_present, Klikr_cache<K,V> cache, Aborter aborter, Window owner)
    //**********************************************************
    {
        this.cache = cache;
        this.key = key;
        this.aborter = aborter;
        this.check_if_present = check_if_present;
        this.owner = owner;
    }

    //**********************************************************
    @Override
    public String thread_name()
    //**********************************************************
    {
        return "Caching to RAM for key: " + key;
    }

    //**********************************************************
    @Override
    public Aborter get_aborter()
    //**********************************************************
    {
        return aborter;
    }
}