package klik.I18N;


import klik.properties.Properties;
import klik.util.Logger;

import java.util.*;

//**********************************************************
public class Local_manager
//**********************************************************
{
    public static final boolean dbg = false;
    public static Logger logger;

    private static Language instance = null;
    public static Map<String, Language> registered = new HashMap<>();

    //**********************************************************
    public static Language get_instance()
    //**********************************************************
    {
        return instance;
    }

    private static Language locals[] = {
            new FR(),
            new US(),
    };

    //**********************************************************
    public static List<String> get_registered_locals()
    //**********************************************************
    {
        List<String> returned = new ArrayList<>();
        for(Language l :locals) returned.add(l.display_name);
        return returned;
    }

    //**********************************************************
    public static void init_Locals(Logger logger_)
    //**********************************************************
    {
        logger = logger_;
        if (registered.isEmpty() == false) return;

        for ( Language l : locals)
        {
            registered.put(l.display_name, l);
        }
        String l = Properties.get_language(logger_);
        instance = registered.get(l);

        if (instance == null) instance = registered.get((new US()).display_name);

    }


    public static void set_instance(String s)
    {
        instance = registered.get(s);
        Properties.set_language(s);
        Locale.setDefault(Locale.FRANCE);

    }
}
