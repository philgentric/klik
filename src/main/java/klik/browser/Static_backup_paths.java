package klik.browser;

import java.nio.file.Path;


//**********************************************************
public class Static_backup_paths
//**********************************************************
{
    private Path backup_sink = null;
    private Path backup_source = null;
    private static Static_backup_paths instance;

    //**********************************************************
    public static Path get_backup_source()
    //**********************************************************
    {
        if ( instance == null) return null;
        return instance.backup_source;
    }
    //**********************************************************
    public static Path get_backup_sink()
    //**********************************************************
    {
        if ( instance == null) return null;
        return instance.backup_sink;
    }

    //**********************************************************
    public static void set_backup_sink(Path p)
    //**********************************************************
    {
        if ( instance == null) create_instance();
        instance.backup_sink = p;
    }
    //**********************************************************
    public static void set_backup_source(Path p)
    //**********************************************************
    {
        if ( instance == null) create_instance();
        instance.backup_source = p;
    }
    //**********************************************************
    private static void create_instance()
    //**********************************************************
    {
        instance = new Static_backup_paths();
    }
}
