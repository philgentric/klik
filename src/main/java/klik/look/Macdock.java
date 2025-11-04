// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.look;

import klik.util.log.Logger;

public class Macdock {
    private static boolean load_done = false;

    static
    {
        try
        {
            //To avoid `java.library.path` setup
            // replace
            // `System.loadLibrary("dockjni")`
            // with
            // `System.load("/absolute/path/to/libdockjni.dylib")`.

            System.loadLibrary("macdock"); // Load the native library for MacOS dock icons libmacdock.dylib
            load_done = true;
            System.out.println("Native library for MacOS dock icons loaded successfully.");
        }
        catch (UnsatisfiedLinkError e)
        {
            System.err.println("Failed to load native library for MacOS dock icons: " + e.getMessage());
            load_done = false;
        }
    }

    private Macdock() {
        // Private constructor to prevent instantiation
    }

    public static boolean is_available(Logger logger)
    {
        String os_name = System.getProperty("os.name").toLowerCase();
        if (os_name.contains("mac"))
        {
            logger.log("MacOS detected, checking if native library is available for dock icons....");
            if (load_done)
            {
                logger.log("Native library for MacOS dock icons is available.");
                return true;
            }
            logger.log("WARNING: Native library for MacOS dock icons is NOT available.");
        }
        return false;
    }




    public static void setup_ext(String badge, byte[] icon_data, Logger logger) {
        if (is_available(logger)) {
            logger.log("calling native_setup for MacOS dock icons with badge: " + badge);
            setup(badge, icon_data);
        }
    }

    private static native void setup(String badge, byte[] iconPng);


}
