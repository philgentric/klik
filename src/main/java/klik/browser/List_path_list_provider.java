package klik.browser;

import klik.browser.virtual_landscape.Path_list_provider;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class List_path_list_provider implements Path_list_provider
{
    public final String name;
    public final List<Path> paths = new ArrayList<>();

    public List_path_list_provider(String name) {
        this.name = name;
    }
    @Override
    public String get_name() {
        return name;
    }

    @Override
    public List<Path> get_paths() {
        return paths;
    }

    @Override
    public Path resolve(String string) {
        return Path.of(name+"/"+string);
    }

    @Override
    public List<File> get_file_list() {
        List<File> returned = new ArrayList<>();
        for ( Path p : paths)
        {
            if ( p.toFile().isDirectory()) continue;
            returned.add(p.toFile());
        }
        return returned;
    }
}
