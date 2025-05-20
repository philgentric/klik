package klik.properties;

import java.util.List;

public interface IProperties
{
    boolean set(String key, String value);
    String get(String key);
    void remove(String key);
    void clear(String keyBase);
    List<String> get_all_keys();
}
