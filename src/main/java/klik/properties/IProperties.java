package klik.properties;

import java.util.List;

public interface IProperties
{
    boolean set(String key, String value);
    String get(String key);
    void remove(String key);
    List<String> get_all_keys();
    String get_tag();
    void clear();
    void force_reload_from_disk();
}
