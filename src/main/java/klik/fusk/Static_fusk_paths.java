package klik.fusk;

import java.nio.file.Path;


//**********************************************************
public class Static_fusk_paths
//**********************************************************
{
    private Path fusk_sink = null;
    private Path fusk_source = null;
    private static Static_fusk_paths instance;

    //**********************************************************
    public static Path get_fusk_source()
    //**********************************************************
    {
        if ( instance == null) return null;
        return instance.fusk_source;
    }
    //**********************************************************
    public static Path get_fusk_sink()
    //**********************************************************
    {
        if ( instance == null) return null;
        return instance.fusk_sink;
    }

    //**********************************************************
    public static void set_fusk_sink(Path p)
    //**********************************************************
    {
        if ( instance == null) create_instance();
        instance.fusk_sink = p;
    }
    //**********************************************************
    public static void set_fusk_source(Path p)
    //**********************************************************
    {
        if ( instance == null) create_instance();
        instance.fusk_source = p;
    }
    //**********************************************************
    private static void create_instance()
    //**********************************************************
    {
        instance = new Static_fusk_paths();
    }
}
