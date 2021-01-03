package klik.I18N;

import klik.util.Logger;

import java.util.*;

public class I18n
{

    private ResourceBundle the_resource_bundle;

    private I18n(String language, String country, Logger logger)
    {
        Locale the_locale = new Locale(language,country);
        Locale.setDefault(the_locale);
        the_resource_bundle = ResourceBundle.getBundle("MessagesBundle", the_locale);
        String classpath  = System.getProperty("java.class.path");
        logger.log(" I18n found in classpath = "+classpath);
   }


    private String get_I18n_string_internal(String key, Logger logger)
    {
        try
        {
            return the_resource_bundle.getString(key);
        }
        catch (MissingResourceException e)
        {
            String classpath  = System.getProperty("java.class.path");
            logger.log("BADBADBAD I18n ->"+key+"<-\nnot found in classpath = "+classpath);
            logger.log("the_resource_bundle contains:");
            Enumeration<String> es = the_resource_bundle.getKeys();
            for (; es.hasMoreElements();)
            {
                String s = es.nextElement();
                logger.log("->"+s+"<-");
            }
            return key;
        }
    }


    // cached instance

    private static I18n cache = null;

    public static String get_I18n_string(String key, Logger logger)
    {
        if (cache == null)
        {
            Language language = Local_manager.get_instance();
            cache = new I18n(language.language, language.country, logger);
        }
        if ( cache.the_resource_bundle == null)
        {
            return key;
        }
        return cache.get_I18n_string_internal(key,logger);
    }

    public static void reset()
    {
        cache = null;
    }

}
