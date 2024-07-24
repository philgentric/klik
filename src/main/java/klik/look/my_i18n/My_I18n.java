package klik.look.my_i18n;

import klik.look.Jar_utils;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;

import java.io.InputStream;
import java.util.*;


//**********************************************************
public class My_I18n
//**********************************************************
{
    private static final boolean dbg = false;
    private ResourceBundle the_resource_bundle;

    //**********************************************************
    private My_I18n(String language, String country, Logger logger)
    //**********************************************************
    {
        Locale the_locale = new Locale.Builder().setLanguage(language).setRegion(country).build();
        Locale.setDefault(the_locale);
        try
        {
            // this method works with gradle
            the_resource_bundle = ResourceBundle.getBundle("klik/MessagesBundle", the_locale);// class_loader, control);
        }
        catch(Exception e)
        {
            logger.log("WARNING: method1 failed to load language resource : "+e+"\n...will try another way ");

            // this method work with jbang
            try {
                String name = "MessagesBundle" + "_" + language + "_" + country+".properties";
                logger.log("trying get_jar_InputStream_by_name with name : "+name);

                InputStream is = Jar_utils.get_jar_InputStream_by_name(name);
                the_resource_bundle = new PropertyResourceBundle(is);
                logger.log("method2 succeeded loading language resource  : "+name);
           }
            catch (Exception e2)
            {
                logger.log("method2 failed to load language resource  : "+e2);
            }
        }
        if ( the_resource_bundle == null)
        {
            logger.log("BAD WARNING failed to load language resource"+language);
            return;
        }
        if ( dbg)
        {
            logger.log(" OK, language resource found for "+the_locale);
            Enumeration<String> x = the_resource_bundle.getKeys();
            while ( x.hasMoreElements())
            {
                String k = x.nextElement();
                logger.log(k + " ==> " + the_resource_bundle.getString(k));
            }
        }
   }


    //**********************************************************
    private String get_I18n_string_internal(String key, Logger logger)
    //**********************************************************
    {
        try
        {
            return the_resource_bundle.getString(key);
        }
        catch (MissingResourceException e)
        {
            String classpath  = System.getProperty("java.class.path");
            logger.log(Stack_trace_getter.get_stack_trace("BAD WARNING My_I18n ->"+key+"<-\nnot found in classpath = "+classpath));
            logger.log("the_resource_bundle contains:");
            Enumeration<String> es = the_resource_bundle.getKeys();
            while (es.hasMoreElements()) {
                logger.log("->"+es.nextElement()+"<-");
            }
            return key;
        }
    }


    // cached instance

    private static My_I18n cache = null;

    //**********************************************************
    public static String get_I18n_string(String key, Logger logger)
    //**********************************************************
    {
        if (cache == null)
        {
            Language language = Language_manager.get_current_language(logger);
            cache = new My_I18n(language.language, language.country, logger);
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
