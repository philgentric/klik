package klik.look.my_i18n;

import klik.look.Jar_utils;
import klik.properties.Non_booleans;
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
    private final Language language;
    private final Locale locale;

    private static My_I18n instance = null;


    //**********************************************************
    public static String get_I18n_string(String key, Logger logger)
    //**********************************************************
    {
        if (instance == null)
        {
            String language_key = Non_booleans.get_language_key();
            Language language = Language.valueOf(language_key);
            Locale locale = language.get_locale();
            instance = new My_I18n(language, locale, logger);
        }
        if ( instance.the_resource_bundle == null)
        {
            return key;
        }
        String returned = instance.get_I18n_string_internal(key,logger);
        if ( returned == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BAD WARNING My_I18n ->"+key+"<- not found"));
            return key;
        }
        if ( dbg) logger.log("OK My_I18n ->"+key+"<- was found for "+instance.language.name()+" : ->"+returned+"<-");
        return returned;
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
            logger.log(Stack_trace_getter.get_stack_trace("BAD WARNING My_I18n ->"+key+"<- not found"));
            logger.log("the resource bundle contains these keys:");
            Enumeration<String> es = the_resource_bundle.getKeys();
            while (es.hasMoreElements()) {
                logger.log("->"+es.nextElement()+"<-");
            }
            return key;
        }
    }




    //**********************************************************
    private My_I18n(Language language, Locale locale, Logger logger)
    //**********************************************************
    {
        this.language = language;
        this.locale = locale;
        Locale.setDefault(locale);
        try
        {
            // this method works with gradle
            the_resource_bundle = ResourceBundle.getBundle("klik/MessagesBundle", locale);// class_loader, control);
        }
        catch(Exception e)
        {
            logger.log("WARNING: method1 failed to load language resource : "+e+"\n...will try another way ");

            // this method work with jbang
            try {
                String name = "MessagesBundle" + "_" + locale.getLanguage() + "_" + locale.getCountry()+".properties";
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
            logger.log("BAD WARNING failed to load language resource: "+locale);
            return;
        }
        if ( dbg)
        {
            logger.log(" OK, language resource found for "+locale);
            Enumeration<String> x = the_resource_bundle.getKeys();
            while ( x.hasMoreElements())
            {
                String k = x.nextElement();
                logger.log(k + " ==> " + the_resource_bundle.getString(k));
            }
        }
   }




    //**********************************************************
    public static void set_new_language(Language my_locale_codes)
    //**********************************************************
    {
        Non_booleans.set_language_key(my_locale_codes.name());
        instance = null;
    }

}
