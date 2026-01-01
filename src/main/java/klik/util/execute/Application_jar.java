package klik.util.execute;

import klik.Klik_application;

import java.io.InputStream;




//**********************************************************
public class Application_jar
//**********************************************************
{
    private final static boolean dbg = true;


    //**********************************************************
    public static InputStream get_jar_InputStream_by_name(String name)
    //**********************************************************
    {
        // this scheme works with Jbang
        ClassLoader class_loader = Thread.currentThread().getContextClassLoader();
        if (dbg) System.out.println("get_InputStream_by_name trying with class_loader : "+class_loader+ " ...");
        InputStream s = class_loader.getResourceAsStream(name);
        if (s != null)
        {
            if (dbg) System.out.println("... worked");
            return s;
        }
        if (dbg) System.out.println("Thread.currentThread().getContextClassLoader().getResourceAsStream DID NOT work\n" +
                "Trying with Klik_application.class.getResourceAsStream ...");

        // this scheme works with Gradle
        InputStream returned =  Klik_application.class.getResourceAsStream(name);
        if ( dbg)
        {
            if ( returned != null) System.out.println("... worked");
        }
        return returned;
    }
}
