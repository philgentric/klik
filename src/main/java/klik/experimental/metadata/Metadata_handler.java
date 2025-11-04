// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.experimental.metadata;

import javafx.stage.Window;
import javafx.util.Pair;
import klik.util.execute.actor.Aborter;
import klik.properties.Properties_manager;
import klik.util.log.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

//**********************************************************
public class Metadata_handler
//**********************************************************
{
    public final Path described_file;
    private final Properties_manager properties_manager;

    //**********************************************************
    public Metadata_handler(Path described_file_, Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        described_file = described_file_;
        Path p = make_metadata_path(described_file_);
        properties_manager = new Properties_manager(p,"metadata for "+described_file,owner,aborter,logger);
    }

    //**********************************************************
    public static Path make_metadata_path(Path p)
    //**********************************************************
    {
        return Paths.get(p.getParent().toAbsolutePath().toString(),p.getFileName().toString()+".properties");
    }

    //**********************************************************
    public void add(String key, String value)
    //**********************************************************
    {
        properties_manager.add(key,value);
    }

    //**********************************************************
    public String get(String key)
    //**********************************************************
    {
        return properties_manager.get(key);
    }

    //**********************************************************
    public String get(String key, String replace_with_this_if_not_found)
    //**********************************************************
    {
        return properties_manager.get(key,replace_with_this_if_not_found);
    }


    //**********************************************************
    public Set<String> get_all_keys()
    //**********************************************************
    {
        return properties_manager.get_all_keys();
    }

    //**********************************************************
    public List<Pair<String,String>> get_pairs_for_base(String base)
    //**********************************************************
    {
        return properties_manager.get_pairs_for_base(base);
    }

    //**********************************************************
    public Pair<String,String> add_for_base(String base, String value)
    //**********************************************************
    {
        String key = properties_manager.save_multiple(base,value);
        return new Pair<>(key,value);
    }

    //**********************************************************
    public void delete(String key, boolean and_save, Logger logger)
    //**********************************************************
    {
        properties_manager.remove(key);
        if ( and_save) properties_manager.store_properties(false);
    }
}
