// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr;

import javafx.application.Application;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//**********************************************************
public record Start_context(List<String> args, String bet_full)
//**********************************************************
{
    private static boolean dbg = false;

    //**********************************************************
    private Map<String, String> parse_arguments(String args)
    //**********************************************************
    {
        Map<String, String> parsed = new HashMap<>();
        if (args == null || args.trim().isEmpty()) return parsed;

        String[] pairs = args.trim().split("\\s+");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                parsed.put(keyValue[0], keyValue[1]);
            }
        }
        return parsed;
    }

    //**********************************************************
    public static Start_context get_context_and_args(Application application)
    //**********************************************************
    {
        Application.Parameters params = application.getParameters();
        List<String> raw_args = params.getRaw();
        List<String> args = new ArrayList<>();
        String bet_full = null;
        for(String s : raw_args)
        {
            bet_full = s;
            if (s.contains(" "))
            {
                if (dbg) System.out.println("❗Warning: argument contains spaces ->" + s+"<-");
                String pieces[] = s.split("\\s+");
                for ( String piece : pieces )
                {
                    if ( !piece.isBlank() )
                    {
                        if (dbg) System.out.println("argument ->" + piece+"<-");
                        args.add(piece);
                    }
                }
            }
            else
            {
                if (dbg) System.out.println("argument  ->" + s+"<-");
                args.add(s);
            }
        }


        Start_context returned = new Start_context(args,bet_full);

        if ( dbg ) returned.print();
        return returned;
    }

    //**********************************************************
    private void print()
    //**********************************************************
    {
        System.out.println("======================= ");
        System.out.println(" NB args = " + args().size());
        for ( String s : args() ) System.out.println("        "+s);
        System.out.println("======================= ");
    }

    //**********************************************************
    public Path extract_path()
    //**********************************************************
    {
        // the challenge is to support
        //Warning: argument contains spaces ->/Users/philippegentric/Desktop/help/misc/not_dangerous/valuable/Music/PG_music_oldies/Queen Greatests Hits<-
        if ( args().isEmpty() ) return null;

        // the name may contain spaces AND integers...
        ///  the only reliable way is to check if the file/folder exists

        File f = new File(bet_full);
        if (f.exists() && f.isDirectory()) {
            // first arg is a path
            return f.toPath();
        }
        // or try the last arg
        String lastArg = args().get(args().size() - 1);
        f = new File(lastArg);
        if (f.exists() && f.isDirectory()) {
            // last arg is a path
            return f.toPath();
        }
        return null;
    }

    //**********************************************************
    public Integer extract_reply_port()
    //**********************************************************
    {
        if ( args().size() < 1)
        {
            System.out.println("no reply port looking at ->" + args()+"<-");
            return null;
        }
        String arg = args().get(0);
        try {
            return Integer.parseInt(arg);
        }
        catch (NumberFormatException e)
        {

        }
        System.out.println("no reply port found in ->" + args()+"<-");
        return null;
    }

}
