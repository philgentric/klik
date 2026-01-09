package klikr.util.cache;

import klikr.util.execute.actor.Actor;
import klikr.util.execute.actor.Message;
import klikr.util.execute.actor.virtual_threads.Concurrency_limiter;
import klikr.util.log.Logger;

import java.nio.file.Path;
import java.util.function.Function;

//**********************************************************
public class RAM_cache_actor<K,V> implements Actor
//**********************************************************
{
    private static final boolean dbg = false;
    private static Concurrency_limiter cl;
    private final Logger logger;
    private final Function<K,V> value_extractor;


    //**********************************************************
    public RAM_cache_actor(Function<K,V> value_extractor, Logger logger)
    //**********************************************************
    {
        this.value_extractor = value_extractor;
        this.logger = logger;
        cl = null;
    }

    //**********************************************************
    public RAM_cache_actor(Function<K,V> value_extractor, double max_number_of_threads_per_core, Logger logger)
    //**********************************************************
    {
        this.value_extractor = value_extractor;
        this.logger = logger;
        cl =  new Concurrency_limiter("Ram cache actor",max_number_of_threads_per_core);

    }

    //**********************************************************
    @Override
    public String run(Message m)
    //**********************************************************
    {
        if ( cl != null)
        {
            try
            {
                cl.acquire();
            }
            catch (InterruptedException e)
            {
                logger.log(""+e);
                return "failed";
            }
        }
        RAM_cache_message<K,V> dm = (RAM_cache_message<K,V>) m;

        if ( dm.check_if_present)
        {
            if (dm.cache.get(dm.key,dm.aborter,null,dm.owner) != null)
            {
                if ( dbg) logger.log("RAM_cache_actor skipping prefill as object is in cache");
                return "OK";
            }
        }
        V value = value_extractor.apply(dm.key);
        dm.cache.inject(dm.key,value,false);
        if ( cl != null) cl.release();
        return "OK";
    }

    //**********************************************************
    @Override
    public String name()
    //**********************************************************
    {
        return "RAM_cache_actor";
    }


}
