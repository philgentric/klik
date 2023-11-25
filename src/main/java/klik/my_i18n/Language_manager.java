package klik.my_i18n;


import klik.properties.Static_application_properties;
import klik.util.Logger;

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
            new FR(),
            new US(),
    };

    //**********************************************************
    public static Language get_current_language()
    //**********************************************************
    {
        return instance;
    }


    //**********************************************************
    public static List<String> get_registered_languages()
    //**********************************************************
    {
        List<String> returned = new ArrayList<>();
        for(Language l : languages) returned.add(l.display_name);
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
            registered_languages.put(l.display_name, l);
        }
        String l = Static_application_properties.get_language(logger_);
        instance = registered_languages.get(l);

        if (instance == null) instance = registered_languages.get((new US()).display_name);

    }


    //**********************************************************
    public static void set_current_language(String language)
    //**********************************************************
    {
        instance = registered_languages.get(language);
        Static_application_properties.set_language(language, logger);
        Locale.setDefault(Locale.FRANCE);

    }
}
