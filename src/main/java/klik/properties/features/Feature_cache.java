package klik.properties.features;

import klik.properties.Booleans;
import klik.properties.Non_booleans;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

//**********************************************************
public class Feature_cache
//**********************************************************
{
    //public static Map<String,String> string_feature_cache = new HashMap<>();

    private static Map<String, List<String_change_target>> string_registered_for = new HashMap<>();


    private static Map<Feature,List<Feature_change_target>> registered_for = new HashMap<>();
    private static List<Feature_change_target> registered_for_all = new ArrayList<>();
    public static Map<Feature,Boolean> boolean_feature_cache = new HashMap<>();
    static {
        for (Feature f : Feature.values())
        {
            boolean_feature_cache.put(f, Booleans.get_boolean(f.name()));
        }
    }

    //**********************************************************
    public static void register_for_all(Feature_change_target fct)
    //**********************************************************
    {
        registered_for_all.add(fct);
    }



    //**********************************************************
    public static void register_for(Feature feature, Feature_change_target fct)
    //**********************************************************
    {
        List<Feature_change_target> l = registered_for.get(feature);
        if (  l == null)
        {
            l = new ArrayList<>();
            registered_for.put(feature,l);
        }
        if ( !l.contains(fct)) l.add(fct);

    }


    //**********************************************************
    public static void string_register_for(String key, String_change_target sct)
    //**********************************************************
    {
        List<String_change_target> l = string_registered_for.get(key);
        if (  l == null)
        {
            l = new ArrayList<>();
            string_registered_for.put(key,l);
        }
        if ( !l.contains(sct)) l.add(sct);
    }
    //**********************************************************
    public static void string_deregister_for(String key, String_change_target sct)
    //**********************************************************
    {
        List<String_change_target> l = string_registered_for.get(key);
        if ( l == null) return;
        l.remove(sct);
    }
    //**********************************************************
    public static void deregister_for_all(Feature_change_target fct)
    //**********************************************************
    {
        registered_for_all.remove(fct);
    }
    //**********************************************************
    public static void deregister_for(Feature feature, Feature_change_target fct)
    //**********************************************************
    {
        List<Feature_change_target> l = registered_for.get(feature);
        if ( l == null) return;
        l.remove(fct);
    }

    //**********************************************************
    public static boolean get(Feature feature)
    //**********************************************************
    {
        return boolean_feature_cache.get(feature);
    }

    //**********************************************************
    public static void update_cached_feature(Feature feature, boolean new_val)
    //**********************************************************
    {
        Booleans.set_boolean(feature.name(),new_val);
        boolean_feature_cache.put(feature,new_val);
        for( Feature_change_target fct : registered_for_all) fct.update(feature,new_val);

        List<Feature_change_target> l = registered_for.get(feature);
        if ( l == null) return;
        for( Feature_change_target fct : l) fct.update(feature,new_val);
    }

    //**********************************************************
    public static void update_string(String key, String new_val)
    //**********************************************************
    {
        if ( key.equals(Non_booleans.LANGUAGE_KEY))
        {
            System.out.println("Feature_cache Non_booleans.set_language_key("+new_val+")");
            Non_booleans.set_language_key(new_val);
        }
        List<String_change_target> l = string_registered_for.get(key);
        if ( l == null) return;
        List<String_change_target> tmp_copy = new ArrayList<>(l); // avoid problems when update_config_string triggers the creation of new Virtaul_landscape, which registers...
        for( String_change_target sct : tmp_copy) sct.update_config_string(key,new_val);
    }
}
