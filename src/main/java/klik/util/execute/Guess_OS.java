package klik.util.execute;

import javafx.stage.Window;
import klik.util.log.Logger;

//**********************************************************
public class Guess_OS
//**********************************************************
{
    //**********************************************************
    public static Operating_system guess(Window owner, Logger logger)
    //**********************************************************
    {
        String os_name = System.getProperty("os.name");

        if ( os_name.toLowerCase().contains("mac")) return Operating_system.MacOS;
        if ( os_name.toLowerCase().contains("linux")) return Operating_system.Linux;
        if ( os_name.toLowerCase().contains("windows")) return Operating_system.Windows;


        logger.log("❌ PANIC cannot guess Operating System ? from: "+os_name);
        return Operating_system.Unknown;
    }
}
