package klik.browser.virtual_landscape;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

//**********************************************************
public interface Path_list_provider
//**********************************************************
{
    String get_name(); // absolute path if true folder
    List<Path> get_paths();
    Path resolve(String string);

    List<File> get_file_list(); // only files, no folders
}
