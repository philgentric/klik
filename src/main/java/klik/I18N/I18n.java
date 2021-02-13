package klik.I18N;

import klik.Klik_application;
import klik.util.Logger;

import java.io.InputStream;
import java.util.Enumeration;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class I18n
{

    private ResourceBundle the_resource_bundle;

    private I18n(String language, String country, Logger logger)
    {
        Locale the_locale = new Locale(language,country);
        Locale.setDefault(the_locale);
        try
        {
           //ClassLoader class_loader = Klik_application.class.getClassLoader();
            //ResourceBundle.Control control = ResourceBundle.Control.getControl(FORMAT_PROPERTIES);
            the_resource_bundle = ResourceBundle.getBundle("klik/MessagesBundle", the_locale);// class_loader, control);
        }
        catch(Exception e)
        {
            the_resource_bundle = null;
            logger.log("BADBADBAD1 failed to load language resource  : "+e);
            String classpath  = System.getProperty("java.class.path");
            logger.log("classpath  : "+classpath);
            logger.log("the_locale = "+the_locale);
            {
                String test_path = "MessagesBundle_fr_FR.properties";
                InputStream x = Klik_application.class.getResourceAsStream(test_path);
                logger.log("InputStream = Klik_application.class.getResourceAsStream(" + test_path + ")=" + x);
            }
            {
                String test_path = "klik/MessagesBundle_fr_FR.properties";
                InputStream x = Klik_application.class.getResourceAsStream(test_path);
                logger.log("InputStream = Klik_application.class.getResourceAsStream(" + test_path + ")=" + x);
            }

            return;
        }
        if ( the_resource_bundle == null)
        {
            logger.log("BADBADBAD2 failed to load language resource; ResourceBundle.getBundle() returns null");
            return;
        }
        logger.log(" OK, language resource found !");
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
