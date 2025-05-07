package klik;

import javafx.application.Application;
import klik.actor.Actor_engine;
import klik.properties.Non_booleans;
import klik.util.log.Logger;
import klik.util.tcp.TCP_client;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

//**********************************************************
public record Start_context(List<String> args, int port, Path path)
//**********************************************************
{
    private static boolean dbg = true;
    // 3 cases
    // no args
    // args[0] is String (typically a path designating the content) ... and more strings could follow
    // args[0] is Integer (typically the port on which to reply "started" ... and more strings could follow
    //**********************************************************
    public static Start_context get_context(Application application)
    //**********************************************************
    {
        Application.Parameters params = application.getParameters();
        List<String> raw_args = params.getRaw();
        List<String> args = new ArrayList<>();
        int port = -1;
        Path path = null;
        if (raw_args.isEmpty())
        {
            path = (new File(System.getProperty(Non_booleans.USER_HOME))).toPath();
        }
        else {

            try {
                port = Integer.parseInt(raw_args.get(0));
            } catch (NumberFormatException e) {
                // first arg is not a int, must be a path
                path = Path.of(raw_args.get(0));
            }
            for (int i = 1; i < raw_args.size(); i++) args.add(raw_args.get(i));
        }

        Start_context returned = new Start_context(args, port, path);

        if ( dbg ) returned.print();
        return returned;
    }

    //**********************************************************
    private void print()
    //**********************************************************
    {
        System.out.println("==== Start_context ====\n   path = "+path());
        System.out.println("   port = " + port());
        System.out.println("   other args = " + args().size());
        for ( String s : args() ) System.out.println("        "+s);
        System.out.println("======================= ");
    }

    //**********************************************************
    public static void send_started(Start_context context, Logger logger)
    //**********************************************************
    {
        if(context.port()<0) return;
        Runnable r = () -> send_started_raw(context, logger);
        Actor_engine.execute(r, logger);
    }

    //**********************************************************
    public static void send_started_raw(Start_context context, Logger logger)
    //**********************************************************
    {
        if(context.port()<0) return;
        TCP_client.request("localhost", context.port(), Launcher.STARTED, logger);
    }

}
