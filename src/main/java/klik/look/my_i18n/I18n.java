package klik.look.my_i18n;

import klik.Klik_application;
import klik.util.Logger;
import klik.util.Stack_trace_getter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

//**********************************************************
public class I18n
//**********************************************************
{
    private static final boolean dbg = false;
    private ResourceBundle the_resource_bundle;

    //**********************************************************
    private I18n(String language, String country, Logger logger)
    //**********************************************************
    {
        Locale the_locale = new Locale.Builder().setLanguage(language).setRegion(country).build();
        Locale.setDefault(the_locale);
        try
        {
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
                try (InputStream x = Klik_application.class.getResourceAsStream(test_path)) {
                    logger.log("InputStream = Klik_application.class.getResourceAsStream(" + test_path + ")=" + x);
                } catch (IOException ex) {
                    logger.log("PANIC: "+Stack_trace_getter.get_stack_trace_for_throwable(ex));
                    return;
                }
            }
            {
                String test_path = "MessagesBundle_fr_FR.properties";
                try (InputStream x = Klik_application.class.getResourceAsStream(test_path)) {
                    logger.log("InputStream = Klik_application.class.getResourceAsStream(" + test_path + ")=" + x);
                } catch (IOException ex) {
                    logger.log("PANIC: "+Stack_trace_getter.get_stack_trace_for_throwable(ex));
                    return;                }
            }

            return;
        }
        if ( the_resource_bundle == null)
        {
            logger.log("BADBADBAD2 failed to load language resource; ResourceBundle.getBundle() returns null");
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
            logger.log(Stack_trace_getter.get_stack_trace("BADBADBAD I18n ->"+key+"<-\nnot found in classpath = "+classpath));
            logger.log("the_resource_bundle contains:");
            Enumeration<String> es = the_resource_bundle.getKeys();
            while (es.hasMoreElements()) {
                logger.log("->"+es.nextElement()+"<-");
            }
            return key;
        }
    }


    // cached instance

    private static I18n cache = null;

    //**********************************************************
    public static String get_I18n_string(String key, Logger logger)
    //**********************************************************
    {
        if (cache == null)
        {
            Language language = Language_manager.get_current_language(logger);
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
