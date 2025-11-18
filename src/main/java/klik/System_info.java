// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik;


import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Properties;

//import java.lang.management.ManagementFactory;
//import com.sun.management.OperatingSystemMXBean;

//**********************************************************
public class System_info
//**********************************************************
{
    //**********************************************************
    public static int how_many_cores()
    //**********************************************************
    {
        return Runtime.getRuntime().availableProcessors();
    }

    // in order to recover from git the code previously here, erqsed recently,
    // thqt was using a JMX MANAGEMENT BEAN TO FIND THE PHYSICAL MACHINE raM size,
    // but unfortunately the term 'physical, is not in the commit messages,
    // open q shell qnd type this git commqnd:
    // git log -S 'physical' --source --all --pretty=format:'%h %ad %s' --date=short
    // it will give you the name of the commit, then
    // to just have a peek at eh old code, type:


    //**********************************************************
    public static void print()
    //**********************************************************
    {
        System.out.println("Is NATIVE?   "+is_native());
        print_machine_properties();

        print_java_system_properties();

        //show_environement_variables();
        //print_all_font_families();

    }

    //**********************************************************
    public static boolean is_native()
    //**********************************************************
    {
        return Boolean.parseBoolean(
                System.getProperty("org.graalvm.nativeimage.isRuntime", "false"));
    }
    private static final boolean use_JMX_for_RAM = false;
    //**********************************************************
    public static long get_total_machine_RAM_in_GBytes()
    //**********************************************************
    {
        //if ( use_JMX_for_RAM) {
        //    OperatingSystemMXBean b = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        //    return b.getTotalPhysicalMemorySize() / (1024L * 1024L * 1024L);
        //}
        //else
        {
            String os_name = System.getProperty("os.name").toLowerCase();
            if (os_name.contains("mac")) {
                String cmd = "sysctl hw.memsize";
                // the reply is something like: hw.memsize: 68719476736
                // these are bytes
                try {
                    Process p = Runtime.getRuntime().exec(cmd);
                    p.waitFor();
                    java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream()));
                    String line = reader.readLine();
                    if (line != null && line.startsWith("hw.memsize: ")) {
                        String s = line.substring(12).trim();
                        long bytes = Long.parseLong(s);
                        return bytes / (1024L * 1024L * 1024L);
                    }
                } catch (Exception e) {
                    System.out.println("Error when executing command: " + cmd);
                    e.printStackTrace();
                }

            } else if (os_name.contains("win")) {
                try {
                    Process process = Runtime.getRuntime().exec("wmic ComputerChip get TotalPhysicalMemory");
                    java.util.Scanner s = new java.util.Scanner(process.getInputStream());
                    while (s.hasNext()) {
                        String line = s.next();
                        if (!line.isEmpty() && line.matches("\\d+")) {
                            long totalMemoryBytes = Long.parseLong(line);
                            long totalMemoryGB = totalMemoryBytes / (1024L * 1024L * 1024L);
                            System.out.println("Total Physical Memory: " + totalMemoryGB + " GB");
                            return totalMemoryGB;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (os_name.contains("nux") || os_name.contains("nix")) {
                String cmd = "grep MemTotal /proc/meminfo";
                // the reply is something like: MemTotal:       16367484 kB
                try {
                    Process p = Runtime.getRuntime().exec(cmd);
                    p.waitFor();
                    java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream()));
                    String line = reader.readLine();
                    if (line != null && line.startsWith("MemTotal:")) {
                        String s = line.substring(9).trim();
                        s = s.substring(0, s.length() - 3).trim(); // remove kB
                        long kbytes = Long.parseLong(s);
                        return (int) (kbytes / (1024L * 1024L));
                    }
                } catch (Exception e) {
                    System.out.println("Error when executing command: " + cmd);
                    e.printStackTrace();
                }
            }
            return -1;

        }
    }

    //**********************************************************
    private static void print_machine_properties()
    //**********************************************************
    {
        System.out.println("Physical RAM on this machine: "+ get_total_machine_RAM_in_GBytes()+" GBytes");
        System.out.println("\n\nNumber of cores: "+ how_many_cores());
        System.out.println("Java VM max RAM for klik: "+(int)(Runtime.getRuntime().maxMemory()/1_000_000_000.0)+" GBytes (reported by Runtime.maxMemory()");
        //System.out.println("Java VM current RAM for klik: "+(int)(Runtime.getRuntime().totalMemory()/1_000_000_000.0)+" GBytes");
        //System.out.println("Java VM currently free RAM for klik: "+(int)(Runtime.getRuntime().freeMemory()/1_000_000_000.0)+" GBytes");
        System.out.println("\n\n");
    }



    //**********************************************************
    private static void print_java_system_properties()
    //**********************************************************
    {
        System.out.println("JRE:    "+Runtime.version());
        System.out.println("JavaFX: "+System.getProperty("javafx.runtime.version"));

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
