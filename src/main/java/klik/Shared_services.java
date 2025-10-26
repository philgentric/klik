package klik;

import klik.util.execute.actor.Aborter;
import klik.properties.File_based_IProperties;
import klik.properties.IProperties;
import klik.properties.boolean_features.Booleans;
import klik.properties.boolean_features.Feature;
import klik.util.log.File_logger;
import klik.util.log.Logger;
import klik.util.log.Simple_logger;

//**********************************************************
public class Shared_services
//**********************************************************
{
    private static Aborter aborter;
    private static Logger logger;
    private static IProperties main_properties;

    public static Logger logger()
    {
        return logger;
    }
    public static Aborter aborter()
    {
        return aborter;
    }
    //**********************************************************
    public static IProperties main_properties()
    //**********************************************************
    {
        return main_properties;
    }
    //**********************************************************
    public static void init(String name)
    //**********************************************************
    {
        if (main_properties != null)
        {
            logger.log(" DONT create Shared_services more than once !");
            return;
        }

        Logger tmp_logger = new Simple_logger();
        aborter = new Aborter("Shared_services", tmp_logger);
        // this properties file holds both the Non-booleans AND the Booleans
        main_properties = new File_based_IProperties(name+" main properties", "klik", null, aborter, tmp_logger);
        logger = get_logger(name);
    }

    //**********************************************************
    public static Logger get_logger(String tag)
    //**********************************************************
    {
        Logger logger;
        if (Booleans.get_boolean(Feature.Log_to_file.name(), null))
        {
            logger = new File_logger(tag);
        }
        else
        {
            logger = new Simple_logger();
        }
        return logger;
    }

}
