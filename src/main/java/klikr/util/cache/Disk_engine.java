package klikr.util.cache;

import java.nio.file.Path;
import java.util.Map;

//**********************************************************
public interface Disk_engine<K,V>
//**********************************************************
{

    int load_from_disk(Map<String, V> cache);
    int save_to_disk(Map<String, V> cache);
}
