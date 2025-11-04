// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.util.files_and_paths;

import klik.util.log.Logger;

//**********************************************************
public class Filename_sanitizer
//**********************************************************
{
    // remove all 'weird' stuff in a string that may make it
    // unsuitable for a filename and especially a URL
    //**********************************************************
    public static String sanitize(String name, Logger logger)
    //**********************************************************
    {
        char[] x = name.toCharArray();
        name = "";
        boolean last_was_underscore = false; // avoid several underscores
        for ( char c:x)
        {
            if (is_allowed(c))
            {
                name += c;
                if ( c == '_') last_was_underscore = true;
            }
            else
            {
                if ( last_was_underscore)
                {
                }
                else
                {
                    name += "_";
                    last_was_underscore = true;
                }

            }
        }
        name = name.replaceFirst("^_", ""); // remove leading underscore
        name = name.replaceAll("_$", ""); // remove trailing underscores
        //logger.log("sanitize "+name);
        return name;
    }


    //**********************************************************
    private static boolean is_allowed(char c)
    //**********************************************************
    {
        int i = (int) c;
        if ((i>= 48) &&( i <= 57)) return true; // numbers 0-9
        if ((i>= 65) &&( i <= 90)) return true; // uppercase A-Z
        if ((i>= 97) &&( i <= 122)) return true; //lowercase a-z
        if (i == 95) return true; // underscore

        // keep common french chars
        if ( c == 'é') return true;
        if ( c == 'è') return true;
        if ( c == 'ê') return true;
        if ( c == 'ô') return true;
        if ( c == 'ç') return true;
        if ( c == 'ù') return true;
        if ( c == 'à') return true;

        return false;
    }
}
