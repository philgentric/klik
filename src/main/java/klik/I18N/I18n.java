package klik.I18N;

import klik.util.Logger;

import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class I18n
{
    private static I18n instance = null;

    private ResourceBundle the_resource_bundle;

    private I18n(Logger logger)
    {
        String language = new String("fr");
        String country = new String("FR");
        Locale the_locale = new Locale(language, country);

        /*Class klass = the_locale.getClass();
        ClassLoader loader = klass.getClassLoader();
        s = loader.
         */

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

    public static String get_I18n_string(String key, Logger logger)
    {
        if (instance == null) instance = new I18n(logger);
        if ( instance.the_resource_bundle == null)
        {
            return key;
        }
        return instance.get_I18n_string_internal(key,logger);
    }

}
