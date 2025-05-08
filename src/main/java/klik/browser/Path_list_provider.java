package klik.browser;

import java.nio.file.Path;
import java.util.List;

//**********************************************************
public interface Path_list_provider
//**********************************************************
{
    String get_name();
    List<Path> get_paths();
    Path resolve(String string);
}
