// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.look.my_i18n;

import javafx.stage.Window;
import klik.look.Jar_utils;
import klik.properties.Non_booleans_properties;
import klik.properties.boolean_features.Feature_cache;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;


//**********************************************************
public class My_I18n
//**********************************************************
{

    private static final boolean dbg = false;
    private static final boolean ultra_dbg = false;
    private ResourceBundle the_resource_bundle;
    private final Language language;
    private final Locale locale;

    private static My_I18n instance = null;

    //**********************************************************
    // must return the key if not found in resources
    public static String get_I18n_string(String key, Window owner, Logger logger)
    //**********************************************************
    {
        if (instance == null)
        {
            String language_key = Non_booleans_properties.get_language_key(owner);
            //logger.log(Stack_trace_getter.get_stack_trace("My_I18n instance is null, rebuilding for "+language_key));
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
            logger.log(Stack_trace_getter.get_stack_trace("❌ BAD WARNING My_I18n ->"+key+"<- not found"));
            return key;
        }
        if ( dbg) logger.log("✅ OK My_I18n ->"+key+"<- was found for "+instance.language.name()+" : ->"+returned+"<-");
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
        catch (NullPointerException e)
        {
            // key is null ???
            return "unknown key";
        }
        catch (ClassCastException e)
        {
            return key;
        }
        catch (MissingResourceException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace("❌ BAD WARNING My_I18n ->"+key+"<- not found"));
            if ( ultra_dbg) {
                logger.log("the resource bundle contains these keys:");
                Enumeration<String> es = the_resource_bundle.getKeys();
                while (es.hasMoreElements()) {
                    logger.log("->" + es.nextElement() + "<-");
                }
            }
            return key;
        }
    }

    //**********************************************************
    private My_I18n(Language language, Locale locale, Logger logger)
    //**********************************************************
    {
        if ( dbg) {
            // Print all resources in the 'klik' directory
            String dirs[] = {"klik/","resources/klik/"};
            for (String dir : dirs) {
                try {
                    System.out.println("directory: " + dir);
                    Enumeration<URL> urls = getClass().getClassLoader().getResources(dir);
                    while (urls.hasMoreElements()) {
                        System.out.println("Resource found: " + urls.nextElement());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
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
            logger.log("❗ WARNING: method1 failed to load language resource : "+e+"\n    ...will try another way ");

            // this method works with jbang
            try {
                String name = "languages/MessagesBundle" + "_" + locale.getLanguage() + "_" + locale.getCountry()+".properties";
                if ( dbg) logger.log("✅ trying get_jar_InputStream_by_name with name : "+name);

                InputStream is = Jar_utils.get_jar_InputStream_by_name(name);
                the_resource_bundle = new PropertyResourceBundle(is);
                if ( dbg) logger.log("✅ method2 succeeded loading language resource  : "+name);
           }
            catch (Exception e2)
            {
                logger.log("method2 failed to load language resource  : "+e2);
            }
        }
        if ( the_resource_bundle == null)
        {
            logger.log("❌ BAD WARNING failed to load language resource: "+locale);
            return;
        }
        if ( dbg)
        {
            logger.log("✅ OK, language resource found for "+locale);
            Enumeration<String> x = the_resource_bundle.getKeys();
            while ( x.hasMoreElements())
            {
                String k = x.nextElement();
                logger.log(k + " ==> " + the_resource_bundle.getString(k));
            }
        }
   }

    //**********************************************************
    public static void set_new_language(Language language, Window owner,Logger logger)
    //**********************************************************
    {
        instance = null;
        Feature_cache.update_string(Non_booleans_properties.LANGUAGE_KEY,language.name(),owner,logger);
    }

    //**********************************************************
    public static void reset()
    //**********************************************************
    {
        instance = null;
    }
}
