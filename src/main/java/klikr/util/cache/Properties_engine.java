package klikr.util.cache;

import javafx.stage.Window;
import klikr.properties.Properties_manager;
import klikr.util.execute.actor.Aborter;
import klikr.util.log.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

//**********************************************************
public class Properties_engine<V> implements Disk_engine<String, V>
//**********************************************************
{
    private static final boolean dbg = false;
    private final String name;
    private final Logger logger;
    private final Function<String,V> string_deserializer;
    private final Function<V,String> string_serializer;
    protected final Properties_manager pm;

    //**********************************************************
    public Properties_engine(
            String name,
            Path folder,
            Function<V, String> string_serializer,
            Function<String, V> string_deserializer,
            Window owner,
            Aborter aborter,
            Logger logger)
    //**********************************************************
    {
        this.name = name;
        this.logger = logger;
        this.string_deserializer = string_deserializer;
        this.string_serializer = string_serializer;

        Path cache_file_path = Path.of(folder.toAbsolutePath().toString(),name);
        pm = new Properties_manager(cache_file_path,name+" cache for folder "+folder,owner, aborter,logger);

    }

    //**********************************************************
    @Override
    public int load_from_disk(Map<String, V> cache)
    //**********************************************************
    {
        int reloaded = 0;
        int already_in_RAM = 0;
        List<String> cleanup = new ArrayList<>();

        // scan disk
        for(String key : pm.get_all_keys())
        {
            if ( dbg) logger.log("RAM cache reloading with key: "+key);

            String string_value = pm.get(key);
            if (cache.get(key) == null)
            {
                // not in RAM?! let us add it
                V value = string_deserializer.apply(string_value);
                if ( value == null)
                {
                    //the value 'on disk' is invalid
                    cleanup.add(key);
                }
                if ( dbg) logger.log("RAM cache deserializer : for type:"+value.getClass().getName()+" "+key+" => "+ string_serializer.apply(value));
                cache.put(key, value);
                reloaded++;
                if ( dbg) logger.log("RAM cache items reloaded : "+reloaded);
            }
            else
            {
                already_in_RAM++;
                if ( dbg) logger.log("already in RAM : "+key+" => "+ cache.get(key));
            }
        }
        for ( String key:cleanup)
        {
            pm.remove(key);
        }
        if ( !cleanup.isEmpty()) pm.save_everything_to_disk(false);

        if ( dbg)
        {
            logger.log(name+": "+already_in_RAM+" already in RAM, "+reloaded+" items reloaded from file");
            logger.log("\n\n\n********************* "+name+ " CACHE************************");
            for (String s  : cache.keySet())
            {
                logger.log(s+" => "+cache.get(s));
            }
            logger.log("****************************************************************\n\n\n");
        }
        return reloaded;
    }


    //**********************************************************
    @Override
    public int save_to_disk(Map<String, V> cache)
    //**********************************************************
    {
        int saved = 0;
        for(Map.Entry<String, V> e : cache.entrySet())
        {
            saved++;
            pm.add(e.getKey(), string_serializer.apply(e.getValue()),false);
        }
        pm.save_everything_to_disk(false);
        if (dbg) logger.log(name+ " : "+saved +" items saved to file");
        return saved;
    }

    //**********************************************************
    public boolean save_one_to_disk(Path path, V value)
    //**********************************************************
    {
        return pm.add(Klikr_cache.path_to_key(path), string_serializer.apply(value),false);
    }

}
