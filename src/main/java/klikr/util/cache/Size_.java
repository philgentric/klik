package klikr.util.cache;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;

//**********************************************************
public class Size_
//**********************************************************
{

    //**********************************************************
    public static <K, V> long of_Map(
            Map<K, V> map,
            Function<K, Long> size_of_K,
            Function<V, Long> size_of_V)
    //**********************************************************
    {
        if (map == null || map.isEmpty()) {
            return 0;
        }

        long entryCount = map.size();

        // 32 bytes per HashMap.Node + ~4 bytes for the reference in the internal Node[] table
        long fixedOverheadPerEntry = 36;
        long totalMemory = entryCount * fixedOverheadPerEntry;

        for (Map.Entry<K, V> entry : map.entrySet()) {
            K key = entry.getKey();
            V value = entry.getValue();

            if (key != null) {
                totalMemory += size_of_K.apply(key);
            }
            if (value != null) {
                totalMemory += size_of_V.apply(value);
            }
        }

        return totalMemory;
    }

    //**********************************************************
    public static Function<Path,Long> of_Path_F()
    //**********************************************************
    {
        return p-> of_Path();
    }

    //**********************************************************
    public static long of_Path()
    //**********************************************************
    {
        return 32L;
    }

    //**********************************************************
    public static long of_Double()
    //**********************************************************
    {
        return Double.SIZE/8L;
    }

    //**********************************************************
    public static Function<Double, Long> of_Double_F()
    //**********************************************************
    {
        return d -> of_Double();
    }

    //**********************************************************
    public static long of_String(String s)
    //**********************************************************
    {
        long data_size = 2L *s.length();
        return  24+(16+data_size+7) & ~7;
    }

    //**********************************************************
    public static Function<String, Long> of_String_F()
    //**********************************************************
    {
        return s -> of_String(s);
    }


    //**********************************************************
    public static long of_enum()
    //**********************************************************
    {
        return 32L;
    }

    //**********************************************************
    public static Function<Long, Long> of_Long_F()
    //**********************************************************
    {
        return l -> of_Long();
    }


    //**********************************************************
    public static Long of_Long()
    //**********************************************************
    {
        return Long.SIZE/8L;
    }

    //**********************************************************
    public static Function<Integer, Long> of_Integer_F()
    //**********************************************************
    {
        return integer -> of_Integer();
    }

    //**********************************************************
    public static long of_Integer()
    //**********************************************************
    {
        return Integer.SIZE/8L;
    }

}
