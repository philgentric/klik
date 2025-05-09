package klik.browser;

import klik.browser.virtual_landscape.Path_list_provider;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

//**********************************************************
public class Folder_path_list_provider implements Path_list_provider
//**********************************************************
{
    private final Path folder_path;

    //**********************************************************
    public Folder_path_list_provider(Path folder_path)
    //**********************************************************
    {
        this.folder_path = folder_path;
    }

    //**********************************************************
    @Override
    public String get_name()
    //**********************************************************
    {
        return folder_path.toAbsolutePath().toString();
    }

    //**********************************************************
    @Override
    public List<Path> get_paths()
    //**********************************************************
    {
        File f = folder_path.toFile();
        File[] files = f.listFiles();
        if ( files == null) return new ArrayList<>();
        List<Path> returned = new ArrayList<>();
        for (File file : files) {
            returned.add(file.toPath());
        }
        return returned;
    }

    //**********************************************************
    @Override
    public Path resolve(String string)
    //**********************************************************
    {
        return folder_path.resolve(string);
    }

    //**********************************************************
    @Override
    public List<File> get_file_list()
    //**********************************************************
    {
        File f = folder_path.toFile();
        File[] files = f.listFiles();
        if ( files == null) return new ArrayList<>();
        List<File> returned = new ArrayList<>();
        for (File file : files)
        {
            if( file.isDirectory() ) continue;
            returned.add(file);
        }
        return returned;
    }
}
