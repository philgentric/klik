package klik.properties;

import klik.actor.Aborter;
import klik.util.log.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

//**********************************************************
public class File_based_IProperties implements IProperties
//**********************************************************
{
    private final static boolean dbg = false;
    private final Logger logger;
    private final String tag;
    private final Properties_manager pm;

    //**********************************************************
    public File_based_IProperties(String tag, Aborter aborter, Logger logger)
    //**********************************************************
    {
        this.tag = tag;
        this.logger = logger;
        String home = System.getProperty(Non_booleans.USER_HOME);
        Path p = Paths.get(home, Non_booleans.CONF_DIR, tag+".properties");
        pm = new Properties_manager(p, tag, aborter,logger);
    }

    //**********************************************************
    @Override
    public boolean set(String key, String value)
    //**********************************************************
    {
        if( dbg) logger.log("File_based_IProperties "+tag+" set() "+key+"-"+value);
        return pm.add(key, value);
    }
    //**********************************************************
    @Override
    public String get(String key)
    //**********************************************************
    {
        if( dbg) logger.log("File_based_IProperties "+tag+" get() "+key);
        return pm.get(key);
    }
    //**********************************************************
    @Override
    public void remove(String key)
    //*********************************************************
    {
        if( dbg) logger.log("File_based_IProperties "+tag+" remove() "+key);
        pm.remove(key);
    }

    //**********************************************************
    @Override
    public void clear()
    //**********************************************************
    {
        if( dbg) logger.log("File_based_IProperties "+tag+" clear() ");
        pm.clear();
    }
    //**********************************************************
    @Override
    public List<String> get_all_keys()
    //**********************************************************
    {
        if( dbg) logger.log("File_based_IProperties "+tag+" get_all_keys()");
        Set<String> x = pm.get_all_keys();
        List<String> result = new ArrayList<>();
        result.addAll(x);
        return result;
    }

    //**********************************************************
    @Override
    public String get_tag()
    //**********************************************************
    {
        return tag;
    }

}
