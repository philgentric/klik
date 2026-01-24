package klikr.util.cache;

import javafx.stage.Window;
import klikr.util.execute.actor.Aborter;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;

import java.io.*;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;

//**********************************************************
public class Binary_file_engine<K,V> implements Disk_engine<K,V>
//**********************************************************
{
    private static final boolean dbg = false;
    private static final boolean ultra_dbg = false;

    public final String name;
    public final Path cache_file_path;
    public final BiPredicate<K, DataOutputStream> key_serializer;
    public final Function<DataInputStream, K> key_deserializer;
    public final BiPredicate<V, DataOutputStream> value_serializer;
    public final Function<DataInputStream, V> value_deserializer;
    public final Function<K,String> string_key_maker;
    public final Function<String,K> object_key_maker;
    public final Aborter aborter;
    public final Window owner;
    public final Logger logger;

    //**********************************************************
    public Binary_file_engine(
            String name,
            Path cache_file_path,
            BiPredicate<K, DataOutputStream> key_serializer,
            Function<DataInputStream, K> key_deserializer,
            BiPredicate<V, DataOutputStream> value_serializer,
            Function<DataInputStream, V> value_deserializer,
            Function<K,String> string_key_maker,
            Function<String,K> object_key_maker,
            Window owner, Aborter aborter, Logger logger
    )
    //**********************************************************
    {
        this.cache_file_path = cache_file_path;
        this.key_serializer = key_serializer;
        this.key_deserializer = key_deserializer;
        this.value_serializer = value_serializer;
        this.value_deserializer = value_deserializer;
        this.string_key_maker = string_key_maker;
        this.object_key_maker = object_key_maker;
        this.logger = logger;
        this.name = name;
        this.aborter = aborter;
        this.owner = owner;
    }

    //**********************************************************
    @Override
    public int load_from_disk(Map<String, V> cache)
    //**********************************************************
    {
        long start = System.currentTimeMillis();

        int reloaded = 0;
        try(DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(cache_file_path.toFile()))))
        {
            int number_of_items = dis.readInt();
            if (dbg) logger.log("number_of_items in cache :"+number_of_items);

            for ( int k = 0; k < number_of_items; k++)
            {
                if ( aborter.should_abort())
                {
                    logger.log("aborting cal reload "+aborter.reason());
                    return reloaded;
                }
                K key = key_deserializer.apply(dis);
                if ( key == null)
                {
                    logger.log(Stack_trace_getter.get_stack_trace("FATAL"));
                    return reloaded;
                }
                if (ultra_dbg) logger.log("key "+key);
                V value = value_deserializer.apply(dis);
                if ( value == null)
                {
                    logger.log(Stack_trace_getter.get_stack_trace("FATAL"));
                    return reloaded;
                }
                if (ultra_dbg) logger.log("value "+value);

                String real_key = string_key_maker.apply(key);
                cache.put(real_key,value);
                if ( k%10000 == 0) logger.log(k +" items loaded from disk ....");
                reloaded++;
            }
            logger.log("Done: "+reloaded+" items loaded from disk");
            return reloaded;
        }
        catch (FileNotFoundException e)
        {
            logger.log("first time in this folder: "+e);
        }
        catch (IOException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
        }
        logger.log("reloading "+reloaded+" similarities from disk took "+(System.currentTimeMillis()-start)+" ms");

        return reloaded;
    }

    //**********************************************************
    @Override
    public int save_to_disk(Map<String, V> cache)
    //**********************************************************
    {

        int saved = 0;
        try(DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(cache_file_path.toFile()))))
        {
            dos.writeInt(cache.size());
            for(Map.Entry<String, V> e : cache.entrySet())
            {
                K object_key = object_key_maker.apply(e.getKey());
                if ( !key_serializer.test(object_key,dos))
                {
                    logger.log(Stack_trace_getter.get_stack_trace(" Panic"));
                    break;
                }
                if ( !value_serializer.test(e.getValue(),dos))
                {
                    logger.log(Stack_trace_getter.get_stack_trace(" Panic"));
                    break;
                }
                saved++;
                //logger.log("to disk similarity "+e.getValue()+" for "+pi1.getFileName().toString()+" "+pi2.getFileName().toString());
            }
        }
        catch (IOException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
        }

        if (dbg) logger.log(saved +" items from cache saved to file");
        return saved;
    }

}
