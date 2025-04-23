package klik.browser;
import klik.properties.Booleans;
import klik.util.files_and_paths.Guess_file_type;
import klik.util.log.Logger;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Get_folder_files
{
    public record Folder_files(boolean error, File[] files){}
    public record Folder_paths(boolean error, List<Path> paths){}

    public static Folder_files get_folder_files(Browser browser, Logger logger)
    {
        File[] files = browser.displayed_folder_path.toFile().listFiles();
        if ( files == null)
        {
            return new Folder_files(true,new File[0]);
        }
        return new Folder_files(false,files);
    }

    public static int how_many_files(Browser browser, Logger logger)
    {
        File[] files = browser.displayed_folder_path.toFile().listFiles();
        if ( files == null)
        {
            return 0;
        }
        int how_many_files = files.length;
        if (!Booleans.get_boolean(Booleans.SHOW_HIDDEN_FILES,logger))
        {
            for (File f : files) {
                if (Guess_file_type.is_this_path_invisible_when_browsing(f.toPath())) {
                    how_many_files--;
                }
            }
        }
        return how_many_files;
    }

    public static Folder_paths get_folder_paths(Browser browser, Logger logger)
    {
        List<Path> returned =  new ArrayList<>();
        File[] files = browser.displayed_folder_path.toFile().listFiles();
        if ( files == null)
        {
            return new Folder_paths(true,returned);
        }
        for ( File f : files)
        {
            returned.add( f.toPath());
        }
        return new Folder_paths(false,returned);
    }
}
