//SOURCES ./Language.java
//SOURCES ./French.java
//SOURCES ./English.java

//SOURCES ./Chinese.java
//SOURCES ./German.java
//SOURCES ./Italian.java
//SOURCES ./Japanese.java
//SOURCES ./Korean.java
//SOURCES ./Portuguese.java
//SOURCES ./Spanish.java
//SOURCES ./Breton.java



package klik.look.my_i18n;


import klik.properties.Non_zooleans;
import klik.util.log.Logger;

import java.util.*;

//**********************************************************
public class Language_manager
//**********************************************************
{
    public static final boolean dbg = false;
    public static Logger logger;

    private static Language instance = null;
    public static Map<String, Language> registered_languages = new HashMap<>();
    private static final Language[] languages = {
            new German(),
            new Breton(),
            new Chinese(),
            new English(),
            new French(),
            new Italian(),
            new Japanese(),
            new Korean(),
            new Portuguese(),
            new Spanish()
    };

    //**********************************************************
    public static Language get_current_language(Logger logger)
    //**********************************************************
    {
        if ( instance == null)
        {
            logger.log("FATAL: you MUST set the language by calling Language_manager.init_registered_languages(logger) before anycall to My_I18n");
        }
        return instance;
    }


    //**********************************************************
    public static List<String> get_registered_language_keys()
    //**********************************************************
    {
        List<String> returned = new ArrayList<>();
        for(Language l : languages) returned.add(l.language_key());
        return returned;
    }

    //**********************************************************
    public static void init_registered_languages(Logger logger_)
    //**********************************************************
    {
        logger = logger_;
        if (!registered_languages.isEmpty()) return;

        for ( Language l : languages)
        {
            //l.print_all();
            registered_languages.put(l.language_key(), l);
        }
        String language_key = Non_zooleans.get_language_key();
        instance = registered_languages.get(language_key);

        if (instance == null) instance = registered_languages.get((new English()).language_key());

    }


    //**********************************************************
    public static void set_current_language_key(String language_key)
    //**********************************************************
    {
        System.out.println("using language_key ->"+language_key+"<-");
        instance = registered_languages.get(language_key);
        Non_zooleans.set_language_key(language_key);
        Locale.setDefault(instance.locale);

    }
}
