package klik;

import java.util.Map;
import java.util.Properties;

//**********************************************************
public class Print_system_info
//**********************************************************
{

    //**********************************************************
    public static void print()
    //**********************************************************
    {
        System.out.println("JRE:    "+Runtime.version());
        System.out.println("JavaFX: "+System.getProperty("javafx.runtime.version"));


        show_java_system_properties();
        show_runtime_properties();

        //show_environement_variables();
        //Font_name.print_all_font_families();

    }

    //**********************************************************
    private static void show_runtime_properties()
    //**********************************************************
    {

        System.out.println("Number of cores: "+Runtime.getRuntime().availableProcessors());
        System.out.println("RAM total: "+(int)(Runtime.getRuntime().totalMemory()/1_000_000.0)+" MBytes");
        System.out.println("RAM free: "+(int)(Runtime.getRuntime().freeMemory()/1_000_000.0)+" MBytes");
    }

    //**********************************************************
    private static void show_java_system_properties()
    //**********************************************************
    {
        Properties p = System.getProperties();
        for ( String name : p.stringPropertyNames())
        {
            System.out.println(name+" = "+p.getProperty(name));
        }
    }

    //**********************************************************
    private static void show_environement_variables()
    //**********************************************************
    {
        Map<String, String> env = System.getenv();
        for (String key : env.keySet()) {
            String value = env.get(key);
            System.out.println(key + " = " + value);
        }
    }
}
