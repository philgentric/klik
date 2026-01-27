// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.properties;

import java.time.LocalDateTime;
import java.util.List;

//**********************************************************
public interface File_storage
//**********************************************************
{
    boolean set(String key, String value);
    String get(String key);
    LocalDateTime get_age(String key);
    void remove(String key);
    List<String> get_all_keys();
    String get_tag();
    void clear();
    void reload_from_disk();
    void save_to_disk();
}
