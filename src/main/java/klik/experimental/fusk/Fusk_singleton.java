//SOURCES ./Defusk_actor_for_one_folder.java
//SOURCES ./Fusk_actor_for_one_folder.java
package klik.experimental.fusk;

import klik.actor.Aborter;
import klik.util.log.Logger;

import java.nio.file.Path;

import static klik.experimental.fusk.Defusk_actor_for_one_folder.defusk_this_folder;
import static klik.experimental.fusk.Fusk_actor_for_one_folder.fusk_this_folder;

//**********************************************************
public class Fusk_singleton
//**********************************************************
{
    private static Fusk_singleton instance;
    public final Logger logger;
    Path source;
    Path destination;
    private Aborter aborter;
    //Aborter aborter = new Aborter();

    //**********************************************************
    public Fusk_singleton(Aborter aborter, Logger logger)
    //**********************************************************
    {
        this.aborter = aborter;
        this.logger = logger;
    }

    //**********************************************************
    public static void abort()
    //**********************************************************
    {
        instance.aborter.abort("fusk instance abort");
    }


    //**********************************************************
    public static void set_source(Path fusk_source, Aborter aborter, Logger logger)
    //**********************************************************
    {
        Fusk_singleton b = get_instance(aborter,logger);
        instance.source = fusk_source;
    }

    //**********************************************************
    private static Fusk_singleton get_instance(Aborter aborter, Logger logger)
    //**********************************************************
    {
        if ( instance == null)
        {
            instance = new Fusk_singleton(aborter, logger);
        }
        return instance;
    }

    //**********************************************************
    public static void set_destination(Path fusk_destination, Aborter aborter, Logger logger)
    //**********************************************************
    {
        Fusk_singleton b = get_instance(aborter, logger);
        instance.destination = fusk_destination;
    }

    //**********************************************************
    public static boolean start_fusk()
    //**********************************************************
    {
        if ( instance == null) return false;
        fusk_this_folder(instance.source.toFile(), instance.destination.toFile(), instance.aborter,instance.logger);

        return true;
    }

    //**********************************************************
    public static boolean start_defusk()
    //**********************************************************
    {
        if ( instance == null) return false;
        defusk_this_folder(instance.source.toFile(), instance.destination.toFile(), instance.aborter, instance.logger);
        return true;
    }
}
