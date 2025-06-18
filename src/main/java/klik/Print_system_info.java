package klik;

import com.sun.management.OperatingSystemMXBean;

import java.lang.management.ManagementFactory;
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
        //print_all_font_families();

    }

    //**********************************************************
    private static void show_runtime_properties()
    //**********************************************************
    {

        OperatingSystemMXBean b = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        System.out.println("\n\nNumber of cores: "+Runtime.getRuntime().availableProcessors());
        System.out.println("Physical RAM on this machine: "+b.getTotalPhysicalMemorySize()/1_000_000_000.0+" GBytes");
        System.out.println("Java VM max RAM for klik: "+(int)(Runtime.getRuntime().maxMemory()/1_000_000_000.0)+" GBytes (reported by Runtime.maxMemory()");
        //System.out.println("Java VM current RAM for klik: "+(int)(Runtime.getRuntime().totalMemory()/1_000_000_000.0)+" GBytes");
        //System.out.println("Java VM currently free RAM for klik: "+(int)(Runtime.getRuntime().freeMemory()/1_000_000_000.0)+" GBytes");
        System.out.println("\n\n");
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
    public static void print_all_font_families()
    //**********************************************************
    {
        System.out.println("*********FONT FAMILIES***********");
        for (String ff : javafx.scene.text.Font.getFamilies())
        {
            System.out.println(ff);
        }
        System.out.println("*********FONT NAMES***********");
        for (String ff : javafx.scene.text.Font.getFontNames())
        {
            System.out.println(ff);
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
