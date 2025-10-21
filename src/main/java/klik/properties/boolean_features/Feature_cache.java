package klik.properties.boolean_features;
//SOURCES ../../Launcher.java
import javafx.stage.Window;
import klik.Klik_application;
import klik.Launcher;
import klik.Shared_services;
import klik.properties.Non_booleans_properties;
import klik.util.log.Logger;
import klik.util.tcp.TCP_client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//**********************************************************
public class Feature_cache
//**********************************************************
{
    //public static Map<String,String> string_feature_cache = new HashMap<>();

    private static Map<String, List<String_change_target>> string_registered_for = new HashMap<>();


    private static Map<Feature,List<Feature_change_target>> registered_for = new HashMap<>();
    private static List<Feature_change_target> registered_for_any_boolean_change = new ArrayList<>();
    public static Map<Feature,Boolean> boolean_feature_cache = new HashMap<>();

    private static List<Feature> default_to_true = List.of(
            Feature.Show_icons_for_files,
            Feature.Reload_last_folder_on_startup,
            Feature.Monitor_folders,
            Feature.Use_escape_to_close_windows,
            Feature.Show_graphicsmagick_install_warning,
            Feature.Show_ffmpeg_install_warning);

    static {
        for (Feature f : Feature.values())
        {
            if ( default_to_true.contains(f))
            {
                boolean_feature_cache.put(f,Booleans.get_boolean_defaults_to_true(f.name(),null));
            }
            else
            {
                boolean_feature_cache.put(f, Booleans.get_boolean(f.name(),null));
            }
        }
    }

    //**********************************************************
    public static void register_for_all(Feature_change_target fct)
    //**********************************************************
    {
        registered_for_any_boolean_change.add(fct);
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
    public static void string_deregister_all(String_change_target sct)
    //**********************************************************
    {
        for( String key : string_registered_for.keySet())
        {
            List<String_change_target> l = string_registered_for.get(key);
            if ( l == null) continue;
            l.remove(sct);
        }
    }
    //**********************************************************
    public static void deregister_for_all(Feature_change_target fct)
    //**********************************************************
    {
        registered_for_any_boolean_change.remove(fct);
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
    public static void update_cached_boolean(Feature feature, boolean new_val, Window owner)
    //**********************************************************
    {
        Booleans.set_boolean(feature.name(),new_val,owner);
        boolean_feature_cache.put(feature,new_val);
        for( Feature_change_target fct : registered_for_any_boolean_change) fct.update(feature,new_val);

        List<Feature_change_target> l = registered_for.get(feature);
        if ( l == null) return;
        for( Feature_change_target fct : l) fct.update(feature,new_val);
    }

    //**********************************************************
    public static void update_string(String key, String new_value, Window owner,Logger logger)
    //**********************************************************
    {
        Preferences_stage.reset();
        System.out.println("Feature_cache: "+key+"=>"+new_value);
        Shared_services.main_properties().set(key, new_value);
        send_UI_changed(Launcher.UI_CHANGED,new_value, logger);
        List<String_change_target> l = string_registered_for.get(key);
        if ( l == null) return;
        List<String_change_target> tmp_copy = new ArrayList<>(l); // avoid problems when update_config_string triggers the creation of new Virtaul_landscape, which registers...
        for( String_change_target sct : tmp_copy) sct.update_config_string(key,new_value);
    }


    //**********************************************************
    public static void send_UI_changed(String msg, String new_lang, Logger logger)
    //**********************************************************
    {
        logger.log("\n\nFeature_cache::sending "+msg+" on port 'ui_change_report_port_at_launcher': "+ Klik_application.ui_change_report_port_at_launcher);
        if (Klik_application.ui_change_report_port_at_launcher != null)
        {
            // when klik is started "standalone" (not from the launcher), this port is null
            TCP_client.send_in_a_thread("localhost", Klik_application.ui_change_report_port_at_launcher, msg+" "+new_lang, logger);
        }
    }

}
