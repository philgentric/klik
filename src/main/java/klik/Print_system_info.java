package klik;

import java.util.Properties;

public class Print_system_info {

    public static void print()
    {
        System.out.println("JRE:    "+Runtime.version());
        System.out.println("JavaFX: "+System.getProperty("javafx.runtime.version"));

        //Font_name.print_all_font_families();
/*
        Map<String,String> env = System.getenv();
        for (String key : env.keySet())
        {
            String value = env.get(key);
            System.out.println(key+" = "+value);
        }
*/

        Properties p = System.getProperties();
        for ( String name : p.stringPropertyNames())
        {
            System.out.println(name+" = "+p.getProperty(name));
        }
        System.out.println("RAM total: "+(int)(Runtime.getRuntime().totalMemory()/1_000_000.0)+" MBytes");
        System.out.println("RAM free: "+(int)(Runtime.getRuntime().freeMemory()/1_000_000.0)+" MBytes");


    }
}
